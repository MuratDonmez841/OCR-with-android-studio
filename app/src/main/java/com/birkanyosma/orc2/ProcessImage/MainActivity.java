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
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.birkanyosma.orc2.AndroidCamera;
import com.birkanyosma.orc2.CameraPreview;
import com.birkanyosma.orc2.CommonUtils;
import com.birkanyosma.orc2.R;
import com.birkanyosma.orc2.User.UserOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        btnCamera=findViewById(R.id.btn_capture);
        btnOcr=findViewById(R.id.btn_ocr);
        txtResult=findViewById(R.id.edit_text_ocr);
        imgCapture=findViewById(R.id.img_ocr);
        btnSave=findViewById(R.id.btn_save);
        btnExit=findViewById(R.id.btnExit);
        progressDialog=new ProgressDialog(MainActivity.this);
        mAuth = FirebaseAuth.getInstance();//firebase auth çekiyoruz
        user = mAuth.getCurrentUser();//doğrunlamış kullanıcıyı çekiyoruz
        frStore = FirebaseFirestore.getInstance();//firestorun bağlantısını çekiyoruz
        String[] PERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET};//bağlantı izinlerini istiyoruz

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, 1);//izinleri gösteriyoruz
        }

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
                txtResult.setText("");
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
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String serialNumber = txtResult.getText().toString();
                if (txtResult.getText().length()==8){
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.setMessage("İşlem yapılıyor..");
                    progressDialog.show();
                        //bu işlemde hashmape serial numberı atıyoruz ve daha sonra firebasede güncelliyoruz
                        userID = user.getUid();



                        Map userupdateMap = new HashMap();
                        userupdateMap.put("serialNumber", serialNumber);
                        frStore.collection("Users").document(userID).update(userupdateMap).addOnCompleteListener(new OnCompleteListener() {
                            @Override
                            public void onComplete(@NonNull Task task) {
                                if (task.isSuccessful()) {
                                    progressDialog.dismiss();
                                    Toast.makeText(getApplicationContext(), "Seri numarınız kaydedildi.", Toast.LENGTH_SHORT).show();
                                } else {
                                    progressDialog.dismiss();
                                    Toast.makeText(getApplicationContext(), "Güncelleme işlemi tamamlanamadı!" + task.getException(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.dismiss();
                                Toast.makeText(getApplicationContext(), "Güncelleme işlemi tamamlanamadı!", Toast.LENGTH_SHORT).show();
                            }
                        });

                }
                else {
                    Toast.makeText(getApplicationContext(), "Seri numaranız en az 8 karakter olmalı.", Toast.LENGTH_SHORT).show();
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

            //resim kesme ayarıdır.buraları açıp değiştiriniz.
            /*final BitmapFactory.Options bitmapOptions=new BitmapFactory.Options();
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
                txtResult.setText(text);
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