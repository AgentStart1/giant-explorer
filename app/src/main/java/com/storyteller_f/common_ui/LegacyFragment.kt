package com.storyteller_f.common_ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class CommonFragment(layoutRes: Int = 0) : Fragment(layoutRes), ResponseFragment, Registry {
    override val vm by responseModel

    override fun onStart() {
        super.onStart()
        observeResponse()
    }
}

abstract class SimpleFragment<T : ViewBinding>(
    private val viewBindingInflateFactory: (LayoutInflater) -> T,
) : CommonFragment() {
    private var nullableBinding: T? = null
    val binding: T get() = checkNotNull(nullableBinding)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val localBinding = viewBindingInflateFactory(inflater)
        nullableBinding = localBinding
        onBindViewEvent(localBinding)
        return localBinding.root
    }

    abstract fun onBindViewEvent(binding: T)

    override fun onDestroyView() {
        nullableBinding = null
        super.onDestroyView()
    }
}
