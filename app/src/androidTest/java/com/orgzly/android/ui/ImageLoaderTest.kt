package com.orgzly.android.ui

import org.junit.Assert.*
import org.junit.Test


class ImageLoaderTest {

    @Test
    fun hasSupportedExtension() {
        assertTrue(ImageLoader.hasSupportedExtension("file.png"))
        assertTrue(ImageLoader.hasSupportedExtension("file.PNG"))
        assertTrue(ImageLoader.hasSupportedExtension("../file.png"))
        assertTrue(ImageLoader.hasSupportedExtension("dir/file.png"))

        assertFalse(ImageLoader.hasSupportedExtension("png"))
        assertFalse(ImageLoader.hasSupportedExtension(".png"))
        assertFalse(ImageLoader.hasSupportedExtension("filepng"))
        assertFalse(ImageLoader.hasSupportedExtension("dir.png/file"))
    }
}