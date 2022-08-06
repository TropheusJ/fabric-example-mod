package io.github.tropheusj.auto_maintainer.updatables.builtin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.tropheusj.auto_maintainer.Config;
import io.github.tropheusj.auto_maintainer.Util;

import org.gradle.api.Project;

import javax.annotation.Nullable;

import java.util.Properties;
import java.util.regex.Pattern;

public class MinecraftUpdatable extends GradlePropertiesBasedUpdatable {
	public static final String UPDATABLE_KEY = "Minecraft";
	public static final String MC_KEY = "minecraft_version";
	public static final Pattern SNAPSHOT = Pattern.compile("([0-9]){2}(w)([0-9]){2}([a-z])");
	public static final Pattern RELEASE = Pattern.compile("([0-9])(\\.)([0-9]){1,2}(\\.)([0-9]){1,2}");

	private boolean needsUpdate;
	private String targetVersion; // the target for snapshots, ex. 22w18a -> 1.19. for releases, is identical to newVersion.

	public MinecraftUpdatable() {
		super("Minecraft", MC_KEY);
	}

	@Override
	public void initialize(Project project, Properties properties, Config config) {
		super.initialize(project, properties, config);
		JsonObject versionManifest = grabManifest();
		JsonArray versions = versionManifest.getAsJsonArray("versions");
		JsonObject latestEntry = versions.get(0).getAsJsonObject();
		newVersion = latestEntry.get("id").getAsString();
		int currentIndex = findIndexOfVersion(currentVersion, versions);
		if (currentIndex == -1)
			throw new RuntimeException("Current MC version [" + currentVersion + "] could not be found in the patch notes!");
		needsUpdate = currentIndex != 0;

		String dataUrl = latestEntry.get("url").getAsString();
		JsonObject data = Util.jsonFromUrl(dataUrl).getAsJsonObject();
		targetVersion = data.get("assets").getAsString();
	}

	@Nullable
	@Override
	public String updateVersion() {
		return needsUpdate ? newVersion : currentVersion;
	}

	public String targetVersion() {
		return targetVersion;
	}

	@Override
	public boolean hasUpdate() {
		return needsUpdate;
	}

	public static JsonObject grabManifest() {
		return Util.jsonFromUrl("http://launchermeta.mojang.com/mc/game/version_manifest.json").getAsJsonObject();
	}

	public static int findIndexOfVersion(String mcVer, JsonArray entries) {
		for (int i = 0; i < entries.size(); i++) {
			JsonObject entry = entries.get(i).getAsJsonObject();
			String version = entry.get("id").getAsString();
			if (mcVer.equals(version)) {
				return i;
			}
		}
		return -1;
	}
}
