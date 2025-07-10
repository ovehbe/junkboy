package com.ovehbe.junkboy.database

import androidx.room.TypeConverter
import java.util.Date

class Converters {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromMessageCategory(category: MessageCategory): String {
        return category.name
    }
    
    @TypeConverter
    fun toMessageCategory(categoryString: String): MessageCategory {
        return MessageCategory.valueOf(categoryString)
    }
    
    @TypeConverter
    fun fromFilterType(filterType: FilterType): String {
        return filterType.name
    }
    
    @TypeConverter
    fun toFilterType(filterTypeString: String): FilterType {
        return FilterType.valueOf(filterTypeString)
    }
} 