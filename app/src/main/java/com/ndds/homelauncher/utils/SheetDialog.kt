package com.ndds.homelauncher.utils

import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import com.ndds.homelauncher.MainActivity
import com.ndds.homelauncher.R
import com.ndds.homelauncher.widgets.ModalItem

class SheetDialog(val appContext: MainActivity) {
    var modalView: View? = null
    var dismissListener: (() -> Unit)? = null
    var isShowing = false
    fun buildContent(title: String, options: Array<String>, onOptionSelected: (optionIndex: Int) -> Unit): View {
        val container = LayoutInflater.from(appContext).inflate(R.layout.modal_content, null) as ViewGroup
        container.findViewById<TextView>(R.id.modal_title).text = title
        val optionsContainer = container.findViewById<ViewGroup>(R.id.modal_options_container)
        options.forEachIndexed { index, option ->
            val optionItem = ModalItem(appContext)
            optionItem.setLabel(option)
            optionItem.setOnClickListener {
                onOptionSelected(index)
            }
            optionsContainer.addView(optionItem)
        }
        return container
    }
     fun setContentView(content: View) {
        if (modalView != null) {
            val modalContainer = modalView!!.findViewById<ViewGroup>(R.id.modal_container)
            modalContainer.removeAllViews()
            modalContainer.addView(content)
            return
        }
        modalView = LayoutInflater.from(appContext).inflate(R.layout.modal_view, null)
        val modalBackdrop = modalView!!.findViewById<View>(R.id.modal_backdrop)
        val modalContainer = modalView!!.findViewById<ViewGroup>(R.id.modal_container)
        modalContainer.addView(content)
        modalContainer.updatePadding(bottom = appContext.findViewById<View>(R.id.home_section).paddingBottom)
        modalContainer.measure(
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
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
                dismissListener?.invoke()
                dismissListener = null
                this@SheetDialog.modalView = null
                isShowing = false
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
        if (isShowing) return
        val currentFocus = appContext.currentFocus
        if (currentFocus != null) {
            val inputMethodManager = appContext.getSystemService(InputMethodManager::class.java)
            inputMethodManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
        }
        appContext.findViewById<ViewGroup>(R.id.superRoot).addView(modalView)
        isShowing = true
    }
    fun show(dismissListener: () -> Unit) {
        this.dismissListener = dismissListener
        show()
    }
}