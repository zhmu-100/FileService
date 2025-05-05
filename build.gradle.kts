plugins {
  kotlin("jvm") version "1.8.0"
  kotlin("plugin.serialization") version "1.8.10"
  id("io.ktor.plugin") version "2.2.4"
  application
  id("com.ncorti.ktfmt.gradle") version "0.11.0"
  jacoco
  id("org.jetbrains.dokka") version "1.9.20"
}

group = "org.clickhouse"
version = "1.0.0"

repositories {
  mavenCentral()
	maven { url = uri("https://jitpack.io") }
}

dependencies {
  implementation(kotlin("stdlib"))
	implementation("com.github.poplopok:Logger:1.0.6")

  implementation("io.ktor:ktor-server-core-jvm:2.2.4")
  implementation("io.ktor:ktor-server-netty-jvm:2.2.4")
  implementation("ch.qos.logback:logback-classic:1.2.11")
  implementation("io.ktor:ktor-server-status-pages:2.2.4")
  implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")


  implementation("io.ktor:ktor-server-content-negotiation-jvm:2.2.4")
  implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.2.4")

  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
  implementation("io.minio:minio:8.5.17")


  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("io.ktor:ktor-server-tests-jvm:2.2.4")
  testImplementation("io.mockk:mockk:1.13.5")
  testImplementation("io.mockk:mockk-agent-jvm:1.13.5") 
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")

}

tasks.test {
  useJUnitPlatform()
}

configure<JacocoPluginExtension> {
  toolVersion = "0.8.8"
}

tasks.named<JacocoReport>("jacocoTestReport") {
  classDirectories.setFrom(
    files(classDirectories.files.map {
      fileTree(it) {
        exclude(
          "org/fileservice/ApiServerKt.class"
        )
      }
    })
  )
  reports {
    html.required.set(true)
    xml.required.set(false)
    csv.required.set(false)
  }
}



tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
  classDirectories.setFrom(
    files(classDirectories.files.map {
      fileTree(it) {
        exclude(
          "org/fileservice/ApiServerKt.class"
        )
      }
    })
  )
  violationRules {
    rule {
      limit {
        minimum = 0.20.toBigDecimal()
        counter = "LINE"
      }
    }
  }
}


tasks.jar {
  manifest {
    attributes["Main-Class"] = application.mainClass.get()
  }
  duplicatesStrategy = DuplicatesStrategy.INCLUDE
  from({
    configurations
      .runtimeClasspath
      .get()
      .filter { it.name.endsWith(".jar") }
      .map { zipTree(it) }
  })
}

application {
  mainClass.set("org.example.AppKt")
}


tasks.check {
  dependsOn(tasks.named("jacocoTestCoverageVerification"))
}

application {
  mainClass.set("org.fileservice.ApiServerKt")
}

kotlin {
  jvmToolchain(17)
}
