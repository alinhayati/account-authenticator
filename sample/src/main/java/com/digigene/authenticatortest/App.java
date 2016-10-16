package com.digigene.authenticatortest;

import android.app.Application;

import com.digigene.accountauthenticator.AuthenticatorManager;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AuthenticatorManager.authenticatorManager = new AuthenticatorManager(this,
                getString(R.string.auth_account_type),
                MyRegistrationActivity.class, MyInterfaceImplementation.class);
    }
}
