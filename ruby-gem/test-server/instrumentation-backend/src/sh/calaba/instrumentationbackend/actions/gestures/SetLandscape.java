package sh.calaba.instrumentationbackend.actions.gestures;

import com.jayway.android.robotium.solo.Solo;

import android.view.Display;
import sh.calaba.instrumentationbackend.InstrumentationBackend;
import sh.calaba.instrumentationbackend.Result;
import sh.calaba.instrumentationbackend.actions.Action;

public class SetLandscape implements Action {

@Override
public Result execute(String... args) {

	InstrumentationBackend.solo.setActivityOrientation(Solo.LANDSCAPE);
	return Result.successResult();
}

@Override
public String key() {
	return "set_landscape";
	}
}