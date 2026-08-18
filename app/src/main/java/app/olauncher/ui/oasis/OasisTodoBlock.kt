package app.olauncher.ui.oasis

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.view.isVisible
import app.olauncher.data.Prefs
import app.olauncher.databinding.OasisTodoBinding
import app.olauncher.databinding.OasisTodoRowBinding
import org.json.JSONArray
import org.json.JSONObject

/**
 * To-do block: a checkable list persisted as a JSON array in SharedPreferences
 * (Prefs.oasisTodos). Matches the Oasis layout: checkbox + text + delete, with an
 * add row underneath.
 */
class OasisTodoBlock(
    private val context: Context,
    private val prefs: Prefs,
    private val binding: OasisTodoBinding
) {
    private data class Item(var text: String, var done: Boolean)

    private val items = mutableListOf<Item>()

    fun bind() {
        binding.apply {
            todoAdd.setOnClickListener { addItem() }
            todoInput.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addItem(); true
                } else false
            }
        }
        reload()
    }

    fun reload() {
        items.clear()
        try {
            val arr = JSONArray(prefs.oasisTodos)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                items.add(Item(o.optString("t"), o.optBoolean("d", false)))
            }
        } catch (_: Exception) {
        }
        render()
    }

    private fun persist() {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("t", it.text).put("d", it.done)) }
        prefs.oasisTodos = arr.toString()
    }

    private fun render() {
        val container = binding.todoContainer
        container.removeAllViews()
        binding.todoEmpty.isVisible = items.isEmpty()

        val inflater = LayoutInflater.from(context)
        items.forEach { item ->
            val row = OasisTodoRowBinding.inflate(inflater, container, false)
            row.todoText.text = item.text
            applyDoneStyle(row.todoText, item.done)

            row.todoCheck.isChecked = item.done
            row.todoCheck.setOnCheckedChangeListener { _, checked ->
                item.done = checked
                persist()
                applyDoneStyle(row.todoText, checked)
            }

            row.todoDelete.setOnClickListener {
                items.remove(item)
                persist()
                render()
            }
            container.addView(row.root)
        }
    }

    private fun applyDoneStyle(view: TextView, done: Boolean) {
        view.paintFlags = if (done) view.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else view.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        view.alpha = if (done) 0.5f else 1f
    }

    private fun addItem() {
        val text = binding.todoInput.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        items.add(Item(text, false))
        persist()
        binding.todoInput.text?.clear()
        render()
    }
}
