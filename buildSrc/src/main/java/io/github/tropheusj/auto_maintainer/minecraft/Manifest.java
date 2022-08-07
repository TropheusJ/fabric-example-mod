package io.github.tropheusj.auto_maintainer.minecraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.tropheusj.auto_maintainer.Util;

public class Manifest {
	public static final String MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
	public final JsonObject root;
	public final JsonArray versions;
	public final JsonObject latestVersion;
	public final String latestVersionId;

	Manifest() {
		root = Util.jsonFromUrl(MANIFEST_URL).getAsJsonObject();
		versions = root.getAsJsonArray("versions");
		latestVersion = versions.get(0).getAsJsonObject();
		latestVersionId = latestVersion.get("id").getAsString();
	}
}
