package com.jjmoo.appjoint;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * Descriptions
 * <p><br>
 *
 * @author Zohn
 *
 */
@SuppressWarnings({"unused", "WeakerAccess", "MismatchedQueryAndUpdateOfCollection"})
public class AppLike {
    private static final String TAG = "AppJoint/AppLike";

    private List<String> mApplicationClassNames = new ArrayList<>();
    private List<Application> mApplications = new ArrayList<>();
    private Context mBase;

    private AppLike() {}

    public static AppLike getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Nullable
    public Application getContext() {
        return null == mBase ? null : (Application) mBase.getApplicationContext();
    }

    public synchronized void attachBaseContext(Context base) {
        mBase = base;
        for (Application application : mApplications) {
            try {
                Method attachBaseContext = ContextWrapper.class.getDeclaredMethod(
                        "attachBaseContext", Context.class);
                attachBaseContext.setAccessible(true);
                attachBaseContext.invoke(application, base);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public synchronized void onCreate() {
        for (Application application : mApplications) {
            application.onCreate();
        }
    }

    public synchronized void onTerminate() {
        for (Application application : mApplications) {
            application.onTerminate();
        }
    }

    public synchronized void onConfigurationChanged(Configuration newConfig) {
        for (Application application : mApplications) {
            application.onConfigurationChanged(newConfig);
        }
    }

    public synchronized void onLowMemory() {
        for (Application application : mApplications) {
            application.onLowMemory();
        }
    }

    public synchronized void onTrimMemory(int level) {
        for (Application application : mApplications) {
            application.onTrimMemory(level);
        }
    }

    private synchronized void addModuleAppName(String className) {
        mApplicationClassNames.add(className);
    }

    private synchronized void initModuleApp() {
        for (String name : mApplicationClassNames) {
            String className = name.replaceAll("/", ".");
            try {
                mApplications.add((Application) Class.forName(className).newInstance());
            } catch (Exception e) {
                Log.w(TAG, "failed to add application: " + className);
            }
        }
    }

    private static class SingletonHolder {
        @SuppressLint("StaticFieldLeak")
        private static final AppLike INSTANCE = new AppLike();
    }
}
