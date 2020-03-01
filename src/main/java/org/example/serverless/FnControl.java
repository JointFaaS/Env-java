package org.example.serverless;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FnControl {
  private Method method;
  private Constructor<?> constructor;
  private Object instance;

  public FnControl(String className, String entryPointFn) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
    Class fnClass = Class.forName(className);
    method = fnClass.getDeclaredMethod(entryPointFn, InputStream.class, OutputStream.class);
    constructor = fnClass.getConstructor();
    instance = constructor.newInstance();
  }

  public void invoke(Object... args)
      throws IllegalAccessException, InvocationTargetException, InstantiationException {
    method.invoke(instance, args);
  }
}
