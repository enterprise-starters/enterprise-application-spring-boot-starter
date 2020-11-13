# Überblick

Dieses Artefakt enthält grundlegende Konfigurationsdateien zum Einrichten der Arbeitsumgebung, sowie für den Buildprozess:

- IDE-Formatter
- Maven-Settings
- CheckStyle Konfiguration
- Templates für die automatische Generierung von Swagger-Api-Dokumentation


# Einrichten der Arbeitsumgebung

## GIT
Bei Git sollte der User und die Email-Adresse gesetzt sein, damit nachvollziehbar ist, wer welche Commits getätigt hat. 

```
git config --global user.name "John Doe"
git config --global user.email johndoe@example.com
```

Folgende Konfiguration ist sinnvoll, damit seitens des Git-Clients keine Änderungen an den Zeilenumbrüchen vorgenommen werden:

```
git config --global core.autocrlf false
```

## IDE-Formatter

Für ein angenehmes gemeinsames Entwickeln sollten alle Entwickler die gleichen Einstellungen für die Code-Formatierung verwenden. 

### STS/Eclipse
Im Ordner `src\main\resources\eclipse` sind _Formatter_- sowie _Cleanup_-Vorlagen zu finden, die direkt importiert werden können. Für _Save Actions_ gibt es einen Screenshot, der eine auf die Code-Analyse-Tools abgestimmte Konfiguration zeigt.


Am Beispiel Java hier eine Anleitung:
- Formatter: `Preferences -> Java -> Code Style -> Formatter` dort dann über `Import...` die entsprechende Datei auswählen.
- Cleanup: `Preferences -> Java -> Code Style -> Clean Up` dort dann über `Import...` die entsprechende Datei auswählen.
- Save Actions: `Preferences -> Java -> Editor -> Save Actions` dort gibt es keine Import/Export-Möglichkeit, daher muss hier manuell die Konfiguration entsprechend dem Screenshot angepasst werden.

### IntelliJ
IntelliJ bietet ähnlich zu Eclipse eine Import/Export-Funktion für Formatter u.Ä., welche über die Settings erreichbar ist. Die Settings werden mit `Ctrl+Alt+S` aufgerufen.
- Formatter: Import über `Editor -> Code Style -> Java`, dort `Scheme -> Import Scheme -> IntelliJ Idea code style XML`. Die Vorlage für den IntelliJ-Formatter befindet sich im Ordner `src\main\resources\intellij`.
- Save Actions: IntelliJ verfügt nicht über Save-Actions, da InteliJ Auto-Save benutzt. Es gibt ein [Plugin](https://plugins.jetbrains.com/plugin/7642-save-actions) dafür.

#### IntelliJ und Eclipse

> Diese Anleitung beschreibt das Anlegen eines IntelliJ-Formatters aus einem Eclipse-Formatter im Allgemeinen. Falls der oben beschriebene IntelliJ-Formatter verwendet wird, sind die Schritte aus diesem Kapitel nicht notwendig.

Diese Einstellungen sollten vorgenommen werden, wenn Eclipse und IntelliJ Idea nebeneinander im Projekt verwendet werden.
IntelliJ kann den Eclipse-Formatter nutzen `Editor -> Code Style -> Java` dort `Scheme -> Import Scheme -> Eclipse XML Profile`.
Allerdings sind dann noch weitere Einstellungen nötig, um dem aktuell verwendeten Eclipse-Formatter zu entsprechen
- JavaDoc: `Editor -> Code Style -> Java -> JavaDoc`:
    - `Uncheck Generate <p> on empty lines` - keine `<p>`-Tags in leeren JavaDoc-Zeilen generieren.
    - `Check keep empty lines` - Leere JavaDoc-Zeilen nicht löschen.
    - `Preserve line feeds` - Schützt vor ungewollten Zeilenumbrüchen.
    - `Do not wrap one line comments` - Keine Zeilenumbrüche in Einzeiler-Kommentaren, die mit `/** ... */` erstellt wurden.
- Indentation: `Editor -> Code Style -> Java -> Tabs and Indents`: `Indent 4`
- Wrapping: `Editor -> Code Style -> Java -> Wrapping and Braces`: Überprüfen, dass folgendes Punkte auf `Do not wrap` stehen:
    - `Annotation parameters`
    - `Local variable annotations`
    - `Parameter annotations`
    - `Field annotations`
    - `Method annotations`
    - `Class annotations`
    - `Enum constants`
- Imports: `Editor -> Code Style -> Java -> Imports`, verhindert, dass Imports mit `*` importiert werden.
    - `Class count to use import with * : 50` (oder mehr)
    - `Name count to use static import with: 30` (oder mehr)
    
    Dann müssen die Imports folgendermaßen im Feld `all static imports` angelegt werden:
    ```
    all static imports
    blank
    java
    blank
    javax
    blank
    org
    blank
    com
    blank
    all other imports
    ```
- Empfehlung für eine Save-Action in Eclipse: `Remove trailing white space - all lines`. Dies verhindert, dass in JavaDoc-Kommentaren `Space` in leere Zeilen eingefügt wird.
Dies entspricht dem Verhalten von IntelliJ und kann dort nicht geändert werden.
- Empfehlung für Fonts in IntelliJ: `Consolas`, jedes aktuelle Mircosoft-Windows hat diesen an Board. `Editor -> Font`: `Consolas, Size 14, Line spacing 1.0`


## Maven-Settings
Die angepassten Maven-Settings müssen aus dem Ordner `src\main\resources\maven` in den Ordner `{User-Home}/.m2` kopiert werden. Dadurch wird zum Beispiel ein spezieller Nexus für Maven konfiguriert.

## Lombok
Zum Verwenden von Lombok muss die IDE entsprechend eingerichtet werden, eine Anleitung dazu findet sich [hier](https://projectlombok.org/setup/overview).

> [Lombok](https://projectlombok.org/) ist ein sehr hilfreiches Tool zum Vermeiden von Boilerplate Code. Es bietet Möglichkeiten wie sehr einfache automatische Generation von Gettern, Settern, Konstruktoren, ToString-Methoden und vieles mehr, einfach durch Anbringen von Annotationen im Quellcode.

In jedem Projekt, in dem Lombok genutzt wird, sollte die Datei, die hier unter `src\main\resources\lombok.config` liegt, in das Hauptverzeichnis des jeweiligen Projektes kopiert werden. Dies sorgt dafür, dass die generierten Codezeilen von JaCoCo bei Berechnung der Code-Coverage ausgelassen werden.  

# CheckStyle-Regeln

Im Ordner `src\main\resources\checkstyle` sind die Checkstyle-Rules und -Suppressions zu finden, die während dem Build-Prozess verwendet werden. Der Aufruf von Checkstyle während dem Build-Prozess (maven-checkstyle-plugin) ist in den Parent-Projekten definiert.

# Templates für die automatische Generierung von Swagger-Api-Dokumentation
Format-Vorlagen für die automatisch generierbare Swagger-Schnittstellendokumentation. Der Aufruf der Swagger-Generierung während dem Build-Prozess (swagger-maven-plugin) ist im Application-Parent-Projekt definiert.
