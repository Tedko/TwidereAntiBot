/*
 *                 Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
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

package org.mariotaku.twidereAntiBot.util.dagger

import android.content.Context
import android.support.v7.widget.RecyclerView
import dagger.Component
import org.mariotaku.twidereAntiBot.activity.*
import org.mariotaku.twidereAntiBot.adapter.*
import org.mariotaku.twidereAntiBot.app.TwidereApplication
import org.mariotaku.twidereAntiBot.fragment.BaseDialogFragment
import org.mariotaku.twidereAntiBot.fragment.BaseFragment
import org.mariotaku.twidereAntiBot.fragment.BasePreferenceFragment
import org.mariotaku.twidereAntiBot.fragment.ThemedPreferenceDialogFragmentCompat
import org.mariotaku.twidereAntiBot.fragment.filter.FilteredUsersFragment
import org.mariotaku.twidereAntiBot.fragment.media.ExoPlayerPageFragment
import org.mariotaku.twidereAntiBot.fragment.media.VideoPageFragment
import org.mariotaku.twidereAntiBot.loader.CacheUserSearchLoader
import org.mariotaku.twidereAntiBot.loader.DefaultAPIConfigLoader
import org.mariotaku.twidereAntiBot.loader.ParcelableStatusLoader
import org.mariotaku.twidereAntiBot.loader.ParcelableUserLoader
import org.mariotaku.twidereAntiBot.loader.statuses.AbsRequestStatusesLoader
import org.mariotaku.twidereAntiBot.loader.userlists.BaseUserListsLoader
import org.mariotaku.twidereAntiBot.preference.AccountsListPreference
import org.mariotaku.twidereAntiBot.preference.KeyboardShortcutPreference
import org.mariotaku.twidereAntiBot.preference.PremiumEntryPreference
import org.mariotaku.twidereAntiBot.preference.PremiumEntryPreferenceCategory
import org.mariotaku.twidereAntiBot.preference.sync.SyncItemPreference
import org.mariotaku.twidereAntiBot.provider.CacheProvider
import org.mariotaku.twidereAntiBot.provider.TwidereDataProvider
import org.mariotaku.twidereAntiBot.service.*
import org.mariotaku.twidereAntiBot.task.BaseAbstractTask
import org.mariotaku.twidereAntiBot.text.util.EmojiEditableFactory
import org.mariotaku.twidereAntiBot.text.util.EmojiSpannableFactory
import org.mariotaku.twidereAntiBot.util.MultiSelectEventHandler
import org.mariotaku.twidereAntiBot.util.filter.UrlFiltersSubscriptionProvider
import org.mariotaku.twidereAntiBot.util.sync.SyncTaskRunner
import javax.inject.Singleton

/**
 * Created by mariotaku on 15/10/5.
 */
@Singleton
@Component(modules = arrayOf(ApplicationModule::class))
interface GeneralComponent {
    fun inject(adapter: DummyItemAdapter)

    fun inject(obj: BaseFragment)

    fun inject(obj: MultiSelectEventHandler)

    fun inject(obj: BaseDialogFragment)

    fun inject(obj: LegacyTaskService)

    fun inject(obj: ComposeActivity)

    fun inject(obj: TwidereDataProvider)

    fun inject(obj: BaseActivity)

    fun inject(obj: BaseRecyclerViewAdapter<RecyclerView.ViewHolder>)

    fun inject(obj: AccountDetailsAdapter)

    fun inject(obj: ComposeAutoCompleteAdapter)

    fun inject(obj: UserAutoCompleteAdapter)

    fun inject(obj: AccountsSpinnerAdapter)

    fun inject(obj: BaseArrayAdapter<Any>)

    fun inject(obj: DraftsAdapter)

    fun inject(obj: BasePreferenceFragment)

    fun inject(obj: FilteredUsersFragment.FilterUsersListAdapter)

    fun inject(obj: EmojiSpannableFactory)

    fun inject(obj: EmojiEditableFactory)

    fun inject(obj: AccountsListPreference.AccountItemPreference)

    fun inject(obj: DependencyHolder)

    fun inject(provider: CacheProvider)

    fun inject(loader: AbsRequestStatusesLoader)

    fun inject(activity: MediaViewerActivity)

    fun inject(service: JobTaskService)

    fun inject(task: BaseAbstractTask<Any, Any, Any>)

    fun inject(preference: KeyboardShortcutPreference)

    fun inject(loader: ParcelableUserLoader)

    fun inject(loader: ParcelableStatusLoader)

    fun inject(loader: DefaultAPIConfigLoader)

    fun inject(application: TwidereApplication)

    fun inject(fragment: ThemedPreferenceDialogFragmentCompat)

    fun inject(service: BaseIntentService)

    fun inject(runner: SyncTaskRunner)

    fun inject(preference: SyncItemPreference)

    fun inject(provider: UrlFiltersSubscriptionProvider)

    fun inject(preference: PremiumEntryPreference)

    fun inject(preference: PremiumEntryPreferenceCategory)

    fun inject(loader: CacheUserSearchLoader)

    fun inject(loader: BaseUserListsLoader)

    fun inject(controller: PremiumDashboardActivity.BaseItemViewController)

    fun inject(fragment: ExoPlayerPageFragment)

    fun inject(service: StreamingService)

    fun inject(service: BaseService)

    fun inject(activity: MainActivity)

    fun inject(fragment: VideoPageFragment)

    companion object {

        private var instance: GeneralComponent? = null

        fun get(context: Context): GeneralComponent {
            return instance ?: run {
                val helper = DaggerGeneralComponent.builder().applicationModule(ApplicationModule.get(context)).build()
                instance = helper
                return@run helper
            }
        }

    }
}
