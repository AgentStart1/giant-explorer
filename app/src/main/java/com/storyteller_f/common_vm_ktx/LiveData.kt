@file:Suppress("UNCHECKED_CAST", "unused", "TooManyFunctions", "ImportOrdering")

package com.storyteller_f.common_vm_ktx

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.collections.set

/**
 * @author storyteller_f
 */

class CountableMediatorLiveData : MediatorLiveData<Any>() {
    var currentIndex = 0
}

class KeyedLiveData<T>(value: T, val key: String = "") : MutableLiveData<T>(value)

/**
 * 合并LiveData，value 变成map
 */
fun LiveData<out Any>.plus(source: LiveData<out Any>, key: String = ""): MediatorLiveData<out Any> {
    val sourceKey = if (source is KeyedLiveData<*> && source.key.trim().isNotEmpty()) {
        source.key
    } else {
        key
    }
    if (this is CountableMediatorLiveData) {
        val index = currentIndex
        val k = if (sourceKey.trim().isEmpty()) index.toString() else key
        addSource(source) {
            value = copyMap(value as Map<String, Any?>?).apply {
                set(k, it)
            }
        }
        currentIndex++
        return this
    } else {
        val mediatorLiveData = CountableMediatorLiveData()
        val k1 = if (this is KeyedLiveData<*> && this.key.trim().isNotEmpty()) {
            this.key
        } else {
            "first"
        }
        mediatorLiveData.addSource(this) {
            if (mediatorLiveData.value is Map<*, *>) {
                mediatorLiveData.value =
                    copyMap(mediatorLiveData.value as Map<String, Any?>?).apply {
                        set(k1, it)
                    }
            }
        }
        val k = if (sourceKey.trim().isEmpty()) "second" else sourceKey

        mediatorLiveData.addSource(source) {
            mediatorLiveData.value =
                copyMap(mediatorLiveData.value as Map<String, Any?>?).apply {
                    set(k, it)
                }
        }
        mediatorLiveData.currentIndex = 2
        return mediatorLiveData
    }
}

fun combine(vararg arrayOfPairs: Pair<String, LiveData<out Any?>>): LiveData<Map<String, Any?>> {
    val mediatorLiveData = MediatorLiveData<Map<String, Any?>>()
    arrayOfPairs.forEach {
        val index = it.first
        mediatorLiveData.addSource(it.second) {
            mediatorLiveData.value = copyMap(mediatorLiveData.value).apply {
                set(index, it)
            }
        }
    }
    return mediatorLiveData.map {
        it as Map<String, Any?>
    }
}

fun copyMap(map: Map<String, Any?>?): MutableMap<String, Any?> {
    val newly = mutableMapOf<String, Any?>()
    map?.forEach {
        newly[it.key] = it.value
    }
    return newly
}

fun <T> LiveData<T>.toDiff(compare: ((T, T) -> Boolean)? = null): MediatorLiveData<Pair<T?, T?>> {
    val mediatorLiveData = MediatorLiveData<Pair<T?, T?>>()
    var oo: T? = value
    mediatorLiveData.addSource(this) {
        val l = oo
        if (l == null || it == null || compare?.invoke(l, it) != true) {
            mediatorLiveData.value = Pair(l, it)
        }
        oo = it
    }
    return mediatorLiveData
}

/**
 * @param compare 如果返回真，那么就会被过滤掉
 */
fun <T> LiveData<T>.toDiffNoNull(compare: ((T, T) -> Boolean)? = null): MediatorLiveData<Pair<T, T>> {
    val mediatorLiveData = MediatorLiveData<Pair<T, T>>()
    var oo: T? = value
    mediatorLiveData.addSource(this) {
        val l = oo
        if (l != null && it != null && compare?.invoke(l, it) != true) {
            mediatorLiveData.value = Pair(l, it)
        }
        oo = it
    }
    return mediatorLiveData
}

@OptIn(FlowPreview::class)
fun <T> LiveData<T>.debounce(ms: Long): LiveData<T> = asFlow().debounce(ms).asLiveData()

fun <T> LiveData<T>.state(owner: LifecycleOwner, ob: Observer<T>) {
    val any = if (owner is Fragment) owner.viewLifecycleOwner else owner
    observe(any, ob)
}

fun <T> Flow<T>.state(owner: LifecycleOwner, function: (T) -> Unit) {
    val any = if (owner is Fragment) owner.viewLifecycleOwner else owner
    observe(any, function)
}

fun <T> Flow<T>.observe(owner: LifecycleOwner, function: (T) -> Unit) {
    owner.lifecycleScope.launch {
        collect {
            function(it)
        }
    }
}

/**
 * @param f 返回是否相等
 */
@Suppress("ComplexCondition")
fun <X> LiveData<X>.distinctUntilChangedBy(f: (X, X) -> Boolean): LiveData<X?> {
    val outputLiveData: MediatorLiveData<X?> = MediatorLiveData<X?>()
    outputLiveData.addSource(
        this,
        object : Observer<X?> {
            var mFirstTime = true
            var previous: X? = null
            override fun onChanged(value: X?) {
                val previousValue = previous
                if (mFirstTime ||
                    previousValue == null && value != null ||
                    previousValue != null && (
                        previousValue !== value ||
                            !f(previousValue, value)
                        )
                ) {
                    mFirstTime = false
                    outputLiveData.value = value
                    previous = value
                }
            }
        }
    )
    return outputLiveData
}

fun <T> MutableLiveData<T>.update(function: (T?) -> T) {
    val old = value
    value = function(old)
}
