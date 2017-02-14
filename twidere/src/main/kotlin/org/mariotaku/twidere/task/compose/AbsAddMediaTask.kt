/*
 *             Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2017 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.task.compose

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import org.mariotaku.abstask.library.AbstractTask
import org.mariotaku.commons.io.StreamUtils
import org.mariotaku.twidere.Constants
import org.mariotaku.twidere.model.ParcelableMedia
import org.mariotaku.twidere.model.ParcelableMediaUpdate
import org.mariotaku.twidere.util.DebugLog
import org.mariotaku.twidere.util.Utils
import java.io.*
import java.lang.ref.WeakReference

open class AbsAddMediaTask<Callback>(
        context: Context,
        val sources: Array<Uri>,
        val deleteSrc: Boolean
) : AbstractTask<Unit, List<ParcelableMediaUpdate>?, Callback>() {

    private val contextRef = WeakReference(context)
    val context: Context? get() = contextRef.get()

    override fun doLongOperation(params: Unit?): List<ParcelableMediaUpdate>? {
        val context = contextRef.get() ?: return null
        val resolver = context.contentResolver
        return sources.mapIndexedNotNull { index, source ->
            var st: InputStream? = null
            var os: OutputStream? = null
            try {
                val sourceMimeType = resolver.getType(source)
                val mediaType = sourceMimeType?.let {
                    return@let when {
                        it.startsWith("video/") -> ParcelableMedia.Type.VIDEO
                        it.startsWith("image/") -> ParcelableMedia.Type.IMAGE
                        else -> ParcelableMedia.Type.IMAGE
                    }
                } ?: ParcelableMedia.Type.IMAGE
                val extension = sourceMimeType?.let { mimeType ->
                    MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                } ?: "tmp"
                val destination = createTempImageUri(context, index, extension)
                st = resolver.openInputStream(source)
                os = resolver.openOutputStream(destination)
                if (st == null || os == null) throw FileNotFoundException()
                StreamUtils.copy(st, os, null, null)
                if (deleteSrc) {
                    Utils.deleteMedia(context, source)
                }
                return@mapIndexedNotNull ParcelableMediaUpdate(destination.toString(), mediaType)
            } catch (e: IOException) {
                DebugLog.w(Constants.LOGTAG, tr = e)
                return@mapIndexedNotNull null
            } finally {
                Utils.closeSilently(os)
                Utils.closeSilently(st)
            }
        }
    }


    private fun createTempImageUri(context: Context, extraNum: Int, ext: String): Uri {
        val file = File(context.cacheDir, "tmp_media_${System.currentTimeMillis()}_$extraNum.$ext")
        return Uri.fromFile(file)
    }

}