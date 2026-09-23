package net.mada.lumea.data.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Le verrou par code, et le bug qui rendait l'application inaccessible.
 *
 * Le pavé essaie le code tout seul dès quatre chiffres, pour que les codes courts
 * s'ouvrent sans appuyer sur « Valider ». Ces essais automatiques comptaient
 * comme des échecs : avec un code à six chiffres, une seule saisie en produisait
 * deux (aux 4ᵉ et 5ᵉ chiffres). Le quota de quatre tentatives tombait en deux
 * essais, le verrouillage temporaire s'enclenchait, et il rejetait ensuite le bon
 * code lui-même. Le code devenait impossible à entrer.
 *
 * Ces tests tournent sur appareil : les préférences chiffrées s'appuient sur le
 * Keystore matériel, qui n'existe pas sur la JVM de développement.
 */
@RunWith(AndroidJUnit4::class)
class LockManagerTest {

    private lateinit var lock: LockManager

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        lock = LockManager(context)
        lock.clearPin()
    }

    @After
    fun tearDown() {
        lock.clearPin()
    }

    @Test
    fun unCodeALongueurMaximaleResteSaisissable() {
        lock.setPin("123456")

        // Ce que fait le pavé quand on tape « 123456 » : deux essais silencieux
        // aux 4ᵉ et 5ᵉ chiffres, puis l'essai réel au 6ᵉ.
        assertFalse(lock.check("1234", silent = true))
        assertFalse(lock.check("12345", silent = true))

        // Les essais automatiques ne consomment aucune tentative…
        assertEquals(0, lock.failedAttempts())
        assertEquals(0L, lock.remainingLockoutMillis())

        // …donc le bon code passe.
        assertTrue(lock.verify("123456"))
    }

    @Test
    fun plusieursSaisiesDAffileeNeVerrouillentPas() {
        lock.setPin("987654")

        // Cinq saisies complètes : dix essais silencieux au total. Avant la
        // correction, le verrouillage s'enclenchait dès la deuxième.
        repeat(5) {
            lock.check("9876", silent = true)
            lock.check("98765", silent = true)
        }

        assertEquals(0, lock.failedAttempts())
        assertTrue(lock.verify("987654"))
    }

    @Test
    fun unMauvaisCodeValideCompteToujours() {
        lock.setPin("1234")

        assertFalse(lock.verify("0000"))
        assertEquals(1, lock.failedAttempts())

        assertFalse(lock.verify("1111"))
        assertEquals(2, lock.failedAttempts())
    }

    @Test
    fun leBonCodeRemetLeCompteurAZero() {
        lock.setPin("1234")

        lock.verify("0000")
        lock.verify("1111")
        assertEquals(2, lock.failedAttempts())

        assertTrue(lock.verify("1234"))
        assertEquals(0, lock.failedAttempts())
        assertEquals(0L, lock.remainingLockoutMillis())
    }

    @Test
    fun leVerrouillageTemporaireSDeclencheApresQuatreVraisEchecs() {
        lock.setPin("1234")

        repeat(4) { lock.verify("0000") }

        assertTrue("le verrouillage devrait être actif", lock.remainingLockoutMillis() > 0)
        // Même le bon code est refusé pendant le délai : c'est voulu.
        assertFalse(lock.verify("1234"))
    }
}
