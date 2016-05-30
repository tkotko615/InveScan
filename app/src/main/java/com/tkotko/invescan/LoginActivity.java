package com.tkotko.invescan;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;

import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world",
            "tkotko2005@gmail.com:tkotko", "tko@:tkotko"
    };
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;
    private UserLoginADTask mADAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private EditText mADidView;
    private EditText mADpwdView;

    //String address="ldap://server1.mydomain.com";
    //String address = "mail.acesconn.com";
    String address = "192.168.0.5";
    int port = 389;
    //String bindDN="CN=name,CN=users,DC=mydomain,DC=com";
    //String bindDN = "cn=t00126,dc=acesconn,dc=com";
    //String bindDN = "acesconn\\t00126";
    //String searchScope = "ou=aDepartment,dc=acesconn,dc=com";
    String searchScope = "dc=acesconn,dc=com";
    //String password = "xxx";
    boolean login_flag = true;
    LDAPConnection c;

    private SharedPreferences spref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setupActionBar();
        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        populateAutoComplete();

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        //for AD login
        mADidView = (EditText) findViewById(R.id.ad_id);
        mADpwdView = (EditText) findViewById(R.id.ad_pwd);
        mADidView.setText("t00126");
        mADpwdView.setText("tkotko@123");
        Button mADSignInButton = (Button) findViewById(R.id.ad_sign_in_button);
        mADSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptADLogin();
            }
        });

        /*創建SharedPreferences
        MODE_PRIVATE : 私用模式開啟該檔案,只有自己的應用程式可以存取該檔案
        MODE_WORLD_READABLE 或 MODE_WORLD_WRITEABLE : 建立共用偏好設定檔案，則知道檔案識別碼的其他任何應用程式都能存取
        */
        spref = getSharedPreferences("userInfo", Context.MODE_PRIVATE);  //Context.MODE_PRIVATE = 0
        //由SharedPreferences取出值
        //File file = new File("/data/data/com.tkotko.invescan/shared_prefs","userInfo.xml");
        File file = new File(getApplicationInfo().dataDir + "/shared_prefs","userInfo.xml");
        if(file.exists()){
            mEmailView.setText(spref.getString("USER_EMAIL", ""));
            mPasswordView.setText(spref.getString("USER_PWD", ""));
        }

    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    private void attemptADLogin() {
        String bindDN;
        String password;

        bindDN = mADidView.getText().toString();
        bindDN = "acesconn\\" + bindDN;
        password = mADpwdView.getText().toString();

        /*
        //測試 LDAP filter
        String filterString = "(cn=t00126)";
        Filter filter = null;
        try {
            filter = Filter.create(filterString);
        } catch (LDAPException le) {
            le.printStackTrace();
            Toast.makeText(getBaseContext(),"LDAP filter is not vaild" , Toast.LENGTH_LONG).show();
        }
        */
        if (mADAuthTask != null) {
            return;
        }
        showProgress(true);
        mADAuthTask = new UserLoginADTask(bindDN, password);
        mADAuthTask.execute((Void) null);

    }

    public class UserLoginADTask extends AsyncTask<Void, Void, Boolean> {

        private final String mID;
        private final String mPassword;

        UserLoginADTask(String id, String password) {
            mID = id;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            /*模擬網路存取
            try {
                // Simulate network access.
                Thread.sleep(2000);  //2秒
            } catch (InterruptedException e) {
                return false;
            }
            */

            /*
            for (String credential : DUMMY_CREDENTIALS) {
                String[] pieces = credential.split(":");
                if (pieces[0].equals(mEmail)) {
                    // Account exists, return true if the password matches.
                    return pieces[1].equals(mPassword);
                }
            }*/
            try {
                c = new LDAPConnection(address, port, mID, mPassword);

                /*
                //查詢LDAP範例
                SearchResultEntry entry = queryDN(searchScope, "cn=t00126", c);
                if (entry == null) {
                    login_flag = false;
                    return;   //登录失败
                }
                */

                /*
                //取得root DSE範例
                Entry rootDSEEntry = c.getEntry("", "subschemaSubentry");
                if (rootDSEEntry == null){
                    System.err.println("Unable to retrieve the root DSE");
                    return;
                }else{
                    System.out.println("Successfully read the root DSE");
                }
                */

                /*
                //由LDAP Connection取得資料
                c.setConnectionName("Demo Connection");
                String con_name = c.getConnectionName();
                long time = c.getConnectTime();
                SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yy HH:mm:ss");
                String dateString = formatter.format(new Date(time));
                Toast.makeText(getBaseContext(),"Connected to LDAP server....connection_name="+con_name+" at time"+dateString, Toast.LENGTH_LONG).show();
                */
            } catch (LDAPException e) {
                login_flag = false;
                e.printStackTrace();
                //Toast.makeText(getBaseContext(),"No connection was established" , Toast.LENGTH_LONG).show();
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                if (login_flag) {
                    c.close();
                    //Toast.makeText(getBaseContext(), "Connection Closed successfully", Toast.LENGTH_LONG).show();
                }
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mADAuthTask = null;
            showProgress(false);

            if (success) {
                Toast.makeText(getBaseContext(), "Auth Success !!", Toast.LENGTH_LONG).show();
                //finish();
            } else {
                //mPasswordView.setError(getString(R.string.error_incorrect_password));
                //mPasswordView.requestFocus();
                Toast.makeText(getBaseContext(), "Auth Fail !!", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            mADAuthTask = null;
            showProgress(false);
        }
    }

    //LDAP 查詢範例
    public SearchResultEntry queryDN(String searchDN, String filter, LDAPConnection connection) {
        try {
            //SearchRequest searchRequest = new SearchRequest(searchDN, SearchScope.SUB, "(" + filter + ")");
            Filter filter2 = Filter.createEqualityFilter("cn", "t00126");
            SearchRequest searchRequest = new SearchRequest(searchDN, SearchScope.SUB, filter2);
            SearchResult searchResult = connection.search(searchRequest);
//                System.out.println(">>>共查詢到" + searchResult.getSearchEntries().size() + "筆資料");
            if (searchResult.getSearchEntries().size() > 0) {
                return searchResult.getSearchEntries().get(0);
            }
        } catch (Exception e) {
            //System.out.println("查詢錯誤,錯誤訊息如下：\n" + e.getMessage());
        }
        return null;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }

            for (String credential : DUMMY_CREDENTIALS) {
                String[] pieces = credential.split(":");
                if (pieces[0].equals(mEmail)) {
                    // Account exists, return true if the password matches.
                    return pieces[1].equals(mPassword);
                }
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                //將資料存入SharedPreferences
                SharedPreferences.Editor editor =spref.edit();
                editor.putString("USER_EMAIL", mEmail);
                editor.putString("USER_PWD", mPassword);
                editor.commit();

                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

