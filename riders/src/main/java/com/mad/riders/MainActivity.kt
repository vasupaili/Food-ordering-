package com.mad.riders

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.CallbackManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.mad.mylibrary.SharedClass.ROOT_UID
import com.mad.riders.ProfileManagment.SignUp

class MainActivity constructor() : AppCompatActivity() {
    private var email: String? = null
    private var password: String? = null
    private var errMsg: String? = null
    private val callbackManager: CallbackManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val auth: FirebaseAuth = FirebaseAuth.getInstance()
        if (auth.getCurrentUser() == null) {
            val progressDialog: ProgressDialog = ProgressDialog(this)
            progressDialog.setTitle("Authenticating...")
            findViewById<View>(R.id.sign_up).setOnClickListener(View.OnClickListener({ e: View? ->
                val login: Intent = Intent(this, SignUp::class.java)
                startActivityForResult(login, 1)
            }))
            findViewById<View>(R.id.login).setOnClickListener(View.OnClickListener { h: View? ->
                if (checkFields()) {
                    progressDialog.show()
                    auth.signInWithEmailAndPassword((email)!!, (password)!!)
                        .addOnCompleteListener(
                            this,
                            OnCompleteListener<AuthResult?> { task: Task<AuthResult?> ->
                                if (task.isSuccessful()) {
                                    ROOT_UID = auth.getUid().toString()
                                    val fragment: Intent = Intent(this, FragmentManager::class.java)
                                    startActivity(fragment)
                                    progressDialog.dismiss()
                                    finish()
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Wrong Username or Password",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        )
                } else {
                    Toast.makeText(this@MainActivity, errMsg, Toast.LENGTH_LONG).show()
                    progressDialog.dismiss()
                }
            })
        } else {
            ROOT_UID = auth.getCurrentUser()!!.getUid()
            val fragment: Intent = Intent(this, FragmentManager::class.java)
            startActivity(fragment)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == 1) {
            val fragment: Intent = Intent(this, FragmentManager::class.java)
            startActivity(fragment)
            finish()
        }
    }

    fun checkFields(): Boolean {
        email = (findViewById<View>(R.id.email) as EditText).getText().toString()
        password = (findViewById<View>(R.id.password) as EditText).getText().toString()
        if (email!!.trim({ it <= ' ' }).length == 0 || !Patterns.EMAIL_ADDRESS.matcher(email)
                .matches()
        ) {
            errMsg = "Invalid Mail"
            return false
        }
        if (password!!.trim({ it <= ' ' }).length == 0) {
            errMsg = "Fill password"
            return false
        }
        return true
    }
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}