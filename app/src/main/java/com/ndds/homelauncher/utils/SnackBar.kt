package com.ndds.homelauncher.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.ndds.homelauncher.R

class SnackBar(val context: Context, val rootView: ViewGroup) {
    var isShowing = false
    var topInset = 0f
    fun show(message: CharSequence, icon: Drawable?) {
        val snackBarView = LayoutInflater.from(context).inflate(R.layout.snackbar, rootView, false)
        snackBarView.findViewById<TextView>(R.id.snackbar_label).text = message
        snackBarView.findViewById<ImageView>(R.id.snackbar_icon).let {
            if (icon == null)
                it.visibility = View.GONE
            else
                it.setImageDrawable(icon)
        }
        snackBarView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val valueAnimator = ValueAnimator.ofFloat(-snackBarView.measuredHeight.toFloat(), topInset + context.resources.getDimension(
            R.dimen.snackBar_top_margin))
        valueAnimator.duration = 500
        valueAnimator.interpolator = DecelerateInterpolator()
        valueAnimator.addUpdateListener { animator ->
            snackBarView.translationY = animator.animatedValue as Float
            if (animator.animatedFraction == 1f)
                Handler(Looper.getMainLooper()).postDelayed({
                    exit(snackBarView)
                }, 1500)
        }
        rootView.addView(snackBarView)
        valueAnimator.start()
        isShowing = true
    }
    fun exit(snackBarView: View) {
        val valueAnimator = ValueAnimator.ofFloat(snackBarView.translationY, -snackBarView.measuredHeight.toFloat())
        valueAnimator.duration = 500
        valueAnimator.interpolator = AccelerateInterpolator()
        valueAnimator.addUpdateListener { animator ->
            snackBarView.translationY = animator.animatedValue as Float
            if (animator.animatedFraction == 1f) {
                rootView.removeView(snackBarView)
                isShowing = false
            }
        }
        valueAnimator.start()
    }
}