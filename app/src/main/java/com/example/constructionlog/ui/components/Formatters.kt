package com.constructionlog.app.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
internal val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年MM月")

fun formatDate(time: Long): String = dayFormatter.format(
    Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate()
)

fun parseDate(dateString: String): Long? = runCatching {
    dayFormatter.parse(dateString, java.time.LocalDate::from)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}.getOrNull()

private val stageKeywords: List<Pair<String, List<String>>> = listOf(
    "开工准备" to listOf("量房", "设计", "开工", "交底", "放线", "拆旧"),
    "拆改阶段" to listOf("拆墙", "砌墙", "铲墙", "清运", "开槽", "封窗"),
    "水电阶段" to listOf("水管", "电线", "布线", "强电", "弱电", "打压", "线盒"),
    "泥瓦阶段" to listOf("防水", "闭水", "贴砖", "找平", "地漏", "美缝"),
    "木工阶段" to listOf("吊顶", "龙骨", "石膏板", "柜体", "打柜", "门套"),
    "油工阶段" to listOf("刮腻子", "打磨", "底漆", "面漆", "乳胶漆", "墙漆"),
    "安装阶段" to listOf("橱柜", "洁具", "地板", "木门", "灯具", "开关", "插座", "空调"),
    "收尾验收" to listOf("保洁", "验收", "整改", "软装", "入住", "收尾")
)

fun estimateStageFromContent(content: String): String {
    val text = content.trim()
    if (text.isEmpty()) return "施工记录"

    var bestIndex = -1
    var bestScore = 0
    stageKeywords.forEachIndexed { index, (_, keywords) ->
        val score = keywords.count { keyword -> text.contains(keyword, ignoreCase = true) }
        if (score > bestScore || (score == bestScore && score > 0 && index > bestIndex)) {
            bestIndex = index
            bestScore = score
        }
    }
    return if (bestIndex >= 0) stageKeywords[bestIndex].first else "施工记录"
}
