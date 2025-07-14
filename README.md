![Build status](https://github.com/navikt/ismanglendemedvirkning/workflows/main/badge.svg?branch=main)

# ismanglendemedvirkning

Applikasjon for å lagre vurderinger i henhold til §8-8 i folketrygdloven rundt manglende medvirkning til oppfølging fra NAV.

## Technologies used

* Docker
* Gradle
* Kafka
* Kotlin
* Ktor
* Postgres

##### Test Libraries:

* Mockk
* JUnit

#### Requirements

* JDK 21

### Build

Run `./gradlew clean shadowJar`

### Lint (Ktlint)

##### Command line

Run checking: `./gradlew --continue ktlintCheck`

Run formatting: `./gradlew ktlintFormat`

##### Git Hooks

Apply checking: `./gradlew addKtlintCheckGitPreCommitHook`

Apply formatting: `./gradlew addKtlintFormatGitPreCommitHook`

## Contact

### For NAV employees

We are available at the Slack channel `#isyfo`.