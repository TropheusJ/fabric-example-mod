package io.github.tropheusj.auto_maintainer.updatables.builtin;

import com.google.gson.JsonArray;

import com.google.gson.JsonElement;

import com.google.gson.JsonObject;

import io.github.tropheusj.auto_maintainer.Config;
import io.github.tropheusj.auto_maintainer.Util;

import org.gradle.api.Project;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class QuiltMappingsUpdatable extends GradlePropertiesBasedUpdatable {
	public static final String QM_KEY = "qm_build";
	public static final String QM_META = "https://meta.quiltmc.org/v3/versions/quilt-mappings";

	public QuiltMappingsUpdatable() {
		super("Quilt Mappings", QM_KEY);
	}

	@Override
	public void initialize(Project project, Properties properties, Config config) {
		super.initialize(project, properties, config);
		String mcVer = Util.getMcVer();
		newVersion = String.valueOf(findLatestBuild(mcVer));
	}

	@Override
	public boolean currentVersionFine() {
		return true; // if no supported version is found, an exception is thrown by findLatestBuild, so this is always fine
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
}
