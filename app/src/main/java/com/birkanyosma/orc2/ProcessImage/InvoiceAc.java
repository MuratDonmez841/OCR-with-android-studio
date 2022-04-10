package com.birkanyosma.orc2.ProcessImage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.birkanyosma.orc2.AndroidCamera;
import com.birkanyosma.orc2.CommonUtils;
import com.birkanyosma.orc2.R;
import com.birkanyosma.orc2.User.UserOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvoiceAc extends AppCompatActivity {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    Button btnCamera,btnOcr,btnSave,btnExit;
    TextView txtResult;
    ImageView imgCapture;
    Bitmap finalImageBitmap;
    String lastFileName="";
    FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseFirestore frStore;
    ProgressDialog progressDialog;
    String userID;

    Button btnStartCamera;
    Button btnOldInvoiceSave;
    ProgressDialog progressBar;
    TextView editTextCompare;
    TextView editTextOldInvoice;
    Button btnCompare;
    private String language;
    Bundle b;
    Intent i;
    private int sourceW = 0;
    private int sourceH = 0;
    private boolean isRecognized = false;
    String serialNumber;
    boolean cameraCheck = true;
    Button btnSerialNumberSave;
    TextView editTextSerialNumber;
    String page;
    double oldInvoiceInt;
    double newInvoiceInt;
    double resultInvoice;
    Button newInvoiceCam;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_invoice);
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        frStore = FirebaseFirestore.getInstance();
        btnOcr=findViewById(R.id.btn_ocr_invoice);
        btnOldInvoiceSave = findViewById(R.id.btn_save_old_invoice);
        progressDialog = new ProgressDialog(InvoiceAc.this);
        btnStartCamera = findViewById(R.id.btn_start_camera_old_invoice);
        btnExit = findViewById(R.id.btn_exit_old_invoice);
        txtResult = findViewById(R.id.text_result_old_invoice);
        newInvoiceCam=findViewById(R.id.btn_new_invoice_cam);
        imgCapture = findViewById(R.id.grid_img);
        editTextCompare = findViewById(R.id.text_old_invoice_compare_result);
        editTextOldInvoice = findViewById(R.id.text_old_invoice);
        btnCompare = findViewById(R.id.btn_compare_old_invoice);
        editTextSerialNumber = findViewById(R.id.text_result_serial_number);

        String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET};//bağlantı izinlerini istiyoruz

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, 1);//izinleri gösteriyoruz
        }

        userID = user.getUid();
        newInvoiceCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraCheck=true;
                dispatchTakePictureIntent();

            }
        });
        btnStartCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraCheck=false;
                dispatchTakePictureIntent();
            }
        });
        btnOcr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textOcrFromImg();
            }
        });
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), UserOptions.class);
                startActivity(i);
                finish();
            }
        });
        //bu işlemde firebaseden eski sayaç bilgisini çekiyoruz
        frStore.collection("Users").document(userID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                editTextOldInvoice.setText(task.getResult().getString("oldInvoice"));
                serialNumber=task.getResult().getString("serialNumber");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "İnternet bağlantınızı kontrol ediniz!", Toast.LENGTH_SHORT).show();
            }
        });
        //eski sayaç bilgisini yeni sayaç bilgisi ile deiştiriyoruz
        btnOldInvoiceSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    progressDialog.setMessage("İşlemi yapılıyor..");
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.setTitle("Güncelleme");
                    progressDialog.show();
                    String oldInvoice = txtResult.getText().toString();
                    if (oldInvoice.length() == 5) {
                        Map updateMap = new HashMap();
                        updateMap.put("date", Timestamp.now());//tarih
                        updateMap.put("oldInvoice", oldInvoice);//mapi doldurduk
                        //bu fonksiyon yeni sayaç bilgimizi update ediyor
                        frStore.collection("Users").document(userID).update(updateMap).addOnCompleteListener(new OnCompleteListener() {
                            @Override
                            public void onComplete(@NonNull Task task) {
                                if (task.isSuccessful()) {
                                    progressDialog.dismiss();
                                    frStore.collection("Users").document(userID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            editTextOldInvoice.setText(task.getResult().getString("oldInvoice"));

                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(getApplicationContext(), "İnternet bağlantınızı kontrol ediniz!", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    Toast.makeText(getApplicationContext(), "Güncelleme tamamlandı.", Toast.LENGTH_SHORT).show();
                                } else {
                                    progressDialog.dismiss();
                                    Toast.makeText(getApplicationContext(), "Güncelleme tamamlanamadı.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.dismiss();
                                Toast.makeText(getApplicationContext(), "İnternet bağlantınızı kontrol ediniz!", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
            }
        });

        btnCompare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (serialNumber.equals(editTextSerialNumber.getText().toString())){
                    //burada tl cinsinden fiyatını hesaplıyoruz birim fiyatı 2 olarak aldım çarpanı değiştirebilirsiniz.
                    String oldInvoice = editTextOldInvoice.getText().toString();
                    String newInvoice = txtResult.getText().toString();
                    if (!oldInvoice.equals("-") && !newInvoice.equals("")) {
                        if (newInvoice.length() == 5) {
                            progressDialog.setMessage("İşlemi yapılıyor..");
                            progressDialog.setCanceledOnTouchOutside(false);
                            progressDialog.setTitle("Güncelleme");
                            progressDialog.show();
                            oldInvoiceInt = Double.parseDouble(oldInvoice);
                            newInvoiceInt = Double.parseDouble(newInvoice);
                            resultInvoice = newInvoiceInt - oldInvoiceInt;
                            resultInvoice = (resultInvoice * 2) - (resultInvoice * 10 / 100);//birim fiyatı 2
                            Map updateMap = new HashMap();
                            updateMap.put("invoice", resultInvoice);
                            frStore.collection("Users").document(userID).update(updateMap).addOnCompleteListener(new OnCompleteListener() {
                                @Override
                                public void onComplete(@NonNull Task task) {
                                    if (task.isSuccessful()){
                                        Toast.makeText(getApplicationContext(),"Fatura kaydedildi",Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            editTextCompare.setText(resultInvoice + " ₺");
                        } else {
                            Toast.makeText(getApplicationContext(), "Sayaç değeri 5 haneli olmalı.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Sayaç değerleri boş olamaz.", Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(),"Seri numaraları uyuşmuyor",Toast.LENGTH_SHORT).show();
                }

                String oldInvoice = txtResult.getText().toString();

                if (oldInvoice.length() == 5) {
                    Map updateMap = new HashMap();
                    updateMap.put("date", Timestamp.now());//tarih
                    updateMap.put("oldInvoice", oldInvoice);//mapi doldurduk
                    //bu fonksiyon yeni sayaç bilgimizi update ediyor
                    frStore.collection("Users").document(userID).update(updateMap).addOnCompleteListener(new OnCompleteListener() {
                        @Override
                        public void onComplete(@NonNull Task task) {
                            if (task.isSuccessful()) {
                                progressDialog.dismiss();
                                frStore.collection("Users").document(userID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        editTextOldInvoice.setText(task.getResult().getString("oldInvoice"));

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(getApplicationContext(), "İnternet bağlantınızı kontrol ediniz!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                Toast.makeText(getApplicationContext(), "Güncelleme tamamlandı.", Toast.LENGTH_SHORT).show();
                            } else {
                                progressDialog.dismiss();
                                Toast.makeText(getApplicationContext(), "Güncelleme tamamlanamadı.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "İnternet bağlantınızı kontrol ediniz!", Toast.LENGTH_SHORT).show();
                        }
                    });

                }

            }
        });

    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(getApplicationContext(), AndroidCamera.class);
        try {
            lastFileName = CommonUtils.APP_PATH + "capture" + System.currentTimeMillis() + ".jpg";
            takePictureIntent.putExtra("output", lastFileName);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(),"Error "+e,Toast.LENGTH_LONG).show();
        }
    }
    public Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            //Çektiğimiz foroğrafı decode edip işlemeye hazırlıyoruz
            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap imageBitmap = BitmapFactory.decodeFile(lastFileName, options);

            if (imageBitmap == null) {
                // fotoğraf tanınamassa olacak olay
                imgCapture.setImageBitmap(imageBitmap);
                return;
            }

            finalImageBitmap = imageBitmap.getWidth() > imageBitmap.getHeight()
                    ? rotateBitmap(imageBitmap, 90) : imageBitmap;

            imageBitmap=finalImageBitmap;
            //burayı değiştirin diğer sayfa içindir.
         /*  final BitmapFactory.Options bitmapOptions=new BitmapFactory.Options();
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            bitmapOptions.inDensity = metrics.densityDpi;
            bitmapOptions.inTargetDensity=1;

            finalImageBitmap.setDensity(Bitmap.DENSITY_NONE);
            finalImageBitmap = Bitmap.createBitmap(finalImageBitmap, 10, 350, 2200, 650);*/


            imgCapture.setImageBitmap(imageBitmap);

        }
    }

    private void textOcrFromImg() {

        FirebaseVisionImage firebaseVisionImage=FirebaseVisionImage.fromBitmap(finalImageBitmap);
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();
        detector.processImage(firebaseVisionImage).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                displayTextFromImg(firebaseVisionText);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(),"Error: "+e,Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayTextFromImg(FirebaseVisionText firebaseVisionText) {
        List<FirebaseVisionText.TextBlock> blockList=firebaseVisionText.getTextBlocks();
        if (blockList.size()==0)  {
            Toast.makeText(getApplicationContext(),"Yazı algılanmadı.",Toast.LENGTH_SHORT).show();
        }
        else{
            for (FirebaseVisionText.TextBlock block: firebaseVisionText.getTextBlocks()){
                String text=block.getText();
                if (cameraCheck){
                    txtResult.setText(text);
                }
                else{editTextSerialNumber.setText(text);}

            }
        }

    }
    public static boolean hasPermissions(Context context, String... permissions) {
        //permisson kontrolleri
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // izinler verildi.
                } else {
                    this.finish();
                }
                return;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}