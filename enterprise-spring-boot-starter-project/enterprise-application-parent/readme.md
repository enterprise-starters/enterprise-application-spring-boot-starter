# Enterprise Application Parent

Dieses Artefakt enthält die Maven-Basiskonfiguration für Services. Folgende Konfigurationen werden vorgenommen
- Definition von Maven-Properties
- Einbinden von Dependencies, die in allen Services benötigt werden
- Definition eines Maven-Build-Profils

Zu jedem der Punkte gibt es unten ein separates Kapitel. Wird der Enterprise-Application-Parent als Maven-Parent eigebunden, werden die hier vorgenommenen Konfigurationen automatisch geerbt.

Die Definition von Dependency-Versionen und Maven-Plugin-Versionen sowie generelle Konfiguration von Maven-Plugins ist in das Artefakt _enterprise-dependencies_ ausgelagert.

## Maven-Properties
- project.build.sourceEncoding=UTF-8
- project.reporting.outputEncoding=UTF-8
- java.version=11
- jacoco.rule.complexity.coveredratio=0.300
  <br> Definiert die minimale Code-Coverage, die beim Build vom `jacoco-maven-plugin` überprüft wird.

## Dependencies
- spring-boot-starter-test
- lombok
- spring-boot-configuration-processor
- swagger-annotations
- jaxb-api

## Maven-Build-Profil
Es wird ein Maven-Profil (Id: `build`) mit folgenden Plugins definiert:
- maven-checkstyle-plugin
- maven-pmd-plugin
- swagger-maven-plugin
- spring-boot-maven-plugin
- maven-javadoc-plugin
- jacoco-maven-plugin
- versions-maven-plugin

Zum Teil werden die Plugins im Artefakt _enterprise-dependencies_ konfiguriert.
