package io.github.tropheusj.auto_maintainer.updatables.builtin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.tropheusj.auto_maintainer.Config;
import io.github.tropheusj.auto_maintainer.Util;
import io.github.tropheusj.auto_maintainer.updatables.Updatable;

import io.github.tropheusj.auto_maintainer.updatables.UpdateType;

import org.gradle.api.Project;

import javax.annotation.Nullable;

import java.util.Properties;

/**
 * An Updatable for any mod found on Modrinth.
 */
public class ModrinthUpdatable implements Updatable {
	private final String name;
	private final String projectId;
	private final String gradlePropertiesKey;
	private final UpdateType type;
	private String currentVersion;
	private String newVersion;

	public ModrinthUpdatable(String name, String projectId, String gradlePropertiesKey, UpdateType type) {
		this.name = name;
		this.projectId = projectId;
		this.gradlePropertiesKey = gradlePropertiesKey;
		this.type = type;
	}

	@Override
	public void initialize(Project project, Properties properties, Config config) {
		currentVersion = properties.getProperty(gradlePropertiesKey);
		if (currentVersion == null) {
			throw new RuntimeException(String.format(
					"'%s' could not find it's current version; Expected a key in gradle.properties matching '%s'\n",
					name, gradlePropertiesKey
			));
		}
		Updatable mc = config.getUpdatables().get(MinecraftUpdatable.UPDATABLE_KEY);
		String mcVer = mc.updateVersion();
		newVersion = findLatestFromMcVer(mcVer, projectId);
	}

	@Override
	public boolean hasUpdate() {
		return newVersion != null && !currentVersion.equals(newVersion);
	}

	@Override
	public UpdateType updateType() {
		return type;
	}

	@Override
	public void update(Project project, Properties properties) {
		properties.setProperty(gradlePropertiesKey, newVersion);
	}

	@Override
	public String currentVersion() {
		return currentVersion;
	}

	@Nullable
	@Override
	public String updateVersion() {
		return newVersion;
	}

	public static String findLatestFromMcVer(String mcVer, String projectId) {
		JsonArray allVersions = Util.jsonFromUrl("https://api.modrinth.com/v2/project/" + projectId + "/version").getAsJsonArray();
		boolean unstableAllowed = !MinecraftUpdatable.RELEASE.matcher(mcVer).find();
		for (JsonElement element : allVersions) {
			JsonObject version = element.getAsJsonObject();
			JsonArray supportedVersions = version.getAsJsonArray("game_versions");
			String type = version.get("version_type").getAsString();
			if (!unstableAllowed && !type.equals("release"))
				continue;
			for (JsonElement supported : supportedVersions) {
				String asString = supported.getAsString();
				if (asString.equals(mcVer)) {
					return version.get("version_number").getAsString();
				}
			}
		}
		return null;
	}
}
