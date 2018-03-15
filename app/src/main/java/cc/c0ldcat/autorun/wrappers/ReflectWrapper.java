package cc.c0ldcat.autorun.wrappers;

import cc.c0ldcat.autorun.utils.CommonUtils;
import cc.c0ldcat.autorun.utils.LogUtils;
import cc.c0ldcat.autorun.utils.ReflectHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ReflectWrapper {
    private Object object;

    public ReflectWrapper(Object object) {
        this.object = object;
    }

    public Object getAttribute(String name) throws NoSuchFieldException, IllegalAccessException {
        return ReflectHelper.getPrivateObject(getObjectClass(), name, object);
    }

    public Object getAttributeIfExist(String name) {
        try {
            return getAttribute(name);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    public Method getMethod(String methodName, Class<?> ...params) throws NoSuchMethodException {
        return ReflectHelper.getPrivateMethod(getObjectClass(), methodName, params);
    }

    public Object invokeMethodIfAccessable(String methodName, Object ...params) {
        try {
            return invokeMethod(methodName, params);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LogUtils.e(CommonUtils.exceptionStacktraceToString(e));
            return null;
        }
    }

    public Object invokeMethod(String methodName, Object ...params) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        List<Class<?>> paramsClassList = new ArrayList<>();

        for (Object param : params) {
            paramsClassList.add(param.getClass());
        }

        return invokeMethod(getMethod(methodName, paramsClassList.toArray(new Class[paramsClassList.size()])));
    }

    public Object invokeMethod(Method method, Object ...params) throws IllegalAccessException, InvocationTargetException {
        return method.invoke(object, params);
    }

    public Class<?> getObjectClass() {
        return object.getClass();
    }

    public Object getObject() {
        return object;
    }

}
