@file:Suppress("MagicNumber", "ArgumentListWrapping")

package com.storyteller_f.ui_list.ui

import android.content.Context
import android.graphics.Canvas
import android.text.SpannableString
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.storyteller_f.giant_explorer.databinding.ListWithStateBinding
import com.storyteller_f.ui_list.adapter.ManualAdapter
import com.storyteller_f.ui_list.adapter.SimpleSourceAdapter
import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

class ListWithState @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {
    private val binding = ListWithStateBinding.inflate(LayoutInflater.from(context), this)
    val recyclerView: RecyclerView get() = binding.list

    init {
        binding.list.setHasFixedSize(true)
    }

    fun flash(uiState: UIState) = with(binding) {
        list.isVisible = uiState.data
        emptyList.isVisible = uiState.empty
        progressBar.isVisible = uiState.progress
        retryButton.isVisible = uiState.retry
        uiState.refresh?.let { refreshLayout.isRefreshing = it }
        errorPage.isVisible = uiState.showErrorPage
        errorMsg.isVisible = uiState.error != null
        uiState.error?.let { errorMsg.text = it }
    }

    fun <IH : DataItemHolder, VH : AbstractViewHolder<IH>> sourceUp(
        adapter: SimpleSourceAdapter<IH, VH>,
        lifecycleOwner: LifecycleOwner,
        plugLayoutManager: Boolean = true,
        refresh: () -> Unit = {},
        flash: (CombinedLoadStates, Int) -> UIState = Companion::simple,
    ) {
        if (plugLayoutManager) setupLinearLayoutManager()
        recyclerView.adapter = adapter.withLoadStateHeaderAndFooter(
            header = SimpleLoadStateAdapter { adapter.retry() },
            footer = SimpleLoadStateAdapter { adapter.retry() },
        )
        binding.refreshLayout.setOnRefreshListener {
            refresh()
            adapter.refresh()
        }
        binding.retryButton.setOnClickListener {
            refresh()
            adapter.retry()
        }
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                adapter.loadStateFlow.collectLatest { states ->
                    flash(flash(states, adapter.itemCount))
                }
            }
        }
    }

    fun manualUp(adapter: ManualAdapter<*, *>, refresh: (() -> Unit)? = null) {
        recyclerView.adapter = adapter
        setupLinearLayoutManager()
        binding.refreshLayout.isEnabled = refresh != null
        binding.refreshLayout.setOnRefreshListener { refresh?.invoke() }
        binding.retryButton.setOnClickListener { refresh?.invoke() }
    }

    fun setupDampingSwipeSupport(block: (AbstractViewHolder<out DataItemHolder>, Int) -> Unit) {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START or ItemTouchHelper.END) {
            private var dispatched = false
            override fun getSwipeEscapeVelocity(defaultValue: Float) = 1_000_000F
            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder) = 10F
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean,
            ) {
                if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) return
                val distance = abs(dX)
                if (distance < 200F) dispatched = false
                val clamped = when {
                    distance < 200F -> dX / 2F
                    distance < 300F -> (if (dX > 0) 1 else -1) * (100F + (distance - 200F) / 4F)
                    else -> {
                        if (!dispatched) {
                            @Suppress("UNCHECKED_CAST")
                            block(
                                viewHolder as AbstractViewHolder<out DataItemHolder>,
                                if (dX > 0) ItemTouchHelper.RIGHT else ItemTouchHelper.LEFT
                            )
                            dispatched = true
                        }
                        (if (dX > 0) 1 else -1) * 125F
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, clamped, dY, actionState, isCurrentlyActive)
            }
        }).attachToRecyclerView(recyclerView)
    }

    private fun setupLinearLayoutManager() {
        recyclerView.layoutManager = LinearLayoutManager(context)
    }

    data class UIState(
        val retry: Boolean,
        val data: Boolean,
        val empty: Boolean,
        val progress: Boolean,
        val error: CharSequence?,
        val refresh: Boolean?,
    ) {
        val showErrorPage: Boolean get() = retry || error != null

        companion object {
            val loading = UIState(false, false, false, true, null, null)
        }
    }

    companion object {
        fun simple(loadState: CombinedLoadStates, itemCount: Int): UIState = buildState(
            refresh = loadState.mediator?.refresh ?: loadState.source.refresh,
            itemCount = itemCount,
            error = listOf(
                loadState.source.refresh,
                loadState.source.append,
                loadState.source.prepend,
                loadState.mediator?.refresh,
                loadState.mediator?.append,
                loadState.mediator?.prepend,
            ).filterIsInstance<LoadState.Error>().firstOrNull(),
        )

        fun remote(loadState: CombinedLoadStates, itemCount: Int): UIState = buildState(
            refresh = loadState.source.refresh,
            itemCount = itemCount,
            error = listOf(
                loadState.source.refresh,
                loadState.source.append,
                loadState.source.prepend,
                loadState.mediator?.refresh,
                loadState.mediator?.append,
                loadState.mediator?.prepend,
            ).filterIsInstance<LoadState.Error>().firstOrNull(),
        )

        private fun buildState(refresh: LoadState, itemCount: Int, error: LoadState.Error?) = UIState(
            retry = refresh is LoadState.Error,
            data = refresh is LoadState.NotLoading && itemCount > 0,
            empty = refresh is LoadState.NotLoading && itemCount == 0,
            progress = refresh is LoadState.Loading,
            error = error?.error?.localizedMessage?.let(::SpannableString),
            refresh = if (refresh is LoadState.Loading) null else false,
        )
    }
}
