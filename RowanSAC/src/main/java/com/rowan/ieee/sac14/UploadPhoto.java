package com.rowan.ieee.sac14;

        import java.io.DataOutputStream;
        import java.io.File;
        import java.io.FileInputStream;
        import java.net.HttpURLConnection;
        import java.net.MalformedURLException;
        import java.net.URL;

        import android.annotation.SuppressLint;
        import android.app.Activity;
        import android.app.ProgressDialog;
        import android.content.Intent;
        import android.graphics.Bitmap;
        import android.media.ExifInterface;
        import android.net.Uri;
        import android.os.Bundle;
        import android.text.Editable;
        import android.text.TextWatcher;
        import android.util.Log;
        import android.view.KeyEvent;
        import android.view.MotionEvent;
        import android.view.View;
        import android.view.View.OnClickListener;
        import android.widget.Button;
        import android.widget.EditText;
        import android.widget.ImageButton;
        import android.widget.ImageView;
        import android.widget.TextView;
        import android.widget.Toast;

public class UploadPhoto extends Activity {

    TextView messageText;
    EditText caption;
    int serverResponseCode = 0;
    ProgressDialog dialog = null;

    String upLoadServerUri = null;

    /**********  File Path *************/
    String uploadFilePath;
    String uploadFileName;
    String captiontext = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_photo);

        messageText  = (TextView)findViewById(R.id.messageText);

        messageText.setText("Uploading file path :- '/mnt/sdcard/"+uploadFileName+"'");

        /************* Php script path ****************/
        upLoadServerUri = "http://www.rowan.edu/clubs/ieee/sac/app/appImageUpload.php";

        //Cheers("Hello");
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if(intent != null) {
            //Bundle extras = intent.getExtras();
            uploadFilePath = intent.getStringExtra("path");
            uploadFileName = intent.getStringExtra("name");
            uploadFileName = uploadFileName.split("/")[uploadFileName.split("/").length-1];

           // Cheers("Assigning name/path "+uploadFilePath);
            //messageText.setText("Chosen\n"+uploadFilePath+" "+uploadFileName);
            messageText.setText("Add a Caption");
            ImageView img = (ImageView) findViewById(R.id.upload_photo_preview);
            try {
                img.setImageURI(Uri.parse(uploadFilePath));
            } catch(Exception e) {
                Cheers(e.getMessage());
            }
        }
        final ImageView preview = (ImageView) findViewById(R.id.upload_photo_preview);
        //If we click on the preview, we want it to open up the image picker again
            preview.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent main = new Intent(getApplicationContext(), MainActivity.class);
                    main.putExtra("reupload", "100");
                    startActivity(main);
                }
            });
            preview.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction() == MotionEvent.ACTION_DOWN)
                        preview.setBackgroundColor(getResources().getColor(R.color.gray));
                    else if(event.getAction() == MotionEvent.ACTION_UP)
                        preview.setBackgroundColor(getResources().getColor(R.color.white));
                    return false;
                }
            });

        final ImageButton uploadButton = (ImageButton) findViewById(R.id.upload_photo_button);

        uploadButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

               // Cheers("Now we upload");
               // Prevent future posts
                uploadButton.setEnabled(false);
                uploadButton.setVisibility(View.INVISIBLE);

                try {
                        try {
                            dialog = ProgressDialog.show(UploadPhoto.this, "", "Uploading file...", true);
                            caption = (EditText) findViewById(R.id.upload_photo_caption);
                            if(captiontext.length() > 200)
                                captiontext = caption.getText().toString().substring(0,200);
                            else
                                captiontext = caption.getText().toString();

                            ExifInterface exif = new ExifInterface(uploadFilePath);
                            exif.setAttribute(ExifInterface.TAG_MODEL, captiontext);
                            exif.saveAttributes();

                            new Thread(new Runnable() {
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            messageText.setText("Uploading started.....");
                                        }
                                    });
                                    //captiontext.substring(0,200)
                                    uploadFile(uploadFilePath, uploadFileName, captiontext);

                                }
                            }).start();

                        } catch(Exception e) {
                            dialog = ProgressDialog.show(UploadPhoto.this, "", "Uploading file...", true);
                            new Thread(new Runnable() {
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            messageText.setText("Uploading started.....");
                                        }
                                    });
                                    //captiontext.substring(0,200)
                                    uploadFile(uploadFilePath, uploadFileName, " ");

                                }
                            }).start();
                            Cheers(e.getMessage());
                        }
                        //c.Cheers("Upload Photo: "+uploadFilePath+" "+uploadFileName);

                } catch(Exception e) {
                    Cheers(e.getMessage());
                }
            }
        });

        final EditText caption = (EditText) findViewById(R.id.upload_photo_caption);
        caption.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @SuppressLint("ResourceAsColor")
            @Override
            public void afterTextChanged(Editable s) {
                captiontext = caption.getText().toString();
                TextView limit = (TextView) findViewById(R.id.upload_photo_limit);
                //Integer ch = (200-s.length());
                int ch = 200-s.length();
                limit.setText(ch+"");
                //Cheers(String.valueOf(limit.getCurrentTextColor()));
                //Cheers(String.valueOf(ch > 20));
                if(ch < 0) {
                    limit.setTextColor(getResources().getColor(R.color.red));
                } else {
                    limit.setTextColor(getResources().getColor(R.color.rowanbrowndark));
                }
            }
        });

    }
    private void Cheers(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
    public int uploadFile(String sourceFileUri, String sourceFileName, String caption) {


        String fileName = sourceFileUri;

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(sourceFileUri);

        if (!sourceFile.isFile()) {

            dialog.dismiss();

            Log.e("uploadFile", "Source File not exist :"
                    +uploadFilePath + "" + uploadFileName);

            runOnUiThread(new Runnable() {
                public void run() {
                    messageText.setText("Source File not exist :"
                            +uploadFilePath + "" + uploadFileName);
                }
            });

            return 0;

        }
        else
        {
            try {

                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(upLoadServerUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", fileName);
                conn.setRequestProperty("caption", caption);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name='uploaded_file';filename='"
                        + fileName + "'" + lineEnd);

                        dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                final String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);

                if(serverResponseCode == 200){

                    runOnUiThread(new Runnable() {
                        public void run() {

                            /*String msg = "File Upload Completed.\n Your photo will be approved and be public shortly.\nView the photo at: "+
                                    "http://xsorcreations.com/sac/imageuploads/"+uploadFileName;*/
                            String msg = "Res 200: "+serverResponseMessage;

                            messageText.setText(msg);
                            Toast.makeText(UploadPhoto.this, "File Upload Complete.",
                                    Toast.LENGTH_SHORT).show();
                            onBackPressed();
                        }
                    });
                } else {
                    messageText.setText("Got response "+serverResponseCode);
                }

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {

                dialog.dismiss();
                ex.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        messageText.setText("MalformedURLException Exception : check script url.");
                        Toast.makeText(UploadPhoto.this, "MalformedURLException",
                                Toast.LENGTH_SHORT).show();
                    }
                });

                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {

                dialog.dismiss();
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        messageText.setText("Got Exception : see logcat ");
                        Toast.makeText(UploadPhoto.this, "Got Exception : see logcat ",
                                Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("Upload file to server Exception", "Exception : "
                        + e.getMessage(), e);
            }
            dialog.dismiss();
            return serverResponseCode;

        } // End else block
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //    finish();

    }
}