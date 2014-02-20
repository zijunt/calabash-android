package sh.calaba.instrumentationbackend.query;

import java.lang.RuntimeException;
import java.lang.System;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.lang.reflect.Field;

import sh.calaba.instrumentationbackend.InstrumentationBackend;
import sh.calaba.instrumentationbackend.query.ast.UIQueryUtils;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

public class ViewMapper {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object extractDataFromView(View v) {		
		
		Map data = new HashMap();
		data.put("class", getClassNameForView(v));		
		data.put("description", v.toString());
		data.put("contentDescription", getContentDescriptionForView(v));
		data.put("enabled", v.isEnabled());
		
		data.put("id", getIdForView(v));

		Map rect = getRectForView(v);

		data.put("rect", rect);

		if (v instanceof Button) {
			Button b = (Button) v;
			data.put("text", b.getText().toString());
		}
		if (v instanceof CheckBox) {
			CheckBox c = (CheckBox) v;
			data.put("checked", c.isChecked());
		}
		if (v instanceof TextView) {
			TextView t = (TextView) v;
			data.put("text", t.getText().toString());
		}
		return data;

	}

	public static Map<String, Integer> getRectForView(View v) {
		Map<String,Integer> rect = new HashMap<String,Integer>();

        float[] scale = findScaleFactor(v);
        float scaleFactorX = scale[0];
        float scaleFactorY = scale[1];

        int rawX = 0;
        int rawY = 0;
        int width = 0;
        int height = 0;

        if (scaleFactorX < 0.99f || scaleFactorX > 1.01f || scaleFactorY < 0.99f || scaleFactorY > 1.01f) {
            int[] viewLocationInWindow = new int[2];
            v.getLocationInWindow(viewLocationInWindow);
            int viewLocationInWindowX = viewLocationInWindow[0];
            int viewLocationInWindowY = viewLocationInWindow[1];

            int scaledX = (int)(scaleFactorX * viewLocationInWindowX);
            int scaledY = (int)(scaleFactorY * viewLocationInWindowY);

            // Offset the coordinates of the view with regards to the rootview
            View rootView = v.getRootView();

            int[] rootViewLocation = new int[2];
            rootView.getLocationOnScreen(rootViewLocation);
            int rootViewLocationX = rootViewLocation[0];
            int rootViewLocationY = rootViewLocation[1];

            // If the screen has been rotated, the position of the root view is correct
            boolean rotated = isDisplayRotated(v);

            int rootViewRawX = (int)(rootViewLocationX * (rotated ? 1 : scaleFactorX));
            int rootViewRawY = (int)(rootViewLocationY * (rotated ? 1 : scaleFactorY));

            rawX = rootViewRawX + scaledX;
            rawY = rootViewRawY + scaledY;

            width = (int)(v.getWidth() * scaleFactorX);
            height = (int)(v.getHeight() * scaleFactorY);
        } else {
            int[] location = new int[2];

            v.getLocationOnScreen(location);

            rawX = location[0];
            rawY = location[1];

            width = v.getWidth();
            height = v.getHeight();
        }

		rect.put("x", rawX);
		rect.put("y", rawY);

		rect.put("center_x", (int)(rawX + width / 2.0f));
		rect.put("center_y", (int)(rawY + height / 2.0f));
		
		rect.put("width", width);
		rect.put("height", height);

		return rect;
	}

    public static float[] findScaleFactor(View v) {
        try {
            float[] scaleFactor = new float[2];

            DisplayMetrics displayMetrics = v.getContext().getResources().getDisplayMetrics();
            Class<?> displayMetricsClass = displayMetrics.getClass();

            Field field = displayMetricsClass.getField("widthPixels");
            int width = field.getInt(displayMetrics);
            field = displayMetricsClass.getField("noncompatWidthPixels");
            int noncompatWidth = field.getInt(displayMetrics);
            scaleFactor[0] = (float)noncompatWidth / width;

            field = displayMetricsClass.getField("heightPixels");
            int height = field.getInt(displayMetrics);
            field = displayMetricsClass.getField("noncompatHeightPixels");
            int noncompatHeight = field.getInt(displayMetrics);
            scaleFactor[1] = (float)noncompatHeight / height;

            return scaleFactor;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

	public static String getContentDescriptionForView(View v) {
		CharSequence description = v.getContentDescription();
		return description != null ? description.toString() : null;
	}

	public static String getClassNameForView(View v) {
		return v.getClass().getName();
	}

	public static String getIdForView(View v) {
		String id = null;
		try {
			id = InstrumentationBackend.solo.getCurrentActivity()
					.getResources().getResourceEntryName(v.getId());
		} catch (Resources.NotFoundException e) {
			System.out.println("Resource not found for " + v.getId()
					+ ". Moving on.");
		}
		return id;
	}

    public static boolean isDisplayRotated(View v) {
        Display display = null;

        try {
            if (Build.VERSION.SDK_INT < 17) {
                display  = (Display) v.getContext().getSystemService(Context.DISPLAY_SERVICE);
            } else {
                display = v.getDisplay();
            }
        } catch (Exception e) {
            return false;
        }

        try {
            if (Build.VERSION.SDK_INT < 8) { // orientation method was deprecated in API level 8
                return (display.getOrientation() != 0);
            } else {
                return (display.getRotation() != 0);
            }
        } catch (Exception e) {
            return false;
        }
    }

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object mapView(Object o) {
		if (o instanceof View) {
			return extractDataFromView((View) o);
		} 
		else if (o instanceof Map) {			
			Map copy = new HashMap();			
			for (Object e : ((Map) o).entrySet()) {
				Map.Entry entry = (Entry) e;
				Object value = entry.getValue();
				if (value instanceof View) {
					copy.put(entry.getKey(), UIQueryUtils.getId((View) value));
				}				
				else {
					copy.put(entry.getKey(),entry.getValue());
				}			
			}
			
			return copy;
		} 
		else if (o instanceof CharSequence) {
			return o.toString();
		}
		return o;
	}

}
