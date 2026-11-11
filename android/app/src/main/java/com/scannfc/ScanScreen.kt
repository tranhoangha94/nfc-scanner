package com.scannfc

import androidx.compose.foundation.Image
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    state: ScanUiState,
    onRename: (uid: String, name: String) -> Unit,
    onClearLogs: () -> Unit,
    onStartClone: () -> Unit,
    onCancelClone: () -> Unit
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

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        BarcodeSection(uid = card.uid)

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        CloneSection(
                            state = state,
                            onStartClone = onStartClone,
                            onCancelClone = onCancelClone
                        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarcodeSection(uid: String) {
    var content by remember(uid) { mutableStateOf(uid.replace(":", "")) }
    val qr = remember(content) { BarcodeGen.qr(content) }
    val barcode = remember(content) { BarcodeGen.code128(content) }

    Text(
        "Mã vạch / QR",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    Text(
        "Chỉ dùng được nếu máy có camera / khe quét mã vạch. " +
            "Máy CHẠM thẻ (NFC) KHÔNG đọc được mã trên màn hình.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = content,
        onValueChange = { content = it },
        label = { Text("Nội dung mã (mặc định = UID)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        qr?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "QR code",
                modifier = Modifier.size(200.dp)
            )
            Spacer(Modifier.height(12.dp))
        }
        barcode?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Barcode",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                content,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
        if (qr == null && barcode == null) {
            Text("(Nhập nội dung để tạo mã)", color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun CloneSection(
    state: ScanUiState,
    onStartClone: () -> Unit,
    onCancelClone: () -> Unit
) {
    Text(
        "Clone UID sang thẻ magic",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    Text(
        "Chép UID thẻ vừa quét sang một thẻ \"magic\" Gen2/CUID trắng. " +
            "Chỉ ghi được lên thẻ magic (không ghi lên thẻ thật hoặc Gen1a).",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline
    )
    Spacer(Modifier.height(4.dp))
    Text(
        if (state.cloneSourceFromCard) "Nguồn: đọc đủ block 0 từ thẻ ✔"
        else "Nguồn: dựng từ UID (8 byte nhà SX = 0) — đủ cho máy đọc UID",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline
    )
    Spacer(Modifier.height(10.dp))

    if (!state.cloneMode) {
        Button(
            onClick = onStartClone,
            enabled = state.canClone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ghi UID này sang thẻ magic")
        }
    } else {
        Text(
            "⏳ Giờ áp THẺ MAGIC trắng vào mặt sau điện thoại…",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onCancelClone, modifier = Modifier.fillMaxWidth()) {
            Text("Hủy")
        }
    }

    state.cloneResult?.let { msg ->
        Spacer(Modifier.height(8.dp))
        Text(
            msg,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (state.cloneSuccess) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error
        )
    }
}
