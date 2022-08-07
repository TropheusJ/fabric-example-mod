package io.github.tropheusj.auto_maintainer;

import io.github.tropheusj.auto_maintainer.updatables.Updatable;

import io.github.tropheusj.auto_maintainer.updatables.UpdateRequirement;
import io.github.tropheusj.auto_maintainer.updatables.builtin.FabricLoaderUpdatable;
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

	private boolean quilt;

	/**
	 * This project is quilt-based, not fabric-based.
	 * // TODO quilt
	 */
	public void quilt() {
		quilt = true;
	}

	public boolean isQuilt() {
		return quilt;
	}

	private BranchCreationMode branchCreationMode = BranchCreationMode.MAJOR;

	public void setBranchCreationMode(BranchCreationMode mode) {
		this.branchCreationMode = mode;
	}

	public BranchCreationMode getBranchCreationMode() {
		return branchCreationMode;
	}

	private String branchFormat = "%s";

	public void setBranchFormat(String format) {
		this.branchFormat = format;
	}

	public String getBranchFormat() {
		return branchFormat;
	}

	private boolean allowUnstableUpdates = false;

	public void setAllowUnstableUpdates(boolean allowUnstableUpdates) {
		this.allowUnstableUpdates = allowUnstableUpdates;
	}

	public boolean allowUnstableUpdates() {
		return allowUnstableUpdates;
	}

	public Config() {
		// defaults
		modrinth("Fabric API", "P7dR8mSH", "fabric_version", UpdateRequirement.REQUIRED_FOR_UPDATE);
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
		modrinth(name, projectId, UpdateRequirement.UPDATE_IF_AVAILABLE);
	}

	/**
	 * Depend on a mod from Modrinth that will be disabled whenever unavailable.
	 */
	public void modrinthDisableIfUnavailable(String name, String projectId) {
		modrinth(name, projectId, UpdateRequirement.DISABLE_IF_UNAVAILABLE);
	}

	/**
	 * Depend on a mod from Modrinth that is absolutely required to successfully build.
	 */
	public void modrinthRequired(String name, String projectId) {
		modrinth(name, projectId, UpdateRequirement.REQUIRED_FOR_UPDATE);
	}

	/**
	 * Depend on a mod from Modrinth with the specified UpdateRequirement.
	 */
	public void modrinth(String name, String projectId, UpdateRequirement type) {
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
	public void modrinth(String name, String projectId, String gradlePropertiesKey, UpdateRequirement type) {
		getUpdatables().put(name, new ModrinthUpdatable(name, projectId, gradlePropertiesKey, type));
	}
}
