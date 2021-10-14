plugins {
	kotlin("jvm") version "1.5.10"
	id("org.jetbrains.intellij") version "1.2.0"
	java
}

group = "com.chylex.intellij.keyboardmaster"
version = "0.1.4"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

intellij {
    version.set("2021.2.2")
	
	if (System.getenv("IDEAVIM") == "1") {
		plugins.add("IdeaVIM:0.66")
	}
}
