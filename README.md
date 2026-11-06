# TempoTap

Eine moderne Android-App zum Messen von **BPM (Beats per Minute)** per Tap – inspiriert von der Idee von [beatsperminuteonline.com](https://www.beatsperminuteonline.com/), aber komplett eigenständig als native App umgesetzt.

## Funktionen

- **Tap-Tempo** mit großer Tippfläche, Ausreißer-Filter, Streuungsanzeige und Stabilitäts-Feedback
- **Präzises Metronom** mit sample-genauem Klick, Akzent auf der Eins, Takten 2/3/4/6 und Auto-Stopp
- **Verlauf** mit automatisch gespeicherten Sessions, Statistik und Liniendiagramm
- **Material You** (dynamische Farben ab Android 12) inklusive Fallback-Palette, Dark Mode und edge-to-edge Layout
- **Adaptive Oberfläche** für Smartphones, Tablets und Foldables (Bottom-Navigation bzw. Navigation Rail, zweispaltige Tap-Ansicht)
- **Lokal & privat**: keine Konten, keine Netzwerkberechtigung, alle Daten bleiben auf dem Gerät
- Haptik, optionaler Tipp-Klang, Bildschirm-wach-halten, konfigurierbares Session-Timeout
- Deutsch als Standardsprache, englische Lokalisierung

## Screens

| Tempo | Metronom | Verlauf | Einstellungen |
| --- | --- | --- | --- |
| Große Tap-Fläche, Live-BPM, Statistik | Tempo-Slider, Beat-Anzeige, Presets | Sessions, Ø-Wert, Chart | Design, Verhalten, Daten |

## Tech-Stack

- Kotlin 2.4, Jetpack Compose, Material 3
- Navigation Compose, Lifecycle ViewModels, StateFlow
- DataStore Preferences (Einstellungen + Sessions als JSON)
- kotlinx.serialization
- AudioTrack-basiertes Metronom (klanglich synthetisiert, kein Sample-Asset)
- minSdk 26, target/compileSdk 36

## Bauen

### Android Studio

1. Projektordner `TempoTap` in Android Studio öffnen.
2. Gradle synchronisieren lassen.
3. Auf einem Gerät/Emulator starten oder **Build → Build Bundle(s)/APK(s) → Build APK(s)** wählen.

### Kommandozeile

```bash
cd TempoTap
./gradlew :app:assembleDebug
```

Die Debug-APK liegt danach unter:

```
app/build/outputs/apk/debug/app-debug.apk
```

Voraussetzung: JDK 17+ und Android SDK 36 (`platforms;android-36`, `build-tools;36.0.0`).

### Fertige APK

Im Projektordner liegt zusätzlich `TempoTap-1.0.0-debug.apk` (Debug-Signatur, direkt installierbar). Die Datei kann per Dateimanager auf dem Gerät geöffnet oder über ADB installiert werden:

```bash
adb install TempoTap-1.0.0-debug.apk
```

Für eine Weitergabe an andere Geräte sollte ein eigener Release-Key verwendet werden; die Debug-APK ist dafür nicht gedacht.

## Architektur

```
app/src/main/java/de/tempotap/app/
├── audio/        ClickPlayer, MetronomeEngine, MetronomeController
├── data/         DataStore-Repositories, Modelle
├── tempo/        Reine, testbare BPM-Logik
├── ui/
│   ├── about/    Über-Screen
│   ├── components/ gemeinsame UI-Bausteine
│   ├── history/  Verlauf + Chart
│   ├── metronome/Metronom-Screen
│   ├── settings/ Einstellungen
│   ├── tempo/    Tap-Screen
│   └── theme/    Material-3-Theme, Typografie, Shapes
└── MainActivity.kt
```

Die BPM-Berechnung ist bewusst frei von Android-Abhängigkeiten und wird in `app/src/test` getestet:

```bash
./gradlew :app:testDebugUnitTest
```

## Hinweise

- Das Metronom läuft, solange der App-Prozess lebt. Für dauerhaftes Abspielen im Hintergrund wäre ein Foreground-Service mit Medien-Notification der nächste Ausbauschritt.
- Sessions werden nach einer Pause automatisch gespeichert und auf 100 Einträge begrenzt.
- Es werden keine Inhalte, Marken oder Assets von beatsperminuteonline.com verwendet. Die Website diente nur als funktionale Inspiration.
