package io.github.tropheusj.auto_maintainer.updatables.builtin;

import com.google.gson.JsonArray;

import com.google.gson.JsonObject;

import io.github.tropheusj.auto_maintainer.Config;
import io.github.tropheusj.auto_maintainer.Util;
import io.github.tropheusj.auto_maintainer.updatables.Updatable;

import io.github.tropheusj.auto_maintainer.updatables.UpdateType;

import org.gradle.api.Project;

import java.util.Properties;

public class FabricLoaderUpdatable implements Updatable {
	public static final String PROPERTY_KEY = "loader_version";
	public static final String LOADER_META = "https://meta.fabricmc.net/v2/versions/loader/";

	private String currentVersion;
	private String newVersion;

	@Override
	public void initialize(Project project, Properties properties, Config config) {
		currentVersion = properties.getProperty(PROPERTY_KEY);
		Util.checkNull(currentVersion, "Fabric Loader", PROPERTY_KEY);
		String mcVer = config.getUpdatables().get(MinecraftUpdatable.UPDATABLE_KEY).updateVersion();
		newVersion = getNewVersion(mcVer);
	}

	@Override
	public boolean hasUpdate() {
		return !currentVersion.equals(newVersion);
	}

	@Override
	public void update(Project project, Properties properties) {

	}

	@Override
	public String currentVersion() {
		return currentVersion;
	}

	@Override
	public String updateVersion() {
		return newVersion;
	}

	@Override
	public UpdateType updateType() {
		return UpdateType.UPDATE_IF_AVAILABLE;
	}

	public static String getNewVersion(String mcVer) {
		JsonArray versions = Util.jsonFromUrl(LOADER_META + mcVer).getAsJsonArray();
		JsonObject latest = versions.get(0).getAsJsonObject();
		JsonObject loaderData = latest.getAsJsonObject("loader");
		return loaderData.get("version").getAsString();
	}
}
