package com.mad.customer.UI

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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.mad.customer.R
import com.mad.mylibrary.SharedClass
import com.mad.mylibrary.User
import java.io.File
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import java.util.Objects
import java.util.UUID


class SignUp : AppCompatActivity() {
    private var dialog_open = false
    private var name: String? = null
    private var surname: String? = null
    private var mail: String? = null
    private var phone: String? = null
    private var currentPhotoPath: String? = null
    private var psw: String? = null
    private val psw_confirm: String? = null
    private var address: String? = null
    private var error_msg: String? = null
    private var database: FirebaseDatabase? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        database = FirebaseDatabase.getInstance()
        val auth = FirebaseAuth.getInstance()

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
        findViewById<View>(R.id.address).setOnClickListener { l: View? ->
            val intent = Autocomplete.IntentBuilder(
                AutocompleteActivityMode.FULLSCREEN, fields
            )
                .build(this)
            startActivityForResult(intent, 3)
        }
        val confirm_reg = findViewById<Button>(R.id.sign_up)
        confirm_reg.setOnClickListener { e: View? ->
            if (checkFields()) {
                auth.createUserWithEmailAndPassword(mail!!, psw!!)
                    .addOnCompleteListener(this) { task: Task<AuthResult?> ->
                        if (task.isSuccessful) {
                            SharedClass.ROOT_UID = auth.uid.toString()
                            storeDatabase()
                        } else {
                            Log.d("ERROR", "createUserWithEmail:failure", task.exception)
                        }
                    }
            } else {
                Toast.makeText(applicationContext, error_msg, Toast.LENGTH_LONG).show()
            }
        }
        findViewById<View>(R.id.plus).setOnClickListener { p: View? -> editPhoto() }
        findViewById<View>(R.id.img_profile).setOnClickListener { e: View? -> editPhoto() }
    }

    private fun storeDatabase() {
        val progressDialog = ProgressDialog(this)
        val myRef = database!!.getReference(SharedClass.CUSTOMER_PATH + "/" + SharedClass.ROOT_UID)
        progressDialog.setTitle("Creating profile...")
        progressDialog.show()
        if (currentPhotoPath != null) {
            val url = Uri.fromFile(File(currentPhotoPath))
            val storageReference = FirebaseStorage.getInstance().reference
            val ref = storageReference.child("images/" + UUID.randomUUID().toString())
            ref.putFile(url).continueWithTask<Uri> { task: Task<UploadTask.TaskSnapshot?> ->
                if (!task.isSuccessful) {
                    throw task.exception!!
                }
                ref.downloadUrl
            }
                .addOnCompleteListener { task: Task<Uri> ->
                    if (task.isSuccessful) {
                        val downUri = task.result
                        Log.d("URL", "onComplete: Url: $downUri")
                        val new_user: MutableMap<String, Any> = HashMap()
                        new_user["customer_info"] = User(
                            "malanti", name!!, surname!!, mail!!, phone!!, address!!, downUri.toString()
                        )
                        myRef.updateChildren(new_user)
                        val i = Intent()
                        setResult(1, i)
                        progressDialog.dismiss()
                        finish()
                    }
                }.addOnFailureListener { task: Exception -> Log.d("FAILURE", task.message!!) }
        } else {
            val new_user: MutableMap<String, Any> = HashMap()
            new_user["customer_info"] = User(
                "malnati", name!!, surname!!, mail!!, phone!!, address!!, null
            )
            myRef.updateChildren(new_user)
            val i = Intent()
            setResult(1, i)
            progressDialog.dismiss()
            finish()
        }
    }

    private fun editPhoto() {
        val alertDialog =
            AlertDialog.Builder(this@SignUp, R.style.AppTheme_AlertDialogStyle).create()
        val factory = LayoutInflater.from(this@SignUp)
        val view = factory.inflate(R.layout.custom_dialog, null)
        dialog_open = true
        alertDialog.setOnCancelListener { dialog: DialogInterface? ->
            dialog_open = false
            alertDialog.dismiss()
        }
        view.findViewById<View>(R.id.camera).setOnClickListener { c: View? ->
            cameraIntent()
            dialog_open = false
            alertDialog.dismiss()
        }
        view.findViewById<View>(R.id.gallery).setOnClickListener { g: View? ->
            galleryIntent()
            dialog_open = false
            alertDialog.dismiss()
        }
        view.findViewById<View>(R.id.button_camera).setOnClickListener { v: View? ->
            cameraIntent()
            dialog_open = false
            alertDialog.dismiss()
        }
        view.findViewById<View>(R.id.button_gallery).setOnClickListener { r: View? ->
            galleryIntent()
            dialog_open = false
            alertDialog.dismiss()
        }
        alertDialog.setView(view)
        alertDialog.show()
    }

    private fun cameraIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile = createImageFile()
            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(
                    this,
                    "com.mad.customer.fileprovider",
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
                this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                SharedClass.PERMISSION_GALLERY_REQUEST
            )
        } else {
            val photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            startActivityForResult(photoPickerIntent, 1)
        }
    }

    private fun checkFields(): Boolean {
        name = (findViewById<View>(R.id.name) as EditText).text.toString()
        surname = (findViewById<View>(R.id.surname) as EditText).text.toString()
        mail = (findViewById<View>(R.id.mail) as EditText).text.toString()
        phone = (findViewById<View>(R.id.phone) as EditText).text.toString()
        psw = (findViewById<View>(R.id.psw) as EditText).text.toString()
        address = (findViewById<View>(R.id.address) as EditText).text.toString()
        if (name!!.trim { it <= ' ' }.length == 0) {
            error_msg = "Fill name"
            return false
        }
        if (surname!!.trim { it <= ' ' }.length == 0) {
            error_msg = "Fill address"
            return false
        }
        if (mail!!.trim { it <= ' ' }.length == 0 || !Patterns.EMAIL_ADDRESS.matcher(mail)
                .matches()
        ) {
            error_msg = "Invalid e-mail"
            return false
        }
        if (phone!!.trim { it <= ' ' }.length != 10) {
            error_msg = "Invalid phone number"
            return false
        }
        return true
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
            SharedClass.PERMISSION_GALLERY_REQUEST -> {

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
        if (requestCode == 3) {
            if (resultCode == RESULT_OK) {
                val place = Autocomplete.getPlaceFromIntent(data)
                val address_text = findViewById<EditText>(R.id.address)
                address_text.setText(place.address)
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
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putString(
            SharedClass.Name,
            (findViewById<View>(R.id.name) as EditText).text.toString()
        )
        savedInstanceState.putString(
            SharedClass.Address,
            (findViewById<View>(R.id.surname) as EditText).text.toString()
        )
        savedInstanceState.putString(
            SharedClass.Mail,
            (findViewById<View>(R.id.mail) as EditText).text.toString()
        )
        savedInstanceState.putString(
            SharedClass.Phone,
            (findViewById<View>(R.id.phone) as EditText).text.toString()
        )
        savedInstanceState.putString(SharedClass.Photo, currentPhotoPath)
        savedInstanceState.putBoolean(SharedClass.CameraOpen, dialog_open)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        (findViewById<View>(R.id.name) as EditText).setText(savedInstanceState.getString(SharedClass.Name))
        (findViewById<View>(R.id.surname) as EditText).setText(
            savedInstanceState.getString(
                SharedClass.Address
            )
        )
        (findViewById<View>(R.id.mail) as EditText).setText(savedInstanceState.getString(SharedClass.Mail))
        (findViewById<View>(R.id.phone) as EditText).setText(
            savedInstanceState.getString(
                SharedClass.Phone
            )
        )
        currentPhotoPath = savedInstanceState.getString(SharedClass.Photo)
        if (currentPhotoPath != null) {
            Glide.with(applicationContext).load(currentPhotoPath)
                .into((findViewById<View>(R.id.img_profile) as ImageView))
        }
        if (savedInstanceState.getBoolean(SharedClass.CameraOpen)) editPhoto()
    }
}