package io.github.tropheusj.auto_maintainer;

import io.github.tropheusj.auto_maintainer.updatables.Updatable;

import io.github.tropheusj.auto_maintainer.updatables.UpdateType;
import io.github.tropheusj.auto_maintainer.updatables.builtin.FabricLoaderUpdatable;
import io.github.tropheusj.auto_maintainer.updatables.builtin.MinecraftUpdatable;
import io.github.tropheusj.auto_maintainer.updatables.builtin.ModrinthUpdatable;
import io.github.tropheusj.auto_maintainer.updatables.builtin.QuiltMappingsUpdatable;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Config {
	private LinkedHashMap<String, Updatable> updatables = new LinkedHashMap<>();

	public Map<String, Updatable> getUpdatables() {
		return updatables;
	}

	public void setUpdatables(LinkedHashMap<String, Updatable> updatables) {
		this.updatables = updatables;
	}

	public Config() {
		// defaults
		getUpdatables().put("Minecraft", new MinecraftUpdatable());
		modrinth("Fabric API", "P7dR8mSH", "fabric_version", UpdateType.REQUIRED_FOR_UPDATE);
		getUpdatables().put("Quilt Mappings Build", new QuiltMappingsUpdatable());
		getUpdatables().put("Fabric Loader", new FabricLoaderUpdatable());
	}

	/**
	 * Disable the Fabric API dependency.
	 */
	public void noFapi() {
		getUpdatables().remove("fapi");
	}

	/**
	 * Depend on a mod from Modrinth that will be updated whenever available.
	 */
	public void modrinthUpdateIfAvailable(String name, String projectId) {
		modrinth(name, projectId, UpdateType.UPDATE_IF_AVAILABLE);
	}

	/**
	 * Depend on a mod from Modrinth that will be disabled whenever unavailable.
	 */
	public void modrinthDisableIfUnavailable(String name, String projectId) {
		modrinth(name, projectId, UpdateType.DISABLE_IF_UNAVAILABLE);
	}

	/**
	 * Depend on a mod from Modrinth that is absolutely required to successfully build.
	 */
	public void modrinthRequired(String name, String projectId) {
		modrinth(name, projectId, UpdateType.REQUIRED_FOR_UPDATE);
	}

	/**
	 * Depend on a mod from Modrinth with the specified UpdateType.
	 */
	public void modrinth(String name, String projectId, UpdateType type) {
		modrinth(name, projectId, Util.snakeCase(name) + "_version", type);
	}

	/**
	 * Add a dependency on any mod available on Modrinth.
	 * @param name the name of this mod, to show in logs. ex 'Sodium', 'Quilt Loading Screen'.
	 * @param projectId the project ID of the mod. Can be found on the bottom left
	 *                  of a mod page, under 'Technical information'
	 * @param gradlePropertiesKey the key in the project gradle.properties file used
	 *                            to determine the version of this mod to use
	 * @param type the UpdateType for this mod.
	 */
	public void modrinth(String name, String projectId, String gradlePropertiesKey, UpdateType type) {
		getUpdatables().put(name, new ModrinthUpdatable(name, projectId, gradlePropertiesKey, type));
	}
}
