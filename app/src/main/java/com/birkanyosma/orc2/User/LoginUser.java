package com.birkanyosma.orc2.User;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.birkanyosma.orc2.LocalDB.RemindingMail;
import com.birkanyosma.orc2.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;

/** burada kullanıcı id şifresi ile giriş yapacak*/
public class LoginUser extends Activity {
    ProgressDialog dialog;
    private FirebaseAuth mAuth;
    TextView txtNewAcc;
    EditText txtMail;
    TextView txtResetPassword;
    EditText txtPassword;
    Button btnLogin;
    String mail, password;
    RemindingMail db;
    ArrayList<HashMap<String, String>> remindingList;
    CheckBox reminding;
    String check = "False";
    String oturumDurum = "False";
    FirebaseFirestore frStore;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_login_user);
        txtNewAcc = findViewById(R.id.txt_register);
        txtMail = findViewById(R.id.edit_txt_email);
        txtResetPassword = findViewById(R.id.txt_reset_password);
        txtPassword = findViewById(R.id.edit_txt_password);
        reminding = findViewById(R.id.checkbox_reminding);
        btnLogin = findViewById(R.id.btn_Login);
        mAuth = FirebaseAuth.getInstance();
        frStore = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        dialog = new ProgressDialog(LoginUser.this);
        setMailPassword();//beni hatırlaya basıldıysa edit texte yazılıyor

        if (check.equals("True")) {
            reminding.setChecked(true);
        } else {
            reminding.setChecked(false);
        }

        reminding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!reminding.isChecked()) {
                    db = new RemindingMail(getApplicationContext());
                    db.resetReminding();
                }
            }
        });
        txtNewAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //yeni hesap için sayfa geçişi
                Intent i = new Intent(getApplicationContext(), RegisterUser.class);
                startActivity(i);
                finish();
            }
        });
        txtResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //şifre sıfırlama metodu
                setContentView(R.layout.sifre_sifirla_layout);
                ImageView imgClose = findViewById(R.id.img_close);
                final EditText txtResetPasswordMail = findViewById(R.id.txt_reset_password_mail);
                Button btnResetPasswordSend = findViewById(R.id.btn_reset_password_send);
                imgClose.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(getApplicationContext(), LoginUser.class);
                        startActivity(i);
                        finish();
                    }
                });
                btnResetPasswordSend.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //mail yollanıyor ve mailden şifre sıfırlanıyor
                        String resetPassMail = txtResetPasswordMail.getText().toString();
                        if (!resetPassMail.equals("")) {
                            FirebaseAuth auth = FirebaseAuth.getInstance();
                            auth.sendPasswordResetEmail(resetPassMail)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(getApplicationContext(), "Mailinizi kontrol edin!", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(getApplicationContext(), "Mail gönderilemedi!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(getApplicationContext(), "Mail kısmı boş kalamaz!", Toast.LENGTH_SHORT).show();
                        }

                    }
                });

            }
        });
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //id şifre ile giriş
                mail = txtMail.getText().toString();
                password = txtPassword.getText().toString();
                if (!TextUtils.isEmpty(mail) && !TextUtils.isEmpty(password)) {
                    if (reminding.isChecked()) {
                        db = new RemindingMail(getApplicationContext());
                        if (remindingList.size() != 0) {
                            db.resetReminding();
                        }
                        check = "True";
                        db.reminding(mail, password, check, "True");
                        db.close();
                    } else {
                        db = new RemindingMail(getApplicationContext());
                        db.resetReminding();
                    }

                    dialog.setTitle("Oturum Açılıyor");
                    dialog.setMessage("Giriş yapılıyor lütfen bekleyin...");
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();
                    Login(mail, password);//giriş fonksiyonu
                } else {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    };
                    AlertDialog.Builder builder = new AlertDialog.Builder(LoginUser.this);
                    builder.setMessage("Mail veya şifre boş kalamaz!").setPositiveButton("Tamam", dialogClickListener).show();
                }
            }
        });
    }

    public void Login(String mail, String password) {
        mAuth.signInWithEmailAndPassword(mail, password)
                .addOnCompleteListener(LoginUser.this
                        , new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    dialog.dismiss();
                                    user = mAuth.getCurrentUser();
                                    if (user.isEmailVerified()) {// email doğrulanmış ise giriş yapıyor yoksa doğrulama gönderilsin mi diye soruyor
                                        Toast.makeText(getApplicationContext(), "Giriş yapıldı", Toast.LENGTH_SHORT).show();
                                        Intent i = new Intent(getApplicationContext(), UserOptions.class);
                                        startActivity(i);
                                        finish();
                                    } else {
                                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (which == DialogInterface.BUTTON_NEGATIVE) {

                                                }
                                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                                    user.sendEmailVerification()
                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()) {
                                                                        Toast.makeText(getApplicationContext(), "E-posta doğrulamanız tekrar gönderildi.", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                }
                                                            }).addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Toast.makeText(getApplicationContext(), "E-posta Bulunamadı!", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                }
                                            }
                                        };

                                        AlertDialog.Builder builder = new AlertDialog.Builder(LoginUser.this);
                                        builder.setMessage("E-postanız doğrulanmamış.E-posta doğruması tekrar gönderilsin mi?").setNegativeButton("İptal", dialogClickListener).setPositiveButton("Tamam", dialogClickListener).show();


                                    }

                                } else {
                                    dialog.dismiss();
                                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (which == DialogInterface.BUTTON_NEGATIVE) {

                                            }
                                        }
                                    };

                                    AlertDialog.Builder builder = new AlertDialog.Builder(LoginUser.this);
                                    builder.setMessage("Giriş yapılamadı!\n" + task.getException().getMessage()).setPositiveButton("Tamam", dialogClickListener).show();

                                }


                            }
                        });

    }

    public void setMailPassword() {
        //edit texlerin doldurulması
        db = new RemindingMail(getApplicationContext());
        remindingList = db.remindingList();
        if (remindingList.size() != 0) {
            for (int i = 0; i < remindingList.size(); i++) {
                txtMail.setText(remindingList.get(i).get("email"));
                txtPassword.setText(remindingList.get(i).get("password"));
                check = remindingList.get(i).get("checkboxdurum");
                oturumDurum = remindingList.get(i).get("oturumdurum");
            }
        }
    }
}