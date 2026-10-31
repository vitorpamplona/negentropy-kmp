# negentropy-kmp

Kotlin Multiplatform implementation of the Negentropy range-based set reconciliation protocol
(NIP-77). Published to Maven Central; see `README.md`.

## Build and test

```bash
./gradlew installGitHook                       # pre-push: spotlessCheck jvmTest testAndroidHostTest
./gradlew spotlessApply
./gradlew jvmTest testAndroidHostTest
```

## Remotes and pull requests

The repo is on GitHub, `github.com/vitorpamplona/negentropy-kmp`, which holds the canonical `main`,
and on nostr as a NIP-34 repository,
`nostr://npub1gcxzte5zlkncx26j68ez60fzkvtkm9e0vrwdcvsjakxf9mu9qewqlfnj5z/relay.ngit.dev/negentropy-kmp`:
a push to that remote goes to its GRASP git servers and to GitHub. A PR can go out either way: a
GitHub PR (`gh`), or a nostr proposal (`ngit`, a `pr/…` branch, reviewed on
[gitworkshop.dev](https://gitworkshop.dev/npub1gcxzte5zlkncx26j68ez60fzkvtkm9e0vrwdcvsjakxf9mu9qewqlfnj5z/relay.ngit.dev/negentropy-kmp)).
Remote names vary per clone, so identify them by URL. Open a PR only when the human you work for
asks, and before opening, revising or merging one by either path, use the `ngit-pr` skill.
