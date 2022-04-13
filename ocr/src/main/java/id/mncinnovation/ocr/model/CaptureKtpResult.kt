package id.mncinnovation.ocr.model

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import id.mncinnovation.identification.core.utils.BitmapUtils
import kotlinx.parcelize.Parcelize

@Parcelize
data class CaptureKtpResult(val isSuccess: Boolean,
                            val errorMessage: String?,
                            val imageUri: Uri,
                            val ktp: Ktp
): Parcelable{
    fun getBitmapImage(context: Context): Bitmap? {
        return BitmapUtils.getBitmapFromContentUri(context.contentResolver, imageUri)
    }
}