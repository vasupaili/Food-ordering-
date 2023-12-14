package com.mad.appetit.Startup

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.mad.appetit.R
import com.mad.appetitimport.FragmentManagers
import com.mad.mylibrary.SharedClass
import com.mad.mylibrary.SharedClass.ROOT_UID
import com.mad.mylibrary.SharedClass.SIGNUP


class MainActivity : AppCompatActivity() {
    private var email: String? = null
    private var password: String? = null
    private var errMsg = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            val progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Authenticating...")
            findViewById<View>(R.id.sign_up).setOnClickListener { e: View? ->
                val login = Intent(this, SignUp::class.java)
                startActivityForResult(login, SIGNUP)
            }
            findViewById<View>(R.id.login).setOnClickListener { h: View? ->
                if (checkFields()) {
                    progressDialog.setCancelable(false)
                    progressDialog.show()
                    auth.signInWithEmailAndPassword(email!!, password!!)
                        .addOnCompleteListener(this) { task: Task<AuthResult?> ->
                            if (task.isSuccessful) {
                                ROOT_UID = auth.uid.toString()
                                progressDialog.dismiss()
                                val fragment = Intent(this, FragmentManagers::class.java)
                                startActivity(fragment)
                                finish()
                            } else {
                                //Log.w("LOGIN", "signInWithCredential:failure", task.getException());
                                progressDialog.dismiss()
                                Snackbar.make(
                                    findViewById(R.id.email),
                                    "Authentication Failed. Try again.",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .addOnFailureListener { e: Exception? ->
                            progressDialog.dismiss()
                            Snackbar.make(
                                findViewById(R.id.email),
                                "Authentication Failed. Try again.",
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    Snackbar.make(findViewById(R.id.email), errMsg, Snackbar.LENGTH_SHORT).show()
                }
            }
        } else {
            ROOT_UID = auth.uid.toString()

            val fragment = Intent(this, FragmentManagers::class.java)
            startActivity(fragment)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null && resultCode == SIGNUP) {
            val fragment = Intent(this, FragmentManagers::class.java)
            startActivity(fragment)
            finish()
        }
    }

    fun checkFields(): Boolean {
        email = (findViewById<View>(R.id.email) as EditText).text.toString()
        password = (findViewById<View>(R.id.password) as EditText).text.toString()
        if (email!!.trim { it <= ' ' }.length == 0 || !Patterns.EMAIL_ADDRESS.matcher(email)
                .matches()
        ) {
            errMsg = "Invalid Mail"
            return false
        }
        if (password!!.trim { it <= ' ' }.length == 0) {
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