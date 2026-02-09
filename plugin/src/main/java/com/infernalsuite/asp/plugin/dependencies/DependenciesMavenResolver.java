package com.infernalsuite.asp.plugin.dependencies;

import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

public class DependenciesMavenResolver {

    private final MavenLibraryResolver resolver = new MavenLibraryResolver();

    public void addRepositories() {
        this.resolver.addRepository(new RemoteRepository.Builder(
                "central",
                "default",
                MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR
        ).build());

        this.resolver.addRepository(new RemoteRepository.Builder(
                "is-releases",
                "default",
                "https://repo.infernalsuite.com/repository/maven-releases/"
        ).build());
    }

    public void addDependency(String groupId, String artifactId, String version) {
        Artifact artifact = new DefaultArtifact(groupId, artifactId, "jar", version);
        this.resolver.addDependency(new Dependency(artifact, null));
    }

    public MavenLibraryResolver getResolver() {
        return resolver;
    }
}
