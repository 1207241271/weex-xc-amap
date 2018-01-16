package com.alibaba.weex.amap.module;

import android.support.annotation.Nullable;
import android.util.Log;

import com.alibaba.weex.amap.component.WXMapPolygonComponent;
import com.alibaba.weex.amap.component.WXMapViewComponent;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMapException;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.geocoder.StreetNumber;
import com.taobao.weex.WXEnvironment;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.common.WXModule;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.utils.WXLogUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by budao on 2017/1/24.
 */

public class WXMapModule extends WXModule implements GeocodeSearch.OnGeocodeSearchListener {
  private static final String RESULT = "result";
  private static final String DATA = "data";

  private static final String RESULT_OK = "success";
  private static final String RESULT_FAILED = "failed";
  private static final int RANGE_MILES = 200;
  private static final int GEO_SEARCH_SUCCESS = 1000;
  private static final int GEO_SEARCH_IN_ERROR = 1200;

  private GeocodeSearch geocoderSearch;
  private IdentityHashMap<RegeocodeQuery, Integer> queryMap;
  private List<HashMap<String, Object>> resultAddr;
  private JSCallback regeoCallback;
  private int geoCodeCount = 0;

  public WXMapModule() {
    geocoderSearch = new GeocodeSearch(WXEnvironment.getApplication());
    geocoderSearch.setOnGeocodeSearchListener(this);
    queryMap = new IdentityHashMap<>();
  }

  /**
   * get line distance between to POI.
   */
  @JSMethod
  public void getLineDistance(String posA, String posB, @Nullable final JSCallback callback) {
    Log.v("getDistance", posA + ", " + posB);
    float distance = -1;
    try {
      JSONArray jsonArray = new JSONArray(posA);
      LatLng latLngA = new LatLng(jsonArray.optDouble(1), jsonArray.optDouble(0));
      JSONArray jsonArrayB = new JSONArray(posB);
      LatLng latLngB = new LatLng(jsonArrayB.optDouble(1), jsonArrayB.optDouble(0));
      distance = AMapUtils.calculateLineDistance(latLngA, latLngB);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    if (callback != null) {
      HashMap map = new HashMap(2);
      HashMap data = new HashMap(1);
      data.put("distance", distance);
      map.put(DATA, data);
      map.put(RESULT, distance >= 0 ? RESULT_OK : RESULT_FAILED);
      callback.invoke(map);
    }
  }

  @JSMethod
  public void polygonContainsMarker(String position, String id, @Nullable final JSCallback callback) {
    boolean contains = false;
    boolean success = false;
    try {
      JSONArray jsonArray = new JSONArray(position);
      LatLng latLng = new LatLng(jsonArray.optDouble(1), jsonArray.optDouble(0));

      WXComponent component = findComponent(id);

      if (component != null && component instanceof WXMapPolygonComponent) {
        contains = ((WXMapPolygonComponent) component).contains(latLng);
        success = true;
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
    if (callback != null) {
      HashMap map = new HashMap(2);
      map.put(DATA, contains);
      map.put(RESULT, success ? RESULT_OK : RESULT_FAILED);
      callback.invoke(map);
    }

  }

  @JSMethod
  public void getCenterLocation(String id, @Nullable final JSCallback callback){
    WXComponent component = findComponent(id);
    HashMap map = new HashMap(2);
    if (component != null && component instanceof WXMapViewComponent) {
      MapView mapView = ((WXMapViewComponent) component).getMapView();
      LatLng target = null;
      try {
        target = mapView.getMap().getCameraPosition().target;
      } catch (Exception e) {
        e.printStackTrace();
        if (callback != null) {
          map.put(RESULT, RESULT_FAILED);
          callback.invoke(map);
        }
        return;
      }

      map.put(RESULT, RESULT_OK);
      Double[] LngLat = {target.longitude, target.latitude};
      map.put(DATA, LngLat);
      if (callback != null) {
        callback.invoke(map);
      }
    } else {
      if (callback != null) {
        map.put(RESULT, RESULT_FAILED);
        callback.invoke(map);
      }
    }
  }

  /**
   * get user location.
   */
  @JSMethod
  public void getUserLocation(String id, @Nullable final JSCallback callback) {
    final AMapLocationClient client = new AMapLocationClient(
        WXEnvironment.getApplication().getApplicationContext());
    final AMapLocationClientOption clientOption = new AMapLocationClientOption();
    //设置定位监听
    client.setLocationListener(new AMapLocationListener() {
      public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null && aMapLocation.getErrorCode() == 0) {
          if (callback != null) {
            HashMap map = new HashMap(2);
            HashMap data = new HashMap(1);
            ArrayList position = new ArrayList();
            position.add(aMapLocation.getLongitude());
            position.add(aMapLocation.getLatitude());
            data.put("position", position);
            map.put(DATA, data);
            map.put(RESULT, aMapLocation.getLongitude() > 0 && aMapLocation.getLatitude() > 0 ? RESULT_OK : RESULT_FAILED);
            callback.invoke(map);
          }
        } else {
          String errText = "定位失败," + aMapLocation.getErrorCode() + ": " + aMapLocation.getErrorInfo();
          WXLogUtils.e("WXMapModule", errText);
        }
        if (client != null) {
          client.stopLocation();
          client.onDestroy();
        }
      }
    });
    //设置为高精度定位模式
    clientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
    clientOption.setOnceLocation(true);
    //设置定位参数
    client.setLocationOption(clientOption);
    // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
    // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
    // 在定位结束后，在合适的生命周期调用onDestroy()方法
    // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
    client.startLocation();
  }

  @Override
  public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
    if (i == GEO_SEARCH_SUCCESS) {
      HashMap<String, Object> map = new HashMap<>();
      RegeocodeQuery regeocodeQuery = regeocodeResult.getRegeocodeQuery();
      map.put("formattedAddress", regeocodeResult.getRegeocodeAddress().getFormatAddress());
      HashMap<String, Object> detailMap = new HashMap<>();
      detailMap.put("city", regeocodeResult.getRegeocodeAddress().getCity());
      detailMap.put("country", regeocodeResult.getRegeocodeAddress().getCountry());
      detailMap.put("district",regeocodeResult.getRegeocodeAddress().getDistrict());
      detailMap.put("township", regeocodeResult.getRegeocodeAddress().getTownship());

      HashMap<String, String> streetNumberMap = new HashMap<>();
      StreetNumber streetNumber = regeocodeResult.getRegeocodeAddress().getStreetNumber();
      if (streetNumber != null) {
        streetNumberMap.put("street", streetNumber.getStreet());
        streetNumberMap.put("direction", streetNumber.getDirection());
        streetNumberMap.put("number", streetNumber.getNumber());
      }

      detailMap.put("streetNumber", streetNumberMap);
      map.put("addressComponent", detailMap);
      resultAddr.set(queryMap.get(regeocodeQuery), map);
      geoCodeCount++;

      if (geoCodeCount == resultAddr.size() && resultAddr.size() == queryMap.size()) {
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put(RESULT, RESULT_OK);
        resultMap.put(DATA, resultAddr);
        if (regeoCallback != null) {
          regeoCallback.invoke(resultMap);
        }
      }
    }else{
      geoCodeCount++;
      if (geoCodeCount == resultAddr.size()) {
        HashMap<String, Object> resultMap = new HashMap<>();
        resultMap.put(RESULT, RESULT_OK);
        resultMap.put(DATA, resultAddr);
        if (regeoCallback != null) {
          regeoCallback.invoke(resultMap);
        }
      }
    }
  }

  @Override
  public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

  }

  /**
   * 目前仅支持单次调用结束后调用，todo: 需要支持多次调用。
   * @param positions
   * @param callback
   */
  @JSMethod
  public void reGeoPositions(String positions, JSCallback callback) {
    queryMap.clear();
    regeoCallback = callback;
    geoCodeCount = 0;
    try {
      JSONArray posArr = new JSONArray(positions);
      resultAddr = new ArrayList<>();
      resultAddr.addAll(Collections.nCopies(posArr.length(), new HashMap<String, Object>(1)));
      for (int i = 0; i < posArr.length(); i++) {
        JSONArray jsonArray = posArr.getJSONArray(i);
        RegeocodeQuery regeocodeQuery = new RegeocodeQuery(new LatLonPoint(jsonArray.getDouble(1),
                jsonArray.getDouble(0)), RANGE_MILES, GeocodeSearch.GPS);
        geocoderSearch.getFromLocationAsyn(regeocodeQuery);
        queryMap.put(regeocodeQuery, i);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
