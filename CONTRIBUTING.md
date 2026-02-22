# Contributing to Periodt

First off, thank you for considering contributing to **Periodt**! It’s people like you that make open-source such a great tool for the privacy community.

## Local Development Setup

To get started with development on your local machine:

* **Fork and Clone:** Fork the repository and clone it to your local machine.
* **Android Studio:** Open the project in the latest version of Android Studio.
* **JDK Version:** Ensure you are using **JDK 17**, as defined in our CI pipeline.
* **SDK:** You will need the Android SDK (API 34+) installed.

## Testing Requirements

We take algorithm accuracy seriously. Before submitting any changes, you must ensure all tests pass.

* **Run Unit Tests:** Execute `./gradlew testDebugUnitTest` in your terminal.
* **Prediction Logic:** If you are modifying the cycle prediction math, you **must** update the corresponding tests in `PredictionTests.kt`.



## Privacy & Security Guidelines

Because Periodt is a privacy-first app, we have strict rules for contributions:

* **No Analytics:** Do not add any third-party tracking, crash reporting (like Firebase), or analytics.
* **No Internet Permission:** The app must remain functional without the `INTERNET` permission.
* **Encryption:** Any changes to the Room database must maintain SQLCipher encryption.

## Pull Request Process

We use a strict branch protection policy to keep the `main` branch stable.

1. **Branching:** Create a new branch for your feature or fix (e.g., `git checkout -b feature/amazing-new-logic`).
2. **Commit Messages:** Use descriptive commit messages (e.g., `feat: add support for irregular cycle notifications`).
3. **The Quality Check:** Once you push your branch and open a PR, our GitHub Actions will automatically run the suite of tests.
4. **Review:** Your PR must pass all status checks before it can be merged.



## Code of Conduct

Please be respectful and kind to others in issues and pull requests. We are here to build a safe tool for everyone!
