# Enterprise AWS ECS Spring-Boot-Starter
Dieser Starter ist für Services entwickelt, die in der AWS (Amazon Web Services) über ECS (Elastic Container Service) deployed werden. 
Er baut auf dem enterprise-application-spring-boot-stater auf.

Features:
- [Hazelcast-Dicsovery für AWS ECS](./readme.md#hazelcast)
- [Metrik Export zu Cloudwatch](./readme.md#metrik-export-zu-cloudwatch)
- [SQS Zugriff mit Circuitbreaker](./readme.md#sqs-zugriff-mit-circuitbreaker)

<!---
TODO: 
- Demo-Service??
- Hinweis, dass keine Tests vorhanden / Einbringung gewünscht
-->

## Hazelcast

Damit sich die einzelnen Knoten finden, wird eine spezielle AWS-ECS-Hazelcast-Discovery benötigt, die im nächsten Abschnitt beschrieben wird. Um diese zu verwenden muss der Hazelcast-Discovery-Typ auf `AwsEcs` gestellt werden.

```ini
enterprise-application.hazelcast.used-discovery-type=AwsEcs
```

Diese Einstellung sollte nur zur Laufzeit innerhalb des AWS-ECS-Clusters verwendet werden.  
In der lokalen Entwicklungsumgebung inkl. JUnit-Tests sollte der Wert immer auf `Tcp` eingestellt werden.

Siehe auch Abschnitt _Hazelcast_ in der [readme](../enterprise-application-spring-boot-starter/README.md) des `enterprise-application-spring-boot-starter`s.

### AWS ECS Hazelcast-Dicsovery
Der Discovery-Mechanismus verwendet intern das Amazon AWS ECS SDK um passende Container Instanzen zu ermitteln. Weitere Konfigurationsparameter sind bei dieser Variante nicht notwendig. Über den service-name des Containers werden passende Instanzen über das SDK ermittelt und der Cluster entsprechend gebildet. Wichtig ist das der Standard Hazelcast Tcp Port (default: 5701) beim Container als PortMapping - ContainerPort: 5701 auch eingetragen/freigegeben wird. Über diesen baut Hazelcast den Cluster auf.
Die Discovery Implementierung befindet sich unterhalb des Packages `de.enterprise.starters.aws.ecs.clustering.hazelcast.discovery.spi.discovery.aws`. Eine fertige releaste Library gibt es aktuell nicht.

## Metrik Export zu Cloudwatch

Für die Anbindung der Metriken an AWS Cloudwatch gibt es eine AutoConfiguration in der Bibiliothek `spring-cloud-aws-autoconfigure`. Diese wird über das Property

```ini
management.metrics.export.cloudwatch.namespace=<gewünschter Custom Namespace in der CloudWatch>
```

aktiviert. Bei der Aktivierung wird der micrometer.CloudWatchMeterRegistry erzeugt. Dieser exportiert regelmäßig (aktuell alle 10 Sekunden) die Metriken zu CloudWatch.

In den Default-Properties dieses Starters wird der Namespace wie folgt konfiguriert:

```ini
management.metrics.export.cloudwatch.namespace=de.enterprise.service.${enterprise-application.project.artifactId}
```
Über das Property 

```ini
management.metrics.export.cloudwatch.enabled=false
```
	
kann der CloudWatch-Export explizit deaktiviert werden. In den Grundeinstellungen sollte innerhalb der konkreten Services in den `application-dev-local.properties` dieser Wert immer gesetzt werden, da ein Export der lokalen Entwicklungsmetriken nicht notwendig ist.

ACHTUNG: Es gibt das Property 

```ini
# Each request is also limited to no more than 20 different metrics.
management.metrics.export.cloudwatch.batchSize=20 
```

Dieses sollte wegen der Beschränkung auf 20 Metriken pro Request auf der geringen batchSize von 20 stehen bleiben. 

Mit den Default-Properties ist also der Export von Metriken zu Cloudwatch aktiviert und konfiguriert. Lediglich die Deaktivierung für lokale Starts und Test muss im eigenen Service vorgenommen werden.

## SQS Zugriff mit Circuitbreaker

### Features auf einen Blick
SQS-Mechanismus aus [spring-cloud-aws-messaging](https://cloud.spring.io/spring-cloud-aws/) mit folgenden Erweiterungen:
- Verwendung der Circuitbreaker-Frameworks [resilience4j](https://github.com/resilience4j/resilience4j)
- Retry der Verarbeitung von Nachrichten, die auf Grund von technischen Fehlern nicht verarbeitet werden konnten
- Eigener Mechanismus zum aktiven Löschen von Poison Pills

### Features
Bei einer SQS-Queue können bis zu 10 Nachrichten gleichzeitig abgefragt werden. (Maximum von 10 ist durch AWS vorgegeben, Wert ist über Properties einstellbar.) Die Verarbeitung der Nachrichten erfolgt einzeln asynchron. Nachrichten werden nur bei erfolgreicher Verarbeitung aus der SQS-Queue gelöscht. Fliegt also bei der Verarbeitung einer Nachricht eine Exception, wird die Nachricht nicht gelöscht und ist nach Ablauf des AWS-_visibilityTimeouts_ wieder in der Queue sichtbar.

Durch einen Circuitbreaker-Mechanismus kann die Abfrage von Nachrichten von der Queue verlangsamt werden. Der Status des Circuitbreakers (geschlossen, offen, halb-offen) ergibt sich aus der Verarbeitung der Queue-Nachrichten. Bei der Abfrage der Nachrichten wird der Status des Circuitbreakers überprüft. Erlaubt der Circuitbreaker aktuell keine Verarbeitung, wird eine Sekunde abgewartet und der Status erneut überprüft.

Bei Anlage einer SQS-Queue kann die _Message Retention Period_ definiert werden. Dieser Wert definiert, wie lange eine Nachricht in der Queue gehalten wird. Ist dieser Zeitraum überschritten, wird die entsprechende Nachricht automatisch aus der Queue entfernt. Um in der eigenen Anwendung programmatisch eingreifen zu können, wurde im Starter ein analoger Mechanismus zum Löschen von Nachrichten, die lange in der Queue sind, eingebaut.

### Verwendung
Um SQS-Queues zu konsumieren muss die folgende Maven-Dependency dem Projekt hinzugefügt werden:

```xml
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-aws-messaging</artifactId>
</dependency>
```

Empfangen und Verarbeiten von SQS-Nachrichten:

```java
@SqsListener("${enterprise-application.sqs.queues.avNotifications.name}")
protected void receiveMessage(AvChangedMessage avChangedMessage, @Headers Map<String, String> headers) {
	...
}
```

Die Methode `receiveMessage` verarbeitet die Nachrichten aus der SQS-Queue, deren logischer (externer) Name im Property `enterprise-application.sqs.queues.avNotifications.name` definiert ist. Der interne Name der Queue lautet hier `avNotifications`, aus diesem ergibt sich auch der Name des Circuitbreakers, der in diesem Beispiel `sqsListenerAvNotifications` lautet.

> __Hinweis__: Die Annotation `@SqsListener` hat ein Attribut `deletionPolicy`, durch das normalerweise gesteuert werden kann, wann die Nachricht aus der Queue gelöscht wird. 
>
>Jeglicher Wert der hier angegeben wird, wird aktuell in unserer angepassten SQS-Verarbeitung überschrieben durch `SqsMessageDeletionPolicy.ON_SUCCESS`, was bedeutet, dass eine Nachricht nur dann aus der Queue gelöscht wird, wenn die Verarbeitung erfolgreich, also ohne Exceptions, durchgelaufen ist.  

Ergänzend hier noch der [Link zur Doku von Spring-Cloud-AWS](https://cloud.spring.io/spring-cloud-aws/spring-cloud-aws.html#_messaging).

#### Format der SQS-Nachrichten
Als Standard wird Json erwartet, wenn die Nachricht kein Attribut `contentType` gesetzt hat, wird versucht die Nachricht als Json zu deserialisieren. Kann die Nachricht nicht konvertiert werden, also nicht in den erwarteten Java-Typ umgewandelt werden, wird die Nachricht aus der Queue gelöscht. Es erfolgt also kein Retry.

Der erwartete Java-Typ wird durch Methodenparameter der mit `@SqsListener` annotierten Methode definiert. Im Beispiel aus dem vorherigen Absatz wäre das die Klasse `AvChangedMessage`.

#### Konfiguration von SQS-Abfragen
Die Konfiguration von SQS-Default-Einstellungen, sowie spezifischen Einstellungen pro Queue ist mit der Klasse `SqsProperties` möglich. 

Default Properties, die für alle Queues gelten:
- `backOff`: Zeit, die nach Exception beim Abrufen von neuen SQS-Nachrichten gewartet wird bis zum nächsten Versuch
- `maxNumberOfMessages`: Maximale Anzahl an Nachrichten pro Anfrage
- `visibilityTimeout`: Zeit, in der eine Nachricht nach Abfrage aus der Queue nicht sichtbar ist. Wird die Nachricht während dieser Zeit NICHT aus der Queue gelöscht, kann sie wieder abgerufen werden.
- `waitTimeOut`: Wird dieser Wert größer 0 gesetzt, wird _Long Polling_ verwendet, d.h. die Anfrage an AWS wartet die entsprechende Zeit, ob neue Nachrichten eingehen.

Spezifische Queue-Einstellungen:
- Definition des externen Queue-Namens über das Property `name`
- Löschen von nicht verarbeiteten Nachrichten:
  - `deleteMessagesOnMaxAgeEnabled`: Mechanismus aktivieren / deaktivieren
  - `maxAge`: Wert (Datentyp: Duration. Mögliche Syntax für Property-Dateien: [Spring Boot Doku](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html#boot-features-external-config-conversion-duration))
  
> Hinweis: Die Einstellungen für `maxAge` ssollten niedriger konfiguriert werden, als die dür die Queue definierte _Message Retention Period_. Anderenfalls wird dieser Mechanismus niemals greifen.


Beispiel für Properties:

```ini
# Default Properties
enterprise-application.sqs.defaults.back-off-time=10
enterprise-application.sqs.defaults.request.max-number-of-messages=10
enterprise-application.sqs.defaults.request.visibility-timeout=30
enterprise-application.sqs.defaults.request.wait-time-out=20

# Spezifische Queue Properties - internalQueueName=avNotifications
enterprise-application.sqs.queues.avNotifications.name=MagsTest
enterprise-application.sqs.queues.avNotifications.delete-messages-on-max-age-enabled=true
enterprise-application.sqs.queues.avNotifications.max-age=10

# Spezifische Queue Properties - internalQueueName=another-queue
enterprise-application.sqs.queues.another-queue.name=Test2
```

#### Konfiguration des Circuitbreakers
Für jede verwendete SQS-Queue muss eine Circuitbreaker-Konfiguration vorliegen. Der Name des Circuitbreakers ergibt sich aus dem internen Namen der Sqs-Queue mit dem Präfix `sqs-listener-`. 

Für weitere Dokumentation zur Konfiguration der Circuitbreaker sei auf den [Userguide von Resilience4j](http://resilience4j.github.io/resilience4j/#_circuitbreaker) verwiesen.

Beispiel-Konfiguration:

```ini
resilience4j.circuitbreaker.backends.sqsListenerAvNotifications.register-health-indicator=true
resilience4j.circuitbreaker.backends.sqsListenerAvNotifications.failure-rate-threshold=10
resilience4j.circuitbreaker.backends.sqsListenerAvNotifications.ring-buffer-size-in-closed-state=20
resilience4j.circuitbreaker.backends.sqsListenerAvNotifications.ring-buffer-size-in-half-open-state=5
resilience4j.circuitbreaker.backends.sqsListenerAvNotifications.wait-duration-in-open-state=10s
```

#### Metriken
Für die Abarbeitung der Events aus den SQS Queues werden Metriken erzeugt, unterhalb des Prefixes `sqs`, wie z.B.

- `sqs.messages.processing.success` -> Enthält die Anzahl erfolgreich verarbeiteter SQS Nachrichten. 

## Tracing
Innerhalb der AWS sollten eingehende Requests, die über einen AWS LoadBalancer kommen, einen HTTP Header `X-Amzn-Trace-Id` besitzen. Ist dies der Fall, so wird diese TraceId übernommen und intern verwendet. Existiert kein Header wird eine neue TraceId erzeugt. 

Damit das funktioniert, muss das Property 
```
enterprise-application.tracing.request-header-name=X-Amzn-Trace-Id
```
gesetzt sein.