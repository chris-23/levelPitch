# M2 bench validation (Pixel 10 Pro)

Goal: show that simple mode reaches **≤ 0.2°** after zero calibration, and
answer the open question in SPEC.md: is zero calibration enough, or is a flip
calibration needed?

## Setup

- A stiff board, with two support points a known distance **L** apart
  (573 mm is convenient: 10 mm of shim ≈ 1.00°).
- Shims of known thickness (measure them with calipers), a spirit level or
  digital inclinometer, and a table that nobody leans on.
- Level the board with shims until the spirit level reads level both ways.
- Tape a corner stop on the board so the phone always goes back to the same
  spot, with its edge along the board edge. "Front" means the end you will raise.
- Expected angle for shim height h: **asin(h / L)**, e.g. 10 mm / 573 mm = 1.000°,
  20 mm = 2.000°, 30 mm = 3.001°, 50 mm = 5.006°.

For each measurement, write down pitch, roll, noise, drift and the sample count
from the result card.

## 1. Noise baseline (board level, no zero)

Clear the zero and choose orientation Front. Tap Measure 5 times without moving anything else.

- Expect a spread (max − min) of ≤ 0.05° and no "phone moved" rejections.
- The noise and drift figures show how much margin the stillness limits
  (0.5° noise, 0.1° drift) leave.

## 2. Flip test: sensor bias (board level, no zero)

1. Orientation Front: measure.
2. Turn the phone 180° in place, choose Rear, and measure.
3. Repeat for Left and Right.

On a level board every orientation should read the same vehicle tilt. Half
the Front/Rear difference is the sensor bias along that axis. If the bias is
more than ~0.1°, a zero only holds for the orientation it was set in (the app
stores one zero per orientation for this reason), and a flip calibration is
worth adding.

## 3. Zero, then known angles

1. Board level, orientation Front: tap **Set zero**, then Measure → expect ±0.00–0.02°.
2. Raise the front support by 10, 20, 30 and 50 mm. Measure each (pitch should
   rise, "nose up").
3. Put the board back to level and zero, then raise the *left* side
   instead (turn the board or shim the side). Roll should rise with "left side up".
4. One combined case: front +20 mm and left +10 mm.

Pass: |app − expected| ≤ 0.2° at every step. A spirit level that disagrees
with the shim calculation points to board flex or shim error.

## 4. Orientations

Set a zero for each orientation (Front, Left, Rear, Right) on the level board.
Then, at front +20 mm, measure in each orientation. All four should read
≈ +2.00° pitch, 0.00° roll. A sign flip here means a mapping bug.

## 5. Placement repeatability

At front +20 mm, pick the phone up and put it back against the stop 5 times,
measuring each time. Aim for a spread ≤ 0.1°. Also try it deliberately turned
~5° away from the edge: roll should pick up ≈ 9 % of the pitch (≈ 0.17° at
2°). That is the yaw error that SPEC.md says a zero cannot fix.

## 6. Stillness check

Each of these should give "phone moved": tap the phone during the window, lean on
the table, or pick the phone up. None of the quiet runs in steps 1–5 should be
rejected. If they are, raise the limits in `StillnessLimits`.

## Results (2026-09-23)

Rough setup, so treat these as ballpark numbers. Angles are in degrees.

### Steps 1–2: noise and flip test (level board)

| Orientation | Pitch | Roll | Noise | Drift | n |
|---|---|---|---|---|---|
| Front | 0.00 | 0.00 | 0.04 | 0.01 | 201 |
| Front | 0.00 | −0.01 | 0.04 | 0.00 | 201 |
| Rear | −0.67 | −0.64 | 0.04 | 0.01 | 201 |
| Left | −0.30 | +0.35 | 0.04 | 0.00 | 201 |

The Front rows read 0.00, so a Front zero was probably already set. Rear and
Left are uncorrected. An earlier session read Rear −0.55 / −0.16 and Left
−0.10 / +0.35 on the same board.

### Step 3: known angles (Front, zero set)

| Shim | Expected | Pitch | Error | Roll | Noise | Drift | n |
|---|---|---|---|---|---|---|---|
| +10 mm | 1.000 | 1.10 | +0.10 | 0.10 | 0.05 | 0.00 | 201 |
| +20 mm | 2.000 | 2.16 | +0.16 | 0.14 | 0.04 | 0.01 | 201 |
| +30 mm | 3.001 | 2.83 | −0.17 | 0.18 | 0.06 | 0.00 | 201 |
| +50 mm | 5.006 | 5.17 | +0.16 | 0.15 | 0.09 | 0.01 | 201 |

### Step 4: all orientations at ~10° pitch (zero set per orientation)

| Orientation | Pitch | Roll | Roll / pitch | Phone turn that would explain it |
|---|---|---|---|---|
| Front | +10.21 | +1.31 | 13 % | 7.3° |
| Left | +10.08 | +1.99 | 20 % | 11.2° |
| Right | +10.31 | −0.08 | −1 % | −0.4° |
| Rear | +9.95 | +0.87 | 9 % | 5.0° |

### Step 5: placement repeatability

Picking the phone up and putting it back against the stop: spread ≈ 0.01°,
well under the 0.1° aim.

### Step 6: stillness

Tapping the screen or moving the phone during the window gives "phone
moved". No quiet run in steps 1–5 was rejected.

## Conclusions

- **Pitch accuracy passes.** Up to 5° the error is within ±0.17°, with no
  trend over angle, so there is no visible scale error. At ~10° all four
  orientations read 9.95–10.31° with the right sign. That confirms the
  orientation mappings on the device.
- **Noise and stillness pass.** Noise is 0.04–0.09° per sample and drift
  ≤ 0.01°. The limits (0.5° / 0.1°) need no tuning, and moving the phone or
  tapping the screen is rejected.
- **One zero per orientation is required, and no flip calibration is
  needed.** Offsets that turn with the phone are 0.3–0.7°.
- **Placement repeatability is fine with a stop** (≈ 0.01°). The session-to-session
  differences in step 2 more likely came from the setup changing between
  sessions than from the phone.
- **Roll crosstalk is the remaining error.** Pure pitch also shows up as roll:
  3–9 % below 5° and up to 20 % at 10°. It depends on the orientation, which fits
  the phone lying turned (yawed) by up to ~11° against the board axis better
  than a rolled board would (Right shows almost none). A zero cannot remove it. At a
  typical 2–3° vehicle tilt, 10–20 % crosstalk is 0.2–0.6° of false roll, which
  can exceed the 0.25° target. Mitigation options are in SPEC.md.
