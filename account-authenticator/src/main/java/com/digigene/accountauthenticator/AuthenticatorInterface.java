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
import android.content.Context;
import android.os.Bundle;

import com.digigene.accountauthenticator.result.RegisterResult;
import com.digigene.accountauthenticator.result.SignInResult;
import com.digigene.accountauthenticator.result.SignUpResult;

public interface AuthenticatorInterface {

    /**
     * Authentication token types shall be defined and customized here, e.g. FULL_ACCESS, LIMITED ACCESS, etc
     */
    public String[] userAccessTypes();

    /**
     * @param {boolean} isRunInBackground, isNotificationShownInBackground and isCallbackRunInBackgroundThread can be customized here (Optional).
     */

    public static interface RunInBackgroundOptions {
        public boolean setDoesCallbackRunInBackgroundThread();
//        public boolean setIsRunInBackground();
//        public boolean setIsNotificationShownInBackground();
    }

    public static interface ServerConnection {
        /**
         * Used for signing in to the server takes place using the provided access token
         */
        public SignInResult signInToServer(Context context, Account account, String authTokenType, String accessToken, Bundle options);

        /**
         * Access token is returned as a result of this method, in which refresh token is used for getting access token from the server.
         */
        public SignUpResult signUpToServer(Context context, Account account, String authTokenType, String refreshToken, Bundle options);

        /**
         * Refresh token is returned as a result of this method
         */
        public RegisterResult registerInServer(Context context, Account account, String password, String authTokenType, String[] requiredFeatures, Bundle options);
    }

    public static interface CallBack {
        //        public void doAfterRegistration(Context context);
        public void doAfterSignUpIsUnsuccessful(Context context, Account account, String authTokenType, SignUpResult signUpResult, Bundle options);

        public void doAfterSignInIsSuccessful(Context context, Account account, String authTokenType, String authToken, SignInResult signInResult, Bundle options);
    }

}
