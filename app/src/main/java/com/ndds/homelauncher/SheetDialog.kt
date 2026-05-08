package com.ndds.homelauncher

import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding

class SheetDialog(val appContext: MainActivity) {
    var modalView: View? = null
     fun setContentView(content: View) {
        if (modalView != null) return
        modalView = LayoutInflater.from(appContext).inflate(R.layout.modal_view, null)
        val modalBackdrop = modalView!!.findViewById<View>(R.id.modal_backdrop)
        val modalContainer = modalView!!.findViewById<ViewGroup>(R.id.modal_container)
        modalContainer.addView(content)
        modalContainer.updatePadding(bottom = appContext.findViewById<View>(R.id.home_section).paddingBottom)
        modalContainer.measure(
            View.MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        val containerHeight = modalContainer.measuredHeight
        modalContainer.translationY = containerHeight.toFloat()
        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.duration = 300
        valueAnimator.interpolator = DecelerateInterpolator()
        valueAnimator.addUpdateListener { animator ->
            modalBackdrop.alpha = animator.animatedValue as Float
            modalContainer.translationY = (1 - animator.animatedFraction) * containerHeight
        }
        modalBackdrop.setOnClickListener { dismiss() }
        modalView!!.post {
            valueAnimator.start()
        }
    }

     fun dismiss(): Boolean {
        val modalView = modalView ?: return true
        val modalBackdrop = modalView.findViewById<View>(R.id.modal_backdrop)
        val modalContainer = modalView.findViewById<ViewGroup>(R.id.modal_container)
         val parentView = appContext.findViewById<ViewGroup>(R.id.superRoot)
        val valueAnimator = ValueAnimator.ofFloat(1f, 0f)
        valueAnimator.duration = 300
        valueAnimator.interpolator = AccelerateInterpolator()
        valueAnimator.addUpdateListener { animator ->
            modalBackdrop.alpha = animator.animatedValue as Float
            modalContainer.translationY = animator.animatedFraction * modalContainer.height
            if (animator.animatedFraction == 1f) {
                parentView.removeView(modalView)
                this@SheetDialog.modalView = null
            }
        }
        modalView.post {
            valueAnimator.start()
        }
         return false
    }
    fun adjustPadding(insets: Insets) {
        if (modalView != null) {
            modalView!!.findViewById<View>(R.id.modal_container).updatePadding(bottom = insets.bottom)
        }
    }

    fun show() {
        val currentFocus = appContext.currentFocus
        if (currentFocus != null) {
            val inputMethodManager = appContext.getSystemService(InputMethodManager::class.java)
            inputMethodManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
        appContext.findViewById<ViewGroup>(R.id.superRoot).addView(modalView)
    }
}