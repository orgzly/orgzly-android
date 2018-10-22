package com.orgzly.android.ui

import android.graphics.Color
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


object ImageLoader {
    @JvmStatic
    fun loadImages(textWithMarkup: TextViewWithMarkup) {
        val context = textWithMarkup.context

        // Only if AppPreferences.displayImages(context) is true
        // Setup image visualization inside the note
        if ( AppPreferences.displayInlineImages(context)
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

            if(file.exists()) {
                // Get the Uri
                val contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", file)

                // Setup a placeholder
                val drawable = ColorDrawable(Color.TRANSPARENT)
                drawable.setBounds(0, 0, AppPreferences.setImageFixedWidth(context), AppPreferences.setImageFixedWidth(context))

                Glide.with(context)
                    .asDrawable()
                    // Use a placeholder
                    .apply(RequestOptions().placeholder(drawable))
                    // And scaled thumbnails for faster display
                    .thumbnail(Glide.with(context)
                            .applyDefaultRequestOptions(RequestOptions().override(AppPreferences.setImageFixedWidth(context)))
                            .load(contentUri))
                    .load(contentUri)
                    .into(object : SimpleTarget<Drawable>() {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                            fitDrawable(textWithMarkup, resource)
                            text.setSpan(ImageSpan(resource), text.getSpanStart(span), text.getSpanEnd(span), text.getSpanFlags(span))
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

    fun fitDrawable(view: View, drawable: Drawable) {
        // Get the display metrics to be able to rescale the image if needed
        val metrics = view.context.resources.displayMetrics

        // Gather drawable information
        // Scale the height by the scaledDensity to get original image size
        val drawableHeight = drawable.intrinsicHeight.toFloat() * metrics.scaledDensity
        val drawableWidth = drawable.intrinsicWidth.toFloat() * metrics.scaledDensity

        // Use either a fixed size or a scaled size according to user preferences
        var fixedSize = -1
        if (!AppPreferences.enableImageScaling(view.context)) {
            fixedSize = AppPreferences.setImageFixedWidth(view.context)
        }

        if (fixedSize > 0) {
            // Keep aspect ratio when using fixed size
            val ratio = drawableHeight / drawableWidth
            drawable.setBounds(0, 0, fixedSize, (fixedSize * ratio).toInt())
        } else {
            // Rescale the drawable if it is larger that the current view width
            if (drawableWidth > view.width) {
                //Compute image ratio
                val ratio = drawableHeight / drawableWidth
                // Ensure that the images have a minimum size
                val width = Math.max(view.width, 256).toFloat()

                drawable.setBounds(0, 0, width.toInt(), (width * ratio).toInt())
            } else {
                drawable.setBounds(0, 0, drawableWidth.toInt(), drawableHeight.toInt())
            }
        }
    }
}