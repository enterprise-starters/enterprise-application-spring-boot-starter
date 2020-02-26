Verwendung
==========
Um die Features des Enterprise-Spring-Boot-Application-Starters zu nutzen, muss dieser als Dependency eingebunden werden.

```xml
<dependency>
	<groupId>de.enterprise.spring-boot</groupId>
	<artifactId>enterprise-application-spring-boot-starter</artifactId>
	<version>{AKTUELLE VERSION}</version>
</dependency>
```	

---


Features
========
- Abstract Application für vereinfachten Start mit Default-Properties
- Allgemein gültige Klassen
  * spezielle Exceptions inkl. ExceptionHandler
  * Validierungs-Annotation für SpEL
- Automatische Rest-Template-Konfiguration
- Clustering Unterstützung über Hazelcast
- Logging
  * Logging eingehender Request/Response Informationen
  * Logging ausgehender Request/Response Informationen
  * Default Logback Konfiguration
- Metriken
  * Erweiterte Metriken
  * Micrometer Common-Tags
- Tracing Mechanismus
- Resilience4j als Circuitbreaker-Framework
- Swagger-Integration

---

Details zu den Features
=======================

## Abstract Application für vereinfachten Start mit Default-Properties

### AbstractApplication
Bei der "normalen" Spring-Boot Initialisierung wird per Default kein Spring Profil aktiviert. Um eine komfortablere Entwicklung in der lokalen Entwicklungsumgebung zu ermöglichen, werden zusätzliche Profile per Default aktiviert. Dadurch kann jeder Entwickler sofort mit der lokalen Entwicklung und der passenden Umgebung loslegen.

Diese Anpassungen können durch Erweiterung der Klasse `AbstractApplication` verwendet werden. Eine eigene `*Application` Klasse sieht dann wie folgt aus: 

```java
public class DemoServiceApplication extends AbstractApplication {
	public static void main(String[] args) {
		new DemoServiceApplication().run();
	}
}
```

Per Default sind dann bei lokaler Entwicklung die Profile `dev`, `dev-local` und `<Computername>` aktiviert. Das ermöglicht das komfortable Setzen von lokalen Konfigurationen.

> __Hinweis für Linux und Mac-User__: Damit dieses Feature wie vorgesehen funktioniert, muss die Umgebungsvariable `COMPUTERNAME` gesetzt werden. Bei Windows ist diese automatische gefüllt.

### Default-Properties

Die Datei `src/main/resources/application-default.properties` enthält Default-Properties, die bei jedem Service greifen, der diesen Starter einbindet und die `AbstractApplication` nutzt.

In den Default-Properties werden eingebundene Spring Boot Starter konfiguriert oder Default-Werte für die Features des Enterprise-Application-Starters hinterlegt.

Unter anderem sind folgende Konfigurationen enthalten:
- __Spring Boot Actuator__: Der Management-Endpoint wird auf `/manage` festgelegt, das heißt, dass alle Actuator Endpunke unterhalb davon liegen. Als Beispiel `/manage/info` - für diesen Endpunkt werden hier auch zusätzliche Informationen konfiguriert.
- __Datenbank__: Defaults (TODO: verschieben in den geplanten enterprise-jpa-spring-boot-starter)
- __Logging__: zum Beispiel die verschiedenen Log-Patterns
- __Clustering__: Default Hazelcast Konfiguration


## Allgemein gültige Klassen

### Spezielle Exceptions inkl. ExceptionHandler

Der Starter enthält spezielle Exception-Typen im Package `de.enterprise.spring.boot.application.starter.exception`. 

In Webanwendungen ist für die folgenden Exceptions ein automatisches Mapping auf Http-Stati konfiguriert:

|Exception					|Http-Status|
| ------------- 			| -----:|
|BadRequestException		|400|
|ResourceNotFoundException	|404|
|ValidationException		|422|
|TechnicalException			|500|

Das bedeutet, dass zum Beispiel in vom Controller aufgerufenen Service-Methoden diese Exceptions geworfen werden können und die Response den entsprechenden Http-Status erhält - ohne dass ein Catch der Exception im Controller notwendig ist.

Neben dem Mapping auf den Http-Status werden die Felder `message` und `code`, die alle hier genannten Exeption-Typen haben, im  Responsebody zurückgegeben. Die Konfiguration zum Exception-Handling ist in der Klasse `GlobalExceptionRestControllerHandler` zu finden.

Neben dem Handling der lokal definierten Exceptions wird das Spring-Standard Exception-Handlings leicht geändert: Bei Auftreten von `MethodArgumentNotValidExcpetion`s (werden geworfen bei Validierungsfehlern von `@Valid` annotierten Objekten im Controller) wird der Http-Status 422 zurückgegeben.

### Validierungs-Annotation für SpEL (Spring Expression Language)

Im Package `de.enterprise.spring.boot.application.starter.validation` befindet sich die Validierungsannotation `@SpELAssert`, die einen SpEL-Ausdruck auswertet. Diese ist zum Beispiel hilfreich für Validierungen, die sich auf mehrere Felder beziehen. Sie kann verwendet werden, wie "normale" Validierungsannotationen aus dem Package `javax.validation.constraints`, also zum Beispiel `@NotNull`. 

Nutzungsbeispiel:
```java
@SpELAssert(value = "strasse != null || postfach != null", message = "Either 'strasse' or 'postfach' must be not null")
public class Adresse {
	private String strasse;
	private String postfach;
	...
}
```

## Automatische RestTemplate-Konfiguration

Für jedes benötigte RestTemplate muss zunächst eine leere Konfigurationsklasse angelegt werden, die `HttpClientConfig` erweitert:
```java 
@ConfigurationProperties("restservices.booking.api")
public class BookingApiProperties extends HttpClientConfig {}
```
Dadurch kann das anzulegende RestTemplate über die Properties konfiguriert werden. Das einzige zwingend benötigte Property ist `base-address`. Optional können noch Basic-Auth-Credentials angegeben werden, sowie die Defaults für die Logging-Einstellungen und Timeouts überschrieben werden. 

```ini
# Pflicht
restservices.booking.api.base-address=https://book.me
# Optional: Basic-Auth-Credentials
restservices.booking.api.basic-auth.username=apiUser
restservices.booking.api.basic-auth.password=<PW>
# Überschreiben von Defaults
restservices.booking.api.connect-timeout=5
restservices.booking.api.read-timeout=10
restservices.booking.api.connection-request-timeout=12
restservices.booking.api.log-details-enabled=false
```
Für die Erzeugung eines RestTemplates soll dann die Klasse `ExtendedRestTemplate` genutzt werden. Im einfachsten Fall sieht das dann so aus:
```java
@Bean
public ExtendedRestTemplate bookingServiceRestTemplate(BookingApiProperties apiProperties) {
	return apiProperties
		.createPreConfiguredRestTemplateBuilder()
		.build(ExtendedRestTemplate.class);
}
```
Dabei werden dann die über die Properties definierten Werte automatisch gesetzt - im Gegensatz zum herkömmlichen Spring RestTemplate werden hier auch Default-Timeout-Werte gesetzt.

Über die Methode `createPreConfiguredRestTemplateBuilder` gibt es die Möglichkeit vor Erzeugen des ExtendedRestTemplates noch auf den RestTemplateBuilder zuzugreifen um weitere Konfiguration vorzunehmen.

Die beschriebene Implementierung ist im Package `de.enterprise.spring.boot.application.starter.httpclient` zu finden.

## Clustering Unterstützung über Hazelcast

Hazelcast wird als Basis für die Clusterfähigkeit verwendet. Für den konkreten Einsatz muss im Projekt, neben der enterprise-application-spring-boot-starter Dependency, auch noch die Hazelcast Depencency 

```xml
<dependency>
	<groupId>com.hazelcast</groupId>
	<artifactId>hazelcast</artifactId>
</dependency>
```

hinzugefügt werden und folgende Properties gesetzt werden

```ini
enterprise-application.hazelcast.group-name=demo
enterprise-application.hazelcast.group-password=demoPW
```

Jedes Projekt muss beim `group-name` und `group-password` einen neuen Wert vergeben. Damit es einfacher ist werden diese Werte automatisch wie folgt befüllt (definiert in `application-default.properties`):

```ini
enterprise-application.hazelcast.group-name=${enterprise-application.project.artifact-id}
enterprise-application.hazelcast.group-password=PW4${enterprise-application.project.artifact-id}
```

Unterschiedliche Gruppen-Namen sind wichtig, damit die Cluster-Gruppen korrekt auseinander gehalten werden. (Ein Knoten von Service A soll nur mit anderen Knoten von Service A sprechen, nicht mit Knoten von Service B.) Eine gemeinsame Gruppe über unterschiedliche Services ist nur dann notwendig wenn diese über Hazelcast Daten austauschen sollen.

### Cluster Discovery

Die Ermittlung der Cluster-Member ist in der aktuellen Umsetzung über zwei Wege möglich. Via TCP, AWS ECS (benötigt zusätzlich den `enterprise-aws-spring-boot-starter`) oder Kubernetes (benötigt zusätzlich den `enterprise-kubernetes-spring-boot-starter`). 

Dieser Wert lässt sich in den Properties wie folgt festlegen.

```ini
enterprise-application.hazelcast.discovery-type=Tcp|AwsEcs|Kubernetes
```

Als default ist `Tcp` in der Klasse `HazelcastProperties` gesetzt. 

Für den konkret verwendeten Discovery-Typen muss eine Implementierung des Interfaces `HazelcastDiscoveryConfigurer` als Bean verfügbar sein.

#### TCP Discovery
Hier implementiert in der Klasse `TcpHazelcastDiscoveryConfigurer`. 
Für Details sei auf die offizielle [Dokumentation von Hazelcast](http://docs.hazelcast.org/docs/3.10.2/manual/html-single/index.html#discovering-members-by-tcp) verwiesen.

#### AWS-ECS Discovery
Um die AWS-ECS Discovery zu nutzen, muss der `enterprise-aws-spring-boot-starter` zusätzlich zum `enterprise-application-spring-boot-starter` eingebunden werden. Siehe dazu die readme des `enterprise-aws-spring-boot-starter`s.

#### Kubernetes Discovery
Um die Kubernetes Hazelcast-Discovery zu nutzen, muss der `enterprise-kubernetes-spring-boot-starter` zusätzlich zum `enterprise-application-spring-boot-starter` eingebunden werden. Siehe dazu die readme des `enterprise-kubernetes-spring-boot-starter`s.

### Scheduling

Das Scheduling kann über die `@Scheduled` Annotation durchgeführt werden (wichtig: `@EnableScheduling` in Konfigurations-Klassen nicht vergessen). Es findet ein Abgleich über die Cluster-Knoten statt, das bedeutet, dass ein Task zur definierten Zeit auch immer nur genau einmal innerhalb des Clusters ausgeführt wird. 

Achtung: Das funktioniert nur mit Cron-Ausdrücken innerhalb der `@Scheduled` Annotation, also über das Attribut `cron` - wie im Beispiel unten. Bei Verwendung von `delay` oder `fixRate` funktioniert die Synchronisation im Cluster nicht, da es dort keinen "global" definierten Ausführungszeitpunkt gibt (Ausführungszeitpunkt ist abhängig vom Startzeitpunkt der Anwendung). Starten zwei Anwendungen eines Clusters nicht zu genau der gleichen Zeit, wovon auszugehen ist, dann laufen die Tasks immer auf beiden Cluster-Knoten. 

Anwendungsbeispiel:
```java
@Scheduled(cron = "${enterprise-application.example.cron:*/5 * * * * *}")
public void scheduledMethod(){
	...
}
```

Für das Testen empfiehlt es sich, die Crons zu deaktivieren (bzw. die Ausführung auf sehr selten zu stellen) und die Methode im Test gezielt auszuführen. Dies gelingt durch Hinzufügen folgender Zeile in die Integrationtest-Properties:

```ini
enterprise-application.example.cron=0 0 23 1 1 ?
```

Dadurch wird der entsprechende Cron-Job nur am 01.01. jeden Jahres um 23 Uhr ausgeführt.

## Logging

### Logging eingehender Request/Response Informationen
Für die Nachverfolgung von eingehenden Request/Response Aktivitäten (d.h. eingehende Anfragen über einen Controller) können alle Requests mitgelogged werden.
Dafür gibt es den Logger "request-logger". Per default ist dieser auf `INFO` Level gesetzt, was ein Logging jedes einzelnen Requests/Responses bedeutet. 

Die Implementierung dazu befindet sich im Package `de.enterprise.spring.boot.application.starter.logging`.
Über Properties unterhalb des Prefixes `enterprise-application.logging` lässt sich eine Verfeinerung des Outputs vornehmen.

### Logging ausgehender Request/Response Informationen

Auch für ausgehende Requests/Responses, also für Requests, die aus der Anwendung ausgehen (via RestTemplate) kann Logging aktiviert werden. Und zwar mit folgendem Property:

```ini
enterprise-application.logging.log-outgoing-request-details-enabled=true
```

Das Logging greift nur bei Verwendung der `ExtendedRestTemplate`s und definiert, ob das Logging dafür generell aktiviert ist. Durch das Property `log-details-enabled` (vgl. Abschnit _Automatische RestTemplate-Konfiguration_)
kann diese generelle Einstellung pro ExtendedRestTemplate noch explizit überschrieben werden.

### Default Logback Konfiguration

Der Starter enthält eine Logback Konfiguration in der `logback-default.xml`. Die grundlegenden Logging-Einstellung sind darin ausgelagert, sodass die Logging-Konfiguration in der tatsächlichen Anwendung sehr übersichtlich ausfallen kann. Beispiel einer `logback-spring.xml` im Service:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="true" scanPeriod="30 seconds">
	<include resource="META-INF/logback-default.xml" />
	<logger name="de.enterprise.service.demo" level="${log_level:-INFO}" />
</configuration>
```
Features der Default-Logback:
- Console-Appender (name: `CONSOLE`)
- Container-Console-Appender (spezifisches für Logformat für Docker-Container) (name: `CONTAINER_CONSOLE`)
- Rolling-File-Appender (name: `LOGFILE`)

Die Logback Datei sollte im Service im Ordner `src/main/resources` abgelegt werden.

Damit eine leichte Anpassung des Log-Outputs zu erzielen ist gibt es für die einzelnen Appender extra Properties die das Log-Output-Pattern festlegen:

- CONSOLE: `enterprise-application.logging.appender-console-pattern`
- CONTAINER_CONSOLE: `enterprise-application.logging.appender-container-console-pattern`
- LOGFILE: `enterprise-application.logging.appender-logfile-pattern`

In den Default Properties ist für jeden der Appender eine Konfiguration hinterlegt.

Für die Tests sollte zusätzlich eine separate Logback Datei unter `src/test/resources` abgelegt werden, die nicht auf der default-logback basiert. Der Grund dafür ist das abweichende Inititalisierungsverhalten von Spring-Boot bei Integration-Tests. Das Log-Pattern welches in der default-logback definiert ist, wird beim normalen Anwendungsstart aus den Properties gezogen - dies funktioniert bei den Tests nicht.

Vorlage für eine minimale Test-Logback:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
	<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
		<encoder>
			<pattern> %d{HH:mm:ss.SSS} %level %X{traceId} %logger{36} - %msg%n
			</pattern>
		</encoder>
	</appender>
	<logger name="de.enterprise" level="info" />
	<root level="INFO">
		<appender-ref ref="CONSOLE" />
	</root>
</configuration>
```

## Metriken

> __Metriken in Spring Boot__
>
>Spring Boot Actuator bindet die Metrik-Sammlungs-Bibliothek [Micrometer](https://micrometer.io/) ein. Micrometer ist _dimensional-first_, das heißt alle Metriken werden mehrdimensional gesammelt und übermittelt. Bedeutet unterhalb eines Metriknamens, wie in den Beispielen, gibt es eine Reihe von Tags (Dimensionen) die konkrete Werte beinhalten.
>
> Der Actuator-Endpunkt `/manage/metrics` stellt alle Metrik-Namen zur Verfügung, die man dann einzeln abfragen kann, zum Beispiel `/manage/metrics/http.client.requests`.
>
> Weitere Einschränkung dann auf die Tags möglich über Request-Parameter.

### Erweiterte Metriken

Über die Spring Boot Actuator Einbindung bzw. diverser eigener Actuator Metrik Implementierungen werden alle möglichen Messungen vorgenommen. Hier ein kleiner Auszug:

Unter `hikaricp.connections` gibt es alle möglichen Messwerte zu DB-Connections, wie z.B.

- `hikaricp.connections.usage.max` -> Maximale Anzahl verwendeter DB-Connections
- `hikaricp.connections.usage.avg` -> Durchschnittlich verwendete Anzahl DB-Connections

Unter `http.client.requests` und `http.server.requests` gibt es Messwerte zu ausgehenden bzw. eingehenden HTTP Requests, wie z.B.

- `http.client.requests.avg` -> Durschnittliche Request-Dauer

### Micrometer Common-Tags

Für jede Metrik werden über die Konfiguration `ActuatorAutoConfiguration` allgemeine Tags hinzugefügt. Diese dienen der besseren Separierung der einzelnen Metrikwerte.
Per default werden im Tag "profiles" alle aktivierten Spring Profiles übermittelt. Das sollte bei einer Laufzeitumgebung (nicht Entwicklung) immer nur genau eines sein. 

Über das Property

```ini
enterprise-application.actuator.instance-common-tag-value=${hostname}
```
wird außerdem noch der tag "instance" hinzugefügt. Wird wie im Beispiel der Wert auf ${hostname} gesetzt, so wird der Wert aus der Umgebungsvariablen "hostname" gesetzt. Soll kein "instance"-Tag geschrieben werden, aktueller Default, so kann dieses Property ohne Wert angegeben werden.

Über einen weiteren common Tag (im Beispiel der Default) wird die Versionsnummer des aktuellen Services mit in die Metriken geschrieben:

```ini
enterprise-application.actuator.version-common-tag-value=${enterprise-application.project.version}
```

## Tracing Mechanismus

Im Starter ist per Default ein Tracing Mechanismus eingebaut/aktiviert. Das bedeutet dass jede Aktion (eingehender Request, Scheduled Job, etc.) mit einer TraceId versehen wird. Diese wird im MDC der SLF4 Implementierung gehalten. Über die TraceId, welche per Default im Log mit erscheint, können alle Log-Einträge ermittelt werden die durch eine der Aktionen ausgelöst wurde.

Die Implementierung befindet sich im Package  `de.enterprise.spring.boot.application.starter.tracing`.

Das Verhalten kann über Properties beeinflusst werden:

```ini
# Default properties 
enterprise-application.tracing.enabled=true
enterprise-application.tracing.request-header-name=X-Trace-Id
enterprise-application.tracing.application-name=${enterprise-application.application.name}
```

Die TraceId wird bei ausgehenden RestTemplate Calls automatisch als Header mit hinzugefügt. So ist eine Verfolgung über die TraceId auch über Services hinweg möglich.

## Resilience4j als Circuitbreaker-Framework
Als Circuitbreaker-Framwork ist die Bibliothek Resilience4j eingebunden. Diese bietet unter anderem folgende Features:
- Erzeugen der in den Properties definierten Circuitbreaker
- Erzeugen der mit `@CircuitBreaker` definierten Circuitbreaker
- Integration von Health-Informationen der Circuitbreaker in den Health-Endpoint (`/manage/health` - authorisiert)
- Circuitbreaker-Manage-Endpunkte:
  -  `/manage/circuitbreakers` zeigt alle konfigurierten Circuitbreaker
-  `/manage/circuitbreaker-events/{circuitbreaker-name}` zeigt die Events eines konkreten Circuitbreakers

Siehe auch Projektseite von resilience4j auf [github](https://github.com/resilience4j/resilience4j).


## Swagger-Integration

### Automatische Generierung von Swagger-API-Dokumentation

Durch Einbinden des `swagger-maven-plugin` im enterprise-application-parent wird beim Build automatisch eine `swagger.json` sowie eine `api.html` erzeugt. Diese Dateien sind im Ordner `/target/classes/docs/` zu finden.

Durch Annotationen aus der Bibilothek `swagger-annotations` kann der Inhalt der generierten Swagger-Dokumentation optimiert werden.

Die Vorlagen für die HTML-Generierung liegen im Projekt `enterprise-build-tools`.

### Swagger Actuator Endpunkt

In der laufenden Anwendung werden die generierten Swagger-Dateien über den Endpunkt `/manage/api` zur Verfügung gestellt. Je nach verwendetem `Accept`-Header wird entweder die JSON- oder die HTML-Datei zurückgegeben.

Die Implementierung für diesen Endpunkt ist in der Klasse `DocumentationMvcEndpoint` zu finden.
