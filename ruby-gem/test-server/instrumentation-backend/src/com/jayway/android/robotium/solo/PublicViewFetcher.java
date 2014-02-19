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
        Map<String,Integer> viewRect = ViewMapper.getRectForView(view);
        Map<String,Integer> parentViewRect = null;
        int parentY = 0;
        int parentHeight = 0;

        if (view.equals(parent)) {
            return true;
        } else if (parent != null) {
            parentViewRect = ViewMapper.getRectForView(parent);
            parentY = parentViewRect.get("y");
            parentHeight = parentViewRect.get("height");
        }

        int windowHeight = 0;

        if (parent == null) {
            View rootView = view.getRootView();

            if (rootView != null && !view.equals(rootView)) {
                Map<String,Integer> rootViewRect = ViewMapper.getRectForView(view);
                windowHeight = rootViewRect.get("y") + rootViewRect.get("height");
            }
        } else {
            windowHeight = parentY + parentHeight;
        }

        if (viewRect.get("center_y") > windowHeight) {
            System.out.println("center_y > windowheight " + String.valueOf(windowHeight));
            return false;
        } else if (viewRect.get("center_y") < parentY) {
            System.out.println("center_y < parentY " + String.valueOf(parentY));
            return false;
        }

        return true;
    }
}
