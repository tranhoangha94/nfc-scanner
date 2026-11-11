package com.scannfc

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.scannfc.ui.ScanNfcTheme

enum class Screen { HOME, ADD, LIST, DETAIL, ATTEND }

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var storage: Storage

    private var cloneBlock0: ByteArray? = null
    private var pendingBlock0Hex: String? = null
    private var lastScanUid: String? = null
    private var lastScanAt = 0L

    private var uiState by mutableStateOf(ScanUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storage = Storage(this)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        uiState = uiState.copy(
            nfcSupported = nfcAdapter != null,
            cards = storage.getCards(),
            logs = storage.getLogs()
        )

        setContent {
            ScanNfcTheme {
                AppScreen(
                    state = uiState,
                    onNavigate = { screen -> navigate(screen) },
                    onOpenCard = { uid ->
                        uiState = uiState.copy(
                            screen = Screen.DETAIL,
                            selectedUid = uid,
                            cloneMode = false,
                            cloneResult = null
                        )
                    },
                    onSaveCurrentCard = { name -> saveCurrentCard(name) },
                    onDeleteCard = { uid -> deleteCard(uid) },
                    onStartClone = { uid -> startClone(uid) },
                    onCancelClone = { uiState = uiState.copy(cloneMode = false) },
                    onClearLogs = {
                        storage.clearLogs()
                        uiState = uiState.copy(logs = emptyList())
                    }
                )
            }
        }
    }

    private fun navigate(screen: Screen) {
        val fresh = screen == Screen.ADD || screen == Screen.ATTEND
        uiState = uiState.copy(
            screen = screen,
            lastCard = if (fresh) null else uiState.lastCard,
            lastEntry = if (fresh) null else uiState.lastEntry,
            cloneMode = false,
            cloneResult = null,
            cards = storage.getCards(),
            logs = storage.getLogs()
        )
    }

    private fun saveCurrentCard(name: String) {
        val card = uiState.lastCard ?: return
        storage.saveCard(
            SavedCard(
                uid = card.uid,
                name = name.ifBlank { "(chưa đặt tên)" },
                type = card.type,
                block0Hex = pendingBlock0Hex,
                savedAt = System.currentTimeMillis()
            )
        )
        Toast.makeText(this, "Đã lưu thẻ ✔", Toast.LENGTH_SHORT).show()
        pendingBlock0Hex = null
        uiState = uiState.copy(screen = Screen.LIST, lastCard = null, cards = storage.getCards())
    }

    private fun deleteCard(uid: String) {
        storage.deleteCard(uid)
        uiState = uiState.copy(
            cards = storage.getCards(),
            screen = if (uiState.selectedUid == uid) Screen.LIST else uiState.screen
        )
    }

    private fun startClone(uid: String) {
        val c = storage.getCard(uid)
        cloneBlock0 = c?.block0Hex?.let { hexToBytes(it) }
        uiState = if (cloneBlock0 == null) {
            uiState.copy(cloneMode = false, cloneResult = "Thẻ này chưa lưu dữ liệu block 0 để ghi.", cloneSuccess = false)
        } else {
            uiState.copy(cloneMode = true, cloneResult = null, cloneSuccess = false)
        }
    }

    override fun onResume() {
        super.onResume()
        uiState = uiState.copy(
            nfcEnabled = nfcAdapter?.isEnabled == true,
            cards = storage.getCards(),
            logs = storage.getLogs()
        )
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
        // Chế độ clone: thẻ đang áp là thẻ MAGIC đích -> ghi block 0
        if (uiState.cloneMode) {
            val b0 = cloneBlock0
            val res = if (b0 == null) NfcWriter.fail("Chưa có dữ liệu UID để ghi")
            else NfcWriter.writeBlock0(tag, b0)
            runOnUiThread {
                uiState = uiState.copy(cloneMode = false, cloneResult = res.message, cloneSuccess = res.ok)
            }
            return
        }

        when (uiState.screen) {
            Screen.ADD -> {
                val card = NfcReader.read(tag)
                val (b0, fromCard) = NfcWriter.sourceBlock0(tag)
                pendingBlock0Hex = b0?.let { toHex(it) }
                runOnUiThread {
                    uiState = uiState.copy(lastCard = card, cloneSourceFromCard = fromCard)
                }
            }
            Screen.ATTEND -> {
                val card = NfcReader.read(tag)
                val now = System.currentTimeMillis()
                if (card.uid == lastScanUid && now - lastScanAt < 2000L) return
                lastScanUid = card.uid
                lastScanAt = now
                val entry = storage.addAttendance(card.uid, card.type)
                runOnUiThread {
                    uiState = uiState.copy(lastCard = card, lastEntry = entry, logs = storage.getLogs())
                }
            }
            else -> {
                // HOME / LIST / DETAIL: bỏ qua lần quẹt (tránh ghi nhầm)
            }
        }
    }

    private fun toHex(b: ByteArray): String = b.joinToString("") { "%02X".format(it) }

    private fun hexToBytes(s: String): ByteArray? = try {
        val clean = s.replace(" ", "").replace(":", "")
        if (clean.isEmpty() || clean.length % 2 != 0) null
        else ByteArray(clean.length / 2) { clean.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    } catch (e: Exception) {
        null
    }
}

data class ScanUiState(
    val nfcSupported: Boolean = true,
    val nfcEnabled: Boolean = true,
    val screen: Screen = Screen.HOME,
    val lastCard: CardInfo? = null,
    val cards: List<SavedCard> = emptyList(),
    val selectedUid: String? = null,
    val cloneSourceFromCard: Boolean = false,
    val cloneMode: Boolean = false,
    val cloneResult: String? = null,
    val cloneSuccess: Boolean = false,
    val lastEntry: LogEntry? = null,
    val logs: List<LogEntry> = emptyList()
)
