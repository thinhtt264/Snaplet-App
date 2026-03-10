package com.thinh.snaplet.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.thinh.snaplet.utils.FileUtils.MAX_DIMENSION
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

object FileUtils {

    private const val JPEG_QUALITY = 95

    /**
     * Max dimension (long edge) before downscaling. Chosen to match feed display:
     * MediaPage shows post image in 400.dp height × full width (~1000–1200px × 1080–1440px).
     * 1920px is enough for sharp 1:1 on typical phones and keeps back-camera processing fast.
     */
    private const val MAX_DIMENSION = 1920

    /**
     * Applies orientation (EXIF + optional horizontal flip), downscales if over [MAX_DIMENSION], keeps JPEG.
     *
     * @param file Source image (e.g. JPEG from camera)
     * @param flipHorizontal true = mirror (front camera), false = EXIF normalization only
     * @return Path to the output .jpg file, or null on failure (original file unchanged)
     */
    fun flipAndCompressImage(file: File, flipHorizontal: Boolean = false): String? {
        if (!file.exists() || !file.canRead()) return null
        val path = file.absolutePath
        val parentDir = file.parentFile ?: return null
        val outputFile = File.createTempFile("snaplet_", ".jpg", parentDir)
        return try {
            val (boundsW, boundsH) = decodeBounds(path) ?: return null
            val exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = matrixForExifOrientation(orientation)
            if (flipHorizontal) matrix.postScale(-1f, 1f)
            val rect = RectF(0f, 0f, boundsW.toFloat(), boundsH.toFloat())
            matrix.mapRect(rect)
            matrix.postTranslate(-rect.left, -rect.top)
            val outWidth = rect.width().toInt().coerceAtLeast(1)
            val outHeight = rect.height().toInt().coerceAtLeast(1)
            val inSampleSize = computeInSampleSize(maxOf(outWidth, outHeight), MAX_DIMENSION)

            val bitmap = decodeWithSampleSize(path, inSampleSize) ?: return null
            val result =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()

            FileOutputStream(outputFile).use { out ->
                result.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.fd.sync()
            }
            result.recycle()

            ExifInterface(outputFile.absolutePath).apply {
                setAttribute(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString()
                )
                saveAttributes()
            }
            outputFile.absolutePath
        } catch (e: Exception) {
            Logger.e(e, "flipAndCompressImage failed: $path")
            if (outputFile.exists()) outputFile.delete()
            null
        }
    }

    /** Decode only bounds; returns (width, height) or null. */
    private fun decodeBounds(path: String): Pair<Int, Int>? {
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, this)
            if (outWidth <= 0 || outHeight <= 0) null else Pair(outWidth, outHeight)
        }
    }

    /** Decode with inSampleSize; RGB_565 since we encode to JPEG (no alpha). */
    private fun decodeWithSampleSize(path: String, inSampleSize: Int): Bitmap? {
        return BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.RGB_565
        }.let { BitmapFactory.decodeFile(path, it) }
    }

    /** Smallest power-of-2 inSampleSize so that maxDimension / inSampleSize <= maxTarget. */
    private fun computeInSampleSize(maxDimension: Int, maxTarget: Int): Int {
        if (maxDimension <= maxTarget) return 1
        var sampleSize = 1
        while ((maxDimension / sampleSize) > maxTarget) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun buildTransformMatrix(
        rotationDeg: Float = 0f,
        flipH: Boolean = false,
        flipV: Boolean = false,
    ): Matrix = Matrix().apply {
        if (rotationDeg != 0f) setRotate(rotationDeg)
        if (flipH) postScale(-1f, 1f)
        if (flipV) postScale(1f, -1f)
    }

    private fun matrixForExifOrientation(orientation: Int): Matrix = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> buildTransformMatrix(rotationDeg = 90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> buildTransformMatrix(rotationDeg = 180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> buildTransformMatrix(rotationDeg = 270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> buildTransformMatrix(flipH = true)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> buildTransformMatrix(flipV = true)
        else -> Matrix()
    }

    /**
     * Maps the frame rect (position + size in display coords) back to original
     * image pixels, then applies rotation + flip via Matrix.
     *
     * Image is displayed at 1:1 (no scale), so:
     *   ratioX = origW / displayW
     *   ratioY = origH / displayH
     *   srcRect = frameRect * ratio
     */
    fun cropImageRegion(
        context: Context,
        uri: Uri,
        displayImageW: Int,
        displayImageH: Int,
        frameLeft: Float,
        frameTop: Float,
        frameW: Float,
        frameH: Float,
        rotationDeg: Int = 0,
        isFlippedH: Boolean = false,
        isFlippedV: Boolean = false,
    ): Bitmap? {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
            val origW = opts.outWidth
            val origH = opts.outHeight
            if (origW <= 0 || origH <= 0) return null

            val ratioX = origW.toFloat() / displayImageW
            val ratioY = origH.toFloat() / displayImageH

            val srcLeft: Int = (frameLeft * ratioX).roundToInt().coerceIn(0, origW)
            val srcTop: Int = (frameTop * ratioY).roundToInt().coerceIn(0, origH)
            val srcRight: Int = ((frameLeft + frameW) * ratioX).roundToInt().coerceIn(0, origW)
            val srcBottom: Int = ((frameTop + frameH) * ratioY).roundToInt().coerceIn(0, origH)

            if (srcRight <= srcLeft || srcBottom <= srcTop) return null

            val region = context.contentResolver.openInputStream(uri)?.use { stream ->
                val decoder = BitmapRegionDecoder.newInstance(stream, false)
                decoder?.decodeRegion(
                    Rect(srcLeft, srcTop, srcRight, srcBottom),
                    BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 })
                    .also { decoder?.recycle() }
            } ?: return null

            val needTransform = rotationDeg != 0 || isFlippedH || isFlippedV
            if (!needTransform) return region

            val matrix = buildTransformMatrix(
                rotationDeg = rotationDeg.toFloat(),
                flipH = isFlippedH,
                flipV = isFlippedV,
            )

            Bitmap.createBitmap(region, 0, 0, region.width, region.height, matrix, true)
                .also { region.recycle() }
        } catch (e: Exception) {
            Logger.d("Crop region failed: ${e.message}")
            null
        }
    }

    fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val file = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            Logger.d("Save bitmap to cache failed: ${e.message}")
            null
        }
    }

    /** Deletes the file at [filePath]. Returns true if deleted. */
    fun deleteFileFromPath(filePath: String?): Boolean {
        if (filePath == null) {
            return false
        }

        return try {
            val file = File(filePath)
            if (file.exists()) {
                val deleted = file.delete()
                if (!deleted) {
                    Logger.w("⚠️ Failed to delete file (file.delete() returned false): $filePath")
                }
                deleted
            } else {
                false
            }
        } catch (e: Exception) {
            Logger.e(e, "❌ Failed to delete file: $filePath")
            false
        }
    }

    /** Deletes each file at [filePaths]. Returns count of deleted files. */
    fun deleteFilesFromPaths(filePaths: List<String?>): Int {
        return filePaths.count { deleteFileFromPath(it) }
    }
}
