# Negentropy Kotlin Multiplatform

Kotlin multiplatform implementation of Negentropy Range-Based-Set-Reconciliation protocol.

It's basically a binary search set-reconciliation algorithm.
You can read about the details [here](https://logperiodic.com/rbsr.html).
This code is basically a re-implementation of [Doug Hoyte's repository here](https://github.com/hoytech/negentropy)

## Storage

First, you need to create a storage instance. Currently only `Vector` is implemented.
Add all the items in your collection with `insert(timestamp, hash)` and call `seal()`

    StorageVector().apply {
        insert(1678011277, "eb6b05c2e3b008592ac666594d78ed83e7b9ab30f825b9b08878128f7500008c")
        insert(1678011278, "39b916432333e069a4386917609215cc688eb99f06fed01aadc29b1b4b92d6f0")
        insert(1678011279, "abc81d58ebe3b9a87100d47f58bf15e9b1cbf62d38623f11d0f0d17179f5f3ba")

        seal()
    }

*  `timestamp` should be a unix timestamp
*  `id` should be a byte array of the event id

## Reconciliation

Create a Negentropy object:

    val ne = Negentropy(storage, 50_000)

* The second parameter (`50_000` above) is the `frameSizeLimit`. This can be omitted (or `0`) to permit unlimited-sized frames.

On the client-side, create an initial message, and then transmit it to the server, receive the response, and `reconcile` until complete (signified by returning `null` for `newMsg`):

    val msg = ne.initiate();

    while (msg !== null) {
        val response = <queryServer>(msg);
        val (newMsg, have, need) = ne.reconcile(msg);
        msg = newMsg;
        // handle have/need (there may be duplicates from previous calls to reconcile())
    }

*  The output `msg`s and the IDs in the `have`/`need` arrays are hex strings.

The server-side is similar, except it doesn't create an initial message, there are no `have`/`need` arrays, and `newMsg` will never be `null`:

    while (1) {
        val msg = <receiveMsgFromClient>();
        val reconciled = ne.reconcile(msg);
        respondToClient(reconciled.msg);
    }

* The `initiate()` and `reconcile()` methods are not suspending functions but they will take a while to process.

## Developer Setup

Make sure to have the following pre-requisites installed:
1. Java 17+
2. Android Studio or IntelliJ Idea CE

## Building

Build the app:
```bash
./gradlew clean assemble
```

## Testing
```bash
./gradlew allTests
```

## Running Conformance Tests with other implementations

Clone [Doug Hoyte's repository here](https://github.com/hoytech/negentropy) and clone this repository inside of it.

```bash
git clone https://github.com/hoytech/negentropy
cd negentropy
git clone https://github.com/vitorpamplona/negentropy-kmp
cd negentropy-kmp
```

Create a `local.properties` file that points to your Android SDK. If you are using Android Studio,
the IDE performs this step for you.

For example:

```bash
echo "sdk.dir=/Users/<your user>/Library/Android/sdk" > local.properties
```

Run `./gradlew assemble` to generate the `.jar` for the library and

```bash
perl test.pl kotlin,js
```

to run the test with a kotlin node and a javascript node

## Publishing

Install GnuPG and generate a key:

```bash
gpg --gen-key
```

Run `gpg --list-keys` to show your GPG keys.

Distribute the public key:

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys <pubkey>
```

Export your private key to a file

```bash
gpg --export-secret-keys > ~/.gnupg/secring.gpg
```

Generate a User Token on MavenCentral

To publish from local, and add the following fields to your ~/.gradle/gradle.properties file:

```properties
mavenCentralUsername=<maven user>
mavenCentralPassword=<maven password>
signing.keyId=<gpg key id>
signing.password=<gpg key passphrase>
signing.secretKeyRingFile=<yourhome>/.gnupg/secring.gpg
```

Then run

```bash
./gradlew publishAllPublicationsToMavenCentral --no-configuration-cache
```

To publish from GitHub Actions, export your private key as a base64 string:

```bash
gpg --export-secret-keys --armor <key-id> ~/.gnupg/secring.gpg | grep -v '\-\-' | grep -v '^=.' | tr -d '\n'
```

and add the following secrets to your GitHub secrets:

```properties
SONATYPE_USERNAME=<maven user>
SONATYPE_PASSWORD=<maven password>
SIGNING_PRIVATE_KEY=<base64versionOfTheFile>
SIGNING_PASSWORD=<gpg key passphrase>
```

And just tag the release version starting with `v`

## Contributing

Issues can be logged on [GitHub issues](https://github.com/vitorpamplona/negentropy-kmp/issues). [Pull requests](https://github.com/vitorpamplona/negentropy-kmp/pulls) are very welcome.

By contributing to this repository, you agree to license your work under the MIT license. Any work contributed where you are not the original author must contain its license header with the original author(s) and source.

# Contributors

<a align="center" href="https://github.com/vitorpamplona/negentropy-kmp/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=vitorpamplona/negentropy-kmp" />
</a>

# MIT License

<pre>
Copyright (c) 2024 Vitor Pamplona

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
</pre>
