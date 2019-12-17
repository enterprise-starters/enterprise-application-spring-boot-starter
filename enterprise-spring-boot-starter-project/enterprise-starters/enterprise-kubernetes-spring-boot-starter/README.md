# Enterprise-Starter für Kubernetes

Features:
- Clustering mit Hazelcast

## Clustering mit Hazelcast

In der `pom.xml` des Services müssen dieser Starter, sowie `hazelcast-spring` und `hazelcast-kubernetes` als Abhängigkeiten hinzugefügt werden. 

```xml
<dependency>
    <groupId>de.enterprise-starters</groupId>
    <artifactId>enterprise-kubernetes-spring-boot-starter</artifactId>
    <version>{aktuelle Version}</version>
</dependency>
<dependency>
    <groupId>com.hazelcast</groupId>
    <artifactId>hazelcast-spring</artifactId>
</dependency>
<dependency>
    <groupId>com.hazelcast</groupId>
    <artifactId>hazelcast-kubernetes</artifactId>
</dependency>
```

In den Properties des Service muss folgendes ergänzt werden:

```ini
# Hazelcast Kubernetes 
enterprise-application.hazelcast.discovery-type=Kubernetes
enterprise-application.hazelcast.kubernetes-discovery-config.use-dns=false
enterprise-application.hazelcast.kubernetes-discovery-config.service-name=${enterprise-application.project.artifact-id}
```

Dies sollte jedoch nur für die Umgebungen konfiguriert werden, in denen der Service im Kubernetes Cluster läuft. Für die lokale Ausführung muss das Property 

```ini
enterprise-application.hazelcast.discovery-type=Tcp
```
gesetzt werden.