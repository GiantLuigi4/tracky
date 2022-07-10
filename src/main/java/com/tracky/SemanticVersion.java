package com.tracky;

import java.util.ArrayList;

public class SemanticVersion {
	private final String text;
	private final int[] asInts;
	
	public SemanticVersion(String text) {
		this.text = text;
		asInts = split();
	}
	
	public int[] split() {
		ArrayList<Integer> list = new ArrayList<>();
		for (String s : text.split("\\."))
			list.add(Integer.parseInt(s));
		int[] ints = new int[list.size()];
		for (int i = 0; i < ints.length; i++)
			ints[i] = list.get(i);
		return ints;
	}
	
	public boolean isNewer(SemanticVersion other) {
		for (int i = 0; i < other.asInts.length; i++)
			if (asInts[i] > other.asInts[i])
				return true;
		return false;
	}
	
	public boolean isSame(SemanticVersion other) {
		for (int i = 0; i < other.asInts.length; i++)
			if (asInts[i] != other.asInts[i])
				return false;
		return true;
	}
	
	public boolean isOlder(SemanticVersion other) {
		return !isNewer(other) && !isSame(other);
	}
}
