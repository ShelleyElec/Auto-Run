package cc.c0ldcat.autorun.modules.real;

import cc.c0ldcat.autorun.models.Location;
import cc.c0ldcat.autorun.models.SimpleLocation;
import cc.c0ldcat.autorun.modules.Module;
import cc.c0ldcat.autorun.utils.CommonUtils;
import cc.c0ldcat.autorun.utils.LogUtils;
import cc.c0ldcat.autorun.wrappers.com.amap.api.maps.AMapWrapper;
import cc.c0ldcat.autorun.wrappers.com.amap.api.maps.model.LatLngWrapper;
import cc.c0ldcat.autorun.wrappers.com.amap.api.maps.model.MarkerOptionsWrapper;
import de.robv.android.xposed.XC_MethodHook;

import java.util.*;

public class GetCheckPoint extends Module {
    private ClassLoader classLoader;
    private Object amap;
    private List<Location> latLngs = new ArrayList<>();

    public GetCheckPoint(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public Object getAMap() {
        return amap;
    }

    public Location getNextCheckPoint(Location location) throws NoSuchElementException {
        Location min = null;
        for (Location latLng : latLngs) {
            if (min == null || CommonUtils.distance(location, latLng) < CommonUtils.distance(location, min)) {
                min = latLng;
            }
        }

        if (min != null) latLngs.remove(min);

        return min;
    }

    @Override
    public void load() {
        super.load();
        new AMapWrapper().hookAddMarker(classLoader,
                new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                MarkerOptionsWrapper markerOption = new MarkerOptionsWrapper();
                markerOption.setObject(param.args[0]);

                String title = markerOption.getTitle();

                // if change map
                if(amap != param.thisObject) {
                    LogUtils.i("new run plan");
                    latLngs.clear();
                    amap = param.thisObject;
                }

                if (title == null || title.equals("必经点") || title.equals("途经点")) {
                    LatLngWrapper latLng = markerOption.getPosition();

                    if (title == null) {
                        title = "起点";
                    } else {
                        latLngs.add(latLng);
                    }

                    LogUtils.i(title + ": " + latLng);
                }
            }
        });
    }
}
