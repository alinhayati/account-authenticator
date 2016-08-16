package com.digigene.authenticatortest;

import android.accounts.Account;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import com.digigene.accountauthenticator.AbstractInterfaceImplementation;
import com.digigene.accountauthenticator.AuthenticatorManager;
import com.digigene.accountauthenticator.result.RegisterResult;
import com.digigene.accountauthenticator.result.SignInResult;
import com.digigene.accountauthenticator.result.SignUpResult;

public class MyInterfaceImplementation extends AbstractInterfaceImplementation {
    public final int ACCESS_TOKEN_EXPIRATION_COUNTER = 2;
    public final int REFRESH_TOKEN_EXPIRATION_COUNTER = 5;
    public static int counter = 0;

    @Override
    public String[] userAccessTypes() {
        return new String[]{"REGULAR_USER", "SUPER_USER"};
    }

    @Override
    public void doAfterSignUpIsUnsuccessful(Context context, Account account, String authTokenType, SignUpResult signUpResult, Bundle options) {
        Toast.makeText(context, "Sign-up was not possible due to the following:\n" + signUpResult.errMessage, Toast.LENGTH_LONG).show();
        AuthenticatorManager.authenticatorManager.addAccount(authTokenType, null, options);
    }

    @Override
    public void doAfterSignInIsSuccessful(Context context, Account account, String authTokenType, String authToken, SignInResult signInResult, Bundle options) {
        Toast.makeText(context, "User is successfully signed in", Toast.LENGTH_SHORT).show();
    }

    @Override
    public SignInResult signInToServer(Context context, Account account, String authTokenType, String accessToken, Bundle options) {
        counter = counter + 1;
        SignInResult signInResult = new SignInResult();
        signInResult.isSuccessful = true;
        synchronized (this) {
            try {
                this.wait(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if ((counter > ACCESS_TOKEN_EXPIRATION_COUNTER && !accessToken.equalsIgnoreCase("SECOND_ACCESS_TOKEN")) || counter > REFRESH_TOKEN_EXPIRATION_COUNTER) {
            signInResult.isSuccessful = false;
            signInResult.errMessage = "Access token is expired";
            signInResult.isAccessTokenExpired = true;
            return signInResult;
        }
        return signInResult;
    }

    @Override
    public SignUpResult signUpToServer(Context context, Account account, String authTokenType, String refreshToken, Bundle options) {
        SignUpResult signUpResult = new SignUpResult();
        synchronized (this) {
            try {
                this.wait(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        signUpResult.isSuccessful = true;
        signUpResult.accessToken = "FIRST_ACCESS_TOKEN";
        signUpResult.refreshToken = "FIRST_REFRESH_TOKEN";
        if (counter > REFRESH_TOKEN_EXPIRATION_COUNTER) {
            counter = 0;
            signUpResult.isSuccessful = false;
            signUpResult.errMessage = "User credentials have expired, please login again";
            return signUpResult;
        }
        if (counter > ACCESS_TOKEN_EXPIRATION_COUNTER) {
            signUpResult.accessToken = "SECOND_ACCESS_TOKEN";
        }
        return signUpResult;
    }

    @Override
    public RegisterResult registerInServer(Context context, Account account, String password, String authTokenType, String[] requiredFeatures, Bundle options) {
        RegisterResult registerResult = new RegisterResult();
        registerResult.isSuccessful = false;
        synchronized (this) {
            try {
                this.wait(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (true) {  // password is checked here and, if true, refresh token is generated for the user
            registerResult.isSuccessful = true;
            registerResult.refreshToken = "INITIAL_REFRESH_TOKEN";
        }
        return registerResult;
    }

    @Override
    public boolean setDoesCallbackRunInBackgroundThread() {
        return false;
    }
}
