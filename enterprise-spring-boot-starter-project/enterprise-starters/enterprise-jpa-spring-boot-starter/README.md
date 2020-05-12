# enterprise-jpa-spring-boot-starter - In Aufbau

Starter für Projekte, die JPA nutzen wollen.
- Spring Data JPA Integration
- sinnvolle zusätzliche Hibernate Bibliotheken
- Flyway // MÖGLICHKEIT EINBAUEN MIT ÄLTEREN VERSIONEN ZU ARBEITE? HÄUFIG NOTWENDIG BEI ORACLE DBS.
- H2?
- Best Practices für verschiedene DB-Typen (z.B. Properties / Konfigurationen)
- Auditing-Erweiterungen // ERLEDIGT
- ...



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

## Oracle

### Treiber
```xml
<dependency>
	<groupId>xxx</groupId>
	<artifactId>xxx</artifactId>
</dependency>
```

### Properties

```
spring.datasource.driverClassName=oracle.jdbc.driver.OracleDriver
spring.jpa.database-platform=org.hibernate.dialect.Oracle10gDialect
spring.datasource.hikari.connection-test-query=select 1 from dual
```