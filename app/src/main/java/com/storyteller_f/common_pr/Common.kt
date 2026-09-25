@file:Suppress("detekt.formatting")

package com.storyteller_f.common_pr

import android.content.Context
import android.util.TypedValue
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.storyteller_f.common_vm_ktx.state
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import com.storyteller_f.ext_func_definition.ExtFuncFlatType
import kotlinx.coroutines.flow.Flow

context(owner: LifecycleOwner)
fun <T> LiveData<T>.state(ob: Observer<T>) {
    state(owner, ob)
}

context(owner: LifecycleOwner)
fun <T> Flow<T>.state(function: (T) -> Unit) {
    state(owner, function)
}

context(ctx: Context)
@ExtFuncFlat(ExtFuncFlatType.V4, isContextReceiver = true)
val Float.dip: Float
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this@dip, ctx.resources.displayMetrics)

context(ctx: Context)
@ExtFuncFlat(ExtFuncFlatType.V4, isContextReceiver = true)
val Float.dipToInt: Int
    get() = dip.toInt()

