plugins {
	id("java")
	id("application")
	id("com.gradleup.shadow") version "8.3.3"
}

group = "com.melluh"
version = "1.0.0"

java {
	toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
	mavenLocal()
	mavenCentral()
}

dependencies {
	implementation("org.tinylog:tinylog-api:2.4.1")
	implementation("org.tinylog:tinylog-impl:2.4.1")
	implementation("com.google.guava:guava:33.4.8-jre")

	implementation("com.melluh:simple-http-server:1.1.0")
	implementation("org.xerial:sqlite-jdbc:3.36.0.3")
	implementation("com.grack:nanojson:1.7")
}

application {
	mainClass = "com.melluh.rtsprecorder.RtspRecorder"
}

tasks {
	assemble {
		dependsOn(shadowJar)
	}
}