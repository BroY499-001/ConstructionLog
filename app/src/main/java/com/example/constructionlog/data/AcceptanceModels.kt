package com.constructionlog.app.data

import androidx.room.Embedded
import androidx.room.Relation

data class AcceptanceFormWithDetails(
    @Embedded val form: AcceptanceFormEntity,
    @Relation(parentColumn = "id", entityColumn = "formId")
    val items: List<AcceptanceItemEntity>,
    @Relation(parentColumn = "id", entityColumn = "formId")
    val materials: List<AcceptanceMaterialEntity>,
    @Relation(parentColumn = "id", entityColumn = "formId")
    val images: List<AcceptanceImageEntity>
)
