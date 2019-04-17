package com.jjmoo.appjoint;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Descriptions
 * <p><br>
 *
 * @author Zohn
 *
 */
@SuppressWarnings("unused")
public class AppLike {
    private List<Application> mApplications = new ArrayList<>();
    private Context mBase;

    private AppLike() {}

    public static AppLike getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public Application getContext() {
        return (Application) mBase.getApplicationContext();
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

    protected synchronized void addModuleApplication(Application application) {
        mApplications.add(application);
    }

    private static class SingletonHolder {
        @SuppressLint("StaticFieldLeak")
        private static final AppLike INSTANCE = new AppLike();
    }
}
