@file:Suppress("ConvertLambdaToReference")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "1.8.0"
	id("org.jetbrains.intellij") version "1.15.0"
}

group = "com.chylex.intellij.keyboardmaster"
version = "0.4.1"

repositories {
	mavenCentral()
}

intellij {
	type.set("IU")
	version.set("233.11799-EAP-CANDIDATE-SNAPSHOT")
	updateSinceUntilBuild.set(false)
	
	plugins.add("com.intellij.java")
	
	if (System.getenv("IDEAVIM") == "1") {
		plugins.add("IdeaVIM:0.66")
	}
}

kotlin {
	jvmToolchain(17)
}

dependencies {
	testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

tasks.patchPluginXml {
	sinceBuild.set("233.11799.30")
}

tasks.test {
	useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
	kotlinOptions.freeCompilerArgs = listOf(
		"-Xjvm-default=all"
	)
}
