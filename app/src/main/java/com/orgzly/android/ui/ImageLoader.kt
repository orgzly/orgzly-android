package com.orgzly.android.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
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

                val size = fitDrawable(textWithMarkup, options.outWidth, options.outHeight)

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
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                val bd = BitmapDrawable(App.getAppContext().resources, resource)
                                fitDrawable(textWithMarkup, bd)
                                text.setSpan(ImageSpan(bd), text.getSpanStart(span), text.getSpanEnd(span), text.getSpanFlags(span))
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

    private fun fitDrawable(view: View, width: Int, height: Int): Pair<Int, Int> {
        var newWidth = width
        var newHeight = height

        // Get the display metrics to be able to rescale the image if needed
        val metrics = view.context.resources.displayMetrics

        // Use either a fixed size or a scaled size according to user preferences
        var fixedSize = -1
        if (AppPreferences.imagesScaleDownToWidth(view.context)) {
            fixedSize = AppPreferences.imagesScaleDownToWidthValue(view.context)
        }

        // Before image loading view.width might not be initialized
        // So we take a default maximum value that will be reduced
        var maxWidth = view.width
        if (maxWidth == 0) {
            maxWidth = metrics.widthPixels
        }

        val ratio = height.toFloat() / width.toFloat()

        if (fixedSize > 0) {
            newWidth = fixedSize
            newHeight = (fixedSize * ratio).toInt()

        } else if (width > maxWidth) {
            // Otherwise if we are using rescaling and the image is wider that the max width

            // Ensure that the images have a minimum size
            val width = Math.max(maxWidth, 256).toFloat()

            newWidth = width.toInt()
            newHeight = (width * ratio).toInt()
        }

        return Pair(newWidth, newHeight)
    }

    fun fitDrawable(view: View, drawable: BitmapDrawable) {
        // Compute the new size of the drawable
        val newSize = fitDrawable(view, drawable.bitmap.width, drawable.bitmap.height)
        // Set the bounds to match the new size
        drawable.setBounds(0, 0, newSize.first, newSize.second)
    }
}