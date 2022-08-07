package io.github.tropheusj.auto_maintainer.minecraft;

public record Versions(String oldVer, String newVer, String targetVer, boolean upToDate) {
	public Versions(String oldVer, String newVer, String targetVer) {
		this(oldVer, newVer, targetVer, oldVer.equals(newVer));
	}
}
