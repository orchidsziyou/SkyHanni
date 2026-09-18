package at.hannibal2.skyhanni.features.misc.commandwheel

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.features.garden.MouseSensitivityReducer
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import at.hannibal2.skyhanni.utils.render.ShaderRenderUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.seconds

/**
 * Opens a radial command wheel in the center of the screen while holding the configured keybind.
 * Moving the mouse into a direction selects the command of that direction.
 * Depending on the "Execute On Release" option, the selected entry runs on left click or on
 * releasing the keybind. Right clicking closes the wheel without running anything.
 * The camera is locked while the wheel is open.
 */
@SkyHanniModule
object CommandWheel {

    private val config get() = SkyHanniMod.feature.misc.commandWheel

    private const val SECTOR_COUNT = 8
    // base radii, scaled by the "Wheel Size" config option
    private const val BASE_WHEEL_RADIUS = 55
    private const val BASE_LABEL_RADIUS = 38
    private const val SECTOR_ANGLE = 360.0 / SECTOR_COUNT

    private val backgroundColor = Color(0, 0, 0, 140)

    private val sectorNames = listOf(
        "Up", "Up Right", "Right", "Down Right",
        "Down", "Down Left", "Left", "Up Left",
    )

    private val sectorCommands: List<String?>
        get() = listOf(
            config.slotUp,
            config.slotUpRight,
            config.slotRight,
            config.slotDownRight,
            config.slotDown,
            config.slotDownLeft,
            config.slotLeft,
            config.slotUpLeft,
        ).map { it.trim().takeIf { command -> command.isNotEmpty() } }

    private val isEnabled
        get() = config.enabled &&
            config.keybind != GLFW.GLFW_KEY_UNKNOWN && sectorCommands.any { it != null }

    private var isOpen = false
    private var openTime = SimpleTimeMark.farPast()
    private var movedX = 0.0
    private var movedY = 0.0
    private var selectedSector: Int? = null
    private var suppressedClickButton: Int? = null

    @HandleEvent
    fun onKeyDown(event: KeyDownEvent) {
        if (isOpen) {
            if (MinecraftCompat.screen != null) return
            when (event.keyCode) {
                KeyboardManager.LEFT_MOUSE -> if (selectedSector != null) {
                    // suppress the vanilla attack triggered by the same click
                    suppressedClickButton = KeyboardManager.LEFT_MOUSE
                    close(execute = true)
                }

                KeyboardManager.RIGHT_MOUSE -> {
                    // suppress the vanilla item use triggered by the same click
                    suppressedClickButton = KeyboardManager.RIGHT_MOUSE
                    close(execute = false)
                }
            }
            return
        }
        if (!isEnabled) return
        if (event.keyCode != config.keybind) return
        if (MinecraftCompat.screen != null) return
        open()
    }

    @HandleEvent
    fun onItemClick(event: ItemClickEvent) {
        if (isOpen || suppressedClickButton != null) event.cancel()
    }

    private fun open() {
        isOpen = true
        openTime = SimpleTimeMark.now()
        movedX = 0.0
        movedY = 0.0
        selectedSector = null
        MouseSensitivityReducer.setTemporaryLock(true)
    }

    @HandleEvent
    private fun onTick() {
        suppressedClickButton?.let { button ->
            if (!MouseCompat.isButtonDown(button)) suppressedClickButton = null
        }
        if (!isOpen) return
        if (!config.keybind.isKeyHeld()) {
            // release runs the selected entry when "Execute On Release" is enabled
            val execute = config.executeOnRelease && selectedSector != null
            close(execute = execute)
            return
        }
        if (MinecraftCompat.screen != null || openTime.passedSince() > config.timeoutSeconds.seconds) {
            close(execute = false)
            return
        }
        // The mouse deltas are sampled once per tick: fast flicks are captured well,
        // very slow drags may need a lower dead zone to register.
        movedX += MouseCompat.deltaMouseX
        movedY += MouseCompat.deltaMouseY
        selectedSector = computeSelectedSector()
    }

    private fun computeSelectedSector(): Int? {
        val distance = sqrt(movedX * movedX + movedY * movedY)
        if (distance < config.deadZone) return null
        // Screen coordinates have y pointing down, so angle 0 is right and -90 is up.
        val angle = Math.toDegrees(atan2(movedY, movedX))
        val normalized = (angle + 90.0 + SECTOR_ANGLE / 2).mod(360.0)
        return (normalized / SECTOR_ANGLE).toInt().mod(SECTOR_COUNT)
    }

    private fun close(execute: Boolean) {
        isOpen = false
        MouseSensitivityReducer.setTemporaryLock(false)
        val sector = selectedSector
        selectedSector = null
        if (!execute) return
        val command = sector?.let { sectorCommands.getOrNull(it) } ?: return
        // entries starting with '/' are executed as commands, anything else is sent as a chat message
        ChatUtils.sendMessageToServer(command)
        if (config.showLogMessage) {
            if (command.startsWith("/")) {
                ChatUtils.chat("§7Command wheel ran: §e$command")
            } else {
                ChatUtils.chat("§7Command wheel sent: §e$command")
            }
        }
    }

    @HandleEvent
    private fun onGuiRenderOverlay() {
        if (!isOpen) return
        renderWheel()
    }

    private fun renderWheel() {
        val centerX = GuiScreenUtils.scaledWindowWidth / 2
        val centerY = GuiScreenUtils.scaledWindowHeight / 2
        val scale = config.sizePercent / 100.0
        val wheelRadius = (BASE_WHEEL_RADIUS * scale).toInt()
        val labelRadius = (BASE_LABEL_RADIUS * scale).toInt()

        DrawContextUtils.translated(centerX - wheelRadius, centerY - wheelRadius) {
            ShaderRenderUtils.drawFilledCircle(0, 0, backgroundColor, wheelRadius, smoothness = 10f)
        }

        sectorCommands.forEachIndexed { index, command ->
            val selected = index == selectedSector
            val angleRad = Math.toRadians(-90.0 + index * SECTOR_ANGLE)
            val labelX = centerX + (labelRadius * cos(angleRad)).toInt()
            val labelY = centerY + (labelRadius * sin(angleRad)).toInt()

            val prefix = if (selected) "§e§l" else "§7"
            val label = command ?: "§8${sectorNames[index]}"
            val renderable = Renderable.text(
                "$prefix$label",
                scale = scale,
                horizontalAlign = HorizontalAlignment.CENTER,
                verticalAlign = VerticalAlignment.CENTER,
            )
            Position(labelX - renderable.width / 2, labelY - renderable.height / 2)
                .renderRenderable(renderable, posLabel = "Command Wheel ${sectorNames[index]}", addToGuiManager = false)
        }

        val selectedCommand = selectedSector?.let { sectorCommands.getOrNull(it) }
        val hintText = if (config.executeOnRelease) "§7Release key to run" else "§7Left click to run"
        val centerText = if (selectedCommand != null) "§e§l$selectedCommand" else hintText
        val centerRenderable = Renderable.text(
            centerText,
            horizontalAlign = HorizontalAlignment.CENTER,
            verticalAlign = VerticalAlignment.CENTER,
        )
        Position(centerX - centerRenderable.width / 2, centerY - centerRenderable.height / 2 - 4)
            .renderRenderable(centerRenderable, posLabel = "Command Wheel Center", addToGuiManager = false)
    }

    @HandleEvent
    fun onWorldChange() {
        if (isOpen) close(execute = false)
    }
}
