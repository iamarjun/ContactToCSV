package com.alwaysbaked.contacttocsv;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.alwaysbaked.contacttocsv.util.IterableCursor;
import com.alwaysbaked.contacttocsv.util.Zipper;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String ROOT_DIR = Environment.getExternalStorageDirectory().getPath();
    private static final String OUTPUT = ROOT_DIR + "/Download/contacts.csv";

    private List<Contact> contactList;
    private CompositeDisposable disposable;
    private Snackbar snackbar;

    @BindView(R.id.btn_export)
    Button mExport;
    @BindView(android.R.id.content)
    View contentView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        Log.d(TAG, "onCreate: started.");

        disposable = new CompositeDisposable();
        contactList = new ArrayList<>();

        export();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.clear();
    }

    public void readContacts() {
        Log.d(TAG, "readContacts: reading the phone contacts.");

        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC");

        Observable.fromIterable(new IterableCursor(cursor))
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Cursor>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "onSubscribe: disposing the subscription");
                        disposable.add(d);
                    }

                    @Override
                    public void onNext(Cursor cursor) {
                        Log.d(TAG, "accept: Writing Contacts details to a list.");

                        Contact contact = new Contact();
                        contact.setName(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)));
                        contact.setNumber(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                        contactList.add(contact);

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError: " + e.getMessage());

                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: Contacts List size: " + contactList.size());

                        String[] data = new String[contactList.size()];

                        int i = 0;
                        for (Contact contact : contactList) {
                            data[i] = contact.toString();
                            i++;
                        }

                        for (String mData : data)
                            Log.d(TAG, "onComplete: Info: " + mData);

                        toCSV(data);


                    }
                });
    }

    private void toCSV(String[] strings) {
        Log.d(TAG, "toCSV: converting to csv");

        try {
            CSVWriter writer = new CSVWriter(new FileWriter(OUTPUT));
            writer.writeNext(strings);

            zipCSV();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void zipCSV() {
        Log.d(TAG, "zipCSV: zipping the csv file.");

        Zipper zipper = new Zipper();
        String[] path = new String[1];
        path[0] = OUTPUT;
        zipper.zip(path, ROOT_DIR + "/" + "MyContacts.zip");

        snackbar = Snackbar
                .make(contentView, "MyContacts.zip Created in Root Directory", Snackbar.LENGTH_LONG);

        snackbar.show();
    }

    private void export() {
        mExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: checking for permission");

                // checking for permission
                requestMultiplePermissions();

            }
        });
    }

    private void requestMultiplePermissions() {
        Log.d(TAG, "requestMultiplePermissions: Requesting...");
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted())
                            readContacts();

                        else {
                            snackbar = Snackbar
                                    .make(contentView, "All Permissions Needed.", Snackbar.LENGTH_LONG)
                                    .setAction("Settings", new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            openSettings();
                                        }
                                    });
                            snackbar.show();
                        }

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions,
                                                                   PermissionToken token) {

                    }
                })
                .withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        Log.e(TAG, "onError: " + error.toString());
                    }
                })
                .check();
    }

    // navigating user to settings
    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }

}
