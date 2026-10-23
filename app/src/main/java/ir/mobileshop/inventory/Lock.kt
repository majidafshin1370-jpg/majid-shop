package ir.mobileshop.inventory

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest
import java.security.SecureRandom

/** Manager sees everything; a seller does not see buy prices, profit, reports, cheques or settings. */
enum class Role { Manager, Seller }

/** True while the manager is signed in (also when no lock is set). Screens hide money details when false. */
val LocalIsManager = compositionLocalOf { true }

/**
 * App lock kept on this phone only. The PIN is stored as a salted hash, never as text.
 * This keeps casual users out of the app; it does not encrypt the data on the phone.
 */
object ShopLock {
    private const val FILE = "lock"
    const val MIN_PIN = 4
    const val MAX_PIN = 8

    private fun prefs(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(c: Context) = !prefs(c).getString("hash", "").isNullOrEmpty()

    private fun hash(salt: String, pin: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun setPin(c: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        prefs(c).edit().putString("salt", salt).putString("hash", hash(salt, pin)).apply()
    }

    fun check(c: Context, pin: String): Boolean {
        val p = prefs(c)
        val salt = p.getString("salt", "") ?: ""
        val saved = p.getString("hash", "") ?: ""
        return saved.isNotEmpty() && hash(salt, pin) == saved
    }

    fun clear(c: Context) {
        prefs(c).edit().remove("salt").remove("hash").remove("biometric").apply()
    }

    fun biometricEnabled(c: Context) = prefs(c).getBoolean("biometric", false)
    fun setBiometricEnabled(c: Context, v: Boolean) = prefs(c).edit().putBoolean("biometric", v).apply()

    fun biometricAvailable(c: Context): Boolean =
        BiometricManager.from(c).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS

    fun showBiometric(activity: FragmentActivity, onSuccess: () -> Unit) {
        val prompt = BiometricPrompt(
            activity, ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("ورود مدیر")
            .setNegativeButtonText("انصراف")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }
}

private const val MAX_TRIES = 5
private const val LOCKOUT_MS = 60_000L

/** Covers the whole app until the manager enters the PIN (or fingerprint) or the seller entry is chosen. */
@Composable
fun LockScreen(onManager: () -> Unit, onSeller: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var failures by remember { mutableStateOf(0) }
    var lockedUntil by remember { mutableStateOf(0L) }
    val useBiometric = remember { ShopLock.biometricEnabled(context) && ShopLock.biometricAvailable(context) }

    // The lock screen is the home screen of a locked phone: back leaves the app instead of opening it.
    BackHandler { activity?.moveTaskToBack(true) }

    LaunchedEffect(Unit) {
        if (useBiometric && activity != null) ShopLock.showBiometric(activity, onManager)
    }

    Column(
        Modifier.fillMaxSize().background(PageGrey)
            .pointerInput(Unit) { detectTapGestures { } } // nothing underneath can be tapped
            .statusBarsPadding().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("فروشگاه بلاد شاپور", fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text("برنامه قفل است", color = InkGrey, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.digitsOnly().take(ShopLock.MAX_PIN); message = null },
            label = { Text("رمز مدیر", fontSize = 13.sp) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = shopFieldColors()
        )
        message?.let {
            Text(it, color = Red, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val now = System.currentTimeMillis()
                if (now < lockedUntil) {
                    message = "چند بار اشتباه زدید؛ ${((lockedUntil - now) / 1000 + 1).faNum()} ثانیه صبر کنید."
                } else if (ShopLock.check(context, pin)) {
                    failures = 0
                    onManager()
                } else {
                    failures++
                    pin = ""
                    if (failures >= MAX_TRIES) {
                        failures = 0
                        lockedUntil = now + LOCKOUT_MS
                        message = "رمز ${MAX_TRIES.faNum()} بار اشتباه بود؛ یک دقیقه صبر کنید."
                    } else {
                        message = "رمز اشتباه است."
                    }
                }
            },
            enabled = pin.length >= ShopLock.MIN_PIN,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Red, contentColor = Color.White)
        ) { Text("ورود مدیر", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        if (useBiometric && activity != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { ShopLock.showBiometric(activity, onManager) },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Fingerprint, null, tint = Red)
                Text("  ورود با اثر انگشت", color = InkDark)
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = onSeller,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("ورود به‌عنوان فروشنده", color = InkDark) }
        Text(
            "فروشنده قیمت خرید، سود، گزارش‌ها، چک‌ها و تنظیمات را نمی‌بیند.",
            color = InkGrey, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp)
        )
    }
}
