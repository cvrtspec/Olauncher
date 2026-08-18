package app.olauncher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import app.olauncher.MainViewModel
import app.olauncher.R
import app.olauncher.data.Prefs
import app.olauncher.databinding.FragmentHomeAppsBinding
import app.olauncher.databinding.HomeAppRowBinding

/**
 * Settings -> Home apps: the current home list with move up / move down / remove per row.
 * Apps are added from the app list (long-press -> Home).
 */
class HomeAppsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private var _binding: FragmentHomeAppsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeAppsBinding.inflate(inflater, container, false)
        prefs = Prefs(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
        populate()
    }

    private fun populate() {
        binding.homeAppsContainer.removeAllViews()
        val count = prefs.homeAppsNum
        binding.homeAppsEmpty.isVisible = count == 0
        for (i in 1..count) {
            val row = HomeAppRowBinding.inflate(layoutInflater, binding.homeAppsContainer, false)
            row.homeAppName.text = prefs.getAppName(i).ifBlank { getString(R.string.app) }
            row.homeAppUp.alpha = if (i == 1) 0.3f else 1f
            row.homeAppDown.alpha = if (i == count) 0.3f else 1f
            row.homeAppUp.setOnClickListener {
                if (i > 1) {
                    prefs.moveHomeApp(i, i - 1)
                    changed()
                }
            }
            row.homeAppDown.setOnClickListener {
                if (i < count) {
                    prefs.moveHomeApp(i, i + 1)
                    changed()
                }
            }
            row.homeAppRemove.setOnClickListener {
                prefs.removeHomeApp(i)
                changed()
            }
            binding.homeAppsContainer.addView(row.root)
        }
    }

    private fun changed() {
        viewModel.refreshHome(true)
        populate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
