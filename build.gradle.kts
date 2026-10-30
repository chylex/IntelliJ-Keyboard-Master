@file:Suppress("ConvertLambdaToReference")

plugins {
	kotlin("jvm")
	id("org.jetbrains.intellij.platform")
}

group = "com.chylex.intellij.keyboardmaster"
version = "0.6.11"

repositories {
	mavenCentral()
	
	intellijPlatform {
		defaultRepositories()
	}
}

dependencies {
	intellijPlatform {
		@Suppress("DEPRECATION")
		intellijIdeaUltimate("2026.2")
		
		bundledModule("intellij.platform.vcs.impl")
		bundledModule("intellij.platform.vcs.log.impl")
		
		bundledPlugin("com.intellij.java")
		bundledPlugin("Git4Idea")
		
		compatiblePlugin("com.intellij.classic.ui")
		
		if (System.getenv("IDEAVIM") == "1") {
			plugin("IdeaVIM", "2.10.2")
		}
	}
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
