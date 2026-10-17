# Puppy Quest on Android

## Play it on your phone now

1. Download the newest `puppy-quest-X.Y.Z.apk` from GitHub Releases onto the phone (or add the repo in Obtainium).
2. Tap it. Android asks to allow installs from that app (Chrome, Files or Drive): allow it, then tap **Install**.
3. If Play Protect warns that it doesn't know the developer, choose **More details > Install anyway**. That warning is normal for apps that aren't from the Play Store yet.

The game opens sideways, full screen, and works with no internet. The phone's **Back** button opens or closes the pause menu.

Saves in the app are separate from saves in the browser version, so the app starts fresh.

**Updates:** when the game changes, ask Claude for a new release and Obtainium offers the update. Saves are kept as long as you don't uninstall first. (Every release is signed with the same sideload key, stored as the `SIDELOAD_KEYSTORE_BASE64` repo secret, so updates install over the top.)

## Getting it on Google Play

Already done by Claude:
- **Privacy policy** is online at https://dustedz.github.io/puppy-quest/privacy.html (once GitHub Pages is turned on, step 1).
- **Signed Play bundle**: every release build also makes `puppy-quest-X.Y.Z-play.aab`, signed with your upload key, on the GitHub Actions run page (once the key secrets exist, step 3).
- **Store listing text and graphics**: `android/store-listing.md` and `android/art/`.

What only you can do, in order:

1. **Turn on GitHub Pages**: repo **Settings > Pages**, Source "Deploy from a branch", Branch `main`, folder `/docs`, **Save**.
2. **Create a Play Console developer account** at play.google.com/console ($25 one time, ID check). Choose a personal account unless you have a business.
3. **Make your upload key** (Claude can make it in a folder on your laptop, or run the `keytool` command below yourself) and add three repo secrets in **Settings > Secrets and variables > Actions**:
   - `PLAY_UPLOAD_KEYSTORE_BASE64`: the keystore as base64 text
   - `PLAY_UPLOAD_KEYSTORE_PASSWORD`: its password
   - `PLAY_UPLOAD_KEY_ALIAS`: `upload`
   ```
   keytool -genkeypair -v -keystore puppy-quest-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
   ```
   Back up the `.jks` file and password somewhere safe (password manager plus a USB stick). It never goes in the repo or the project files.
4. **Create the app** in Play Console: name Puppy Quest, Game, Free. Leave **Play App Signing** on (Google keeps the real signing key; a lost upload key can be reset).
5. **Fill in App content**: privacy policy URL from above; Ads: no; Target audience: 6–8, 9–12 and 13+ (Families policy); Data safety: no data collected or shared; Content rating questionnaire (expect Everyone / PEGI 3); App access: no login needed.
6. **Store listing**: paste the text from `android/store-listing.md`, upload the icon, feature graphic and screenshots from `android/art/`, and add a contact email.
7. **Closed test**: Testing > Closed testing > create a track, upload the signed `-play.aab`, add at least **12 testers** (their Google account emails) and keep them opted in for **14 days in a row**. New personal accounts must do this before going public.
8. **Apply for production** once the 14 days are up, then roll out.

## What is in this folder

- `puppy-quest-android/build.sh` packs `dog-game/index.html` and its art into an Android app. It swaps the online three.js and Google Fonts for local copies, so the app needs no internet. It does not change the game file.
- `src/` holds the small Android wrapper: one full-screen screen that runs the game, keeps it sideways and awake, and pauses the music when you leave the app.
- `out/` holds the builds: `puppy-quest-debug.apk` (for your phone) and `puppy-quest-release-unsigned.aab` (for Play once signed).
- Version: 1.0 (versionCode 1), works on Android 7 and newer, targets Android 16 (API 36) as Play currently requires. Each Play upload needs a higher versionCode; Claude bumps it when rebuilding.
