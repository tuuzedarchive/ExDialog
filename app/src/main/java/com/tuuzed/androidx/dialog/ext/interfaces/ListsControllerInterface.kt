package com.tuuzed.androidx.dialog.ext.interfaces

import androidx.annotation.ColorInt
import com.github.ybq.android.spinkit.sprite.Sprite

internal interface ListsControllerInterface {
    fun showLoadingView(icon: Sprite? = null, @ColorInt color: Int? = null)
    fun showClickButton(text: CharSequence, @ColorInt color: Int? = null, click: () -> Unit)
    fun items(items: List<*>)
}