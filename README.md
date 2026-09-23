# LevelPitch

Android app for levelling a parked camper with drive-on wedges. It measures the
vehicle's tilt with the phone's accelerometer and recommends a wedge step per
wheel. An experimental camera mode estimates ground heights with ARCore depth.
See [SPEC.md](SPEC.md) for the product spec and milestones.

Status: **M3** (usable level loop). Set up vehicles and wedges, calibrate the
zero, then measure → place the recommended wedges → measure again until
level. Sensor bench results are in [docs/bench-validation.md](docs/bench-validation.md).

## Requirements

- JDK 17 (a full JDK with `javac`). Gradle finds it automatically through
  `gradle/gradle-daemon-jvm.properties`; any JRE can launch `./gradlew`.
- Android SDK with platform 36. Set its path in `local.properties`
  (`sdk.dir=/path/to/Android/Sdk`, not committed) or with `ANDROID_HOME`.

## Build, test, install

```sh
./gradlew testDebugUnitTest   # all JVM unit tests (:app and :leveling), no device needed
./gradlew assembleDebug       # build the debug APK
./gradlew installDebug        # install on a connected device/emulator via adb
```

CI (`.github/workflows/ci.yml`) runs `testDebugUnitTest assembleDebug` on every
push and pull request.

## Layout

| Path | Contents |
|---|---|
| `app/` | Android app: Compose UI (level loop, profiles, calibration), accelerometer sampling, JSON profile store |
| `leveling/` | Pure Kotlin/JVM module: levelling math and recommendation engine, no Android deps |
| `docs/` | Bench validation protocol and results |

## Conventions

Vehicle frame: x forward, y left, z up. Pitch is positive when the nose is up, roll
is positive when the left side is up. The unit tests in `leveling/` are the source
of truth for these signs.
