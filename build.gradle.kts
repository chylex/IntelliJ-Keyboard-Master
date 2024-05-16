@file:Suppress("ConvertLambdaToReference")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "1.9.22"
	id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.chylex.intellij.keyboardmaster"
version = "0.5.4"

repositories {
	mavenCentral()
}

intellij {
	type.set("IU")
	version.set("2024.1.1")
	updateSinceUntilBuild.set(false)
	
	plugins.add("com.intellij.java")
	
	if (System.getenv("IDEAVIM") == "1") {
		plugins.add("IdeaVIM:2.10.2")
	}
}

kotlin {
	jvmToolchain(17)
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter:5.11.0-M1")
}

tasks.patchPluginXml {
	sinceBuild.set("241")
}

tasks.test {
	useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
	kotlinOptions.freeCompilerArgs = listOf(
		"-Xjvm-default=all"
	)
}
