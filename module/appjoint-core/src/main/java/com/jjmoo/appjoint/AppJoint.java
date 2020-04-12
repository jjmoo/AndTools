package com.jjmoo.appjoint;

import android.app.Application;
import android.content.Context;

import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Zohn
 */
@SuppressWarnings({"unchecked", "unused"})
public class AppJoint {
    private static Map<Class, Class> sImplClassMap = new HashMap<>();
    private static Map<Class, Object> sInstanceMap = new HashMap<>();

    @Nullable @CheckResult
    public static synchronized <T> T service(Class<T> clazz) {
        T result = (T) sInstanceMap.get(clazz);
        if (null == result) {
            Class<?> impl = sImplClassMap.get(clazz);
            if (null != impl) {
                try {
                    Constructor<?>[] constructors = impl.getConstructors();
                    for (Constructor<?> constructor : constructors) {
                        Class<?>[] params = constructor.getParameterTypes();
                        if (0 == params.length) {
                            result = (T) impl.newInstance();
                        } else if (1 == params.length) {
                            if (Context.class == params[0] || Application.class == params[0]) {
                                result = (T) constructor.newInstance(
                                        AppLike.getInstance().getContext());
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (null == result) {
                    throw new RuntimeException("no available constructor found !");
                }
                sInstanceMap.put(clazz, result);
            }
        }
        return result;
    }

    private static synchronized <T> void register(
            Class<T> clazz, Class<? extends T> implClazz) {
        sImplClassMap.put(clazz, implClazz);
    }
}
