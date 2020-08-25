package com.wechantloup.upnpvideoplayer.data.content

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.wechantloup.upnpvideoplayer.data.dataholder.VideoElement

@Database(entities = [VideoElement::class], version = 1)
internal abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao

    @Dao
    interface VideoDao {
        @Query("SELECT * FROM video WHERE id LIKE :id LIMIT 1")
        suspend fun findById(id: Long): VideoElement?

        @Query("SELECT * FROM video ORDER BY \"date\" DESC")
        suspend fun all(): List<VideoElement>

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        suspend fun insert(element: VideoElement): Long?

        @Update
        suspend fun update(element: VideoElement): Int

        @Delete
        suspend fun delete(element: VideoElement): Int
    }
}