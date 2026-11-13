package tv.cleanyour.feature.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tv.cleanyour.core.flows.AuraChoice
import tv.cleanyour.ui.design.component.CytBottomSheet
import tv.cleanyour.ui.design.component.ListDivider
import tv.cleanyour.ui.design.component.ListGroup
import tv.cleanyour.ui.design.component.PrimaryButton
import tv.cleanyour.ui.design.component.SegmentOption
import tv.cleanyour.ui.design.component.SegmentedChoice
import tv.cleanyour.ui.design.component.SheetHeader
import tv.cleanyour.ui.design.component.SwitchRow

// The AURA home screen: pick it instead of the classic one and set its look. "Put it on the TV" saves the
// choice and sends the saved look again with it (HomeScreenIntent.SetAura), so the TV changes in one push.
@Composable
internal fun AuraRowSummary(aura: AuraChoice?): String =
    if (aura?.enabled == true) stringResource(R.string.home_aura_on) else stringResource(R.string.home_aura_off)

@Composable
internal fun AuraSheet(current: AuraChoice?, onDone: (AuraChoice) -> Unit, onDismiss: () -> Unit) {
    var c by remember { mutableStateOf(current ?: AuraChoice()) }
    val themes = listOf(R.string.home_aura_theme_orbit, R.string.home_aura_theme_graphite, R.string.home_aura_theme_light).map { stringResource(it) }
    val colours = listOf(R.string.home_aura_focus_mint, R.string.home_aura_focus_blue, R.string.home_aura_focus_violet,
        R.string.home_aura_focus_amber, R.string.home_aura_focus_rose).map { stringResource(it) }
    CytBottomSheet(onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            SheetHeader(stringResource(R.string.home_aura_title), subtitle = stringResource(R.string.home_aura_note))
            ListGroup(Modifier.padding(top = 16.dp)) {
                SwitchRow(stringResource(R.string.home_aura_use), c.enabled, { c = c.copy(enabled = it) },
                    subtitle = stringResource(R.string.home_aura_use_note))
            }
            if (c.enabled) {
                Body(stringResource(R.string.home_aura_theme), Modifier.padding(top = 16.dp, start = 4.dp))
                SegmentedChoice(themes.mapIndexed { i, n -> SegmentOption("t$i", n, n) }, "t${c.theme}", { c = c.copy(theme = it.key.drop(1).toInt()) },
                    Modifier.padding(top = 8.dp))
                Body(stringResource(R.string.home_aura_focus), Modifier.padding(top = 16.dp, start = 4.dp))
                SegmentedChoice(colours.mapIndexed { i, n -> SegmentOption("f$i", n, n) }, "f${c.focus}", { c = c.copy(focus = it.key.drop(1).toInt()) },
                    Modifier.padding(top = 8.dp))
                Body(stringResource(R.string.home_aura_columns), Modifier.padding(top = 16.dp, start = 4.dp))
                SegmentedChoice((4..7).map { SegmentOption("c$it", "$it", "$it") }, "c${c.columns}", { c = c.copy(columns = it.key.drop(1).toInt()) },
                    Modifier.padding(top = 8.dp))
                ListGroup(Modifier.padding(top = 16.dp)) {
                    SwitchRow(stringResource(R.string.home_aura_greeting), c.greeting, { c = c.copy(greeting = it) })
                    ListDivider()
                    SwitchRow(stringResource(R.string.home_aura_quick), c.quick, { c = c.copy(quick = it) })
                    ListDivider()
                    SwitchRow(stringResource(R.string.home_aura_hints), c.hints, { c = c.copy(hints = it) })
                    ListDivider()
                    SwitchRow(stringResource(R.string.home_aura_voice), c.voice, { c = c.copy(voice = it) })
                    ListDivider()
                    SwitchRow(stringResource(R.string.home_aura_anim), c.animations, { c = c.copy(animations = it) },
                        subtitle = stringResource(R.string.home_aura_anim_note))
                }
                Body(stringResource(R.string.home_aura_more), Modifier.padding(top = 12.dp, start = 4.dp))
            }
            PrimaryButton(stringResource(R.string.home_aura_send), { onDone(c) }, Modifier.padding(top = 20.dp, bottom = 16.dp), guardObscured = true)
        }
    }
}
