package io.github.tropheusj.auto_maintainer.minecraft;

public record Versions(String oldVer, String newVer, boolean upToDate) {
	public Versions(String oldVer, String newVer) {
		this(oldVer, newVer, oldVer.equals(newVer));
	}
}
