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

package com.digigene.accountauthenticator;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.digigene.accountauthenticator.activity.RegistrationActivity;

public class AppAccountAuthenticator extends AbstractAccountAuthenticator {

    private Context context;
    private Class<? extends RegistrationActivity> registrationActivityClass;

    public AppAccountAuthenticator(Context context) {
        super(context);
        this.context = context;
        loadRegistrationClassFromSharedPref();
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        Intent intent = makeIntent(response, accountType, authTokenType, requiredFeatures, options);
        Bundle bundle = makeBundle(intent);
        return bundle;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        AuthenticatorManager authenticatorManager = AuthenticatorManager.authenticatorManager;
        Bundle result;
        AccountManager accountManager = AccountManager.get(context);
        // case 1: access token is available
        result = authenticatorManager.getAccessTokenFromCache(account, authTokenType, accountManager);
        if (result != null) {
            return result;
        }
        final String refreshToken = accountManager.getPassword(account);
        // case 2: access token is not available but refresh token is
        if (refreshToken != null) {
            result = authenticatorManager.makeResultBundle(account, refreshToken, null);
            return result;
        }
        // case 3: neither tokens is available but the account exists
        if (isAccountAvailable(account, accountManager)) {
            result = authenticatorManager.makeResultBundle(account, null, null);
            return result;
        }
        // case 4: account does not exist
        return new Bundle();
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        return null;
    }

    private boolean isAccountAvailable(Account account, AccountManager accountManager) {
        Account[] availableAccounts = accountManager.getAccountsByType(account.type);
        for (Account availableAccount : availableAccounts) {
            if (account.name.equals(availableAccount.name) && account.type.equals(availableAccount.type)) {
                return true;
            }
        }
        return false;
    }

    private void loadRegistrationClassFromSharedPref() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String registrationActivityClassName = sharedPreferences.getString(AuthenticatorManager.KEY_REGISTRATION_ACTIVITY_CLASS_NAME, RegistrationActivity.class.getName());

        try {
            registrationActivityClass = (Class<? extends RegistrationActivity>) Class.forName(registrationActivityClassName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Log.d("AppAccountAuthenticator", "The class name for RegistrationActivity is not correct or it does not extend " + RegistrationActivity.class.getName());
        }
    }

    @NonNull
    private Bundle makeBundle(Intent intent) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @NonNull
    private Intent makeIntent(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) {
        Intent intent = new Intent(context, registrationActivityClass);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType);
        intent.putExtra(AuthenticatorManager.KEY_AUTH_TOKEN_TYPE, authTokenType);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(AuthenticatorManager.KEY_REQUIRED_FEATURES, requiredFeatures);
        intent.putExtra(AuthenticatorManager.KEY_AUTH_ACCOUNT_OPTIONS, options);
        return intent;
    }

}
