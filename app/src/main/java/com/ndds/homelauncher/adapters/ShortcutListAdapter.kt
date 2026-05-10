package com.ndds.homelauncher.adapters

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Process
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.ndds.homelauncher.utils.SheetDialog
import com.ndds.homelauncher.widgets.ModalItem

class ShortcutListAdapter(val sheetDialog: SheetDialog,
                          val packageName: String,
                          context: Context,
                          val data: List<ShortcutInfo>
): ArrayAdapter<ShortcutInfo>(context,0,data) {
    val launcherApps = context.getSystemService(LauncherApps::class.java)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var row: ModalItem
        if (convertView == null)
            row = ModalItem(context)
        else
            row = convertView as ModalItem
        val shortcut = data[position]
        val iconDrawable = launcherApps.getShortcutIconDrawable(shortcut, 0)
        if (iconDrawable != null)
            row.setIcon(iconDrawable)
        row.setLabel(shortcut.shortLabel!!)
        row.setOnClickListener { v ->
            sheetDialog.dismiss()
            context.getSystemService(LauncherApps::class.java)
                .startShortcut(
                    packageName,
                    shortcut.id,
                    null,
                    null,
                    Process.myUserHandle()
                )
        }
        return row
    }
}