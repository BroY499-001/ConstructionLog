package com.example.constructionlog.data

import androidx.room.Embedded
import androidx.room.Relation

data class LogWithImages(
    @Embedded val log: ConstructionLogEntity,
    @Relation(parentColumn = "id", entityColumn = "logId")
    val images: List<LogImageEntity>
)
