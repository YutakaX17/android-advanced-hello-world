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

The `0.2.0` application composes KMP Core and Compose Core `0.1.0` with KMP Messages
and Compose Messages `0.2.0`. Its process-scoped Android SQLDelight driver and Ktor
client share one durable synchronization engine. Manual refresh and unique WorkManager
jobs therefore reuse persisted outbox operations and idempotency keys. Background work
requires a connected network and uses exponential retry backoff.

Debug builds use `http://10.0.2.2:8000` as `BuildConfig.API_BASE_URL`, which reaches a
backend running on the Android emulator host, and only the debug manifest permits cleartext
traffic. Release builds require an independently supplied HTTPS endpoint and fail before
compilation when it is missing, malformed, or non-HTTPS:

```shell
./gradlew -PreleaseApiBaseUrl=https://api.example.com lintRelease assembleRelease
```

The endpoint is public build configuration, not a credential. Do not include credentials,
tokens, query parameters, or fragments in it.
