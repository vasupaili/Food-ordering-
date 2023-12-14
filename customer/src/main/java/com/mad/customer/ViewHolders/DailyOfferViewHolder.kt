package com.mad.customer.ViewHoldersimport

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mad.customer.Itemsimport.DishItem
import com.mad.customer.R
import java.io.InputStream


class DailyOfferViewHolder constructor(val view: View) : RecyclerView.ViewHolder(
    view
) {
    private val dishPhoto: ImageView
    private val dishName: TextView
    private val dishDesc: TextView
    private val dishPrice: TextView
    private val dishQuantity: TextView? = null

    init {
        dishName = view.findViewById(R.id.dish_name)
        dishDesc = view.findViewById(R.id.dish_desc)
        dishPrice = view.findViewById(R.id.dish_price)
        dishPhoto = view.findViewById(R.id.dish_image)
    }

    fun setData(current: DishItem, position: Int) {
        val inputStream: InputStream? = null
        dishName.setText(current.name)
        dishDesc.setText(current.desc)
        dishPrice.setText(current.price.toString() + " €")
        if (current.photoUri!= null) {
            Glide.with(view.getContext()).load(current.photoUri).override(80, 80)
                .into(dishPhoto)
        }
    }
}