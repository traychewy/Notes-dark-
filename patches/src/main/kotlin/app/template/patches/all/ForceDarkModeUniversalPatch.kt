package app.template.patches.all

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.template.patches.shared.hookApplicationOnCreate

private const val EXTENSION_CLASS = "Lapp/template/extension/darkmode/ForceDarkModePatch;"

/** Name of the style the extension applies to every activity to allow auto-darkening. */
private const val FORCE_DARK_STYLE = "morphe_force_dark"

@Suppress("unused")
val forceDarkModeUniversalPatch = patch(
    name = "Force dark mode (any app)",
    description = "Shows the app in dark mode, whatever the system theme is. " +
        "Uses the app's own dark theme, and can auto-darken apps that don't have one.",
    // Universal patches must be off by default; turn it on per app in Morphe.
    default = false,
) {
    // No compatibleWith(): this patch can be applied to any app.

    extendWith("extensions/extension.mpe")

    val autoDarken by booleanOption(
        key = "autoDarken",
        default = true,
        title = "Auto-darken apps without a dark theme",
        description = "Lets Android invert the colours of light-only screens (Android 10+). " +
            "Turn off if an app's colours look wrong.",
    )

    execute {
        if (autoDarken == true) {
            // A style the extension applies on top of each activity's theme. Written as its
            // own values file so no existing resources are touched.
            resourceFile("res/values/morphe_force_dark.xml", copy = false).writeText(
                """
                    <?xml version="1.0" encoding="utf-8"?>
                    <resources>
                        <style name="$FORCE_DARK_STYLE">
                            <item name="android:forceDarkAllowed">true</item>
                        </style>
                    </resources>
                """.trimIndent(),
            )
        }

        hookApplicationOnCreate(
            extensionMethod = "$EXTENSION_CLASS->apply(Landroid/content/Context;)V",
            fallbackApplication = "app.template.extension.darkmode.ForceDarkApplication",
        )
    }
}
