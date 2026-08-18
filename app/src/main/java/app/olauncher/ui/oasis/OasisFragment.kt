package app.olauncher.ui.oasis

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.fragment.findNavController
import app.olauncher.R
import app.olauncher.data.Prefs
import app.olauncher.databinding.FragmentOasisBinding
import app.olauncher.helper.getColorFromAttr
import app.olauncher.ui.BaseFragment

/**
 * Oasis panel — a single scrollable page reached by swiping right on the home screen.
 * Hosts the To-do, Notes, Calendar and Pomodoro blocks and a link to the Oasis settings.
 */
class OasisFragment : BaseFragment() {

    private lateinit var prefs: Prefs

    private var _binding: FragmentOasisBinding? = null
    private val binding get() = _binding!!

    private var todoBlock: OasisTodoBlock? = null
    private var notesBlock: OasisNotesBlock? = null
    private var calendarBlock: OasisCalendarBlock? = null
    private var pomodoroBlock: OasisPomodoroBlock? = null

    private val calendarPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            calendarBlock?.onPermissionResult(granted)
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOasisBinding.inflate(inflater, container, false)
        prefs = Prefs(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bg = requireContext().getColorFromAttr(android.R.attr.colorBackground)
        val textColor = requireContext().getColorFromAttr(android.R.attr.textColorPrimary)
        binding.oasisLayout.setBackgroundColor(bg)
        binding.oasisTitle.setTextColor(textColor)
        binding.oasisSettingsButton.setTextColor(textColor)

        // Keep content clear of the status bar, navigation bar and keyboard.
        val basePaddingTop = binding.oasisContent.paddingTop
        val baseSpacer = binding.oasisBottomSpacer.layoutParams.height
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            binding.oasisContent.updatePadding(top = basePaddingTop + bars.top)
            val lp = binding.oasisBottomSpacer.layoutParams
            lp.height = baseSpacer + maxOf(bars.bottom, ime.bottom)
            binding.oasisBottomSpacer.layoutParams = lp
            insets
        }

        todoBlock = OasisTodoBlock(requireContext(), prefs, binding.todoBlock).also { it.bind() }
        notesBlock = OasisNotesBlock(requireContext(), prefs, binding.notesBlock).also { it.bind() }
        calendarBlock = OasisCalendarBlock(requireContext(), prefs, binding.calendarBlock) {
            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }.also { it.bind() }
        pomodoroBlock = OasisPomodoroBlock(requireContext(), prefs, binding.pomoBlock).also { it.bind() }

        binding.oasisSettingsButton.setOnClickListener { openSettings() }

        applyBlockVisibility()
    }

    override fun onResume() {
        super.onResume()
        applyBlockVisibility()
        todoBlock?.reload()
        notesBlock?.reload()
        calendarBlock?.refresh()
    }

    override fun onPause() {
        super.onPause()
        notesBlock?.flush()
    }

    private fun applyBlockVisibility() {
        binding.todoBlock.root.isVisible = prefs.oasisShowTodo
        binding.notesBlock.root.isVisible = prefs.oasisShowNotes
        binding.calendarBlock.root.isVisible = prefs.oasisShowCalendar
        binding.pomoBlock.root.isVisible = prefs.oasisShowPomodoro
    }

    private fun openSettings() {
        if (findNavController().currentDestination?.id == R.id.oasisFragment) {
            findNavController().navigate(R.id.action_oasisFragment_to_oasisSettingsFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pomodoroBlock?.stop()
        todoBlock = null
        notesBlock = null
        calendarBlock = null
        pomodoroBlock = null
        _binding = null
    }
}
