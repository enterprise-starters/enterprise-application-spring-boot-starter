# enterprise-mongodb-spring-boot-starter - In Aufbau
Erweiterung der Pom damit Klassen für die QueryDSL Verwendung generiert werden. Nur notwendig wenn auch mit der QueryDSL gearbeitet werden soll.

# querydsl
Für die Verwendung von der querydsl Erweiterung müssen Klassen generiert werden. Damit 
dies funktioniert müssen folgende Teile in der pom.xml ergänzt werden.

``` xml
    <properties>
        <apt-maven-plugin-processor-class>org.springframework.data.mongodb.repository.support.MongoAnnotationProcessor</apt-maven-plugin-processor-class>
    </properties>

    <build>
        <plugins>
            <plugin>
                <groupId>com.mysema.maven</groupId>
                <artifactId>apt-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

```

# Lokale MongoDB Instanz
Für die Lokale Entwicklung mit einer eigenen MongoDB kann z.B. leicht über docker-compose eine 
eigene Instanz gestartet werden.

Dafür folgende Datei im Projekt anlegen zum Beispiel unter den Name stack.yml.

``` yaml

# Use root/example as user/password credentials
version: '3.3'

services:

  mongo:
    image: mongo
    restart: always
    ports:
      - 27017:27017
    volumes:
      - <lokaler Pfad>:/data/db
    environment:
      MONGO_INITDB_ROOT_USERNAME: root
      MONGO_INITDB_ROOT_PASSWORD: example

  mongo-express:
    image: mongo-express
    restart: always
    ports:
      - 9708:8081
    environment:
      ME_CONFIG_MONGODB_ADMINUSERNAME: root
      ME_CONFIG_MONGODB_ADMINPASSWORD: example
      
```
Anpassen von Username/Passwort falls gewünscht.
Volume <lokaler Pfad> muss ersetzt werden.

Diese kann dann wie folgt verwendet werden und passend konfiguriert werden.

Starten über
```
    docker-compose -f stack.yml up
```

DB und User anlegen über mongo shell

```
    docker exec -it mongodb_mongo_1 bash
    
    mongo --username root --password example --authenticationDatabase admin
    
    use <database-name>
    
    db.createUser(
    {
        "user": "<database-username>",
        "pwd": "<database-password>",
        "roles": [
            {
                "role": "readWrite",
                "db": "<database-name>"
            }
        ]
    })
```

Die folgenden Werte müssen noch Sinnvoll ersetzt werden.

<database-name>
<database-username>
<database-password>
