import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `java-library`
    `maven-publish`
    signing
}

group = "net.t1y"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.okhttp)

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.0.21")
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// ===== Publishing =====

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.getByName("javadoc", Javadoc::class).destinationDir)
}

tasks.withType<Javadoc> {
    // Suppress Javadoc warnings for Kotlin-generated Java stubs
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:none", "-quiet")
        addStringOption("encoding", "UTF-8")
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])

            artifact(sourcesJar)
            artifact(javadocJar)

            groupId = "net.t1y"
            artifactId = "t1y-sdk"
            version = "1.0.0"

            pom {
                name.set("t1yOS SDK for Kotlin/Android")
                description.set(
                    "t1yOS Serverless Platform Kotlin/Android SDK — " +
                    "cloud database, metadata, and cloud functions client"
                )
                url.set("https://github.com/t1yOS/t1y-sdk-android")

                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("t1yos")
                        name.set("华易云联（杭州）网络科技有限责任公司")
                        email.set("wwwanghua@outlook.com")
                        organization.set("华易云联")
                        organizationUrl.set("https://www.t1y.net")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/t1yOS/t1y-sdk-android.git")
                    developerConnection.set("scm:git:ssh://github.com/t1yOS/t1y-sdk-android.git")
                    url.set("https://github.com/t1yOS/t1y-sdk-android")
                }
            }
        }
    }

    repositories {
        maven {
            name = "localDir"
            url = uri(layout.buildDirectory.dir("repo")) 
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["release"])
}
