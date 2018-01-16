package com.alibaba.weex.amap.component;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.View;

import com.alibaba.weex.amap.util.Constant;
import com.alibaba.weex.amap.util.Utils;
import com.amap.api.maps.AMap;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.dom.WXDomObject;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXComponentProp;
import com.taobao.weex.ui.component.WXVContainer;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Collections;

/**
 * Created by budao on 2017/3/3.
 */

public class WXMapCircleComponent extends WXComponent<View> {
  private MapView mMapView;
  private AMap mMap;
  private Circle mCircle;
  private int mColor = 0;
  private int mFillColor = 0;
  private float mFillOpacity = 1f;
  private String mStrokeStyle;
  private float mWeight = 1.0f;
  private float mRadius = 1.0f;
  private LatLng mCenter;
  private PolylineOptions mLineOptions;
  private Polyline mPolyLine;

  public WXMapCircleComponent(WXSDKInstance instance, WXDomObject dom, WXVContainer parent) {
    super(instance, dom, parent);
  }

  @Override
  protected View initComponentHostView(@NonNull Context context) {
    if (getParent() != null && getParent() instanceof WXMapViewComponent) {
      mMapView = ((WXMapViewComponent) getParent()).getHostView();
      mMap = mMapView.getMap();
      initCircle();
    }
    // FixMe： 只是为了绕过updateProperties中的逻辑检查
    return new View(context);
  }

  @WXComponentProp(name = Constant.Name.CENTER)
  public void setPath(String param) {
    try {
      JSONArray center = new JSONArray(param);
      if (center != null && center.length() == 2) {
        mCenter = new LatLng(center.getDouble(1), center.getDouble(0));
        mCircle.setCenter(mCenter);
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  @WXComponentProp(name = Constant.Name.STROKE_COLOR)
  public void setStrokeColor(String param) {
    mColor = Color.parseColor(param);
    mCircle.setStrokeColor(mColor);
    if ("dashed".equalsIgnoreCase(mStrokeStyle)) {
      mLineOptions.width(0).color(Color.TRANSPARENT).setPoints(Collections.<LatLng>emptyList());
      addPolylinescircle(mCenter, mRadius);
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
    mCircle.setFillColor(mFillColor);
  }

  @WXComponentProp(name = Constant.Name.STROKE_WIDTH)
  public void setStrokeWeight(float param) {
    mWeight = Utils.dp2px(getContext(), param);
    boolean dashed = "dashed".equalsIgnoreCase(mStrokeStyle);
    mCircle.setStrokeWidth(dashed ? 0 : mWeight);
    if (dashed) {
      resetLine();
      addPolylinescircle(mCenter, mRadius);
    } else {
      resetLine();
    }
  }

  @WXComponentProp(name = Constant.Name.FILL_OPACITY)
  public void setFillOpacity(float param) {
    mFillOpacity = param;
    if (mFillOpacity != 1f && Color.alpha(mFillColor) == 255) {
      mFillColor = Color.argb((int) (mFillOpacity * 255), Color.red(mFillColor), Color.green(mFillColor), Color.blue(mFillColor));
      mCircle.setFillColor(mFillColor);
    }
  }

  @WXComponentProp(name = Constant.Name.RADIUS)
  public void setRadius(float param) {
    mRadius = param;
    mCircle.setRadius(mRadius);
    if ("dashed".equalsIgnoreCase(mStrokeStyle)) {
      resetLine();
      addPolylinescircle(mCenter, mRadius);
    } else {
      resetLine();
    }
  }

  @WXComponentProp(name = Constant.Name.STROKE_STYLE)
  public void setStrokeStyle(String style) {
    mStrokeStyle = style;
    if ("dashed".equalsIgnoreCase(mStrokeStyle)) {
      mCircle.setStrokeWidth(0f);
      mCircle.setStrokeColor(Color.TRANSPARENT);
      addPolylinescircle(mCenter, mRadius);
    }
  }

  private void initCircle() {
    boolean dashed = "dashed".equalsIgnoreCase(mStrokeStyle);
    CircleOptions circleOptions = new CircleOptions();
    circleOptions.strokeColor(dashed ? Color.TRANSPARENT : mColor);
    circleOptions.strokeWidth(dashed ? 0 : mWeight);
    circleOptions.radius(mRadius);
    circleOptions.fillColor(mFillColor);
    mCircle = mMap.addCircle(circleOptions);
//    if (dashed) {
//      addPolylinescircle(mCenter, mRadius);
//    }
  }

  private void addPolylinescircle(LatLng centerpoint, float radius) {
    double r = 6371000.79;
    mLineOptions = new PolylineOptions();
    int numpoints = 360;
    double phase = 2 * Math.PI / numpoints;

    //画图
    for (int i = 0; i < numpoints; i++) {
      double dx = (radius * Math.cos(i * phase));
      double dy = (radius * Math.sin(i * phase));//乘以1.6 椭圆比例

      double dlng = dx / (r * Math.cos(centerpoint.latitude * Math.PI / 180) * Math.PI / 180);
      double dlat = dy / (r * Math.PI / 180);
      double newlng = centerpoint.longitude + dlng;
      mLineOptions.add(new LatLng(centerpoint.latitude + dlat, newlng));
    }

    mPolyLine =mMap.addPolyline(mLineOptions.setDottedLineType(
            PolylineOptions.DOTTEDLINE_TYPE_SQUARE).width(mWeight).
            useGradient(false).setDottedLine(true).color(mColor));
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
    if (mCircle != null) {
      mCircle.remove();
    }
    resetLine();
  }
}
