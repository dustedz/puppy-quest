# Puppy Quest on Android

## Play it on your phone now

1. Download **puppy-quest-debug.apk** (in `android/puppy-quest-android/out/`) onto the phone.
2. Tap it. Android asks to allow installs from that app (Chrome, Files or Drive): allow it, then tap **Install**.
3. If Play Protect warns that it doesn't know the developer, choose **More details > Install anyway**. That warning is normal for apps that aren't from the Play Store yet.

The game opens sideways, full screen, and works with no internet. The phone's **Back** button opens or closes the pause menu.

Saves in the app are separate from saves in the browser version, so the app starts fresh.

**Updates:** when the game changes, ask Claude to rebuild the APK, then install the new one over the old one. Saves are kept as long as you don't uninstall first. (The app is signed with a test key kept in `android/puppy-quest-android/keys/`. It is a standard throwaway debug key, not a secret, but it must stay there so updates install over the top.)

## Getting it on Google Play: what only you can do

1. **Create a Play Console developer account** at play.google.com/console. It costs a one-time $25 and Google verifies your identity. Choose a personal account unless you have a business.
2. **Closed test first (personal accounts).** New personal accounts must run a closed test with at least **12 testers opted in for 14 days in a row** before Google lets you publish to everyone. Family and friends with Android phones count. Plan for this; it is the longest step.
3. **Decide the app ID now.** It is `com.jackandemily.puppyquest`. It can never change after the first upload, so tell Claude now if you want something else.
4. **Make your upload key, on your own computer, and keep it private.** Claude did not create this key, so no secret is sitting in the shared project files. With Java installed (or Android Studio), run:
   ```
   keytool -genkeypair -v -keystore puppy-quest-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```
   Pick a strong password and back up the `.jks` file and password somewhere safe (a password manager plus a USB stick). Turn on **Play App Signing** when you create the app (it is the default): Google keeps the real signing key, and if you ever lose the upload key Google can reset it.
5. **Sign the bundle.** Copy `puppy-quest-release-unsigned.aab` to `puppy-quest-release.aab`, then run:
   ```
   jarsigner -keystore puppy-quest-upload.jks -sigalg SHA256withRSA -digestalg SHA-256 puppy-quest-release.aab upload
   ```
   Upload that `.aab` in Play Console. (Or on a Linux machine: `RELEASE_KEYSTORE=puppy-quest-upload.jks ./build.sh`.)
6. **Host the privacy policy.** Fill in the blanks in `privacy-policy.html` and put it at a public web address (Google Sites or GitHub Pages are free). Play requires a privacy policy link, especially for kids' apps.
7. **Answer the Play Console forms:**
   - **Target audience and content:** pick the age groups. If you include under-13s, the app falls under Google's **Families policy**. Puppy Quest fits it: no ads, no data collection, no third-party SDKs, no outside links.
   - **Data safety:** "No data collected" and "No data shared".
   - **Content rating (IARC questionnaire):** no violence, no chat, no purchases, no gambling. Expect Everyone / PEGI 3.
   - **Ads:** No. **In-app purchases:** No.
   - **Store listing:** text and graphics are drafted in `store-listing.md` and `art/`.
8. **Keep the game original.** Store names are parodies and the music is original, which is good. The nature art is CC0, three.js is MIT and the fonts are SIL OFL (licenses are in `vendor/`), so all of that is fine to publish.

## What is in this folder

- `puppy-quest-android/build.sh` packs `dog-game/index.html` and its art into an Android app. It swaps the online three.js and Google Fonts for local copies, so the app needs no internet. It does not change the game file.
- `src/` holds the small Android wrapper: one full-screen screen that runs the game, keeps it sideways and awake, and pauses the music when you leave the app.
- `out/` holds the builds: `puppy-quest-debug.apk` (for your phone) and `puppy-quest-release-unsigned.aab` (for Play once signed).
- Version: 1.0 (versionCode 1), works on Android 7 and newer, targets Android 16 (API 36) as Play currently requires. Each Play upload needs a higher versionCode; Claude bumps it when rebuilding.
