package com.roozyar.app

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, val note: String = "", val priority: Int = 1,
    val dateDay: Long, val timeMin: Int = -1, val done: Boolean = false
)
@Entity data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, val days: Int = 127
)
@Entity(primaryKeys = ["habitId", "day"]) data class HabitLog(val habitId: Long, val day: Long)
@Entity data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, val body: String, val category: String = ""
)

@Dao interface TaskDao {
    @Query("SELECT * FROM Task ORDER BY done, dateDay, timeMin") fun observe(): Flow<List<Task>>
    @Query("SELECT * FROM Task WHERE done = 0 AND timeMin >= 0") fun pending(): List<Task>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(t: Task): Long
    @Update suspend fun update(t: Task)
    @Delete suspend fun delete(t: Task)
}
@Dao interface HabitDao {
    @Query("SELECT * FROM Habit ORDER BY id") fun observe(): Flow<List<Habit>>
    @Query("SELECT * FROM HabitLog") fun observeLogs(): Flow<List<HabitLog>>
    @Insert suspend fun insert(h: Habit): Long
    @Update suspend fun update(h: Habit)
    @Delete suspend fun delete(h: Habit)
    @Query("DELETE FROM HabitLog WHERE habitId = :id") suspend fun deleteLogs(id: Long)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addLog(l: HabitLog)
    @Delete suspend fun removeLog(l: HabitLog)
}
@Dao interface NoteDao {
    @Query("SELECT * FROM Note ORDER BY id DESC") fun observe(): Flow<List<Note>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(n: Note): Long
    @Delete suspend fun delete(n: Note)
}

@Database(entities = [Task::class, Habit::class, HabitLog::class, Note::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun tasks(): TaskDao
    abstract fun habits(): HabitDao
    abstract fun notes(): NoteDao
    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(c: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "roozyar.db").build().also { inst = it }
        }
    }
}
