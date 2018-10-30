package com.orgzly.android.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Environment
import android.support.v4.content.FileProvider
import android.text.Spannable
import android.text.style.ImageSpan
import android.view.View
import com.orgzly.BuildConfig
import com.orgzly.android.App
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.views.TextViewWithMarkup
import com.orgzly.android.ui.views.style.FileLinkSpan
import com.orgzly.android.util.AppPermissions
import java.io.File
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.request.RequestOptions
import com.orgzly.android.util.LogUtils


object ImageLoader {
    @JvmStatic
    fun loadImages(textWithMarkup: TextViewWithMarkup) {
        val context = textWithMarkup.context

        // Only if AppPreferences.displayImages(context) is true
        // Setup image visualization inside the note
        if (AppPreferences.imagesEnabled(context)
                // Storage permission has been granted
                && AppPermissions.isGranted(context, AppPermissions.Usage.EXTERNAL_FILES_ACCESS)) {
            // Load the associated image for each FileLinkSpan
            SpanUtils.forEachSpan(textWithMarkup.text as Spannable, FileLinkSpan::class.java) { span ->
                loadImage(textWithMarkup, span)
            }
        }
    }

    private fun loadImage(textWithMarkup: TextViewWithMarkup, span: FileLinkSpan) {
        val path = span.path

        if (hasSupportedExtension(path)) {
            val text = textWithMarkup.text as Spannable
            // Get the current context
            val context = App.getAppContext()

            // Get the file
            val file = File(Environment.getExternalStorageDirectory(), path)

            if (file.exists()) {
                // Get the Uri
                val contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", file)

                // Get image sizes to reduce their memory footprint by rescaling
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().toString() + "/" + path, options)

                val size = calculateImageDisplaySize(
                        file.name, "pre-load", textWithMarkup, options.outWidth, options.outHeight)

                // Setup a placeholder
                val drawable = ColorDrawable(Color.TRANSPARENT)
                drawable.setBounds(0, 0, size.first, size.second)

                Glide.with(context)
                        .asBitmap()
                        // Use a placeholder
                        .apply(RequestOptions().placeholder(drawable))
                        // Override the bitmap size, mainly used for big images
                        // as it's useless to display more pixel that the pixel density allows
                        .apply(RequestOptions().override(size.first, size.second))
                        .load(contentUri)
                        .into(object : SimpleTarget<Bitmap>() {

                            val start = text.getSpanStart(span)
                            val end = text.getSpanEnd(span)
                            val flags = text.getSpanFlags(span)

                            var placeholderSpan: ImageSpan? = null

                            override fun onLoadStarted(placeholder: Drawable?) {
                                if (placeholder != null) {
                                    placeholderSpan = ImageSpan(placeholder)
                                    text.setSpan(placeholderSpan, start, end, flags)
                                }
                            }

                            override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>?) {
                                val bitmapDrawable = BitmapDrawable(
                                        textWithMarkup.context.resources, bitmap)

                                val newSize = calculateImageDisplaySize(
                                        file.name, "on-load",
                                        textWithMarkup,
                                        bitmapDrawable.bitmap.width,
                                        bitmapDrawable.bitmap.height)

                                bitmapDrawable.setBounds(0, 0, newSize.first, newSize.second)

                                placeholderSpan?.let {
                                    text.removeSpan(it)
                                }

                                text.setSpan(ImageSpan(bitmapDrawable), start, end, flags)
                            }
                        })
            }
        }
    }

    fun hasSupportedExtension(path: String): Boolean {
        var ret = false
        var index: Int
        var s = ""

        // Find the last slash in the path
        // to avoid case of a point in the path and a file without extension
        index = path.lastIndexOf("/")

        // If we found the last slash, extract the file name
        if (index != -1) {
            s = path.substring(index + 1)
        }

        // Extract the extension
        index = s.lastIndexOf(".")

        // If we found an extension, extract it and test it
        if (index != -1) {
            s = s.substring(index + 1).toLowerCase()

            if (s == "jpg" || s == "jpeg" || s == "gif"
                    || s == "png" || s == "bmp" || s == "webp") {
                ret = true
            }
        }

        return ret
    }

    private fun calculateImageDisplaySize(
            file: String, stage: String, view: View, width: Int, height: Int): Pair<Int, Int> {

        val ratio = height.toFloat() / width.toFloat()

        var newWidth = width

        // Get the display metrics to be able to rescale the image if needed
        val metrics = view.context.resources.displayMetrics

        // Before image loading view.width might not be initialized
        // So we take a default maximum value that will be reduced
        var maxWidth = view.width
        if (maxWidth == 0) {
            maxWidth = metrics.widthPixels
        }

        // Within limits
        if (newWidth > maxWidth) {
            newWidth = maxWidth
        }

        // Scale down to width
        if (AppPreferences.imagesScaleDownToWidth(view.context)) {
            val downToWidth = AppPreferences.imagesScaleDownToWidthValue(view.context)

            if (newWidth > downToWidth) {
                newWidth = downToWidth
            }
        }

        val newHeight = (newWidth * ratio).toInt()

        if (BuildConfig.LOG_DEBUG)
            LogUtils.d(TAG, file, String.format("%-8s  View: %-4d  Metrics: %-4d  Input: %-4dx%-4d  Ratio: %.5f  Output: %-4dx%-4d",
                    stage, view.width, metrics.widthPixels, width, height, ratio, newWidth, newHeight))

        return Pair(newWidth, newHeight)
    }

    private val TAG = ImageLoader::class.java.name
}