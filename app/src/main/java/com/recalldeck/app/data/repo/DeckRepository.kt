package com.recalldeck.app.data.repo

import com.recalldeck.app.data.db.CardDao
import com.recalldeck.app.data.db.CardEntity
import com.recalldeck.app.data.db.CardState
import com.recalldeck.app.data.db.CategoryDao
import com.recalldeck.app.data.db.CategoryEntity
import com.recalldeck.app.data.db.SubjectDao
import com.recalldeck.app.data.db.SubjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for subjects, categories and cards (CRUD).
 */
class DeckRepository(
    private val subjectDao: SubjectDao,
    private val categoryDao: CategoryDao,
    private val cardDao: CardDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun observeSubjects(): Flow<List<SubjectEntity>> = subjectDao.observeAll()

    suspend fun getSubject(id: Long): SubjectEntity? = subjectDao.getById(id)

    suspend fun createSubject(name: String, colorHex: String): Long =
        subjectDao.insert(
            SubjectEntity(
                name = name,
                colorHex = colorHex,
                position = subjectDao.nextPosition(),
                createdAt = now(),
            ),
        )

    suspend fun renameSubject(subject: SubjectEntity, name: String) =
        subjectDao.update(subject.copy(name = name))

    suspend fun deleteSubject(subject: SubjectEntity) = subjectDao.delete(subject)

    fun observeCategories(subjectId: Long): Flow<List<CategoryEntity>> =
        categoryDao.observeBySubject(subjectId)

    suspend fun getCategory(id: Long): CategoryEntity? = categoryDao.getById(id)

    suspend fun getAllCategories(): List<CategoryEntity> = categoryDao.getAll()

    suspend fun createCategory(subjectId: Long, name: String): Long =
        categoryDao.insert(
            CategoryEntity(
                subjectId = subjectId,
                name = name,
                position = categoryDao.nextPosition(subjectId),
                createdAt = now(),
            ),
        )

    suspend fun renameCategory(category: CategoryEntity, name: String) =
        categoryDao.update(category.copy(name = name))

    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.delete(category)

    fun observeCards(categoryId: Long): Flow<List<CardEntity>> =
        cardDao.observeByCategory(categoryId)

    fun observeCardsBySubject(subjectId: Long): Flow<List<CardEntity>> =
        cardDao.observeBySubject(subjectId)

    fun observeAllCards(): Flow<List<CardEntity>> = cardDao.observeAll()

    fun observeDueCount(now: Long): Flow<Int> = cardDao.observeDueCount(now)

    suspend fun getCard(id: Long): CardEntity? = cardDao.getById(id)

    suspend fun createCard(card: CardEntity): Long = cardDao.insert(card)

    suspend fun createCards(cards: List<CardEntity>): List<Long> = cardDao.insertAll(cards)

    suspend fun updateCard(card: CardEntity) = cardDao.update(card.copy(updatedAt = now()))

    suspend fun deleteCards(ids: List<Long>) = cardDao.deleteByIds(ids)

    suspend fun moveCards(ids: List<Long>, categoryId: Long) =
        cardDao.moveToCategory(ids, categoryId, now())

    suspend fun setCardsState(ids: List<Long>, state: CardState) =
        cardDao.updateState(ids, state, now())

    suspend fun nextGroupId(): Long = cardDao.nextGroupId()
}
