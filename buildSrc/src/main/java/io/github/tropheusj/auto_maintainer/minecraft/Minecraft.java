package io.github.tropheusj.auto_maintainer.minecraft;

import io.github.tropheusj.auto_maintainer.Config;
import io.github.tropheusj.auto_maintainer.Util;

import org.gradle.api.Project;

import java.util.Properties;
import java.util.regex.Pattern;

public class Minecraft {
	public static final Pattern SNAPSHOT = Pattern.compile("([0-9]){2}(w)([0-9]){2}([a-z])");
	public static final Pattern RELEASE = Pattern.compile("([0-9])(\\.)([0-9]){1,2}(\\.)([0-9]){1,2}");
	public static final String GRADLE_PROPERTIES_MC_VER_KEY = "minecraft_version";

	public static Minecraft INSTANCE = null;

	public final Versions versions;
	public final Manifest manifest;

	private Minecraft(Config config, Project project) {
		manifest = new Manifest();
		String oldVersion = currentVersionFromProperties(Util.getGradleProperties(project));
		String newVersion = manifest.latestVersionId;
		versions = new Versions(oldVersion, newVersion);
	}

	public static void init(Config config, Project project) {
		if (INSTANCE != null)
			return;
		INSTANCE = new Minecraft(config, project);
	}

	private String currentVersionFromProperties(Properties properties) {
		return properties.getProperty(GRADLE_PROPERTIES_MC_VER_KEY);
	}
}
