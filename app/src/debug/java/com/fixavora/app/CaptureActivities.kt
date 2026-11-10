package com.fixavora.app

/** Debug-only hosts for stable screenshot orientations; all screens and data flows are inherited. */
class TabletCaptureActivity : MainActivity()
class CompactCaptureActivity : MainActivity()

/** Isolated form host; no production database or billing state is injected into tests. */
class EditorTestActivity : androidx.activity.ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
    }
}
