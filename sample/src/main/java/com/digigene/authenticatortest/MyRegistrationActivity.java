package com.digigene.authenticatortest;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.digigene.accountauthenticator.activity.RegistrationActivity;

public class MyRegistrationActivity extends RegistrationActivity {
    private EditText accountNameEditText, passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration_layout);
        accountNameEditText = (EditText) findViewById(R.id.account_name);
        passwordEditText = (EditText) findViewById(R.id.password);
    }

    public void startAuthentication(View view) {
        register(accountNameEditText.getText().toString(), passwordEditText.getText().toString(),
                null, null);
    }

}
