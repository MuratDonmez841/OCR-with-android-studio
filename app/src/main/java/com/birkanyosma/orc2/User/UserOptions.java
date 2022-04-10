package com.birkanyosma.orc2.User;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.birkanyosma.orc2.ProcessImage.InvoiceAc;
import com.birkanyosma.orc2.ProcessImage.MainActivity;
import com.birkanyosma.orc2.R;
import com.google.firebase.auth.FirebaseAuth;

public class UserOptions extends AppCompatActivity {
    Button btnExit;
    Button btnInvoice;
    Button btnAddSerialNumber;
    FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_user_options);

        btnExit = findViewById(R.id.btn_exit);
        btnInvoice = findViewById(R.id.btn_invoice);
        mAuth = FirebaseAuth.getInstance();
        btnAddSerialNumber = (Button) findViewById(R.id.btn_add_serial_number);

        btnAddSerialNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(i);
                finish();
            }
        });
        btnInvoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), InvoiceAc.class);
                startActivity(i);
                finish();
            }
        });
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Intent i = new Intent(getApplicationContext(), LoginUser.class);
                startActivity(i);
                finish();
            }
        });
    }
}