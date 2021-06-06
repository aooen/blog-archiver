package com.aooen.blog_archiver.database

import android.content.Context
import androidx.room.*
import java.util.*

const val DB_NAME = "blog.db"

@androidx.room.Database(entities = [Blog::class, Article::class], version = 1, exportSchema = false)
@TypeConverters(Database.Converters::class)
abstract class Database : RoomDatabase() {
    abstract fun blogDao(): BlogDao

    companion object {
        @Volatile private var instance: Database? = null

        @JvmStatic fun getInstance(context: Context): Database =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, Database::class.java, DB_NAME).allowMainThreadQueries().build().also {
                    instance = it
                }
            }
    }

    class Converters {
        @TypeConverter
        fun fromTimestamp(value: Long?): Date? {
            return value?.let { Date(it) }
        }

        @TypeConverter
        fun dateToTimestamp(date: Date?): Long? {
            return date?.time
        }
    }
}