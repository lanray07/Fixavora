package com.fixavora.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.fixavora.app.ui.*

class MainActivity : ComponentActivity() {
    private val model: HomeViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FixavoraRoot(model) }
    }
    override fun onResume() { super.onResume(); model.billing.connect() }
}
