package org.example.serverless;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarControl {
  final static int CLASS_LENGTH = 6;
  private Method method;
  private Constructor<?> constructor;

  public JarControl(String path, String entryPoint) {

    JarFile jarFile = null;
    ClassLoader cl = null;
    try {
      File file = new File(path);
      if (!file.exists()) {
        System.out.println("jar not exist.");
        return;
      }
      jarFile = new JarFile(file);
      Enumeration<JarEntry> e = jarFile.entries();
      URL[] urls = { new URL("jar:file:" + file + "!/") };


      if (this.getClass().getClassLoader() == null) {
        cl = URLClassLoader.newInstance(urls);
      } else {
        cl = URLClassLoader.newInstance(urls, this.getClass().getClassLoader());
      }

      if (cl == null) {
        System.out.println("Failed to initialize the classloader");
        return;
      }


      Class<?> claz = cl.loadClass(entryPoint);
      method = claz.getDeclaredMethod("handle", InputStream.class, OutputStream.class);
      constructor = claz.getConstructor();
    } catch (IOException | ClassNotFoundException | NoSuchMethodException e) {
      e.printStackTrace();
    }
  }

  public void invoke(Object... args)
      throws IllegalAccessException, InvocationTargetException, InstantiationException {
    method.invoke(constructor.newInstance(), args);
  }

  public Boolean isReady() {
    return constructor != null;
  }
}
