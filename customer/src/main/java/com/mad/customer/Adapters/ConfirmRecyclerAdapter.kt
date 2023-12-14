package com.mad.customer.Adaptersimport

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mad.customer.R
import com.mad.customer.UI.Confirm

class ConfirmRecyclerAdapter constructor(
    context: Context?,
    var names: ArrayList<String>,
    var prices: ArrayList<String>,
    var quantities: ArrayList<String>,
    var confirm: Confirm
) : RecyclerView.Adapter<ConfirmRecyclerAdapter.MyViewHolder>() {
    var mInflater: LayoutInflater

    init {
        mInflater = LayoutInflater.from(context)
    }

    inner class MyViewHolder constructor(var view_item: View) : RecyclerView.ViewHolder(
        view_item
    ) {
        var dish_name: TextView
        var dish_quant: TextView
        var dish_price: TextView

        init {
            dish_name = view_item.findViewById(R.id.dish_conf_name)
            dish_price = view_item.findViewById(R.id.dish_conf_price)
            dish_quant = view_item.findViewById(R.id.dish_conf_quantity)
        }
    }

    public override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        i: Int
    ): ConfirmRecyclerAdapter.MyViewHolder {
        val view: View = mInflater.inflate(R.layout.dish_confirm_item, viewGroup, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return names.size
    }


    public override fun onBindViewHolder(
        myViewHolder: ConfirmRecyclerAdapter.MyViewHolder,
        position: Int
    ) {
        val name: String = names.get(position)
        val price: String = prices.get(position)
        val quantity: String = quantities.get(position)
        myViewHolder.dish_name.setText(name)
        myViewHolder.dish_quant.setText(quantity)
        myViewHolder.dish_price.setText(price + " €")
    }

//    val itemCount: Int
//        get() {
//            return names.size
//        }
}