package com.storyteller_f.giant_explorer.control

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.text.TextUtils
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.DragStartHelper
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.withResumed
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.annotation_defination.ItemHolder
import com.storyteller_f.common_pr.dipToInt
import com.storyteller_f.common_ui.ctx
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.common_ui.setVisible
import com.storyteller_f.common_vm_ktx.VMScope
import com.storyteller_f.common_vm_ktx.svm
import com.storyteller_f.common_vm_ktx.update
import com.storyteller_f.common_vm_ktx.vm
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.FileInfo
import com.storyteller_f.file_system_ktx.fileIcon
import com.storyteller_f.file_system_ktx.isDirectory
import com.storyteller_f.file_system_ktx.isFile
import com.storyteller_f.file_system_local.permission.checkFilePermission
import com.storyteller_f.file_system_local.permission.requestFilePermission
import com.storyteller_f.giant_explorer.PC_END_ON
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.control.ui_list.buildFileItemHolder
import com.storyteller_f.giant_explorer.database.AppDatabase
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.ViewHolderFileBinding
import com.storyteller_f.giant_explorer.databinding.ViewHolderFileGridBinding
import com.storyteller_f.giant_explorer.databinding.ViewHolderFileSentinelBinding
import com.storyteller_f.giant_explorer.model.FileModel
import com.storyteller_f.giant_explorer.service.FileOperationEventBus
import com.storyteller_f.ui_list.adapter.SimpleSourceAdapter
import com.storyteller_f.ui_list.core.BindingViewHolder
import com.storyteller_f.ui_list.core.BuildBatch
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.ItemHolderProvider
import com.storyteller_f.ui_list.data.SimpleResponse
import com.storyteller_f.ui_list.event.findFragmentOrNull
import com.storyteller_f.ui_list.source.SearchHandler
import com.storyteller_f.ui_list.source.SimpleSearchRepository
import com.storyteller_f.ui_list.ui.ListWithState
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.Locale

private const val FILE_LIST_SENTINEL_TYPE = "sentinel"
private const val FILE_LIST_SENTINEL_ID = "__file_list_sentinel__"

fun fileListAdapter() = SimpleSourceAdapter<FileItemHolder, FileViewHolder>(
    mapOf(
        FileItemHolder::class to BuildBatch(
            b2 = { parent, type ->
                if (type == FILE_LIST_SENTINEL_TYPE) {
                    FileSentinelViewHolder(
                        ViewHolderFileSentinelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    )
                } else {
                    buildFileItemHolder(parent, type)
                }
            }
        )
    )
)

class FileListViewModel(stateHandle: SavedStateHandle) : ViewModel() {
    val displayGrid = stateHandle.getLiveData("display", false)
    val filterHiddenFile = stateHandle.getLiveData("filterHiddenFile", false)
}

class FileListSearchViewModel(
    database: AppDatabase,
    selected: MutableLiveData<List<DataItemHolder>>
) : ViewModel() {
    val handler = SearchHandler(
        SimpleSearchRepository(fileSearchService(database))
    ) { fileModel: FileModel, sq: FileExplorerSearch ->
        FileItemHolder(fileModel, selected.value.orEmpty(), sq.display)
    }

    fun search(query: FileExplorerSearch) = handler.search(query, viewModelScope)
}

/**
 * @param owner 一般来说owner 都是this
 * @param scope viewModel 作用域
 */
class FileListObserver<T>(
    private val owner: T,
    args: () -> FileListFragmentArgs,
    val scope: VMScope
) where T : ViewModelStoreOwner, T : HasDefaultViewModelProviderFactory, T : LifecycleOwner {
    private val adapter = fileListAdapter()

    init {
        owner.lifecycleScope.launch {
            // The retained adapter consumes each invalidation once, including while its view is absent.
            adapter.onPagesUpdatedFlow.first { adapter.itemCount > 0 }
            FileOperationEventBus.shared.completions.collect { adapter.refresh() }
        }
    }

    fun releaseView(listWithState: ListWithState) {
        listWithState.recyclerView.adapter = null
    }

    val fileInstance: FileInstance?
        get() = session.fileInstance.value
    val selected: List<DataItemHolder>?
        get() = session.selected.value

    val fileListViewModel by owner.svm({}, scope) { handle, _ ->
        FileListViewModel(handle)
    }

    private val session by owner.vm(args) {
        FileExplorerSession(
            when (owner) {
                is Fragment -> owner.requireActivity().application
                is ComponentActivity -> owner.application
                else -> throw IllegalArgumentException("unrecognized ${owner.javaClass}")
            },
            it.uri
        )
    }

    private val dataViewModel by owner.vm(
        {
            val database = when (owner) {
                is Fragment -> owner.requireDatabase
                is Context -> owner.requireDatabase
                else -> throw IllegalArgumentException("unrecognized ${owner.javaClass}")
            }
            database to session.selected
        }
    ) { (database, selected) ->
        FileListSearchViewModel(database, selected)
    }

    private val pagingCollection = owner.lifecycleScope.launch(start = CoroutineStart.LAZY) {
        searches().collectLatest { query ->
            dataViewModel.search(query).collectLatest { adapter.submitData(it) }
        }
    }

    private fun searches(): Flow<FileExplorerSearch> = combine(
        session.fileInstance.asFlow().filterNotNull(),
        fileListViewModel.filterHiddenFile.asFlow(),
        currentSortFilterConfig.asFlow(),
        fileListViewModel.displayGrid.asFlow(),
    ) { fileInstance, hidden, sort, grid ->
        FileExplorerSearch(fileInstance, hidden, sort, if (grid) "grid" else "")
    }.distinctUntilChanged()

    fun bindView(
        listWithState: ListWithState,
        rightSwipe: (FileItemHolder) -> Unit,
        updatePath: (String) -> Unit
    ) {
        val viewOwner = if (owner is Fragment) owner.viewLifecycleOwner else owner
        bindLayoutManager(listWithState, viewOwner)
        bindContent(listWithState, viewOwner, rightSwipe)
        bindPath(viewOwner, updatePath)
        bindPermissions(viewOwner)
        startDataCollection()
    }

    private fun startDataCollection() {
        // Recreated views reconnect to the retained adapter without resubmitting cached pages.
        pagingCollection.start()
    }

    private fun bindLayoutManager(listWithState: ListWithState, viewOwner: LifecycleOwner) {
        fileListViewModel.displayGrid.observe(viewOwner) { grid ->
            listWithState.recyclerView.layoutManager = if (grid) {
                val spanCount = listWithState.context.run {
                    (resources.displayMetrics.widthPixels / 120.dipToInt).coerceAtLeast(1)
                }
                GridLayoutManager(listWithState.context, spanCount).apply {
                    spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int =
                            if (adapter.peek(position)?.isSentinel == true) spanCount else 1
                    }
                }
            } else {
                LinearLayoutManager(listWithState.context)
            }
        }
    }

    private fun bindContent(
        listWithState: ListWithState,
        viewOwner: LifecycleOwner,
        rightSwipe: (FileItemHolder) -> Unit,
    ) {
        listWithState.sourceUp(
            adapter,
            viewOwner,
            plugLayoutManager = false,
            flash = ListWithState.Companion::remote,
            displayAdapter = adapter,
        )
        listWithState.setupDampingSwipeSupport { viewHolder, direction ->
            val itemHolder = viewHolder.fileItemOrNull() ?: return@setupDampingSwipeSupport
            if (itemHolder.isSentinel) return@setupDampingSwipeSupport
            if (direction == ItemTouchHelper.LEFT) {
                session.selected.update {
                    val (selectedHolders, currentSelected) = it.toggle(itemHolder)
                    viewHolder.itemView.isSelected = currentSelected
                    selectedHolders
                }
            } else {
                rightSwipe(itemHolder)
            }
        }
    }

    private fun bindPath(viewOwner: LifecycleOwner, updatePath: (String) -> Unit) {
        viewOwner.lifecycleScope.launch {
            // A predictive-back preview must not replace the active page's path.
            viewOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                session.fileInstance.asFlow().filterNotNull().collect { updatePath(it.path) }
            }
        }
    }

    private fun bindPermissions(viewOwner: LifecycleOwner) {
        val context = owner.ctx
        viewOwner.lifecycleScope.launch {
            session.fileInstance.asFlow().filterNotNull().map { it.uri }.distinctUntilChanged()
                .collectLatest { path ->
                    val granted = withContext(Dispatchers.IO) { context.applicationContext.checkFilePermission(path) }
                    if (!granted) {
                        viewOwner.lifecycle.withResumed { Unit }
                        if (context.requestFilePermission(path)) adapter.refresh()
                    }
                }
        }
    }

    fun update(toParent: FileInstance) {
        session.fileInstance.value = toParent
    }
}

interface FileItemHolderEvent {
    fun onClick(view: View, itemHolder: FileItemHolder)
}

@ItemHolder("file")
class FileItemHolder(
    val file: FileModel,
    val selected: List<DataItemHolder>,
    variant: String
) : DataItemHolder(if (file.fullPath == FILE_LIST_SENTINEL_ID) FILE_LIST_SENTINEL_TYPE else variant) {
    override fun areItemsTheSame(other: DataItemHolder) =
        (other as FileItemHolder).file.fullPath == file.fullPath

    override fun areContentsTheSame(other: DataItemHolder): Boolean {
        return (other as FileItemHolder).file == file
    }
}

private val FileItemHolder.isSentinel: Boolean
    get() = type == FILE_LIST_SENTINEL_TYPE

class FileSentinelViewHolder(binding: ViewHolderFileSentinelBinding) :
    BindingViewHolder<FileItemHolder>(binding) {
    override fun bindData(itemHolder: FileItemHolder) = Unit
}

@BindItemHolder(FileItemHolder::class, type = "grid")
class FileGridViewHolder(private val binding: ViewHolderFileGridBinding) :
    BindingViewHolder<FileItemHolder>(binding) {
    override fun bindData(itemHolder: FileItemHolder) {
        binding.fileName.setTextIfChanged(itemHolder.file.name)
        binding.fileIcon.fileIcon(itemHolder.file.item)
        binding.symLink.isVisible = itemHolder.file.isSymLink
        itemHolder.file.item.dragSupport(itemView)
        binding.root.isSelected = itemHolder.selected.any { it.areItemsTheSame(itemHolder) }
        binding.root.setBackgroundResource(
            if (itemHolder.file.item.isFile) R.drawable.background_file else R.drawable.background_folder
        )
        itemView.setOnClick {
            it.findFragmentOrNull<FileItemHolderEvent>()?.onClick(it, itemHolder)
        }
    }
}

@BindItemHolder(FileItemHolder::class)
class FileViewHolder(private val binding: ViewHolderFileBinding) :
    BindingViewHolder<FileItemHolder>(binding) {
    override fun bindData(itemHolder: FileItemHolder) {
        val file = itemHolder.file
        binding.fileName.setTextIfChanged(file.name)
        binding.fileIcon.fileIcon(file.item)
        val item = file.item
        binding.root.isSelected = run {
            val firstOrNull = itemHolder.selected.firstOrNull {
                it.areItemsTheSame(
                    itemHolder
                )
            }
            firstOrNull != null
        } == true
        binding.root.setBackgroundResource(
            if (file.item.isFile) R.drawable.background_file else R.drawable.background_folder
        )
        binding.fileSize.setVisible(file.size != -1L) {
            it.setTextIfChanged(file.formattedSize)
        }
        binding.fileMD.setVisible(file.md.valid()) {
            it.setTextIfChanged(file.md)
        }

        binding.torrentName.setVisible(file.torrentName.valid()) {
            it.setTextIfChanged(file.torrentName)
        }

        val fileTime = item.time
        val lastModified = fileTime.lastModified ?: 0
        val formattedLastModifiedTime = fileTime.formattedLastModifiedTime
        binding.modifiedTime.setVisible(lastModified > 0 && formattedLastModifiedTime.valid()) {
            it.setTextIfChanged(formattedLastModifiedTime)
        }

        binding.detail.setTextIfChanged(item.permissions.toString())
        binding.symLink.isVisible = file.isSymLink
        file.item.dragSupport(binding.root)
        itemView.setOnClick {
            it.findFragmentOrNull<FileItemHolderEvent>()?.onClick(it, itemHolder)
        }
    }
}

private fun TextView.setTextIfChanged(value: CharSequence?) {
    if (!TextUtils.equals(text, value)) text = value
}

private fun FileInfo.dragSupport(root: View) {
    DragStartHelper(root) { view: View, _: DragStartHelper ->
        val clipData = ClipData.newPlainText(FileListFragment.CLIP_DATA_KEY, uri.toString())
        val flags = View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
        view.startDragAndDrop(clipData, View.DragShadowBuilder(view), null, flags)
    }.apply {
        attach()
    }
    root.setOnDragListener(
        if (!isDirectory) {
            null
        } else {
            { v, event ->
                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> {
                        val clipDescription = event.clipDescription
                        clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) &&
                            clipDescription.label == FileListFragment.CLIP_DATA_KEY
                    }

                    DragEvent.ACTION_DROP -> {
                        v.findFragmentOrNull<FileListFragment>()
                            ?.pasteFiles(event.clipData, uri)
                        true
                    }

                    else -> true
                }
            }
        }
    )
}

fun String?.valid() = this?.trim()?.isNotEmpty() == true

suspend fun format1024(args: Long): String {
    if (args < 0) {
        return "Error"
    }
    val flags = arrayOf("B.", "KB", "MB", "GB", "TB")
    var flag = 0
    var size = args.toDouble()
    while (size >= PC_END_ON) {
        yield()
        size /= PC_END_ON
        flag += 1
    }
    assert(flag < flags.size) {
        "$flag $size $args"
    }
    return String.format(Locale.CHINA, "%.2f %s", size, flags[flag])
}

data class FileExplorerSearch(
    val path: FileInstance,
    val filterHiddenFile: Boolean,
    val sortFilterConfig: SortFilterConfig,
    val display: String
)

fun fileSearchService(
    database: AppDatabase
): suspend (searchQuery: FileExplorerSearch, start: Int, count: Int) -> SimpleResponse<FileModel> {
    return { searchQuery: FileExplorerSearch, start: Int, count: Int ->
        val listSafe = withContext(Dispatchers.IO) {
            searchQuery.path.list()
        }

        val config = searchQuery.sortFilterConfig

        // 判断是否需要隐藏，如果为true 代表不隐藏
        val filterPredicate: (FileInfo) -> Boolean = {
            !searchQuery.filterHiddenFile || !it.kind.isHidden
        }
        val directories = listSafe.directories
        val files = listSafe.files

        // 应用过滤和排序
        val filteredDirs = if (searchQuery.filterHiddenFile || config.filterHidden) {
            config.apply(directories.filter(filterPredicate))
        } else {
            config.sort(directories)
        }

        val filteredFiles = if (searchQuery.filterHiddenFile || config.filterHidden) {
            config.apply(files.filter(filterPredicate))
        } else {
            config.sort(files)
        }

        val listFiles = filteredDirs.plus(filteredFiles)
        val total = listFiles.size + 1
        val index = start - 1
        val startPosition = index * count
        if (startPosition >= total) {
            SimpleResponse(0)
        } else {
            val endPosition = (startPosition + count).coerceAtMost(total)
            val items = buildList {
                if (startPosition == 0) {
                    add(fileListSentinelModel())
                }
                val realStartPosition = (startPosition - 1).coerceAtLeast(0)
                val realEndPosition = (endPosition - 1).coerceAtMost(listFiles.size)
                if (realStartPosition < realEndPosition) {
                    addAll(
                        listFiles
                            .subList(realStartPosition, realEndPosition)
                            .map { model ->
                                fileModelBuilder(model, database)
                            }
                    )
                }
            }
            SimpleResponse(
                total = total,
                items = items,
                if (total > count * start) start + 1 else null
            )
        }
    }
}

private fun fileListSentinelModel(): FileModel {
    val item = FileInfo(
        FILE_LIST_SENTINEL_ID,
        android.net.Uri.EMPTY,
        FileTime(),
        FileKind.File(null, false, 0, ""),
        FilePermissions.USER_READABLE
    )
    return FileModel(
        item,
        FILE_LIST_SENTINEL_ID,
        FILE_LIST_SENTINEL_ID,
        0,
        isHidden = false,
        isSymLink = false,
        torrentName = null,
        md = null,
        formattedSize = "",
    )
}

private suspend fun fileModelBuilder(
    model: FileInfo,
    database: AppDatabase
): FileModel {
    val fileTime = model.time
    val lastModified = fileTime.lastModified ?: 0
    var md = ""
    var torrentName = ""
    val kind = model.kind
    val length = if (kind is FileKind.File) {
        database.mdDao().search(model.uri)?.takeIf {
            it.lastUpdateTime > lastModified
        }?.let {
            md = it.data
        }
        if (model.extension == "torrent") {
            database.torrentDao().search(model.uri)?.takeIf {
                it.lastUpdateTime > lastModified
            }?.let {
                torrentName = it.torrent
            }
        }
        kind.size
    } else {
        // 从数据库中查找
        val directory = database.sizeDao().search(model.uri)
        if (directory != null && directory.lastUpdateTime > lastModified) {
            directory.size
        } else {
            -1
        }
    }
    val fileKind = model.kind
    return FileModel(
        model,
        model.name,
        model.fullPath,
        length,
        fileKind.isHidden,
        fileKind.linkType != null,
        torrentName,
        md,
        format1024(length)
    )
}

/**
 * 反选。通过areItemsTheSame 比较，而不是equals
 * @return 返回新的选中列表和上一次是否处于选中状态
 */
fun List<DataItemHolder>?.toggle(
    holder: DataItemHolder
): Pair<List<DataItemHolder>, Boolean> {
    val oldSelectedHolders = this ?: mutableListOf()
    val otherHolders = oldSelectedHolders.filter {
        !it.areItemsTheSame(holder)
    }
    val newState = otherHolders.size == oldSelectedHolders.size
    val selected = if (newState) otherHolders + holder else otherHolders
    return selected to newState
}

fun MutableLiveData<List<DataItemHolder>>.toggle(viewHolder: RecyclerView.ViewHolder) {
    val itemHolder = viewHolder.fileItemOrNull() ?: return
    update {
        val (selectedHolders, currentSelected) =
            it.toggle(itemHolder)
        viewHolder.itemView.isSelected = currentSelected
        selectedHolders
    }
}

internal fun RecyclerView.ViewHolder.fileItemOrNull(): FileItemHolder? =
    (bindingAdapter as? ItemHolderProvider<*>)?.getItemHolder(bindingAdapterPosition) as? FileItemHolder
