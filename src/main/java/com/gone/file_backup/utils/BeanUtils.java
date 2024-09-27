package com.gone.file_backup.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

public class BeanUtils extends org.springframework.beans.BeanUtils {

    public static <T> T copyProperties(Object src, Class<T> clazz) {
        try {
            T instance = clazz.getConstructor().newInstance();
            copyProperties(src, instance);
            return instance;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> List<T> copyList(List<?> list, Class<T> clazz) {
        List<T> collect = list.stream()
                .map(obj -> copyProperties(obj, clazz))
                .collect(Collectors.toList());
        return collect;
    }
}
