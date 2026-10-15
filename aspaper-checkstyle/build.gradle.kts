plugins {
    java
    id("io.papermc.paperweight.paper-checkstyle")
}

val testData = sourceSets.create("testData")

tasks.named<io.papermc.paperweight.checkstyle.tasks.MergeCheckstyleConfigs>("mergeCheckstyleConfigs") {
    overrideConfigFile.set(layout.projectDirectory.file("../paper-checkstyle/.checkstyle/checkstyle.xml"))
}

sourceSets {
    main {
        java.srcDir(file("../paper-checkstyle/src/main/java"))
        resources.srcDir(file("../paper-checkstyle/src/main/resources"))
    }
    test {
        java.srcDir(file("../paper-checkstyle/src/test/java"))
        resources.srcDir(file("../paper-checkstyle/src/test/resources"))
    }
}

testData.java.srcDir(file("../paper-checkstyle/src/testData/java"))
testData.resources.srcDir(file("../paper-checkstyle/src/testData/resources"))

dependencies {
    implementation("com.puppycrawl.tools:checkstyle:13.8.0")
    implementation("org.jspecify:jspecify:1.0.0")

    testCompileOnly("org.jetbrains:annotations:26.1.0")
    testImplementation(testData.output)

    testImplementation("org.junit.jupiter:junit-jupiter:5.13.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testData.implementationConfigurationName("org.jspecify:jspecify:1.0.0")

    checkstyle(project(":aspaper-checkstyle"))
}

tasks {
    test {
        workingDir = file("../paper-checkstyle")
        useJUnitPlatform()
    }
}
