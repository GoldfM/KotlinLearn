package com.example.myapplication

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_items ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todo_items WHERE uid = :uid")
    suspend fun getById(uid: String): TodoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TodoEntity)

    @Delete
    suspend fun delete(entity: TodoEntity)

    @Query("SELECT * FROM todo_items WHERE isSynced = 0")
    suspend fun getUnsynced(): List<TodoEntity>

    @Query("UPDATE todo_items SET isSynced = 1 WHERE uid = :uid")
    suspend fun markAsSynced(uid: String)
}