package app.olauncher.ui.oasis

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.findNavController
import app.olauncher.R
import app.olauncher.data.Prefs
import app.olauncher.databinding.FragmentOasisBinding
import app.olauncher.ui.BaseFragment

/**
 * Oasis panel — a single scrollable page reached by swiping right on the home screen; swiping
 * left on the panel returns home. Hosts the To-do, Notes, Calendar and Pomodoro blocks and a
 * link to the Oasis settings.
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

        // Swipe left anywhere on the panel returns to the home screen (the panel is opened by
        // swiping right on the home screen).
        binding.oasisLayout.onSwipeLeft = { close() }

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
    }

    override fun onResume() {
        super.onResume()
        todoBlock?.reload()
        notesBlock?.reload()
        calendarBlock?.refresh()
    }

    override fun onPause() {
        super.onPause()
        notesBlock?.flush()
    }

    private fun close() {
        if (findNavController().currentDestination?.id == R.id.oasisFragment) {
            findNavController().popBackStack()
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
