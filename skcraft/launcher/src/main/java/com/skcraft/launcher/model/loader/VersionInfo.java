// Generated by delombok at Sat Jul 14 04:26:21 CEST 2018
/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */
package com.skcraft.launcher.model.loader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.skcraft.launcher.model.minecraft.Library;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VersionInfo {
    private String minecraftArguments;
    private String mainClass;
    private List<Library> libraries;

    @SuppressWarnings("all")
    public VersionInfo() {
    }

    @SuppressWarnings("all")
    public String getMinecraftArguments() {
        return this.minecraftArguments;
    }

    @SuppressWarnings("all")
    public String getMainClass() {
        return this.mainClass;
    }

    @SuppressWarnings("all")
    public List<Library> getLibraries() {
        return this.libraries;
    }

    @SuppressWarnings("all")
    public void setMinecraftArguments(final String minecraftArguments) {
        this.minecraftArguments = minecraftArguments;
    }

    @SuppressWarnings("all")
    public void setMainClass(final String mainClass) {
        this.mainClass = mainClass;
    }

    @SuppressWarnings("all")
    public void setLibraries(final List<Library> libraries) {
        this.libraries = libraries;
    }

    @Override
    @SuppressWarnings("all")
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof VersionInfo)) return false;
        final VersionInfo other = (VersionInfo) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$minecraftArguments = this.getMinecraftArguments();
        final Object other$minecraftArguments = other.getMinecraftArguments();
        if (this$minecraftArguments == null ? other$minecraftArguments != null : !this$minecraftArguments.equals(other$minecraftArguments)) return false;
        final Object this$mainClass = this.getMainClass();
        final Object other$mainClass = other.getMainClass();
        if (this$mainClass == null ? other$mainClass != null : !this$mainClass.equals(other$mainClass)) return false;
        final Object this$libraries = this.getLibraries();
        final Object other$libraries = other.getLibraries();
        if (this$libraries == null ? other$libraries != null : !this$libraries.equals(other$libraries)) return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final Object other) {
        return other instanceof VersionInfo;
    }

    @Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $minecraftArguments = this.getMinecraftArguments();
        result = result * PRIME + ($minecraftArguments == null ? 43 : $minecraftArguments.hashCode());
        final Object $mainClass = this.getMainClass();
        result = result * PRIME + ($mainClass == null ? 43 : $mainClass.hashCode());
        final Object $libraries = this.getLibraries();
        result = result * PRIME + ($libraries == null ? 43 : $libraries.hashCode());
        return result;
    }

    @Override
    @SuppressWarnings("all")
    public String toString() {
        return "VersionInfo(minecraftArguments=" + this.getMinecraftArguments() + ", mainClass=" + this.getMainClass() + ", libraries=" + this.getLibraries() + ")";
    }
}
