package com.grinderwolf.swm.clsm;

import javassist.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class NMSTransformer implements ClassFileTransformer {

    private static final Pattern PATTERN = Pattern.compile("^(\\w+)\\s*\\((.*?)\\)\\s*@(.+?\\.txt)$");
    private static final boolean DEBUG = Boolean.getBoolean("clsmDebug");

    private static final Map<String, Map<String, Change[]>> versionChanges = new HashMap<>();

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        try {
            Path modifierApiPath = Files.createTempFile("slimeworldmodifier", ".jar");
            try (final InputStream inputStream = Objects.requireNonNull(NMSTransformer.class.getResourceAsStream("/slimeworldmanager-classmodifierapi.txt"))) {
                try (final OutputStream outputStream = new FileOutputStream(modifierApiPath.toFile())) {
                    byte[] buf = new byte[8192];
                    int length;
                    while ((length = inputStream.read(buf)) > 0) {
                        outputStream.write(buf, 0, length);
                    }
                }
            }

            instrumentation.appendToSystemClassLoaderSearch(new JarFile(modifierApiPath.toFile()));
            instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(modifierApiPath.toFile()));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        instrumentation.addTransformer(new NMSTransformer());

        try (InputStream fileStream = NMSTransformer.class.getResourceAsStream("/list.yml")) {
            if (fileStream == null) {
                System.err.println("Failed to find change list.");
                System.exit(1);

                return;
            }

            Yaml yaml = new Yaml();

            try (InputStreamReader reader = new InputStreamReader(fileStream)) {
                Map<String, Object> versionData = yaml.load(reader);
                for (String version : versionData.keySet()) {
                    Map<String, Object> data = (Map<String, Object>) versionData.get(version);

                    for (String originalClazz : data.keySet()) {
                        boolean optional = originalClazz.startsWith("__optional__");
                        String clazz = originalClazz.substring(optional ? 12 : 0);

                        if (!(data.get(originalClazz) instanceof ArrayList)) {
                            System.err.println("Invalid change list for class " + clazz + ".");

                            continue;
                        }

                        List<String> changeList = (List<String>) data.get(originalClazz);
                        Change[] changeArray = new Change[changeList.size()];

                        for (int i = 0; i < changeList.size(); i++) {
                            Matcher matcher = PATTERN.matcher(changeList.get(i));

                            if (!matcher.find()) {
                                System.err.println("Invalid change '" + changeList.get(i) + "' on class " + clazz + ".");
                                System.exit(1);
                            }

                            String methodName = matcher.group(1);
                            String paramsString = matcher.group(2).trim();
                            String[] parameters;

                            if (paramsString.isEmpty()) {
                                parameters = new String[0];
                            } else {
                                parameters = matcher.group(2).split(",");
                            }

                            String location = matcher.group(3);
                            String content;

                            try (InputStream changeStream = NMSTransformer.class.getResourceAsStream("/" + location)) {
                                if (changeStream == null) {
                                    System.err.println("Failed to find data for change " + changeList.get(i) + " on class " + clazz + ".");
                                    System.exit(1);
                                }

                                byte[] contentByteArray = readAllBytes(changeStream);
                                content = new String(contentByteArray, StandardCharsets.UTF_8);
                            }

                            changeArray[i] = new Change(methodName, parameters, content, optional);
                        }

                        if (DEBUG) {
                            System.out.println("Loaded " + changeArray.length + " changes for class " + clazz + ".");
                        }

                        Map<String, Change[]> changes = versionChanges.computeIfAbsent(version, k -> new HashMap<>());

                        Change[] oldChanges = changes.get(clazz);

                        if (oldChanges == null) {
                            changes.put(clazz, changeArray);
                        } else {
                            Change[] newChanges = new Change[oldChanges.length + changeArray.length];

                            System.arraycopy(oldChanges, 0, newChanges, 0, oldChanges.length);
                            System.arraycopy(changeArray, 0, newChanges, oldChanges.length, changeArray.length);

                            changes.put(clazz, newChanges);
                        }
                    }
                }
            }
        } catch (IOException ex) {
            System.err.println("Failed to load class list.");

            ex.printStackTrace();
        }
    }

    private static byte[] readAllBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];
        int readLen;

        while ((readLen = stream.read(buffer)) != -1) {
            byteStream.write(buffer, 0, readLen);
        }

        return byteStream.toByteArray();
    }

    private String version;
    private Set<String> filesChecked = new HashSet<>();

    public @NonNull String getNMSVersion(final @NonNull String minecraftVersion) {
        return switch (minecraftVersion) {
            case "1.18.2" -> "v1_18_R2";
            case "1.19", "1.19.1" -> "v1_19_R1";
            case "1.19.2" -> "v1_19_R2";
            default -> throw new UnsupportedOperationException(minecraftVersion);
        };
    }

    @Override
    public byte[] transform(ClassLoader classLoader, String className, Class<?> classBeingTransformed, ProtectionDomain protectionDomain, byte[] bytes) {
        if (this.version == null) {
            final URL location = protectionDomain.getCodeSource().getLocation();
            if ("file".equals(location.getProtocol())) {
                final String filePath = location.getFile();
                if (filesChecked.contains(filePath)) {
                    return null;
                }

                try {
                    final ZipFile zipFile = new ZipFile(filePath);

                    final Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        final ZipEntry zipEntry = entries.nextElement();
                        if (!zipEntry.isDirectory() && "version.json".equals(zipEntry.getName())) {
                            final byte[] contents = readAllBytes(zipFile.getInputStream(zipEntry));
                            final Map<String, String> versionInfo = new Yaml().load(new String(contents, StandardCharsets.UTF_8));
                            this.version = getNMSVersion(versionInfo.get("id"));

                            this.filesChecked = null; // allow collection
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                } finally {
                    if (this.version == null) {
                        this.filesChecked.add(filePath);
                    }
                }

                if (this.version == null) {
                    return null;
                }
            } else {
                return null;
            }
        }

        final var changes = versionChanges.get(this.version);
        if (changes != null) {
            if (className != null) {
                if (changes.containsKey(className)) {
                    String fixedClassName = className.replace("/", ".");

                    if (DEBUG) {
                        System.out.println("Applying changes for class " + fixedClassName);
                    }

                    try {
                        ClassPool pool = ClassPool.getDefault();
                        pool.appendClassPath(new LoaderClassPath(classLoader));
                        pool.appendClassPath(new LoaderClassPath(NMSTransformer.class.getClassLoader()));

                        CtClass ctClass = pool.get(fixedClassName);

                        for (Change change : changes.get(className)) {
                            try {
                                CtMethod[] methods = ctClass.getDeclaredMethods(change.getMethodName());
                                boolean found = false;

                                main:
                                for (CtMethod method : methods) {
                                    CtClass[] params = method.getParameterTypes();

                                    if (params.length != change.getParams().length) {
                                        continue;
                                    }

                                    for (int i = 0; i < params.length; i++) {
                                        if (!change.getParams()[i].trim().equals(params[i].getName())) {
                                            continue main;
                                        }
                                    }

                                    found = true;

                                    try {
                                        method.insertBefore(change.getContent());
                                    } catch (CannotCompileException ex) {
                                        if (!change.isOptional()) { // If it's an optional change we can ignore it
                                            throw ex;
                                        }
                                    }

                                    break;
                                }

                                if (!found && !change.isOptional()) {
                                    throw new NotFoundException("Unknown method " + change.getMethodName());
                                }
                            } catch (CannotCompileException ex) {
                                throw new CannotCompileException("Method " + change.getMethodName(), ex);
                            }
                        }

                        return ctClass.toBytecode();
                    } catch (NotFoundException | CannotCompileException | IOException ex) {
                        System.err.println("Failed to override methods from class " + fixedClassName + ".");
                        ex.printStackTrace();
                    }
                }
            }
        }

        return null;
    }

    @Getter
    @RequiredArgsConstructor
    private static class Change {

        private final String methodName;
        private final String[] params;
        private final String content;
        private final boolean optional;


        @Override
        public String toString() {
            return "Change{" +
                    "methodName='" + methodName + '\'' +
                    ", params=" + Arrays.toString(params) +
                    ", content='" + content + '\'' +
                    ", optional=" + optional +
                    '}';
        }
    }
}