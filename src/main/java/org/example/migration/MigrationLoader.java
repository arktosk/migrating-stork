package org.example.migration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

public class MigrationLoader {

  Logger logger = LoggerFactory.getLogger(this.getClass());

  AutowireCapableBeanFactory autowireCapableBeanFactory;

  public MigrationLoader(ApplicationContext applicationContext) {
    this.autowireCapableBeanFactory = applicationContext.getAutowireCapableBeanFactory();
  }

  public List<BaseMigration> findAllMigrationsInPackage(String packageName) {
    InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(packageName.replaceAll("[.]", "/"));
    if (stream == null) {
      return List.of();
    }
    // 
    InputStreamReader streamReader = new InputStreamReader(stream);
    BufferedReader bufferedReader = new BufferedReader(streamReader);

    Set<Class<BaseMigration>> migrationClasses = new HashSet<>();

    bufferedReader.lines()
        .forEach(line -> {
          if (!line.endsWith(".class")) {
            return;
          }
          Class<BaseMigration> migrationClass = getMigrationClass(line, packageName);
          if (migrationClass == null) {
            return;
          }
          migrationClasses.add(migrationClass);
        });

    List<BaseMigration> migrations = migrationClasses.stream()
        .map(this::getMigrationWrapper)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .sorted((m1, m2) -> m1.getVersion() - m2.getVersion())
        .toList();

    assertUniqueVersions(migrations);

    return migrations;
  }

  private Class<BaseMigration> getMigrationClass(String className, String packageName) {
    try {
      Class<?> nextClass = Class.forName(packageName + "." + className.substring(0, className.lastIndexOf('.')));

      if (!BaseMigration.class.isAssignableFrom(nextClass)) {
        return null;
      }

      @SuppressWarnings("unchecked")
      Class<BaseMigration> migrationClass = (Class<BaseMigration>) nextClass;

      return migrationClass;
    } catch (ClassNotFoundException ignored) {
      logger.warn("Cannot find \"%s\" class in \"%s\" package", className, packageName);
      // handle the exception
    }
    return null;
  }

  private Optional<BaseMigration> getMigrationWrapper(Class<BaseMigration> clazz) {
    try {
      Constructor<BaseMigration> ctor = clazz.getConstructor();
      BaseMigration instance = ctor.newInstance();

      var beanName = String.format("__%s__", clazz.getSimpleName().toLowerCase());

      autowireCapableBeanFactory.autowireBean(instance);
      autowireCapableBeanFactory.initializeBean(instance, beanName);

      return Optional.of(instance);
    } catch (Exception ignored) {
      logger.warn("Unable to create instance of \"%s\" class", clazz.getName());
      return Optional.empty();
    }
  }

  private void assertUniqueVersions(List<BaseMigration> migrationList) {
    Set<Integer> set = new HashSet<>();

    List<BaseMigration> duplicates = migrationList.stream()
        .filter(migration -> !set.add(migration.getVersion()))
        .toList();

    Assert.isTrue(duplicates.size() == 0, "Not every migration has unique version");
  }

}