@file:Suppress("ConvertLambdaToReference")

plugins {
	kotlin("jvm")
	id("org.jetbrains.intellij.platform")
}

group = "com.chylex.intellij.keyboardmaster"
version = "0.6.2"

repositories {
	mavenCentral()
	
	intellijPlatform {
		defaultRepositories()
	}
}

dependencies {
	intellijPlatform {
		@Suppress("DEPRECATION")
		intellijIdeaUltimate("2024.2")
		
		bundledPlugin("com.intellij.java")
		
		if (System.getenv("IDEAVIM") == "1") {
			plugin("IdeaVIM", "2.10.2")
		}
	}
	
	testImplementation("org.junit.jupiter:junit-jupiter:5.11.0-M1")
}

intellijPlatform {
	pluginConfiguration {
		ideaVersion {
			sinceBuild.set("242")
			untilBuild.set(provider { null })
		}
	}
}

kotlin {
	jvmToolchain(21)
	
	compilerOptions {
		freeCompilerArgs = listOf(
			"-X" + "jvm-default=all",
			"-X" + "lambdas=indy"
		)
	}
}

tasks.test {
	useJUnitPlatform()
}
