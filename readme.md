# Enterprise-Spring-Boot-Starter


## Was ist der Enterprise-Starter?

<!-- Fachlich -->
Der Enterprise-Starter definiert ein Standard-Setup und ein Standard-Vorgehen und ist gleichzeitig Werkzeugkoffer für die Entwicklung von Spring-Boot-basierten Cloud-native Microservices.

Der Enterprise-Starter ist eine Sammlung von verschiedenen Bibliotheken, in die die Best Practices aus diversen IT-Projekten eingeflossen sind. Bei Verwendung des Enterprise-Starters kommt also Code zum Einsatz, der sich bereits bewährt hat. 
Dazu hat der Enterprise-Starter auch den Anspruch den enthaltenen Code bzw. die vorhandenen Funktionen durch Dokumentation sowie Tutorials auch verstehbar und leicht einsetzbar zu machen.

Darüberhinaus liefert der Starter ein bewährtes Standard-Vorgehen für neue Projekte. Er enthält eine Anleitung zum initialen Einrichten der Arbeitsumgebung eines Entwicklers. 

### Was heißt das jetzt technisch?
Das Enterprise-Starter-Projekt baut auf Spring-Boot auf. Wie bei Spring-Boot gibt es auch im Enterprise-Starter-Projekt einen Basis-_Starter_, sowie Basis-_Parents_. Hinzu kommen spezifische Starter für verschiedene Technologien, wie zum Beispiel der Enterprise-Kubernetes-Starter, der dann interessant ist, wenn der zu entwickelnde Service auf einem Kuberenetes Cluster installiert werden soll.

<!-- Konzept Spring-Boot-Starter -->
Das Konzept von Spring-Boot-Startern ist die Auslagerung notwendiger (Basis-)Konfiguration in ein referenziebares Artefakt. Statt diese Konfiguration in jedem Projekt einzeln durchführen zu müssen, muss lediglich der Starter als Abhängigkeit eingebunden werden. Die Standardkonfiguration aus dem Starter, kann bei Bedarf auch noch wieder geändert werden, zumeist über Properties. Dieses Prinzip wird auch als _Convention over Configuration_ beschrieben.

<!-- Enterprise-Application-Starter-->
Der [Enterprise-Application-Spring-Boot-Starter](./enterprise-spring-boot-starter-project/enterprise-application-spring-boot-starter), oben als Basis-Starter bezeichnet, ist eine Codebibliothek, die bewährte und getestete Implementierungen bzw. Lösungen enthält. Gleichzeitig wird damit auch ein technisches Standard-Setup definiert, also eine Auswahl an Bibliotheken, deren Einsatz sich in der Vergangenheit bewährt hat. Beim Einsatz in einem Projekt bringt der Enterprise-Application-Starter einerseits einen Großteil der Basiskonfiguration mit, andererseits kann er auch für das Projekt individualisiert werden, sodass der nächste Microservice kaum noch Aufwand bereitet.

>TODO: Für die Individualisierung pro Projekt sollte ein eigener Starter angelegt werden, der den Enterprise-Spring-Boot-Starter einbindet.

<!-- Parents -->
Zusätzlich zu den verschiedenen Startern gibt es im Enterprise-Starter-Projekt auch Parent-Artefakte, über die vor allem Versionen von Abhängigkeiten, sowie die Schritte des Build-Prozesses definiert werden. Auch hier ist der Zweck die Zentralisierung von Konfiguration, sodass diese im konkreten Projekt geerbt werden kann und nur bei Bedarf verändert werden muss.

<!-- Einrichten Arbeitsumgebung -->
Die Anleitung zur Einrichtung der Arbeitsumgebung fängt mit der Konfiguration des Git-Clients an und endet bei der Einrichtung von Formattern für die IDE. Diese sind abgestimmt auf die statischen Code-Analyse Tools, die in den Build-Prozess integriert sind.

Insgesamt bietet das Enterprise-Starter-Projekt eine sehr gute Basis für die Entwicklung von Cloud-native Microservices. Die Individualisierung pro Projekt ist dabei möglich und sinnvoll.

# Verwendung des Enterprise-Starters

## Welche Bedingungen gibt es für die Verwendung des Starters?
- Spring Boot 2
- Maven

## Wie nutze ich den Enterprise-Starter?
1. Jeder Entwickler muss seine Entwicklungsumgebung einrichten. Die Anleitung dazu ist in der [readme.md von build-tools](./enterprise-spring-boot-starter-project/enterprise-build-tools/readme.md) zu finden. 
2. Für jeden Service (Spring-Boot-Anwendung) der entwickelt wird, sind gewisse Schritte notwendig, um den des Enterprise-Starter in die Anwendung zu integrieren. Diese sind [hier](./enterprise-spring-boot-starter-project/docs/howto-integrate.md) beschrieben.

# Dokumentationen <!-- und Tutorials -->

<!--
## Tutorials
Die Idee der Tutorials ist, die Features der Enterprise-Starter-Toolbox aufzuzeigen. Jedes Tutorial ist so konzipiert, dass es Schritt für Schritt nachprogrammiert werden kann. Zu jedem Turorial gibt es ein separates Git-Repository, in dem der zugehörige Quellcode abgelegt ist. Jeder Schritt aus dem Tutorial ist dort in der Commit-Historie nachvollziehbar.

Folgende Tutorials gibt es zum Enterprise-Spring-Boot-Starter:
- [Basis Tutorial](./enterprise-spring-boot-starter-project/docs/tutorial-basics.md)
- [Tutorial Clustering](./enterprise-spring-boot-starter-project/docs/tutorial-clustering.md)
-->

<!--- - TODO Tutorial Service 2 Service Security -->
<!---
> Die Tutorials bauen auf die oben (im Kapitel _Wie nutze ich den Enterprise-Starter beim Kunden?_) genannten Anleitungen auf. Diese sollten vor Bearbeitung der Tutorials gelesen bzw. angewandt werden.
-->
<!-- 
Weitere Ideen für Tutorials
  - Service 2 Service Security
    - Ein Service-Projekt, ein Client
  - Weitere mögliche Themen:
    - ExtendedRestTemplate, zum Beispiel mit BasicAuth
    - Datenbank mit JPA (= Einsatz des jpa-starters)
    - Datenbank mit MongoDB (= Einsatz des mongodb-starters)
    - Lombok
    -->

## Übersicht über alle Dokumentationen
- [Readme Buildtoos](enterprise-spring-boot-starter-project/enterprise-build-tools/readme.md)
  - Einrichten der Arbeitsumgebung
- [Howto Integration](./enterprise-spring-boot-starter-project/docs/howto-integrate.md)
  - Integration des Enterprise-Starters in eine Spring-Boot-Anwendung
- [Readme Enterprise-Spring-Boot-Starter-Project](./enterprise-spring-boot-starter-project/readme.md)
  - Übersicht Maven-Module
- [Readme des Application-Starters](./enterprise-spring-boot-starter-project/enterprise-application-spring-boot-starter/README.md)
  - Detaillierte Beschreibung der Features