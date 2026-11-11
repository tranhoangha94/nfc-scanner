package com.scannfc

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.scannfc.ui.ScanNfcTheme

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var storage: Storage

    private var uiState by mutableStateOf(ScanUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storage = Storage(this)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        uiState = uiState.copy(
            nfcSupported = nfcAdapter != null,
            logs = storage.getLogs()
        )

        setContent {
            ScanNfcTheme {
                ScanScreen(
                    state = uiState,
                    onRename = { uid, name ->
                        storage.setName(uid, name)
                        // đồng bộ tên cho các log cùng UID để lịch sử hiển thị nhất quán
                        uiState = uiState.copy(
                            lastName = storage.getName(uid),
                            logs = storage.getLogs()
                        )
                    },
                    onClearLogs = {
                        storage.clearLogs()
                        uiState = uiState.copy(logs = emptyList())
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        uiState = uiState.copy(nfcEnabled = nfcAdapter?.isEnabled == true)
        enableReader()
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    private fun enableReader() {
        val adapter = nfcAdapter ?: return
        if (!adapter.isEnabled) return
        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
        adapter.enableReaderMode(this, { tag -> onTagScanned(tag) }, flags, null)
    }

    private fun onTagScanned(tag: Tag) {
        val card = NfcReader.read(tag)
        val entry = storage.addScan(card)
        runOnUiThread {
            uiState = uiState.copy(
                lastCard = card,
                lastEntry = entry,
                lastName = storage.getName(card.uid),
                logs = storage.getLogs()
            )
        }
    }
}

data class ScanUiState(
    val nfcSupported: Boolean = true,
    val nfcEnabled: Boolean = true,
    val lastCard: CardInfo? = null,
    val lastEntry: LogEntry? = null,
    val lastName: String? = null,
    val logs: List<LogEntry> = emptyList()
)
