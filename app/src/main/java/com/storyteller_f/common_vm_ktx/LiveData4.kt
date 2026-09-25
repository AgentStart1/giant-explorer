package com.storyteller_f.common_vm_ktx

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

data class Dao4<A, B, C, D>(val d1: A?, val d2: B?, val d3: C?, val d4: D?)

data class Value4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

fun <A, B, C, D> combineDao(
    first: LiveData<A>,
    second: LiveData<B>,
    third: LiveData<C>,
    fourth: LiveData<D>,
): LiveData<Dao4<A, B, C, D>> {
    val result = MediatorLiveData<Dao4<A, B, C, D>>()
    fun publish() {
        result.value = Dao4(first.value, second.value, third.value, fourth.value)
    }
    result.addSource(first) { publish() }
    result.addSource(second) { publish() }
    result.addSource(third) { publish() }
    result.addSource(fourth) { publish() }
    return result
}

fun <A : Any, B : Any, C : Any, D : Any> LiveData<Dao4<A, B, C, D>>.wait4(): LiveData<Value4<A, B, C, D>> {
    val result = MediatorLiveData<Value4<A, B, C, D>>()
    result.addSource(this) { values ->
        val first = values.d1
        val second = values.d2
        val third = values.d3
        val fourth = values.d4
        if (first == null) return@addSource
        if (second == null) return@addSource
        if (third == null) return@addSource
        if (fourth == null) return@addSource
        result.value = Value4(first, second, third, fourth)
    }
    return result
}

infix fun <A, B, C> Pair<A, B>.to(third: C) = Triple(first, second, third)
