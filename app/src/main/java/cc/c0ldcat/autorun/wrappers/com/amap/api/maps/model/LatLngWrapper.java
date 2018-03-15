package cc.c0ldcat.autorun.wrappers.com.amap.api.maps.model;

import cc.c0ldcat.autorun.wrappers.ReflectWrapper;

public class LatLngWrapper extends ReflectWrapper {
    public LatLngWrapper(Object object) {
        super(object);
    }

    public double getLatitude() {
        return (double) getAttributeIfExist("latitude");
    }

    public double getLongitude() {
        return (double) getAttributeIfExist("longitude");
    }
    @Override
    public String toString() {
        return new StringBuilder().append("(").append(getLatitude()).append(", ").append(getLongitude()).append(")").toString();
    }
}
