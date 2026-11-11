package com.scannfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV

/** Thông tin đọc được từ 1 thẻ NFC. */
data class CardInfo(
    val uid: String,
    val type: String,
    val details: List<Pair<String, String>>
)

object NfcReader {

    fun read(tag: Tag): CardInfo {
        val uid = tag.id.toHex()
        val techs = tag.techList.map { it.substringAfterLast('.') }

        val details = mutableListOf<Pair<String, String>>()
        details += "UID" to uid
        details += "Độ dài UID" to "${tag.id.size} byte"
        details += "Tần số" to "13.56 MHz (HF)"
        details += "Công nghệ" to techs.joinToString(", ")

        var type = "Không xác định"

        // NfcA: ATQA + SAK (không cần kết nối)
        NfcA.get(tag)?.let { a ->
            details += "ATQA" to a.atqa.toHex()
            details += "SAK" to "0x%02X".format(a.sak)
        }

        // MIFARE Classic
        MifareClassic.get(tag)?.let { mc ->
            type = when (mc.type) {
                MifareClassic.TYPE_CLASSIC -> "MIFARE Classic"
                MifareClassic.TYPE_PLUS -> "MIFARE Plus"
                MifareClassic.TYPE_PRO -> "MIFARE Pro"
                else -> "MIFARE Classic (?)"
            }
            details += "Dung lượng" to "${mc.size} byte"
            details += "Số sector" to "${mc.sectorCount}"
            details += "Số block" to "${mc.blockCount}"
            details += "Lưu ý" to "Dữ liệu mã hóa Crypto1 — chỉ đọc được nếu có key"
        }

        // MIFARE Ultralight / NTAG
        MifareUltralight.get(tag)?.let { mu ->
            type = when (mu.type) {
                MifareUltralight.TYPE_ULTRALIGHT -> "MIFARE Ultralight"
                MifareUltralight.TYPE_ULTRALIGHT_C -> "MIFARE Ultralight C"
                else -> "MIFARE Ultralight / NTAG"
            }
        }

        // ISO-DEP (thẻ thông minh / APDU)
        IsoDep.get(tag)?.let { iso ->
            if (type == "Không xác định") type = "ISO-DEP (ISO 14443-4)"
            iso.historicalBytes?.takeIf { it.isNotEmpty() }?.let {
                details += "Historical bytes" to it.toHex()
            }
            iso.hiLayerResponse?.takeIf { it.isNotEmpty() }?.let {
                details += "Hi-layer resp" to it.toHex()
            }
        }

        NfcB.get(tag)?.let { if (type == "Không xác định") type = "NfcB (ISO 14443-3B)" }
        NfcF.get(tag)?.let { if (type == "Không xác định") type = "FeliCa (NfcF)" }
        NfcV.get(tag)?.let { v ->
            if (type == "Không xác định") type = "ISO 15693 (NfcV)"
            details += "DSFID" to "0x%02X".format(v.dsfId)
            details += "RESP flags" to "0x%02X".format(v.responseFlags)
        }

        // NDEF (metadata, không đọc nội dung để tránh lỗi/chậm)
        Ndef.get(tag)?.let { ndef ->
            val rw = if (ndef.isWritable) "ghi được" else "chỉ đọc"
            details += "NDEF" to "${ndef.type}, tối đa ${ndef.maxSize} byte, $rw"
        }

        // Đoán theo độ dài UID nếu vẫn chưa rõ loại
        if (type == "Không xác định") {
            type = when (tag.id.size) {
                4 -> "UID 4 byte (có thể MIFARE Classic)"
                7 -> "UID 7 byte (có thể NTAG / Ultralight / DESFire)"
                else -> "Thẻ ${tag.id.size} byte"
            }
        }

        return CardInfo(uid = uid, type = type, details = details)
    }

    private fun ByteArray.toHex(): String =
        if (isEmpty()) "(trống)" else joinToString(":") { "%02X".format(it) }
}
