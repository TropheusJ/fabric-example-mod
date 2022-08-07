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
	private boolean currentVersionFine;

	public ModrinthUpdatable(String name, String projectId, String gradlePropertiesKey, UpdateRequirement type) {
		super(name, gradlePropertiesKey);
		this.projectId = projectId;
		this.type = type;
	}

	@Override
	public void initialize(Project project, Properties properties, Config config) {
		super.initialize(project, properties, config);
		String mcVer = Util.getMcVer();
		JsonArray allVersions = Util.jsonFromUrl("https://api.modrinth.com/v2/project/" + projectId + "/version").getAsJsonArray();
		checkNewVersion(allVersions, mcVer, config.allowUnstableUpdates());
		checkCurrentVersionFine(allVersions, mcVer);
	}

	@Override
	public boolean currentVersionFine() {
		return currentVersionFine;
	}

	@Override
	public UpdateRequirement updateType() {
		return type;
	}

	public void checkNewVersion(JsonArray allVersions, String mcVer, boolean allowUnstable) {
		versions: for (JsonElement element : allVersions) {
			JsonObject version = element.getAsJsonObject();
			String versionType = version.get("version_type").getAsString();
			if (!allowUnstable && !"release".equals(versionType))
				continue;
			if (!supportsLoader(version, false)) // todo: quilt
				continue;
			JsonArray supportedMcVersions = version.getAsJsonArray("game_versions");
			for (JsonElement supported : supportedMcVersions) {
				String asString = supported.getAsString();
				if (asString.equals(mcVer)) {
					newVersion = version.get("version_number").getAsString();
					break versions; // exit early - we've found the newest version that works
				}
			}
		}
	}

	public void checkCurrentVersionFine(JsonArray allVersions, String mcVer) {
		if (!hasUpdate()) {
			// did not find an update - find current version, see if it supports the target MC version
			versions: for (JsonElement element : allVersions) {
				JsonObject version = element.getAsJsonObject();
				if (!supportsLoader(version, false)) // todo: quilt
					continue;
				if (!currentVersion.equals(version.get("version_number").getAsString()))
					continue;
				JsonArray supportedMcVersions = version.getAsJsonArray("game_versions");
				for (JsonElement supported : supportedMcVersions) {
					String asString = supported.getAsString();
					if (asString.equals(mcVer)) {
						currentVersionFine = true;
						break versions;
					}
				}
			}
		}
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
