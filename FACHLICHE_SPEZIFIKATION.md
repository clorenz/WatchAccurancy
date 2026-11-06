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
3. **100% Abwärtskompatibilität**: Vollständige Unterstützung des historischen WatchCheck-Backup-Formats (`WATCH:`- und `LOG:`-Struktur) für nahtlosen Import bestehender Messhistorien sowie verlustfreien Export.
4. **Horologische Fachstandards**: Berechnung des täglichen Gangs in Sekunden pro Tag ($s/d$), Auswertung nach Uhrenlagen (Lagenprüfung) und Evaluierung nach COSC-Chronometernormen.

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
- `model: String` (Modellbezeichnung, z. B. "Rolex Datejust")
- `serialNumber: String` (Optionale Seriennummer)
- `caliber: String` (Uhrwerk / Kaliber, z. B. "Cal. 3135")
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
- Anzeige aller Uhren mit Seriennummer, Kaliber und Notiz-Vorschau.
- Anlegen neuer Uhren über Modal-Dialog mit automatischer ID-Generierung.
- Bearbeiten bestehender Stammdaten.
- Löschen von Uhren inklusive Sicherheitsabfrage (kaskadierendes Löschen aller zugehörigen Messpunkte).
- Schnellumschaltung zwischen den Uhren mit direktem Absprung in Messung oder Diagramm.
- 1-Klick-Laden von vorkonfigurierten historischen Beispieldaten (Rolex, Omega, Seiko).

### 5.2 Messwerterfassung (`MeasureScreen`)
- **NTP-Status-Banner**: Zeigt Live-Verbindungsstatus, NTP-Servername, geschätzte Genauigkeit ($\pm X \text{ ms}$) und Atomzeit an.
- **Ziel-Uhrenzeit-Wähler**: Automatische Vorbelegung auf die nächste volle Minute (`:00`) sowie Schnellschalttasten (`-1 Min`, `+1 Min`, `Nächste Minute`).
- **Großer taktiler Erfassungs-Button („JETZT!“)**: Direkt unter der Ziel-Uhrenzeit platziert für optimale Blick- und Klickergonomie beim Beobachten des Zeigersprungs der mechanischen Uhr. Leuchtendes WatchCheck-Orange mit haptischer Vibrations-Rückmeldung.
- **Messperioden-Steuerung (WatchCheck-Sets)**: Möglichkeit, im Mess-Dialog eine neue Messperiode (`setId + 1`) zu beginnen. Der neue Messpunkt wird automatisch als neue Referenz (Basispunkt) für die Periode definiert. Bei bestehenden Perioden kann auch eine Zuordnung vorgenommen werden.
- **Lagen-Auswahl**: Übersichtliches Dropdown mit Farbkodierung und horologischen Erläuterungen für alle 8 Lagen.
- **Info-Freitext-Eingabe (Permanent sichtbar)**: Direkt zugängliches Textfeld zur Eingabe von Notizen und Kontext (z. B. "Büro getragen", "Nachts 12 Uhr abgelegt", "Temperatur 21°C") mit praktischen Schnellwahl-Chips.
- **Zusatzwert**: Separates numerisches Eingabefeld (z. B. für Temperatur in °C oder Gangreserve).
- **Automatischer Workflow-Übergang**: Unmittelbar nach Speichern der Messung wechselt die Anwendung automatisch auf die tabellarische Uhren-Übersichtsseite und selektiert die betreffende Periode.

### 5.3 Uhren-Übersicht, Messperioden-Tabs & Tabellarische Messergebnisse (`ChartsScreen`)
Die Übersichtsseite bietet, getreu dem Vorbild der ursprünglichen WatchCheck-App, eine tabellarische und grafische Übersicht:
- **Messperioden-Auswahl per Tabs (WatchCheck-Modus)**:
  - Horizontale Tabs zur Auswahl der jeweiligen Messperiode: `[Alle (Gesamtanzahl)]`, `[Periode 1]`, `[Periode 2]`, ...
  - Sowohl die Tabelle als auch die Diagramme und die Statistik-Karten filtern synchron auf die gewählte Periode.
  - In einer Periode bildet die erste Messung die Referenz (`Gang = Referenz`); nachfolgende Messungen errechnen den täglichen Gang exakt innerhalb der betreffenden Periode.
- **Spaltenstruktur der Tabelle**:
  1. **Referenz (NTP)**: Exakter atomarer Zeitstempel **mit Sekunden** (Format: `dd.MM.yy HH:mm:ss`, z. B. `06.11.26 14:30:15`).
  2. **Abweichung**: Absolute Abweichung der Uhr von der Atomzeit in Sekunden mit Vorzeichen (z. B. `+2.45s`).
  3. **Täglicher Gang**: Berechnete Gangdrift in Sekunden pro Tag ($s/d$), bei der Erstmessung einer Periode als `Ref.` markiert.
  4. **Lage**: Horologischer Lagen-Code mit farbkodiertem Badge (`WORN`, `DU`, `DD`, `CU`, `CD`, `CL`, `CR`, `WINDER`).
  5. **(I)-Info-Button**: Zeigt beim Antippen einen modalen Info-Dialog (`MeasurementInfoDialog`) mit dem vollständigen Freitext, Zusatzwerten, Referenz- und Zeigerzeit sowie Lageninformationen.
- **Direkte Zeilen-Editierung**: Durch Antippen einer beliebigen Zeile in der Tabelle öffnet sich sofort der Korrektur-Dialog (`EditMeasurementDialog`).

### 5.4 Nachträgliche Korrektur von Messpunkten (`EditMeasurementDialog`)
Ermöglicht das unkomplizierte Nachjustieren von:
- Zeigerzeit ($t_{\text{Uhr}}$) mit Schnelltasten ($-1\text{m}$, $-1\text{s}$, $+1\text{s}$, $+1\text{m}$) und Live-Vorschau der resultierenden Abweichung.
- Messperiode (`setId`): Zuordnung zu einer anderen Periode oder Neuanlage.
- Uhrenlage (`positionCode`).
- Info-Freitext und Zusatzwert (Temperatur).
- Alle Diagramme, Gangraten und COSC-Statistiken werden bei Änderungen in Echtzeit neu berechnet.

### 5.5 Interaktive Diagramme & Horologische Kennzahlen
Neben der Tabellenansicht steht ein Umschalter auf das **Liniendiagramm** bereit:
- Vollständige Synchronisation mit den **Messperioden-Tabs** (Darstellung der gewählten Periode oder Gesamthistorie).
- Umschaltung: **Gangabweichung ($s/d$)** vs. **Kumulierte Gesamtabweichung ($s$)**.
- Farbkodierte Datenpunkte nach Uhrenlage mit Touch-Scrubbing-Tooltip.
- Horologische Statistik-Karte (mittlerer Gang, Min/Max, Gesamtdrift, COSC-Konformitätsprüfung).
- Lagenvergleichs-Tabelle mit Durchschnittswerten je Position.

### 5.6 Backup-Import & Export (`BackupScreen`)
- **Historischer WatchCheck-Parser**:
  Liest zeilenweise Textdateien im Format:
  ```text
  WATCH: ID|Modell|Seriennummer|Erstellt-Zeitstempel|
  LOG: LogID|WatchID|SetID|NTP-Zeitstempel-MS|Uhr-Zeitstempel-MS|Lage-Code|Zusatzwert|NotizText
  ```
- **Integritätsprüfung & Validierung**:
  - Prüft Gültigkeit von IDs und Zeitstempeln (Plausibilitätsfenster 2000 bis 2100).
  - Verwaiste Logs (deren `WatchID` im File fehlt) erhalten automatisch eine generierte Platzhalter-Uhr, sodass keine Messwerte verloren gehen.
- **Import-Vorschau**: Zeigt vor dem Schreibvorgang Anzahl erkannter Uhren, Messpunkte und Warnungen.
- **Export**: Erzeugt identisch formatierte Text-Dateien zum Kopieren in die Zwischenablage oder Teilen per Android-Share-Sheet (`ACTION_SEND`).

### 5.7 Design-System & Visuelle Identität (WatchCheck Orange)
- **Klassisches Farbschema**: Wiedererkennbarer, kontraststarker Look der Original-App mit leuchtendem Signal-Orange (`#FF6D00` / `#E65100`), tiefem Anthrazit (`#141414` / `#1F1F1F`) und Akzenten in Gold/Amber.
- **Erfassungs-Button**: Leuchtendes Radial-Gradient in WatchCheck-Orange für maximale Übersicht und taktile Zielgenauigkeit im Moment des Sekundenzeigersprungs.
- **App-Icon**: Klassisches WatchCheck-Zifferblatt mit Zeigern und Präzisions-Häkchen auf leuchtendem Orange-Hintergrund als adaptives Android-Icon mit Raster-Fallbacks für alle Dichten.

---

## 6. Historisches CSV-Sicherungsformat

### 6.1 Uhren-Datensatz (`WATCH:`)
```text
WATCH: <ID>|<Modell>|<Seriennummer>|<Erstellt-Zeitstempel-MS>|
```
*Beispiel:*
```text
WATCH: 3|Rolex Datejust|E283744|1573237608379|
```

### 6.2 Messpunkt-Datensatz (`LOG:`)
```text
LOG: <LogID>|<WatchID>|<SetID>|<NTP-MS>|<Uhr-MS>|<Lage>|<Zusatzwert>|<Notiz>
```
*Beispiele:*
```text
LOG: 35|1|2|1565212365546|1565212500000|DU|20|
LOG: 52|3|0|1573927428709|1573927440000|WORN|-273|Nachts 12O
```

---

## 7. Qualitätssicherung & Tests

Die Funktionalität ist durch automatisierte JVM-Tests abgesichert:
1. **`ExampleUnitTest.kt`**:
   - `testParseUserHistoricalBackupExample`: Validiert das Parsen exakt der in der Aufgabenstellung genannten Beispiele (`WATCH: 3...`, `LOG: 35...`, `LOG: 52...`).
   - `testExportRoundTrip`: Testet die verlustfreie Serialisierung und anschließende Re-Parsierung.
   - `testRateCalculatorDailyDrift`: Mathematische Verifikation der Gangberechnung (Drift von $+2{,}5 \text{ s/d}$ über 24 Stunden).
2. **`ExampleRobolectricTest.kt`**:
   - Verifiziert Ressourcen, App-Name (`WatchAccurancy`) und Android Context-Integration.

---

## 8. Wartung & Fortschreibung

Diese Spezifikation wird bei Erweiterungen des Datenmodells, neuen Analysemethoden (z. B. Fourier-Frequenzanalyse des Gangs oder PDF-Export) und UI-Anpassungen fortlaufend aktualisiert.
