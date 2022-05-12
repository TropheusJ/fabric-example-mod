package io.github.tropheusj.auto_maintainer.updatables.builtin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.github.tropheusj.auto_maintainer.Config;
import io.github.tropheusj.auto_maintainer.Util;

import io.github.tropheusj.auto_maintainer.updatables.UpdateRequirement;

import org.gradle.api.Project;

import java.util.Properties;

/**
 * An Updatable for any mod found on Modrinth.
 */
public class ModrinthUpdatable extends GradlePropertiesBasedUpdatable {
	private final String projectId;
	private final UpdateRequirement type;

	public ModrinthUpdatable(String name, String projectId, String gradlePropertiesKey, UpdateRequirement type) {
		super(name, gradlePropertiesKey);
		this.projectId = projectId;
		this.type = type;
	}

	@Override
	public void initialize(Project project, Properties properties, Config config) {
		super.initialize(project, properties, config);
		String mcVer = Util.getMcVer(config);
		newVersion = findLatestFromMcVer(mcVer, projectId);
	}

	@Override
	public UpdateRequirement updateType() {
		return type;
	}

	public static String findLatestFromMcVer(String mcVer, String projectId) {
		JsonArray allVersions = Util.jsonFromUrl("https://api.modrinth.com/v2/project/" + projectId + "/version").getAsJsonArray();
		for (JsonElement element : allVersions) {
			JsonObject version = element.getAsJsonObject();

			if (!supportsLoader(version, false)) // todo: quilt
				continue;
			JsonArray supportedMcVersions = version.getAsJsonArray("game_versions");
			for (JsonElement supported : supportedMcVersions) {
				String asString = supported.getAsString();
				if (asString.equals(mcVer)) {
					return version.get("version_number").getAsString();
				}
			}
		}
		return null;
	}

	public static boolean supportsLoader(JsonObject modVersion, boolean quilt) {
		JsonArray loaders = modVersion.getAsJsonArray("loaders");
		for (JsonElement loaderElement : loaders) {
			String loader = loaderElement.getAsString();
			if (loader.equals("fabric") || (quilt && loader.equals("quilt"))) {
				return true;
			}
		}
		return false;
	}
}
