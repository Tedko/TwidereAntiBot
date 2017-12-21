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

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import kotlinx.android.synthetic.main.dialog_user_list_detail_editor.*
import org.mariotaku.ktextension.empty
import org.mariotaku.ktextension.string
import org.mariotaku.microblog.library.model.microblog.UserListUpdate
import org.mariotaku.twidere.R
import org.mariotaku.twidere.extension.*
import org.mariotaku.twidere.extension.model.api.twitter.setPublic
import org.mariotaku.twidere.promise.UserListPromises
import org.mariotaku.twidere.text.validator.UserListNameValidator

class CreateUserListDialogFragment : BaseDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(context!!)
        builder.setView(R.layout.dialog_user_list_detail_editor)

        builder.setTitle(R.string.new_user_list)
        builder.positive(android.R.string.ok) { dialog ->
            if (dialog.editName.empty) return@positive
            val update = UserListUpdate().apply {
                setName(dialog.editName.string)
                setPublic(dialog.isPublic.isChecked)
                setDescription(dialog.editDescription.string)
            }
            UserListPromises.get(context!!).create(arguments!!.accountKey!!, update)
        }
        val dialog = builder.create()
        dialog.applyOnShow {
            applyTheme()
            val editName = dialog.editName
            editName.addValidator(UserListNameValidator(getString(R.string.invalid_list_name)))
        }
        return dialog
    }

}
