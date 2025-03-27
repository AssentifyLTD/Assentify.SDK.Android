package com.assentify.sdk.ScanNFC

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.gemalto.jp2.JP2Decoder
import org.jnbis.WsqDecoder
import java.io.InputStream

object ImagesConstantsTypes {
    const val jp2 = "image/jp2"
    const val jpeg2000 = "image/jpeg2000"
    const val wsq = "#image/x-wsq"
}

object NfcImageUtil {

    fun decodeImage(mimeType: String, inputStream: InputStream?): Bitmap {
        return if (mimeType.equals(ImagesConstantsTypes.jp2, ignoreCase = true) || mimeType.equals(
                ImagesConstantsTypes.jpeg2000,
                ignoreCase = true
            )
        ) {
            JP2Decoder(inputStream).decode()
        } else if (mimeType.equals(ImagesConstantsTypes.wsq, ignoreCase = true)) {
            val wsqDecoder = WsqDecoder()
            val bitmap = wsqDecoder.decode(inputStream)
            val byteData = bitmap.pixels
            val intData = IntArray(byteData.size)
            for (j in byteData.indices) {
                intData[j] = 0xFF000000.toInt() or
                        (byteData[j].toInt() and 0xFF shl 16) or
                        (byteData[j].toInt() and 0xFF shl 8) or
                        (byteData[j].toInt() and 0xFF)
            }
            Bitmap.createBitmap(
                intData,
                0,
                bitmap.width,
                bitmap.width,
                bitmap.height,
                Bitmap.Config.ARGB_8888
            )
        } else {
            BitmapFactory.decodeStream(inputStream)
        }
    }
}
