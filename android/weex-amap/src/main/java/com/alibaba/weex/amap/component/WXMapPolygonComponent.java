package com.alibaba.weex.amap.component;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.View;

import com.alibaba.weex.amap.util.Constant;
import com.alibaba.weex.amap.util.Utils;
import com.amap.api.maps.AMap;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.PolygonOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.dom.WXDomObject;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXComponentProp;
import com.taobao.weex.ui.component.WXVContainer;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by budao on 2017/3/3.
 */

public class WXMapPolygonComponent extends WXComponent<View> {
  ArrayList<LatLng> mPosition = new ArrayList<>();
  private MapView mMapView;
  private AMap mMap;
  private Polygon mPolygon;
  private int mColor = 0;
  private int mFillColor = 0;
  private float mFillOpacity = 1f;
  private PolylineOptions mLineOptions;
  private float mWidth = 1.0f;
  private String mStrokeStyle;
  private PolygonOptions polygonOptions;
  private Polyline mPolyLine;

  public WXMapPolygonComponent(WXSDKInstance instance, WXDomObject dom, WXVContainer parent) {
    super(instance, dom, parent);
  }

  @Override
  protected View initComponentHostView(@NonNull Context context) {
    if (getParent() != null && getParent() instanceof WXMapViewComponent) {
      mMapView = ((WXMapViewComponent) getParent()).getHostView();
      mMap = mMapView.getMap();
      initPolygon();
    }
    // FixMe： 只是为了绕过updateProperties中的逻辑检查
    return new View(context);
  }

  @WXComponentProp(name = Constant.Name.PATH)
  public void setPath(String param) {
    mPosition.clear();
    try {
      JSONArray path = new JSONArray(param);
      if (path != null) {
        for (int i = 0; i < path.length(); i++) {
          JSONArray position = path.getJSONArray(i);
          mPosition.add(new LatLng(position.getDouble(1), position.getDouble(0)));
        }
      }

    } catch (JSONException e) {
      e.printStackTrace();
    }
    mPolygon.setPoints(mPosition);
  }

  @WXComponentProp(name = Constant.Name.STROKE_COLOR)
  public void setStrokeColor(String param) {
    mColor = Color.parseColor(param);
    mPolygon.setStrokeColor(mColor);
    if ("dashed".equalsIgnoreCase(mStrokeStyle)) {
      mPolygon.setStrokeWidth(0f);
      mPolygon.setStrokeColor(Color.TRANSPARENT);
      resetLine();
      mLineOptions = new PolylineOptions();
      mPolyLine = mMap.addPolyline(mLineOptions
              .setDottedLineType(PolylineOptions.DOTTEDLINE_TYPE_SQUARE)
              .width(mWidth).color(mColor).addAll(mPosition).setDottedLine(true));
    } else {
      resetLine();
    }
  }

  @WXComponentProp(name = Constant.Name.FILL_COLOR)
  public void setFillColor(String param) {
    mFillColor = Color.parseColor(param);
    if (mFillOpacity != 1f && Color.alpha(mFillColor) == 255) {
      mFillColor = Color.argb((int) (mFillOpacity * 255), Color.red(mFillColor), Color.green(mFillColor), Color.blue(mFillColor));
    }
    mPolygon.setFillColor(mFillColor);
  }

  @WXComponentProp(name = Constant.Name.FILL_OPACITY)
  public void setFillOpacity(float param) {
    mFillOpacity = param;
    if (mFillOpacity != 1f && Color.alpha(mFillColor) == 255) {
      mFillColor = Color.argb((int) (mFillOpacity * 255), Color.red(mFillColor), Color.green(mFillColor), Color.blue(mFillColor));
      mPolygon.setFillColor(mFillColor);
    }
  }

  @WXComponentProp(name = Constant.Name.STROKE_WIDTH)
  public void setStrokeWidth(float param) {
    mWidth = Utils.dp2px(getContext(), param);
    mPolygon.setStrokeWidth(mWidth);
    if ("dashed".equalsIgnoreCase(mStrokeStyle)) {
      mPolygon.setStrokeWidth(0f);
      mPolygon.setStrokeColor(Color.TRANSPARENT);
      resetLine();
      mLineOptions = new PolylineOptions();
      mPolyLine = mMap.addPolyline(mLineOptions.setDottedLineType(PolylineOptions.DOTTEDLINE_TYPE_SQUARE)
              .width(mWidth).color(mColor).addAll(mPosition).setDottedLine(true));
    } else {
      resetLine();
    }
  }

  @WXComponentProp(name = Constant.Name.STROKE_STYLE)
  public void setStrokeStyle(String param) {
    mStrokeStyle = param;
    if ("dashed".equalsIgnoreCase(mStrokeStyle)) {
      mPolygon.setStrokeWidth(0f);
      mPolygon.setStrokeColor(Color.TRANSPARENT);
      mLineOptions = new PolylineOptions();
      mPolyLine = mMap.addPolyline(mLineOptions.
              setDottedLineType(PolylineOptions.DOTTEDLINE_TYPE_SQUARE)
              .width(mWidth).color(mColor)
              .addAll(mPosition).setDottedLine(true));
    } else {
      resetLine();
    }
  }


  private void initPolygon() {
    boolean dashed = "dashed".equalsIgnoreCase(mStrokeStyle);
    if (polygonOptions != null) {
      polygonOptions.fillColor(Color.TRANSPARENT).getPoints().clear();
    }
    polygonOptions = new PolygonOptions();
    polygonOptions.addAll(mPosition);
    polygonOptions.strokeColor(dashed ? Color.TRANSPARENT : mColor);
    polygonOptions.strokeWidth(dashed ? 0 : mWidth);
    polygonOptions.fillColor(mFillColor);
    mPolygon = mMap.addPolygon(polygonOptions);
//    if (dashed) {
//      PolylineOptions polylineOptions = new PolylineOptions();
//      mMap.addPolyline(polylineOptions.width(mWidth).color(mColor).addAll(mPosition).setDottedLine(true));
//    }
  }

  public boolean contains(LatLng latLng) {
    return mPolygon != null && mPolygon.contains(latLng);
  }

  private void resetLine() {
    if (mLineOptions != null) {
      mLineOptions.width(0).color(Color.TRANSPARENT).setPoints(Collections.<LatLng>emptyList());
    }
    if (mPolyLine != null) {
      mPolyLine.remove();
    }
  }

  @Override
  public void destroy() {
    super.destroy();
    if (mPosition != null) {
      mPosition.clear();
    }
    if (polygonOptions != null) {
      polygonOptions.fillColor(Color.TRANSPARENT).getPoints().clear();
    }
    if (mPolygon != null) {
      mPolygon.remove();
    }
    resetLine();
  }
}
