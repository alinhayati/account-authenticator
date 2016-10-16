//        Copyright (C) 2016 DigiGene, (www.DigiGene.com)(alinhayati[at]gmail[dot]com)
//
//        Licensed under the Apache License, Version 2.0 (the "License");
//        you may not use this file except in compliance with the License.
//        You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//        Unless required by applicable law or agreed to in writing, software
//        distributed under the License is distributed on an "AS IS" BASIS,
//        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//        See the License for the specific language governing permissions and
//        limitations under the License.

package com.digigene.accountauthenticator.activity;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.digigene.accountauthenticator.AbstractInterfaceImplementation;
import com.digigene.accountauthenticator.AuthenticatorManager;
import com.digigene.accountauthenticator.R;
import com.digigene.accountauthenticator.result.RegisterResult;

public class RegistrationActivity extends AppCompatActivity {

    private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
    private Bundle mResultBundle = null;
    private AbstractInterfaceImplementation interfaceImplementation;
    private String accountName, accountType, authTokenType;
    private String[] requiredFeatures;
    private Bundle options;
    private boolean isFromGetAuth, isAddingNewAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAccountAuthenticatorResponse =
                getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);

        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse.onRequestContinued();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent startingIntent = getIntent();
        getParamsFromIntent(startingIntent);
        loadInterfaceImplementationFromSharedPref();
    }

    public void finish() {
        if (mAccountAuthenticatorResponse != null) {
            if (mResultBundle != null) {
                mAccountAuthenticatorResponse.onResult(mResultBundle);
            } else {
                mAccountAuthenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED,
                        "canceled");
            }
            mAccountAuthenticatorResponse = null;
        }
        super.finish();
    }

    protected void register(String accountName, String password, String[] requiredFeatures, Bundle options) {
        Context context = getBaseContext();
        if (accountName == null || accountName.trim().isEmpty()) {
            Toast.makeText(context, context.getString(R.string.auth_msg_account_name_is_null), Toast.LENGTH_SHORT).show();
            return;
        }
        if (options == null) {
            options = new Bundle();
        }
        options.putBoolean(AuthenticatorManager.KEY_IS_ADD_FROM_INSIDE_APP, isFromGetAuth);
        options.putBoolean(AuthenticatorManager.KEY_IS_ADDING_NEW_ACCOUNT, isAddingNewAccount);
        new RegisterAsync(context, accountName, password, requiredFeatures, options).execute();
    }

    private Bundle makeBundle(String accountName, String accountType, String authTokenType, String refreshToken, Bundle options) {
        Bundle bundle = new Bundle();
        bundle.putString(AccountManager.KEY_ACCOUNT_NAME, accountName);
        bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
        bundle.putString(AccountManager.KEY_PASSWORD, refreshToken);
        bundle.putString(AuthenticatorManager.KEY_AUTH_TOKEN_TYPE, authTokenType);
        bundle.putBundle(AuthenticatorManager.KEY_AUTH_ACCOUNT_OPTIONS, options);
        return bundle;
    }

    private void getParamsFromIntent(Intent intent) {
        if (intent != null) {
            accountType = intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
            authTokenType = intent.getStringExtra(AuthenticatorManager.KEY_AUTH_TOKEN_TYPE);
            requiredFeatures = intent.getStringArrayExtra(AuthenticatorManager.KEY_REQUIRED_FEATURES);
            options = intent.getBundleExtra(AuthenticatorManager.KEY_AUTH_ACCOUNT_OPTIONS);
            isFromGetAuth = options.getBoolean(AuthenticatorManager.KEY_IS_ADD_FROM_INSIDE_APP, false);
            isAddingNewAccount = options.getBoolean(AuthenticatorManager.KEY_IS_ADDING_NEW_ACCOUNT, false);
            accountName = options.getString(AccountManager.KEY_ACCOUNT_NAME, null);
        }
    }

    private void loadInterfaceImplementationFromSharedPref() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(RegistrationActivity.this);
        String interfaceImplementationClassName = sharedPreferences.getString(AuthenticatorManager.KEY_INTERFACE_IMPLEMENTATION_CLASS_NAME, AbstractInterfaceImplementation.class.getName());
        Class<? extends AbstractInterfaceImplementation> interfaceImplementationClass = null;
        try {
            interfaceImplementationClass = (Class<? extends AbstractInterfaceImplementation>) Class.forName(interfaceImplementationClassName);
            interfaceImplementation = interfaceImplementationClass.newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Log.d("AppAccountAuthenticator", "The class name for AbstractInterfaceImplementation is not correct or it does not extend " + AbstractInterfaceImplementation.class.getName());
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public final void setAccountAuthenticatorResult(Bundle result) {
        mResultBundle = result;
    }

    private class RegisterAsync extends AsyncTask {

        private Context context;
        private String accountName;
        private String password;
        private String[] requiredFeatures;
        private Bundle options;
        private Account account;
        private RegisterResult registerResult;

        public RegisterAsync(Context context, String accountName, String password, String[] requiredFeatures, Bundle options) {
            this.context = context;
            this.accountName = accountName;
            this.password = password;
            this.requiredFeatures = requiredFeatures;
            this.options = options;
        }

        @Override
        protected Object doInBackground(Object[] params) {
            account = new Account(accountName, accountType);
            registerResult = interfaceImplementation.registerInServer(context, account, password, authTokenType, requiredFeatures, options);
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            if (registerResult.isSuccessful) {
                String refreshToken = registerResult.refreshToken;
                AccountManager.get(context).addAccountExplicitly(account, refreshToken, options);
                Bundle bundle = makeBundle(accountName, accountType, authTokenType, refreshToken, options);
                setAccountAuthenticatorResult(bundle);
                if (!isFromGetAuth) {
                    Toast.makeText(RegistrationActivity.this, getString(R.string.auth_msg_registration_successful), Toast.LENGTH_SHORT).show();
                }
                finish();
            } else {
                throw new RuntimeException("User registration is not successful in authenticator due to the following error:/n" +
                        registerResult.errMessage);
            }
        }
    }

}
