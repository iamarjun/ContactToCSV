package com.alwaysbaked.contacttocsv

import android.Manifest
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.alwaysbaked.contacttocsv.util.IterableCursor
import com.alwaysbaked.contacttocsv.util.Zipper
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.opencsv.CSVWriter
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.io.FileWriter
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private var contactList: MutableList<Contact>? = null
    private var disposable: CompositeDisposable? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate: started.")

        disposable = CompositeDisposable()
        contactList = ArrayList()

        export()

    }

    override fun onDestroy() {
        super.onDestroy()
        disposable!!.clear()
    }

    fun readContacts() {
        Log.d(TAG, "readContacts: reading the phone contacts.")

        val cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC")

        Observable.fromIterable(IterableCursor(cursor))
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<Cursor> {
                    override fun onSubscribe(d: Disposable) {
                        Log.d(TAG, "onSubscribe: disposing the subscription")
                        disposable!!.add(d)
                    }

                    override fun onNext(cursor: Cursor) {
                        Log.d(TAG, "accept: Writing Contacts details to a list.")

                        val contact = Contact()
                        contact.name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                        contact.number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        contactList!!.add(contact)

                    }

                    override fun onError(e: Throwable) {
                        Log.d(TAG, "onError: " + e.message)

                    }

                    override fun onComplete() {
                        Log.d(TAG, "onComplete: Contacts List size: " + contactList!!.size)

                        val data = arrayOfNulls<String>(contactList!!.size)

                        for ((i, contact) in contactList!!.withIndex()) {
                            data[i] = contact.toString()
                        }

                        for (mData in data)
                            Log.d(TAG, "onComplete: Info: $mData")

                        toCSV(data)


                    }
                })
    }

    private fun toCSV(strings: Array<String?>) {
        Log.d(TAG, "toCSV: converting to csv")

        try {
            val writer = CSVWriter(FileWriter(OUTPUT))
            writer.writeNext(strings)

            zipCSV()

        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun zipCSV() {
        Log.d(TAG, "zipCSV: zipping the csv file.")

        val zipper = Zipper()
        val path = arrayOfNulls<String>(1)
        path[0] = OUTPUT
        zipper.zip(path, "$ROOT_DIR/MyContacts.zip")

        Snackbar.make(root_layout, "MyContacts.zip Created in Root Directory", Snackbar.LENGTH_LONG)
                .show()

    }

    private fun export() {
        btn_export.setOnClickListener {
            Log.d(TAG, "onClick: checking for permission")

            // checking for permission
            requestMultiplePermissions()
        }
    }

    private fun requestMultiplePermissions() {
        Log.d(TAG, "requestMultiplePermissions: Requesting...")
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        if (report.areAllPermissionsGranted())
                            readContacts()
                        else {
                            Snackbar.make(root_layout, "All Permissions Needed.", Snackbar.LENGTH_LONG)
                                    .setAction("Settings") { openSettings() }
                                    .show()

                        }

                    }

                    override fun onPermissionRationaleShouldBeShown(permissions: List<PermissionRequest>,
                                                                    token: PermissionToken) {

                    }
                })
                .withErrorListener { error -> Log.e(TAG, "onError: " + error.toString()) }
                .check()
    }

    // navigating user to settings
    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivityForResult(intent, 101)
    }

    companion object {
        private const val TAG = "MainActivity"
        private val ROOT_DIR = Environment.getExternalStorageDirectory().path
        private val OUTPUT = "$ROOT_DIR/Download/contacts.csv"
    }

}
