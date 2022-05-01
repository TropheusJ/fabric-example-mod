package io.github.tropheusj.auto_maintainer.updatables;

/**
 * How updating this dependency should be handled.
 */
public enum UpdateRequirement {
	/**
	 * This dependency is needed. If an update is not found, the build will fail.
	 */
	REQUIRED_FOR_UPDATE,
	/**
	 * This dependency is not needed. If an update is not found, it will be disabled.
	 */
	DISABLE_IF_UNAVAILABLE,
	/**
	 * This dependency does not need to be updated, but it will be updated anyway if available.
	 */
	UPDATE_IF_AVAILABLE
}
