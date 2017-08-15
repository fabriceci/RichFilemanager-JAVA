package com.fabriceci.fmc.model;


public class ConfigExtensions {

    private String policy;

    private boolean ignoreCase = true;

    private String[] restrictions;

    public String[] getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(String[] restrictions) {
        this.restrictions = restrictions;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public boolean getIgnoreCase() {
        return ignoreCase;
    }
}