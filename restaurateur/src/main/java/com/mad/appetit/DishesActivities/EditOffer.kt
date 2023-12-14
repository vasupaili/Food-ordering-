package com.mad.appetit.DishesActivitiesimport

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.Toast

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

import com.google.firebase.storage.UploadTask
import com.mad.appetit.R

import com.mad.mylibrary.DishItem
import com.mad.mylibrary.SharedClass.DISHES_PATH
import com.mad.mylibrary.SharedClass.EDIT_EXISTING_DISH
import com.mad.mylibrary.SharedClass.PERMISSION_GALLERY_REQUEST
import com.mad.mylibrary.SharedClass.RESTAURATEUR_INFO
import com.mad.mylibrary.SharedClass.ROOT_UID
import java.io.File
import java.util.Date
import java.util.Objects
import java.util.UUID



class EditOffer : AppCompatActivity() {
    private var frequency = 0
    private var name: String? = null
    private var desc: String? = null
    private var currentPhotoPath: String? = null
    private var error_msg = ""
    private var keyChild: String? = null
    private var priceValue = -1f
    private var quantValue = -1
    private lateinit var imageview: ImageView
    private var editing = false
    private var photoChanged = false
    private lateinit var priceButton: Button
    private lateinit var quantButton: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_offer)
        priceButton = findViewById(R.id.price)
        quantButton = findViewById(R.id.quantity)
        priceButton.setOnClickListener(View.OnClickListener { e: View? -> setPrice() })
        quantButton.setOnClickListener(View.OnClickListener { f: View? -> setQuantity() })
        findViewById<View>(R.id.plus).setOnClickListener { p: View? -> editPhoto() }
        findViewById<View>(R.id.img_profile).setOnClickListener { e: View? -> editPhoto() }
        imageview = findViewById(R.id.img_profile)
        val dishName = intent.getStringExtra(EDIT_EXISTING_DISH)
        if (dishName != null) getData(dishName) else imageview.setImageResource(R.drawable.hamburger)
        findViewById<View>(R.id.button).setOnClickListener { e: View? ->
            if (checkFields()) {
                storeDatabase()
                finish()
            } else {
                Toast.makeText(applicationContext, error_msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getData(dishName: String) {
        val myRef = FirebaseDatabase.getInstance().reference
        val query = myRef.child(RESTAURATEUR_INFO + "/" + ROOT_UID + "/" + DISHES_PATH)
            .orderByChild("name").equalTo(dishName)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    var dish = DishItem()
                    for (d in dataSnapshot.children) {
                        dish = d.getValue(DishItem::class.java)!!
                        keyChild = d.key
                        break
                    }
                    editing = true
                    name = dish.name
                    desc = dish.desc
                    priceValue = dish.price
                    quantValue = dish.quantity
                    currentPhotoPath = dish.photo
                    frequency = dish.frequency
                    if (currentPhotoPath != null) Glide.with(applicationContext)
                        .load(currentPhotoPath).diskCacheStrategy(DiskCacheStrategy.RESOURCE).into(
                        (findViewById<View>(R.id.img_profile) as ImageView)
                    ) else Glide.with(applicationContext).load(R.drawable.hamburger)
                        .into(imageview!!)
                    (findViewById<View>(R.id.name) as EditText).setText(name)
                    (findViewById<View>(R.id.description) as EditText).setText(desc)
                    priceButton!!.text = java.lang.Float.toString(priceValue)
                    quantButton!!.text = Integer.toString(quantValue)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("EDIT OFFER", "Failed to read value.", error.toException())
            }
        })
    }

    private fun checkFields(): Boolean {
        name = (findViewById<View>(R.id.name) as EditText).text.toString()
        desc = (findViewById<View>(R.id.description) as EditText).text.toString()
        if (name!!.trim { it <= ' ' }.length == 0) {
            error_msg = "Fill name"
            return false
        }
        if (desc!!.trim { it <= ' ' }.length == 0) {
            error_msg = "Fill description"
            return false
        }
        if (priceValue == -1f) {
            error_msg = "Insert price"
            return false
        }
        if (quantValue == -1) {
            error_msg = "Insert quantity"
            return false
        }
        return true
    }

    private fun setCentsValue(): Array<String?> {
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

    private fun setPrice() {
        val priceDialog = AlertDialog.Builder(this).create()
        val inflater = LayoutInflater.from(this@EditOffer)
        val view = inflater.inflate(R.layout.price_dialog, null)
        val euro = view.findViewById<NumberPicker>(R.id.euro_picker)
        val cent = view.findViewById<NumberPicker>(R.id.cent_picker)
        priceDialog.setView(view)
        priceDialog.setButton(
            AlertDialog.BUTTON_POSITIVE,
            "OK"
        ) { dialog: DialogInterface?, which: Int ->
            val centValue = cent.value.toFloat()
            priceValue = euro.value + centValue / 100
            priceButton!!.text = java.lang.Float.toString(priceValue)
        }
        priceDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE,
            "CANCEL"
        ) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
        euro.minValue = 0
        euro.maxValue = 9999
        val cents = setCentsValue()
        cent.displayedValues = cents
        cent.minValue = 0
        cent.maxValue = 99
        if (priceValue != -1f) {
            euro.value = priceValue.toInt()
            cent.value = priceValue.toString().split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[1].toInt()
        } else {
            euro.value = 0
            cent.value = 0
        }
        priceDialog.show()
    }

    private fun setQuantity() {
        val quantDialog = AlertDialog.Builder(this).create()
        val inflater = LayoutInflater.from(this@EditOffer)
        val view = inflater.inflate(R.layout.quantity_dialog, null)
        val quantity = view.findViewById<NumberPicker>(R.id.quant_picker)
        quantDialog.setView(view)
        quantDialog.setButton(
            AlertDialog.BUTTON_POSITIVE,
            "OK"
        ) { dialog: DialogInterface?, which: Int ->
            quantValue = quantity.value
            quantButton!!.text = Integer.toString(quantValue)
        }
        quantDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE,
            "CANCEL"
        ) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
        quantity.minValue = 1
        quantity.maxValue = 999
        if (quantValue != -1) quantity.value = quantValue else quantity.value = 1
        quantDialog.show()
    }

    private fun editPhoto() {
        val alertDialog = AlertDialog.Builder(this@EditOffer, R.style.AlertDialogStyle).create()
        val factory = LayoutInflater.from(this@EditOffer)
        val view = factory.inflate(R.layout.custom_dialog, null)
        alertDialog.setOnCancelListener { dialog: DialogInterface? -> alertDialog.dismiss() }
        view.findViewById<View>(R.id.camera).setOnClickListener { c: View? ->
            cameraIntent()
            alertDialog.dismiss()
        }
        view.findViewById<View>(R.id.gallery).setOnClickListener { g: View? ->
            galleryIntent()
            alertDialog.dismiss()
        }
        alertDialog.setView(view)
        alertDialog.setButton(
            AlertDialog.BUTTON_NEUTRAL,
            "Camera"
        ) { dialog: DialogInterface, which: Int ->
            cameraIntent()
            dialog.dismiss()
        }
        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE,
            "Gallery"
        ) { dialog: DialogInterface, which: Int ->
            galleryIntent()
            dialog.dismiss()
        }
        alertDialog.show()
    }

    private fun cameraIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile = createImageFile()
            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(
                    this,
                    "com.mad.appetit",
                    photoFile
                )
                photoChanged = true
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, 2)
            }
        }
    }

    private fun galleryIntent() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf<String>(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_GALLERY_REQUEST
            )
        } else {
            val photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            startActivityForResult(photoPickerIntent, 1)
        }
    }

    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "IMG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File(
            storageDir.toString() + File.separator +
                    imageFileName +  /* prefix */
                    ".jpg"
        )

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.absolutePath
        return image
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_GALLERY_REQUEST -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission Run Time: ", "Obtained")
                    val photoPickerIntent = Intent(Intent.ACTION_PICK)
                    photoPickerIntent.type = "image/*"
                    startActivityForResult(photoPickerIntent, 1)
                } else {
                    Log.d("Permission Run Time: ", "Denied")
                    Toast.makeText(
                        applicationContext,
                        "Access to media files denied",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK && null != data) {
            val selectedImage = data.data
            photoChanged = true
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(
                selectedImage!!,
                filePathColumn, null, null, null
            )
            cursor!!.moveToFirst()
            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
            val picturePath = cursor.getString(columnIndex)
            cursor.close()
            currentPhotoPath = picturePath
        }
        if ((requestCode == 1 || requestCode == 2) && resultCode == RESULT_OK) {
            Glide.with(applicationContext).load(currentPhotoPath)
                .into((findViewById<View>(R.id.img_profile) as ImageView))
        }
    }

    private fun storeDatabase() {
        val myRef = FirebaseDatabase.getInstance().getReference(
            RESTAURATEUR_INFO + "/" + ROOT_UID + "/" + DISHES_PATH
        )
        val storageReference = FirebaseStorage.getInstance().reference
        val dishMap: MutableMap<String?, Any> = HashMap()
        if (!editing) keyChild = myRef.push().key
        if (photoChanged) {
            val photoUri = Uri.fromFile(File(currentPhotoPath))
            val ref = storageReference.child("images/" + UUID.randomUUID().toString())
            ref.putFile(photoUri).continueWithTask { task: Task<UploadTask.TaskSnapshot?> ->
                if (!task.isSuccessful) {
                    throw Objects.requireNonNull(task.exception)!!
                }
                ref.downloadUrl
            }
                .addOnCompleteListener { task: Task<Uri> ->
                    if (task.isSuccessful) {
                        val downUri = task.result
                        dishMap[keyChild] = DishItem(
                            name!!,
                            desc!!,
                            priceValue,
                            quantValue,
                            downUri.toString(),
                            frequency
                        )
                        myRef.updateChildren(dishMap)
                    }
                }
        } else {
            if (currentPhotoPath != null) dishMap[keyChild] = DishItem(
                name!!,
                desc!!,
                priceValue,
                quantValue,
                currentPhotoPath,
                frequency
            ) else dishMap[keyChild] = DishItem(
                name!!, desc!!, priceValue, quantValue, null, frequency
            )
            myRef.updateChildren(dishMap)
        }
    }

    override fun onBackPressed() {
        val dialog = AlertDialog.Builder(this@EditOffer).create()
        val inflater = LayoutInflater.from(this@EditOffer)
        val view = inflater.inflate(R.layout.reservation_dialog, null)
        view.findViewById<View>(R.id.button_confirm)
            .setOnClickListener { e: View? -> super.onBackPressed() }
        view.findViewById<View>(R.id.button_cancel)
            .setOnClickListener { e: View? -> dialog.dismiss() }
        dialog.setView(view)
        dialog.setTitle("Are you sure to cancel?")
        dialog.show()
    }
}