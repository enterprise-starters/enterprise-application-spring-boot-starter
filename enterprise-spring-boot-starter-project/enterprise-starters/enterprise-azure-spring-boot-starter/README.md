# Enterprise-Azure-Spring-Boot-Starter

Dieser Starter soll alle Azure-spezifische Basis-Konfiguration bündeln.

Erfreulicherweise ist die offizielle Azure Dokumentation relativ gut, hier der [Hauptartikel zu Spring in Azure](https://docs.microsoft.com/de-de/java/azure/spring-framework/?view=azure-java-stable).

## Verwendung Key Vault / Schlüsseltresor

> Es wird davon ausgegangen, dass alle Anwendungen, die diesen Starter nutzen auch einen Key-Vault verwenden.

Dependency (hier eingebunden im Starter, im Service nicht auch einzubinden):

```xml
<dependency>
    <groupId>com.microsoft.azure</groupId>
    <artifactId>azure-keyvault-secrets-spring-boot-starter</artifactId>
</dependency>
```

Folgende Properties müssen im Service definiert werden:

```ini
azure.keyvault.uri=<Key Vault DNS Name>
azure.keyvault.client-id=<Service Principal Client Id>
azure.keyvault.client-key=<Service Principal Client Key>
```

Der Service Principal muss die Berechtigung "Auflisten" für das Key-Vault besitzen.

### Zugriff auf Geheimnisse in der Spring Boot Anwendung
Geheimnisse aus dem Vault werden mit der oben beschriebenen Einrichtung bei Start der Spring Boot Anwendung geladen. Spring nutzt den KeyVault dann als weitere Quelle für die Spring Properties.

Lediglich die Syntax ist leicht anders: Azure-Keys dürfen nur Bindestriche und alphanumerische Zeichen enthalten. D.h. für alles wofür man in Spring normalerweise einen Punkt verwendet, muss man dort Bindestriche verwenden. Beispiel: Das Spring Property `spring.datasource.url` kann über den Azure-Vault-Key `spring-datasource-url` befüllt werden.

### Weiterführende Links
- [Azure Doku Spring Boot & Key Vault](https://docs.microsoft.com/de-de/java/azure/spring-framework/configure-spring-boot-starter-java-app-with-azure-key-vault?view=azure-java-stable)
- [GitHub azure-keyvault-secrets-spring-boot-starter](https://github.com/Microsoft/azure-spring-boot/tree/master/azure-spring-boot-starters/azure-keyvault-secrets-spring-boot-starter)
- [GitHub azure-keyvault-secrets-spring-boot-sample](https://github.com/Microsoft/azure-spring-boot/tree/master/azure-spring-boot-samples/azure-keyvault-secrets-spring-boot-sample)

## Application Insights - Logging und Metriken

Spring Boot Starter für Azure Application Insights (hier im Starter eingebunden - nicht im Service zu definieren): 
```xml
<dependency>
	<groupId>com.microsoft.azure</groupId>
	<artifactId>applicationinsights-spring-boot-starter</artifactId>
</dependency>
```

Zur Definition der zu verwendenen Insights-Ressource muss folgendes Property im einbindenden Service gesetzt werden:

```ini
azure.application-insights.instrumentation-key=<Application Insights Instrumentation Key>
```

### Logging mit Logback

Durch die entsprechende Konfiguration werden die "normalen" Logs zu Azure Application Insights exportiert.

Maven-Dependency (hier im Starter eingebunden - nicht im Service zu definieren):

```xml
<dependency>
    <groupId>com.microsoft.azure</groupId>
    <artifactId>applicationinsights-logging-logback</artifactId>
</dependency>
```

Zusätzlich wird eine Konfiguration des entsprechenden Logback Appenders benötigt. Dafür muss die `logback-spring.xml` des Services um ein weiteres `include` ergänzt werden:

```xml
<include resource="META-INF/logback-azure-default.xml" />
```

Dadurch wird ein Logback-Appender aktiviert, der die Logs zu Azure Application Insights exportiert. Für lokale Starts sollte dies deaktiviert werden, was über folgendes Property möglich ist:

```
enterprise.azure.logging.application-insights-appender-enabled=false
```



Links
- [Azure Doku Spring Boot & Application Insights](https://docs.microsoft.com/de-de/java/azure/spring-framework/configure-spring-boot-java-applicationinsights?view=azure-java-stable)
- [Github azure-application-insights-spring-boot-starter](https://github.com/Microsoft/ApplicationInsights-Java/blob/master/azure-application-insights-spring-boot-starter)

#### Logs angucken im Azure Portal
1. Application Insights Overview
2. Oben in der Leiste "Analytics" auswählen
3. Beispiel Suche: 
    ```
    traces
    | limit 1000
    | where timestamp > ago(10m)
    | where message contains "GET"  
    | where customDimensions contains "demo-service-3b7b83fe-3297-4040-ac36-0e85d1d50e91"
    ```

### Metriken mit Micrometer

Maven-Dependency (hier im Starter eingebunden - nicht im Service zu definieren):

```xml
<dependency>
	<groupId>com.microsoft.azure</groupId>
	<artifactId>azure-spring-boot-metrics-starter</artifactId>
</dependency>
```

#### Metriken angucken im Azure Portal:
1. Application Insights aufrufen
2. -> Metrics
3. Metrik Namespace: Custom, azure.applicationinsights
4. Nächstes Dropdown zeigt dann Spring Boot Metriken

Weiterführende Links:
- [Azure Doku - Metriken mit Spring Boot und Application Insights](https://docs.microsoft.com/de-de/azure/azure-monitor/app/micrometer-java)

### Tests

Während der Ausführung von Tests sollten weder Logs noch Metriken zu Application Insights exportiert werden. Dies kann durch folgende Properties erreicht werden:

```ini
# Disable Application Insights for tests
azure.application-insights.enabled=false
azure.application-insights.web.enabled=false
```

> Das weiter oben genannte Property zum Deaktivieren des Insights-Logback-Appenders muss hier nicht gesetzt werden, da für die Tests eine separate Logback-Datei existiert, in der der zusätzliche Include nicht vorhanden ist.
