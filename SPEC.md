# LevelPitch — Spec (working name)

Android app that tells you how to level a parked camper with drive-on wedges.
Learning project with a real use case; personal use first, Play Store release
later if it works well.

## Problem

After parking on a pitch, the vehicle is rarely level. Today: spirit level,
guess a wedge, drive on, check again. The app replaces the guessing with a
measurement and a concrete recommendation ("front left: step 2, rear left:
step 1"), and verifies the result.

## Goals

- **Simple mode (primary, ships first):** measure vehicle tilt with the phone's
  accelerometer, phone lying inside the vehicle. Target accuracy ≤ 0.25°
  after calibration: half the default 0.5° levelling tolerance, so "level
  within 0.5°" is trustworthy and step choices near a boundary stay right
  (one 30 mm step ≈ 1.1° roll over a 1.6 m track).
- **Wedge recommendation:** per-wheel wedge steps for the user's own stepped
  wedges, plus residual tilt after applying them.
- **Camera mode (experimental, ships second):** estimate ground heights at the
  wheel contact points from outside using ARCore depth, feed the same
  recommendation engine, and **measure its accuracy against simple mode**.

## Non-goals

- Measuring a pitch *before* parking or comparing pitches.
- Hydraulic jacks, levelling systems, stabiliser legs.
- Live guidance while driving onto wedges, or streaming to a second device.
- Vehicle body tilt from the camera (possible later stretch goal).
- Per-frame segmentation in camera mode (see ObjectViz cost tradeoff).
- Cloud features, accounts, network access of any kind.

## Supported vehicles

| Type | Levelling DoF | Output |
|---|---|---|
| 2-axle motorhome / van | pitch + roll, any wheel can be raised | wedge step per wheel |
| Single-axle caravan | roll via wedges; pitch via jockey wheel | wedge step per side + jockey wheel up/down in cm |
| Tandem-axle caravan | roll via wedges (both wheels of a side together); pitch via jockey wheel | wedge step per side + jockey wheel cm |

## Core flows

**F1 Setup (once).** Create a vehicle profile (type, dimensions) and an
equipment profile (wedge step heights, number of wedges owned). Choose a
measuring surface inside the vehicle and how the phone lies on it (top edge
toward vehicle front by default; 90° rotations selectable).

**F2 Zero calibration (once per vehicle, repeatable).** With the vehicle known
level (e.g. checked with a spirit level on the chassis or on a level pad), tap
"Set zero". Stores a pitch/roll offset that absorbs both the surface-vs-chassis
misalignment and the sensor bias.

**F3 Level loop (every pitch).**
1. Place phone on the surface, tap Measure. The app averages ~2 s of
   accelerometer data and rejects the result if the phone moved.
2. Shows pitch/roll, a top-down vehicle diagram, and the wedge recommendation
   with residual tilt ("level within 0.2°").
3. User places wedges, drives on, and marks which wedges are in place.
4. Re-measure. The app computes a correction relative to the current wedge
   state ("front left: one step higher") until within tolerance (default 0.5°).

Safety line in the UI: handbrake and chocks before leaving the vehicle.

**F4 Camera scan (experimental).**
1. Stand in front of or behind the vehicle so both wheels of one axle are in
   view. The ARCore session starts and detects the ground.
2. Tap each visible wheel once. The tap becomes a point prompt for MobileSAM
   on that single frame, giving a tyre mask.
3. Move slowly for a few seconds while keyframes record. Ground depth in an
   annulus around each tyre's contact region (tyre mask excluded) is fused
   into voxels, and a RANSAC plane is fitted locally per wheel.
4. Contact-point height = plane height at the tyre's contact point in
   ARCore's gravity-aligned world frame.
5. Heights go to the same recommendation engine; AR labels are anchored at
   each wheel ("+6 cm / step 2").
6. For 2-axle vehicles, repeat at the other end. Linking the front and rear
   axle (pitch) needs tracking across the vehicle length, which is the main
   accuracy risk (see Open questions).

**F5 Evaluation (experimental).** After a camera scan, the user levels using
the camera recommendation, then runs a simple-mode measurement. The app logs
predicted heights, recommended steps, the camera-implied tilt, the IMU tilt
and the residual after levelling. Logs export as JSON for offline analysis
(`tools/` Python scripts, as in ObjectViz).

## Recommendation engine (pure Kotlin)

- Vehicle frame: x forward, y left, z up; wheel contact points from the
  profile.
- Input is **either** tilt (pitch θ, roll φ) from simple mode **or** measured
  contact heights from camera mode. Tilt converts to heights with
  z_i = x_i·tan θ + y_i·tan φ.
- Wheels are only ever raised, never lowered. Candidates are all
  combinations of available steps (including none) under the "wedges owned"
  limit. The search space is tiny (≤ 4 wheels × few steps), so brute force.
- Frame origin: centre of the wheel footprint (mid-wheelbase for motorhomes,
  (mid-)axle centre for caravans). One wheel stands on one step of one
  wedge; a tandem caravan's two wheels of a side always get the same step,
  so raising a side costs two wedges. Output is a step per wheel (0 = none).
- Objective: minimize residual tilt of the least-squares plane through the
  raised contact points: the total tilt (steepest slope, combining pitch and
  roll) for motorhomes, |roll| for caravans. Residuals within 0.01° count
  as equal; ties go to fewer changes (re-measure only), then fewer wedges,
  then lower wedges.
- Caravans: optimize roll only via wedges; pitch becomes a jockey wheel
  adjustment that brings the hitch to axle height *after* the wedges are in
  place: Δhitch = mean raised wheel height − hitch-to-axle distance × tan θ
  (+ = raise the hitch). Wedges lift the axle centre, e.g. one side +30 mm
  means +15 mm at the hitch. Camera heights say nothing about the body, so a
  caravan gets jockey advice only from a tilt measurement.
- If the tallest step is insufficient, report the best achievable residual
  and suggest turning or repositioning the vehicle.
- Level check on a measurement: motorhome total tilt ≤ tolerance; caravan
  |roll| ≤ tolerance and |pitch| ≤ tolerance (otherwise the jockey wheel
  still needs adjusting). Once level, the loop stops suggesting changes.
- Re-measure: the new measurement is relative to the current wedge state,
  so new target = current heights + correction, snapped again. Implemented
  as: ground heights = heights from the new tilt − current wedge heights,
  then the normal search; the result is an absolute wedge state plus the
  per-wheel step changes. Ties first prefer fewer changes to the current
  state (don't make the user drive off equally good wedges). A tilt
  measurement cannot see chassis twist, so twist from the current wedges is
  not recovered.

## Sensor measurement (simple mode)

- Phone lies screen-up; `PhoneOrientation` says where its top edge points
  (front/left/rear/right, 0/90/180/270° counter-clockwise from above) and
  maps Android sensor axes (x right edge, y top edge, z out of screen) to
  the vehicle frame.
- Angles are what a spirit level along each vehicle axis shows:
  pitch = asin(up_x), roll = asin(up_y) of the unit up vector (the mean
  accelerometer reading at rest). The engine's tan form differs by
  1/cos(angle) in height (0.4 % at 5°), which is negligible.
- Zero calibration stores the raw tilt read on a level vehicle (it holds
  surface misalignment + sensor bias). Readings are corrected by the
  smallest rotation that makes that zero vertical: exact for a tilted
  surface (plain angle subtraction would be ≤ 0.02° off at 5°/5°). It
  cannot correct a phone *turned* on the surface: a 5° turn mixes
  sin 5° ≈ 9 % of pitch into roll (0.26° at 3° tilt), so the phone must be
  aligned with an edge of the surface.
- Measure: wait 0.5 s for the tap to settle, then average ~2 s of raw
  accelerometer samples (TYPE_ACCELEROMETER, ~100 Hz). Rejected if the phone
  is not flat screen-up (> 15°), if single samples scatter too much (RMS
  > 0.5°, vibration) or if a quarter-window mean drifts from the overall
  mean (> 0.1°, rocking). Thresholds are starting values for the bench test.
- Orientation and one zero per orientation are part of the vehicle profile
  (M3); the zero depends on orientation because the sensor bias turns with
  the phone.

## Vehicle presets

The vehicle editor offers common motorhome base vehicles (Ducato platform
incl. its motorhome chassis with 1980 mm rear track, Sprinter, Crafter/TGE,
Transit, VW T6/T6.1) with their wheelbases; choosing one fills type,
wheelbase and track, and a new vehicle starts from the most common one
(Ducato motorhome chassis, 4035 mm). Values are manufacturer figures
(September 2026); the engine uses one track, so presets store the mean of
front and rear. A few cm off changes recommended heights by ~1 %, so
presets are good enough, but the editor asks to compare with the vehicle
papers because converters sometimes change the chassis. Caravans have no
common bases, so they get no presets.

## Data model

```
VehicleProfile   id, name, type {MOTORHOME_2AXLE, CARAVAN_SINGLE, CARAVAN_TANDEM},
                 wheelbaseMm, trackMm, hitchToAxleMm?, tandemSpacingMm?,
                 phoneOrientation {0,90,180,270},
                 zeroOffsets {orientation -> (pitchDeg, rollDeg)}, toleranceDeg = 0.5
EquipmentProfile id, name, stepHeightsMm [e.g. 30, 60, 90], wedgesOwned
Measurement      timestamp, source {IMU, CAMERA}, pitchDeg, rollDeg (zero-corrected),
                 noiseDeg, driftDeg, sampleCount, wedgeState at measurement time,
                 contactHeightsMm? (camera)
LevelSession     id, vehicleId, equipmentId, measurements[], wedgeState {wheel -> step}
CameraCaptureLog sessionId, perWheel {contactPoint, planeNormal, inliers, rmsMm},
                 trackingInfo, linked IMU measurement
```

Storage (decided in M3): one kotlinx.serialization JSON file in app-private
storage with all profiles, the active vehicle and wedge set, and the current
level session (so the wedge state survives an app restart). A few profiles
don't need a database, and the format matches the F5 JSON export. Written
atomically; an unreadable file is kept aside as `.corrupt` and the app starts
empty. One zero per orientation (bench result, M2), so `zeroOffsets` replaces
the single zeroOffset.

## Architecture & stack

- Kotlin, Jetpack Compose, single activity, ViewModels (same stack as ObjectViz).
- Base package `io.github.cnissler.levelpitch`.
- Gradle module `:leveling`: pure Kotlin/JVM (math and recommendation
  engine), so the compiler rules out Android deps. `:app` depends on it, and
  `./gradlew testDebugUnitTest` also runs its tests.
- Packages in `:app`: `sensor/`, `profiles/` (model + JSON repository),
  `ui/` (level loop, `profiles/` editors, calibration; navigation-compose),
  `ar/` (experimental, isolated). Screen logic that decides what to show
  (`levelUiState`, form parsing) is pure Kotlin with JVM tests.
- ARCore declared **optional**, so the app installs on any device. Camera mode
  is hidden when Depth is unsupported, and behind an "Experimental" toggle
  for now.
- Reuse from ObjectViz, adapted rather than copied blindly: `DepthProjection`
  (use `textureIntrinsics`; 16-bit depth findings), voxel merging,
  `fitPlane` RANSAC + eigenvector refinement, levelness gate.
  **Differences to revisit:** depth range (ObjectViz clips at 1.5 m; ground
  here is 1–4 m away), plane fit is local per wheel rather than one global
  supporting plane, and the object of interest is the ground itself.

## Testing

- JVM unit tests for all math: angle conversion and sign conventions,
  recommendation engine (all vehicle types, step snapping, insufficient steps,
  re-measure correction). Run via `./gradlew testDebugUnitTest`.
- Emulator: UI flows; tilt via the emulator's virtual sensor controls.
- Pixel 10 Pro over USB/adb: real accuracy. Protocol: board on a level
  surface, raise one end with shims of known thickness (1 cm over 57.3 cm ≈
  1°), compare against the app and a spirit level; repeat across several
  angles and phone orientations.
- Camera mode: logged field captures on the real vehicle, analysed offline.

## Milestones (each = several small, focused commits; tests green at each commit)

- **M0** Project skeleton: Gradle, Compose shell, CI-able test task, README.
- **M1** Recommendation engine + unit tests (pure Kotlin).
- **M2** Sensor measurement: averaging, stillness detection, orientation
  handling, zero calibration; bench-validated on the Pixel.
- **M3** Profiles + level loop UI + wedge state + re-measure. → **usable
  for real trips.**
- **M4** ARCore session, ground detection, wheel tap + single-frame MobileSAM.
- **M5** Per-wheel local ground plane + contact heights → recommendation.
- **M6** AR wheel labels, evaluation logging, JSON export, analysis script.
- **Later** Play Store readiness: English + German UI, onboarding, handling
  devices without Depth, privacy/data-safety declarations.

## Open questions / risks

- ~~Accelerometer bias and noise on the Pixel: does zero calibration alone
  reach the target, or is a flip calibration needed?~~ Answered by the M2
  bench test (docs/bench-validation.md, rough setup): noise 0.04° per
  sample, drift ≤ 0.01°; with a zero, 1–5° pitch reads within ±0.17° with
  no trend over angle, and ~10° reads 9.95–10.31° in all four orientations.
  Offsets that turn with the phone are 0.3–0.7°, so one zero *per
  orientation* is required. A flip calibration is not needed. Putting the
  phone back against a stop repeats within ≈ 0.01°.
- **Roll crosstalk from a turned phone** (open, main simple-mode risk;
  mitigation parked on 2026-09-23 and not part of M3, revisit after M3): on
  the bench, pure pitch also read as roll, 3–9 % below 5° and 0–20 % at 10°
  depending on orientation, which fits the phone lying turned by up to ~11°.
  At a typical 2–3° tilt that is 0.2–0.6° of false roll, above the 0.25°
  target. A zero cannot remove it. Options:
  (a) UI guidance: a fixed spot with a stop, phone edge along it;
  (b) learn the turn from a known pure-pitch change: when both wheels of one
  axle go up by the same step, the measured change should be pure pitch,
  and its roll share gives the turn angle, which is then rotated out
  (automatic in the level loop, or as a guided step after Set zero).
- ARCore depth accuracy on grass/gravel at 1–4 m.
- Tyre contact-point estimation: the contact patch is occluded; how well does
  mask bottom + local ground plane approximate it?
- Pitch for 2-axle vehicles needs front and rear axle heights in one
  consistent frame. Does tracking drift over the vehicle length stay below
  ~1 cm (≈ 0.16° over 3.5 m)?
- How large are suspension and load effects? I.e. how much does levelled
  ground differ from a levelled vehicle? The F5 evaluation answers this.
