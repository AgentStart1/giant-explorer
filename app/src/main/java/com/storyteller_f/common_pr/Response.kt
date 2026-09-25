@file:Suppress("detekt.formatting")

package com.storyteller_f.common_pr

import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.storyteller_f.common_ui.FragmentRequest
import com.storyteller_f.common_ui.Registry
import com.storyteller_f.common_ui.observeResponse
import com.storyteller_f.common_ui.request
import kotlin.reflect.KClass


context (f: F)
fun <T : Parcelable, F> FragmentRequest.response(
    result: KClass<T>,
    action: F.(T) -> Unit
) where F : Fragment, F : Registry {
    f.observeResponse(this, result, action)
}

context (a: A)
fun <T : Parcelable, A> FragmentRequest.response(
    result: KClass<T>,
    action: A.(T) -> Unit
) where A : FragmentActivity, A : Registry {
    a.observeResponse(this, result, action)
}

context (f: F)
fun <T : Parcelable, F> FragmentRequest.response(
    result: Class<T>,
    action: F.(T) -> Unit
) where F : Fragment, F : Registry {
    f.observeResponse(this, result, action)
}

context (a: A)
fun <T : Parcelable, A> FragmentRequest.response(
    result: Class<T>,
    action: A.(T) -> Unit
) where A : FragmentActivity, A : Registry {
    a.observeResponse(this, result, action)
}

context (f: F)
fun <F> NavDirections.request() where F : Fragment, F : Registry =
    f.findNavController().request(this)

context (a: A)
fun <A> NavDirections.request(viewId: Int) where A : FragmentActivity, A : Registry =
    a.findNavController(viewId).request(this)

