package com.fabriceci.fmc.model;

public class ConfigSecurity {
    private boolean readOnly;

    private ConfigExtensions extensions;

    public boolean getReadOnly ()
    {
        return readOnly;
    }

    public void setReadOnly (boolean readOnly)
    {
        this.readOnly = readOnly;
    }

    public ConfigExtensions getExtensions ()
    {
        return extensions;
    }

    public void setExtensions (ConfigExtensions extensions)
    {
        this.extensions = extensions;
    }
}
