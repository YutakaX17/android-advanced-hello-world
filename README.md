# Android Advanced Hello World

Android assembler for the Advanced Hello World Kotlin Multiplatform repository family.
It produces the all-in-one APK and consumes the KMP domain and Compose UI repositories.

## Local build

Clone all five repositories as siblings, configure `local.properties` with `sdk.dir`,
then run:

```shell
./gradlew -PuseLocalCompositeBuilds=true check assembleDebug
```

Local composite substitution is opt-in. Without `useLocalCompositeBuilds`, Gradle resolves
the pinned family version from GitHub Packages and requires `GITHUB_ACTOR` plus a
`GITHUB_TOKEN` with `read:packages`. CI uses this standalone production path.

## Module registry

`modules.json` is the distribution compatibility manifest and is constrained by
`modules.schema.json`. After changing modules, regenerate and verify the committed Kotlin
registry:

```shell
python3 scripts/generate_feature_registry.py
python3 scripts/generate_feature_registry.py --check
python3 -m unittest discover -s scripts/tests
```

The Gradle `check` lifecycle also runs the generated-registry currentness check.

The foundation composes the four immutable `0.1.0` family releases and demonstrates
offline-first UI state through a temporary in-memory adapter. Durable SQLDelight
persistence, background synchronization, and backend transport are deliberately tracked
as subsequent reviewable slices.
