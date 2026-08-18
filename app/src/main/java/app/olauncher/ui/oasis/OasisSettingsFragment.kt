package app.olauncher.ui.oasis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import app.olauncher.R
import app.olauncher.data.Prefs
import app.olauncher.databinding.FragmentOasisSettingsBinding
import app.olauncher.helper.getColorFromAttr
import app.olauncher.ui.BaseFragment

/** Per-block show/hide toggles for the Oasis panel. */
class OasisSettingsFragment : BaseFragment() {

    private lateinit var prefs: Prefs

    private var _binding: FragmentOasisSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOasisSettingsBinding.inflate(inflater, container, false)
        prefs = Prefs(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bg = requireContext().getColorFromAttr(android.R.attr.colorBackground)
        val textColor = requireContext().getColorFromAttr(android.R.attr.textColorPrimary)
        binding.oasisSettingsLayout.setBackgroundColor(bg)
        binding.settingsTitle.setTextColor(textColor)
        listOf(binding.toggleTodo, binding.toggleNotes, binding.toggleCalendar, binding.togglePomodoro)
            .forEach { it.setTextColor(textColor) }

        bindRow(binding.toggleTodo, R.string.oasis_show_todo, prefs.oasisShowTodo) {
            prefs.oasisShowTodo = it
        }
        bindRow(binding.toggleNotes, R.string.oasis_show_notes, prefs.oasisShowNotes) {
            prefs.oasisShowNotes = it
        }
        bindRow(binding.toggleCalendar, R.string.oasis_show_calendar, prefs.oasisShowCalendar) {
            prefs.oasisShowCalendar = it
        }
        bindRow(binding.togglePomodoro, R.string.oasis_show_pomodoro, prefs.oasisShowPomodoro) {
            prefs.oasisShowPomodoro = it
        }
    }

    private fun bindRow(row: TextView, labelRes: Int, initial: Boolean, onChange: (Boolean) -> Unit) {
        var state = initial
        setRowText(row, labelRes, state)
        row.setOnClickListener {
            state = !state
            onChange(state)
            setRowText(row, labelRes, state)
        }
    }

    private fun setRowText(row: TextView, labelRes: Int, on: Boolean) {
        val status = getString(if (on) R.string.on else R.string.off)
        row.text = "${getString(labelRes)}:  $status"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
