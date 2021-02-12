# Enterprise Build Tools

Dieses Artefakt enthält grundlegende Konfigurationsdateien für den Buildprozess:

- CheckStyle Konfiguration
- Templates für die automatische Generierung von Swagger-Api-Dokumentation

# CheckStyle-Regeln
Im Ordner `src\main\resources\checkstyle` sind die Checkstyle-Rules und -Suppressions zu finden, die während des Build-Prozesses verwendet werden. Die Integration von Checkstyle in den Build-Prozess (maven-checkstyle-plugin) ist in den Parent-Projekten definiert.

# Templates für die automatische Generierung von Swagger-Api-Dokumentation
Der Ordner `src\main\resources\swagger` enthält Format-Vorlagen für die automatisch generierbare Swagger-Schnittstellendokumentation. Der Integration der Swagger-Generierung in den Build-Prozess (swagger-maven-plugin) ist im Application-Parent-Projekt definiert.
