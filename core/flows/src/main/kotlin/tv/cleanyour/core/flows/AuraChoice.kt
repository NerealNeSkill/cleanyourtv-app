package tv.cleanyour.core.flows

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import tv.cleanyour.core.spec.LauncherConfigDoc

// The AURA home screen of the launcher build that carries it (launcher/docs/aura.md): whether this TV
// opens AURA instead of the classic screen, and its look and switches. Kept per TV in tvs/<id>/aura.txt
// (AuraPrefs form, apart from home.txt so the Look editor's files stay as they were). A TV with no file
// gets none of these keys, so a TV that never chose AURA is sent exactly what it was before.
// The keys go into config.json through LauncherConfigDoc.unknown, as the launcher's AuraStyle.syncOverlay reads them.
data class AuraChoice(
    val enabled: Boolean = true,   // this fork's build carries AURA as its home screen
    val theme: Int = 0,           // 0 Orbit, 1 Graphite, 2 Light
    val focus: Int = 0,           // 0 mint, 1 blue, 2 violet, 3 amber, 4 rose
    val columns: Int = 5,         // 4 to 7 app columns
    val hints: Boolean = true,    // the control hints under the page
    val greeting: Boolean = true,
    val quick: Boolean = true,    // the quick actions row
    val voice: Boolean = true,
    val animations: Boolean = true,
) {
    // The keys this choice writes. Defaults are written too: a later change back must reach the TV.
    fun keys(): Map<String, JsonElement> = linkedMapOf(
        "aura_ui" to JsonPrimitive(enabled),
        "aura_theme" to JsonPrimitive(theme.coerceIn(0, 2)),
        "aura_focus" to JsonPrimitive(focus.coerceIn(0, 4)),
        "aura_cols" to JsonPrimitive(columns.coerceIn(4, 7)),
        "aura_hints" to JsonPrimitive(hints),
        "aura_greeting" to JsonPrimitive(greeting),
        "aura_quick" to JsonPrimitive(quick),
        "aura_voice" to JsonPrimitive(voice),
        "aura_anim" to JsonPrimitive(animations),
    )

    fun applyTo(doc: LauncherConfigDoc): LauncherConfigDoc = doc.copy(unknown = doc.unknown + keys())

    companion object {
        fun encode(c: AuraChoice): String = listOf(
            "enabled" to c.enabled, "theme" to c.theme, "focus" to c.focus, "cols" to c.columns, "hints" to c.hints,
            "greeting" to c.greeting, "quick" to c.quick, "voice" to c.voice, "anim" to c.animations,
        ).joinToString("") { (k, v) -> "$k=$v\n" }

        fun decode(text: String): AuraChoice {
            val map = text.lines().filter { '=' in it }.associate { it.substringBefore('=') to it.substringAfter('=') }
            val d = AuraChoice()
            fun bool(k: String, or: Boolean) = map[k]?.toBooleanStrictOrNull() ?: or
            fun int(k: String, r: IntRange, or: Int) = map[k]?.toIntOrNull()?.takeIf { it in r } ?: or
            return AuraChoice(
                enabled = bool("enabled", d.enabled), theme = int("theme", 0..2, d.theme), focus = int("focus", 0..4, d.focus),
                columns = int("cols", 4..7, d.columns), hints = bool("hints", d.hints), greeting = bool("greeting", d.greeting),
                quick = bool("quick", d.quick), voice = bool("voice", d.voice), animations = bool("anim", d.animations),
            )
        }
    }
}
