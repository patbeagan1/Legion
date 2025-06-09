import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    val kotlinVersion = "2.1.21"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("com.vanniktech.maven.publish") version "0.32.0"
}

group = "io.github.patbeagan1"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

mavenPublishing {
//    publishToMavenCentral(SonatypeHost.DEFAULT)
    // or when publishing to https://s01.oss.sonatype.org
//    publishToMavenCentral(SonatypeHost.S01)
    // or when publishing to https://central.sonatype.com/
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    coordinates("io.github.patbeagan1", "legion", version as String?)

    pom {
        name.set("Legion")
        description.set("Asynchronous typesafe task graph")
        inceptionYear.set("2025")
        url.set("https://github.com/patbeagan1/Legion/")
        licenses {
            license {
                name.set("MIT")
//                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
//                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("patbeagan1")
                name.set("patbeagan1")
                url.set("https://github.com/patbeagan1/")
            }
        }
        scm {
            url.set("https://github.com/patbeagan1/Legion/")
            connection.set("scm:git:git://github.com/patbeagan1/Legion.git")
            developerConnection.set("scm:git:ssh://git@github.com/patbeagan1/Legion.git")
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.google.code.gson:gson:2.10.1")

    //coroutines
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.21")

    testImplementation(kotlin("test"))
    // junit
    // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    depKtor()
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    compilerOptions.freeCompilerArgs.add("-Xcontext-receivers")
}

fun DependencyHandlerScope.depKtor() {
    val ktorVersion = "3.1.3"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
}
