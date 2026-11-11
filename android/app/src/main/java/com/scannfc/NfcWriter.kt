package com.scannfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.NfcA

/** Kết quả ghi thẻ. */
data class WriteResult(val ok: Boolean, val message: String)

/**
 * Ghi UID (block 0) sang thẻ "magic" Gen2 / CUID bằng MifareClassic API.
 * - Chỉ hoạt động với thẻ magic cho phép ghi block 0 (Gen2/CUID).
 * - Thẻ MIFARE Classic thật (block 0 chỉ đọc) và thẻ Gen1a (cần lệnh backdoor)
 *   sẽ KHÔNG ghi được qua đường này.
 */
object NfcWriter {

    // Các key mặc định hay gặp để auth sector 0.
    private val DEFAULT_KEYS: List<ByteArray> = listOf(
        byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
        byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
        byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte()),
        byteArrayOf(0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte())
    )

    fun fail(msg: String) = WriteResult(false, msg)

    /**
     * Lấy 16 byte block 0 từ thẻ nguồn.
     * @return Pair(block0, đọcTừThẻ). Nếu không auth được, dựng tạm từ UID + SAK/ATQA
     *         (đủ để clone UID; 8 byte nhà sản xuất để 0).
     */
    fun sourceBlock0(tag: Tag): Pair<ByteArray?, Boolean> {
        // thử đọc thật block 0
        MifareClassic.get(tag)?.let { mfc ->
            try {
                mfc.connect()
                for (key in DEFAULT_KEYS) {
                    if (mfc.authenticateSectorWithKeyA(0, key)) {
                        val b0 = mfc.readBlock(0)
                        if (b0 != null && b0.size == 16) return b0.copyOf() to true
                    }
                }
            } catch (_: Exception) {
            } finally {
                try { mfc.close() } catch (_: Exception) {}
            }
        }
        // dựng tạm từ UID
        return buildBlock0(tag)?.let { it to false } ?: (null to false)
    }

    private fun buildBlock0(tag: Tag): ByteArray? {
        val uid = tag.id ?: return null
        val a = NfcA.get(tag)
        val sak = (a?.sak ?: 0x08).toByte()
        val atqa = a?.atqa ?: byteArrayOf(0x04, 0x00)
        val b = ByteArray(16)
        if (uid.size == 4) {
            System.arraycopy(uid, 0, b, 0, 4)
            // BCC = XOR 4 byte UID
            b[4] = (uid[0].toInt() xor uid[1].toInt() xor uid[2].toInt() xor uid[3].toInt()).toByte()
            b[5] = sak
            b[6] = atqa.getOrElse(0) { 0x04 }
            b[7] = atqa.getOrElse(1) { 0x00 }
            // b[8..15] = 0 (byte nhà sản xuất)
        } else {
            // UID 7 byte: bố cục block 0 khác, chép tạm (có thể không khớp hoàn toàn)
            System.arraycopy(uid, 0, b, 0, minOf(uid.size, 16))
        }
        return b
    }

    /** Ghi 16 byte block 0 sang thẻ magic đang áp. */
    fun writeBlock0(tag: Tag, block0: ByteArray): WriteResult {
        if (block0.size != 16) return fail("Dữ liệu block 0 không hợp lệ")
        val mfc = MifareClassic.get(tag) ?: return fail("Thẻ đích không phải MIFARE Classic")
        return try {
            mfc.connect()
            var authed = false
            for (key in DEFAULT_KEYS) {
                if (mfc.authenticateSectorWithKeyA(0, key)) { authed = true; break }
            }
            if (!authed) return fail("Không auth được sector 0 của thẻ đích (key không mặc định)")
            mfc.writeBlock(0, block0)
            // đọc lại kiểm tra (UID mới có thể làm thao tác sau lỗi, nên bọc try riêng)
            val verified = try {
                val back = mfc.readBlock(0)
                back != null && back.size == 16 && back.copyOfRange(0, 4).contentEquals(block0.copyOfRange(0, 4))
            } catch (_: Exception) { false }
            if (verified) WriteResult(true, "Ghi UID thành công ✔")
            else WriteResult(true, "Đã gửi lệnh ghi (không xác nhận lại được — thử quẹt kiểm tra)")
        } catch (e: Exception) {
            fail("Ghi thất bại: ${e.message ?: "thẻ không cho ghi block 0 (không phải magic Gen2/CUID?)"}")
        } finally {
            try { mfc.close() } catch (_: Exception) {}
        }
    }
}
