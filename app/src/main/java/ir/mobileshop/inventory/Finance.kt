package ir.mobileshop.inventory

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val EXPENSE_CATEGORIES = listOf("اجاره", "حقوق", "قبض‌ها", "حمل‌ونقل", "تبلیغات", "تعمیرات", "خرید لوازم", "سایر")

private fun statusLabel(s: String) = when (s) {
    "done" -> "وصول شده"
    "bounced" -> "برگشتی"
    else -> "در انتظار"
}

private fun daysText(date: String): Pair<String, Boolean> {
    val d = JalaliDates.daysUntil(date) ?: return "" to false
    return when {
        d < 0 -> "${(-d).faNum()} روز گذشته" to true
        d == 0 -> "امروز" to true
        d == 1 -> "فردا" to true
        else -> "${d.faNum()} روز دیگر" to false
    }
}

/** Last [n] Shamsi months as "yyyy/MM" keys, newest first. */
private fun lastMonths(n: Int): List<String> {
    val t = JalaliDates.parseShamsi(JalaliDates.getShamsiToday()) ?: return emptyList()
    var y = t[0]
    var m = t[1]
    return List(n) {
        val key = "%04d/%02d".format(java.util.Locale.US, y, m)
        m -= 1
        if (m == 0) { m = 12; y -= 1 }
        key
    }
}

private fun monthTitle(key: String): String {
    val p = key.split("/")
    val m = p.getOrNull(1)?.toIntOrNull() ?: return key
    return "${MONTH_NAMES.getOrElse(m - 1) { "" }} ${p[0]}".fa()
}

/** Manager-only tab: monthly net profit, cheques and expenses. */
@Composable
fun FinanceTab(vm: InventoryViewModel, contentPadding: PaddingValues) {
    val context = LocalContext.current
    var section by rememberSaveable { mutableStateOf(0) } // 0 summary, 1 cheques, 2 expenses

    // Android 13+ needs the user's OK before a notification can be shown (cheque due-date reminder).
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission(context)) {
            askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(Modifier.fillMaxSize().background(PageGrey)) {
        HeroHeader(height = 84.dp) {
            Row(
                Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("مالی", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryChip("خلاصه", section == 0) { section = 0 }
            CategoryChip("چک‌ها", section == 1) { section = 1 }
            CategoryChip("هزینه‌ها", section == 2) { section = 2 }
        }
        Box(Modifier.weight(1f)) {
            when (section) {
                0 -> SummarySection(vm, contentPadding)
                1 -> ChequeSection(vm, contentPadding)
                else -> ExpenseSection(vm, contentPadding)
            }
        }
    }
}

// ───────────────────────────── Summary ─────────────────────────────

@Composable
private fun SummarySection(vm: InventoryViewModel, pad: PaddingValues) {
    val sales by vm.sales.collectAsState()
    val expenses by vm.expenses.collectAsState()
    val cheques by vm.cheques.collectAsState()
    val months = remember { lastMonths(6) }
    var month by rememberSaveable { mutableStateOf(months.firstOrNull() ?: "") }

    val monthSales = sales.filter { it.date.latinDigits().startsWith(month) }
    val monthExpenses = expenses.filter { it.date.latinDigits().startsWith(month) }
    val revenue = monthSales.sumOf { it.total }
    val profit = monthSales.sumOf { it.profit }
    val spent = monthExpenses.sumOf { it.amount }
    val net = profit - spent
    val pending = cheques.filter { it.status == "pending" }
    val dueSoon = pending.count { (JalaliDates.daysUntil(it.dueDate) ?: 999) <= 3 }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(16.dp).padding(bottom = pad.calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            months.forEach { m -> CategoryChip(monthTitle(m), month == m) { month = m } }
        }
        WhiteCard {
            SectionTitle("سود خالص ${monthTitle(month)}")
            Spacer(Modifier.height(6.dp))
            Text(
                net.faMoney(), fontSize = 24.sp, fontWeight = FontWeight.Bold,
                color = if (net >= 0) GoodGreen else Red
            )
            Spacer(Modifier.height(10.dp))
            SummaryLine("جمع فروش", revenue.faMoney(), InkDark)
            SummaryLine("سود فروش (قبل از هزینه)", profit.faMoney(), if (profit >= 0) GoodGreen else Red)
            SummaryLine("هزینه‌ها", spent.faMoney(), Red)
            Spacer(Modifier.height(6.dp))
            Text("سود خالص = سود فروش − هزینه‌های ثبت‌شده در همین ماه.", fontSize = 11.sp, color = InkGrey)
        }
        WhiteCard {
            SectionTitle("چک‌های در انتظار")
            Spacer(Modifier.height(6.dp))
            SummaryLine("دریافتی", pending.filter { it.direction == "received" }.sumOf { it.amount }.faMoney(), GoodGreen)
            SummaryLine("پرداختی", pending.filter { it.direction == "paid" }.sumOf { it.amount }.faMoney(), Red)
            if (dueSoon > 0) {
                Spacer(Modifier.height(6.dp))
                Text("${dueSoon.faNum()} چک تا ۳ روز دیگر سررسید می‌شود یا گذشته است.", fontSize = 12.sp, color = Red)
            }
        }
    }
}

@Composable
private fun SummaryLine(title: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(title, Modifier.weight(1f), fontSize = 13.sp, color = InkGrey)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ───────────────────────────── Cheques ─────────────────────────────

@Composable
private fun ChequeSection(vm: InventoryViewModel, pad: PaddingValues) {
    val cheques by vm.cheques.collectAsState()
    var filter by rememberSaveable { mutableStateOf("pending") } // pending / done / bounced / all
    var editing by remember { mutableStateOf<Cheque?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Cheque?>(null) }

    val list = cheques.filter { filter == "all" || it.status == filter }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = pad.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { adding = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Red, contentColor = Color.White)
                ) { Text("ثبت چک جدید", fontWeight = FontWeight.Bold) }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryChip("در انتظار", filter == "pending") { filter = "pending" }
                    CategoryChip("وصول شده", filter == "done") { filter = "done" }
                    CategoryChip("برگشتی", filter == "bounced") { filter = "bounced" }
                    CategoryChip("همه", filter == "all") { filter = "all" }
                }
                if (list.isEmpty()) Text("چکی در این بخش نیست.", color = InkGrey, fontSize = 13.sp)
            }
        }
        items(list, key = { it.id }) { c ->
            ChequeCard(
                c, onOpen = { editing = c },
                onStatus = { vm.setChequeStatus(c, it) },
                onDelete = { deleting = c }
            )
        }
    }

    if (adding) ChequeDialog(vm, null) { adding = false }
    editing?.let { ChequeDialog(vm, it) { editing = null } }
    deleting?.let { c ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("حذف چک؟", fontWeight = FontWeight.Bold) },
            text = { Text("چک ${c.amount.faMoney()} حذف می‌شود.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteCheque(c); deleting = null }) { Text("حذف", color = Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun ChequeCard(c: Cheque, onOpen: () -> Unit, onStatus: (String) -> Unit, onDelete: () -> Unit) {
    val received = c.direction == "received"
    val (left, urgent) = if (c.status == "pending") daysText(c.dueDate) else "" to false
    WhiteCard(Modifier.clickable(onClick = onOpen)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (received) "دریافتی" else "پرداختی",
                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = if (received) GoodGreen else Red,
                modifier = Modifier
                    .background(if (received) GoodGreenSoft else RedSoft, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
            Spacer(Modifier.padding(horizontal = 4.dp))
            Text(c.party.ifBlank { "بدون نام" }, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(statusLabel(c.status), fontSize = 12.sp, color = InkGrey)
        }
        Spacer(Modifier.height(6.dp))
        Text(c.amount.faMoney(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        val sub = listOf(c.bank, if (c.number.isNotBlank()) "شماره ${c.number}" else "").filter { it.isNotBlank() }.joinToString(" • ")
        if (sub.isNotBlank()) Text(sub.fa(), fontSize = 12.sp, color = InkGrey)
        Text(
            "سررسید: ${c.dueDate.fa()}" + if (left.isNotEmpty()) "  ($left)" else "",
            fontSize = 12.sp, color = if (urgent) Red else InkDark,
            fontWeight = if (urgent) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (c.status == "pending") {
                OutlinedButton(onClick = { onStatus("done") }, shape = RoundedCornerShape(10.dp)) {
                    Text(if (received) "وصول شد" else "پرداخت شد", fontSize = 12.sp)
                }
                OutlinedButton(onClick = { onStatus("bounced") }, shape = RoundedCornerShape(10.dp)) {
                    Text("برگشت خورد", fontSize = 12.sp, color = Red)
                }
            } else {
                TextButton(onClick = { onStatus("pending") }) { Text("بازگشت به انتظار", fontSize = 12.sp, color = InkGrey) }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "حذف چک", tint = InkGrey) }
        }
    }
}

@Composable
private fun ChequeDialog(vm: InventoryViewModel, existing: Cheque?, onDismiss: () -> Unit) {
    val debtors by vm.debtors.collectAsState()
    val open = debtors.filter { !it.debtor.closed }
    var direction by remember { mutableStateOf(existing?.direction ?: "received") }
    var amount by remember { mutableStateOf(existing?.amount?.toString() ?: "") }
    var party by remember { mutableStateOf(existing?.party ?: "") }
    var number by remember { mutableStateOf(existing?.number ?: "") }
    var bank by remember { mutableStateOf(existing?.bank ?: "") }
    var due by remember { mutableStateOf(existing?.dueDate ?: JalaliDates.getShamsiToday()) }
    var debtorId by remember { mutableStateOf(existing?.debtorId ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "ثبت چک" else "ویرایش چک", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryChip("دریافتی", direction == "received") { direction = "received" }
                    CategoryChip("پرداختی", direction == "paid") { direction = "paid" }
                }
                DialogField("مبلغ (تومان)", amount, keyboard = KeyboardType.Number) { amount = it.digitsOnly().take(13) }
                DialogField(if (direction == "received") "از طرف (نام)" else "به نام", party) { party = it }
                DialogField("شماره چک", number, keyboard = KeyboardType.Number) { number = it.digitsOnly().take(20) }
                DialogField("بانک", bank) { bank = it }
                ShamsiDateField("تاریخ سررسید", due) { due = it }
                if (direction == "received" && open.isNotEmpty()) {
                    val idx = open.indexOfFirst { it.debtor.id == debtorId }.takeIf { it >= 0 }
                    PickerBox(
                        placeholder = "مربوط به بدهکار (اختیاری)",
                        text = idx?.let { open[it].debtor.name },
                        items = open.map { it.debtor.name },
                        selected = idx,
                        modifier = Modifier.fillMaxWidth()
                    ) { debtorId = open[it].debtor.id }
                    if (debtorId.isNotEmpty()) {
                        TextButton(onClick = { debtorId = "" }) { Text("بدون بدهکار", fontSize = 12.sp, color = InkGrey) }
                    }
                }
                DialogField("توضیحات", note) { note = it }
                error?.let { Text(it, color = Red, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = amount.toLongOrNull() ?: 0L
                if (a <= 0) { error = "مبلغ را وارد کنید."; return@TextButton }
                if (due.isBlank()) { error = "تاریخ سررسید را انتخاب کنید."; return@TextButton }
                val base = existing ?: Cheque(direction = direction, amount = a, dueDate = due)
                vm.saveCheque(
                    base.copy(
                        direction = direction, amount = a, party = party.trim(), number = number,
                        bank = bank.trim(), dueDate = due,
                        debtorId = if (direction == "received") debtorId else "", note = note.trim()
                    )
                )
                onDismiss()
            }) { Text("ذخیره", color = Red, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

// ───────────────────────────── Expenses ─────────────────────────────

@Composable
private fun ExpenseSection(vm: InventoryViewModel, pad: PaddingValues) {
    val expenses by vm.expenses.collectAsState()
    val thisMonth = remember { lastMonths(1).firstOrNull() ?: "" }
    var editing by remember { mutableStateOf<Expense?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Expense?>(null) }

    val monthList = expenses.filter { it.date.latinDigits().startsWith(thisMonth) }
    val byCategory = monthList.groupBy { it.category }.map { it.key to it.value.sumOf { e -> e.amount } }
        .sortedByDescending { it.second }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = pad.calculateBottomPadding() + 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { adding = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Red, contentColor = Color.White)
                ) { Text("ثبت هزینه", fontWeight = FontWeight.Bold) }
                WhiteCard {
                    SectionTitle("هزینه‌های ${monthTitle(thisMonth)}")
                    Text(monthList.sumOf { it.amount }.faMoney(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Red)
                    byCategory.forEach { (c, v) ->
                        SummaryLine(c, v.faMoney(), InkDark)
                    }
                }
                SectionTitle("همه‌ی هزینه‌ها")
                if (expenses.isEmpty()) Text("هنوز هزینه‌ای ثبت نشده.", color = InkGrey, fontSize = 13.sp)
            }
        }
        items(expenses, key = { it.id }) { e ->
            WhiteCard(Modifier.clickable { editing = e }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(e.category, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(listOf(e.date.fa(), e.note).filter { it.isNotBlank() }.joinToString(" • "), fontSize = 12.sp, color = InkGrey)
                    }
                    Text(e.amount.faMoney(), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Red)
                    IconButton(onClick = { deleting = e }) { Icon(Icons.Filled.Delete, "حذف هزینه", tint = InkGrey) }
                }
            }
        }
    }

    if (adding) ExpenseDialog(vm, null) { adding = false }
    editing?.let { ExpenseDialog(vm, it) { editing = null } }
    deleting?.let { e ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("حذف هزینه؟", fontWeight = FontWeight.Bold) },
            text = { Text("${e.category} — ${e.amount.faMoney()}") },
            confirmButton = {
                TextButton(onClick = { vm.deleteExpense(e); deleting = null }) { Text("حذف", color = Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun ExpenseDialog(vm: InventoryViewModel, existing: Expense?, onDismiss: () -> Unit) {
    var category by remember { mutableStateOf(existing?.category ?: EXPENSE_CATEGORIES.first()) }
    var amount by remember { mutableStateOf(existing?.amount?.toString() ?: "") }
    var date by remember { mutableStateOf(existing?.date ?: JalaliDates.getShamsiToday()) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "ثبت هزینه" else "ویرایش هزینه", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ComboField("نوع هزینه", category, EXPENSE_CATEGORIES.map { ComboOption(it) }) { category = it }
                DialogField("مبلغ (تومان)", amount, keyboard = KeyboardType.Number) { amount = it.digitsOnly().take(13) }
                ShamsiDateField("تاریخ", date) { date = it }
                DialogField("توضیحات", note) { note = it }
                error?.let { Text(it, color = Red, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = amount.toLongOrNull() ?: 0L
                if (a <= 0) { error = "مبلغ را وارد کنید."; return@TextButton }
                if (category.isBlank()) { error = "نوع هزینه را انتخاب کنید."; return@TextButton }
                val base = existing ?: Expense(category = category.trim(), amount = a, date = date)
                vm.saveExpense(base.copy(category = category.trim(), amount = a, date = date, note = note.trim()))
                onDismiss()
            }) { Text("ذخیره", color = Red, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
