# Tutorial Clustering mit Hazelcast

__Überblick__

Dieses Tutorial zeigt, wie Clustering mit Hazelcast in einer Anwendung aktiviert werden kann.
Das Tutorial ist so aufgebaut, dass jeder Schritt nachprogrammiert und ausprobiert werden kann.

Der Quellcode zu diesem Tutorial findet sich in diesem [Git Repository](https://gitlab.enterprise.de/enterprise-essentials/tutorial-clustering). Die einzelnen vorgestellten Schritte können in der Commit-Historie nachvollzogen werden.

__Was ist Clustering?__

Um Ausfallsicherheit zu gewährleisten oder um eine Anwendung zu skalieren werden von einer Anwendung häufig mehrere Instanzen in einer Umgebung deployed. Die Gesamtheit der Instanzen wird als _Cluster_, die einzelnen Instanzen werden als _Knoten_ bezeichnet.

Mit einer Bibliothek wie _Hazelcast_ wird dafür gesorgt, dass sich diese Knoten gegenseitig kennen und miteinander interagieren können. Dadurch bietet sich eine sehr große Bandbreite an Möglichkeiten.

__Inhalt dieses Tutorials__

Zwei häufig benötigte Clustering-Funktionen werden in diesem Tutorial vorgestellt: 
  1. Zugriff auf ein geteiltes Objekt, einfachstes Beispiel dafür ist ein Zähler, der von mehreren Instanzen verwendet wird.
  2. Zeitgesteuerte Aufgaben (_Cron Jobs_), die nur auf einem Knoten ausgeführt werden sollen, wie zum Beispiel das Anstoßen eines Monatsabschlusses.

Die einzelnen Kapitel:
  1. Einbinden von Hazelcast
  2. Zugriff auf ein geteiltes Objekt
  3. Cron Jobs
  4. Logging und Metriken

## Einbinden von Hazelcast
Der anfängliche Codebasis für dieses Tutorial ist ein leeres Spring-Boot-Projekt, in das bereits der Application-Starter nach dieser [Anleitung](../docs/howto-integrate.md) integriert wurde. 

Der erste Schritt ist das Hinzufügen von Hazelcast als Dependency in der pom.xml:

```xml
<dependency>
    <groupId>com.hazelcast</groupId>
    <artifactId>hazelcast</artifactId>
</dependency>
```
> Die Angabe der zu verwendenden Version ist hier nicht notwendig, diese wird im Artefakt `enterprise-dependencies` definiert, welches über den `enterprise-application-parent` von der eigenen Anwendung verwendet wird.

Durch die Autokonfiguration aus dem Starter reicht dieses Einbinden der Dependency aus, damit sich zwei gestartete Knoten finden.

Das kann lokal leicht ausprobiert werden, dazu muss lediglich die Anwendung mindestens zweimal, auf (mindestens) zwei verschiedenen Ports, gestartet werden. Dafür muss die Anwendung zunächst gebaut werden und kann dann zweimal über die erzeugte Jar-Datei gestartet werden. In dem Ordner in dem die pom.xml liegt müssen dafür folgende Befehle ausgeführt werden:

```
# 1. Bauen der Anwendung
> mvn clean install

# 2. Wechsel in Ordner target, in dem die Jar-Datei liegt
> cd target

# 3. Eine Instanz auf Port 8080 starten
> java -jar -Dserver.port=8080 {jar-name}.jar

# 4. In einem zweiten Fenster: eine zweite Instanz auf Port 8088 starten
> java -jar -Dserver.port=8088 {jar-name}.jar
```

Beim Starten der zweiten Instanz sollte im Log zu erkennen sein, dass sich die beiden Knoten erfolgreich gefunden haben. In beiden Logs sollte eine ähnliche Ausgabe wie diese zu sehen sein:

```log
23:34:02.407 INFO   c.h.internal.cluster.ClusterService - [127.0.0.1]:6001 [tutorial-clustering] [3.11]

Members {size:2, ver:12} [
    Member [127.0.0.1]:6000 - 35983674-db67-4d26-86cb-f7834b6e9e19
    Member [127.0.0.1]:6001 - ccefba9d-8040-4073-b464-84b7dfe34fd7 this
]
```

### Erläuterung der Autokonfiguration im Starter
Der Grund, warum das Clustering bereits mit sehr wenig Aufwand funktioniert, liegt ist die Autokonfiguration im Starter, sowie die dort vorhandenen Default-Properties. Folgende Hazelcast-spezifische Properties sind dort zu finden:

```
# Hazelcast Discovery Type
enterprise-application.hazelcast.discovery-type=Tcp

# Group-Name und Group-Password
enterprise-application.hazelcast.group-name=${enterprise-application.project.artifact-id}
enterprise-application.hazelcast.group-password=PW4${enterprise-application.project.artifact-id}

# Tcp-Discovery-spezifische Properties
enterprise-application.hazelcast.tcp-discovery-config.members=127.0.0.1
enterprise-application.hazelcast.tcp-discovery-config.port=6000
```

Um zu verstehen, was diese bewirken, werden sie hier kurz vorgestellt:

#### Hazelcast Discovery Type
Der _Discovery Type_ wird auf `Tcp` gesetzt, was bedeutet, dass sich die einzelnen Knoten über Tcp suchen und finden. Läuft die Anwendung in einer Cloud-Umgebung ist zumeist eine spezielle Hazelcast-Discovery für die entsprechende Cloud-Technologie notwendig.

Weitere mögliche Discovery-Typen: `AwsEcs` und `Kubernetes`. Um diese zu verwenden muss der entsprechende Starter (`enterprise-aws`, bzw. `enterprise-kubernetes`) eingebunden werden.

#### Group-Name und Group-Password
Über die beiden Properties `group-name` und `group-password` kann definiert werden zu welchem Cluster die Anwendung gehört. Im Normalfall sollen nur Instanzen der gleichen Anwendung einen Cluster bilden. Mit der vorliegenden Konfiguration aus den Default-Properties ist dies gewährleistet. (Solange keine zwei Anwendungen mit gleicher Maven-Artefakt-Id auf einem Server deployed sind - wovon auszugehen sein sollte.)

> Achtung: Der Passwort-Mechanismus ist deprecated und ab Hazelcast 3.11 werden die Passwörter nicht mehr überprüft. Falls die Anwendung nicht in einem abgeschlossenen Netzwerk läuft, also öffentlich verfügbar ist, sollte dies beachtet werden. Die Integration in den Enterprise-Starter ist geplant.
> 
> Ein denkbarer Workaround wäre, erstmal eine vorherige Hazelcast-Version zu nutzen. 

#### Tcp-Discovery-spezifische Properties
Für den Discovcery-Typen `Tcp` muss definiert werden, unter welchen IP-Adressen die (anderen) Instanzen des Clusters laufen. Zusätzlich können die Ports definiert werden, über die die Cluster-Instanzen untereinander kommunizieren. Der mit `tcp-discovery-config.port` definierte Port stellt den ersten Port dar, der verwendet wird. Ist dieser bei Start einer Instanz bereits belegt, wird der nächsthöhere verwendet.


## Zugriff auf ein geteiltes Objekt

Die Konfiguration und der Zugriff auf Cluster-weit-geteilte Objekte sind über die Bean `HazelcastInstance` möglich.
Hazelcast bringt eigene Implementierungen herkömmlicher Datentypen mit, mit deren Hilfe eine Objekt Cluster-weit verfügbar gemacht wird. Zum Beispiel `IAtomicLong` für einen numerischen Datentyp. Über die HazelcastInstance-Bean, die hier wie eine Map fungiert, kann ein Wert über einen Schlüsselwert (hier der String `test`) abgefragt werden:

```java
IAtomicLong atomicLong = this.hazelcastInstance.getAtomicLong("test");
```
Wird die Zahl auf einem der Cluster-Instanzen geändert, ist diese Änderung sofort auf allen Cluster-Instanzen verfügbar. Als Beispiel wird der Wert hier um eins erhöht:

```java
long addAndGet = atomicLong.addAndGet(1L);
```

Hier ein vollständiges Beispiel: 


```java
package de.enterprise.spring.boot.tutorialclustering;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;

@RestController
public class HelloHazelcastController {

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @GetMapping("/counter")
    public String countUp() {
        IAtomicLong atomicLong = this.hazelcastInstance.getAtomicLong("test");
        long addAndGet = atomicLong.addAndGet(1L);
        return Long.toString(addAndGet);
    }
}
```

Zum Testen müssen wieder, wie im ersten Abschnitt beschrieben, zwei Instanzen gestartet werden. Über den Browser lässt sich jetzt die Anwendung testen, indem http://localhost:8080/counter und http://localhost:8088/counter abwechselnd aufgerufen werden.

```
GET http://localhost:8080/counter
--> Ergebnis: 1

GET http://localhost:8088/counter
--> Ergebnis: 2
```

## Cron Jobs
Für zeitgesteuerte Tasks kann die Spring-Annotation `@Scheduled` genutzt werden. Ein offizielles Tutorial dazu findet sich [hier](https://spring.io/guides/gs/scheduling-tasks/). Die Annotation kann an public Methoden von Spring-Beans verwendet werden, und sorgt dafür, dass diese regelmäßig ausgeführt werden.

```java
@Scheduled(cron = "0/5 * * * * ?")
public void doSomething() { ... }
```

Das Scheduling-Feature muss mit `@EnableScheduling` an der Konfigurations-Klasse aktiviert werden. 

Im Enterprise-Starter findet sich eine Implementierung, die dafür sorgt, dass diese Cronjobs nur auf einer Instanz im Cluster ausgeführt werden.

> Wichtig: Das fuktioniert nur, wenn die `@Scheduled` Annotation mit `cron`-Expressions genutzt wird, da dann die geplante Ausführungszeit auf allen Instanzen identisch ist. Bei Verwendung von `fixedDelay` oder `fixedRate` ist dies nicht der Fall. 

Im folgenden Beispiel wird alle fünf Sekunden der Log `Hello` erzeugt. Zunächst die Konfigurations-Klasse:
```java 
package de.enterprise.spring.boot.tutorialclustering.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ApplicationConfiguration {

}
```

Hier die Service-Klasse, die die Cronjob-Definition enthält:
```java 
package de.enterprise.spring.boot.tutorialclustering;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CronDemoService {

    @Scheduled(cron = "0/5 * * * * ?")
    public void doSomething() {
        log.info("Hello");
    }
}
```

Zum Testen müssen wieder, wie im ersten Abschnitt beschrieben, zwei Instanzen gestartet werden. In der Logausgabe ist dann zu sehen, dass die Methode pro Ausführungszeitpunkt nur auf einer der beiden Instanzen ausgeführt wurde.

Auf welcher der Instanzen der Cronjob pro Ausführungszeitpunkt ausgeführt wird, lässt sich nicht vorhersagen. 

## Logging und Metriken

### Allgemeine Hazelcast Metriken

Es werden einige allgemeine Hazelcast Metriken aufgezeichent, zum Beispiel `hazelcast.cluster.state` und `hazelcast.cluster.size`.

Der aktuellen Metrik-Werte lassen sich zum Beispiel über http://localhost:8080/manage/metrics/hazelcast.cluster.size abfragen. 

> Hinweis: Diese Actuator-Endpunkt ist mit Basic-Auth geschützt, falls nicht anders gesetzt, findet sich das Passwort in den [default.properties](../enterprise-application-spring-boot-starter/src/main/resources/META-INF/application-default.properties).

### Metriken für Cronjobs
Speziell für die CronJobs werden einige weitere Metriken aufgezeichnet, die zum Beispiel Informationen über die Dauer und Häufigkeit der Ausführungen liefern. Einige Beispiele:
- `hazelcast.cluster.scheduling.duration`
- `hazelcast.cluster.scheduling.startExecution`
- `hazelcast.cluster.scheduling.finishedExecution`
- `hazelcast.cluster.scheduling.failedExecution`

### Logging für Cronjobs
Per Default ist für die CronJobs im Enterprise-Starter Logging aktiviert. Dabei wird je ein Log-Statement für Start und Ende des CronJobs ausgegeben, das sieht beispielweise so aus:

```
12:46:55.009 INFO  tutorial-clustering-6b9c342b-ddb4-44e2-8b2b-6f3980ce2453 d.a.s.b.a.s.c.s.ScheduledTaskExecutionProtocol - Start CronJob job=CronDemoService-doSomething
12:46:55.009 INFO  tutorial-clustering-6b9c342b-ddb4-44e2-8b2b-6f3980ce2453 d.a.s.b.t.CronDemoService - Hello
12:46:55.009 INFO  tutorial-clustering-6b9c342b-ddb4-44e2-8b2b-6f3980ce2453 d.a.s.b.a.s.c.s.ScheduledTaskExecutionProtocol - Finished CronJob job=CronDemoService-doSomething
```

Dieses Logging kann über Properties deaktiviert werden, entweder komplett für alle CronJobs mit 

```
enterprise-application.hazelcast.logging-properties.enabled=false
```

oder speziell für einzelne CronJobs:

```
enterprise-application.hazelcast.logging-properties.ignored-tasks=CronDemoService-doSomething
```
