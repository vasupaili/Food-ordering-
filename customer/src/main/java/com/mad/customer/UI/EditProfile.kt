package com.mad.customer.UIimport

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.provider.MediaStore
import android.util.Patterns
import android.view.LayoutInflater
import android.view.MenuItem

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task


import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Continuation
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.mad.customer.R
import com.mad.mylibrary.SharedClass
import com.mad.mylibrary.User
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Arrays
import java.util.Objects
import java.util.UUID
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.widget.AutocompleteActivity
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date

class EditProfile constructor() : AppCompatActivity() {
    private var name: String? = null
    private var surname: String? = null
    private var mail: String? = null
    private var phone: String? = null
    private var currentPhotoPath: String? = null
    private var address: String? = null
    private lateinit var addressButton: EditText
    private var dialog_open: Boolean = false
    private var photoChanged: Boolean = false
    private var error_msg: String? = null
    private var database: FirebaseDatabase? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        database = FirebaseDatabase.getInstance()
        data

        // Initialize Places.
        Places.initialize(getApplicationContext(), "AIzaSyA2qxKUpKZQyJOz_ZCp0TUM_z6Ynw0eKNw")
        // Create a new Places client instance.
        val placesClient: PlacesClient = Places.createClient(this)
        // Set the fields to specify which types of place data to return.
        val fields: List<Place.Field> = Arrays.asList(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.LAT_LNG,
            Place.Field.ADDRESS
        )
        addressButton = findViewById(R.id.address_modify)
        addressButton.setOnClickListener(View.OnClickListener { l: View? ->
            val intent: Intent = Autocomplete.IntentBuilder(
                AutocompleteActivityMode.FULLSCREEN, fields
            )
                .build(this)
            startActivityForResult(intent, 3)
        })
        val confirm_reg: Button = findViewById(R.id.back_order_button)
        confirm_reg.setOnClickListener(View.OnClickListener { e: View? ->
            if (checkFields()) {
                storeDatabase()
            } else {
                Toast.makeText(getApplicationContext(), error_msg, Toast.LENGTH_LONG).show()
            }
        })
        getSupportActionBar()!!.setDisplayShowHomeEnabled(true)
        getSupportActionBar()!!.setDisplayHomeAsUpEnabled(true)
        findViewById<View>(R.id.plus).setOnClickListener(View.OnClickListener({ p: View? -> editPhoto() }))
        findViewById<View>(R.id.img_profile).setOnClickListener(View.OnClickListener({ e: View? -> editPhoto() }))
    }

    public override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.getItemId()
        if (id == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private val data: Unit
        private get() {
            name = SharedClass.user!!.name
            surname = SharedClass.user!!.surname
            address = SharedClass.user!!.addr
            mail = SharedClass.user!!.email
            phone = SharedClass.user!!.phone
            currentPhotoPath = SharedClass.user!!.photoPath
            (findViewById<View>(R.id.name) as EditText).setText(name)
            (findViewById<View>(R.id.address_modify) as EditText).setText(address)
            (findViewById<View>(R.id.mail) as EditText).setText(mail)
            (findViewById<View>(R.id.phone2) as EditText).setText(phone)
            (findViewById<View>(R.id.surname) as EditText).setText(surname)
            var inputStream: InputStream? = null
            try {
                val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
                StrictMode.setThreadPolicy(policy)
                inputStream = URL(currentPhotoPath).openStream()
                if (inputStream != null) Glide.with(getApplicationContext()).load(currentPhotoPath)
                    .into(
                        (findViewById<View>(R.id.img_profile) as ImageView?)!!
                    ) else (findViewById<View>(R.id.img_profile) as ImageView).setImageResource(R.drawable.person)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    private fun checkFields(): Boolean {
        name = (findViewById<View>(R.id.name) as EditText).getText().toString()
        surname = (findViewById<View>(R.id.surname) as EditText).getText().toString()
        mail = (findViewById<View>(R.id.mail) as EditText).getText().toString()
        phone = (findViewById<View>(R.id.phone2) as EditText).getText().toString()
        address = (findViewById<View>(R.id.address_modify) as EditText).getText().toString()
        if (name!!.trim({ it <= ' ' }).length == 0) {
            error_msg = "Fill name"
            return false
        }
        if (surname!!.trim({ it <= ' ' }).length == 0) {
            error_msg = "Fill surname"
            return false
        }
        if (mail!!.trim({ it <= ' ' }).length == 0 || !Patterns.EMAIL_ADDRESS.matcher(mail)
                .matches()
        ) {
            error_msg = "Invalid e-mail"
            return false
        }
        if (phone!!.trim({ it <= ' ' }).length != 10) {
            error_msg = "Fill phone number"
            return false
        }
        if (address!!.trim({ it <= ' ' }).length == 0) {
            error_msg = "Fill address"
            return false
        }
        return true
    }

    private fun storeDatabase() {
        val progressDialog: ProgressDialog = ProgressDialog(this)
        val myRef: DatabaseReference = FirebaseDatabase.getInstance()
            .getReference(SharedClass.CUSTOMER_PATH + "/" + SharedClass.ROOT_UID)
        val storageReference: StorageReference = FirebaseStorage.getInstance().getReference()
        val profileMap: MutableMap<String, Any> = HashMap()
        progressDialog.setTitle("Updating profile...")
        progressDialog.show()
        if (photoChanged && currentPhotoPath != null) {
            val photoUri: Uri = Uri.fromFile(File(currentPhotoPath))
            val ref: StorageReference =
                storageReference.child("images/" + UUID.randomUUID().toString())
            ref.putFile(photoUri)
                .continueWithTask<Uri>(Continuation<UploadTask.TaskSnapshot?, Task<Uri>>({ task: Task<UploadTask.TaskSnapshot?> ->
                    if (!task.isSuccessful()) {
                        throw Objects.requireNonNull(task.getException())!!
                    }
                    ref.getDownloadUrl()
                })).addOnCompleteListener(OnCompleteListener<Uri> { task: Task<Uri> ->
                    if (task.isSuccessful()) {
                        val downUri: Uri = task.getResult()
                        profileMap.put(
                            "customer_info", User(
                                "malnati",
                                name!!,
                                surname!!,
                                mail!!,
                                phone!!,
                                address!!,
                                downUri.toString()
                            )
                        )
                        myRef.updateChildren(profileMap)
                        progressDialog.dismiss()
                        finish()
                    }
                })
        } else {
            if (currentPhotoPath != null) profileMap.put(
                "customer_info", User(
                    "malnati", name!!, surname!!, mail!!, phone!!, address!!, currentPhotoPath
                )
            ) else profileMap.put(
                "customer_info", User(
                    "malnati",  name!!, surname!!, mail!!, phone!!, address!!, null
                )
            )
            myRef.updateChildren(profileMap)
            progressDialog.dismiss()
            finish()
        }
    }

    private fun editPhoto() {
        val alertDialog: AlertDialog =
            AlertDialog.Builder(this@EditProfile, R.style.AppTheme_AlertDialogStyle).create()
        val factory: LayoutInflater = LayoutInflater.from(this@EditProfile)
        val view: View = factory.inflate(R.layout.custom_dialog, null)
        dialog_open = true
        alertDialog.setOnCancelListener(DialogInterface.OnCancelListener { dialog: DialogInterface? ->
            dialog_open = false
            alertDialog.dismiss()
        })
        view.findViewById<View>(R.id.camera).setOnClickListener(View.OnClickListener { c: View? ->
            cameraIntent()
            dialog_open = false
            alertDialog.dismiss()
        })
        view.findViewById<View>(R.id.gallery).setOnClickListener(View.OnClickListener { g: View? ->
            galleryIntent()
            dialog_open = false
            alertDialog.dismiss()
        })
        view.findViewById<View>(R.id.button_camera)
            .setOnClickListener(View.OnClickListener { v: View? ->
                cameraIntent()
                dialog_open = false
                alertDialog.dismiss()
            })
        view.findViewById<View>(R.id.button_gallery)
            .setOnClickListener(View.OnClickListener { r: View? ->
                galleryIntent()
                dialog_open = false
                alertDialog.dismiss()
            })
        alertDialog.setView(view)
        alertDialog.show()
    }

    private fun cameraIntent() {
        val takePictureIntent: Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            val photoFile: File? = createImageFile()
            if (photoFile != null) {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "com.mad.customer.fileprovider",
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
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                SharedClass.PERMISSION_GALLERY_REQUEST
            )
        } else {
            val photoPickerIntent: Intent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.setType("image/*")
            startActivityForResult(photoPickerIntent, 1)
        }
    }

    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName: String = "IMG_" + timeStamp + "_"
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image: File = File(
            (storageDir.toString() + File.separator +
                    imageFileName +  /* prefix */
                    ".jpg")
        )

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath()
        return image
    }

    public override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            SharedClass.PERMISSION_GALLERY_REQUEST -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults.get(0) == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission Run Time: ", "Obtained")
                    val photoPickerIntent: Intent = Intent(Intent.ACTION_PICK)
                    photoPickerIntent.setType("image/*")
                    startActivityForResult(photoPickerIntent, 1)
                } else {
                    Log.d("Permission Run Time: ", "Denied")
                    Toast.makeText(
                        getApplicationContext(),
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
        if ((requestCode == 1) && (resultCode == RESULT_OK) && (null != data)) {
            val selectedImage: Uri? = data.getData()
            photoChanged = true
            val filePathColumn: Array<String> = arrayOf(MediaStore.Images.Media.DATA)
            val cursor: Cursor? = getContentResolver().query(
                (selectedImage)!!,
                filePathColumn, null, null, null
            )
            cursor!!.moveToFirst()
            val columnIndex: Int = cursor.getColumnIndex(filePathColumn.get(0))
            val picturePath: String = cursor.getString(columnIndex)
            cursor.close()
            currentPhotoPath = picturePath
        }
        if ((requestCode == 1 || requestCode == 2) && resultCode == RESULT_OK) {
            Glide.with(getApplicationContext()).load(currentPhotoPath)
                .into((findViewById<View>(R.id.img_profile) as ImageView?)!!)
        }
        if (requestCode == 3) {
            if (resultCode == RESULT_OK) {
                val place: Place = Autocomplete.getPlaceFromIntent(data)
                addressButton!!.setText(place.getAddress())
                if (currentPhotoPath != null) {
                    Glide.with(Objects.requireNonNull<EditProfile>(this))
                        .load(currentPhotoPath)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .into((findViewById<View>(R.id.img_profile) as ImageView?)!!)
                } else {
                    Glide.with(Objects.requireNonNull<EditProfile>(this))
                        .load(R.drawable.restaurant_home)
                        .into((findViewById<View>(R.id.img_profile) as ImageView?)!!)
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Handle the error.
                val status: Status = Autocomplete.getStatusFromIntent(data)
                Log.i("TAG", (status.getStatusMessage())!!)
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putString(
            SharedClass.Name,
            (findViewById<View>(R.id.name) as EditText).getText().toString()
        )
        savedInstanceState.putString(
            SharedClass.Address,
            (findViewById<View>(R.id.surname) as EditText).getText().toString()
        )
        savedInstanceState.putString(
            SharedClass.Mail,
            (findViewById<View>(R.id.mail) as EditText).getText().toString()
        )
        savedInstanceState.putString(
            SharedClass.Phone,
            (findViewById<View>(R.id.phone2) as EditText).getText().toString()
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
        (findViewById<View>(R.id.phone2) as EditText).setText(
            savedInstanceState.getString(
                SharedClass.Phone
            )
        )
        currentPhotoPath = savedInstanceState.getString(SharedClass.Photo)
        if (currentPhotoPath != null) Glide.with(getApplicationContext()).load(currentPhotoPath)
            .into(
                (findViewById<View>(R.id.img_profile) as ImageView?)!!
            )
        if (savedInstanceState.getBoolean(SharedClass.CameraOpen)) editPhoto()
    }
}