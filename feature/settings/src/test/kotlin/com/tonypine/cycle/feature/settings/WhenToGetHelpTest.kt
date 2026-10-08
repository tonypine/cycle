package com.tonypine.cycle.feature.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.Language
import com.tonypine.cycle.core.testing.renderIn
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
    // Read at each use, so a test that switches language with renderIn reads the new one.
    private val resources get() = ApplicationProvider.getApplicationContext<Context>().resources

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

    @Test
    fun `each language says the same, line by line, as 0007 has it`() {
        Translations.forEach { (language, text) ->
            renderIn(language)

            assertEquals(language.tag, text.combined(text.introCombined), section(ContraceptionMethod.COMBINED_PILL))
            assertEquals(language.tag, text.combined(text.introPatch), section(ContraceptionMethod.PATCH))
            assertEquals(language.tag, text.combined(text.introRing), section(ContraceptionMethod.RING))
            assertEquals(language.tag, text.iud, section(ContraceptionMethod.COPPER_IUD))
            assertEquals(language.tag, text.iud, section(ContraceptionMethod.HORMONAL_IUD))
        }
    }

    /** One language's section, as 0007 has it under "Translations". */
    private class Translation(
        val introCombined: String,
        val introPatch: String,
        val introRing: String,
        val combined: (intro: String) -> List<String>,
        val iud: List<String>
    )

    private companion object {
        val Translations = mapOf(
            Language("pt-BR") to Translation(
                introCombined = "As clínicas passam estes sinais a todas as pessoas que usam " +
                    "a pílula combinada, o adesivo ou o anel.",
                introPatch = "As clínicas passam estes sinais a todas as pessoas que usam o adesivo.",
                introRing = "As clínicas passam estes sinais a todas as pessoas que usam o anel.",
                combined = { intro ->
                    listOf(
                        "Quando procurar ajuda",
                        intro,
                        "Procure atendimento de emergência agora se você tiver:",
                        "- dor no peito, ou falta de ar, ou tosse com sangue",
                        "- fraqueza ou dormência repentina no rosto, em um braço ou em uma perna, ou dificuldade para falar",
                        "Procure orientação médica urgente hoje se você tiver:",
                        "- dor, inchaço ou vermelhidão em uma perna, geralmente na panturrilha",
                        "O Cycle não verifica seu registro em busca destes sinais."
                    )
                },
                iud = listOf(
                    "Quando procurar ajuda",
                    "As clínicas passam estes sinais a todas as pessoas que usam um DIU.",
                    "Procure orientação médica urgente hoje se você tiver:",
                    "- dor no pé da barriga que não passa com analgésicos",
                    "- dor repentina no pé da barriga que piora ou não passa",
                    "- febre alta",
                    "- corrimento fora do comum ou com mau cheiro",
                    "- sangramento muito intenso",
                    "O Cycle não verifica seu registro em busca destes sinais."
                )
            ),
            Language("es") to Translation(
                introCombined = "Las clínicas dan estas señales a todas las personas que usan " +
                    "la píldora combinada, el parche o el anillo.",
                introPatch = "Las clínicas dan estas señales a todas las personas que usan el parche.",
                introRing = "Las clínicas dan estas señales a todas las personas que usan el anillo.",
                combined = { intro ->
                    listOf(
                        "Cuándo buscar ayuda",
                        intro,
                        "Busca ayuda de emergencia ahora si tienes:",
                        "- dolor en el pecho, o te falta el aire, o toses sangre",
                        "- debilidad o entumecimiento repentinos en la cara, un brazo o una pierna, o dificultad para hablar",
                        "Busca atención médica urgente hoy si tienes:",
                        "- dolor, hinchazón o enrojecimiento en una pierna, sobre todo en la pantorrilla",
                        "Cycle no revisa tu registro en busca de estas señales."
                    )
                },
                iud = listOf(
                    "Cuándo buscar ayuda",
                    "Las clínicas dan estas señales a todas las personas que llevan un DIU.",
                    "Busca atención médica urgente hoy si tienes:",
                    "- dolor en la parte baja de la barriga que no se calma con analgésicos",
                    "- dolor repentino en la parte baja de la barriga que empeora o no se va",
                    "- fiebre alta",
                    "- flujo vaginal raro o con mal olor",
                    "- un sangrado muy abundante",
                    "Cycle no revisa tu registro en busca de estas señales."
                )
            ),
            Language("de") to Translation(
                introCombined = "Praxen geben diese Anzeichen allen mit, die die Kombinationspille, " +
                    "das Pflaster oder den Ring verwenden.",
                introPatch = "Praxen geben diese Anzeichen allen mit, die das Pflaster verwenden.",
                introRing = "Praxen geben diese Anzeichen allen mit, die den Ring verwenden.",
                combined = { intro ->
                    listOf(
                        "Wann du Hilfe brauchst",
                        intro,
                        "Hol sofort Notfallhilfe, wenn du Folgendes hast:",
                        "- Schmerzen in der Brust, Atemnot oder du hustest Blut",
                        "- plötzliche Schwäche oder Taubheit im Gesicht, in einem Arm oder einem Bein, oder Schwierigkeiten beim Sprechen",
                        "Hol dir heute noch dringend ärztlichen Rat, wenn du Folgendes hast:",
                        "- Schmerzen, Schwellung oder Rötung in einem Bein, meist in der Wade",
                        "Cycle prüft deine Einträge nicht auf diese Anzeichen."
                    )
                },
                iud = listOf(
                    "Wann du Hilfe brauchst",
                    "Praxen geben diese Anzeichen allen mit, die eine Spirale haben.",
                    "Hol dir heute noch dringend ärztlichen Rat, wenn du Folgendes hast:",
                    "- Schmerzen im Unterbauch, gegen die Schmerzmittel nicht helfen",
                    "- plötzliche Schmerzen im Unterbauch, die schlimmer werden oder nicht weggehen",
                    "- hohes Fieber",
                    "- ungewöhnlichen oder übel riechenden Ausfluss",
                    "- sehr starke Blutungen",
                    "Cycle prüft deine Einträge nicht auf diese Anzeichen."
                )
            )
        )
    }
}
