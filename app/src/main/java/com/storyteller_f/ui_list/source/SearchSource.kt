package com.storyteller_f.ui_list.source

import android.util.Log
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.map
import com.storyteller_f.common_vm_ktx.vm
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.Model
import com.storyteller_f.ui_list.data.SimpleResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SimpleSearchSource<D : Model, SQ : Any>(
    val service: suspend (SQ, Int, Int) -> SimpleResponse<D>,
    private val sq: SQ
) : PagingSource<Int, D>() {
    override fun getRefreshKey(state: PagingState<Int, D>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, D> {
        val position = params.key ?: STARTING_PAGE_INDEX
        return try {
            val response = service(sq, position, params.loadSize)
            val items = response.items
            val nextKey = if (items.isEmpty() || items.size < params.loadSize || response.total == 0) {
                null
            } else {
                // initial load size = 3 * NETWORK_PAGE_SIZE
                // ensure we're not requesting duplicating items, at the 2nd request
                position + (params.loadSize / NETWORK_PAGE_SIZE)
            }
            LoadResult.Page(
                data = items,
                prevKey = if (position == STARTING_PAGE_INDEX) null else position - 1,
                nextKey = nextKey
            )
        } catch (exception: Exception) {
            Log.e(TAG, "load: ", exception)
            LoadResult.Error(exception)
        }
    }

    companion object {
        private const val TAG = "SearchSource"
    }
}

class SimpleSearchRepository<D : Model, SQ : Any>(
    private val service: suspend (SQ, Int, Int) -> SimpleResponse<D>,
) {
    fun search(sq: SQ): Flow<PagingData<D>> {
        return Pager(
            config = PagingConfig(
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { SimpleSearchSource(service, sq) }
        ).flow
    }
}

private const val STARTING_PAGE_INDEX = 1
private const val NETWORK_PAGE_SIZE = 30

class SearchHandler<D : Model, SQ : Any, Holder : DataItemHolder>(
    private val repository: SimpleSearchRepository<D, SQ>,
    val processFactory: (D, SQ) -> Holder,
) {
    private var currentQueryValue: SQ? = null
    var lastJob: Job? = null

    private var currentSearchResult: Flow<PagingData<Holder>>? = null

    fun search(sq: SQ, scope: CoroutineScope): Flow<PagingData<Holder>> {
        val lastResult = currentSearchResult
        if (sq == currentQueryValue && lastResult != null) {
            return lastResult
        }
        currentQueryValue = sq
        val newResult: Flow<PagingData<Holder>> = repository.search(sq)
            .map { pagingData ->
                pagingData.map {
                    processFactory(it, sq)
                }
            }
            .cachedIn(scope)
        currentSearchResult = newResult
        return newResult
    }
}

@Deprecated(
    message = "Use a business ViewModel that owns SearchHandler instead of the shared SimpleSearchViewModel.",
    level = DeprecationLevel.WARNING
)
class SimpleSearchViewModel<D : Model, SQ : Any, Holder : DataItemHolder>(
    private val repository: SimpleSearchRepository<D, SQ>,
    val processFactory: (D, SQ) -> Holder,
) : ViewModel() {
    internal val handler = SearchHandler(repository, processFactory)

    var lastJob: Job?
        get() = handler.lastJob
        set(value) {
            handler.lastJob = value
        }

    fun search(sq: SQ): Flow<PagingData<Holder>> {
        return handler.search(sq, viewModelScope)
    }
}

@Deprecated(
    message = "Use a business ViewModel that owns SearchHandler instead of SimpleSearchViewModel.",
    level = DeprecationLevel.WARNING
)
@Suppress("DEPRECATION")
fun <SQ : Any, Holder : DataItemHolder> SimpleSearchViewModel<*, SQ, Holder>.observerInScope(
    lifecycleOwner: LifecycleOwner,
    search: SQ,
    block: suspend (PagingData<Holder>) -> Unit
) {
    lastJob?.cancel()
    lastJob = lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            search(search).collectLatest {
                block(it)
            }
        }
    }
}

/**
 * @param service startPage from 1
 */
class SearchProducer<D : Model, SQ : Any, Holder : DataItemHolder>(
    val service: suspend (SQ, startPage: Int, count: Int) -> SimpleResponse<D>,
    val processFactory: (D, SQ) -> Holder,
)

@Deprecated(
    message = "Define a business ViewModel and use SearchHandler inside it.",
    level = DeprecationLevel.WARNING
)
@Suppress("DEPRECATION")
fun <D : Model, SQ : Any, Holder : DataItemHolder, T> T.search(
    searchProducer: SearchProducer<D, SQ, Holder>
) where T : HasDefaultViewModelProviderFactory, T : ViewModelStoreOwner = vm({}) {
    SimpleSearchViewModel(
        SimpleSearchRepository(searchProducer.service),
        searchProducer.processFactory,
    )
}

@Deprecated(
    message = "Define a business ViewModel and use SearchHandler inside it.",
    level = DeprecationLevel.WARNING
)
@Suppress("DEPRECATION")
fun <D : Model, SQ : Any, Holder : DataItemHolder, T, ARG> T.search(
    arg: () -> ARG,
    searchContentProducer: (ARG) -> SearchProducer<D, SQ, Holder>
) where T : HasDefaultViewModelProviderFactory, T : ViewModelStoreOwner = vm({}) {
    val searchProducer = searchContentProducer(arg())
    SimpleSearchViewModel(
        SimpleSearchRepository(searchProducer.service),
        searchProducer.processFactory,
    )
}
