package io.github.tropheusj.auto_maintainer.updatables.builtin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.tropheusj.auto_maintainer.Config;
import io.github.tropheusj.auto_maintainer.Util;
import io.github.tropheusj.auto_maintainer.updatables.Updatable;

import org.gradle.api.Project;

import javax.annotation.Nullable;

import java.util.Properties;
import java.util.regex.Pattern;

public class MinecraftUpdatable implements Updatable {
	public static final String UPDATABLE_KEY = "Minecraft";
	public static final String MC_KEY = "minecraft_version";
	public static final Pattern SNAPSHOT = Pattern.compile("([0-9]){2}(w)([0-9]){2}([a-z])");
	public static final Pattern RELEASE = Pattern.compile("([0-9])(\\.)([0-9]){1,2}(\\.)([0-9]){1,2}");
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private JsonObject minecraftPatchNotes;
	private JsonArray entries;
	private String currentVersion;
	private String newVersion;
	private boolean needsUpdate;

	@Override
	public void initialize(Project project, Properties properties, Config config) {
		currentVersion = properties.getProperty(MC_KEY);
		minecraftPatchNotes = grabPatchNotes();
		entries = minecraftPatchNotes.getAsJsonArray("entries");
		JsonObject latestEntry = entries.get(0).getAsJsonObject();
		newVersion = latestEntry.get("version").getAsString();
		int currentIndex = findIndexOfVersion(currentVersion, entries);
		if (currentIndex == -1)
			throw new RuntimeException("Current MC version [" + currentVersion + "] could not be found in the patch notes!");
		needsUpdate = currentIndex != 0;
	}

	@Override
	public String currentVersion() {
		return currentVersion;
	}

	@Nullable
	@Override
	public String updateVersion() {
		return needsUpdate ? newVersion : currentVersion;
	}

	@Override
	public boolean hasUpdate() {
		return needsUpdate;
	}

	@Override
	public void update(Project project, Properties properties) {
		properties.setProperty(MC_KEY, newVersion);
	}

	public static JsonObject grabPatchNotes() {
		return Util.jsonFromUrl("https://launchercontent.mojang.com/javaPatchNotes.json").getAsJsonObject();
	}

	public static int findIndexOfVersion(String mcVer, JsonArray entries) {
		for (int i = 0; i < entries.size(); i++) {
			JsonObject entry = entries.get(i).getAsJsonObject();
			String version = entry.get("version").getAsString();
			if (mcVer.equals(version)) {
				return i;
			}
		}
		return -1;
	}
}
