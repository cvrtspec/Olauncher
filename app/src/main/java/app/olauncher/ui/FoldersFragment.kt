package app.olauncher.ui

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import app.olauncher.MainViewModel
import app.olauncher.R
import app.olauncher.data.Constants
import app.olauncher.data.Prefs
import app.olauncher.databinding.FolderAddRowBinding
import app.olauncher.databinding.FolderAppRowBinding
import app.olauncher.databinding.FolderManageRowBinding
import app.olauncher.databinding.FragmentFoldersBinding
import app.olauncher.helper.showToast

/**
 * Settings -> Folders: every folder with the apps inside it, in the style of the Home apps manager.
 * Folders and their apps move up / down (the app order is the order of the folder window on the home
 * screen); an app can be removed from its folder, moved to another folder, or added through an app
 * picker; folders are created, renamed, deleted and put on / taken off the home screen here.
 * Works for home-placed folders too, which have no row in the app list.
 */
class FoldersFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private var _binding: FragmentFoldersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoldersBinding.inflate(inflater, container, false)
        prefs = Prefs(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")
        binding.foldersNew.setOnClickListener {
            showFolderNameDialog(R.string.folder_new, "") { name ->
                prefs.addFolder(name)
                populate()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Also runs when the "Add app" picker pops back to this screen.
        populate()
    }

    private fun populate() {
        val container = binding.foldersContainer
        container.removeAllViews()
        val folders = prefs.getFolders()
        binding.foldersEmpty.isVisible = folders.isEmpty()
        val pm = requireContext().packageManager
        for ((fi, folder) in folders.withIndex()) {
            val header = FolderManageRowBinding.inflate(layoutInflater, container, false)
            header.folderManageName.text = folder.name.ifBlank { getString(R.string.folder) }
            val count = resources.getQuantityString(R.plurals.folder_app_count, folder.apps.size, folder.apps.size)
            header.folderManageMeta.text =
                if (prefs.homeContainsFolder(folder.id)) getString(R.string.folder_meta_on_home, count) else count
            header.folderManageUp.alpha = if (fi == 0) 0.3f else 1f
            header.folderManageDown.alpha = if (fi == folders.lastIndex) 0.3f else 1f
            header.folderManageUp.setOnClickListener {
                if (fi > 0) {
                    prefs.moveFolder(fi, fi - 1)
                    populate()
                }
            }
            header.folderManageDown.setOnClickListener {
                if (fi < folders.lastIndex) {
                    prefs.moveFolder(fi, fi + 1)
                    populate()
                }
            }
            header.root.setOnClickListener { showFolderActions(folder) }
            header.folderManageMore.setOnClickListener { showFolderActions(folder) }
            container.addView(header.root)

            for ((ai, key) in folder.apps.withIndex()) {
                val row = FolderAppRowBinding.inflate(layoutInflater, container, false)
                val label = appLabel(folder, key, pm)
                row.folderAppName.text = label
                row.folderAppUp.alpha = if (ai == 0) 0.3f else 1f
                row.folderAppDown.alpha = if (ai == folder.apps.lastIndex) 0.3f else 1f
                row.folderAppUp.setOnClickListener {
                    if (ai > 0) {
                        prefs.moveFolderApp(folder.id, ai, ai - 1)
                        populate()
                    }
                }
                row.folderAppDown.setOnClickListener {
                    if (ai < folder.apps.lastIndex) {
                        prefs.moveFolderApp(folder.id, ai, ai + 1)
                        populate()
                    }
                }
                row.folderAppRemove.setOnClickListener {
                    prefs.removeAppFromFolders(key)
                    requireContext().showToast(getString(R.string.folder_removed_from))
                    populate()
                }
                row.root.setOnClickListener { showMoveDialog(folder, key, label) }
                container.addView(row.root)
            }

            val add = FolderAddRowBinding.inflate(layoutInflater, container, false)
            add.root.setOnClickListener {
                viewModel.getAppList(true)
                findNavController().navigate(
                    R.id.action_foldersFragment_to_appListFragment,
                    bundleOf(
                        Constants.Key.FLAG to Constants.FLAG_ADD_TO_FOLDER,
                        Constants.Key.FOLDER to folder.id
                    )
                )
            }
            container.addView(add.root)
        }
    }

    /** Same resolution as the home-screen folder window: rename label, cached label, then the installed label. */
    private fun appLabel(folder: Prefs.Folder, key: String, pm: PackageManager): String {
        val packageName = key.substringBefore("|")
        return prefs.getAppRenameLabel(packageName).ifBlank { folder.labels[key] ?: "" }.ifBlank {
            try {
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            } catch (e: Exception) {
                packageName
            }
        }
    }

    /** Tap on an app row: move it to another (or a new) folder. */
    private fun showMoveDialog(from: Prefs.Folder, key: String, label: String) {
        val others = prefs.getFolders().filter { it.id != from.id }
        val items = others.map { it.name.ifBlank { getString(R.string.folder) } } + getString(R.string.folder_new)
        val cachedLabel = from.labels[key] ?: ""
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.folder_choose) + " – " + label)
            .setItems(items.toTypedArray()) { _, which ->
                if (which == others.size) {
                    showFolderNameDialog(R.string.folder_new, "") { name ->
                        val folder = prefs.addFolder(name)
                        prefs.assignAppToFolder(folder.id, key, cachedLabel)
                        populate()
                    }
                } else {
                    prefs.assignAppToFolder(others[which].id, key, cachedLabel)
                    populate()
                }
            }
            .show()
    }

    /** Tap on a folder row: home screen on/off, rename, delete - the same actions as in the app list. */
    private fun showFolderActions(folder: Prefs.Folder) {
        val onHome = prefs.homeContainsFolder(folder.id)
        val actions = arrayOf(
            getString(if (onHome) R.string.folder_remove_from_home else R.string.folder_add_to_home),
            getString(R.string.folder_rename),
            getString(R.string.folder_delete)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(folder.name.ifBlank { getString(R.string.folder) })
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> {
                        if (onHome) prefs.removeFolderFromHome(folder.id)
                        else if (!prefs.addFolderToHome(folder)) requireContext().showToast(getString(R.string.home_apps_full))
                        homeChanged()
                    }

                    1 -> showFolderNameDialog(R.string.folder_rename, folder.name) { name ->
                        prefs.renameFolder(folder.id, name)
                        homeChanged()
                    }

                    2 -> {
                        prefs.deleteFolder(folder.id)
                        homeChanged()
                    }
                }
            }
            .show()
    }

    /** Folder name input; blank names (or spaces) are allowed and show as an icon-only folder. */
    private fun showFolderNameDialog(titleRes: Int, initial: String, onDone: (String) -> Unit) {
        val input = EditText(requireContext())
        input.setSingleLine(true)
        input.hint = getString(R.string.folder_name_hint)
        input.setText(initial)
        input.setSelection(initial.length)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val wrapper = FrameLayout(requireContext())
        wrapper.setPadding(pad, pad / 2, pad, 0)
        wrapper.addView(input)
        AlertDialog.Builder(requireContext())
            .setTitle(titleRes)
            .setView(wrapper)
            .setPositiveButton(android.R.string.ok) { _, _ -> onDone(input.text.toString().replace("\n", "")) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** The home list changed (folder name, slot added or removed): redraw home, then this screen. */
    private fun homeChanged() {
        viewModel.refreshHome(true)
        populate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
