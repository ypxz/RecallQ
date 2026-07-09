package com.recalldeck.app.data.backup

import com.recalldeck.app.data.db.CardEntity
import com.recalldeck.app.data.db.CardState
import com.recalldeck.app.data.db.CardType
import com.recalldeck.app.data.db.CategoryEntity
import com.recalldeck.app.data.db.ReviewLogEntity
import com.recalldeck.app.data.db.SubjectEntity
import kotlinx.serialization.Serializable

const val BACKUP_SCHEMA_VERSION = 1

@Serializable
data class BackupFile(
    val schemaVersion: Int,
    val exportedAt: Long,
    val subjects: List<SubjectBackup>,
    val categories: List<CategoryBackup>,
    val cards: List<CardBackup>,
    val reviewLogs: List<ReviewLogBackup>,
)

@Serializable
data class SubjectBackup(
    val id: Long,
    val name: String,
    val colorHex: String,
    val position: Int,
    val createdAt: Long,
)

@Serializable
data class CategoryBackup(
    val id: Long,
    val subjectId: Long,
    val name: String,
    val position: Int,
    val createdAt: Long,
)

@Serializable
data class CardBackup(
    val id: Long,
    val categoryId: Long,
    val groupId: Long? = null,
    val type: String,
    val clozeIndex: Int? = null,
    val question: String,
    val answer: String,
    val hint: String? = null,
    val mnemonic: String? = null,
    val elaboration: String? = null,
    val imagePath: String? = null,
    val state: String,
    val dueAt: Long,
    val stability: Double,
    val difficulty: Double,
    val reps: Int,
    val lapses: Int,
    val lastReviewAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class ReviewLogBackup(
    val id: Long,
    val cardId: Long,
    val reviewedAt: Long,
    val rating: Int,
    val stateBefore: String,
    val elapsedDays: Double,
    val scheduledDays: Double,
    val durationMs: Long,
    val countedTowardSchedule: Boolean,
)

fun SubjectEntity.toBackup() = SubjectBackup(id, name, colorHex, position, createdAt)

fun SubjectBackup.toEntity() = SubjectEntity(id, name, colorHex, position, createdAt)

fun CategoryEntity.toBackup() = CategoryBackup(id, subjectId, name, position, createdAt)

fun CategoryBackup.toEntity() = CategoryEntity(id, subjectId, name, position, createdAt)

fun CardEntity.toBackup() = CardBackup(
    id = id,
    categoryId = categoryId,
    groupId = groupId,
    type = type.name,
    clozeIndex = clozeIndex,
    question = question,
    answer = answer,
    hint = hint,
    mnemonic = mnemonic,
    elaboration = elaboration,
    imagePath = imagePath,
    state = state.name,
    dueAt = dueAt,
    stability = stability,
    difficulty = difficulty,
    reps = reps,
    lapses = lapses,
    lastReviewAt = lastReviewAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun CardBackup.toEntity() = CardEntity(
    id = id,
    categoryId = categoryId,
    groupId = groupId,
    type = CardType.valueOf(type),
    clozeIndex = clozeIndex,
    question = question,
    answer = answer,
    hint = hint,
    mnemonic = mnemonic,
    elaboration = elaboration,
    imagePath = imagePath,
    state = CardState.valueOf(state),
    dueAt = dueAt,
    stability = stability,
    difficulty = difficulty,
    reps = reps,
    lapses = lapses,
    lastReviewAt = lastReviewAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ReviewLogEntity.toBackup() = ReviewLogBackup(
    id = id,
    cardId = cardId,
    reviewedAt = reviewedAt,
    rating = rating,
    stateBefore = stateBefore.name,
    elapsedDays = elapsedDays,
    scheduledDays = scheduledDays,
    durationMs = durationMs,
    countedTowardSchedule = countedTowardSchedule,
)

fun ReviewLogBackup.toEntity() = ReviewLogEntity(
    id = id,
    cardId = cardId,
    reviewedAt = reviewedAt,
    rating = rating,
    stateBefore = CardState.valueOf(stateBefore),
    elapsedDays = elapsedDays,
    scheduledDays = scheduledDays,
    durationMs = durationMs,
    countedTowardSchedule = countedTowardSchedule,
)
