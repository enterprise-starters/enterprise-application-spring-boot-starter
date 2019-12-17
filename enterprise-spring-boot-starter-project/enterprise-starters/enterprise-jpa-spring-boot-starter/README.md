# enterprise-jpa-spring-boot-starter - In Aufbau

Starter für Projekte, die JPA nutzen wollen.
- Spring Data JPA Integration
- sinnvolle zusätzliche Hibernate Bibliotheken
- Flyway
- H2?
- Best Practices für verschiedene DB-Typen (z.B. Properties / Konfigurationen)
- Auditing-Erweiterungen
- ...

# Snippets
Properties für die Default-Properties:
```
# Database
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.maximum-pool-size=10
spring.jpa.properties.hibernate.generate_statistics=true
spring.jpa.properties.hibernate.session.events.log=false

spring.jpa.show-sql=false
spring.jpa.hibernate.ddl-auto=none
```
Auditing: siehe Ordner Code-Snippets

# Best Practices
## MySql

### Treiber
```xml
<dependency>
	<groupId>mysql</groupId>
	<artifactId>mysql-connector-java</artifactId>
</dependency>
```
### Properties

```
spring.datasource.hikari.connection-test-query=SELECT 1
spring.jpa.database-platform=org.hibernate.dialect.MySQL5Dialect
```
