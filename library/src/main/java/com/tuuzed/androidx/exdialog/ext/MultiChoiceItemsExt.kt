package com.tuuzed.androidx.exdialog.ext

import android.graphics.drawable.Drawable
import android.widget.CheckBox
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tuuzed.androidx.exdialog.ExDialog
import com.tuuzed.androidx.exdialog.R
import com.tuuzed.androidx.exdialog.internal.interfaces.BasicControllerInterface
import com.tuuzed.androidx.exdialog.internal.interfaces.ExDialogInterface
import com.tuuzed.androidx.exdialog.internal.interfaces.ListsControllerInterface
import com.tuuzed.recyclerview.adapter.AbstractItemViewBinder
import com.tuuzed.recyclerview.adapter.CommonItemViewHolder
import com.tuuzed.recyclerview.adapter.RecyclerViewAdapter

fun <T> ExDialog.multiChoiceItems(
    @StringRes titleRes: Int? = null, title: CharSequence? = null,
    @DrawableRes iconRes: Int? = null, icon: Drawable? = null,
    //
    onSelectedItemChanged: MultiChoiceItemsCallback<T>? = null,
    callback: MultiChoiceItemsCallback<T>? = null,
    items: List<T>? = null,
    selectedIndices: List<Int>? = null,
    disableIndices: List<Int>? = null,
    //
    toReadable: ItemToReadable<T> = { it.toString() },
    func: (MultiChoiceItemsController<T>.() -> Unit)? = null
) {
    lists(titleRes, title, iconRes, icon) {
        MultiChoiceItemsController(
            this@multiChoiceItems, this, toReadable
        ).apply {

            onSelectedItemChanged?.let { onSelectedItemChanged(it) }
            callback?.let { callback(it) }
            items?.let { items(it, selectedIndices ?: emptyList(), disableIndices ?: emptyList()) }

            func?.invoke(this)
        }
    }
}

class MultiChoiceItemsController<T>(
    private val dialog: ExDialog,
    private val delegate: ListsController,
    private val toReadable: ItemToReadable<T>
) : ExDialogInterface by dialog,
    BasicControllerInterface by delegate,
    ListsControllerInterface by delegate {

    private var listAdapter: RecyclerViewAdapter? = null
    private var callback: MultiChoiceItemsCallback<T>? = null
    private var itemClickCallback: MultiChoiceItemsCallback<T>? = null


    init {
        delegate.config { _, listAdapter ->
            this.listAdapter = listAdapter
            listAdapter.bind(Space::class.java, SpaceItemViewBinder)
            listAdapter.bind(Item::class.java, ItemViewBinder(listAdapter))
        }
    }

    fun callback(callback: MultiChoiceItemsCallback<T>?) {
        this.callback = callback
    }

    fun onSelectedItemChanged(callback: MultiChoiceItemsCallback<T>?) {
        this.itemClickCallback = callback
    }

    fun items(items: List<T>, selectedIndices: List<Int> = emptyList(), disableIndices: List<Int> = emptyList()) {
        delegate.items(
            listOf(
                Space,
                *items.mapIndexed { index, item ->
                    Item(item, selectedIndices.contains(index), disableIndices.contains(index))
                }.toTypedArray(),
                Space
            )
        )
    }

    override fun positiveButton(
        textRes: Int?, text: CharSequence?,
        iconRes: Int?, icon: Drawable?,
        colorRes: Int?, color: Int?,
        click: DialogButtonClick?
    ) {
        itemClickCallback?.also { callback ->
            getCheckedItems { indices, items ->
                callback(dialog, indices, items)
            }
        }
        delegate.positiveButton(textRes, text, iconRes, icon, colorRes, color) {
            callback?.also { callback ->
                getCheckedItems { indices, items ->
                    callback(dialog, indices, items)
                }
            }
            click?.invoke(dialog)
        }
    }

    private fun reviseIndex(index: Int): Int = index - 1
    private inline fun getCheckedItems(receiver: (indices: List<Int>, items: List<T>) -> Unit) {
        val indices = mutableListOf<Int>()
        val items = mutableListOf<T>()
        listAdapter?.items?.forEachIndexed { index, item ->
            if (item is Item<*> && item.checked) {
                indices.add(reviseIndex(index))
                @Suppress("UNCHECKED_CAST")
                items.add(item.data as T)
            }
        }
        receiver(indices, items)
    }


    private class Item<T>(val data: T, var checked: Boolean, val disable: Boolean)
    private inner class ItemViewBinder(
        private val listAdapter: RecyclerViewAdapter
    ) : AbstractItemViewBinder<Item<T>>() {
        override fun getLayoutId(): Int = R.layout.listitem_multichoiceitems
        override fun onBindViewHolder(holder: CommonItemViewHolder, item: Item<T>, position: Int) {
            if (item.disable) {
                holder.itemView.also {
                    it.isClickable = false
                    it.isEnabled = false
                    it.alpha = 0.5f
                }
            } else {
                holder.itemView.also {
                    it.isClickable = true
                    it.isEnabled = true
                    it.alpha = 1.0f
                }
            }
            holder.text(R.id.text, toReadable(item.data))
            holder.find<CheckBox>(R.id.checkbox).isChecked = item.checked
            holder.click(R.id.item_layout) {
                item.checked = !item.checked
                listAdapter.notifyItemChanged(position)
                itemClickCallback?.also { callback ->
                    getCheckedItems { indices, items ->
                        callback(dialog, indices, items)
                    }
                }
            }
        }
    }

}


