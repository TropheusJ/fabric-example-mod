plugins {
	alias(libs.plugins.loom)
	alias(libs.plugins.maven)
}

base.archivesName = "modid"
group = "io.github.tropheusj"

val buildNum = providers.environmentVariable("GITHUB_RUN_NUMBER")
    .filter(String::isNotEmpty)
	.map { "build.$it" }
    .orElse("local")
    .get()

version = "0.1.0+$buildNum-mc${libs.versions.minecraft.get()}"

repositories {
	exclusiveContent {
        forRepositories(maven("https://api.modrinth.com/maven")).filter {
            includeGroup("maven.modrinth")
        }
    }
}

val testmod: SourceSet by sourceSets.creating
val testmodImplementation: Configuration by configurations.getting

dependencies {
	minecraft(libs.minecraft)
	mappings(loom.officialMojangMappings())

	modImplementation(libs.bundles.fabric)
    modLocalRuntime(libs.bundles.dev)

    testmodImplementation(project(":", configuration = "namedElements"))
}

configurations {
    getByName("testmodCompileClasspath").extendsFrom(compileClasspath.get())
    getByName("testmodRuntimeClasspath").extendsFrom(runtimeClasspath.get())
}

tasks.processResources {
	val properties: Map<String, Any> = mapOf(
		"version" to version,
        "minecraft_version" to libs.versions.minecraft.get(),
		"loader_version" to libs.versions.loader.get(),
		"fapi_version" to libs.versions.fapi.get()
	)

	inputs.properties(properties)

	filesMatching("fabric.mod.json") {
		expand(properties)
	}
}

loom {
	runs {
		register("testmodClient") {
			client()
			name("Testmod Client")
			source(testmod)
		}
		register("testmodServer") {
			server()
			name("Testmod Server")
			source(testmod)
		}
		register("gametest") {
			server()
			source(testmod)
            ideConfigGenerated(false) // this is meant for CI
            property("fabric-api.gametest")
            property("fabric-api.gametest.report-file=${layout.buildDirectory}/junit.xml")
			runDir("run/gametest_server")
		}

        configureEach {
            property("mixin.debug.export", "true")
        }
	}
}

java {
	withSourcesJar()
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	repositories {
        listOf("Releases", "Snapshots").forEach {
            maven("https://mvn.devos.one/${it.lowercase()}") {
                name = "devOs$it"
                credentials(PasswordCredentials::class)
            }
        }
	}
}
