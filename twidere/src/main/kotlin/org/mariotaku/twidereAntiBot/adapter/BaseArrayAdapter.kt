/*
 * 				Twidere - Twitter client for Android
 * 
 *  Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package org.mariotaku.twidereAntiBot.adapter

import android.content.Context
import android.content.SharedPreferences
import android.support.v4.text.BidiFormatter
import com.bumptech.glide.RequestManager
import org.mariotaku.kpreferences.get
import org.mariotaku.twidereAntiBot.R
import org.mariotaku.twidereAntiBot.adapter.iface.IContentAdapter
import org.mariotaku.twidereAntiBot.adapter.iface.IItemCountsAdapter
import org.mariotaku.twidereAntiBot.adapter.iface.ILoadMoreSupportAdapter
import org.mariotaku.twidereAntiBot.constant.*
import org.mariotaku.twidereAntiBot.model.ItemCounts
import org.mariotaku.twidereAntiBot.util.*
import org.mariotaku.twidereAntiBot.util.dagger.GeneralComponent
import javax.inject.Inject

open class BaseArrayAdapter<T>(
        context: Context,
        layoutRes: Int,
        collection: Collection<T>? = null,
        override val requestManager: RequestManager
) : ArrayAdapter<T>(context, layoutRes, collection), IContentAdapter, ILoadMoreSupportAdapter,
        IItemCountsAdapter {
    val linkify: TwidereLinkify

    @Inject
    override lateinit var userColorNameManager: UserColorNameManager
    @Inject
    override lateinit var bidiFormatter: BidiFormatter
    @Inject
    override lateinit var twitterWrapper: AsyncTwitterWrapper
    @Inject
    lateinit var multiSelectManager: MultiSelectManager
    @Inject
    lateinit var preferences: SharedPreferences
    @Inject
    lateinit var permissionsManager: PermissionsManager

    final override val profileImageSize: String = context.getString(R.string.profile_image_size)
    final override val profileImageStyle: Int
    final override val textSize: Float
    final override val profileImageEnabled: Boolean
    final override val showAbsoluteTime: Boolean
    val nameFirst: Boolean

    override val itemCounts: ItemCounts = ItemCounts(1)


    override var loadMoreSupportedPosition: Long = 0
        @ILoadMoreSupportAdapter.IndicatorPosition set(value) {
            field = value
            loadMoreIndicatorPosition = ILoadMoreSupportAdapter.apply(loadMoreIndicatorPosition, value)
            notifyDataSetChanged()
        }

    override var loadMoreIndicatorPosition: Long = 0
        @ILoadMoreSupportAdapter.IndicatorPosition set(value) {
            if (field == value) return
            field = ILoadMoreSupportAdapter.apply(value, loadMoreSupportedPosition)
            notifyDataSetChanged()
        }

    init {
        @Suppress("UNCHECKED_CAST")
        GeneralComponent.get(context).inject(this as BaseArrayAdapter<Any>)
        linkify = TwidereLinkify(OnLinkClickHandler(context, multiSelectManager, preferences))
        profileImageStyle = preferences[profileImageStyleKey]
        textSize = preferences[textSizeKey].toFloat()
        profileImageEnabled = preferences[displayProfileImageKey]
        showAbsoluteTime = preferences[showAbsoluteTimeKey]
        nameFirst = preferences[nameFirstKey]
    }

    override fun getItemCount(): Int = count

}
