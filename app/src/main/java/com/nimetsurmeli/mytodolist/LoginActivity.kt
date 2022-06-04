package com.nimetsurmeli.mytodolist

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_login.*
import java.util.concurrent.TimeUnit


class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        RxTextView.afterTextChangeEvents(editText_email)
            .skipInitialValue()
            .map {
                wrapper_email.error = null
                it.view().text.toString()
            }
            .debounce(1, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread())
            .compose(verifyEmailPattern)
            .compose(verifyEmailPattern)
            .compose(retryWhenError {
                wrapper_email.error = it.message
            })
            .subscribe()

        RxTextView.afterTextChangeEvents(editText_password)
            .skipInitialValue()
            .map {
                wrapper_password.error = null
                it.view().text.toString()
            }
            .debounce(1, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread())
            .compose(verifyPasswordLength)
            .compose(retryWhenError {
                wrapper_password.error = it.message
            })
            .subscribe()

        val loginBtn = findViewById<Button>(R.id.button_login)

        //loginBtn.isEnabled = !((editText_password.text.toString().trim() == "")||(editText_email.text.toString().trim() == ""))

        loginBtn.isEnabled = false
        val editTexts = listOf(editText_email, editText_password)
        for (editText in editTexts) {
            editText.addTextChangedListener(object : TextWatcher {
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    var et1 = editText_email.text.toString().trim()
                    var et2 = editText_password.text.toString().trim()

                    loginBtn.isEnabled = et1.isNotEmpty()
                            && et2.isNotEmpty()
                }

                override fun beforeTextChanged(
                    s: CharSequence, start: Int, count: Int, after: Int) {
                }

                override fun afterTextChanged(
                    s: Editable) {
                }
            })
        }

        loginBtn.setOnClickListener {
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)
        }

    }


    private val verifyPasswordLength = ObservableTransformer<String, String> { observable ->
        observable.flatMap {
            Observable.just(it).map {
                it.trim() }
                .filter { it.length > 8 }
                .singleOrError()
                .onErrorResumeNext {
                    if (it is NoSuchElementException) {
                        Single.error(Exception("Password must be at least 8 characters"))
                    } else {
                        Single.error(it)
                    }
                }
                .toObservable()
        }
    }

    private val verifyEmailPattern = ObservableTransformer<String, String> { observable ->
        observable.flatMap {
            Observable.just(it).map { it.trim() }
                .filter {
                    Patterns.EMAIL_ADDRESS.matcher(it).matches()
                }
                .singleOrError()
                .onErrorResumeNext {
                    if (it is NoSuchElementException) {
                        Single.error(Exception("Invalid email format"))
                    } else {
                        Single.error(it)
                    }

                }.toObservable()
        }
    }

    private inline fun retryWhenError(crossinline onError: (ex: Throwable) -> Unit): ObservableTransformer<String, String> = ObservableTransformer { observable ->
        observable.retryWhen { errors ->
            errors.flatMap {
                onError(it)
                Observable.just("")
            }
        }

    }






}
