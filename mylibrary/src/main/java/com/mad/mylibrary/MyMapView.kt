package com.mad.mylibrary

import android.content.Context
import android.view.MotionEvent
import com.google.android.gms.maps.MapView

class MyMapView(context: Context?) : MapView(context!!) {
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        when (action) {
            MotionEvent.ACTION_DOWN ->                 // Disallow ScrollView to intercept touch events.
                this.parent.requestDisallowInterceptTouchEvent(true)

            MotionEvent.ACTION_UP ->                 // Allow ScrollView to intercept touch events.
                this.parent.requestDisallowInterceptTouchEvent(false)
        }

        // Handle MapView's touch events.
        super.onTouchEvent(ev)
        return true
    }
}