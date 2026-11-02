package com.roozyar.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VM(app: Application) : AndroidViewModel(app) {
    private val db = AppDb.get(app)
    private fun <T> kotlinx.coroutines.flow.Flow<List<T>>.st() =
        stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val tasks = db.tasks().observe().st()
    val habits = db.habits().observe().st()
    val logs = db.habits().observeLogs().st()
    val notes = db.notes().observe().st()

    fun saveTask(t: Task) = viewModelScope.launch {
        val id = if (t.id == 0L) db.tasks().insert(t) else { db.tasks().update(t); t.id }
        Reminders.sync(getApplication(), t.copy(id = id))
    }
    fun toggleTask(t: Task) = saveTask(t.copy(done = !t.done))
    fun deleteTask(t: Task) = viewModelScope.launch {
        Reminders.cancel(getApplication(), t.id.toInt()); db.tasks().delete(t)
    }

    fun saveHabit(h: Habit) = viewModelScope.launch {
        if (h.id == 0L) db.habits().insert(h) else db.habits().update(h)
    }
    fun deleteHabit(h: Habit) = viewModelScope.launch { db.habits().deleteLogs(h.id); db.habits().delete(h) }
    fun toggleHabit(h: Habit, day: Long, done: Boolean) = viewModelScope.launch {
        if (done) db.habits().removeLog(HabitLog(h.id, day)) else db.habits().addLog(HabitLog(h.id, day))
    }

    fun saveNote(n: Note) = viewModelScope.launch { db.notes().insert(n) }
    fun deleteNote(n: Note) = viewModelScope.launch { db.notes().delete(n) }
}
