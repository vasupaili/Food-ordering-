package com.mad.appetit.Startup

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask

import com.mad.appetit.R
import com.mad.mylibrary.Restaurateur
import com.mad.mylibrary.SharedClass.PERMISSION_GALLERY_REQUEST
import com.mad.mylibrary.SharedClass.RESTAURATEUR_INFO
import com.mad.mylibrary.SharedClass.ROOT_UID
import com.mad.mylibrary.SharedClass.SIGNUP
import com.mad.mylibrary.StarItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import java.util.Objects
import java.util.UUID

class SignUp : AppCompatActivity() {
    private var mail: String? = null
    private var psw: String? = null
    private var name: String? = null
    private var addr: String? = null
    private var descr: String? = null
    private var phone: String? = null
    private var errMsg = ""
    private var currentPhotoPath: String? = null
    private var openingTime: String? = null
    private var closingTime: String? = null
    private var latitude = 0.0
    private var longitude = 0.0
    private lateinit var openingTimeButton: Button
    private lateinit var closingTimeButton: Button
    private lateinit var address: Button
    private var progressDialog: ProgressDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize Places.
        Places.initialize(applicationContext, "AIzaSyA2qxKUpKZQyJOz_ZCp0TUM_z6Ynw0eKNw")
        // Create a new Places client instance.
        val placesClient = Places.createClient(this)
        // Set the fields to specify which types of place data to return.
        val fields = Arrays.asList(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.LAT_LNG,
            Place.Field.ADDRESS
        )
        val auth = FirebaseAuth.getInstance()
        address = findViewById(R.id.button_address)
        address.setOnClickListener(View.OnClickListener { l: View? ->
            val intent = Autocomplete.IntentBuilder(
                AutocompleteActivityMode.FULLSCREEN, fields
            )
                .build(this)
            startActivityForResult(intent, 3)
        })
        findViewById<View>(R.id.plus).setOnClickListener { p: View? -> editPhoto() }
        findViewById<View>(R.id.img_profile).setOnClickListener { e: View? -> editPhoto() }
        openingTimeButton = findViewById(R.id.opening_time)
        openingTimeButton.setOnClickListener(View.OnClickListener { h: View? -> setOpeningTimeDialog() })
        closingTimeButton = findViewById(R.id.opening_time2)
        closingTimeButton.setOnClickListener(View.OnClickListener { h: View? -> setClosingTimeDialog() })
        findViewById<View>(R.id.button).setOnClickListener { e: View? ->
            if (checkFields()) {
                progressDialog = ProgressDialog(this@SignUp)
                progressDialog!!.setTitle("Creating profile.\nOperation may take few minutes...")
                progressDialog!!.setCancelable(false)
                progressDialog!!.show()
                auth.createUserWithEmailAndPassword(mail!!, psw!!)
                    .addOnCompleteListener(this) { task: Task<AuthResult?> ->
                        if (task.isSuccessful) {
                            ROOT_UID = auth.uid.toString()
                            storeDatabase()
                        } else {
                            //Log.w("SIGN IN", "Error: createUserWithEmail:failure", task.getException());
                            Toast.makeText(
                                this@SignUp,
                                "Registration failed. Try again",
                                Toast.LENGTH_LONG
                            ).show()
                            progressDialog!!.dismiss()
                        }
                    }
            } else {
                Toast.makeText(this@SignUp, errMsg, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun checkFields(): Boolean {
        mail = (findViewById<View>(R.id.mail) as EditText).text.toString()
        psw = (findViewById<View>(R.id.psw) as EditText).text.toString()
        name = (findViewById<View>(R.id.name) as EditText).text.toString()
        addr = (findViewById<View>(R.id.button_address) as Button).text.toString()
        descr = (findViewById<View>(R.id.description) as EditText).text.toString()
        phone = (findViewById<View>(R.id.time_text) as EditText).text.toString()
        if (mail!!.trim { it <= ' ' }.length == 0 || !Patterns.EMAIL_ADDRESS.matcher(mail)
                .matches()
        ) {
            errMsg = "Invalid Mail"
            return false
        }
        if (psw!!.trim { it <= ' ' }.length < 6) {
            errMsg = "Password should be at least 6 characters"
            return false
        }
        if (name!!.trim { it <= ' ' }.length == 0) {
            errMsg = "Fill name"
            return false
        }
        if (addr!!.trim { it <= ' ' }.length == 0) {
            errMsg = "Fill address"
            return false
        }
        if (phone!!.trim { it <= ' ' }.length != 10) {
            errMsg = "Invalid phone number"
            return false
        }
        if (openingTime!!.trim { it <= ' ' }.length == 0) {
            errMsg = "Fill opening time"
            return false
        }
        if (closingTime!!.trim { it <= ' ' }.length == 0) {
            errMsg = "Fill closing time"
            return false
        }
        return true
    }

    private fun editPhoto() {
        val alertDialog = AlertDialog.Builder(this@SignUp, R.style.AlertDialogStyle).create()
        val factory = LayoutInflater.from(this@SignUp)
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

    private fun setOpeningTimeDialog() {
        val openingTimeDialog = AlertDialog.Builder(this).create()
        val inflater = LayoutInflater.from(this@SignUp)
        val viewOpening = inflater.inflate(R.layout.opening_time_dialog, null)
        val hour = viewOpening.findViewById<NumberPicker>(R.id.hour_picker)
        val min = viewOpening.findViewById<NumberPicker>(R.id.min_picker)
        openingTimeDialog.setView(viewOpening)
        openingTimeDialog.setButton(
            AlertDialog.BUTTON_POSITIVE,
            "OK"
        ) { dialog: DialogInterface?, which: Int ->
            val hourValue = hour.value
            val minValue = min.value
            var hourString = Integer.toString(hourValue)
            var minString = Integer.toString(minValue)
            if (hourValue < 10) hourString = "0$hourValue"
            if (minValue < 10) minString = "0$minValue"
            openingTime = "$hourString:$minString"
            openingTimeButton!!.text = openingTime
        }
        openingTimeDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE,
            "CANCEL"
        ) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
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

    private fun setClosingTimeDialog() {
        val closingTimeDialog = AlertDialog.Builder(this).create()
        val inflater = LayoutInflater.from(this@SignUp)
        val viewClosing = inflater.inflate(R.layout.closing_time_dialog, null)
        val hour = viewClosing.findViewById<NumberPicker>(R.id.hour_picker)
        val min = viewClosing.findViewById<NumberPicker>(R.id.min_picker)
        closingTimeDialog.setView(viewClosing)
        closingTimeDialog.setButton(
            AlertDialog.BUTTON_POSITIVE,
            "OK"
        ) { dialog: DialogInterface?, which: Int ->
            val hourValue = hour.value
            val minValue = min.value
            var hourString = Integer.toString(hourValue)
            var minString = Integer.toString(minValue)
            if (hourValue < 10) hourString = "0$hourValue"
            if (minValue < 10) minString = "0$minValue"
            closingTime = "$hourString:$minString"
            closingTimeButton!!.text = closingTime
        }
        closingTimeDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE,
            "CANCEL"
        ) { dialog: DialogInterface, which: Int -> dialog.dismiss() }
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
        closingTimeDialog.show()
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
        if (requestCode == 3 && resultCode == RESULT_OK) {
            val place = Autocomplete.getPlaceFromIntent(data)
            latitude = place.latLng.latitude
            longitude = place.latLng.longitude
            address!!.text = place.address
            if (currentPhotoPath != null) {
                Glide.with(Objects.requireNonNull(this))
                    .load(currentPhotoPath)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .into((findViewById<View>(R.id.img_profile) as ImageView))
            } else {
                Glide.with(Objects.requireNonNull(this))
                    .load(R.drawable.restaurant_home)
                    .into((findViewById<View>(R.id.img_profile) as ImageView))
            }
            Log.i("TAG", "Place: " + place.address)
        } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
            // TODO: Handle the error.
            val status = Autocomplete.getStatusFromIntent(data)
            Log.i("TAG", status.statusMessage!!)
        } else if (resultCode == RESULT_CANCELED) {
            // The user canceled the operation.
        }
    }

    fun storeDatabase() {
        val myRef = FirebaseDatabase.getInstance().getReference(RESTAURATEUR_INFO + "/" + ROOT_UID)
        val restMap: MutableMap<String, Any> = HashMap()
        val posInfoMap: MutableMap<String, Any> = HashMap()
        if (currentPhotoPath != null) {
            val photoUri = Uri.fromFile(File(currentPhotoPath))
            val storageReference = FirebaseStorage.getInstance().reference
            val ref = storageReference.child("images/" + UUID.randomUUID().toString())
            ref.putFile(photoUri).continueWithTask<Uri> { task: Task<UploadTask.TaskSnapshot?> ->
                if (!task.isSuccessful) {
                    progressDialog!!.dismiss()
                    throw Objects.requireNonNull(task.exception)!!
                }
                ref.downloadUrl
            }
                .addOnCompleteListener { task: Task<Uri> ->
                    if (task.isSuccessful) {
                        val downUri = task.result
                        restMap["info"] = Restaurateur(
                            mail!!, name!!, addr!!, descr!!,
                            "$openingTime - $closingTime", phone!!, downUri.toString()
                        )
                        myRef.updateChildren(restMap)
                        posInfoMap["info_pos"] = LatLng(latitude, longitude)
                        myRef.updateChildren(posInfoMap)
                        restMap.clear()
                        restMap["stars"] = StarItem(0, 0, 0)
                        myRef.updateChildren(restMap)
                        progressDialog!!.dismiss()
                        val i = Intent()
                        setResult(SIGNUP, i)
                        finish()
                    } else {
                        //Log.w("LOGIN", "signInWithCredential:failure", task.getException());
                        progressDialog!!.dismiss()
                        Snackbar.make(
                            findViewById(R.id.email),
                            "Authentication Failed. Try again.",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }.addOnFailureListener { e: Exception? ->
                    progressDialog!!.dismiss()
                    Snackbar.make(
                        findViewById(R.id.email),
                        "Authentication Failed. Try again.",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
        } else {
            restMap["info"] = Restaurateur(
                mail!!, name!!, addr!!, descr!!,
                "$openingTime - $closingTime", phone!!, null
            )
            myRef.updateChildren(restMap)
            posInfoMap["info_pos"] = LatLng(latitude, longitude)
            myRef.updateChildren(posInfoMap)
            restMap.clear()
            restMap["stars"] = StarItem(0, 0, 0)
            myRef.updateChildren(restMap)
            progressDialog!!.dismiss()
            val i = Intent()
            setResult(SIGNUP, i)
            finish()
        }
    }
}