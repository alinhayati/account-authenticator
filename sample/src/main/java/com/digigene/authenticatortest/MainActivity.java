package com.digigene.authenticatortest;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.digigene.accountauthenticator.AuthenticatorManager;

public class MainActivity extends Activity {
    EditText accountNameEditText;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        accountNameEditText = (EditText) findViewById(R.id.account_name);
    }

    public void signIn(View view) {
        String authTokenType = "REGULAR_USER";
        AuthenticatorManager.authenticatorManager.getAccessToken(accountNameEditText.getText().toString(),
                authTokenType, null, this);
    }

    public void addUser(View view) {
        String authTokenType = "REGULAR_USER";
        AuthenticatorManager.authenticatorManager.addAccount(authTokenType, null, null, this);
    }

}
