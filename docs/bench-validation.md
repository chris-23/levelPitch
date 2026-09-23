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

## Results

| Step | Orientation | Shim / setup | Expected | Pitch | Roll | Noise | Drift | n | Note |
|---|---|---|---|---|---|---|---|---|---|
| 1 | Front | level | 0 / 0 | 0.0| 0.0| 0.04| 0.01| 201| |
| 2 | Front | level | 0 / 0 |0.0 | -0.01| 0.04|0.00 |201 | |
| 2 | Rear | level | 0 / 0 |-0.67  |-0.64 |0.04 |0.01 |201 | |
| 2 | Left | level | 0 / 0 |-0.3  |0.35 |0.04 |0.00 |201 | |
| 3 | Front | +10 mm front | 1.000 / 0 | 1.1 |0.1 |0.05 |0.00 |201 | |
| 3 | Front | +20 mm front | 2.000 / 0 | 2.16| 0.14| 0.04| 0.01| 201| |
| 3 | Front | +30 mm front | 3.001 / 0 | 2.83|0.18 |0.06 |0.00 |201 | |
| 3 | Front | +50 mm front | 5.006 / 0 | 5.17| 0.15| 0.09| 0.01|201 | |

Conclusions (2026-09-23, rough setup, Front orientation checked with tilt):
accuracy within ±0.2° up to 5°, orientation-dependent offsets of 0.3–0.7°
(so one zero per orientation, no flip calibration), limits need no tuning.
Steps 4–6 not run yet. Details are in SPEC.md's open questions.
