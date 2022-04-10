package com.birkanyosma.orc2.User;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.birkanyosma.orc2.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/** Bu sayfada kullanıcı kaydı alıyoruz*/
public class RegisterUser extends Activity {
    private FirebaseDatabase db;
    private Button btn_register;
    private EditText txt_name;
    private EditText txt_email;
    private EditText txtpassword, txtpassword2;
    private TextView txt_back, txt_kosul;
    private TextView txt_name_control, txt_sifre_control;
    private String mail;
    private String password;
    private ProgressDialog dialog;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mfirestore;
    private String name;
    private boolean nameCheck = true, passCheck = true;
    private ArrayList<String> nameControlList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_register_user);
        db = FirebaseDatabase.getInstance();
        txt_back = findViewById(R.id.txt_back);
        txt_name = findViewById(R.id.txt_Name);
        txt_email = findViewById(R.id.txt_email);
        txt_name_control = findViewById(R.id.txt_user_registaration_name_control);
        txt_sifre_control = findViewById(R.id.txt_sifre_uyusmazlıgı);
        txtpassword = findViewById(R.id.txt_password);
        txtpassword2 = findViewById(R.id.txt_password2);
        btn_register = findViewById(R.id.btn_register_user_register_page);
        mAuth = FirebaseAuth.getInstance();
        mfirestore = FirebaseFirestore.getInstance();
        dialog = new ProgressDialog(RegisterUser.this);
        txt_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), LoginUser.class);
                startActivity(i);
                finish();//geri çıkmak için
            }
        });
        // bu fonksiyonda databasede ki isimleri çekiyoruz
        mfirestore.collection("Users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot doc : task.getResult()) {
                        doc.getString("name");
                        nameControlList.add(doc.getString("name"));
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
        //1. password ile 2. passwordun eşleşip eşleşmediğini kontrol ediyor
        txtpassword2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (txtpassword.getText().toString().equals(txtpassword2.getText().toString())) {
                    txt_sifre_control.setVisibility(View.GONE);
                    btn_register.setEnabled(true);
                    btn_register.setClickable(true);
                    passCheck = true;
                } else {
                    txt_sifre_control.setVisibility(View.VISIBLE);
                    passCheck = false;
                    btn_register.setEnabled(false);
                    btn_register.setClickable(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        //aldığınız isimle dayabasedeki isimleri karşılaştırıyor aynı isimleri almanıza izin vermiyor
        txt_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                name = txt_name.getText().toString();
                if (nameControlList != null) {
                    for (String names : nameControlList) {
                        if (name.toLowerCase().equals(names.toLowerCase())) {
                            txt_name_control.setVisibility(View.VISIBLE);
                            nameCheck = false;
                            btn_register.setEnabled(false);
                            btn_register.setClickable(false);
                            break;
                        } else {
                            txt_name_control.setVisibility(View.GONE);
                            nameCheck = true;
                            btn_register.setEnabled(true);
                            btn_register.setClickable(true);
                        }

                    }
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        //kayıt işlemi yapıyoruz
        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (passCheck) {
                    mail = txt_email.getText().toString();
                    password = txtpassword.getText().toString();
                    name = txt_name.getText().toString();
                    if (nameCheck) {
                        if (!TextUtils.isEmpty(mail) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(name)) {
                            dialog.setTitle("Kayıt İşlemi");
                            dialog.setMessage("Kaydınız yapılıyor...");
                            dialog.setCanceledOnTouchOutside(false);
                            dialog.show();
                            Register(mail, password, name);//kayıt işlemi fonksiyonu
                        } else {
                            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == DialogInterface.BUTTON_NEGATIVE) {

                                    }
                                }
                            };

                            AlertDialog.Builder builder = new AlertDialog.Builder(RegisterUser.this);
                            builder.setMessage("Bilgileri eksizsiz bir şekilde doldurun!").setPositiveButton("Tamam", dialogClickListener).show();

                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Kullanıcı adı kullanılmaktadır!", Toast.LENGTH_SHORT).show();
                    }
                }

            }


        });


    }

    public void Register(final String mail, String password, final String name) {
        mAuth.createUserWithEmailAndPassword(mail, password)
                .addOnCompleteListener(RegisterUser.this, new OnCompleteListener<AuthResult>() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            final String userID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                            //kayıt olan kişinin bilgileri dolduruyoruz
                            HashMap<String, String> userMap = new HashMap<>();
                            userMap.put("name", name);
                            userMap.put("mail", mail);
                            userMap.put("serialNumber", "-");//bıralar default bilgiler değiştirmeyin
                            userMap.put("oldInvoice","-");
                            userMap.put("date","-");
                            userMap.put("invoice","-");

                            mfirestore.collection("Users").document(userID).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        //kayıt işlemi başarılı oldu
                                        dialog.dismiss();
                                        txt_name.setText("");//textleri temizliyoruz
                                        txt_email.setText("");
                                        txtpassword.setText("");
                                        Toast.makeText(getApplicationContext(), "Kayıt Yapıldı!", Toast.LENGTH_SHORT).show();
                                        Intent i = new Intent(getApplicationContext(), LoginUser.class);//ana sayfaya geçiş
                                        startActivity(i);
                                        finish();

                                    } else {
                                        Toast.makeText(getApplicationContext(), "Kayıt Yapılamadı!", Toast.LENGTH_SHORT).show();
                                    }

                                }

                            });
                            //bu fonksiyonda e posta doğrulaması yapıyoruz
                            FirebaseUser user = mAuth.getCurrentUser();
                            user.sendEmailVerification()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(getApplicationContext(), "Doğrulama Gönderildi", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(getApplicationContext(), "E-mail Bulunamadı!", Toast.LENGTH_SHORT).show();
                                }
                            });

                        } else {
                            dialog.dismiss();
                            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == DialogInterface.BUTTON_NEGATIVE) {

                                    }
                                }
                            };

                            AlertDialog.Builder builder = new AlertDialog.Builder(RegisterUser.this);
                            builder.setMessage("Kayıt Yapılamadı").setPositiveButton("Tamam", dialogClickListener).show();


                        }


                    }
                });
    }

}