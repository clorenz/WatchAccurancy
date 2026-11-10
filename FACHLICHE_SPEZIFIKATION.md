# Fachliche Spezifikation: WatchAccurancy

**Software:** WatchAccurancy  
**Paket-ID:** `de.uhrwerksarchiv.watchaccurancy`  
**Lizenz:** GNU General Public License v3 (GPLv3) – Freie Open-Source-Software  
**Zielplattform:** Android (ab API 24 bis API 36)  
**Technologie-Stack:** Kotlin 2.2, Jetpack Compose, Material Design 3, Room SQLite Database, Coroutines, Flow, RFC 4330 SNTP  
**Stand der Dokumentation:** November 2026  

---

## 1. Produktübersicht & Vision

**WatchAccurancy** ist eine präzise, quelloffene Horologie-Anwendung für Liebhaber mechanischer Uhren, Uhrmacher und Sammler. Die App dient der exakten Dokumentation, Analyse und Visualisierung der Ganggenauigkeit (Präzision) mechanischer Automatik- und Handaufzugsuhren.

### Kernprinzipien
1. **Atomgenaue Referenz**: Abgleich im exakten Millisekundenbruchteil über weltweit verteilte NTP-Zeitserverpools (`pool.ntp.org`, Physikalisch-Technische Bundesanstalt Braunschweig `ptbtime1.ptb.de`, Cloudflare, Google).
2. **Volle Datenhoheit & Datenschutz**: Keine Cloud-Zwänge, keine Tracker, keine Werbenetzwerke. Sämtliche Daten verbleiben lokal in einer verschlüsselbaren SQLite/Room-Datenbank auf dem Gerät des Benutzers.
3. **100% Abwärtskompatibilität zu WatchCheck**: Vollständige Unterstützung des Original-WatchCheck-Backup-Formats (`watchcheck2-*.data`, `.txt`, `.csv`) inklusive Dateiauswahl-Dialog (Storage Access Framework), Header-Verarbeitung (`WatchCheck2`), `WATCH:`- und `LOG:`-Struktur.
4. **Horologische Fachstandards**: Berechnung des täglichen Gangs in Sekunden pro Tag ($s/d$), Auswertung nach Uhrenlagen (Lagenprüfung) und Evaluierung nach COSC-Chronometernormen.
5. **Markenneutralität**: Keine kommerziellen Markennamen in Beispielen oder Platzhaltern; durchgängige Verwendung präziser Uhrmacher-Begriffe (z. B. "Klassische Automatikuhr", "Handaufzug Chronograph", "Taucheruhr Automatik").

---

## 2. Mathematische & Horologische Grundlagen

### 2.1 Absolute Abweichung (Offset)
Der Benutzer setzt eine Referenz-Uhrenzeit fest (z. B. 14:30:00 Uhr). Im exakten Augenblick, in dem der physische Sekundenzeiger der mechanischen Uhr den 12-Uhr-Index (Sekunde 00) erreicht, betätigt er den Erfassungsbutton.
Die absolute Abweichung $\Delta t$ zum NTP-Atomzeitpunkt $t_{\text{NTP}}$ berechnet sich wie folgt:

$$\Delta t = t_{\text{Uhr}} - t_{\text{NTP}}$$

- $\Delta t > 0$: Die Uhr **geht vor** (Plus-Bereich / Vorgehen).
- $\Delta t < 0$: Die Uhr **geht nach** (Minus-Bereich / Nachgehen).

### 2.2 Gangberechnung in Sekunden pro Tag ($s/d$)
Zwischen zwei zeitlich aufeinanderfolgenden Messpunkten $P_1$ und $P_2$ mit den absoluten Abweichungen $\Delta t_1$ und $\Delta t_2$ sowie den NTP-Zeitstempeln $t_{\text{NTP}, 1}$ und $t_{\text{NTP}, 2}$ wird die Gangdrift ermittelt:

$$\Delta T_{\text{Tage}} = \frac{t_{\text{NTP}, 2} - t_{\text{NTP}, 1}}{86\,400\,000 \text{ ms}}$$

$$\text{Gang} = \frac{\Delta t_2 - \Delta t_1}{\Delta T_{\text{Tage}}} \quad \left[\frac{\text{s}}{\text{d}}\right]$$

*Beispiel:*  
- Tag 1 (12:00:00 Uhr): Abweichung $+4{,}0 \text{ s}$  
- Tag 2 (12:00:00 Uhr, $\Delta T = 1{,}0 \text{ Tag}$): Abweichung $+6{,}5 \text{ s}$  
- Gang: $(+6{,}5 - (+4{,}0)) / 1{,}0 = +2{,}5 \text{ s/d}$

### 2.3 Uhrenlagen (Lagenunruhe & Isochronismus)
Mechanische Unruh-Spiralsysteme reagieren auf die Erdanziehungskraft. Je nach Lage der Uhr im Raum treten veränderte Reibungswerte an den Zapfen der Unruh auf. Zur präzisen Kalibrierung unterstützt WatchAccurancy alle 8 international etablierten Lagen-Codes:

| Lagen-Code | Deutsche Bezeichnung | Internationale Bezeichnung | Horologische Bedeutung |
| :--- | :--- | :--- | :--- |
| **WORN** | Am Arm getragen | Worn on wrist | Reale Alltagsbelastung durch Armbewegungen |
| **WINDER** | Uhrenbeweger | Watch Winder | Kontinuierliche Rotation im Beweger |
| **DU** | Zifferblatt oben | Dial Up (Pos. 1) | Flach liegend auf Gehäuseboden |
| **DD** | Zifferblatt unten | Dial Down (Pos. 2) | Flach liegend auf Glas |
| **CU** | Krone oben | Crown Up (Pos. 3) | Vertikal, 12 Uhr nach unten |
| **CD** | Krone unten | Crown Down (Pos. 4) | Vertikal, 12 Uhr nach oben |
| **CL** | Krone links | Crown Left (Pos. 5) | Vertikal, 9 Uhr nach unten |
| **CR** | Krone rechts | Crown Right (Pos. 6) | Vertikal, 3 Uhr nach unten |

### 2.4 COSC-Chronometernorm
Die App vergleicht den mittleren Gang automatisch mit der offiziellen Schweizer Chronometernorm COSC (Contrôle Officiel Suisse des Chronomètres für mechanische Armbanduhren):
- Toleranzbereich: **$-4{,}0 \text{ s/d}$ bis $+6{,}0 \text{ s/d}$**
- Ein Indikator auf der Statistik-Karte weist aus, ob das Werk innerhalb der Chronometertoleranz reguliert ist.

---

## 3. System- & Softwarearchitektur

Die Architektur folgt dem modernen **MVVM (Model-View-ViewModel)**-Muster unter Verwendung von Android Jetpack-Bibliotheken:

```
┌────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI                   │
│  WatchListScreen  |  MeasureScreen  |  ChartsScreen    │
│            BackupScreen  |  WatchPrecisionChart        │
└───────────────────────────┬────────────────────────────┘
                            │ StateFlow / Events
┌───────────────────────────▼────────────────────────────┐
│                      WatchViewModel                    │
│   Reaktive UI-Zustände, Filterung, Berechnungen        │
└─────────────┬────────────────────────────┬─────────────┘
              │                            │
┌─────────────▼──────────────┐ ┌───────────▼─────────────┐
│      WatchRepository       │ │       NtpManager        │
│ Transaktionssichere DB-Ops │ │  RFC 4330 SNTP-Client   │
└─────────────┬──────────────┘ └───────────┬─────────────┘
              │                            │
┌─────────────▼──────────────┐ ┌───────────▼─────────────┐
│     Room SQLite Database   │ │  UDP Socket (Port 123)  │
│  `watches` & `measurement` │ │  `pool.ntp.org` / PTB   │
└────────────────────────────┘ └─────────────────────────┘
```

- **Präzisions-Zeiterfassung**: Beim Tippen auf den Erfassungsbutton wird sofort `SystemClock.elapsedRealtime()` abgefragt. Der `NtpManager` rechnet diesen Monotonic-Hardware-Tick auf den letzten verifizierten NTP-Offset um. Dadurch entstehen **keine Latenzverzögerungen** durch Netzwerkaufrufe im Klickmoment.
- **Haptisches Feedback**: Jeder Klick wird über den Android `Vibrator`-Service quittiert.

---

## 4. Datenmodell & Room-Schema

### 4.1 Entität `Watch` (Tabelle `watches`)
Repräsentiert eine mechanische Uhr in der Sammlung:
- `id: Long` (Primärschlüssel, autogeneriert oder aus historischem Backup übernommen)
- `model: String` (Modellbezeichnung, z. B. "Klassische Automatikuhr")
- `serialNumber: String` (Optionale Seriennummer, z. B. "SN-48201")
- `caliber: String` (Uhrwerk / Kaliber, z. B. "Automatik 2824-2")
- `notes: String` (Notizen, z. B. "Letzte Revision Mai 2024")
- `createdAt: Long` (Erstellungszeitpunkt in Millisekunden)

### 4.2 Entität `MeasurementLog` (Tabelle `measurement_logs`)
Repräsentiert einen einzelnen Messpunkt:
- `id: Long` (Primärschlüssel)
- `watchId: Long` (Fremdschlüssel auf `watches.id`, `ON DELETE CASCADE`)
- `setId: Int` (Gruppierungs-ID einer Messreihe, Standard 0)
- `ntpTimestampMs: Long` (Exakte NTP-Referenzzeit in ms)
- `watchTimestampMs: Long` (Auf der Uhr angezeigte Zeigerzeit in ms)
- `positionCode: String` (Lagecode: `WORN`, `DU`, `DD`, etc.)
- `extraValue: Double` (Zusatzwert, z. B. Temperatur in °C oder Gangreserve)
- `notes: String` (Optionale Bemerkung zum Messpunkt)

---

## 5. Funktionsumfang im Detail

### 5.1 Uhren-Verwaltung (`WatchListScreen`)
- **Sortierung nach aktuellstem Messdatum**:
  - Die Uhr mit dem **aktuellsten Messdatum** wird standardmäßig an **oberster Stelle** der Uhrenliste aufgeführt.
  - Alle weiteren Uhren sind absteigend nach ihrem jeweils letzten Messzeitpunkt sortiert.
  - Uhren ohne bisherige Messungen werden am Ende der Liste (sortiert nach Anlagezeitpunkt `createdAt` absteigend) aufgeführt.
  - Jede Uhrenkarte zeigt das formatierte Datum der letzten Messung an (z. B. `Letzte Messung: 12.08.23 14:30`) bzw. `Noch keine Messungen`.
- Anzeige aller Uhren mit Seriennummer, Kaliber und Notiz-Vorschau.
- Anlegen neuer Uhren über Modal-Dialog mit automatischer ID-Generierung.
- Bearbeiten bestehender Stammdaten.
- Löschen von Uhren inklusive Sicherheitsabfrage (kaskadierendes Löschen aller zugehörigen Messpunkte).
- Schnellumschaltung zwischen den Uhren mit direktem Absprung in die Messwerterfassung („Messen“) oder direkt in die grafische Verlaufsauswertung („Diagramm“ öffnet unmittelbar den Diagramm-Tab).
- 1-Klick-Laden von neutralen Beispieldaten (Fliegerchronograph, Automatikuhr, Taucheruhr).

### 5.2 Messwerterfassung (`MeasureScreen`)
- **NTP-Status-Banner**: Zeigt Live-Verbindungsstatus, NTP-Servername, geschätzte Genauigkeit ($\pm X \text{ ms}$) und Atomzeit an.
- **Intelligente Ziel-Uhrenzeit-Voreinstellung**:
  - Die Ziel-Uhrzeit (:00-Sekundenmarke) wird automatisch auf Basis der letzten Messung dieser Uhr vorausberechnet:
    Geht die mechanische Uhr beispielsweise 2 Minuten vor ($\Delta t = +120\text{ s}$), liegt auch die voreingestellte Ziel-Uhrzeit 2 Minuten vor der Atomzeit.
  - Der Nutzer muss im Normalfall keine Minuten mehr manuell verstellen, sondern beobachtet lediglich das Zifferblatt seiner mechanischen Uhr und tippt im Nulldurchgang auf „JETZT!“.
  - Liegen bis zum nächsten vollen Zeigerminutensprung weniger als 15 Sekunden Reaktionszeit vor, springt die Voreinstellung automatisch auf die darauffolgende Zeigerminute für entspanntes Vorbereiten.
  - Schnellschalttasten (`-1 Min`, `+1 Min`, `Nächste Minute`) stehen weiterhin für manuelle Anpassungen zur Verfügung.
- **Voreinstellung von Lage und Temperatur**:
  - Für jede Uhr werden Position und Temperatur der vorherigen Messung automatisch voreingestellt.
  - **Temperatur-Standards**: Standardwert für die Temperatur ist **$20^\circ\text{C}$**, außer wenn die Position **„Am Arm“ (`WORN`)** gewählt ist, dann beträgt der Standardwert **$35^\circ\text{C}$** (Körperwärme).
  - Schnellwahl-Chips (`35°C (Am Arm)`, `20°C (Standard)`, `22°C (Zimmer)`, `18°C (Kühl)`) ermöglichen die blitzschnelle Feinjustierung.
- **Großer taktiler Erfassungs-Button („JETZT!“)**: Direkt unter der Ziel-Uhrenzeit platziert für optimale Blick- und Klickergonomie beim Beobachten des Zeigersprungs der mechanischen Uhr. Leuchtendes WatchCheck-Orange mit haptischer Vibrations-Rückmeldung.
- **Messperioden-Steuerung**:
  - Benennung der Perioden mit **Start- und Enddatum** wie bei WatchCheck2 (z. B. `12.08.23 - 25.08.23` oder `12.08.23`).
  - Möglichkeit, im Mess-Dialog eine neue Messperiode (`setId + 1`) zu beginnen (`Neue Periode (ab heute)`). Der neue Messpunkt wird automatisch als neue Referenz (Basispunkt) für die Periode definiert.
  - Bei bestehenden Perioden kann eine Zuordnung über die Date-Range-Chips erfolgen.
- **Info-Freitext-Eingabe (Permanent sichtbar)**: Direkt zugängliches Textfeld zur Eingabe von Notizen und Kontext (z. B. "Büro getragen", "Nachts 12 Uhr abgelegt") mit praktischen Schnellwahl-Chips.
- **Automatischer Workflow-Übergang**: Unmittelbar nach Speichern der Messung wechselt die Anwendung automatisch auf die tabellarische Uhren-Übersichtsseite und selektiert die betreffende Periode.

### 5.3 Uhren-Übersicht, Messperioden-Tabs & Tabellarische Messergebnisse (`ChartsScreen`)
Die Übersichtsseite bietet, getreu dem Vorbild der ursprünglichen WatchCheck-App, eine tabellarische und grafische Übersicht:
- **WatchCheck-Header**: Header-Banner mit Uhrwerkgrafik (`background_watchcheck.jpg`), rundem WatchCheck-Zifferblatt-Icon (`icon.png`) auf reinem weißem Hintergrund mit goldgelber Umrandung (`#FFA000`).
- **Standardmäßige Anzeige der aktuellsten Periode**:
  - Beim Starten der App sowie bei jedem Uhrenwechsel wird für die betreffende Uhr **standardmäßig automatisch die letzte/aktuellste Messperiode** selektiert und angezeigt.
- **Messperioden-Auswahl per Tabs (WatchCheck-Modus mit Start- und Enddatum)**:
  - Horizontale Tabs zur Auswahl der jeweiligen Messperiode benannt nach **Start- und Enddatum**: `[Alle (Gesamtanzahl)]`, `[12.08.23 - 25.08.23]`, `[01.09.23 - 15.09.23]`, ...
  - Sowohl die Tabelle als auch die Diagramme und die Statistik-Karten filtern synchron auf die gewählte Periode.
  - In einer Periode bildet die erste Messung die Referenz (`Gang = Ref.`); nachfolgende Messungen errechnen den täglichen Gang exakt innerhalb der betreffenden Periode.
- **Spaltenstruktur der Tabelle (WatchCheck-Layout)**:
  1. **Referenz (NTP)**: Exakter atomarer Zeitstempel **mit Sekunden** (Format: `dd.MM.yy HH:mm:ss`, z. B. `06.11.26 14:30:15`).
  2. **Abw. (Abweichung)**: Absolute Abweichung der Uhr von der Atomzeit in Sekunden mit Vorzeichen und **WatchCheck-Farben**: Grün (`#00AA00`) bei Vorgang ($+$) und Rot (`#AA0000`) bei Nachgang ($-$).
  3. **Gang**: Berechnete Gangdrift in Sekunden pro Tag ($s/d$), bei der Erstmessung einer Periode als `Ref.` markiert.
  4. **Lage**: Horologischer Lagen-Code mit farbkodiertem Badge (`WORN`, `DU`, `DD`, `CU`, `CD`, `CL`, `CR`, `WINDER`).
  5. **Temp.**: Anzeige des Zusatzwerts bzw. der Temperatur (z. B. `20°`, `35°` oder `-`).
  6. **Aktion**: Enthält den `(i)`-Info-Button für Freitexte und Details sowie das `Mülleimer`-Icon zum schnellen, direkten Löschen des Eintrags.
- **WatchCheck Result-Footer**: Fußzeile unter der Tabelle mit mittlerem Gang der Periode und Dauer des Beobachtungszeitraums in Tagen.
- **Direkte Zeilen-Editierung**: Durch Antippen einer beliebigen Zeile in der Tabelle öffnet sich sofort der Korrektur-Dialog (`EditMeasurementDialog`).

### 5.4 Nachträgliche Korrektur von Messpunkten (`EditMeasurementDialog`)
Ermöglicht das unkomplizierte Nachjustieren von:
- Zeigerzeit ($t_{\text{Uhr}}$) mit Schnelltasten ($-1\text{m}$, $-1\text{s}$, $+1\text{s}$, $+1\text{m}$) und Live-Vorschau der resultierenden Abweichung.
- Messperiode (`setId`): Zuordnung zu einer anderen Periode oder Neuanlage.
- Uhrenlage (`positionCode`).
- Info-Freitext und Zusatzwert (Temperatur).
- Drei Aktionsschaltflächen im Fußbereich gemäß WatchCheck-Vorlage:
  1. **Löschen**: Ermöglicht das direkte Entfernen der Messung über die Sicherheitsabfrage.
  2. **Abbrechen**: Verwirft alle Änderungen.
  3. **Speichern**: Übernimmt die korrigierten Parameter in die lokale Datenbank.
- Alle Diagramme, Gangraten und COSC-Statistiken werden bei Änderungen in Echtzeit neu berechnet.

### 5.5 Nachträgliches Löschen von Messpunkten
Messungen können an drei ergonomischen Stellen nachträglich gelöscht werden:
1. **Direkt in der Tabelle**: Über das Mülleimer-Icon in der Spalte "Aktion" jeder Zeile.
2. **Im Info-Dialog (`MeasurementInfoDialog`)**: Über die rote Schaltfläche "Löschen" neben dem "Bearbeiten"-Button.
3. **Im Korrektur-Dialog (`EditMeasurementDialog`)**: Über die "Löschen"-Schaltfläche in der Aktionsleiste.
- **Sicherheitsbestätigung**: Vor dem endgültigen Löschen erscheint der Original-Sicherheitsdialog mit sekundengenauem Zeitstempel:
  *„Möchten Sie wirklich den Logeintrag vom dd.MM.yyyy HH:mm:ss löschen?“*
- Nach dem Löschen erfolgt eine Snack-Nachricht (`Logeintrag vom ... erfolgreich gelöscht.`), und die Tabelle, Statistiken und Diagramme aktualisieren sich unmittelbar.

### 5.6 Interaktive Diagramme & Horologische Kennzahlen
Neben der Tabellenansicht steht ein Umschalter auf das **Liniendiagramm** bereit:
- **Robuste Canvas-Dimensionierung & Absturzschutz**:
  - Dynamische Ermittlung der tatsächlichen Pixelmaße über `DrawScope.size` statt unbegrenzter `BoxWithConstraints`-Höhenwerte (verhindert `Infinity`-Exceptions und Render-Crashes im vertikalen Scroll-Container).
  - Sicherheitsabfragen für Skalierungsberechnungen, Gitterlinien (`stepY`-Schleifenschutz) und Koordinaten-Clipping.
  - Sichere Leerzustände für Uhren mit weniger als zwei Messpunkten (Hinweiskarte mit klarer Handlungsanweisung).
- Vollständige Synchronisation mit den **Messperioden-Tabs** (Darstellung der gewählten Periode oder Gesamthistorie).
- Umschaltung: **Gangabweichung ($s/d$)** vs. **Kumulierte Gesamtabweichung ($s$)**.
- Farbkodierte Datenpunkte nach Uhrenlage mit Touch-Scrubbing-Tooltip.
- Horologische Statistik-Karte (mittlerer Gang, Min/Max, Gesamtdrift, COSC-Konformitätsprüfung).
- Lagenvergleichs-Tabelle mit Durchschnittswerten je Position.

### 5.7 Backup-Import & Export (`BackupScreen`)
- **Interaktiver Dateiauswahl-Dialog (Storage Access Framework)**:
  - Über die Schaltfläche *„WatchCheck-Datei auswählen (.data, .txt, .csv)“* öffnet sich der native Android System-Dateidialog (`ActivityResultContracts.OpenDocument`).
  - Unterstützt das direkte Auswählen von Original-WatchCheck-Exportdateien (`watchcheck2-*.data`), Text- und CSV-Dateien aus Downloads, internem Speicher oder Cloud-Laufwerken (Google Drive).
  - Völlig berechtigungsfrei (keine sensiblen `READ_EXTERNAL_STORAGE`-Berechtigungen erforderlich, 100% Play-Store-konform).
  - Zeigt Dateinamen und Dateigröße nach Auswahl übersichtlich als Badge an.
- **Originalgetreuer WatchCheck2-Parser**:
  - Erkennt optionale UTF-8 BOMs (`\uFEFF`) und ignoriert Header-Zeilen (`WatchCheck2` / `WatchCheck`) ohne Fehler oder Warnungen.
  - Unterstützt 4- und 5-spaltige `WATCH:`-Zeilen (`id|name|serial|createdAt|comment`) sowie `LOG:`-Zeilen (`id|watchId|period|refTime|watchTime|position|temperature|comment`).
  - Behandelt in WatchCheck vorkommende `"null"`-Werte, leere Notizen und historische ungesetzte Temperaturen (`-273` / `-273.15`) fehlertolerant.
- **Integritätsprüfung & Validierung**:
  - Prüft Gültigkeit von IDs und Zeitstempeln (Plausibilitätsfenster 2000 bis 2100).
  - Verwaiste Logs (deren `WatchID` im File fehlt) erhalten automatisch eine generierte Platzhalter-Uhr, sodass keine Messwerte verloren gehen.
- **Import-Vorschau**: Zeigt vor dem Schreibvorgang Anzahl erkannter Uhren, Messpunkte und Warnungen.
- **Dateiexport & Teilen**:
  - Ermöglicht das direkte Speichern als `watchcheck2-YYYYMMDD-HHmmss.data` Datei über den Android-Speicherdialog (`ActivityResultContracts.CreateDocument`).
  - Schaltflächen zum Kopieren in die Zwischenablage und Teilen per Android-Share-Sheet (`ACTION_SEND`).

### 5.8 Design-System, Heller & Dunkler Modus (WatchCheck2 Design & Theming)
- **Heller Modus (Light Mode) & Dunkler Modus (Dark Mode)**:
  - **Heller Modus (Standard)**: Originalgetreue Umsetzung der WatchCheck2-Applikation (`VERSION_2_0`). Strahlend weißer Kartenhintergrund (`#FFFFFF`), leichter Material-Hintergrund (`#FAFAFA`), Material Amber Kopfleiste (`#FFA000` / `#FFC107`) und kontraststarke Typografie (`#212121`).
  - **Reines Uhren-Icon auf weißem Grund**: Sowohl das App-Launcher-Icon als auch die Zifferblatt-Symbole in den Uhrenkarten und der Titelleiste erscheinen auf reinem **weißem Hintergrund** (`#FFFFFF`) mit goldgelber Amber-Umrandung (`#FFA000`), ohne störende orangefarbene Hintergründe.
  - **Kein künstlicher dunkler Banner**: Dunkle Bildleisten und Schriftzüge wie "WATCHCHECK" wurden entfernt. Die Uhrenübersicht beginnt sauber und aufgeräumt mit der Bestandsübersicht.
  - **Dunkler Modus**: Kontraststarkes AMOLED-Anthrazit (`#141414` / `#1F1F1F`) mit warmen WatchCheck-Amber-Akzenten (`#FFA000`).
  - **Systemstandard**: Automatische Umschaltung basierend auf der Systemeinstellung von Android (Tag/Nacht).
  - **Schnell-Umschalter in der TopAppBar**: Über das Sonnen-/Mondsichel-Symbol in der Kopfzeile kann das Design jederzeit mit einem Klick zwischen Hell, Dunkel und System umgeschaltet werden.
  - **Einstellungen im Backup-Bereich**: Direkte Auswahl über FilterChips mit persistenter Speicherung der Benutzereinstellung in den Android `SharedPreferences`.
- **Authentische Grafik-Assets**:
  - `icon.png`: Originales kreisrundes Zifferblatt-Logo von WatchCheck mit feinen Zeigern und Indizes auf weißem Grund.
  - `ic_notification.png`: Originales Benachrichtigungssymbol für Messungen.
  - `ic_launcher.png` und `ic_watchcheck_foreground.png`: Originales Launcher-Icon mit voller Adaptivitätsunterstützung für alle Displaydichten (mdpi bis xxxhdpi) auf weißem Hintergrund.
- **WatchCheck-Farbschema**:
  - Primärakzente in **Material Amber 500 / 700** (`#FFC107` / `#FFA000`) für Kopfzeilen, Tabellenköpfe und Ränder.
  - **Material Blue 500** (`#2196F3`) für Akzente und Sekundäraktionen.
  - **Grün `#00AA00`** für positive Abweichungen (Vorgang).
  - **Rot `#AA0000`** für negative Abweichungen (Nachgang).

### 5.9 Tägliche Mess-Erinnerung (WatchCheck Notification-Feature)
- **Erinnerungsfunktion wie bei WatchCheck**:
  - Ermöglicht das Aktivieren einer täglichen Benachrichtigung per Schalter im Einstellungsbereich (`BackupScreen`).
  - **Individuelle Uhrzeit**: Schnellauswahl über Chips (z. B. 08:00, 12:00, 18:00, 20:00, 21:30, 22:00 Uhr).
  - **Zuverlässiger Hintergrund-Alarm**: Implementiert über `AlarmManager.setAndAllowWhileIdle()` mit `ReminderReceiver` (`BroadcastReceiver`).
  - **Geräteneustart**: Registrierung für `BOOT_COMPLETED`, damit die tägliche Erinnerung auch nach dem Neustart des Geräts pünktlich wieder eingeplant wird.
  - **Berechtigungshandling ab Android 13 (`POST_NOTIFICATIONS`)**: Saubere Laufzeit-Berechtigungsabfrage vor Aktivierung der Benachrichtigung; bei Verweigerung bleibt die Einstellung deaktiviert.
  - **Klick auf Benachrichtigung**: Öffnet direkt die WatchAccurancy-App (`MainActivity`) mit Start der Messung.

### 5.10 Zweisprachigkeit (Deutsch & Englisch)
- **Automatische Anpassung an die Gerätesprache**:
  - Sämtliche Texte, Bezeichnungen, Dialoge, Navigationsleisten und Uhrenlagen sind vollständig lokalisiert:
    - **Deutsch (`values-de/strings.xml`)**: Wenn das Gerät auf Deutsch eingestellt ist.
    - **Englisch (`values/strings.xml`)**: Als internationaler Standard für alle anderen Gerätesprachen (Englisch, Französisch, Spanisch, etc.).
  - Datums- und Uhrzeitformatierungen verwenden `Locale.getDefault()` für landestypische Formatierung.

---

## 6. Historisches Sicherungsformat (WatchCheck-Kompatibilität)

### 6.1 Dateistruktur
Exportdateien beginnen mit dem Header `WatchCheck2` gefolgt von Uhrendeklarationen und Messprotokollen:

```text
WatchCheck2
# WatchAccurancy / WatchCheck Backup Export

WATCH: <ID>|<Modell>|<Seriennummer>|<Erstellt-Zeitstempel-MS>|<Notiz>
LOG: <LogID>|<WatchID>|<SetID>|<NTP-MS>|<Uhr-MS>|<Lage>|<Zusatzwert>|<Notiz>
```

### 6.2 Neutrale Horologie-Beispiele (Keine kommerziellen Markennamen)
```text
WatchCheck2
WATCH: 1|Handaufzug Chronograph|CHRONO-7712|1565100000000|Klassischer Fliegerchronograph
WATCH: 2|Klassische Automatikuhr|AUTO-2837|1573237608379|Alltagsuhr mit Datum
WATCH: 3|Taucheruhr Automatik|DIVER-9012|1581000000000|Wasserdicht 200m

LOG: 35|1|2|1565212365546|1565212500000|DU|20|Zifferblatt oben
LOG: 51|2|0|1573841028000|1573841040000|WORN|21|Getragen
LOG: 52|2|0|1573927428709|1573927440000|WORN|-273|Nachts 12O
```

---

## 7. Qualitätssicherung & Tests

Die Funktionalität ist durch automatisierte JVM-Tests abgesichert:
1. **`ExampleUnitTest.kt`**:
   - `testParseUserHistoricalBackupExample`: Validiert das Parsen von Backups mit neutralen Modellbezeichnungen.
   - `testParseAuthenticWatchCheckExportFileWithHeader`: Testet das Einlesen originaler WatchCheck2-Dateien mit `WatchCheck2`-Header, `"null"`-Tokens und `-273` Temperaturwerten.
   - `testExportRoundTrip`: Testet die verlustfreie Serialisierung und anschließende Re-Parsierung.
   - `testRateCalculatorDailyDrift`: Mathematische Verifikation der Gangberechnung (Drift von $+2{,}5 \text{ s/d}$ über 24 Stunden).
   - `testMultiPeriodRateCalculation`: Verifiziert die getrennte Gangberechnung über verschiedene Messperioden.
2. **`ExampleRobolectricTest.kt`**:
   - Verifiziert Ressourcen, App-Name (`WatchAccurancy`), Paketname (`de.uhrwerksarchiv.watchaccurancy`) und Android Context-Integration.

---

## 8. Wartung & Fortschreibung

Diese Spezifikation wird bei Erweiterungen des Datenmodells, neuen Analysemethoden und UI-Anpassungen fortlaufend aktuell gehalten.
