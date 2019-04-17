package com.jjmoo.appjoint;

import android.content.Context;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;

/**
 * @author Zohn
 */
@SuppressWarnings({"unchecked", "unused"})
public class AppJoint {
    private static Map<Class, Class> sImplClassMap = new HashMap<>();
    private static Map<Class, Object> sInstanceMap = new HashMap<>();

    @NonNull
    public static synchronized <T> T service(Class<T> clazz) {
        T result = (T) sInstanceMap.get(clazz);
        if (null == result) {
            Class<?> impl = sImplClassMap.get(clazz);
            if (null != impl) {
                boolean succeed = false;
                try {
                    result = (T) impl.newInstance();
                    succeed = true;
                } catch (Exception e) {
                    //ignore
                }
                if (!succeed) {
                    try {
                        Constructor constructor = impl.getConstructor(Context.class);
                        result = (T) constructor.newInstance(AppLike.getInstance().getContext());
                    } catch (Exception e) {
                        throw new RuntimeException("no available constructor found", e);
                    }
                }
                sInstanceMap.put(clazz, result);
            } else {
                throw new RuntimeException("no such kind of service!");
            }
        }
        return result;
    }

    private static synchronized <T> void register(
            Class<T> clazz, Class<? extends T> implClazz) {
        sImplClassMap.put(clazz, implClazz);
    }
}
