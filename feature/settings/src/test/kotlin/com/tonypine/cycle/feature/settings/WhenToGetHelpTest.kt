package com.tonypine.cycle.feature.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.model.ContraceptionMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * "When to get help" on each method, word for word as in
 * `docs/decisions/0007-urgent-symptoms-on-a-method.md`. Changing a word here means changing that
 * record in the same PR.
 */
@RunWith(AndroidJUnit4::class)
class WhenToGetHelpTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    /** The section as she reads it, line by line: title, intro, each action line and its signs, the end. */
    private fun section(method: ContraceptionMethod?): List<String>? = getHelp(method)?.let { help ->
        listOf(resources.getString(R.string.get_help_title), resources.getString(help.intro)) +
            help.actions.flatMap { action ->
                listOf(resources.getString(action.title)) + action.signs.map { "- " + resources.getString(it) }
            } +
            resources.getString(R.string.get_help_not_checked)
    }

    private fun combined(intro: String) = listOf(
        "When to get help",
        intro,
        "Get emergency help now if you have:",
        "- chest pain, or you feel short of breath, or you cough up blood",
        "- sudden weakness or numbness in your face, an arm or a leg, or trouble speaking",
        "Get urgent medical advice today if you have:",
        "- pain, swelling or redness in one leg, usually the calf",
        "Cycle does not check your log for these signs."
    )

    private val iud = listOf(
        "When to get help",
        "Clinics give these signs to everyone who has an IUD.",
        "Get urgent medical advice today if you have:",
        "- pain low in your tummy that painkillers do not help",
        "- sudden pain low in your tummy that gets worse or does not go away",
        "- a high temperature",
        "- unusual or smelly discharge",
        "- very heavy bleeding",
        "Cycle does not check your log for these signs."
    )

    @Test
    fun `the combined pill names all three combined methods`() {
        assertEquals(
            combined("Clinics give these signs to everyone who uses the combined pill, patch or ring."),
            section(ContraceptionMethod.COMBINED_PILL)
        )
    }

    @Test
    fun `the patch names the patch only`() {
        assertEquals(
            combined("Clinics give these signs to everyone who uses the patch."),
            section(ContraceptionMethod.PATCH)
        )
    }

    @Test
    fun `the ring names the ring only`() {
        assertEquals(
            combined("Clinics give these signs to everyone who uses the ring."),
            section(ContraceptionMethod.RING)
        )
    }

    @Test
    fun `both IUDs have the IUD text`() {
        assertEquals(iud, section(ContraceptionMethod.COPPER_IUD))
        assertEquals(iud, section(ContraceptionMethod.HORMONAL_IUD))
    }

    @Test
    fun `the progestogen-only pill, implant, injection and no method have none`() {
        assertNull(section(ContraceptionMethod.PROGESTOGEN_PILL))
        assertNull(section(ContraceptionMethod.IMPLANT))
        assertNull(section(ContraceptionMethod.INJECTION))
        assertNull(section(null))
    }
}
