package ir.mobileshop.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Settings card: turn the app lock on/off, change the PIN, switch fingerprint. Manager only. */
@Composable
fun LockSettingsCard() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(ShopLock.isEnabled(context)) }
    var bio by remember { mutableStateOf(ShopLock.biometricEnabled(context)) }
    var current by remember { mutableStateOf("") }
    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var good by remember { mutableStateOf(false) }

    fun report(text: String, ok: Boolean) { message = text; good = ok }
    fun clearFields() { current = ""; pin1 = ""; pin2 = "" }

    @Composable
    fun PinField(label: String, value: String, onChange: (String) -> Unit) {
        OutlinedTextField(
            value = value,
            onValueChange = { onChange(it.digitsOnly().take(ShopLock.MAX_PIN)) },
            label = { Text(label, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = RoundedCornerShape(12.dp),
            colors = shopFieldColors()
        )
    }

    /** Checks the two new PINs; returns the PIN or null after showing the problem. */
    fun newPinOrNull(): String? {
        if (pin1.length < ShopLock.MIN_PIN) { report("رمز باید حداقل ${ShopLock.MIN_PIN.faNum()} رقم باشد.", false); return null }
        if (pin1 != pin2) { report("دو رمز یکسان نیستند.", false); return null }
        return pin1
    }

    WhiteCard {
        SectionTitle("قفل برنامه و نقش‌ها")
        Text(
            "با رمز (و اثر انگشت) ورود به برنامه محدود می‌شود. فروشنده بدون رمز با دکمه‌ی «ورود به‌عنوان فروشنده» وارد می‌شود " +
                "و قیمت خرید، سود، گزارش‌ها، مالی و تنظیمات را نمی‌بیند. اگر رمز را فراموش کنید باید برنامه را دوباره نصب کنید " +
                "(اطلاعات روی سرور می‌ماند).",
            color = InkGrey, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!enabled) {
                PinField("رمز جدید (${ShopLock.MIN_PIN.faNum()} تا ${ShopLock.MAX_PIN.faNum()} رقم)", pin1) { pin1 = it }
                PinField("تکرار رمز", pin2) { pin2 = it }
                Button(
                    onClick = {
                        val p = newPinOrNull() ?: return@Button
                        ShopLock.setPin(context, p)
                        enabled = true
                        clearFields()
                        report("قفل فعال شد.", true)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Red, contentColor = Color.White)
                ) { Text("فعال‌سازی قفل", fontWeight = FontWeight.Bold) }
            } else {
                if (ShopLock.biometricAvailable(context)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ورود مدیر با اثر انگشت", Modifier.weight(1f), fontSize = 14.sp)
                        Switch(checked = bio, onCheckedChange = {
                            bio = it
                            ShopLock.setBiometricEnabled(context, it)
                        })
                    }
                }
                PinField("رمز فعلی", current) { current = it }
                PinField("رمز جدید (برای تغییر)", pin1) { pin1 = it }
                PinField("تکرار رمز جدید", pin2) { pin2 = it }
                Button(
                    onClick = {
                        if (!ShopLock.check(context, current)) { report("رمز فعلی درست نیست.", false); return@Button }
                        val p = newPinOrNull() ?: return@Button
                        ShopLock.setPin(context, p)
                        clearFields()
                        report("رمز تغییر کرد.", true)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Red, contentColor = Color.White)
                ) { Text("تغییر رمز", fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = {
                        if (!ShopLock.check(context, current)) { report("برای غیرفعال‌کردن، رمز فعلی را وارد کنید.", false); return@OutlinedButton }
                        ShopLock.clear(context)
                        enabled = false
                        bio = false
                        clearFields()
                        report("قفل غیرفعال شد.", true)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("غیرفعال‌کردن قفل", color = Red, fontWeight = FontWeight.Bold) }
            }
            message?.let { Text(it, color = if (good) GoodGreen else Red, fontSize = 13.sp) }
        }
    }
}
