// Renders the app icon PNGs in ../src/res from the SVGs in this folder.
//   node make-icons.js   (needs Playwright with Chromium: npm i -g playwright)
// icon-foreground.svg + icon-background.svg -> adaptive icon layers (Android 8+, and the Android 12+ splash screen)
// icon.svg -> ic_launcher.png for Android 7 and the splash_logo shown while the app starts on Android 7-11
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const ART = __dirname;
const RES = path.join(ART, '..', 'src', 'res');
const DENSITIES = { mdpi: 1, hdpi: 1.5, xhdpi: 2, xxhdpi: 3, xxxhdpi: 4 };

function svg(name) {
  return fs.readFileSync(path.join(ART, name), 'utf8').replace(/<metadata>[\s\S]*?<\/metadata>/, '');
}

(async () => {
  const browser = await chromium.launch(process.env.CHROMIUM ? { executablePath: process.env.CHROMIUM } : {});
  const page = await browser.newPage();
  async function render(source, size, out, round) {
    const radius = round ? 'border-radius:18%;overflow:hidden;' : '';
    await page.setViewportSize({ width: size, height: size });
    await page.setContent(`<html><body style="margin:0;background:transparent">
      <div style="width:${size}px;height:${size}px;${radius}">${source.replace('<svg ', `<svg style="display:block;width:${size}px;height:${size}px" `)}</div>
      </body></html>`);
    fs.mkdirSync(path.dirname(out), { recursive: true });
    await page.screenshot({ path: out, omitBackground: true });
  }
  const full = svg('icon.svg'), fg = svg('icon-foreground.svg'), bg = svg('icon-background.svg');
  for (const [d, k] of Object.entries(DENSITIES)) {
    await render(full, 48 * k, path.join(RES, `mipmap-${d}`, 'ic_launcher.png'), true);
    await render(fg, 108 * k, path.join(RES, `mipmap-${d}`, 'ic_launcher_foreground.png'), false);
    await render(bg, 108 * k, path.join(RES, `mipmap-${d}`, 'ic_launcher_background.png'), false);
  }
  await render(full, 384, path.join(RES, 'drawable-xxxhdpi', 'splash_logo.png'), true);
  await browser.close();
})();
