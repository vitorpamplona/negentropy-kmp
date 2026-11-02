# negentropy-kmp

A Kotlin Multiplatform port of [Negentropy](https://github.com/hoytech/negentropy), Doug Hoyte's
range-based set reconciliation protocol (version 1, `0x61`): two peers find which items each holds
that the other lacks by exchanging fingerprints of ranges and splitting the ranges that differ.
Published to Maven Central as `com.vitorpamplona.negentropy:kmp-negentropy`, for JVM, Android,
JS, Wasm, iOS, macOS, Linux and Windows. It speaks the same bytes as the reference's C++, JS, Go
and Rust implementations, and the reference repository's cross-language test suite runs this
library.

## Layout

- `negentropy/` — the library, in `com.vitorpamplona.negentropy.*`: `Negentropy` (`initiate`,
  `reconcile`, the frame size limit) and `ReconciliationResult` at the root; `message` (the wire
  format: `VarInt`, `Mode` (Skip, Fingerprint, IdList), `MessageBuilder` with its delayed skips
  and `branch`/`merge`, `MessageConsumer`); `storage` (`IStorage`, `StorageVector`, the
  prefix-sum `PrefixSumStorageVector` that answers range fingerprints in O(1), `Bound`, `Id`);
  `fingerprint` (`Accumulator256`, `FingerprintCalculator`); `platform` (`expect fun sha256`).
- Source sets: `sha256` is JCA's `MessageDigest` in `jvmMain` and `androidMain`, CommonCrypto in
  `appleMain`, and a Kotlin port of Okio's SHA-256 in `jsMain`, `wasmJsMain`, `linuxX64Main` and
  `mingwX64Main`.
- `negentropy/src/jvmMain/.../Main.kt` — the stdin/stdout harness (`item`, `seal`, `initiate`,
  `msg` in; `msg`, `have`, `need`, `done` out, frame limit from `FRAMESIZELIMIT`) that the
  reference repository's `test/fuzz.pl` and `test/test.pl` run from the published JVM jar
  (`test/kotlin/Makefile` pins the version). It ships in the jar on purpose; changing what it
  reads or prints breaks the reference's cross-language tests.
- `negentropy/src/commonTest/resources/` — item sets and two-node transcripts recorded from the
  reference's harnesses; `testutils/InstructionParser` replays a transcript and checks every
  message this library produces against the recorded one. `Benchmark*Test` time load, seal and
  reconciliation and print the results; they run with the normal suite.

## Rules

- **Byte-identical to the reference.** Messages and fingerprints must match
  `hoytech/negentropy` exactly, so a peer can be any implementation. A change to what goes on the
  wire (bounds, skips, fingerprints, frame size cuts) is checked side by side against the
  reference: build its C++ harness (`test/cpp`, `make harness`) or use its JS one, point
  `test/Utils.pm`'s `kotlin` entry at a jar built from this tree (`./gradlew :negentropy:jvmJar`
  plus the Kotlin stdlib on the classpath), and run `test/fuzz.pl` and `test/test.pl` across
  pairs, comparing transcripts as well as results. A protocol bug found here is usually in the
  reference too: report it there and keep the fix the same as upstream's.
- **Recorded vectors come from the reference.** When an intended change alters a recorded
  message, re-record it with the reference harness where possible. If you patch one by hand,
  first replay the transcript with live messages to show that only the intended messages change
  and the final have/need sets stay the same, and say so in the commit.
- **No dependencies** beyond the Kotlin stdlib (and, for tests, `kotlin-test`). Users bring this
  into clients on every platform; it must not drag anything else onto their classpath.
- **Performance matters**: `reconcile`, `seal` and range fingerprints are the hot paths on large
  sets. Keep per-item allocation out of them (ids live in flat buffers, `seal` radix-sorts), and
  check the `Benchmark*Test` output before and after a change that touches them.
- `commonMain` and `commonTest` stay free of JVM-only APIs: the native and web targets compile
  them as common code. Tests read resources through `readTestResource`, which every platform
  implements; new resource files need nothing else, as the Apple copy tasks take the whole folder.
- Apple code compiles on Linux as metadata (`:negentropy:compileAppleMainKotlinMetadata`) but runs
  only on macOS: CI's macOS job runs `iosSimulatorArm64Test`. `mingwX64` tests run only on
  Windows.
- JVM target is 21, set in `negentropy/build.gradle.kts`.
- Formatting is Spotless with ktlint and the license header in `spotless/copyright.kt`; `preBuild`
  runs `spotlessApply`.
- After a Kotlin upgrade, run `./gradlew kotlinUpgradeYarnLock kotlinWasmUpgradeYarnLock` and
  commit `kotlin-js-store/`, or the JS and Wasm tests fail at `kotlinStoreYarnLock`.
- Before pushing: `./gradlew spotlessApply`, then
  `./gradlew spotlessCheck :negentropy:jvmTest :negentropy:testAndroidHostTest :negentropy:linuxX64Test :negentropy:jsNodeTest :negentropy:wasmJsNodeTest :negentropy:compileAppleMainKotlinMetadata`
  (the pre-push hook runs these; `./gradlew installGitHook` installs it). The Wasm tests need
  tooling Gradle fetches from codeload.github.com, which some sandboxes block (Claude Code on the
  web among them, unless the environment allows that host): the hook then skips them with a
  warning, and the PR should say they did not run.
- Releases follow semantic versioning: a breaking change to the public API bumps the major
  version.
- Commits: conventional commits (`feat:`, `fix:`, `test:`, `build:`, …). Never `--no-verify`.

## Remotes and pull requests

The repo is on GitHub, `github.com/vitorpamplona/negentropy-kmp`, which holds the canonical
`main`, and on nostr as a NIP-34 repository,
`nostr://npub1gcxzte5zlkncx26j68ez60fzkvtkm9e0vrwdcvsjakxf9mu9qewqlfnj5z/relay.ngit.dev/negentropy-kmp`:
a push to that remote goes to its GRASP git servers and to GitHub. A PR can go out either way: a
GitHub PR (`gh`), or a nostr proposal (`ngit`, a `pr/…` branch, reviewed on
[gitworkshop.dev](https://gitworkshop.dev/npub1gcxzte5zlkncx26j68ez60fzkvtkm9e0vrwdcvsjakxf9mu9qewqlfnj5z/relay.ngit.dev/negentropy-kmp)).
Remote names vary per clone, so identify them by URL. Open a PR only when the human you work for
asks, and before opening, revising or merging one by either path, use the `ngit-pr` skill.

## Releasing

Bump `VERSION_NAME` in `gradle.properties`, then push a `v<version>` tag. The
`create-release.yml` workflow runs every test on macOS, publishes to Maven Central and opens the
GitHub release. It needs the signing secrets `SONATYPE_USERNAME`, `SONATYPE_PASSWORD`,
`SIGNING_PRIVATE_KEY` and `SIGNING_PASSWORD`. After a release, the reference repository's
`test/kotlin/Makefile` pins the version its cross-language tests download.
