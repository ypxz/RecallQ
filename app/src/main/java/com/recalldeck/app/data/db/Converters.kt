package com.recalldeck.app.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun cardTypeToString(value: CardType): String = value.name

    @TypeConverter
    fun stringToCardType(value: String): CardType = CardType.valueOf(value)

    @TypeConverter
    fun cardStateToString(value: CardState): String = value.name

    @TypeConverter
    fun stringToCardState(value: String): CardState = CardState.valueOf(value)
}
