package io.github.tropheusj.auto_maintainer.updatables.builtin;

import com.google.gson.JsonArray;

import com.google.gson.JsonObject;

import io.github.tropheusj.auto_maintainer.Config;
import io.github.tropheusj.auto_maintainer.Util;

import io.github.tropheusj.auto_maintainer.updatables.UpdateRequirement;

import org.gradle.api.Project;

import java.util.Properties;

public class FabricLoaderUpdatable extends GradlePropertiesBasedUpdatable {
	public static final String PROPERTY_KEY = "loader_version";
	public static final String LOADER_META = "https://meta.fabricmc.net/v2/versions/loader/";

	public FabricLoaderUpdatable() {
		super("Fabric Loader", PROPERTY_KEY);
	}

	@Override
	public void initialize(Project project, Properties properties, Config config) {
		super.initialize(project, properties, config);
		String mcVer = Util.getMcVer();
		newVersion = getNewVersion(mcVer);
	}

	@Override
	public UpdateRequirement updateType() {
		return UpdateRequirement.UPDATE_IF_AVAILABLE;
	}

	public static String getNewVersion(String mcVer) {
		JsonArray versions = Util.jsonFromUrl(LOADER_META + mcVer).getAsJsonArray();
		JsonObject latest = versions.get(0).getAsJsonObject();
		JsonObject loaderData = latest.getAsJsonObject("loader");
		return loaderData.get("version").getAsString();
	}
}
