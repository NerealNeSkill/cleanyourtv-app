package tv.cleanyour.core.planner

import java.util.Locale

// The plan's narration lines are English and double as keys inside the planner, so they stay as they are; the screens
// show them through here, which turns the known lines into Russian when the phone is in Russian. A line it does not
// know is shown as it is.
object NarrationText {
    private val exact = mapOf(
        "Restore point saved" to "Точка восстановления сохранена",
        "Giving the TV a moment" to "Даю ТВ секунду",
        "Installing the new home screen" to "Устанавливаю новый главный экран",
        "Restarting the updated home screen" to "Перезапускаю обновлённый главный экран",
        "Sending the look to the TV" to "Отправляю оформление на ТВ",
        "Showing the progress on the TV" to "Показываю ход работы на ТВ",
        "Saving the restore point on the TV" to "Сохраняю точку восстановления на ТВ",
        "Saving the restore list on the TV" to "Сохраняю список восстановления на ТВ",
        "Removing the new home screen from the TV" to "Удаляю новый главный экран с ТВ",
        "Switching on the Home button helper" to "Включаю помощника кнопки «Домой»",
        "Switching on the Home button helper (the TV's other accessibility services stay as they are)" to
            "Включаю помощника кнопки «Домой» (остальные службы специальных возможностей ТВ не трогаю)",
        "Switching off the Home button helper" to "Выключаю помощника кнопки «Домой»",
        "Switching off the TV's own home screen" to "Отключаю родной главный экран ТВ",
        "Switching the TV's own home screen back on" to "Возвращаю родной главный экран ТВ",
        "Switching the home screen's remote buttons back on" to "Возвращаю кнопки пульта родному главному экрану",
        "Switching the setup wizard back on" to "Возвращаю мастер первого запуска",
        "Switching on accessibility for the helper" to "Включаю специальные возможности для помощника",
        "Switching accessibility back off" to "Снова выключаю специальные возможности",
        "Switching debugging off" to "Выключаю отладку",
        "Switching wireless debugging off" to "Выключаю отладку по Wi-Fi",
        "Switch off the first-run setup wizard so the new home screen can take over" to
            "Отключаю мастер первого запуска, чтобы новый главный экран занял его место",
        "Replace the home screen app (its look, order, app list, settings and card answers on the TV are reset)" to
            "Заменяю приложение главного экрана (его вид, порядок, список приложений, настройки и ответы карточек на ТВ сбросятся)",
    )

    private val patterns = listOf(
        Regex("^Giving the TV (\\d+) seconds to settle$") to { m: MatchResult -> "Даю ТВ ${m.groupValues[1]} с, чтобы он успокоился" },
        Regex("^Updating the home screen to version (.+)$") to { m: MatchResult -> "Обновляю главный экран до версии ${m.groupValues[1]}" },
        Regex("^Switching off (.+), which also answers the Home button$") to { m: MatchResult -> "Отключаю ${m.groupValues[1]}: он тоже отвечает на кнопку «Домой»" },
        Regex("^Switching off (.+)$") to { m: MatchResult -> "Отключаю ${m.groupValues[1]}" },
        Regex("^Switching (.+) back on: PatchWall needs it$") to { m: MatchResult -> "Возвращаю ${m.groupValues[1]}: нужен PatchWall" },
        Regex("^Switching (.+) back on$") to { m: MatchResult -> "Возвращаю ${m.groupValues[1]}" },
        Regex("^Switching (.+) back off$") to { m: MatchResult -> "Снова отключаю ${m.groupValues[1]}" },
        Regex("^Putting back the TV's (.+) setting$") to { m: MatchResult -> "Возвращаю настройку ТВ: ${m.groupValues[1]}" },
        Regex("^Removing (.+) from the TV$") to { m: MatchResult -> "Удаляю ${m.groupValues[1]} с ТВ" },
        Regex("^Adding (.+) to the TV$") to { m: MatchResult -> "Добавляю ${m.groupValues[1]} на ТВ" },
    )

    fun localized(text: String, locale: Locale = Locale.getDefault()): String {
        if (locale.language != "ru") return text
        exact[text]?.let { return it }
        for ((re, make) in patterns) re.find(text)?.let { return make(it) }
        return text
    }
}
