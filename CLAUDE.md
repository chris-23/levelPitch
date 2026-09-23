# LevelPitch

Android app for levelling a parked camper with drive-on wedges.
Product spec, milestones and open questions:

@SPEC.md

## Working rules
- Current milestone: M1. Don't start the next milestone without asking.
- Small, focused commits; `./gradlew testDebugUnitTest` must pass before each commit.
- The `:leveling` module is pure Kotlin/JVM with no Android dependencies, and every function there has JVM unit tests.
- Sign conventions: vehicle frame x forward, y left, z up; pitch positive = nose up,
  roll positive = left side up. Tests are the source of truth for these.
- When a design decision changes, update SPEC.md in the same commit.

## Reference code
`reference/` contains ar/ files from ObjectViz. Adapt, don't copy blindly;
see the "Differences to revisit" note in SPEC.md.