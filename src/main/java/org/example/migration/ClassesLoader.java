package org.example.migration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ClassesLoader {

  public <T> Set<Class<T>> findAllClassesMatchingSuper(String packageName, Class<T> superClass) {
    InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(packageName.replaceAll("[.]", "/"));
    if (stream == null) {
      return Set.of();
    }
    InputStreamReader streamReader = new InputStreamReader(stream);
    BufferedReader bufferedReader = new BufferedReader(streamReader);

    Set<Class<T>> matchingClasses = new HashSet<>();

    bufferedReader.lines()
        .forEach(line -> {
          if (!line.endsWith(".class")) {
            return;
          }
          Class<?> nextClass = getClass(line, packageName);
          if (nextClass == null) {
            return;
          }
          if (!superClass.isAssignableFrom(nextClass)) {
            return;
          }

          @SuppressWarnings("unchecked")
          Class<T> matchedClass = (Class<T>) nextClass;

          matchingClasses.add(matchedClass);
        });

    return Collections.unmodifiableSet(matchingClasses);
  }

  private Class<?> getClass(String className, String packageName) {
    try {
      Class<?> nextClass = Class.forName(packageName + "." + className.substring(0, className.lastIndexOf('.')));
      return nextClass;
    } catch (ClassNotFoundException e) {
      // handle the exception
    }
    return null;
  }
}