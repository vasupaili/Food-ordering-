package com.mad.customer.Adaptersimport

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.mad.customer.R
import com.mad.mylibrary.SharedClass
import java.text.DecimalFormat


class OrderDetailsRecyclerAdapter constructor(
    context: Context?,
    private val keys: ArrayList<String>,
    private val nums: ArrayList<String>,
    private val key: String
) : RecyclerView.Adapter<OrderDetailsRecyclerAdapter.MyViewHolder?>() {
    var mInflater: LayoutInflater

    inner class MyViewHolder constructor(var view_item: View) : RecyclerView.ViewHolder(
        view_item
    ) {
        var dish_name: TextView
        var dish_quant: TextView
        var dish_price: TextView

        init {
            dish_name = view_item.findViewById(R.id.orderdetail_dishname)
            dish_price = view_item.findViewById(R.id.orderdetail_price)
            dish_quant = view_item.findViewById(R.id.orderdetail_quantity)
        }

        fun setData(key_dish: String?, num: String?) {
            val query: Query =
                FirebaseDatabase.getInstance().getReference(SharedClass.RESTAURATEUR_INFO).child(
                    key
                ).child("dishes").child((key_dish)!!)
            query.addValueEventListener(object : ValueEventListener {
                public override fun onDataChange(dataSnapshot: DataSnapshot) {
                    dish_name.setText(dataSnapshot.child("name").getValue() as String?)
                    dish_price.setText(
                        (DecimalFormat("#.##")).format(
                            dataSnapshot.child("price").getValue()
                        ).toString() + " €"
                    )
                }

                public override fun onCancelled(databaseError: DatabaseError) {}
            })
            dish_quant.setText(num)
        }
    }

    init {
        mInflater = LayoutInflater.from(context)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OrderDetailsRecyclerAdapter.MyViewHolder {
        val view: View = mInflater.inflate(R.layout.detailorder_dishitem, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return keys.size
    }

    public override fun onBindViewHolder(
        holder: OrderDetailsRecyclerAdapter.MyViewHolder,
        position: Int
    ) {
        holder.setData(keys.get(position), nums.get(position))
    }

//    val itemCount: Int
//        get() {
//            return keys.size
//        }
}