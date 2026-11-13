package tv.cleanyour.core.planner

import tv.cleanyour.core.model.Component
import tv.cleanyour.core.model.PackageName
import tv.cleanyour.core.model.Snapshot

// AURA Edition: this app has its own launcher package, so a TV that the original Clean Your TV already
// set up has THAT launcher as Home. It is not the person's own choice but the sibling build of ours,
// so the switch treats it as the home screen to hand back to (it is switched off for the new one and
// back on by Undo). Built from parts on purpose: the build must not rename this package.
object LegacyHome {
    private val PKG = PackageName("com.gathin" + ".tvhome")
    val component = Component(PKG, ".MainActivity")

    // `stocks` plus the original launcher, only while it is what Home answers to.
    fun withLegacy(snapshot: Snapshot, stocks: List<Component>): List<Component> {
        val resolved = snapshot.home.resolved ?: return stocks
        if (resolved.pkg != PKG || stocks.any { it.pkg == PKG }) return stocks
        return stocks + Component(PKG, resolved.cls)
    }
}
