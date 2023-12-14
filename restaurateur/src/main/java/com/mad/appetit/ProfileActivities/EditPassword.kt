package com.mad.appetit.ProfileActivitiesimport

import android.app.ProgressDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.mad.appetit.R


class EditPassword : AppCompatActivity() {
    private var oldPsw: String? = null
    private var newPsw: String? = null
    private var confirmPsw: String? = null
    private var errMsg = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_password)
        val progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Updating password...")
        findViewById<View>(R.id.text_psw_alert).visibility = View.INVISIBLE
        findViewById<View>(R.id.error_psw).visibility = View.INVISIBLE
        findViewById<View>(R.id.button).setOnClickListener { e: View? ->
            if (checkFields()) {
                findViewById<View>(R.id.text_psw_alert).visibility = View.INVISIBLE
                findViewById<View>(R.id.error_psw).visibility = View.INVISIBLE
                progressDialog.show()
                val user = FirebaseAuth.getInstance().currentUser
                val email = user!!.email
                val credential = EmailAuthProvider.getCredential(email!!, oldPsw!!)
                user.reauthenticate(credential).addOnCompleteListener { task: Task<Void?> ->
                    if (task.isSuccessful) {
                        user.updatePassword(newPsw!!).addOnCompleteListener { task1: Task<Void?> ->
                            if (task1.isSuccessful) {
                                progressDialog.hide()
                                Toast.makeText(
                                    this@EditPassword,
                                    "Password successfully modified",
                                    Toast.LENGTH_LONG
                                ).show()
                                finish()
                            } else {
                                progressDialog.hide()
                                Toast.makeText(
                                    this@EditPassword,
                                    "Something went wrong. Please try again later",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } else {
                        progressDialog.hide()
                        Toast.makeText(
                            this@EditPassword,
                            "Authentication Failed. Wrong password.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else Toast.makeText(this@EditPassword, errMsg, Toast.LENGTH_LONG).show()
        }
    }

    private fun checkFields(): Boolean {
        oldPsw = (findViewById<View>(R.id.old_psw) as EditText).text.toString()
        newPsw = (findViewById<View>(R.id.new_password) as EditText).text.toString()
        confirmPsw = (findViewById<View>(R.id.confirm_new_password) as EditText).text.toString()
        if (oldPsw!!.trim { it <= ' ' }.length == 0) {
            errMsg = "Please insert the old password"
            return false
        }
        if (newPsw!!.trim { it <= ' ' }.length < 6) {
            errMsg = "Password should be at least 6 characters"
            return false
        }
        if (newPsw!!.trim { it <= ' ' }.length != confirmPsw!!.trim { it <= ' ' }.length) {
            errMsg = "Passwords must be equal"
            findViewById<View>(R.id.text_psw_alert).visibility = View.VISIBLE
            findViewById<View>(R.id.error_psw).visibility = View.VISIBLE
            return false
        }
        return true
    }
}