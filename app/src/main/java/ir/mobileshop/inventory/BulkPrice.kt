package ir.mobileshop.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun roundTo(v: Long, step: Long): Long =
    if (step <= 1L) v else ((v + step / 2) / step) * step

/** New price after the change; never below zero. [percent] = true means [value] is a percentage. */
private fun changed(old: Long, increase: Boolean, percent: Boolean, value: Long, step: Long): Long {
    val delta = if (percent) old * value / 100 else value
    val raw = if (increase) old + delta else old - delta
    return roundTo(raw.coerceAtLeast(0L), step)
}

/** Manager-only: raise or lower prices of many products at once, with a preview before anything is saved. */
@Composable
fun BulkPriceScreen(vm: InventoryViewModel, onBack: () -> Unit) {
    val all by vm.allProducts.collectAsState()
    var brand by rememberSaveable { mutableStateOf<String?>(null) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var field by rememberSaveable { mutableStateOf(0) }          // 0 sell, 1 buy, 2 both
    var increase by rememberSaveable { mutableStateOf(true) }
    var percent by rememberSaveable { mutableStateOf(true) }
    var valueText by rememberSaveable { mutableStateOf("") }
    var step by rememberSaveable { mutableStateOf(1000L) }
    var confirm by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf<String?>(null) }

    val brands = all.map { it.brand.trim() }.filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }.sorted()
    val value = valueText.toLongOrNull() ?: 0L
    val scope = all.filter {
        (brand == null || it.brand.trim().equals(brand, true)) && (category == null || it.category == category)
    }
    val valid = value > 0 && (!percent || value <= 100 || increase)
    val updated = if (!valid) emptyList() else scope.map { p ->
        p.copy(
            sellPrice = if (field != 1) changed(p.sellPrice, increase, percent, value, step) else p.sellPrice,
            buyPrice = if (field != 0) changed(p.buyPrice, increase, percent, value, step) else p.buyPrice
        ) to p
    }.filter { (n, o) -> n.sellPrice != o.sellPrice || n.buyPrice != o.buyPrice }

    val fieldName = listOf("قیمت فروش", "قیمت خرید", "قیمت خرید و فروش")[field]
    val summary = buildString {
        append(brand ?: "همه‌ی برندها")
        if (category != null) append(" / $category")
        append(" — $fieldName ")
        append(if (increase) "+" else "−")
        append(if (percent) "${value.faNum()}٪" else value.faMoney())
    }

    Column(Modifier.fillMaxSize().background(PageGrey)) {
        HeroHeader(height = 84.dp) {
            Row(
                Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderIcon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", onBack)
                Text("تغییر گروهی قیمت", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WhiteCard {
                SectionTitle("کدام کالاها؟")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryChip("همه‌ی برندها", brand == null) { brand = null }
                    brands.forEach { b -> CategoryChip(b, brand.equals(b, true)) { brand = b } }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryChip("همه‌ی دسته‌ها", category == null) { category = null }
                    CATEGORIES.forEach { c -> CategoryChip(c, category == c) { category = c } }
                }
            }
            WhiteCard {
                SectionTitle("کدام قیمت؟")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryChip("فروش", field == 0) { field = 0 }
                    CategoryChip("خرید", field == 1) { field = 1 }
                    CategoryChip("هر دو", field == 2) { field = 2 }
                }
                SectionTitle("چه تغییری؟")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryChip("افزایش", increase) { increase = true }
                    CategoryChip("کاهش", !increase) { increase = false }
                    CategoryChip("درصد", percent) { percent = true }
                    CategoryChip("مبلغ ثابت", !percent) { percent = false }
                }
                Spacer(Modifier.height(8.dp))
                DialogField(
                    label = if (percent) "درصد (مثلاً ۳)" else "مبلغ (تومان)",
                    value = valueText, keyboard = KeyboardType.Number
                ) { valueText = it.digitsOnly().take(12) }
                if (percent && increase.not() && value > 100) {
                    Text("درصد کاهش نمی‌تواند بیشتر از ۱۰۰ باشد.", color = Red, fontSize = 12.sp)
                }
                SectionTitle("گرد کردن قیمت جدید")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryChip("بدون گرد کردن", step == 1L) { step = 1L }
                    CategoryChip("به هزار تومان", step == 1000L) { step = 1000L }
                    CategoryChip("به ده هزار تومان", step == 10000L) { step = 10000L }
                }
            }
            WhiteCard {
                SectionTitle("پیش‌نمایش")
                if (!valid) {
                    Text("مقدار تغییر را وارد کنید.", color = InkGrey, fontSize = 13.sp)
                } else {
                    Text(
                        "${updated.size.faNum()} کالا از ${scope.size.faNum()} کالای انتخاب‌شده تغییر می‌کند.",
                        fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                    updated.take(5).forEach { (n, o) ->
                        Spacer(Modifier.height(8.dp))
                        Text(listOf(o.brand, o.model, o.color).filter { it.isNotBlank() }.joinToString(" "), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        if (n.sellPrice != o.sellPrice) {
                            Text("فروش: ${o.sellPrice.faNum()}  ←  ${n.sellPrice.faNum()}", fontSize = 12.sp, color = InkDark)
                        }
                        if (n.buyPrice != o.buyPrice) {
                            Text("خرید: ${o.buyPrice.faNum()}  ←  ${n.buyPrice.faNum()}", fontSize = 12.sp, color = InkDark)
                        }
                    }
                    if (updated.size > 5) {
                        Spacer(Modifier.height(6.dp))
                        Text("و ${(updated.size - 5).faNum()} کالای دیگر…", fontSize = 12.sp, color = InkGrey)
                    }
                }
            }
            done?.let { Text(it, color = GoodGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
        Surface(color = Color.White, shadowElevation = 10.dp) {
            Column(Modifier.navigationBarsPadding().padding(16.dp)) {
                Button(
                    onClick = { confirm = true },
                    enabled = updated.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Red, contentColor = Color.White)
                ) { Text("اعمال تغییر روی ${updated.size.faNum()} کالا", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("اعمال تغییر گروهی؟", fontWeight = FontWeight.Bold) },
            text = { Text("$summary\n${updated.size.faNum()} کالا تغییر می‌کند و در سابقه‌ی تغییرات ثبت می‌شود.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.applyBulkPrices(updated.map { it.first }, summary)
                    confirm = false
                    done = "${updated.size.faNum()} کالا به‌روز شد."
                    valueText = ""
                }) { Text("اعمال کن", color = Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("انصراف") } }
        )
    }
}
