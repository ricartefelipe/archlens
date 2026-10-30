package io.archlens.analyzer.core.build;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildParsersTest {

    @Test
    void mavenResolvesLocalPropertiesModulesAndLeavesRemoteParentUnresolved() {
        String pom = """
                <project>
                  <parent>
                    <groupId>com.example</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.2.3</version>
                  </parent>
                  <artifactId>child</artifactId>
                  <properties>
                    <maven.compiler.source>1.8</maven.compiler.source>
                    <lib.version>9.9</lib.version>
                  </properties>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-dependencies</artifactId>
                        <version>3.3.0</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.acme</groupId>
                      <artifactId>lib</artifactId>
                      <version>${lib.version}</version>
                    </dependency>
                    <dependency>
                      <groupId>com.missing</groupId>
                      <artifactId>remote</artifactId>
                      <version>${not.local}</version>
                    </dependency>
                  </dependencies>
                  <modules>
                    <module>api</module>
                    <module>core</module>
                  </modules>
                </project>
                """;

        MavenPomParser.PomParseResult result = MavenPomParser.parse(pom, "pom.xml");
        assertEquals("8", result.javaVersionHint().orElseThrow());
        assertEquals(java.util.List.of("api", "core"), result.modules());
        assertTrue(result.dependencies().stream().anyMatch(dep ->
                dep.coordinates().equals("com.acme:lib:9.9") && dep.scope().equals("compile")));
        assertTrue(result.dependencies().stream().anyMatch(dep -> dep.coordinates().equals("com.missing:remote:${not.local}")));
        assertFalse(result.dependencies().stream().anyMatch(dep -> dep.coordinates().contains("spring-boot-dependencies")));
        assertTrue(result.limitations().stream().anyMatch(item -> item.code().equals("parent-not-fetched")));
        assertTrue(result.limitations().stream().anyMatch(item -> item.code().equals("bom-not-resolved")));
        assertTrue(result.limitations().stream().anyMatch(item -> item.code().equals("unresolved-property")));
        assertTrue(result.limitations().stream().anyMatch(item -> item.code().equals("dependency-management-not-declared")));
    }

    @Test
    void mavenRejectsDoctypeWithoutReadingExternalEntity() {
        String pom = """
                <?xml version="1.0"?>
                <!DOCTYPE project [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <project><name>&xxe;</name></project>
                """;
        MavenPomParser.PomParseResult result = MavenPomParser.parse(pom, "pom.xml");
        assertTrue(result.dependencies().isEmpty());
        String rendered = result.toString();
        assertFalse(rendered.contains("root:"));
        assertFalse(rendered.contains("/etc/passwd"));
        assertTrue(result.limitations().stream().anyMatch(item -> item.code().equals("pom-unreadable")));
    }

    @Test
    void samplePomMatchesDeclaredDependencies() throws Exception {
        String pom = Files.readString(Path.of("../../samples/legacy-java-demo/pom.xml"));
        MavenPomParser.PomParseResult result = MavenPomParser.parse(pom, "pom.xml");
        assertEquals("8", result.javaVersionHint().orElseThrow());
        assertEquals(5, result.dependencies().size());
        assertTrue(result.dependencies().stream().anyMatch(dep ->
                dep.coordinates().equals("javax:javaee-api:8.0.1") && dep.scope().equals("provided")));
        assertTrue(result.dependencies().stream().anyMatch(dep ->
                dep.coordinates().equals("javax.persistence:javax.persistence-api:2.2") && dep.scope().equals("compile")));
    }

    @Test
    void gradleDoesNotExecuteAndReportsUnsupportedCatalog() {
        String build = """
                // implementation 'evil:commented:1.0'
                plugins { id 'java' }
                java {
                  sourceCompatibility = '1.8'
                }
                dependencies {
                  implementation 'com.acme:real:1.2.0'
                  implementation libs.spring.boot
                  testImplementation project(':api')
                  /* implementation 'hidden:block:1' */
                }
                """;
        GradleBuildParser.GradleParseResult result = GradleBuildParser.parse(build, "build.gradle");
        assertEquals("8", result.javaVersionHint().orElseThrow());
        assertEquals("LOW", result.confidence());
        assertEquals(1, result.dependencies().size());
        assertEquals("com.acme:real:1.2.0", result.dependencies().getFirst().coordinates());
        assertEquals("implementation", result.dependencies().getFirst().scope());
        assertTrue(result.modules().contains("api"));
        assertTrue(result.limitations().stream().anyMatch(item -> item.code().equals("gradle-version-catalog")));
        assertTrue(result.limitations().stream().anyMatch(item -> item.code().equals("gradle-project-dependency")));
        assertFalse(result.dependencies().stream().anyMatch(dep -> dep.coordinates().contains("evil")));
        assertFalse(result.dependencies().stream().anyMatch(dep -> dep.coordinates().contains("hidden")));
    }

    @Test
    void settingsGradleListsIncludedModules() {
        GradleBuildParser.GradleParseResult result = GradleBuildParser.parse(
                "include 'api', 'core'\n",
                "settings.gradle"
        );
        assertEquals(java.util.List.of("api", "core"), result.modules().stream().sorted().toList());
    }
}
