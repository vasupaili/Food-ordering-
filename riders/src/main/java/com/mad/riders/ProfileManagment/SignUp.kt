package com.mad.riders.ProfileManagment

import android.Manifest
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.icu.text.SimpleDateFormat
import android.media.ExifInterface
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
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.mad.mylibrary.SharedClass.RIDERS_PATH
import com.mad.mylibrary.SharedClass.ROOT_UID
import com.mad.mylibrary.User
import com.mad.riders.R
import java.io.File
import java.io.IOException
import java.util.Date

class SignUp : AppCompatActivity() {
    private var dialog_open = false
    private var name: String? = null
    private var surname: String? = null
    private var mail: String? = null
    private var phone: String? = null
    private var currentPhotoPath: String? = null
    private var psw: String? = null
    private var psw_confirm: String? = null
    private var error_msg: String? = null
    private val url: Uri? = null
    private val user_data: SharedPreferences? = null
    lateinit var first_check: SharedPreferences
    private var storage: FirebaseStorage? = null
    var database: FirebaseDatabase? = null
    private var storageReference: StorageReference? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = FirebaseDatabase.getInstance()
        setContentView(R.layout.activity_edit_profile)
        val auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        storageReference = storage!!.reference
        val confirm_reg = findViewById<Button>(R.id.confirm_registration)
        confirm_reg.setOnClickListener { e: View? ->
            if (checkFields()) {
                auth.createUserWithEmailAndPassword((mail)!!, (psw)!!)
                    .addOnCompleteListener(this, OnCompleteListener<AuthResult?> { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("SIGNIN", "createUserWithEmail:success")
                            ROOT_UID = auth.uid.toString()
                            uploadImage(ROOT_UID)
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.d("ERROR", "createUserWithEmail:failure", task.exception)
                        }
                    })
            } else {
                Toast.makeText(applicationContext, error_msg, Toast.LENGTH_LONG).show()
            }
        }
        findViewById<View>(R.id.plus).setOnClickListener { p: View? -> editPhoto() }
        findViewById<View>(R.id.img_profile).setOnClickListener { e: View? -> editPhoto() }
    }

    private fun uploadImage(UID: String) {
        if (currentPhotoPath != null) {
            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Uploading...")
            progressDialog.show()
            val ref = storageReference!!.child("images/").child("riders/").child(UID)
            val file: File? = null
            val imageUri = Uri.fromFile(File(currentPhotoPath))
            ref.putFile(imageUri).continueWithTask<Uri> { task ->
                if (!task.isSuccessful) {
                    throw task.exception!!
                }
                ref.downloadUrl
            }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downUri = task.result
                        Log.d("URL", "onComplete: Url: $downUri")
                        val new_user: MutableMap<String, Any> = HashMap()
                        new_user["rider_info"] = User(
                            "gallottino",
                            name!!,
                            surname!!,
                            mail!!,
                            phone!!, "",
                            downUri.toString()
                        )
                        new_user["available"] = true
                        val myRef = database!!.getReference(RIDERS_PATH + "/" + UID)
                        myRef.updateChildren(new_user)
                        setResult(1)
                        finish()
                    }
                }
        } else {
            val new_user: MutableMap<String, Any> = HashMap()
            new_user["rider_info"] = User(
                "gallottino", name!!, surname!!, mail!!, phone!!, "", ""
            )
            new_user["available"] = true
            val myRef = database!!.getReference(RIDERS_PATH + "/" + UID)
            myRef.updateChildren(new_user)
            setResult(1)
            finish()
        }
    }

    private fun editPhoto() {
        val alertDialog = AlertDialog.Builder(this@SignUp, R.style.AlertDialogStyle).create()
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
        alertDialog.setView(view)
        alertDialog.setButton(
            AlertDialog.BUTTON_NEUTRAL,
            "Camera"
        ) { dialog: DialogInterface, which: Int ->
            cameraIntent()
            dialog_open = false
            dialog.dismiss()
        }
        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE,
            "Gallery"
        ) { dialog: DialogInterface, which: Int ->
            galleryIntent()
            dialog_open = false
            dialog.dismiss()
        }
        alertDialog.show()
    }

    private fun cameraIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                // Error occurred while creating the File
                Log.d("FILE: ", "error creating file")
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(
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
            val photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            startActivityForResult(photoPickerIntent, 1)
        }
    }

    private fun checkFields(): Boolean {
        name = (findViewById<View>(R.id.name) as EditText).text.toString()
        surname = (findViewById<View>(R.id.surname) as EditText).text.toString()
        mail = (findViewById<View>(R.id.mail) as EditText).text.toString()
        phone = (findViewById<View>(R.id.phone2) as EditText).text.toString()
        psw = (findViewById<View>(R.id.psw) as EditText).text.toString()
        psw_confirm = (findViewById<View>(R.id.psw_confirm) as EditText).text.toString()
        if (name!!.trim { it <= ' ' }.length == 0) {
            error_msg = "Insert name"
            return false
        }
        if (surname!!.trim { it <= ' ' }.length == 0) {
            error_msg = "Insert address"
            return false
        }
        if (mail!!.trim { it <= ' ' }.length == 0 || !Patterns.EMAIL_ADDRESS.matcher(mail)
                .matches()
        ) {
            error_msg = "Insert e-mail"
            return false
        }
        if (phone!!.trim { it <= ' ' }.length == 0) {
            error_msg = "Insert phone number"
            return false
        }
        if (psw!!.compareTo(psw_confirm!!) != 0) {
            error_msg = "Passwords don't match"
            return false
        }
        return true
    }

    @Throws(IOException::class)
    private fun setPhoto(photoPath: String) {
        val imgFile = File(photoPath)
        var myBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
        myBitmap = adjustPhoto(myBitmap, photoPath)
        (findViewById<View>(R.id.img_profile) as ImageView).setImageBitmap(myBitmap)
    }

    @Throws(IOException::class)
    private fun adjustPhoto(bitmap: Bitmap?, photoPath: String?): Bitmap? {
        val ei = ExifInterface(photoPath!!)
        val orientation = ei.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
        var rotatedBitmap: Bitmap? = null
        rotatedBitmap = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(
                bitmap,
                90f
            )

            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(
                bitmap,
                180f
            )

            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(
                bitmap,
                270f
            )

            ExifInterface.ORIENTATION_NORMAL -> bitmap
            else -> bitmap
        }
        return rotatedBitmap
    }

    @Throws(IOException::class)
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
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(
                selectedImage!!,
                filePathColumn, null, null, null
            )
            cursor!!.moveToFirst()
            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
            val picturePath = cursor.getString(columnIndex)
            cursor.close()

            //Log.d("Photo path: ", picturePath);
            currentPhotoPath = picturePath
        }
        if ((requestCode == 1 || requestCode == 2) && resultCode == RESULT_OK) {
            val imgFile = File(currentPhotoPath)
            var myBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
            try {
                myBitmap = adjustPhoto(myBitmap, currentPhotoPath)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            (findViewById<View>(R.id.img_profile) as ImageView).setImageBitmap(myBitmap)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        first_check = getSharedPreferences(CheckPREF, 0)
        val editor = first_check.edit()
        editor.putBoolean("firsRun", true)
        editor.apply()
        finish()
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putString(
            Name,
            (findViewById<View>(R.id.name) as EditText).text.toString()
        )
        savedInstanceState.putString(
            Address,
            (findViewById<View>(R.id.surname) as EditText).text.toString()
        )
        savedInstanceState.putString(
            Email,
            (findViewById<View>(R.id.mail) as EditText).text.toString()
        )
        savedInstanceState.putString(
            Phone,
            (findViewById<View>(R.id.phone2) as EditText).text.toString()
        )
        savedInstanceState.putString(Photo, currentPhotoPath)
        savedInstanceState.putBoolean(DialogOpen, dialog_open)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        (findViewById<View>(R.id.name) as EditText).setText(savedInstanceState.getString(Name))
        (findViewById<View>(R.id.surname) as EditText).setText(savedInstanceState.getString(Address))
        (findViewById<View>(R.id.mail) as EditText).setText(savedInstanceState.getString(Email))
        (findViewById<View>(R.id.phone2) as EditText).setText(savedInstanceState.getString(Phone))
        currentPhotoPath = savedInstanceState.getString(Photo)
        if (currentPhotoPath != null) {
            try {
                setPhoto(currentPhotoPath!!)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if (savedInstanceState.getBoolean(DialogOpen)) editPhoto()
    }

    companion object {
        private const val MyPREF = "User_Data"
        private const val CheckPREF = "First Run"
        private const val Name = "keyName"
        private const val Address = "keyAddress"
        private const val Description = "keyDescription"
        private const val Email = "keyEmail"
        private const val Phone = "keyPhone"
        private const val Photo = "keyPhoto"
        private const val FirstRun = "keyRun"
        private const val DialogOpen = "keyDialog"
        private const val PERMISSION_GALLERY_REQUEST = 1
        private fun rotateImage(source: Bitmap?, angle: Float): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(angle)
            return Bitmap.createBitmap(
                source!!, 0, 0, source.width, source.height,
                matrix, true
            )
        }
    }
}