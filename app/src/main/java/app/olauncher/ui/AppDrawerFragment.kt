package app.olauncher.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.text.Spannable
import android.view.Gravity
import androidx.appcompat.app.AlertDialog
import android.widget.FrameLayout
import android.widget.EditText
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.activityViewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import app.olauncher.databinding.FolderRowBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import app.olauncher.MainViewModel
import app.olauncher.R
import app.olauncher.data.AppModel
import app.olauncher.data.Constants
import app.olauncher.data.Prefs
import app.olauncher.databinding.FragmentAppDrawerBinding
import app.olauncher.helper.deletePinnedShortcut
import app.olauncher.helper.hideKeyboard
import app.olauncher.helper.isEinkDisplay
import app.olauncher.helper.isSystemAnimationsDisabled
import app.olauncher.helper.isSystemApp
import app.olauncher.helper.openAppInfo
import app.olauncher.helper.openSearch
import app.olauncher.helper.openUrl
import app.olauncher.helper.showKeyboard
import app.olauncher.helper.showToast
import app.olauncher.helper.uninstall

class AppDrawerFragment : BaseFragment() {

    private lateinit var prefs: Prefs
    private lateinit var adapter: AppDrawerAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var searchTextView: TextView? = null
    private var cachedIsCjkKeyboard: Boolean? = null

    private var flag = Constants.FLAG_LAUNCH_APP
    private var canRename = false
    private var currentFolderId: String? = null
    private var currentAppList: List<AppModel>? = null
    private var currentPrivateSpaceApps: List<AppModel>? = null
    private var currentPrivateSpaceLocked: Boolean = true
    private var currentPrivateSpaceAvailable: Boolean = false

    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentAppDrawerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAppDrawerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        arguments?.let {
            flag = it.getInt(Constants.Key.FLAG, Constants.FLAG_LAUNCH_APP)
            canRename = it.getBoolean(Constants.Key.RENAME, false)
            currentFolderId = it.getString(Constants.Key.FOLDER)
        }

        initViews()
        initSearch()
        initAdapter()
        initObservers()
        initClickListeners()
        renderFolders()
    }

    private fun initViews() {
        // The settings gear only belongs to the plain app list, not to the "pick an app" pickers.
        binding.appSettings.isVisible = flag == Constants.FLAG_LAUNCH_APP
        if (flag == Constants.FLAG_HIDDEN_APPS)
            binding.search.queryHint = getString(R.string.hidden_apps)
        else if (flag in Constants.FLAG_SET_HOME_APP_1..Constants.FLAG_SET_CALENDAR_APP
            || flag in Constants.FLAG_SET_DOCK_1..Constants.FLAG_SET_DOCK_3)
            binding.search.queryHint = "Please select an app"
        try {
            searchTextView = binding.search.findViewById(R.id.search_src_text)
            searchTextView?.gravity = Gravity.START
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initSearch() {
        binding.search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query?.startsWith("!") == true)
                    requireContext().openUrl(Constants.URL_DUCK_SEARCH + query.replace(" ", "%20"))
                else if (adapter.itemCount == 0)
                    requireContext().openSearch(query?.trim())
                else
                    adapter.launchFirstInList()
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                try {
                    adapter.allowAutoLaunch = !isSearchComposing()
                    adapter.filter.filter(newText)
                    binding.appRename.visibility =
                        if (canRename && newText.isNotBlank()) View.VISIBLE else View.GONE
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return false
            }
        })
    }

    private fun isSearchComposing(): Boolean {
        val text = searchTextView?.text
        if (text !is Spannable) return false
        val start = BaseInputConnection.getComposingSpanStart(text)
        val end = BaseInputConnection.getComposingSpanEnd(text)
        if (start !in 0 until end) return false
        return isCjkKeyboard()
    }

    private fun isCjkKeyboard(): Boolean {
        cachedIsCjkKeyboard?.let { return it }
        val result = try {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val subtype = imm.currentInputMethodSubtype
            val language = when {
                subtype == null -> ""
                subtype.languageTag.isNotEmpty() -> subtype.languageTag // e.g. "zh-CN", "ja-JP", "en-US"
                else -> subtype.locale // deprecated fallback, e.g. "zh_CN"
            }
            language.startsWith("zh") || language.startsWith("ja") || language.startsWith("ko")
        } catch (e: Exception) {
            false
        }
        cachedIsCjkKeyboard = result
        return result
    }

    private fun initAdapter() {
        adapter = AppDrawerAdapter(
            flag,
            Gravity.START,
            appClickListener = { appModel ->
                viewModel.selectedApp(appModel, flag)
                if (flag == Constants.FLAG_LAUNCH_APP || flag == Constants.FLAG_HIDDEN_APPS)
                    findNavController().popBackStack(R.id.mainFragment, false)
                else
                    findNavController().popBackStack()
            },
            appAddHomeListener = { appModel ->
                val message = viewModel.addToHome(appModel)
                if (message != 0) requireContext().showToast(getString(message))
                if (message == R.string.home_app_added) updateCombinedAppList()
            },
            appFolderListener = { appModel -> onFolderAction(appModel) },
            appInfoListener = {
                openAppInfo(
                    requireContext(),
                    it.user,
                    it.appPackage
                )
                findNavController().popBackStack(R.id.mainFragment, false)
            },
            appDeleteListener = { appModel ->
                when (appModel) {
                    is AppModel.PrivateSpaceHeader -> {}
                    is AppModel.PinnedShortcut ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                            requireContext().deletePinnedShortcut(
                                packageName = appModel.appPackage,
                                shortcutIdToDelete = appModel.shortcutId,
                                user = appModel.user,
                            )
                        }

                    is AppModel.App -> {
                        if (appModel.user != Process.myUserHandle()) {
                            openAppInfo(requireContext(), appModel.user, appModel.appPackage)
                        } else if (requireContext().isSystemApp(appModel.appPackage, appModel.user)) {
                            requireContext().showToast(getString(R.string.system_app_cannot_delete))
                            openAppInfo(requireContext(), appModel.user, appModel.appPackage)
                        } else {
                            requireContext().uninstall(appModel.appPackage)
                        }
                    }
                }
                viewModel.getAppList()
            },
            appHideListener = { appModel, position ->
                if (appModel is AppModel.PinnedShortcut) {
                    requireContext().showToast("Hiding pinned shortcuts is not supported")
                    return@AppDrawerAdapter
                }
                adapter.appFilteredList.removeAt(position)
                adapter.notifyItemRemoved(position)
                adapter.appsList.remove(appModel)

                val newSet = mutableSetOf<String>()
                newSet.addAll(prefs.hiddenApps)
                if (flag == Constants.FLAG_HIDDEN_APPS)
                    newSet.remove(appModel.appPackage + "|" + appModel.user.toString())
                else
                    newSet.add(appModel.appPackage + "|" + appModel.user.toString())

                prefs.hiddenApps = newSet
                if (newSet.isEmpty())
                    findNavController().popBackStack()
                if (prefs.firstHide) {
                    binding.search.hideKeyboard()
                    prefs.firstHide = false
                    viewModel.showDialog.postValue(Constants.Dialog.HIDDEN)
                    findNavController().navigate(R.id.action_appListFragment_to_settingsFragment2)
                }
                viewModel.getAppList()
                viewModel.getHiddenApps()
            },
            appRenameListener = { appModel, renameLabel ->
                val identifier = when (appModel) {
                    is AppModel.PinnedShortcut -> appModel.shortcutId
                    is AppModel.App -> appModel.appPackage
                    else -> return@AppDrawerAdapter
                }
                prefs.setAppRenameLabel(identifier, renameLabel)
                viewModel.getAppList()
            },
            privateSpaceToggleListener = {
                viewModel.togglePrivateSpaceLock()
            },
            privateSpaceSettingsListener = {
                viewModel.openPrivateSpaceSettings()
                findNavController().popBackStack(R.id.mainFragment, false)
            }
        )

        linearLayoutManager = object : LinearLayoutManager(requireContext()) {
            override fun scrollVerticallyBy(
                dx: Int,
                recycler: Recycler,
                state: RecyclerView.State,
            ): Int {
                val scrollRange = super.scrollVerticallyBy(dx, recycler, state)
                val overScroll = dx - scrollRange
                if (overScroll < -10 && binding.recyclerView.scrollState == RecyclerView.SCROLL_STATE_DRAGGING)
                    checkMessageAndExit()
                return scrollRange
            }
        }

        binding.recyclerView.layoutManager = linearLayoutManager
        binding.recyclerView.adapter = adapter
        ContextCompat.getDrawable(requireContext(), R.drawable.fast_scroll_thumb)?.let { grip ->
            val density = resources.displayMetrics.density
            QuickScroller(binding.recyclerView, grip, (34 * density).toInt(), (52 * density).toInt(), (4 * density).toInt())
        }
        binding.recyclerView.addOnScrollListener(getRecyclerViewOnScrollListener())
        binding.recyclerView.itemAnimator = null
        if (requireContext().isEinkDisplay().not() && requireContext().isSystemAnimationsDisabled().not())
            binding.recyclerView.layoutAnimation =
                AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_anim_from_bottom)
    }

    private fun initObservers() {
        viewModel.firstOpen.observe(viewLifecycleOwner) {
        }
        if (flag == Constants.FLAG_HIDDEN_APPS) {
            viewModel.hiddenApps.observe(viewLifecycleOwner) {
                it?.let {
                    adapter.setAppList(it.toMutableList())
                }
            }
        } else {
            viewModel.appList.observe(viewLifecycleOwner) {
                currentAppList = it
                updateCombinedAppList()
            }
            if (flag == Constants.FLAG_LAUNCH_APP) {
                viewModel.privateSpaceAvailable.observe(viewLifecycleOwner) {
                    currentPrivateSpaceAvailable = it
                    updateCombinedAppList()
                }
                viewModel.privateSpaceLocked.observe(viewLifecycleOwner) {
                    currentPrivateSpaceLocked = it
                    updateCombinedAppList()
                }
                viewModel.privateSpaceApps.observe(viewLifecycleOwner) {
                    currentPrivateSpaceApps = it
                    updateCombinedAppList()
                }
            }
        }
    }

    private fun updateCombinedAppList() {
        val apps = currentAppList ?: return
        val combined = apps.toMutableList()

        if (flag == Constants.FLAG_LAUNCH_APP && currentPrivateSpaceAvailable) {
            combined.add(AppModel.PrivateSpaceHeader(isLocked = currentPrivateSpaceLocked))
            if (!currentPrivateSpaceLocked) {
                currentPrivateSpaceApps?.let { combined.addAll(it) }
            }
        }

        // Folders: in the plain list, apps that live in a folder are shown only inside that folder.
        val listForMode: MutableList<AppModel> = if (flag != Constants.FLAG_LAUNCH_APP) combined else {
            val folderId = currentFolderId
            if (folderId != null) {
                val members = prefs.getFolders().find { it.id == folderId }?.apps?.toSet() ?: emptySet()
                combined.filter { it is AppModel.App && folderKey(it) in members }.toMutableList()
            } else {
                // Plain list: apps that live in a folder are reachable through the folder, and apps,
                // shortcuts and folders that sit on the home screen are reachable from home.
                val foldered = prefs.folderedAppKeys()
                val homeApps = prefs.homeAppKeys()
                val homeShortcuts = prefs.homeShortcutKeys()
                combined.filter {
                    when (it) {
                        is AppModel.App -> folderKey(it) !in foldered && folderKey(it) !in homeApps
                        is AppModel.PinnedShortcut -> "${it.appPackage}|${it.user}|${it.shortcutId}" !in homeShortcuts
                        else -> true
                    }
                }.toMutableList()
            }
        }
        adapter.inFolder = flag == Constants.FLAG_LAUNCH_APP && currentFolderId != null
        adapter.setAppList(listForMode)
        adapter.filter.filter(binding.search.query)
    }

    // ---------------- Folders (plain app list only) ----------------

    private fun folderKey(app: AppModel.App) = "${app.appPackage}|${app.user}"

    private fun folderLabel(folder: Prefs.Folder) = folder.name

    private fun renderFolders() {
        val container = binding.foldersContainer
        container.removeAllViews()
        if (flag != Constants.FLAG_LAUNCH_APP) {
            container.isVisible = false
            return
        }
        val folders = prefs.getFolders()
        val current = currentFolderId?.let { id -> folders.find { it.id == id } }
        if (currentFolderId != null && current == null) currentFolderId = null
        if (current != null) {
            val row = FolderRowBinding.inflate(layoutInflater, container, false)
            row.folderName.text = getString(R.string.folder_back, folderLabel(current))
            row.folderCount.text = current.apps.size.toString()
            row.root.setOnClickListener { exitFolder() }
            row.root.setOnLongClickListener { showFolderActions(current); true }
            container.addView(row.root)
            container.isVisible = true
            binding.search.queryHint = folderLabel(current).ifBlank { getString(R.string.folder) }
            return
        }
        binding.search.queryHint = " ___"
        val listFolders = folders.filterNot { prefs.homeContainsFolder(it.id) }
        if (listFolders.isEmpty()) {
            container.isVisible = false
            return
        }
        for (folder in listFolders) {
            val row = FolderRowBinding.inflate(layoutInflater, container, false)
            row.folderName.text = folderLabel(folder)
            row.folderCount.text = folder.apps.size.toString()
            row.root.setOnClickListener { enterFolder(folder.id) }
            row.root.setOnLongClickListener { showFolderActions(folder); true }
            container.addView(row.root)
        }
        container.isVisible = true
    }

    private fun enterFolder(id: String) {
        currentFolderId = id
        binding.search.setQuery("", false)
        renderFolders()
        updateCombinedAppList()
    }

    private fun exitFolder() {
        currentFolderId = null
        binding.search.setQuery("", false)
        renderFolders()
        updateCombinedAppList()
    }

    private fun onFolderAction(appModel: AppModel) {
        val app = appModel as? AppModel.App ?: return
        if (currentFolderId != null) {
            prefs.removeAppFromFolders(folderKey(app))
            requireContext().showToast(getString(R.string.folder_removed_from))
            renderFolders()
            updateCombinedAppList()
        } else showChooseFolderDialog(app)
    }

    private fun showChooseFolderDialog(app: AppModel.App) {
        val folders = prefs.getFolders()
        val labels = folders.map { folderLabel(it).ifBlank { getString(R.string.folder) } } + getString(R.string.folder_new)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.folder_choose)
            .setItems(labels.toTypedArray()) { _, which ->
                if (which == folders.size) {
                    showFolderNameDialog(R.string.folder_new, "") { name ->
                        val folder = prefs.addFolder(name)
                        prefs.assignAppToFolder(folder.id, folderKey(app), app.appLabel)
                        requireContext().showToast(getString(R.string.folder_moved))
                        renderFolders()
                        updateCombinedAppList()
                    }
                } else {
                    prefs.assignAppToFolder(folders[which].id, folderKey(app), app.appLabel)
                    requireContext().showToast(getString(R.string.folder_moved))
                    renderFolders()
                    updateCombinedAppList()
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

    private fun showFolderActions(folder: Prefs.Folder) {
        val onHome = prefs.homeContainsFolder(folder.id)
        val actions = arrayOf(
            getString(if (onHome) R.string.folder_remove_from_home else R.string.folder_add_to_home),
            getString(R.string.folder_rename),
            getString(R.string.folder_delete)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(folderLabel(folder).ifBlank { getString(R.string.folder) })
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> {
                        if (onHome) prefs.removeFolderFromHome(folder.id)
                        else if (!prefs.addFolderToHome(folder)) requireContext().showToast(getString(R.string.home_apps_full))
                        viewModel.refreshHome(true)
                        renderFolders()
                    }

                    1 -> showFolderNameDialog(R.string.folder_rename, folder.name) { name ->
                        prefs.renameFolder(folder.id, name)
                        viewModel.refreshHome(true)
                        renderFolders()
                    }

                    2 -> {
                        prefs.deleteFolder(folder.id)
                        if (currentFolderId == folder.id) currentFolderId = null
                        viewModel.refreshHome(true)
                        renderFolders()
                        updateCombinedAppList()
                    }
                }
            }
            .show()
    }

    private fun initClickListeners() {
        binding.appSettings.setOnClickListener {
            binding.search.hideKeyboard()
            if (findNavController().currentDestination?.id == R.id.appListFragment)
                findNavController().navigate(R.id.action_appListFragment_to_settingsFragment2)
        }
        binding.appRename.setOnClickListener {
            val name = binding.search.query.toString().trim()
            if (name.isEmpty()) {
                requireContext().showToast(getString(R.string.type_a_new_app_name_first))
                binding.search.showKeyboard()
                return@setOnClickListener
            }

            when (flag) {
                Constants.FLAG_SET_HOME_APP_1 -> prefs.appName1 = name
                Constants.FLAG_SET_HOME_APP_2 -> prefs.appName2 = name
                Constants.FLAG_SET_HOME_APP_3 -> prefs.appName3 = name
                Constants.FLAG_SET_HOME_APP_4 -> prefs.appName4 = name
                Constants.FLAG_SET_HOME_APP_5 -> prefs.appName5 = name
                Constants.FLAG_SET_HOME_APP_6 -> prefs.appName6 = name
                Constants.FLAG_SET_HOME_APP_7 -> prefs.appName7 = name
                Constants.FLAG_SET_HOME_APP_8 -> prefs.appName8 = name
            }
            findNavController().popBackStack()
        }
    }

    private fun getRecyclerViewOnScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {

            var onTop = false

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {

                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                        onTop = !recyclerView.canScrollVertically(-1)
                        if (onTop)
                            binding.search.hideKeyboard()
                    }

                    RecyclerView.SCROLL_STATE_IDLE -> {
                        if (!recyclerView.canScrollVertically(1))
                            binding.search.hideKeyboard()
                        else if (!recyclerView.canScrollVertically(-1))
                            if (!onTop && isRemoving.not())
                                binding.search.showKeyboard(prefs.autoShowKeyboard)
                    }
                }
            }
        }
    }

    private fun checkMessageAndExit() {
        findNavController().popBackStack()
        if (flag == Constants.FLAG_LAUNCH_APP)
            viewModel.checkForMessages.call()
    }

    override fun onStart() {
        super.onStart()
        cachedIsCjkKeyboard = null
        binding.search.showKeyboard(prefs.autoShowKeyboard)
    }

    override fun onStop() {
        binding.search.hideKeyboard()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchTextView = null
        _binding = null
    }
}
