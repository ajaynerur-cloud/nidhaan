# Nidhaan (निधान) - know where everything is kept

Nidhaan is a Sanskrit word for a storehouse / repository. The app keeps a three level inventory:

    Location (Home, Garage, Office)
      -> Container (Box, Rack, Wardrobe, Drawer, Shelf, Cabinet, or your own type)
           -> Item (single item or group with quantity, each with its own photos)

* Photos of locations, containers and items are taken with the in-app camera and stored in the
  app's private folder only. They never appear in the phone gallery.
* Container screen shows items as a table: Photo | Item | Qty | Notes.
* Search finds items, containers and locations in one box.
* Optional GPS tag on a location (asked only when you tap "Tag current location").
* Fully offline. Permissions: Camera, optional Location. No storage / internet permission.

## Backup and restore (survives uninstall)
Backup tab -> "Quick backup to Downloads" (Android 10+) or "Choose where to save..." writes ONE json file
containing the whole hierarchy and every photo (Base64). Copy it somewhere safe (Drive, PC).
After reinstalling, the first screen offers "Restore from backup file" - pick the json.
You can also restore later from the Backup tab (this replaces current data).

## Build the APK with GitHub
1. Create a new GitHub repository and push this folder to the `main` branch.
2. Open the repo -> Actions -> "Build Nidhaan APK" (runs automatically on every push).
3. When it finishes, open the run and download the artifact **Nidhaan-debug-apk**. Unzip, copy
   `app-debug.apk` to your phone and install it (allow "install unknown apps" once).

## Build locally (optional)
JDK 17 + Android SDK 34 + Gradle 8.9:
    gradle assembleDebug
APK: app/build/outputs/apk/debug/app-debug.apk

## Tech
Kotlin, Jetpack Compose (Material 3), Room, CameraX, Coil, Gson streaming for backup. minSdk 26.
