@file:Suppress("Filename", "MatchingDeclarationName")

package com.storyteller_f.common_ui

import androidx.appcompat.app.AppCompatActivity

abstract class CommonActivity : AppCompatActivity(), Registry {
    override fun onStart() {
        super.onStart()
        observeResponse()
    }
}
