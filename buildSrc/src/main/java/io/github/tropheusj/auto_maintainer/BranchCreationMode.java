package io.github.tropheusj.auto_maintainer;

/**
 * The mode automaintainer should use when pushing code and making new branches.
 */
public enum BranchCreationMode {
	/**
	 * Every single update, even snapshots, gets its own branch.
	 */
	ALL,
	/**
	 * Only patch versions and major versions, such as 1.17.1, 1.19.2, and 1.18 get new branches.
	 */
	PATCH,
	/**
	 * Only major versions, such as 1.18 and 1.19, get new branches.
	 */
	MAJOR
}
