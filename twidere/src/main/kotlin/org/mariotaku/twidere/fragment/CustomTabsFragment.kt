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

package org.mariotaku.twidere.fragment

import android.accounts.AccountManager
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.graphics.Paint
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.LoaderManager.LoaderCallbacks
import android.support.v4.content.CursorLoader
import android.support.v4.content.Loader
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.widget.ImageViewCompat
import android.support.v7.app.AlertDialog
import android.util.SparseArray
import android.view.*
import android.widget.*
import android.widget.AbsListView.MultiChoiceModeListener
import android.widget.AdapterView.OnItemClickListener
import com.mobeta.android.dslv.SimpleDragSortCursorAdapter
import kotlinx.android.synthetic.main.dialog_custom_tab_editor.*
import kotlinx.android.synthetic.main.layout_draggable_list_with_empty_view.*
import kotlinx.android.synthetic.main.list_item_section_header.view.*
import org.mariotaku.chameleon.Chameleon
import org.mariotaku.ktextension.Bundle
import org.mariotaku.ktextension.contains
import org.mariotaku.ktextension.set
import org.mariotaku.ktextension.spannable
import org.mariotaku.library.objectcursor.ObjectCursor
import org.mariotaku.sqliteqb.library.Columns.Column
import org.mariotaku.sqliteqb.library.Expression
import org.mariotaku.sqliteqb.library.RawItemArray
import org.mariotaku.twidere.R
import org.mariotaku.twidere.TwidereConstants.*
import org.mariotaku.twidere.activity.SettingsActivity
import org.mariotaku.twidere.adapter.AccountsSpinnerAdapter
import org.mariotaku.twidere.adapter.ViewHolderListAdapter
import org.mariotaku.twidere.annotation.CustomTabType
import org.mariotaku.twidere.annotation.TabAccountFlags
import org.mariotaku.twidere.extension.applyTheme
import org.mariotaku.twidere.extension.getAllDetails
import org.mariotaku.twidere.extension.insert
import org.mariotaku.twidere.extension.model.isOfficial
import org.mariotaku.twidere.extension.update
import org.mariotaku.twidere.model.AccountDetails
import org.mariotaku.twidere.model.Tab
import org.mariotaku.twidere.model.tab.DrawableHolder
import org.mariotaku.twidere.model.tab.TabConfiguration
import org.mariotaku.twidere.model.tab.iface.AccountCallback
import org.mariotaku.twidere.provider.TwidereDataStore.Tabs
import org.mariotaku.twidere.util.CustomTabUtils
import org.mariotaku.twidere.util.ThemeUtils
import org.mariotaku.twidere.view.holder.TwoLineWithIconViewHolder
import java.lang.ref.WeakReference

class CustomTabsFragment : BaseFragment(), LoaderCallbacks<Cursor?>, MultiChoiceModeListener {

    private lateinit var adapter: CustomTabsAdapter

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete -> {
                val itemIds = listView.checkedItemIds
                val where = Expression.`in`(Column(Tabs._ID), RawItemArray(itemIds))
                context!!.contentResolver.delete(Tabs.CONTENT_URI, where.sql, null)
                SettingsActivity.setShouldRestart(activity!!)
            }
        }
        mode.finish()
        return true
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
        adapter = CustomTabsAdapter(context!!)
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
        listView.setMultiChoiceModeListener(this)
        listView.onItemClickListener = OnItemClickListener { _, _, position, _ ->
            val tab = adapter.getTab(position)
            val df = TabEditorDialogFragment()
            df.arguments = Bundle {
                this[EXTRA_OBJECT] = tab
            }
            df.show(fragmentManager, TabEditorDialogFragment.TAG_EDIT_TAB)
        }
        listView.adapter = adapter
        listView.emptyView = emptyView
        listView.setDropListener { from, to ->
            adapter.drop(from, to)
            if (listView.choiceMode != AbsListView.CHOICE_MODE_NONE) {
                listView.moveCheckState(from, to)
            }
            saveTabPositions()
        }
        emptyText.setText(R.string.no_tab)
        emptyIcon.setImageResource(R.drawable.ic_info_tab)
        loaderManager.initLoader(0, null, this)
        setListShown(false)
    }

    private fun setListShown(shown: Boolean) {
        listContainer.visibility = if (shown) View.VISIBLE else View.GONE
        progressContainer.visibility = if (shown) View.GONE else View.VISIBLE
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.action_multi_select_items, menu)
        return true
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor?> {
        return CursorLoader(activity!!, Tabs.CONTENT_URI, Tabs.COLUMNS, null, null, Tabs.DEFAULT_SORT_ORDER)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_custom_tabs, menu)
        val context = this.context!!
        val accounts = AccountManager.get(context).getAllDetails(false)
        val itemAdd = menu.findItem(R.id.add_submenu)
        val theme = Chameleon.getOverrideTheme(context, context)
        if (itemAdd != null && itemAdd.hasSubMenu()) {
            val subMenu = itemAdd.subMenu
            subMenu.clear()
            for ((type, conf) in TabConfiguration.all) {
                val accountRequired = TabAccountFlags.FLAG_ACCOUNT_REQUIRED in conf.accountFlags
                val subItem = subMenu.add(0, 0, conf.sortPosition, conf.name.createString(context))
                val disabledByNoAccount = accountRequired && accounts.none(conf::checkAccountAvailability)
                val disabledByDuplicateTab = conf.isSingleTab && CustomTabUtils.isTabAdded(context, type)
                val shouldDisable = disabledByDuplicateTab || disabledByNoAccount
                subItem.isVisible = !shouldDisable
                subItem.isEnabled = !shouldDisable
                subItem.icon = conf.icon.createDrawable(context).apply {
                    mutate()
                    DrawableCompat.setTint(this, theme.textColorPrimary)
                }
                val weakFragment = WeakReference(this)
                subItem.setOnMenuItemClickListener {
                    val fragment = weakFragment.get()?.takeUnless(Fragment::isDetached) ?:
                            return@setOnMenuItemClickListener false
                    val adapter = fragment.adapter
                    val fm = fragment.childFragmentManager

                    val df = TabEditorDialogFragment()
                    df.arguments = Bundle {
                        this[EXTRA_TAB_TYPE] = type
                        if (!adapter.isEmpty) {
                            this[EXTRA_TAB_POSITION] = adapter.getTab(adapter.count - 1).position + 1
                        }
                    }
                    df.show(fm, TabEditorDialogFragment.TAG_ADD_TAB)
                    return@setOnMenuItemClickListener true
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_draggable_list_with_empty_view, container, false)
    }

    override fun onDestroyActionMode(mode: ActionMode) {

    }

    override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long,
            checked: Boolean) {
        updateTitle(mode)
    }


    override fun onLoaderReset(loader: Loader<Cursor?>) {
        adapter.changeCursor(null)
    }

    override fun onLoadFinished(loader: Loader<Cursor?>, cursor: Cursor?) {
        adapter.changeCursor(cursor)
        setListShown(true)
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        updateTitle(mode)
        return true
    }

    override fun onStop() {
        super.onStop()
    }

    private fun saveTabPositions() {
        val positions = adapter.cursorPositions
        val c = adapter.cursor
        if (positions != null && c != null && !c.isClosed) {
            val idIdx = c.getColumnIndex(Tabs._ID)
            for (i in 0 until positions.size) {
                c.moveToPosition(positions[i])
                val id = c.getLong(idIdx)
                val values = ContentValues()
                values.put(Tabs.POSITION, i)
                val where = Expression.equals(Tabs._ID, id).sql
                context!!.contentResolver.update(Tabs.CONTENT_URI, values, where, null)
            }
        }
        SettingsActivity.setShouldRestart(activity!!)
    }

    private fun updateTitle(mode: ActionMode?) {
        if (listView == null || mode == null || activity == null) return
        val count = listView.checkedItemCount
        mode.title = resources.getQuantityString(R.plurals.Nitems_selected, count, count)
    }

    class TabEditorDialogFragment : BaseDialogFragment(), DialogInterface.OnShowListener, AccountCallback {

        private val activityResultMap: SparseArray<TabConfiguration.ExtraConfiguration> = SparseArray()

        override fun onShow(dialogInterface: DialogInterface) {
            val dialog = dialogInterface as AlertDialog
            dialog.applyTheme()
            @CustomTabType
            val tabType: String
            val tab: Tab
            val conf: TabConfiguration
            when (tag) {
                TAG_ADD_TAB -> {
                    tabType = arguments!!.getString(EXTRA_TAB_TYPE)
                    tab = Tab()
                    conf = TabConfiguration.ofType(tabType)!!
                    tab.type = tabType
                    tab.icon = conf.icon.persistentKey
                    tab.position = arguments!!.getInt(EXTRA_TAB_POSITION)
                }
                TAG_EDIT_TAB -> {
                    tab = arguments!!.getParcelable(EXTRA_OBJECT)
                    tabType = tab.type
                    conf = TabConfiguration.ofType(tabType) ?: run {
                        dismiss()
                        return
                    }
                }
                else -> {
                    throw AssertionError()
                }
            }

            val tabName = dialog.tabName
            val iconSpinner = dialog.tabIconSpinner
            val accountSpinner = dialog.accountSpinner
            val accountContainer = dialog.accountContainer
            val extraConfigContainer = dialog.extraConfigContainer

            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)

            val iconsAdapter = TabIconsAdapter(context!!)
            val accountsAdapter = AccountsSpinnerAdapter(context!!, requestManager = requestManager)
            iconSpinner.adapter = iconsAdapter
            accountSpinner.adapter = accountsAdapter

            iconsAdapter.data = DrawableHolder.builtins()

            tabName.hint = conf.name.createString(context)
            tabName.setText(tab.name)
            iconSpinner.setSelection(iconsAdapter.findPositionByKey(tab.icon))

            val editMode = tag == TAG_EDIT_TAB

            val hasAccount = TabAccountFlags.FLAG_HAS_ACCOUNT in conf.accountFlags
            val accountMutable = TabAccountFlags.FLAG_ACCOUNT_MUTABLE in conf.accountFlags
            if (hasAccount && (accountMutable || !editMode)) {
                accountContainer.visibility = View.VISIBLE
                val accountRequired = TabAccountFlags.FLAG_ACCOUNT_REQUIRED in conf.accountFlags
                val accounts = mutableListOf<AccountDetails>()
                if (!accountRequired) {
                    accounts += AccountDetails.dummy()
                }
                val officialKeyOnly = arguments!!.getBoolean(EXTRA_OFFICIAL_KEY_ONLY, false)
                AccountManager.get(context).getAllDetails(true).filterTo(accounts) {
                    if (officialKeyOnly && !it.isOfficial(context!!)) {
                        return@filterTo false
                    }
                    return@filterTo conf.checkAccountAvailability(it)
                }
                accountsAdapter.dummyItemText = getString(R.string.activated_accounts)
                accountsAdapter.accounts = accounts

                tab.arguments?.accountKeys?.firstOrNull()?.let { key ->
                    accountSpinner.setSelection(accountsAdapter.findPositionByKey(key))
                }
            } else {
                accountContainer.visibility = View.GONE
            }

            val extraConfigurations = conf.getExtraConfigurations(context!!).orEmpty()

            fun inflateHeader(title: String): View {
                val headerView = LayoutInflater.from(context).inflate(R.layout.list_item_section_header,
                        extraConfigContainer, false)
                headerView.sectionHeader.text = title
                return headerView
            }

            extraConfigurations.forEachIndexed { idx, extraConf ->
                extraConf.onCreate(context!!)
                extraConf.position = idx + 1
                // Hide immutable settings in edit mode
                if (editMode && !extraConf.isMutable) return@forEachIndexed
                extraConf.headerTitle?.let {
                    // Inflate header with headerTitle
                    extraConfigContainer.addView(inflateHeader(it.createString(context)))
                }
                val view = extraConf.onCreateView(context!!, extraConfigContainer)
                extraConf.onViewCreated(context!!, view, this)
                conf.readExtraConfigurationFrom(tab, extraConf)
                extraConfigContainer.addView(view)
            }

            accountSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                private fun updateExtraTabs(account: AccountDetails?) {
                    extraConfigurations.forEach {
                        it.onAccountSelectionChanged(account)
                    }
                }

                override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                    val account = parent.selectedItem as? AccountDetails
                    updateExtraTabs(account)
                }

                override fun onNothingSelected(view: AdapterView<*>) {

                }
            }

            positiveButton.setOnClickListener {
                tab.name = tabName.text.toString()
                tab.icon = (iconSpinner.selectedItem as DrawableHolder).persistentKey
                if (tab.arguments == null) {
                    tab.arguments = CustomTabUtils.newTabArguments(tabType)
                }
                if (tab.extras == null) {
                    tab.extras = CustomTabUtils.newTabExtras(tabType)
                }
                if (hasAccount && (!editMode || TabAccountFlags.FLAG_ACCOUNT_MUTABLE in conf.accountFlags)) {
                    val account = accountSpinner.selectedItem as? AccountDetails ?: return@setOnClickListener
                    if (!account.dummy) {
                        tab.arguments?.accountKeys = arrayOf(account.key)
                    } else {
                        tab.arguments?.accountKeys = null
                    }
                }
                extraConfigurations.forEach { extraConf ->
                    // Make sure immutable configuration skipped in edit mode
                    if (editMode && !extraConf.isMutable) return@forEach
                    if (!conf.applyExtraConfigurationTo(tab, extraConf)) {
                        extraConf.showRequiredError()
                        return@setOnClickListener
                    }
                }
                when (tag) {
                    TAG_EDIT_TAB -> {
                        val where = Expression.equals(Tabs._ID, tab.id).sql
                        context!!.contentResolver.update(Tabs.CONTENT_URI, Tabs.COLUMNS,
                                where, null, cls = Tab::class.java) { return@update tab }
                    }
                    TAG_ADD_TAB -> {
                        context!!.contentResolver.insert(Tabs.CONTENT_URI, tab, Tab::class.java)
                    }
                }
                SettingsActivity.setShouldRestart(activity!!)
                dismiss()
            }
        }

        override fun getAccount(): AccountDetails? {
            return dialog.findViewById<Spinner>(R.id.accountSpinner).selectedItem as? AccountDetails
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(context!!)
            builder.setView(R.layout.dialog_custom_tab_editor)
            builder.setPositiveButton(R.string.action_save, null)
            builder.setNegativeButton(android.R.string.cancel, null)
            val dialog = builder.create()
            dialog.setOnShowListener(this)
            return dialog
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            val extraConf = activityResultMap.get(requestCode)
            activityResultMap.remove(requestCode)
            extraConf?.onActivityResult(this, requestCode and 0xFF, resultCode, data)
        }

        fun startExtraConfigurationActivityForResult(extraConf: TabConfiguration.ExtraConfiguration, intent: Intent, requestCode: Int) {
            val requestCodeInternal = extraConf.position shl 8 and 0xFF00 or (requestCode and 0xFF)
            activityResultMap.put(requestCodeInternal, extraConf)
            startActivityForResult(intent, requestCodeInternal)
        }

        companion object {

            const val TAG_EDIT_TAB = "edit_tab"
            const val TAG_ADD_TAB = "add_tab"
        }
    }

    internal class TabIconsAdapter(context: Context) : ViewHolderListAdapter<TabIconsAdapter.TabIconViewHolder>() {

        private val iconColor: Int = ThemeUtils.getThemeForegroundColor(context)

        private var itemViewResource = R.layout.spinner_item_custom_tab_icon
        private var dropDownViewResource = R.layout.list_item_two_line_small

        var data: List<DrawableHolder>? = null
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getItem(position: Int): DrawableHolder {
            return data!![position]
        }

        override fun getCount(): Int {
            return data?.size ?: 0
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabIconViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(itemViewResource, parent, false)
            return TabIconViewHolder(this, view)
        }

        override fun onCreateDropDownViewHolder(parent: ViewGroup, viewType: Int): TabIconViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(dropDownViewResource, parent, false)
            return TabDropdownViewHolder(this, view)
        }

        override fun onBindViewHolder(holder: TabIconViewHolder, position: Int) {
            holder.display(getItem(position))
        }

        fun findPositionByKey(key: String): Int {
            return (0 until count).indexOfFirst { getItem(it).persistentKey == key }
        }

        open class TabIconViewHolder(val adapter: TabIconsAdapter, itemView: View) : ViewHolder(itemView) {
            protected val icon: ImageView = itemView.findViewById(android.R.id.icon)

            open fun display(item: DrawableHolder) {
                icon.setImageDrawable(item.createDrawable(icon.context))
            }
        }

        class TabDropdownViewHolder(adapter: TabIconsAdapter, itemView: View) : TabIconViewHolder(adapter, itemView) {
            private val text1: TextView = itemView.findViewById(android.R.id.text1)
            private val text2: TextView = itemView.findViewById(android.R.id.text2)
            override fun display(item: DrawableHolder) {
                super.display(item)
                ImageViewCompat.setImageTintList(icon, text1.textColors)
                text1.spannable = item.name
                text2.visibility = View.GONE
            }
        }
    }

    class CustomTabsAdapter(context: Context) : SimpleDragSortCursorAdapter(context,
            R.layout.list_item_custom_tab, null, emptyArray(), intArrayOf(), 0) {

        private val tempTab: Tab = Tab()
        private var indices: ObjectCursor.CursorIndices<Tab>? = null

        override fun bindView(view: View, context: Context?, cursor: Cursor) {
            super.bindView(view, context, cursor)
            val holder = view.tag as TwoLineWithIconViewHolder
            indices?.parseFields(tempTab, cursor)
            val type = tempTab.type
            val name = tempTab.name
            val iconKey = tempTab.icon
            if (type != null && CustomTabUtils.isTabTypeValid(type)) {
                val typeName = CustomTabUtils.getTabTypeName(context, type)
                holder.text1.spannable = name?.takeIf(String::isNotEmpty) ?: typeName
                holder.text1.paintFlags = holder.text1.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                holder.text2.visibility = View.VISIBLE
                holder.text2.text = typeName
            } else {
                holder.text1.spannable = name
                holder.text1.paintFlags = holder.text1.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                holder.text2.setText(R.string.invalid_tab)
            }
            val icon = CustomTabUtils.getTabIconDrawable(context, DrawableHolder.parse(iconKey))
            holder.icon.visibility = View.VISIBLE
            if (icon != null) {
                holder.icon.setImageDrawable(icon)
            } else {
                holder.icon.setImageResource(R.drawable.ic_action_list)
            }
        }

        override fun changeCursor(cursor: Cursor?) {
            indices = cursor?.let { ObjectCursor.indicesFrom(it, Tab::class.java) }
            super.changeCursor(cursor)
        }

        override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup): View {
            val view = super.newView(context, cursor, parent)
            val tag = view.tag
            if (tag !is TwoLineWithIconViewHolder) {
                val holder = TwoLineWithIconViewHolder(view)
                view.tag = holder
            }
            return view
        }


        fun getTab(position: Int): Tab {
            cursor.moveToPosition(position)
            return indices!!.newObject(cursor)
        }

    }

}

