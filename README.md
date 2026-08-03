# Android Advanced Hello World

Android assembler for the Advanced Hello World Kotlin Multiplatform repository family.
It produces the all-in-one APK and consumes the KMP domain and Compose UI repositories.

## Local build

Clone all five repositories as siblings, configure `local.properties` with `sdk.dir`,
then run:

```shell
./gradlew check assembleDebug
```

The initial feature slice demonstrates composition and offline-first UI state. Durable
SQLDelight persistence, background synchronization, and backend transport are deliberately
tracked as subsequent slices.
