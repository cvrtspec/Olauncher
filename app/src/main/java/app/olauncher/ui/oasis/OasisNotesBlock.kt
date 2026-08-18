package app.olauncher.ui.oasis

import android.content.Context
import app.olauncher.data.Prefs
import app.olauncher.databinding.OasisNotesBinding
import app.olauncher.helper.getColorFromAttr
import org.json.JSONArray

/**
 * Notes block: [PAGE_COUNT] free-text pages with a "1/5" pager, matching Oasis.
 * The pages are stored as a JSON string array in SharedPreferences (Prefs.oasisNotes).
 */
class OasisNotesBlock(
    private val context: Context,
    private val prefs: Prefs,
    private val binding: OasisNotesBinding
) {
    private val pages = MutableList(PAGE_COUNT) { "" }
    private var index = 0

    private val textColor get() = context.getColorFromAttr(android.R.attr.textColorPrimary)
    private val hintColor get() = context.getColorFromAttr(android.R.attr.textColorHint)

    fun bind() {
        binding.apply {
            notesHeader.setTextColor(textColor)
            notesPrev.setTextColor(textColor)
            notesPage.setTextColor(textColor)
            notesNext.setTextColor(textColor)
            notesSave.setTextColor(textColor)
            notesInput.setTextColor(textColor)
            notesInput.setHintTextColor(hintColor)

            notesPrev.setOnClickListener { showPage(index - 1) }
            notesNext.setOnClickListener { showPage(index + 1) }
            notesSave.setOnClickListener {
                captureCurrent()
                persist()
            }
        }
        reload()
    }

    fun reload() {
        for (i in 0 until PAGE_COUNT) pages[i] = ""
        try {
            val arr = JSONArray(prefs.oasisNotes)
            for (i in 0 until minOf(arr.length(), PAGE_COUNT)) pages[i] = arr.optString(i)
        } catch (_: Exception) {
        }
        index = 0
        showPage(0)
    }

    /** Persist the currently-edited page; call from the fragment's onPause. */
    fun flush() {
        captureCurrent()
        persist()
    }

    private fun captureCurrent() {
        pages[index] = binding.notesInput.text?.toString().orEmpty()
    }

    private fun showPage(target: Int) {
        captureCurrent()
        index = ((target % PAGE_COUNT) + PAGE_COUNT) % PAGE_COUNT
        binding.notesInput.setText(pages[index])
        binding.notesInput.setSelection(binding.notesInput.text?.length ?: 0)
        binding.notesPage.text = "${index + 1}/$PAGE_COUNT"
    }

    private fun persist() {
        val arr = JSONArray()
        pages.forEach { arr.put(it) }
        prefs.oasisNotes = arr.toString()
    }

    companion object {
        private const val PAGE_COUNT = 5
    }
}
