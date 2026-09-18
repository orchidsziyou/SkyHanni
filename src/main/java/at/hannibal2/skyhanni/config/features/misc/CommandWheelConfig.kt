package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.glfw.GLFW

class CommandWheelConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Hold the wheel keybind to open a command wheel in the center of the screen.\n" +
            "Move the mouse into a direction to select an entry, then run it by left click or\n" +
            "by releasing the key (see Execute On Release). Right click closes without running anything.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Wheel Keybind",
        desc = "Press and hold this key to open the command wheel.\n" +
            "Left click runs the selected command.",
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var keybind: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(
        name = "Execute On Release",
        desc = "When enabled, the selected entry runs as soon as you release the wheel keybind.\n" +
            "When disabled, you must left click to run the selected entry.\n" +
            "Right click always closes without running anything.",
    )
    @ConfigEditorBoolean
    var executeOnRelease: Boolean = false

    @Expose
    @ConfigOption(
        name = "Execution Log",
        desc = "Show a chat message when an entry is run or sent by the wheel.",
    )
    @ConfigEditorBoolean
    var showLogMessage: Boolean = false

    @Expose
    @ConfigOption(
        name = "Dead Zone",
        desc = "Minimum mouse movement (in pixels) required to select a direction.\n" +
            "Lower values are more sensitive.",
    )
    @ConfigEditorSlider(minValue = 10f, maxValue = 150f, minStep = 5f)
    var deadZone: Int = 40

    @Expose
    @ConfigOption(
        name = "Wheel Size",
        desc = "Size of the command wheel, in percent of the default size.",
    )
    @ConfigEditorSlider(minValue = 50f, maxValue = 200f, minStep = 5f)
    var sizePercent: Int = 100

    @Expose
    @ConfigOption(
        name = "Auto Close Timeout",
        desc = "Automatically close the wheel (without running a command) after holding the key for this many seconds.",
    )
    @ConfigEditorSlider(minValue = 3f, maxValue = 30f, minStep = 1f)
    var timeoutSeconds: Int = 10

    @Expose
    @ConfigOption(name = "Up", desc = "Text for the up direction.\nStart with '/' to run it as a command (e.g. /ah), otherwise it is sent as a chat message (e.g. warp me). Leave empty to hide this direction.")
    @ConfigEditorText
    var slotUp: String = "/ah"

    @Expose
    @ConfigOption(name = "Up Right", desc = "Text for the up right direction.\nStart with '/' to run it as a command, otherwise it is sent as a chat message. Leave empty to hide this direction.")
    @ConfigEditorText
    var slotUpRight: String = ""

    @Expose
    @ConfigOption(name = "Right", desc = "Text for the right direction.\nStart with '/' to run it as a command, otherwise it is sent as a chat message. Leave empty to hide this direction.")
    @ConfigEditorText
    var slotRight: String = ""

    @Expose
    @ConfigOption(name = "Down Right", desc = "Text for the down right direction.\nStart with '/' to run it as a command, otherwise it is sent as a chat message. Leave empty to hide this direction.")
    @ConfigEditorText
    var slotDownRight: String = ""

    @Expose
    @ConfigOption(name = "Down", desc = "Text for the down direction.\nStart with '/' to run it as a command (e.g. /bz), otherwise it is sent as a chat message (e.g. warp me). Leave empty to hide this direction.")
    @ConfigEditorText
    var slotDown: String = "/bz"

    @Expose
    @ConfigOption(name = "Down Left", desc = "Text for the down left direction.\nStart with '/' to run it as a command, otherwise it is sent as a chat message. Leave empty to hide this direction.")
    @ConfigEditorText
    var slotDownLeft: String = ""

    @Expose
    @ConfigOption(name = "Left", desc = "Text for the left direction.\nStart with '/' to run it as a command, otherwise it is sent as a chat message. Leave empty to hide this direction.")
    @ConfigEditorText
    var slotLeft: String = ""

    @Expose
    @ConfigOption(name = "Up Left", desc = "Text for the up left direction.\nStart with '/' to run it as a command, otherwise it is sent as a chat message. Leave empty to hide this direction.")
    @ConfigEditorText
    var slotUpLeft: String = ""
}
