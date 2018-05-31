package cc.c0ldcat.autorun.modules.real;

import android.content.Context;
import cc.c0ldcat.autorun.BuildConfig;
import cc.c0ldcat.autorun.Common;
import cc.c0ldcat.autorun.models.Location;
import cc.c0ldcat.autorun.models.SimpleLocation;
import cc.c0ldcat.autorun.models.SimpleVector;
import cc.c0ldcat.autorun.modules.Module;
import cc.c0ldcat.autorun.utils.CommonUtils;
import cc.c0ldcat.autorun.utils.LogUtils;
import cc.c0ldcat.autorun.wrappers.com.amap.api.location.AMapLocationWrapper;
import cc.c0ldcat.autorun.wrappers.com.example.gita.gxty.ram.service.BaseService.AMapLocationListenerWrapper;
import cc.c0ldcat.autorun.wrappers.com.example.gita.gxty.ram.service.RuningServiceWrapper;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Deque;
import java.util.LinkedList;

// TODO: blindly walk if distance is not enough

public class FakeWalk extends Module {
    private ClassLoader classLoader;
    private GetCheckPoint getCheckPoint;
    private GetMyRuningActivity getMyRuningActivity;

    private Object amap;

    private double latitude = 0;
    private double longitude = 0;

    private boolean waitingRelayPoints = true;
    private Deque<Location> goTos = new LinkedList<>();

    private int step = 0;
    private double speed = 0.0001;

    public FakeWalk(ClassLoader classLoader, GetCheckPoint getCheckPoint, GetMyRuningActivity getMyRuningActivity) {
        this.classLoader = classLoader;
        this.getCheckPoint = getCheckPoint;
        this.getMyRuningActivity = getMyRuningActivity;
    }

    private Location goTo() {
        return goTos.peekFirst();
    }

    @Override
    public void load() {
        super.load();
        new AMapLocationListenerWrapper().hookOnLocationChanged(classLoader, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);

                AMapLocationWrapper aMapLocation = new AMapLocationWrapper();
                aMapLocation.setObject(param.args[0]);

                // if change map
                if (getCheckPoint.getAMap() != amap) {
                    LogUtils.i("new walk plan");
                    step = 0;
                    amap = getCheckPoint.getAMap();
                    goTos.clear();
                    latitude = longitude = 0;
                }

                // init location if no fake location
                if (latitude == 0 || longitude == 0) {
                    LogUtils.i("init location");
                    latitude = aMapLocation.getLatitude();
                    longitude = aMapLocation.getLongitude();
                }

                // get next target
                if (goTo() == null) {
                    goTos.addLast(getCheckPoint.getNextCheckPoint(new SimpleLocation(longitude, latitude)));

                    if (goTo() == null) {
                        LogUtils.i("no more target");
                    } else {
                        waitingRelayPoints = true;
                        addRelayPoints();

                        LogUtils.i("new target " + goTo());
                    }
                }

                // move to target
                if (!waitingRelayPoints && goTo() != null) {
                    SimpleVector stepVector = CommonUtils.vector(new SimpleLocation(longitude, latitude), goTo(), speed);
                    latitude += stepVector.getLatitude();
                    longitude += stepVector.getLongitude();

                    if (Math.abs(latitude - goTo().getLatitude()) < speed
                            && Math.abs(longitude - goTo().getLongitude()) < speed) {
                        LogUtils.i("reach target");
                        goTos.pollFirst();
                    }
                }

                // do move
                aMapLocation.setLatitude(latitude);
                aMapLocation.setLongitude(longitude);

                LogUtils.i("move to " + aMapLocation);
            }
        });

        new RuningServiceWrapper().hookGetBupin(classLoader, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                step += 3;
                return step;
            }
        });
    }

    private void addRelayPoints() {
        Context context = getMyRuningActivity.getMyRuningActivity();
        RequestQueue queue = Volley.newRequestQueue(context);
        String url ="http://restapi.amap.com/v3/direction/walking?origin=" + longitude + "," + latitude + "&destination=" + goTo() + "&key=" + BuildConfig.AMAP_WEB_KEY;

        LogUtils.d("request " + url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                try {
                    JSONArray paths = jsonObject.getJSONObject("route").getJSONArray("paths");
                    JSONObject path = paths.getJSONObject(0);
                    JSONArray steps = path.getJSONArray("steps");
                    for (int i = steps.length() - 1; i >= 0 ; i--) {
                        String[] locationStrs = steps.getJSONObject(i).getString("polyline").split(";");
                        for (int j = locationStrs.length - 1; j >= 0; j--) {
                            String latLngStrs[] = locationStrs[j].split(",");
                            goTos.addFirst(new SimpleLocation(Double.parseDouble(latLngStrs[0]), Double.parseDouble(latLngStrs[1])));
                        }
                    }

                    waitingRelayPoints = false;
                } catch (JSONException e) {
                    LogUtils.e(e);
                    LogUtils.e(jsonObject.toString());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                LogUtils.e("Failed + " + volleyError.networkResponse.statusCode);
            }
        });

        // Add the request to the RequestQueue.
        queue.add(jsonObjectRequest);
    }
}
