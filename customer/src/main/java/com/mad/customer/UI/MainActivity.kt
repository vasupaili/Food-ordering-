package com.mad.customer.UIimport


import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns

import android.view.View
import android.widget.EditText
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.mad.customer.UI.NavApp
import com.mad.customer.UI.SignUp

import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity

import com.mad.customer.R
import com.mad.mylibrary.SharedClass




class MainActivity constructor() : AppCompatActivity() {
    private var email: String? = null
    private var password: String? = null
    private var errMsg: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val auth: FirebaseAuth = FirebaseAuth.getInstance()
        if (auth.getCurrentUser() != null) {
            SharedClass.ROOT_UID = auth.getUid().toString()
            val i: Intent = Intent(this@MainActivity, NavApp::class.java)
            startActivity(i)
            finish()
        }
        findViewById<View>(R.id.sign_up).setOnClickListener(View.OnClickListener({ e: View? ->
            val i: Intent = Intent(this, SignUp::class.java)
            startActivityForResult(i, 1)
        }))
        findViewById<View>(R.id.sign_in).setOnClickListener(View.OnClickListener { e: View? ->
            val progressDialog: ProgressDialog = ProgressDialog(this)
            progressDialog.setTitle("Authenticating...")
            if (checkFields()) {
                progressDialog.show()
                auth.signInWithEmailAndPassword((email)!!, (password)!!)
                    .addOnCompleteListener(this, OnCompleteListener { task: Task<AuthResult?> ->
                        if (task.isSuccessful()) {
                            SharedClass.ROOT_UID = auth.getUid().toString()
                            val fragment: Intent = Intent(this, NavApp::class.java)
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
                    })
            } else {
                Toast.makeText(this@MainActivity, errMsg, Toast.LENGTH_LONG).show()
                progressDialog.dismiss()
            }
        })
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == 1) {
            val fragment: Intent = Intent(this, NavApp::class.java)
            startActivity(fragment)
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}