package app.template.patches.samsungnotes

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.mutableClassDefByOrNull
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_SAMSUNG_NOTES
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import org.w3c.dom.Element

private const val EXTENSION_CLASS = "Lapp/template/extension/samsungnotes/ForceDarkModePatch;"
private const val EXTENSION_CALL =
    "invoke-static {p0}, $EXTENSION_CLASS->apply(Landroid/content/Context;)V"

@Suppress("unused")
val forceDarkModePatch = patch(
    name = "Force dark mode",
    description = "Always shows Samsung Notes in dark mode, whatever the system theme is.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SAMSUNG_NOTES)

    extendWith("extensions/extension.mpe")

    execute {
        // 1. Find the app's Application class from the manifest.
        val applicationType = document("AndroidManifest.xml").use { manifest ->
            val packageName = manifest.documentElement.getAttribute("package")
            val application = manifest.getElementsByTagName("application").item(0) as? Element
                ?: throw PatchException("AndroidManifest.xml has no <application> element.")

            val name = application.getAttribute("android:name").ifEmpty {
                throw PatchException(
                    "The manifest declares no Application class, so there is nowhere to hook.",
                )
            }

            // Names may be relative to the package (".NotesApp") or fully qualified.
            val fullName = if (name.startsWith(".")) packageName + name else name
            "L" + fullName.replace('.', '/') + ";"
        }

        val applicationClass = mutableClassDefByOrNull(applicationType)
            ?: throw PatchException("Application class $applicationType was not found in the dex files.")

        // 2. Call the extension at the very start of Application.onCreate().
        val onCreate = applicationClass.methods.firstOrNull { method ->
            method.name == "onCreate" &&
                method.returnType == "V" &&
                method.parameterTypes.isEmpty() &&
                !AccessFlags.STATIC.isSet(method.accessFlags) &&
                method.implementation != null
        }

        if (onCreate != null) {
            // p0 is "this" (the Application), so no free register is needed.
            onCreate.addInstruction(0, EXTENSION_CALL)
        } else {
            // The class does not override onCreate(), so add one that calls super first.
            val superclass = applicationClass.superclass
                ?: throw PatchException("$applicationType has no superclass.")

            ImmutableMethod(
                applicationClass.type,
                "onCreate",
                emptyList(),
                "V",
                AccessFlags.PUBLIC.value,
                null,
                null,
                MutableMethodImplementation(1),
            ).toMutable().apply {
                addInstructions(
                    0,
                    """
                        invoke-super {p0}, $superclass->onCreate()V
                        $EXTENSION_CALL
                        return-void
                    """,
                )
            }.also(applicationClass.methods::add)
        }
    }
}
