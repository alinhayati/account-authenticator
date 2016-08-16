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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.digigene.accountauthenticator.activity.RegistrationActivity;
import com.digigene.accountauthenticator.result.SignInResult;
import com.digigene.accountauthenticator.result.SignUpResult;

import java.io.IOException;

public class AuthenticatorManager {

    public static final String KEY_REQUIRED_FEATURES = "requiredFeatures";
    public static final String KEY_AUTH_TOKEN_TYPE = "authenticatorType";
    public static final String KEY_AUTH_ACCOUNT_OPTIONS = "accountOptions";
    public static final String KEY_REGISTRATION_ACTIVITY_CLASS_NAME = "registrationActivityClassName";
    public static final String KEY_INTERFACE_IMPLEMENTATION_CLASS_NAME = "interfaceImplementationClassName";
    public static final String KEY_IS_ADD_FROM_INSIDE_APP = "isFromGetAuthToken";
    public static final String KEY_IS_ADDING_NEW_ACCOUNT = "isAddingNewAccount";
    public static final String CALLBACK_THREAD_NAME = "callBackThreadName";

    private boolean isCallbackRunInBackgroundThread = false;
//    private boolean isRunInBackground = false;
//    private boolean isNotificationShownInBackground = false;

    public static AuthenticatorManager authenticatorManager;
    private AbstractInterfaceImplementation interfaceImplementation;
    private Activity activity;
    private Context context;
    private String[] authTokenTypes;
    private String accountType;

    public AuthenticatorManager(@NonNull Context context, @NonNull String accountType, @NonNull Activity callingActivity, @NonNull Class<? extends RegistrationActivity> registrationActivityClass, @NonNull Class<? extends AbstractInterfaceImplementation> interfaceImplementationClass) {
        if (callingActivity == null || registrationActivityClass == null || interfaceImplementationClass == null) {
            throw new IllegalArgumentException("None of the arguments in Authenticator Manager shall be null");
        }
        this.activity = callingActivity;
        this.context = context;
        this.accountType = accountType;
        try {
            this.interfaceImplementation = interfaceImplementationClass.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        this.authTokenTypes = interfaceImplementation.userAccessTypes();
        this.isCallbackRunInBackgroundThread = interfaceImplementation.setDoesCallbackRunInBackgroundThread();
//        this.isRunInBackground = interfaceImplementation.setIsRunInBackground();
//        this.isNotificationShownInBackground = interfaceImplementation.setIsNotificationShownInBackground();

        if (authTokenTypes == null) {
            throw new RuntimeException("Authentication token types cannot be null");
        }
        saveClassNamesInSharedPref(registrationActivityClass.getName(), interfaceImplementationClass.getName());
    }

    public void getAccessToken(String accountName, String authTokenType, Bundle options) {
        if (accountName == null || accountName.trim().isEmpty()) {
            Toast.makeText(context, context.getString(R.string.auth_msg_account_name_is_null), Toast.LENGTH_SHORT).show();
            return;
        }
        Account account;
        if (options == null) {
            options = new Bundle();
        }
        Handler handler = null;
        if (authTokenType == null) {
            authTokenType = authTokenTypes[0];
        } else {
            if (!isAuthTokenValid(authTokenType, authTokenTypes))
                throw new IllegalArgumentException("Authentication token type is not valid.");
        }
        if (isCallbackRunInBackgroundThread) handler = setHandler(CALLBACK_THREAD_NAME);
        account = new Account(accountName, accountType);
        AccountManagerCallback accountManagerCallback = null;
        boolean isAddingNewAccount = options.getBoolean(AuthenticatorManager.KEY_IS_ADDING_NEW_ACCOUNT, false);
        if (!isAddingNewAccount) {
            accountManagerCallback = getAccessTokenCallBack(authTokenType, options, account);
        }
        getAccessTokenFromAccountManager(account, authTokenType, options, accountManagerCallback, handler);
    }

    public void addAccount(String authTokenType, String[] requiredFeatures, Bundle options) {
        AccountManager accountManager = AccountManager.get(context);
        Handler handler = null;
        if (options == null) {
            options = new Bundle();
        }
        if (isCallbackRunInBackgroundThread) handler = setHandler(CALLBACK_THREAD_NAME);
        options.putBoolean(AuthenticatorManager.KEY_IS_ADD_FROM_INSIDE_APP, true);
        options.putBoolean(AuthenticatorManager.KEY_IS_ADDING_NEW_ACCOUNT, true);
        accountManager.addAccount(accountType, authTokenType, requiredFeatures, options, activity, getAddAccountCallBack(), handler);
    }

    private void getAccessTokenFromAccountManager(Account account, String authTokenType, Bundle options, AccountManagerCallback accountManagerCallback, Handler handler) {
        AccountManager accountManager = AccountManager.get(context);
        accountManager.getAuthToken(account, authTokenType, options, activity, accountManagerCallback, handler);
    }

    private AccountManagerCallback getAccessTokenCallBack(final String authTokenType, final Bundle options, final Account account) {
        AccountManagerCallback accountManagerCallback = new AccountManagerCallback() {
            @Override
            public void run(AccountManagerFuture future) {
                try {
                    Bundle bundle = (Bundle) future.getResult();
                    String accountName = bundle.getString(AccountManager.KEY_ACCOUNT_NAME, null);
                    String refreshToken = bundle.getString(AccountManager.KEY_PASSWORD, null);
                    String authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN, null);
                    if (authToken != null) {
                        signInAndDoAfter(account, authTokenType, authToken, options);
                    } else {
                        if (refreshToken != null) {
                            signUpAndDoAfter(account, authTokenType, refreshToken, options);
                        } else {
                            if (accountName != null) {
                                addAccount(authTokenType, null, options);
                            } else {
                                Toast.makeText(context, context.getString(R.string.auth_msg_account_does_not_exist), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                } catch (OperationCanceledException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (AuthenticatorException e) {
                    e.printStackTrace();
                }
            }
        };
        return accountManagerCallback;
    }

    private AccountManagerCallback getAddAccountCallBack() {
        AccountManagerCallback accountManagerCallback = new AccountManagerCallback() {
            @Override
            public void run(AccountManagerFuture future) {
                try {
                    Bundle result = (Bundle) future.getResult();
                    String accountName = result.getString(AccountManager.KEY_ACCOUNT_NAME);
                    String accountType = result.getString(AccountManager.KEY_ACCOUNT_TYPE);
                    String refreshToken = result.getString(AccountManager.KEY_PASSWORD);
                    String authTokenType = result.getString(KEY_AUTH_TOKEN_TYPE);
                    Bundle options = result.getBundle(KEY_AUTH_ACCOUNT_OPTIONS);
                    Account account = new Account(accountName, accountType);
                    signUpAndDoAfter(account, authTokenType, refreshToken, options);
                } catch (OperationCanceledException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (AuthenticatorException e) {
                    e.printStackTrace();
                }
                Toast.makeText(context, "After registering the user", Toast.LENGTH_SHORT).show();
            }
        };
        return accountManagerCallback;
    }

    private Handler setHandler(String threadName) {
        Handler handler;
        HandlerThread handlerThread = new HandlerThread(threadName);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        return handler;
    }

    private boolean isAuthTokenValid(String tokenType, String[] authTokenTypes) {
        boolean authTokenIsValid = false;
        for (int i = 0; i < authTokenTypes.length; i++) {
            if (tokenType.equalsIgnoreCase(authTokenTypes[i])) {
                authTokenIsValid = true;
                break;
            }
        }
        return authTokenIsValid;
    }

    private Bundle doAfterSignUpIsSuccessful(AccountManager accountManager, Account account, String authTokenType, SignUpResult signUpResult, Bundle options) {
        Bundle result;
        if (signUpResult.isSuccessful) {
            accountManager.setPassword(account, signUpResult.refreshToken);
            accountManager.setAuthToken(account, authTokenType, signUpResult.accessToken);
            result = makeResultBundle(account, signUpResult.refreshToken, signUpResult.accessToken);
            Toast.makeText(context, "New refresh token is acquired from the server", Toast.LENGTH_SHORT).show();
            signInAndDoAfter(account, authTokenType, signUpResult.accessToken, options);
            return result;
        } else {
            throw new RuntimeException("Sign-up is not successful due to the following:/n" + signUpResult.errMessage);
        }
    }

    public Bundle getAccessTokenFromCache(Account account, String authTokenType, AccountManager accountManager) {
        Bundle result;
        String cachedAuthToken = accountManager.peekAuthToken(account, authTokenType);
        String refreshToken = accountManager.getPassword(account);
        if (cachedAuthToken != null) {
            result = makeResultBundle(account, refreshToken, cachedAuthToken);
            return result;
        }
        return null;
    }

    public Bundle makeResultBundle(Account account, String refreshToken, String accessToken) {
        Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
        result.putString(AccountManager.KEY_PASSWORD, refreshToken);
        result.putString(AccountManager.KEY_AUTHTOKEN, accessToken);
        return result;
    }

    private SignInResult signInAndDoAfter(final Account account, final String authTokenType, final String authToken, final Bundle options) {
        final SignInResult[] signInResult = new SignInResult[1];
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                signInResult[0] = interfaceImplementation.signInToServer(context, account, authTokenType, authToken, options);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (signInResult[0].isSuccessful) {
                    interfaceImplementation.doAfterSignInIsSuccessful(context, account, authTokenType, authToken, signInResult[0], options);
                } else {
                    if (signInResult[0].isAccessTokenExpired) {
                        AccountManager accountManager = AccountManager.get(context);
                        accountManager.invalidateAuthToken(accountType, authToken);
                        Toast.makeText(context, signInResult[0].errMessage + ", getting new access token from the server", Toast.LENGTH_SHORT).show();
                        signUpAndDoAfter(account, authTokenType, accountManager.getPassword(account), options);
                    } else {
                        Toast.makeText(context, "Sign-in was not possible due to the following:\n" + signInResult[0].errMessage, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }.execute();
        return signInResult[0];
    }

    public Bundle signUpAndDoAfter(final Account account, final String authTokenType, final String refreshToken, final Bundle options) {
        final Bundle[] result = new Bundle[1];
        final SignUpResult[] signUpResult = new SignUpResult[1];
        if (refreshToken != null) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    signUpResult[0] = interfaceImplementation.signUpToServer(context, account, authTokenType, refreshToken, options);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    if (signUpResult[0].isSuccessful) {
                        result[0] = doAfterSignUpIsSuccessful(AccountManager.get(context), account, authTokenType, signUpResult[0], options);
                    } else {
                        AccountManager accountManager = AccountManager.get(context);
                        accountManager.clearPassword(account);
                        interfaceImplementation.doAfterSignUpIsUnsuccessful(context, account, authTokenType, signUpResult[0], options);
                    }
                }
            }.execute();
        } else {
            throw new RuntimeException("Refresh token is null in authenticator");
        }
        return result[0];
    }

    private void saveClassNamesInSharedPref(String registrationActivityName, String interfaceImplementationClassName) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(AuthenticatorManager.KEY_REGISTRATION_ACTIVITY_CLASS_NAME, registrationActivityName);
        editor.putString(AuthenticatorManager.KEY_INTERFACE_IMPLEMENTATION_CLASS_NAME, interfaceImplementationClassName);
        editor.commit();
    }

}
