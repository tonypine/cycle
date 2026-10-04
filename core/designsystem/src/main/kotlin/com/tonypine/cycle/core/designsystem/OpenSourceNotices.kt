package com.tonypine.cycle.core.designsystem

import androidx.annotation.RawRes

/**
 * A third-party asset bundled in the app, with the full licence text and copyright notice in a raw
 * resource next to the asset. The texts ship in the APK; a licences screen can list them from here.
 */
class OpenSourceNotice(val name: String, val license: String, @param:RawRes val text: Int)

val OpenSourceNotices: List<OpenSourceNotice> = listOf(
    OpenSourceNotice("Bricolage Grotesque", "SIL Open Font License 1.1", R.raw.license_bricolage_grotesque),
    OpenSourceNotice("DM Sans", "SIL Open Font License 1.1", R.raw.license_dm_sans),
    OpenSourceNotice("Material Symbols Rounded", "Apache License 2.0", R.raw.license_material_symbols)
)
