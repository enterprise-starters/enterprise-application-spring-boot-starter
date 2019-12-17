# enterprise-mongodb-spring-boot-starter - In Aufbau
Erweiterung der Pom damit Klassen für die QueryDSL Verwendung generiert werden. Nur notwendig wenn auch mit der QueryDSL gearbeitet werden soll.

``` xml

	<plugin>
		<groupId>com.mysema.maven</groupId>
		<artifactId>apt-maven-plugin</artifactId>
		<version>1.1.3</version>
		<executions>
			<execution>
				<goals>
					<goal>process</goal>
				</goals>
				<configuration>
					<outputDirectory>target/generated-sources/java</outputDirectory>
					<processor>org.springframework.data.mongodb.repository.support.MongoAnnotationProcessor</processor>
				</configuration>
			</execution>
		</executions>
	</plugin>


```




Lokale MongoDB über docker-compose starten.

Anpassen von Username/Passwort falls gewünscht.
Volume <lokaler Pfad> muss ersetzt werden.

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

