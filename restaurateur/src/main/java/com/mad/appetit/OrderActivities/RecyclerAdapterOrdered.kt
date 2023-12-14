package com.mad.appetit.OrderActivitiesimport

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mad.appetit.R


public class RecyclerAdapterOrdered(context: Context?, var dishes: ArrayList<String>) :
    RecyclerView.Adapter<RecyclerAdapterOrdered.MyViewHolder?>() {
    var mInflater: LayoutInflater

    init {
        mInflater = LayoutInflater.from(context)
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var dish_name: TextView

        init {
            dish_name = itemView.findViewById(R.id.label)
        }
    }

    override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        i: Int
    ): RecyclerAdapterOrdered.MyViewHolder {
        val view = mInflater.inflate(R.layout.dish_view, viewGroup, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(
        myViewHolder: RecyclerAdapterOrdered.MyViewHolder,
        position: Int
    ) {
        val mCurrent = dishes[position]
        myViewHolder.dish_name.text = mCurrent
    }

    override fun getItemCount(): Int {
        return dishes.size
    }
}