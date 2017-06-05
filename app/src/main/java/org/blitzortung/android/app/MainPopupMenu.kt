package org.blitzortung.android.app

import android.app.Dialog
import android.content.Context
import android.support.v7.widget.PopupMenu
import android.util.Log
import android.view.View
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import org.blitzortung.android.app.components.VersionComponent
import org.blitzortung.android.common.util.LOG_TAG
import org.blitzortung.android.dialogs.AlertDialog
import org.blitzortung.android.dialogs.AlertDialogColorHandler
import org.blitzortung.android.dialogs.InfoDialog
import org.blitzortung.android.dialogs.LogDialog
import org.jetbrains.anko.startActivity

class MainPopupMenu(private val context: Context, anchor: View) : PopupMenu(context, anchor) {
    init {
        setOnMenuItemClickListener { item ->
            val versionComponent = VersionComponent(context)
            if (item.itemId == R.id.menu_preferences) {
                context.startActivity<Preferences>()
            } else {
                val dialog = when (item.itemId) {
                    R.id.menu_info -> InfoDialog(context, versionComponent)

                    R.id.menu_alarms -> AlertDialog(context, AlertDialogColorHandler(context.appKodein().instance()))

                    R.id.menu_log -> LogDialog(context)

                    else -> null
                }

                if (dialog is Dialog) {
                    dialog.show()
                } else {
                    return@setOnMenuItemClickListener false
                }
            }

            return@setOnMenuItemClickListener true
        }
    }

    fun showPopupMenu() {
        inflate(R.menu.main_menu)
        show()
        Log.v(LOG_TAG, "MainPopupMenu.showPopupMenu()")
    }
}
