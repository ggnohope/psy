# Launcher icon — source artwork

Editable **source** images for the app's launcher icon. These are NOT used by the build.

- `ic_launcher_source.png` — foreground/full artwork
- `ic_launcher_background.png` — background layer

The icons the app actually ships are the generated adaptive resources in
`android/app/src/main/res/mipmap-*` (foreground/background webp + `mipmap-anydpi-v26/ic_launcher.xml`),
produced from these sources via Android Studio → **New → Image Asset**.
Re-run Image Asset if you change the artwork here.
