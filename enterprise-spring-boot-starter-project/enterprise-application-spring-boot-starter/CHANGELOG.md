# Version 2.4.0
- Spring Boot 2.4.X
- Die Haupt-Application-Klasse einer Spring-Boot-Anwendung muss nicht mehr geändert werden, um den Enterprise-Starter zu nutzen. Die Integration erfolgt nun - statt wie vorher über Vererbung - über einen Listener (Klasse `EnterpriseStarterInitApplicationListener`).
- Support für Yaml-Properties
