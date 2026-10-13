# Puppy Quest

A cozy 3D game by Emily and her dad: collect paw coins, adopt plush puppies (ESSAs, emotional support stuffed animals), dress them up and explore town.

- `game/` is the game itself: one `index.html` (three.js) plus nature art.
- `android/` wraps the game into an Android app that plays offline, full screen and sideways.
- `PUBLISHING.md` covers installing the APK and getting on Google Play.

## Get updates on your phone with Obtainium

1. Install [Obtainium](https://github.com/ImranR98/Obtainium) on the phone.
2. Tap **Add app**, paste this repository's URL, and tap **Add**.
3. Obtainium installs the newest release and tells you when a new one comes out.

Every release is signed with the same key, so updates install over the old version and saved games are kept.

## Making a release

Update `game/`, raise the number in `VERSION` (for example `1.0.1` to `1.0.2`), and push to `main`.

GitHub Actions builds `puppy-quest-1.0.1.apk`, signs it, and publishes it as a release. The unsigned Play Store bundle (`.aab`) is kept on the workflow run.

### One-time setup: the signing key secret

The build signs APKs with a key stored as a repository secret, never in the code:

- **Settings > Secrets and variables > Actions > New repository secret**
- Name: `SIDELOAD_KEYSTORE_BASE64`, value: the base64 text of the keystore.
- Optional: `SIDELOAD_KEYSTORE_PASSWORD` if the keystore's password isn't `android`.

This key is only for installing outside the Play Store. Google Play uses its own upload key (see `PUBLISHING.md`).

## Building by hand (Ubuntu/Debian)

```
sudo apt install openjdk-21-jdk-headless aapt apksigner zipalign dalvik-exchange android-sdk-platform-23 zip unzip
bash android/build.sh
```

## Credits

- Nature models: Quaternius "Ultimate Stylized Nature" (CC0)
- [three.js](https://threejs.org) (MIT), license in `android/vendor/three-LICENSE.txt`
- Fonts: Lilita One and Nunito (SIL Open Font License), license in `android/vendor/fonts/`
