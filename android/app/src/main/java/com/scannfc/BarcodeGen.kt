package com.scannfc

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

/** Sinh ảnh mã QR / mã vạch Code128 từ một chuỗi (dùng thư viện ZXing). */
object BarcodeGen {

    fun qr(content: String, size: Int = 512): Bitmap? =
        encode(content, BarcodeFormat.QR_CODE, size, size)

    fun code128(content: String, width: Int = 900, height: Int = 280): Bitmap? =
        encode(content, BarcodeFormat.CODE_128, width, height)

    private fun encode(content: String, format: BarcodeFormat, w: Int, h: Int): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(EncodeHintType.MARGIN to 1)
            val matrix = MultiFormatWriter().encode(content, format, w, h, hints)
            val mw = matrix.width
            val mh = matrix.height
            val pixels = IntArray(mw * mh)
            for (y in 0 until mh) {
                val offset = y * mw
                for (x in 0 until mw) {
                    pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(mw, mh, Bitmap.Config.RGB_565).apply {
                setPixels(pixels, 0, mw, 0, 0, mw, mh)
            }
        } catch (e: Exception) {
            null
        }
    }
}
