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
    public static int accessTokenCounter = 0;
    public static int refreshTokenCounter = 0;
    public static int demoCounter = 0;
    public static int accessTokenNo = 0;
    public static int refreshTokenNo = 0;
    public final int ACCESS_TOKEN_EXPIRATION_COUNTER = 2;
    public final int REFRESH_TOKEN_EXPIRATION_COUNTER = 5;
    public final int DEMO_COUNTER = 15;

    @Override
    public String[] userAccessTypes() {
        return new String[]{"REGULAR_USER", "SUPER_USER"};
    }

    @Override
    public void doAfterSignUpIsUnsuccessful(Context context, Account account, String
            authTokenType, SignUpResult signUpResult, Bundle options) {
        Toast.makeText(context, "Sign-up was not possible due to the following:\n" + signUpResult
                .errMessage, Toast.LENGTH_LONG).show();
        AuthenticatorManager.authenticatorManager.addAccount(authTokenType, null, options);
    }

    @Override
    public void doAfterSignInIsSuccessful(Context context, Account account, String authTokenType,
                                          String authToken, SignInResult signInResult, Bundle
                                                  options) {
        demoCounter = demoCounter + 1;
        Toast.makeText(context, "User is successfully signed in: \naccessTokenNo=" +
                accessTokenNo + "\nrefreshTokenNo=" + refreshTokenNo +
                "\ndemoCounter=" + demoCounter, Toast.LENGTH_SHORT).show();
    }

    @Override
    public SignInResult signInToServer(Context context, Account account, String authTokenType,
                                       String accessToken, Bundle options) {
        accessTokenCounter = accessTokenCounter + 1;
        SignInResult signInResult = new SignInResult();
        signInResult.isSuccessful = true;
        synchronized (this) {
            try {
                this.wait(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if ((accessTokenCounter > ACCESS_TOKEN_EXPIRATION_COUNTER || demoCounter > DEMO_COUNTER)) {
            signInResult.isSuccessful = false;
            signInResult.isAccessTokenExpired = true;
            if (demoCounter < DEMO_COUNTER) {
                signInResult.errMessage = "Access token is expired";
                return signInResult;
            }
        }
        return signInResult;
    }

    @Override
    public SignUpResult signUpToServer(Context context, Account account, String authTokenType,
                                       String refreshToken, Bundle options) {
        SignUpResult signUpResult = new SignUpResult();
        synchronized (this) {
            try {
                this.wait(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        refreshTokenCounter = refreshTokenCounter + 1;
        signUpResult.isSuccessful = true;
        signUpResult.accessToken = "ACCESS_TOKEN_NO_" + accessTokenNo;
        signUpResult.refreshToken = "REFRESH_TOKEN_NO_" + refreshTokenNo;
        if (demoCounter > DEMO_COUNTER) {
            signUpResult.isSuccessful = false;
            signUpResult.errMessage = "You have reached your limit of using the demo version. " +
                    "Please buy it for further usage";
            return signUpResult;
        }
        if (refreshTokenCounter > REFRESH_TOKEN_EXPIRATION_COUNTER) {
            refreshTokenCounter = 0;
            signUpResult.isSuccessful = false;
            signUpResult.errMessage = "User credentials have expired, please login again";
            return signUpResult;
        }
        if (accessTokenCounter > ACCESS_TOKEN_EXPIRATION_COUNTER) {
            accessTokenCounter = 0;
            accessTokenNo = accessTokenNo + 1;
            signUpResult.accessToken = "ACCESS_TOKEN_NO_" + accessTokenNo;
        }
        return signUpResult;
    }

    @Override
    public RegisterResult registerInServer(Context context, Account account, String password,
                                           String authTokenType, String[] requiredFeatures,
                                           Bundle options) {
        RegisterResult registerResult = new RegisterResult();
        registerResult.isSuccessful = false;
        synchronized (this) {
            try {
                this.wait(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (true) {  // password is checked here and, if true, refresh token is generated for the
            // user
            refreshTokenNo = refreshTokenNo + 1;
            accessTokenNo = accessTokenNo + 1;
            registerResult.isSuccessful = true;
            registerResult.refreshToken = "REFRESH_TOKEN_NO_" + refreshTokenNo;
        }
        return registerResult;
    }

    @Override
    public boolean setDoesCallbackRunInBackgroundThread() {
        return false;
    }
}
