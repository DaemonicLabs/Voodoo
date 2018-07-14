// Generated by delombok at Sat Jul 14 04:26:21 CEST 2018
/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */
package com.skcraft.launcher.model.modpack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RequireAll implements Condition {
    private List<Feature> features = new ArrayList<Feature>();

    public RequireAll() {
    }

    public RequireAll(List<Feature> features) {
        this.features = features;
    }

    public RequireAll(Feature... feature) {
        features.addAll(Arrays.asList(feature));
    }

    @Override
    public boolean matches() {
        if (features == null) {
            return true;
        }
        for (Feature feature : features) {
            if (!feature.isSelected()) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("all")
    public List<Feature> getFeatures() {
        return this.features;
    }

    @SuppressWarnings("all")
    public void setFeatures(final List<Feature> features) {
        this.features = features;
    }

    @Override
    @SuppressWarnings("all")
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof RequireAll)) return false;
        final RequireAll other = (RequireAll) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$features = this.getFeatures();
        final Object other$features = other.getFeatures();
        if (this$features == null ? other$features != null : !this$features.equals(other$features)) return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final Object other) {
        return other instanceof RequireAll;
    }

    @Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $features = this.getFeatures();
        result = result * PRIME + ($features == null ? 43 : $features.hashCode());
        return result;
    }

    @Override
    @SuppressWarnings("all")
    public String toString() {
        return "RequireAll(features=" + this.getFeatures() + ")";
    }
}
