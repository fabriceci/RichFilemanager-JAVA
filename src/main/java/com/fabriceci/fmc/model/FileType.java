package com.fabriceci.fmc.model;

public enum FileType {
    file ("file"),
    folder ("folder");

    private final String name;

    private FileType(String s) {
        name = s;
    }

    public boolean equalsName(String otherName) {
        // (otherName == null) check is not needed because name.equals(null) returns false
        return name.equals(otherName);
    }

    public String toString() {
        return this.name;
    }
}
