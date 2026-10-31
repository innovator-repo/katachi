package app.katachi

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import app.katachi.core.StatusRow
import java.time.LocalDate

/** Read-only `content://app.katachi.provider/status`: one row of today's progress for the other apps. */
class StatusProvider : ContentProvider() {
    override fun onCreate() = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, order: String?): Cursor? {
        val context = context ?: return null
        val today = LocalDate.now()
        val solved = context.store.history.value.day(today).results.size
        val target = context.lumeState.target(today)
        val all = StatusRow.of(solved, target, context.store.history.value.streak(today))
        val columns = projection?.filter { it in all }?.toTypedArray()?.takeIf { it.isNotEmpty() } ?: all.keys.toTypedArray()
        return MatrixCursor(columns).apply { addRow(columns.map { all.getValue(it) }) }
    }

    override fun getType(uri: Uri) = "vnd.android.cursor.item/vnd.app.katachi.status"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = 0
}
