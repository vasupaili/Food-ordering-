package com.mad.mylibrary

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Calendar
import java.util.Date
import java.util.concurrent.ExecutionException

object Utilities {
    @Throws(ExecutionException::class, InterruptedException::class, IOException::class)
    fun reizeImageFileWithGlide(path: String?): File {
        val imgFile = File(path)
        val myBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
        val resized = Bitmap.createScaledBitmap(
            myBitmap, (myBitmap.width * 0.8).toInt(), (myBitmap.height * 0.8).toInt(),
            true
        )
        val file = File("prova.png")
        val bos = ByteArrayOutputStream()
        val fos = FileOutputStream(file)
        resized.compress(Bitmap.CompressFormat.PNG, 10, bos)
        fos.write(bos.toByteArray())
        fos.flush()
        fos.close()
        return file
    }

    fun updateInfoDish(dishes: HashMap<String, Int>?) {
        val getDishes: Query = FirebaseDatabase.getInstance().getReference(
            SharedClass.RESTAURATEUR_INFO + "/" + SharedClass.ROOT_UID
                    + "/" + SharedClass.RESERVATION_PATH
        )
        getDishes.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (d in dataSnapshot.children) {
                        val dishItem = d.getValue(
                            DishItem::class.java
                        )
                        if (dishes!!.containsKey(dishItem!!.name)) {
                            val keyDish = d.key
                            val updateDish: Query = FirebaseDatabase.getInstance().getReference(
                                SharedClass.RESTAURATEUR_INFO + "/" + SharedClass.ROOT_UID
                                        + "/" + SharedClass.RESERVATION_PATH
                            ).child(keyDish!!)
                            updateDish.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        val newDishItem = dataSnapshot.getValue(
                                            DishItem::class.java
                                        )


                                        if (newDishItem != null) {
                                            newDishItem.quantity=newDishItem.quantity - dishes.get(dishItem.name)!!
                                        }
                                        if (newDishItem != null) {
                                            newDishItem.frequency=newDishItem.frequency + dishes.get(dishItem.name)!!
                                        }
                                        val dishMap: MutableMap<String?, Any?> = HashMap()
                                        val dishRef = FirebaseDatabase.getInstance().getReference(
                                            SharedClass.RESTAURATEUR_INFO + "/" + SharedClass.ROOT_UID + "/" + SharedClass.DISHES_PATH
                                        )
                                        dishMap[dataSnapshot.key] = newDishItem
                                        dishRef.updateChildren(dishMap)
                                    }
                                }

                                override fun onCancelled(databaseError: DatabaseError) {}
                            })
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    @JvmStatic
    fun getDateFromTimestamp(timestamp: Long?): String {
        val d = Date(timestamp!!)
        val c = Calendar.getInstance()
        c.time = d
        val hourValue = c[Calendar.HOUR]
        val minValue = c[Calendar.MINUTE]
        var hourString = Integer.toString(hourValue)
        var minString = Integer.toString(minValue)
        if (hourValue < 10) hourString = "0$hourValue"
        if (minValue < 10) minString = "0$minValue"
        return "$hourString:$minString"
    }
}