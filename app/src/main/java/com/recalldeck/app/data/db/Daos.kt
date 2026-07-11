package com.recalldeck.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Insert
    suspend fun insert(subject: SubjectEntity): Long

    @Update
    suspend fun update(subject: SubjectEntity)

    @Delete
    suspend fun delete(subject: SubjectEntity)

    @Query("SELECT * FROM subjects ORDER BY position")
    fun observeAll(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects ORDER BY position")
    suspend fun getAll(): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getById(id: Long): SubjectEntity?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM subjects")
    suspend fun nextPosition(): Int

    @Insert
    suspend fun insertAll(subjects: List<SubjectEntity>): List<Long>

    @Query("DELETE FROM subjects")
    suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE subjectId = :subjectId ORDER BY position")
    fun observeBySubject(subjectId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY position")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM categories WHERE subjectId = :subjectId")
    suspend fun nextPosition(subjectId: Long): Int

    @Insert
    suspend fun insertAll(categories: List<CategoryEntity>): List<Long>

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface CardDao {
    @Insert
    suspend fun insert(card: CardEntity): Long

    @Insert
    suspend fun insertAll(cards: List<CardEntity>): List<Long>

    @Update
    suspend fun update(card: CardEntity)

    @Delete
    suspend fun delete(card: CardEntity)

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getById(id: Long): CardEntity?

    @Query("SELECT * FROM cards WHERE categoryId = :categoryId ORDER BY createdAt")
    fun observeByCategory(categoryId: Long): Flow<List<CardEntity>>

    @Query(
        """SELECT cards.* FROM cards
           JOIN categories ON categories.id = cards.categoryId
           WHERE categories.subjectId = :subjectId
           ORDER BY cards.createdAt""",
    )
    fun observeBySubject(subjectId: Long): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards ORDER BY createdAt")
    fun observeAll(): Flow<List<CardEntity>>

    @Query(
        """SELECT * FROM cards
           WHERE state IN ('LEARNING', 'REVIEW') AND dueAt <= :now""",
    )
    suspend fun getDue(now: Long): List<CardEntity>

    @Query(
        """SELECT COUNT(*) FROM cards
           WHERE state IN ('LEARNING', 'REVIEW') AND dueAt <= :now""",
    )
    fun observeDueCount(now: Long): Flow<Int>

    @Query("SELECT * FROM cards WHERE state = 'NEW' ORDER BY createdAt LIMIT :limit")
    suspend fun getNew(limit: Int): List<CardEntity>

    @Query("UPDATE cards SET state = :state, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun updateState(ids: List<Long>, state: CardState, updatedAt: Long)

    @Query("UPDATE cards SET categoryId = :categoryId, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun moveToCategory(ids: List<Long>, categoryId: Long, updatedAt: Long)

    @Query("DELETE FROM cards WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM cards WHERE categoryId = :categoryId ORDER BY createdAt")
    suspend fun getByCategory(categoryId: Long): List<CardEntity>

    @Query("SELECT * FROM cards WHERE groupId = :groupId")
    suspend fun getByGroup(groupId: Long): List<CardEntity>

    @Query("SELECT COALESCE(MAX(groupId), 0) + 1 FROM cards")
    suspend fun nextGroupId(): Long

    @Query("SELECT * FROM cards ORDER BY createdAt")
    suspend fun getAll(): List<CardEntity>

    @Query(
        """SELECT cards.* FROM cards
           JOIN categories ON categories.id = cards.categoryId
           WHERE categories.subjectId = :subjectId
           ORDER BY cards.createdAt""",
    )
    suspend fun getBySubject(subjectId: Long): List<CardEntity>

    @Query(
        """SELECT COUNT(*) FROM cards
           WHERE state IN ('LEARNING', 'REVIEW') AND dueAt <= :now""",
    )
    suspend fun getDueCount(now: Long): Int

    @Query("DELETE FROM cards")
    suspend fun deleteAll()
}

@Dao
interface ReviewLogDao {
    @Insert
    suspend fun insert(log: ReviewLogEntity): Long

    @Query("DELETE FROM review_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM review_logs WHERE cardId = :cardId ORDER BY reviewedAt")
    suspend fun getByCard(cardId: Long): List<ReviewLogEntity>

    @Query("SELECT * FROM review_logs WHERE reviewedAt >= :since ORDER BY reviewedAt")
    suspend fun getSince(since: Long): List<ReviewLogEntity>

    @Query("SELECT * FROM review_logs ORDER BY reviewedAt")
    suspend fun getAll(): List<ReviewLogEntity>

    @Query(
        """SELECT cardId, rating FROM (
               SELECT cardId, rating, MAX(reviewedAt) FROM review_logs
               WHERE countedTowardSchedule = 1 GROUP BY cardId
           )""",
    )
    fun observeLastRatings(): Flow<List<CardLastRating>>

    @Insert
    suspend fun insertAll(logs: List<ReviewLogEntity>): List<Long>

    @Query("DELETE FROM review_logs")
    suspend fun deleteAll()
}
