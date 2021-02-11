# IntelliJ-Formatter erzeugen aus Eclipse-Formatter

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