# Quintana

Native Android interval timer with a neat medieval name. Create sets of work, rest and rounds,
swipe to the one you want and start. Nothing more: no sports, no accounts, no statistics.

The quintain was the rotating dummy knights trained on: whoever hit it had to move on before its
counter-arm swung back. The logo shows a shield with the quintain, whose arm doubles as a clock hand.

- Portrait only, dark theme only, German interface.
- Completely offline: the app has **no `INTERNET` permission**. The sets are kept in the app's
  private storage and move to a new phone with the Android device backup.

## Install

Every push to `main` and every pull request builds an APK in GitHub Actions. Open the latest
[Android workflow run](https://github.com/GitRon/quintana/actions/workflows/android.yml), download
the `quintana-apk` artifact, unzip it and install `quintana.apk` on the phone (allow installing from
unknown sources when asked). The settings show the version, which is the CI run number and commit.

### Signing key

CI signs the APK with a fixed key from the repository secrets, so newer builds install as updates
and keep the sets. Create the key once:

```
keytool -genkeypair -keystore quintana.jks -storetype PKCS12 -alias quintana \
    -keyalg RSA -keysize 4096 -validity 36500 -dname "CN=Quintana"
```

Then add two secrets under *Settings → Secrets and variables → Actions*:

- `QUINTANA_KEYSTORE_BASE64`: the output of `base64 -w0 quintana.jks` (macOS: `base64 -i quintana.jks`)
- `QUINTANA_KEYSTORE_PASSWORD`: the password you chose

Keep `quintana.jks` and its password somewhere safe outside the repository; the Play Store needs
the same key for every update. Without the secrets, CI builds a debug APK signed with a throwaway
key, which never installs over a previous build.

With the key, CI also builds a signed App Bundle for the Play Store, the `quintana-aab` artifact.
With Play App Signing, this key is the upload key and Google signs the installed app.

Requires Android 10 or later.

## Usage

- Swipe between sets on the start screen; the last page creates a new set. The pager opens on
  the set used last.
- The pencil edits the shown set. Editing never creates a new set; **Zurück** drops the changes.
- **START** runs the set: the lead-in (gold), then work (red) and rest (blue) in turn. The last
  seconds of each rest are the lead-in to the next round. No rest follows the last round.
- **PAUSE** freezes the run. The stop button has to be **held for a second**, so a gloved hand does
  not end the run by accident. The system back button does nothing while a run is on.
- The settings hold the lead-in for all sets (10 s by default, 0 switches it off) and the list
  of sets, where a set is deleted. A snackbar offers undo for five seconds.

### Signals

| Event | Sound |
| --- | --- |
| Lead-in begins | two short high tones |
| Last 3 s of the lead-in | one tick per second |
| Round begins | bell, once |
| Round ends | bell, twice |
| Set finished | bell, three times |

The signals play on the media stream, so the media volume sets their loudness, and music playing
at the same time only gets quieter for a moment. They are synthesized by
[`tools/generate_sounds.py`](tools/generate_sounds.py), so they carry no third-party license.

### Display and background

The display stays on while a run is on, paused or not; otherwise the system timeout applies. When
the app goes to the background or the phone is locked, a foreground service keeps the run going
with a notification that shows the phase and the time left and can pause it. The service holds a
partial wake lock so the signals come on time, and only runs while a run does.

The time is computed from a monotonic clock (start + elapsed), not by counting ticks, so the
timer does not drift.

## Build

```
./gradlew assembleDebug
```

Needs the Android SDK (compile SDK 35) and JDK 17 or later.

### Checks

CI runs these on every push to `main` and every pull request and fails if one of them does:

```
./gradlew spotlessCheck lintDebug testDebugUnitTest
```

- `spotlessCheck`: ktlint formatting as configured in `.editorconfig`; `./gradlew spotlessApply` fixes it.
- `lintDebug`: Android Lint, with warnings treated as errors. The report is in
  `app/build/reports/lint-results-debug.html`.
- `testDebugUnitTest`: JVM unit tests of the timer plan, its signals, the pausable clock and the
  stored sets. `./gradlew koverHtmlReportDebug` also measures line coverage (report in
  `app/build/reports/kover/htmlDebug`); CI shows it in the job summary but enforces no minimum.

CI also checks that the APK requests no `INTERNET` permission, which a library could otherwise add
through manifest merging.

## How it works

- `Schedule` turns a set and the lead-in into segments (lead-in, work, rest) and the cues for the
  signals. `RunClock` measures the elapsed time on `SystemClock.elapsedRealtime()` and pauses.
- `TimerService` is the foreground service: it plays the cues through `SignalPlayer` (`SoundPool`
  with transient, ducking audio focus) and updates the notification.
- `ActiveRun` holds the run shared by the service and the Compose UI, which redraws the ring every
  frame from the same clock.
- `LibraryStore` keeps the sets and settings in `library.json` in the app's files.

## Play Store

- The timer service uses the foreground service type `specialUse`, since none of the specific
  types fits an interval timer. The Play Console asks for a short justification of it.
- A 512 × 512 icon for the store listing is in [`art/quintana-icon.png`](art/quintana-icon.png).
- Still open: a privacy policy for the listing, even though the app collects no data.

## Fonts

[Cinzel](https://github.com/NDISCOVER/Cinzel), [Barlow and Barlow Condensed](https://github.com/jpt/barlow),
all under the SIL Open Font License; the licenses are in `app/src/main/assets/licenses`.
