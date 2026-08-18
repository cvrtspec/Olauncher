package app.olauncher.ui.oasis

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.text.format.DateFormat
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import app.olauncher.R
import app.olauncher.data.Prefs
import app.olauncher.databinding.OasisCalendarBinding
import app.olauncher.databinding.OasisEventRowBinding
import app.olauncher.helper.getColorFromAttr
import app.olauncher.helper.openCalendar
import app.olauncher.helper.showToast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Calendar block: a per-day agenda read from CalendarContract.Instances (READ_CALENDAR,
 * requested at runtime), with date navigation, a jump-to-today control, an "open calendar
 * app" shortcut (Olauncher's openCalendar helper), and a refresh.
 */
class OasisCalendarBlock(
    private val context: Context,
    private val prefs: Prefs,
    private val binding: OasisCalendarBinding,
    private val requestPermission: () -> Unit
) {
    private data class EventRow(val title: String, val begin: Long, val allDay: Boolean)

    private val day: Calendar = Calendar.getInstance().apply { toDayStart() }
    private val dateFormat = SimpleDateFormat("d MMM ''yy", Locale.getDefault())

    private val textColor get() = context.getColorFromAttr(android.R.attr.textColorPrimary)

    fun bind() {
        binding.apply {
            calHeader.setTextColor(textColor)
            calToday.setTextColor(textColor)
            calOpen.setTextColor(textColor)
            calRefresh.setTextColor(textColor)
            calPrev.setTextColor(textColor)
            calNext.setTextColor(textColor)
            calDate.setTextColor(textColor)
            calEmpty.setTextColor(textColor)
            calPermission.setTextColor(textColor)

            calPrev.setOnClickListener { shiftDay(-1) }
            calNext.setOnClickListener { shiftDay(1) }
            calToday.setOnClickListener {
                day.timeInMillis = System.currentTimeMillis()
                day.toDayStart()
                refresh()
            }
            calOpen.setOnClickListener { openCalendar(context) }
            calRefresh.setOnClickListener { refresh() }
            calPermission.setOnClickListener { requestPermission() }
        }
        refresh()
    }

    fun refresh() {
        binding.calDate.text = dateFormat.format(day.time)
        val granted = hasPermission()
        binding.calPermission.isVisible = !granted
        if (!granted) {
            binding.eventsContainer.removeAllViews()
            binding.calEmpty.isVisible = false
            return
        }
        render(query())
    }

    fun onPermissionResult(granted: Boolean) {
        if (!granted) context.showToast(context.getString(R.string.oasis_permission_required))
        refresh()
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    private fun shiftDay(delta: Int) {
        day.add(Calendar.DAY_OF_MONTH, delta)
        refresh()
    }

    private fun query(): List<EventRow> {
        val start = day.clone() as Calendar
        val startMillis = start.timeInMillis
        val end = start.clone() as Calendar
        end.add(Calendar.DAY_OF_MONTH, 1)
        val endMillis = end.timeInMillis

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(uri, startMillis)
        ContentUris.appendId(uri, endMillis)

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.ALL_DAY
        )
        val selection = "${CalendarContract.Instances.VISIBLE} = 1"
        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

        val rows = mutableListOf<EventRow>()
        try {
            context.contentResolver.query(uri.build(), projection, selection, null, sortOrder)?.use { c ->
                val ti = c.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val bi = c.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val ai = c.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                while (c.moveToNext()) {
                    rows.add(EventRow(c.getString(ti) ?: "", c.getLong(bi), c.getInt(ai) == 1))
                }
            }
        } catch (_: Exception) {
        }
        return rows
    }

    private fun render(rows: List<EventRow>) {
        val container = binding.eventsContainer
        container.removeAllViews()
        binding.calEmpty.isVisible = rows.isEmpty()

        val inflater = LayoutInflater.from(context)
        val timeFormat = DateFormat.getTimeFormat(context)
        val allDay = context.getString(R.string.oasis_calendar_all_day)
        val untitled = context.getString(R.string.oasis_calendar_untitled)
        rows.forEach { e ->
            val row = OasisEventRowBinding.inflate(inflater, container, false)
            row.eventTime.text = if (e.allDay) allDay else timeFormat.format(Date(e.begin))
            row.eventTime.setTextColor(textColor)
            row.eventTitle.text = e.title.ifBlank { untitled }
            row.eventTitle.setTextColor(textColor)
            container.addView(row.root)
        }
    }

    private fun Calendar.toDayStart() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
