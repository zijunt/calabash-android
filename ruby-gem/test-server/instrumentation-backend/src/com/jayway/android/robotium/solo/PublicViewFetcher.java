package com.jayway.android.robotium.solo;

import sh.calaba.instrumentationbackend.query.ViewMapper;

import android.app.Activity;
import android.app.Instrumentation;
import android.view.View;

import java.lang.String;
import java.util.HashMap;
import java.util.Map;

public class PublicViewFetcher extends ViewFetcher {

    public PublicViewFetcher(ActivityUtils activityUtils) {
        super(activityUtils);
    }

    public boolean isViewFullyShown(View view) {
        if (view == null) return false;

        View parent = getScrollOrListParent(view);
        Map viewRect = ViewMapper.getRectForView(view);
        Map parentViewRect = null;
        double parentY = 0;
        double parentHeight = 0;

        if (view.equals(parent)) {
            return true;
        } else if (parent != null) {
            parentViewRect = ViewMapper.getRectForView(parent);
            parentY = (Integer)parentViewRect.get("y");
            parentHeight = (Integer)parentViewRect.get("height");
        }

        double windowHeight = 0.0d;

        if (parent == null) {
            View rootView = view.getRootView();

            if (rootView != null && !view.equals(rootView)) {
                Map rootViewRect = ViewMapper.getRectForView(view);
                windowHeight = ((Integer)rootViewRect.get("y")) + ((Integer)rootViewRect.get("height"));
            }
        } else {
            windowHeight = parentY + parentHeight;
        }

        if ((Float)viewRect.get("center_y") > windowHeight) {
            System.out.println("center_y > windowheight " + String.valueOf(windowHeight));
            return false;
        } else if ((Float)viewRect.get("center_y") < parentY) {
            System.out.println("center_y < parentY " + String.valueOf(parentY));
            return false;
        }

        return true;
    }
}
