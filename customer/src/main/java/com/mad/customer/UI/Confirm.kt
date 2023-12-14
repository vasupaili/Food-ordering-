package com.mad.customer.UI

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.mad.customer.Adaptersimport.ConfirmRecyclerAdapter
import com.mad.customer.Itemsimport.OrderCustomerItem
import com.mad.customer.R
import com.mad.mylibrary.OrderItem
import com.mad.mylibrary.SharedClass
import java.util.Calendar

class Confirm : AppCompatActivity() {
    private var tot: String? = null
    private var resAddr: String? = null
    private var resName: String? = null
    private var resPhoto: String? = null
    private var keys: ArrayList<String>? = null
    private var names: ArrayList<String>? = null
    private var prices: ArrayList<String>? = null
    private var nums: ArrayList<String>? = null
    private var key: String? = null
    private lateinit var recyclerView: RecyclerView
    private var mAdapter: RecyclerView.Adapter<*>? = null
    private var layoutManager: RecyclerView.LayoutManager? = null
    private var desiredTime = ""
    private lateinit var desiredTimeButton: Button
    private var time: Long? = null
    private var timeOpen_open = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        incomingIntent
        desiredTimeButton = findViewById(R.id.desired_time)
        desiredTimeButton.setOnClickListener(View.OnClickListener { l: View? -> setDesiredTimeDialog() })
        findViewById<View>(R.id.confirm_order_button).setOnClickListener { e: View? ->
            if (desiredTime.trim { it <= ' ' }.length > 0) {
                val myRef1 = FirebaseDatabase.getInstance().getReference(
                    SharedClass.RESTAURATEUR_INFO + "/" +
                            key + SharedClass.RESERVATION_PATH
                )

                //Push sul database per storico ordini utente
                val myRef2 = FirebaseDatabase.getInstance()
                    .getReference(SharedClass.CUSTOMER_PATH + "/" + SharedClass.ROOT_UID)
                    .child("orders")
                val order = HashMap<String?, Any>()
                val dishes = HashMap<String, Int>()
                for (key in keys!!) {
                    dishes[key] = nums!![keys!!.indexOf(key)].toInt()
                }
                val keyOrder = myRef2.push().key //key dell'ordine generata
                if (keyOrder != null) {
                    order[keyOrder] = OrderCustomerItem(
                        key,
                        SharedClass.user!!.addr,
                        tot,
                        dishes,
                        time,
                        Long.MAX_VALUE - time!!,
                        SharedClass.STATUS_UNKNOWN,
                        true
                    )
                    myRef2.updateChildren(order)

                    //Push sul database per ristoratore
                    val piatti = HashMap<String, Int>()
                    for (name in names!!) {
                        piatti[name] = nums!![name.indexOf(name)].toInt()
                    }
                    val orderMap = HashMap<String?, Any>()
                    orderMap[keyOrder] = OrderItem(
                        SharedClass.ROOT_UID,
                        SharedClass.user!!.addr,
                        tot,
                        SharedClass.STATUS_UNKNOWN,
                        piatti,
                        time
                    )
                    myRef1.updateChildren(orderMap)

                    SharedClass.orderToTrack[keyOrder] = SharedClass.STATUS_UNKNOWN
                    Toast.makeText(this, "Order confirmed", Toast.LENGTH_LONG).show()
                    setResult(1)
                    finish()
                }
            } else Toast.makeText(this, "Please select desired time", Toast.LENGTH_LONG).show()
        }
    }

    private val incomingIntent: Unit
        private get() {
            keys = intent.getStringArrayListExtra("keys")
            names = intent.getStringArrayListExtra("names")
            prices = intent.getStringArrayListExtra("prices")
            nums = intent.getStringArrayListExtra("nums")
            key = intent.getStringExtra("key")
            resAddr = intent.getStringExtra("raddr")
            resPhoto = intent.getStringExtra("photo")
            resName = intent.getStringExtra("rname")
            recyclerView = findViewById(R.id.dish_conf_recyclerview)
            mAdapter = ConfirmRecyclerAdapter(this, names!!, prices!!, nums!!, this@Confirm)
            layoutManager = LinearLayoutManager(this)
            recyclerView.setAdapter(mAdapter)
            recyclerView.setLayoutManager(layoutManager)
            updatePrice()
        }

    private fun calcoloTotale(prices: ArrayList<String>?, nums: ArrayList<String>?): String {
        var tot = 0f
        for (i in prices!!.indices) {
            val price = prices[i].toFloat()
            val num = nums!![i].toFloat()
            tot = tot + price * num
        }
        return java.lang.Float.toString(tot)
    }

    private fun setTimeValue(): Array<String?> {
        val cent = arrayOfNulls<String>(100)
        for (i in 0..99) {
            if (i < 10) {
                cent[i] = "0$i"
            } else {
                cent[i] = "" + i
            }
        }
        return cent
    }

    private fun setDesiredTimeDialog() {
        val openingTimeDialog =
            AlertDialog.Builder(this, R.style.AppTheme_AlertDialogStyle).create()
        val inflater = LayoutInflater.from(this@Confirm)
        val viewOpening = inflater.inflate(R.layout.opening_time_dialog, null)
        timeOpen_open = true
        val hour = viewOpening.findViewById<NumberPicker>(R.id.hour_picker)
        val min = viewOpening.findViewById<NumberPicker>(R.id.min_picker)
        openingTimeDialog.setView(viewOpening)
        openingTimeDialog.setButton(
            AlertDialog.BUTTON_POSITIVE,
            "OK"
        ) { dialog: DialogInterface?, which: Int ->
            timeOpen_open = false
            val hourValue = hour.value
            val minValue = min.value
            var hourString = Integer.toString(hourValue)
            var minString = Integer.toString(minValue)
            if (hourValue < 10) hourString = "0$hourValue"
            if (minValue < 10) minString = "0$minValue"
            desiredTime = "$hourString:$minString"
            time = getDate(hourValue, minValue)
            desiredTimeButton!!.text = desiredTime
        }
        val hours = setTimeValue()
        hour.displayedValues = hours
        hour.minValue = 0
        hour.maxValue = 23
        hour.value = 0
        val mins = setTimeValue()
        min.displayedValues = mins
        min.minValue = 0
        min.maxValue = 59
        min.value = 0
        openingTimeDialog.show()
    }

    private fun getDate(hour: Int, min: Int): Long {
        val cal = Calendar.getInstance()
        cal[Calendar.HOUR_OF_DAY] = hour
        cal[Calendar.MINUTE] = min
        cal[Calendar.SECOND] = 0
        cal[Calendar.MILLISECOND] = 0
        var date = cal.time
        if (cal.before(Calendar.getInstance())) {
            cal[Calendar.DATE] = cal[Calendar.DATE] + 1
            date = cal.time
        }
        return date.time
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putBoolean(SharedClass.TimeOpen, timeOpen_open)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState.getBoolean(SharedClass.TimeOpen)) setDesiredTimeDialog()
    }

    fun updatePrice() {
        tot = calcoloTotale(prices, nums)
        val totale = findViewById<TextView>(R.id.totale)
        totale.text = "$tot €"
    }
}