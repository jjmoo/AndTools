package com.jjmoo.appjoint;

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
                try {
                    result = (T) impl.newInstance();
                    sInstanceMap.put(clazz, result);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
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
