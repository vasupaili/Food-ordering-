package com.mad.riders.ProfileManagment

import android.Manifest
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.SimpleDateFormat
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.mad.mylibrary.SharedClass.Address
import com.mad.mylibrary.SharedClass.Mail
import com.mad.mylibrary.SharedClass.Name
import com.mad.mylibrary.SharedClass.Phone
import com.mad.mylibrary.SharedClass.Photo
import com.mad.mylibrary.SharedClass.RIDERS_PATH
import com.mad.mylibrary.SharedClass.ROOT_UID
import com.mad.mylibrary.User
import com.mad.riders.R
import java.io.File
import java.io.IOException
import java.util.Date

class EditProfile constructor() : AppCompatActivity() {
    private var dialog_open: Boolean = false
    private var name: String? = null
    private var surname: String? = null
    private var mail: String? = null
    private var phone: String? = null
    private var currentPhotoPath: String? = null
    private var error_msg: String? = null
    private val url: Uri? = null
    private val user_data: SharedPreferences? = null
    private val first_check: SharedPreferences? = null
    private var storage: FirebaseStorage? = null
    var database: FirebaseDatabase? = null
    private var storageReference: StorageReference? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = FirebaseDatabase.getInstance()
        setContentView(R.layout.fragment_edit_profile)
        val auth: FirebaseAuth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        storageReference = storage!!.getReference()
        val confirm_reg: Button = findViewById(R.id.confirm_registration)
        confirm_reg.setOnClickListener(View.OnClickListener({ e: View? ->
            if (checkFields()) {
                uploadImage(ROOT_UID)
            } else {
                Toast.makeText(getApplicationContext(), error_msg, Toast.LENGTH_LONG).show()
            }
        }))
        findViewById<View>(R.id.plus).setOnClickListener(View.OnClickListener({ p: View? -> editPhoto() }))
        findViewById<View>(R.id.img_profile).setOnClickListener(View.OnClickListener({ e: View? -> editPhoto() }))
    }

    private fun uploadImage(UID: String) {
        if (currentPhotoPath != null) {
            val progressDialog: ProgressDialog = ProgressDialog(this)
            progressDialog.setTitle("Uploading...")
            progressDialog.show()
            val ref: StorageReference =
                storageReference!!.child("images/").child("riders/").child(UID)
            val file: File? = null
            val imageUri: Uri = Uri.fromFile(File(currentPhotoPath))
            ref.putFile(imageUri)
                .continueWithTask<Uri>(object : Continuation<UploadTask.TaskSnapshot?, Task<Uri>> {
                    @Throws(Exception::class)
                    public override fun then(task: Task<UploadTask.TaskSnapshot?>): Task<Uri> {
                        if (!task.isSuccessful()) {
                            throw (task.getException())!!
                        }
                        return ref.getDownloadUrl()
                    }
                }).addOnCompleteListener(object : OnCompleteListener<Uri> {
                public override fun onComplete(task: Task<Uri>) {
                    if (task.isSuccessful()) {
                        val downUri: Uri = task.getResult()
                        Log.d("URL", "onComplete: Url: " + downUri.toString())
                        val new_user: MutableMap<String, Any> = HashMap()
                        new_user.put(
                            "rider_info", User(
                                "gallottino",
                                (name)!!,
                                (surname)!!,
                                (mail)!!,
                                (phone)!!, "",
                                downUri.toString()
                            )
                        )
                        new_user.put("available", true)
                        val myRef: DatabaseReference =
                            database!!.getReference(RIDERS_PATH + "/" + UID)
                        myRef.updateChildren(new_user)
                        setResult(1)
                        finish()
                    }
                }
            })
        } else {
            val new_user: MutableMap<String, Any> = HashMap()
            new_user.put(
                "rider_info", User(
                    "gallottino", (name)!!, (surname)!!, (mail)!!, (phone)!!, "", ""
                )
            )
            new_user.put("available", true)
            val myRef: DatabaseReference = database!!.getReference(RIDERS_PATH + "/" + UID)
            myRef.updateChildren(new_user)
            setResult(1)
            finish()
        }
    }

    private fun editPhoto() {
        val alertDialog: AlertDialog =
            AlertDialog.Builder(this@EditProfile, R.style.AlertDialogStyle).create()
        val factory: LayoutInflater = LayoutInflater.from(this@EditProfile)
        val view: View = factory.inflate(R.layout.custom_dialog, null)
        dialog_open = true
        alertDialog.setOnCancelListener(DialogInterface.OnCancelListener({ dialog: DialogInterface? ->
            dialog_open = false
            alertDialog.dismiss()
        }))
        view.findViewById<View>(R.id.camera).setOnClickListener(View.OnClickListener({ c: View? ->
            cameraIntent()
            dialog_open = false
            alertDialog.dismiss()
        }))
        view.findViewById<View>(R.id.gallery).setOnClickListener(View.OnClickListener({ g: View? ->
            galleryIntent()
            dialog_open = false
            alertDialog.dismiss()
        }))
        alertDialog.setView(view)
        alertDialog.setButton(
            AlertDialog.BUTTON_NEUTRAL,
            "Camera",
            DialogInterface.OnClickListener({ dialog: DialogInterface, which: Int ->
                cameraIntent()
                dialog_open = false
                dialog.dismiss()
            })
        )
        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE,
            "Gallery",
            DialogInterface.OnClickListener({ dialog: DialogInterface, which: Int ->
                galleryIntent()
                dialog_open = false
                dialog.dismiss()
            })
        )
        alertDialog.show()
    }

    private fun cameraIntent() {
        val takePictureIntent: Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                // Error occurred while creating the File
                Log.d("FILE: ", "error creating file")
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "com.mad.riders.fileprovider",
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
                PERMISSION_GALLERY_REQUEST
            )
        } else {
            val photoPickerIntent: Intent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.setType("image/*")
            startActivityForResult(photoPickerIntent, 1)
        }
    }

    private fun checkFields(): Boolean {
        name = (findViewById<View>(R.id.name) as EditText).getText().toString()
        surname = (findViewById<View>(R.id.surname) as EditText).getText().toString()
        mail = (findViewById<View>(R.id.mail) as EditText).getText().toString()
        phone = (findViewById<View>(R.id.phone2) as EditText).getText().toString()
        if (name!!.trim({ it <= ' ' }).length == 0) {
            error_msg = "Insert name"
            return false
        }
        if (surname!!.trim({ it <= ' ' }).length == 0) {
            error_msg = "Insert address"
            return false
        }
        if (mail!!.trim({ it <= ' ' }).length == 0 || !Patterns.EMAIL_ADDRESS.matcher(mail)
                .matches()
        ) {
            error_msg = "Insert e-mail"
            return false
        }
        if (phone!!.trim({ it <= ' ' }).length == 0) {
            error_msg = "Insert phone number"
            return false
        }
        return true
    }

    @Throws(IOException::class)
    private fun setPhoto(photoPath: String) {
        val imgFile: File = File(photoPath)
        val myBitmap: Bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath())
        (findViewById<View>(R.id.img_profile) as ImageView).setImageBitmap(myBitmap)
    }

    @Throws(IOException::class)
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
            PERMISSION_GALLERY_REQUEST -> {

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
            val filePathColumn: Array<String> = arrayOf(MediaStore.Images.Media.DATA)
            val cursor: Cursor? = getContentResolver().query(
                (selectedImage)!!,
                filePathColumn, null, null, null
            )
            cursor!!.moveToFirst()
            val columnIndex: Int = cursor.getColumnIndex(filePathColumn.get(0))
            val picturePath: String = cursor.getString(columnIndex)
            cursor.close()

            //Log.d("Photo path: ", picturePath);
            currentPhotoPath = picturePath
        }
        if ((requestCode == 1 || requestCode == 2) && resultCode == RESULT_OK) {
            val imgFile: File = File(currentPhotoPath)
            val myBitmap: Bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath())
            (findViewById<View>(R.id.img_profile) as ImageView).setImageBitmap(myBitmap)
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putString(
            Name,
            (findViewById<View>(R.id.name) as EditText).getText().toString()
        )
        savedInstanceState.putString(
            Address,
            (findViewById<View>(R.id.surname) as EditText).getText().toString()
        )
        savedInstanceState.putString(
            Mail,
            (findViewById<View>(R.id.mail) as EditText).getText().toString()
        )
        savedInstanceState.putString(
            Phone,
            (findViewById<View>(R.id.phone2) as EditText).getText().toString()
        )
        savedInstanceState.putString(Photo, currentPhotoPath)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        (findViewById<View>(R.id.name) as EditText).setText(savedInstanceState.getString(Name))
        (findViewById<View>(R.id.surname) as EditText).setText(savedInstanceState.getString(Address))
        (findViewById<View>(R.id.mail) as EditText).setText(savedInstanceState.getString(Mail))
        (findViewById<View>(R.id.phone2) as EditText).setText(savedInstanceState.getString(Phone))
        currentPhotoPath = savedInstanceState.getString(Photo)
        if (currentPhotoPath != null) {
            try {
                setPhoto(currentPhotoPath!!)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private val PERMISSION_GALLERY_REQUEST: Int = 1
    }
}