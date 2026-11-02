package com.roozyar.app

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

fun streak(h: Habit, logs: Set<Long>, today: Long): Int {
    var d = today; var s = 0
    if (d !in logs) d--
    var guard = 0
    while (guard++ < 400) {
        if (((h.days shr dayIdx(d)) and 1) == 1) { if (d in logs) s++ else break }
        d--
    }
    return s
}

@Composable
fun RoozScreen(vm: VM, dark: Boolean, onDark: (Boolean) -> Unit, accent: Int, onAccent: (Int) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var today by remember { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    LaunchedEffect(Unit) { while (true) { today = LocalDate.now().toEpochDay(); delay(30_000) } }
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val habits by vm.habits.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val items = listOf("امروز" to Icons.Default.Home, "وظایف" to Icons.Default.Check,
        "عادت‌ها" to Icons.Default.Favorite, "یادداشت" to Icons.Default.Edit)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                items.forEachIndexed { i, (l, ic) ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i },
                        icon = { Icon(ic, null) }, label = { Text(l) })
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> TodayTab(vm, tasks, habits, logs, today, dark, onDark, accent, onAccent)
                1 -> TasksTab(vm, tasks, today)
                2 -> HabitsTab(vm, habits, logs, today)
                else -> NotesTab(vm, notes)
            }
        }
    }
}

@Composable
fun ConfirmDelete(what: String, onYes: () -> Unit, onNo: () -> Unit) = AlertDialog(
    onDismissRequest = onNo, title = { Text("حذف؟") },
    text = { Text("«$what» حذف شود؟ این کار قابل بازگشت نیست.") },
    confirmButton = { TextButton(onYes) { Text("حذف") } },
    dismissButton = { TextButton(onNo) { Text("انصراف") } }
)

@Composable
fun Card2(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = Surface(
    modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp
) { Column(Modifier.padding(16.dp), content = content) }

@Composable
fun TodayTab(
    vm: VM, tasks: List<Task>, habits: List<Habit>, logs: List<HabitLog>, today: Long,
    dark: Boolean, onDark: (Boolean) -> Unit, accent: Int, onAccent: (Int) -> Unit
) {
    var clock by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) { while (true) { clock = LocalTime.now(); delay(1000) } }
    val todayTasks = tasks.filter { it.dateDay == today }
    val logSet = logs.filter { it.day == today }.map { it.habitId }.toSet()
    val todayHabits = habits.filter { ((it.days shr dayIdx(today)) and 1) == 1 }
    val total = todayTasks.size + todayHabits.size
    val done = todayTasks.count { it.done } + todayHabits.count { it.id in logSet }
    val progress = if (total == 0) 0f else done.toFloat() / total
    val overdue = tasks.filter { !it.done && it.dateDay < today }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card2 {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(fullDate(today), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("%02d:%02d:%02d".format(clock.hour, clock.minute, clock.second).fa(),
                            fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton({ onDark(!dark) }) { Icon(if (dark) Icons.Default.Favorite else Icons.Default.Star, "تم") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                    ACCENTS.forEachIndexed { i, c ->
                        Box(Modifier.size(if (i == accent) 32.dp else 26.dp).clip(CircleShape).background(c).clickable { onAccent(i) })
                    }
                }
            }
        }
        item {
            Card2 {
                Text("پیشرفت امروز: ${(progress * 100).toInt().fa()}٪  (${done.fa()} از ${total.fa()})", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
            }
        }
        if (overdue.isNotEmpty()) item {
            Card2 { Text("${overdue.size.fa()} کار انجام‌نشده از روزهای قبل دارید", color = MaterialTheme.colorScheme.error) }
        }
        item { Text("کارهای امروز", fontWeight = FontWeight.Bold) }
        if (todayTasks.isEmpty()) item { Text("کاری برای امروز ثبت نشده. از تب «وظایف» اضافه کنید.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(todayTasks, key = { "t${it.id}" }) { t ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(t.done, { vm.toggleTask(t) })
                Text(t.title + if (t.timeMin >= 0) "  (${hm(t.timeMin)})" else "")
            }
        }
        item { Text("عادت‌های امروز", fontWeight = FontWeight.Bold) }
        if (todayHabits.isEmpty()) item { Text("عادتی برای امروز فعال نیست.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(todayHabits, key = { "h${it.id}" }) { h ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(h.id in logSet, { vm.toggleHabit(h, today, h.id in logSet) })
                Text(h.title)
            }
        }
    }
}

@Composable
fun TaskDialog(init: Task?, today: Long, onSave: (Task) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf(init?.title ?: "") }
    var note by remember { mutableStateOf(init?.note ?: "") }
    var pr by remember { mutableIntStateOf(init?.priority ?: 1) }
    var day by remember { mutableLongStateOf(init?.dateDay ?: today) }
    var time by remember { mutableIntStateOf(init?.timeMin ?: -1) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (init == null) "وظیفه جدید" else "ویرایش وظیفه") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("عنوان") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("توضیحات") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("کم", "متوسط", "زیاد").forEachIndexed { i, l -> FilterChip(pr == i, { pr = i }, { Text(l) }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(day == today, { day = today }, { Text("امروز") })
                    FilterChip(day == today + 1, { day = today + 1 }, { Text("فردا") })
                }
                OutlinedButton({
                    val t = if (time >= 0) time else 8 * 60
                    TimePickerDialog(ctx, { _, h, m -> time = h * 60 + m }, t / 60, t % 60, true).show()
                }) { Text(if (time < 0) "یادآور: بدون ساعت" else "یادآور: ${hm(time)}") }
                if (time >= 0) TextButton({ time = -1 }) { Text("حذف ساعت یادآور") }
            }
        },
        confirmButton = {
            TextButton({
                if (title.isNotBlank()) onSave(Task(init?.id ?: 0, title.trim(), note, pr, day, time, init?.done ?: false))
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onDismiss) { Text("انصراف") } }
    )
}

@Composable
fun TasksTab(vm: VM, tasks: List<Task>, today: Long) {
    var edit by remember { mutableStateOf<Task?>(null) }
    var adding by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf<Task?>(null) }
    Box(Modifier.fillMaxSize()) {
        if (tasks.isEmpty()) Text("هنوز وظیفه‌ای ندارید. با دکمه + شروع کنید.", Modifier.align(Alignment.Center))
        LazyColumn(contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tasks, key = { it.id }) { t ->
                Card2(Modifier.clickable { edit = t }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(t.done, { vm.toggleTask(t) })
                        Column(Modifier.weight(1f)) {
                            Text(t.title, fontWeight = FontWeight.Medium)
                            Text(shortDate(t.dateDay) + (if (t.timeMin >= 0) " • ${hm(t.timeMin)}" else "") +
                                " • " + listOf("کم", "متوسط", "زیاد")[t.priority],
                                fontSize = 12.sp, color = if (!t.done && t.dateDay < today) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton({ del = t }) { Icon(Icons.Default.Delete, "حذف") }
                    }
                }
            }
        }
        FloatingActionButton({ adding = true }, Modifier.align(Alignment.BottomStart).padding(20.dp)) { Icon(Icons.Default.Add, "افزودن") }
    }
    if (adding) TaskDialog(null, today, { vm.saveTask(it); adding = false }, { adding = false })
    edit?.let { e -> TaskDialog(e, today, { vm.saveTask(it); edit = null }, { edit = null }) }
    del?.let { d -> ConfirmDelete(d.title, { vm.deleteTask(d); del = null }, { del = null }) }
}

@Composable
fun HabitDialog(init: Habit?, onSave: (Habit) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(init?.title ?: "") }
    var days by remember { mutableIntStateOf(init?.days ?: 127) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (init == null) "عادت جدید" else "ویرایش عادت") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("عنوان") }, singleLine = true)
                Text("روزهای فعال")
                DAYS.chunked(4).forEachIndexed { r, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEachIndexed { c, n ->
                            val idx = r * 4 + c
                            FilterChip(((days shr idx) and 1) == 1, { days = days xor (1 shl idx) }, { Text(n, fontSize = 12.sp) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton({ if (title.isNotBlank() && days != 0) onSave(Habit(init?.id ?: 0, title.trim(), days)) }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onDismiss) { Text("انصراف") } }
    )
}

@Composable
fun HabitsTab(vm: VM, habits: List<Habit>, logs: List<HabitLog>, today: Long) {
    var edit by remember { mutableStateOf<Habit?>(null) }
    var adding by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf<Habit?>(null) }
    Box(Modifier.fillMaxSize()) {
        if (habits.isEmpty()) Text("هنوز عادتی ندارید. با دکمه + شروع کنید.", Modifier.align(Alignment.Center))
        LazyColumn(contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(habits, key = { it.id }) { h ->
                val set = logs.filter { it.habitId == h.id }.map { it.day }.toSet()
                val active = ((h.days shr dayIdx(today)) and 1) == 1
                val doneToday = today in set
                val last7 = (0..6).count { (today - it) in set }
                Card2(Modifier.clickable { edit = h }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(doneToday, { vm.toggleHabit(h, today, doneToday) }, enabled = active)
                        Column(Modifier.weight(1f)) {
                            Text(h.title, fontWeight = FontWeight.Medium)
                            Text("پیاپی: ${streak(h, set, today).fa()} روز • ۷ روز اخیر: ${last7.fa()}", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(DAYS.filterIndexed { i, _ -> ((h.days shr i) and 1) == 1 }.joinToString("، "), fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton({ del = h }) { Icon(Icons.Default.Delete, "حذف") }
                    }
                    LinearProgressIndicator(progress = { last7 / 7f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape))
                }
            }
        }
        FloatingActionButton({ adding = true }, Modifier.align(Alignment.BottomStart).padding(20.dp)) { Icon(Icons.Default.Add, "افزودن") }
    }
    if (adding) HabitDialog(null, { vm.saveHabit(it); adding = false }, { adding = false })
    edit?.let { e -> HabitDialog(e, { vm.saveHabit(it); edit = null }, { edit = null }) }
    del?.let { d -> ConfirmDelete(d.title, { vm.deleteHabit(d); del = null }, { del = null }) }
}

@Composable
fun NoteDialog(init: Note?, onSave: (Note) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(init?.title ?: "") }
    var body by remember { mutableStateOf(init?.body ?: "") }
    var cat by remember { mutableStateOf(init?.category ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (init == null) "یادداشت جدید" else "ویرایش یادداشت") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("عنوان") }, singleLine = true)
                OutlinedTextField(cat, { cat = it }, label = { Text("دسته (مثلاً گزارش کارآموزی)") }, singleLine = true)
                OutlinedTextField(body, { body = it }, label = { Text("متن") }, minLines = 4)
            }
        },
        confirmButton = {
            TextButton({ if (title.isNotBlank() || body.isNotBlank()) onSave(Note(init?.id ?: 0, title.trim(), body, cat.trim())) }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onDismiss) { Text("انصراف") } }
    )
}

@Composable
fun NotesTab(vm: VM, notes: List<Note>) {
    var q by remember { mutableStateOf("") }
    var edit by remember { mutableStateOf<Note?>(null) }
    var adding by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf<Note?>(null) }
    val shown = notes.filter { q.isBlank() || it.title.contains(q, true) || it.body.contains(q, true) || it.category.contains(q, true) }
    Box(Modifier.fillMaxSize()) {
        Column {
            OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 0.dp),
                label = { Text("جست‌وجو") }, singleLine = true)
            if (shown.isEmpty()) Text("یادداشتی پیدا نشد.", Modifier.padding(16.dp))
            LazyColumn(contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { n ->
                    Card2(Modifier.clickable { edit = n }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(n.title.ifBlank { "بدون عنوان" }, fontWeight = FontWeight.Medium)
                                if (n.category.isNotBlank()) Text(n.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Text(n.body, maxLines = 2, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton({ del = n }) { Icon(Icons.Default.Delete, "حذف") }
                        }
                    }
                }
            }
        }
        FloatingActionButton({ adding = true }, Modifier.align(Alignment.BottomStart).padding(20.dp)) { Icon(Icons.Default.Add, "افزودن") }
    }
    if (adding) NoteDialog(null, { vm.saveNote(it); adding = false }, { adding = false })
    edit?.let { e -> NoteDialog(e, { vm.saveNote(it); edit = null }, { edit = null }) }
    del?.let { d -> ConfirmDelete(d.title.ifBlank { "یادداشت" }, { vm.deleteNote(d); del = null }, { del = null }) }
}
