plugins {
    kotlin("multiplatform") version "2.1.0"
}

group = "me.user"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    js(IR) {
        binaries.executable()
        browser {
            commonWebpackConfig {
                devServer = devServer?.copy(
                    open = false // verhindert das automatische Öffnen des Browsers
                )
            }
            testTask {
                useKarma {
                    useFirefox()
                    // useSafari() // to use Safari instead of firefox
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.ionspin.kotlin:bignum:0.3.10")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test")) // This brings all the platform dependencies automatically
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            }
        }
    }

    tasks.register("generateKaitaiManifest") {
        val kaitaiDir = file("src/commonMain/resources/kaitai")
        val outputDir = file("${layout.buildDirectory.get()}/processedResources/js/main")
        val manifestFile = outputDir.resolve("kaitai-manifest.txt")

        inputs.dir(kaitaiDir)
        outputs.file(manifestFile)

        doLast {
            val files = kaitaiDir.listFiles()?.filter { it.extension == "ksy" }?.map { it.nameWithoutExtension } ?: emptyList()
            val textContent = buildString {
                files.forEach { fileName ->
                    appendLine(fileName)
                }
            }
            manifestFile.writeText(textContent)
            println("Kaitai manifest generated at ${manifestFile.absolutePath}")
        }
    }

    tasks.named("jsProcessResources") {
        dependsOn("generateKaitaiManifest")
    }
}

afterEvaluate {
    rootProject.extensions.configure<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension> {
        versions.webpackCli.version="4.10.0"
    }
}