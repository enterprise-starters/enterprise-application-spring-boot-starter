# Integration des Enterprise-Starters in eine Spring-Boot-Anwendung

Im Folgenden ist eine Step by Step Anleitung zu finden, wie der Enterprise-Starter in eine Spring-Boot-Anwendung integriert werden kann. Gerade wenn der Enterprise-Starter zum ersten Mal verwendet wird, empfiehlt es sich die Schritte alle selber einmal durchzugehen.

<!-- Für die Eiligen, die direkt loslegen wollen, findet sich weiter unten das Kapitel _Enterprise-Starter Quick Start_. -->

Eine genauere Beschreibungen der Features findet sich in den readme-Dateien der einzelnen Starter-Bibiliotheken:
- [Readme des enterprise-application-spring-boot-starter](../enterprise-application-spring-boot-starter/README.md)
- [Readme des enterprise-application-parent](../enterprise-application-parent/README.md)

Bevor mit der konkreten Entwicklung - also mit diesem Tutorial - losgelegt wird, empfiehlt es sich die [Anleitung zur Einrichtung der Arbeitsumgebung](../enterprise-build-tools/readme.md#einrichten-der-arbeitsumgebung) durchzuarbeiten.

---

# Step by Step Tutorial

Dies ist eine Auflistung der einzelnen Schritte, die notwendig sind für die Integration des Enterprise-Application-Starters in eine Anwendung.

Die notwendigen Schritte im Überblick:
- Application-Starter in pom.xml einbinden
- Property-Dateien anlegen
- Logging mit Logback konfigurieren
- Anpassen der Integration-Test-Klassen

Zur initialen Anlage eines Spring-Boot-Projektes empfiehlt sich der Spring-Initializr, zum Beispiel zu verwenden über start.spring.io. Als Build-Tool sollte dort Maven ausgewählt werden.

## Application-Starter in pom.xml einbinden
Jeder Microservice, der auf dem Enterprise-Starter basieren soll, sollte den `enterprise-application-parent` als Maven-Parent und den `enterprise-application-spring-boot-starter` als Maven-Dependency einbinden.

Beispiel `pom.xml`:

```xml
... 
<parent>
    <groupId>de.enterprise-starters</groupId>
    <artifactId>enterprise-application-parent</artifactId>
    <version>0.1.1</version>
    <relativePath/> <!-- lookup parent from repository -->
</parent>

<dependencies>
    <dependency>
        <groupId>de.enterprise-starters</groupId>
        <artifactId>enterprise-application-spring-boot-starter</artifactId>
        <version>0.1.1</version>
    </dependency>
    ...
</dependencies>
...
```

Alternativ kann der Application-Starter auch ohne den Application-Parent verwendet werden. Dies ist notwendig, wenn ein anderer Maven-Parent verwendet werden soll, oder das Maven-Build-Profil aus dem Application-Parent nicht verwendet werden soll. 

In diesen Fällen empfiehlt es sich, das Artefakt `enterprise-dependencies` als sog. BOM (Bill of Materials) einzubinden. Dadurch werden die Versionen der verwendeten Bibliotheken aus dem Artefakt `enterprise-dependencies` übernommen. Die `pom.xml` muss dann wie folgt angepasst werden:

```xml
... 
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>de.enterprise-starters</groupId>
            <artifactId>enterprise-dependencies</artifactId>
            <version>{AKTUELLE VERSION}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement> 

<dependencies>
    <dependency>
        <groupId>de.enterprise-starters</groupId>
        <artifactId>enterprise-application-spring-boot-starter</artifactId>
        <version>{AKTUELLE VERSION}</version>
    </dependency>
    ...
</dependencies>
...
```

## Property-Dateien anlegen

### Anlegen von Property-Dateien
Welche Property-Dateien benötigt werden, hängt davon ab, welche Umgebungen für den zu entwickelnden Service vorgesehen sind. Das folgende Beispiel nimmt an, dass es die Umgebungen `DEV`, `TEST` und `PROD` gibt. Pro Umgebung muss ein Spring-Profil mit dem Namen der Umgebung aktiviert werden.

Für jedes existierende Profil bzw. für jede vorgesehene Umgebung sollte eine eigene Property-Datei angelegt werden. Zusätzlich eine Datei für Integrationtest, sowie Property-Dateien für die lokale Ausführung.

Unter `src/main/resources` sollten folgende Property-Dateien angelegt werden:
- `application.properties`<br/>
    --> Basis-Properties
- `application-dev.properties`<br/>
  --> Properties für `DEV`-Profil
- `application-test.properties`<br/>
  --> Properties für `TEST`-Profil
- `application-prod.properties`<br/>
  --> Properties für `PROD`-Profil
- `application-dev-local.properties` & `application-{COMPUTERNAME}.properties`<br/>
  --> Werden bei lokaler Ausführung (ohne definiertes Profil) zusätzlich gezogen

Zusätzlich unter `src/test/resources`:
- `application-integrationtest.properties`<br/>
  --> Properties für Integration-Tests (Aktivierung des Profils `INTEGRATION_TEST` notwendig, vgl. Abschnitt _Anpassen der Integration-Test-Klassen_)

### Erläuterungen
Bei Ausführung mit definiertem Spring-Profil wird die Datei `applciation.properties` sowie die Profil-spezifische-Properties-Datei verwendet. Die Profil-spezifischen-Properties überschreiben die Basis-Properties. 

Bei Ausführung ohne definiertem Spring-Profil - typischerweise eine lokale Ausführung auf dem Entwicklungsrechner - werden folgende Properties gezogen:
- `application.properties`
- `application-dev.properties`
- `application-dev-local.properties`
- `application-{COMPUTERNAME}.properties`

Die Überschreibungs-Reihenfolge entspricht der aufgezählten Reihenfolge.


### Definieren einiger benötigter Properties

Der Enterprise-Starter erwartet einige Properties, die dann über einen Actuator-Endpunkt (/manage/info) veröffentlicht werden. Mit dem `@`-Symbol werden hier Maven-Properties referenziert. Dieser Block kann so in die Root-Properties-Datei `application.properties` kopiert werden.

```
enterprise-application.application.name=@project.artifactId@
enterprise-application.project.name=@project.name@
enterprise-application.project.artifactId=@project.artifactId@
enterprise-application.project.groupId=@project.groupId@
enterprise-application.project.version=@project.version@
enterprise-application.project.description=@project.description@
```


## Logging mit Logback konfigurieren
Zur Konfiguration des Loggings mit dem Logging-Framework _Logback_ muss die Datei `src/main/resources/logback-spring.xml` angelegt werden, mit folgendem Inhalt:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration>
<configuration scan="true" scanPeriod="30 seconds">
    <include resource="META-INF/logback-default.xml" />

    <!-- TODO: Package ersetzen mit dem des eigenen Codes -->
    <logger name="de.enterprise.spring.boot" level="${log_level:-DEBUG}" />
    <!-- Beispiel, wie man das Log-Level einer externen Lib konfigurieren kann -->
    <logger name="org.springframework.web" level="info" />
	
</configuration>
```
Diese Datei verweist auf eine `logback-default.xml`, die im application-starter enthalten ist.

> Achtung: Statt `de.enterprise.spring.boot` muss das eigene Package eingesetzt werden, für das der LogLevel definiert werden soll.

Zusätzlich wird für die Tests eine zweite Logback-Datei benötigt. Das liegt daran, dass die Log-Pattern für die Log-Appender in den Default-Properties definiert werden. Das Starten des Spring-Contextes in Integration-Tests funktioniert etwas anders, als normal und die Pattern können in diesem Fall nicht aus den Properties gezogen werden. Daher muss eine zweite Datei `src/test/resources/logback-spring.xml` mit folgendem Inhalt angelegt werden:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration>
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern> %d{HH:mm:ss.SSS} %level %X{traceId} %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- TODO: Package ersetzen mit dem des eigenen Codes -->
    <logger name="de.enterprise.spring.boot" level="DEBUG" />
    
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>

</configuration>
```

## Anpassen der Integration-Test-Klassen

Eine Integrationtest-Klasse muss wie folgt aussehen:

```java
@SpringBootTest
@ActiveProfiles("integrationtest")
public class DemoServiceApplicationTest {

    @Test
    public void contextLoads() {
    }

}
```
Erklärungen:
- `@SpringBootTest` <br/>
Definiert einen Spring-Boot-Test. Der komplette Application-Context wird dabei hochgefahren.
- `@ActiveProfiles("integrationtest")` <br/>
Aktiviert das Profil `integrationtest` für den Test. Dies sorgt dafür, dass die Property-Datei `src/test/resources/application-integrationtest.properties` gezogen wird.
<!-- Wird benötigt, damit die Default-Properties aus dem Starter auch bei den Tests greifen

TODO Malte, das ist nicht mehr der Fall, oder? -->

> Empfehlung: Anlegen einer abstrakten Testklasse mit diesen zwei Annotationen und jede Integration-Test-Klasse davon erben lassen. So spart man sich die explizite Angabe in den einzelnen Testklassen.

## Was bringt der Starter mit? Was muss im Projekt nicht mehr definiert werden?

Folgende Dependencies müssen in der Projekt pom.xml nicht mehr definiert werden, da sie durch Einbinden des Enterprise-Starters bereits gezogen werden:
  - spring-boot-starter-web
  - spring-boot-starter-actuator
  - spring-boot-starter-security
  - spring-boot-starter-test
  - spring-boot-starter

Ebenfalls müsen die Maven Properties für das Encoding und die Java Version nicht mehr gesetzt werden.

Der Enterprise-Application-Starter definiert viele Default-Properties, die dann in den Properties der einzelnen Services nicht mehr gestezt werden müssen. Die Default Properties sind in der Datei [default.properties](../enterprise-application-spring-boot-starter/src/main/resources/META-INF/application-default.properties) zu finden.


# Best Practices
### Package Struktur und Klassennamen
Am einfachsten ist es, edes neue Projekt über start.spring.io erzeugen. Dann werden die folgenden drei Punkte automatisch berücksichtigt:
- Basis-Package sollte spezifisch sein, nicht nur `de.enterprise`, sondern eher `de.enterprise.smartoffice.appointmentmanagementservice`. Als Vorgehensweise kann man die Maven-GroupId und die Maven-ArtefactId hintereinanderhängen.
- Die Application-Klasse sollte immer direkt in dem Basis-Package liegen, alle weiteren Klassen der Anwendung im gleichen Package oder in Sub-Packages, damit der Spring-ComponentScan wie geplant funktioniert. Siehe dazu auch https://docs.spring.io/spring-boot/docs/current/reference/html/using-boot-structuring-your-code.html
- Der Name der Application-Klasse sollte immer auf `Application` enden.

### Verwendung von Lombok
- Zur Vermeidung von Boilerplate Code (Getter, Setter, etc) sollte Lombok verwendet werden.
- Die Installation und notwendige Konfiguration ist in der [readme von enterprise-build-tools](../enterprise-build-tools/readme.md#lombok) beschrieben.
- Bei Verwendung des enterprise-application-parent ist Lombok bereits als Dependency konfiguriert.

### Erzeugen eines Projekt-Banners

Ein Banner kann unter `src/main/resources` als `banner.txt` abgelegt werden. Dieser wird beim Start der Anwendung im Log ausgegeben. --> 
Sieht schön aus und macht Spaß :)

```
#
#     __   ___        __      __   ___  __          __   ___ 
#    |  \ |__   |\/| /  \    /__` |__  |__) \  / | /  ` |__  
#    |__/ |___  |  | \__/    .__/ |___ |  \  \/  | \__, |___ 
#                                                          
#    :: Spring Boot :: ${spring-boot.formatted-version}
#    :: ${info.build.artifact} :: ${info.build.formatted-version}
#
```

Der oben gezeigte Banner kann als Vorlage verwendet werden. Hier zwei Websites, mit deren Hilfe der Banner erzeugt wurde:
- Bilddatei zu ASCII-Art: https://www.text-image.com/convert/ascii.html
- Text zu ASCII-Art (gewählte Schriftart: _Stick Letters_ ): http://patorjk.com/software/taag/#p=display&f=Stick%20Letters&t=Demo%20Service 

<!--
# Enterprise-Starter Quick Start
Für einen schnellen Start kann das Projekt _demo-service_ als Vorlage verwendet werden. 

Hier müssen lediglich die Maven-Group-Ids vom Applciation Stater und Application-Parent in der pom.xml angepasst werden (siehe Kapitel _Application-Starter und Application-Parent in pom.xml einbinden_ weiter oben im Tutorial).

Zusätzlich ist eine Umbenennung der Packages und der Anwendung sinnvoll.
-->