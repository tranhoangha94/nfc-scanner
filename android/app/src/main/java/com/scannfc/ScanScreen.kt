package com.scannfc

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppScreen(
    state: ScanUiState,
    onNavigate: (Screen) -> Unit,
    onOpenCard: (String) -> Unit,
    onSaveCurrentCard: (String) -> Unit,
    onDeleteCard: (String) -> Unit,
    onStartClone: (String) -> Unit,
    onCancelClone: () -> Unit,
    onClearLogs: () -> Unit
) {
    if (!state.nfcSupported) {
        Scaffold { p ->
            Column(Modifier.padding(p).padding(24.dp)) {
                Text("Thiết bị không hỗ trợ NFC.", style = MaterialTheme.typography.titleMedium)
            }
        }
        return
    }

    when (state.screen) {
        Screen.HOME -> HomeScreen(
            state,
            onAdd = { onNavigate(Screen.ADD) },
            onList = { onNavigate(Screen.LIST) },
            onAttend = { onNavigate(Screen.ATTEND) }
        )
        Screen.ADD -> AddCardScreen(state, onBack = { onNavigate(Screen.HOME) }, onSave = onSaveCurrentCard)
        Screen.LIST -> CardListScreen(
            state,
            onBack = { onNavigate(Screen.HOME) },
            onOpen = onOpenCard,
            onDelete = onDeleteCard,
            onAdd = { onNavigate(Screen.ADD) }
        )
        Screen.DETAIL -> {
            val card = state.cards.find { it.uid == state.selectedUid }
            if (card == null) {
                CardListScreen(state, { onNavigate(Screen.HOME) }, onOpenCard, onDeleteCard, { onNavigate(Screen.ADD) })
            } else {
                VirtualCardScreen(card, state, onBack = { onNavigate(Screen.LIST) }, onStartClone, onCancelClone)
            }
        }
        Screen.ATTEND -> AttendanceScreen(state, onBack = { onNavigate(Screen.HOME) }, onClearLogs = onClearLogs)
    }
}

/* ---------------- HOME ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    state: ScanUiState,
    onAdd: () -> Unit,
    onList: () -> Unit,
    onAttend: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Scan NFC") }) }) { p ->
        Column(
            Modifier
                .padding(p)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📇", fontSize = 64.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Quản lý thẻ NFC",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (!state.nfcEnabled) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "NFC đang tắt — hãy bật NFC trong Cài đặt.",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(40.dp))

            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("➕   Thêm thẻ mới", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(14.dp))
            FilledTonalButton(onClick = onList, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("📋   Danh sách thẻ (${state.cards.size})", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(14.dp))
            FilledTonalButton(onClick = onAttend, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("🕑   Chấm công Vào/Ra", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/* ---------------- ADD (quét thẻ mới) ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCardScreen(
    state: ScanUiState,
    onBack: () -> Unit,
    onSave: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thêm thẻ mới") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←", fontSize = 22.sp) } }
            )
        }
    ) { p ->
        Column(
            Modifier
                .padding(p)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            val card = state.lastCard
            if (card == null) {
                Column(
                    Modifier.fillMaxWidth().padding(top = 56.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📡", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Đưa thẻ vào mặt sau điện thoại để quét",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Đang chờ…", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                var name by remember(card.uid) { mutableStateOf("") }
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Đã đọc được thẻ ✔", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(
                            card.type,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        card.details.forEach { (l, v) -> DetailRow(l, v) }
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Đặt tên thẻ") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { onSave(name) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                            Text("💾  Lưu vào danh sách")
                        }
                    }
                }
            }
        }
    }
}

/* ---------------- LIST (danh sách thẻ) ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardListScreen(
    state: ScanUiState,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Danh sách thẻ") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←", fontSize = 22.sp) } }
            )
        }
    ) { p ->
        Column(Modifier.padding(p).padding(16.dp).fillMaxSize()) {
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Text("➕  Thêm thẻ mới")
            }
            Spacer(Modifier.height(12.dp))

            if (state.cards.isEmpty()) {
                Text(
                    "Chưa có thẻ nào. Bấm \"Thêm thẻ mới\" để quét và lưu thẻ.",
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.cards) { c ->
                        ListItem(
                            modifier = Modifier.clickable { onOpen(c.uid) },
                            headlineContent = { Text(c.name, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("${c.uid} • ${c.type}") },
                            trailingContent = {
                                TextButton(onClick = { onDelete(c.uid) }) { Text("Xóa") }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/* ---------------- DETAIL (thẻ ảo) ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VirtualCardScreen(
    card: SavedCard,
    state: ScanUiState,
    onBack: () -> Unit,
    onStartClone: (String) -> Unit,
    onCancelClone: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(card.name) },
                navigationIcon = { IconButton(onClick = onBack) { Text("←", fontSize = 22.sp) } }
            )
        }
    ) { p ->
        Column(
            Modifier
                .padding(p)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // "Thẻ ảo" — hiển thị như một tấm thẻ
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "THẺ ẢO",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        card.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "UID",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        card.uid,
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        card.type,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Mã để quét", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Giơ mã QR/vạch này cho máy quét có camera. Cổng CHẠM thẻ (NFC) " +
                    "cần app Emula để điện thoại phát sóng.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(12.dp))
            QrBarcodeView(card.uid.replace(":", ""))

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Text("Clone sang thẻ magic", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Chép UID sang thẻ magic Gen2/CUID trắng (áp thẻ magic sau khi bấm).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(10.dp))
            if (!state.cloneMode) {
                Button(onClick = { onStartClone(card.uid) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ghi UID sang thẻ magic")
                }
            } else {
                Text(
                    "⏳ Áp THẺ MAGIC trắng vào mặt sau điện thoại…",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onCancelClone, modifier = Modifier.fillMaxWidth()) { Text("Hủy") }
            }
            state.cloneResult?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    it,
                    fontWeight = FontWeight.SemiBold,
                    color = if (state.cloneSuccess) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/* ---------------- ATTEND (chấm công Vào/Ra) ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttendanceScreen(
    state: ScanUiState,
    onBack: () -> Unit,
    onClearLogs: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chấm công Vào/Ra") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←", fontSize = 22.sp) } }
            )
        }
    ) { p ->
        Column(Modifier.padding(p).padding(16.dp).fillMaxSize()) {
            val entry = state.lastEntry
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    if (entry == null) {
                        Text("Chạm thẻ để chấm công", style = MaterialTheme.typography.titleMedium)
                        Text("Đang chờ…", color = MaterialTheme.colorScheme.outline)
                    } else {
                        Text(
                            if (entry.direction == "IN") "VÀO" else "RA",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (entry.direction == "IN") MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.tertiary
                        )
                        Text(entry.name ?: entry.uid, style = MaterialTheme.typography.titleMedium)
                        Text("🕑 ${entry.timeText()}", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Lịch sử", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (state.logs.isNotEmpty()) TextButton(onClick = onClearLogs) { Text("Xóa hết") }
            }
            if (state.logs.isEmpty()) {
                Text("Chưa có lần chấm công nào.", color = MaterialTheme.colorScheme.outline)
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.logs) { log ->
                        ListItem(
                            headlineContent = { Text(log.name ?: log.uid) },
                            supportingContent = { Text(log.timeText()) },
                            trailingContent = {
                                Text(
                                    if (log.direction == "IN") "VÀO" else "RA",
                                    fontWeight = FontWeight.Bold,
                                    color = if (log.direction == "IN") MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.tertiary
                                )
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/* ---------------- Dùng chung ---------------- */

@Composable
private fun QrBarcodeView(content: String) {
    val qr = remember(content) { BarcodeGen.qr(content) }
    val barcode = remember(content) { BarcodeGen.code128(content) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        qr?.let {
            Image(it.asImageBitmap(), "QR", modifier = Modifier.size(200.dp))
            Spacer(Modifier.height(12.dp))
        }
        barcode?.let {
            Image(it.asImageBitmap(), "Barcode", modifier = Modifier.fillMaxWidth().height(90.dp))
            Spacer(Modifier.height(4.dp))
            Text(content, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
