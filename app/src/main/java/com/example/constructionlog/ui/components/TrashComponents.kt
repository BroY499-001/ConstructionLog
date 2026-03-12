package com.constructionlog.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.constructionlog.app.data.LogWithImages

@Composable
fun TrashList(
    items: List<LogWithImages>,
    onRestore: (Long) -> Unit,
    onDeleteForever: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.DeleteSweep,
            title = "回收站为空",
            description = "删除后的日志会在这里保留 30 天"
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.log.id }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(formatDate(item.log.date), style = MaterialTheme.typography.titleMedium)
                    Text(item.log.location, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        item.log.content,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onRestore(item.log.id) }) {
                            Icon(Icons.Rounded.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("恢复")
                        }
                        OutlinedButton(onClick = { onDeleteForever(item.log.id) }) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("彻底删除")
                        }
                    }
                }
            }
        }
    }
}
