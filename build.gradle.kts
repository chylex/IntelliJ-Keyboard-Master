import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	java
	kotlin("jvm") version "1.5.10"
	id("org.jetbrains.intellij") version "1.2.0"
}

group = "com.chylex.intellij.keyboardmaster"
version = "0.1.5"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

intellij {
    version.set("2021.2.2")
	updateSinceUntilBuild.set(false)
	
	if (System.getenv("IDEAVIM") == "1") {
		plugins.add("IdeaVIM:0.66")
	}
}

tasks.withType<KotlinCompile> {
	kotlinOptions.jvmTarget = "11"
}
