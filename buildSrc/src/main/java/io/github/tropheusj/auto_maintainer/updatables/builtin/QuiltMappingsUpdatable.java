package io.github.tropheusj.auto_maintainer.updatables.builtin;

import com.google.gson.JsonArray;

import com.google.gson.JsonElement;

import com.google.gson.JsonObject;

import io.github.tropheusj.auto_maintainer.Config;
import io.github.tropheusj.auto_maintainer.Util;
import io.github.tropheusj.auto_maintainer.updatables.Updatable;

import org.gradle.api.Project;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class QuiltMappingsUpdatable implements Updatable {
	public static final String QM_KEY = "qm_build";
	public static final String QM_META = "https://meta.quiltmc.org/v3/versions/quilt-mappings";

	private int currentBuild;
	private int newBuild;

	@Override
	public void initialize(Project project, Properties properties, Config config) {
		currentBuild = Integer.parseInt(properties.getProperty(QM_KEY));
		String mcVer = config.getUpdatables().get(MinecraftUpdatable.UPDATABLE_KEY).updateVersion();
		newBuild = findLatestBuild(mcVer);
	}

	public static int findLatestBuild(String mcVer) {
		JsonArray allVersions = Util.jsonFromUrl(QM_META).getAsJsonArray();
		List<JsonObject> candidates = new LinkedList<>();
		for (JsonElement element : allVersions) {
			JsonObject version = element.getAsJsonObject();
			String supportedMc = version.get("gameVersion").getAsString();
			if (supportedMc.equals(mcVer))
				candidates.add(version);
		}
		if (candidates.isEmpty())
			throw new RuntimeException("Could not find valid QM version for MC version [" + mcVer + "]!");
		candidates.sort((c1, c2) -> {
			int build1 = c1.get("build").getAsInt();
			int build2 = c2.get("build").getAsInt();
			return -Integer.compare(build1, build2); // negate: bigger numbers earlier in list
		});
		return candidates.get(0).get("build").getAsInt();
	}

	@Override
	public boolean hasUpdate() {
		return currentBuild != newBuild;
	}

	@Override
	public void update(Project project, Properties properties) {
		String build = String.valueOf(newBuild);
		properties.setProperty(QM_KEY, build);
	}

	@Override
	public String currentVersion() {
		return String.valueOf(currentBuild);
	}

	@Override
	public String updateVersion() {
		return String.valueOf(newBuild);
	}
}
