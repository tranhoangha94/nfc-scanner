package com.scannfc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    state: ScanUiState,
    onRename: (uid: String, name: String) -> Unit,
    onClearLogs: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Scan NFC") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            when {
                !state.nfcSupported -> {
                    Card { Text("Thiết bị không hỗ trợ NFC.", Modifier.padding(16.dp)) }
                    return@Column
                }
                !state.nfcEnabled -> {
                    Card {
                        Text(
                            "NFC đang tắt. Hãy bật NFC trong Cài đặt rồi mở lại app.",
                            Modifier.padding(16.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // Thẻ vừa quẹt
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    val card = state.lastCard
                    if (card == null) {
                        Text(
                            "Đưa thẻ lại gần mặt sau điện thoại",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Đang chờ quẹt…", color = MaterialTheme.colorScheme.outline)
                    } else {
                        val dir = state.lastEntry?.direction
                        Text(
                            if (dir == "IN") "VÀO" else "RA",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (dir == "IN") MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            state.lastName ?: "(Thẻ chưa đặt tên)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            card.type,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        state.lastEntry?.let {
                            Text(
                                "🕑 ${it.timeText()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))

                        // Chi tiết thông tin detect được
                        card.details.forEach { (label, value) ->
                            DetailRow(label, value)
                        }

                        Spacer(Modifier.height(12.dp))
                        RenameField(initial = state.lastName ?: "") { newName ->
                            onRename(card.uid, newName)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Lịch sử",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (state.logs.isNotEmpty()) {
                    TextButton(onClick = onClearLogs) { Text("Xóa hết") }
                }
            }

            LazyColumn(Modifier.fillMaxSize()) {
                items(state.logs) { log ->
                    ListItem(
                        headlineContent = { Text(log.name ?: log.uid) },
                        supportingContent = { Text("${log.type} • ${log.timeText()}") },
                        trailingContent = {
                            Text(
                                if (log.direction == "IN") "VÀO" else "RA",
                                color = if (log.direction == "IN") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenameField(initial: String, onSave: (String) -> Unit) {
    var text by remember(initial) { mutableStateOf(initial) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Đặt tên thẻ") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = { onSave(text) }, enabled = text.isNotBlank()) {
            Text("Lưu")
        }
    }
}
