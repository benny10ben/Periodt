<p align="center">
  <img width="200" height="200" alt="logo 1" src="fastlane/metadata/android/en-US/images/icon.png" />
</p>
<h1 align="center">Periodt: Track & Predict Cycles</h1>
<p align="center">
  Periodt is a privacy‑first Android app that keeps all cycle data on the device and uses on‑device logic to predict upcoming periods, fertile windows, and ovulation estimates — with full pill tracking and post-pill recovery awareness.
</p>
<br />

<p align="center">
  <img src="assets/1.png" width="80%" />
</p>

<br />

<p align="center">
  <a href="https://obtainium.imranr.dev/?url=https://github.com/benny10ben/Periodt">
    <img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png" height="40" alt="Get it on Obtainium" />
  </a>
</p>

<br />

> ⚠️ **Medical disclaimer:** Predictions are for informational purposes only and are not medical advice. Please consult a healthcare professional for accurate results.

## Highlights
- **Private by design** — data stays on the device; no analytics, no ads, no cloud sync.
- **Encrypted at rest** — Room database secured with SQLCipher and an Android Keystore–protected key.
- **Modern Android** — Jetpack Compose UI, Room, Coroutines, DataStore, and home‑screen widgets.
- **Pill tracker** — log contraceptive packs, track daily pills, and get withdrawal bleed predictions timed to pack end.
- **Discovery & Learning mode** — predictions pause after stopping the pill and gradually re-enable as natural cycles re-establish.
- **Smart algorithm** — trend-aware regression, outlier filtering, regularity scoring, and personalised luteal phase data.
- **Reminders** — optional notifications for period, fertile window, and daily pill, each with custom timing.
- **Offline** — no internet permission required.

## How it works

### Prediction algorithm

Periodt uses a multi-stage on-device prediction engine:

1. **Outlier filtering** — unusually long gaps (likely missed logs) are detected using the user's own median cycle length and excluded before any statistics are run.
2. **Trend-aware cycle length** — a weighted linear regression across the last 6 cycles detects whether cycles are shifting shorter or longer, blended with a recency-weighted average.
3. **Regularity scoring** — standard deviation of cycle lengths classifies the pattern as Very Regular, Regular, Somewhat Irregular, or Irregular, which directly widens or narrows the prediction window.
4. **Personalised ovulation** — ovulation is estimated from actual logged luteal phase data where available, not a fixed 14-day assumption. The fertile window expands or contracts based on confidence.
5. **Post-pill awareness** — after stopping the pill, predictions pause during Discovery (0–1 post-pill cycles) and are flagged as Learning (2–3 cycles) before returning to full Normal confidence.

## Roadmap
- [ ] **Advanced Cycle Insights** – Statistical trends and symptom correlation analysis.
- [ ] **Automated Backups** – Support for periodic local backups to custom directories.
- [ ] **Lock Screen Support** – Privacy-focused widgets specifically for the Android Lock Screen.
- [ ] **Localization** – Expanding support for additional languages.

## Contributing
Issues and pull requests are welcome. Please open an issue to discuss significant changes before submitting a PR.

## License
GNU General Public License (GPL). See [LICENSE](./LICENSE) for details.

## Developer
Built by [Benny](https://github.com/benny10ben) — [@benbytee](https://x.com/benbytee)

<br />

<p align="center">
  <a href="https://x.com/benbytee">
    <img src="https://cdn.simpleicons.org/x/white" width="28" alt="X" />
  </a>
  &nbsp;&nbsp;
  <a href="https://www.instagram.com/ben.bytee">
    <img src="https://cdn.simpleicons.org/instagram/white" width="28" alt="Instagram" />
  </a>
  &nbsp;&nbsp;
  <a href="mailto:developer.ben10@gmail.com">
    <img src="https://cdn.simpleicons.org/gmail/white" width="28" alt="Email" />
  </a>
</p>