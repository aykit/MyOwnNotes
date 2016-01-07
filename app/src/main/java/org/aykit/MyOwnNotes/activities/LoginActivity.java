package org.aykit.MyOwnNotes.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;

import org.aykit.MyOwnNotes.R;
import org.aykit.MyOwnNotes.helpers.Settings;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    @Bind(R.id.coordinatorlayout)
    CoordinatorLayout coordinatorLayout;

    @Bind(android.R.id.list)
    ListView listView;

    @Bind(android.R.id.empty)
    Button emptyButton;

    @Bind(android.R.id.progress)
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        listView.setEmptyView(emptyButton);
        listView.setOnItemClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        showAccounts();
    }

    private void queryPassword(final Account account) {

        new MaterialDialog.Builder(this)
                .title(R.string.settings_text_password)
                .content(account.name)
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                .input(R.string.settings_text_password_hint, 0, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        String password = input.toString();
                        PreferenceManager.getDefaultSharedPreferences(LoginActivity.this).edit().putString(Settings.PREF_ACCOUNT_PASSWORD, password).apply();

                        useAccount(account);
                    }
                })
                .negativeText(android.R.string.cancel)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {

                        PreferenceManager.getDefaultSharedPreferences(LoginActivity.this).edit().remove(Settings.PREF_ACCOUNT_PASSWORD).apply();
                    }
                })
                .show();

    }

    /*
    *    starts owncloud app
    *    - if not found open in playstore
    *    - if playstore not found open in browser
    */
    public void openOwncloud(View view) {
        String packageName = "com.owncloud.android";

        PackageManager manager = getPackageManager();
        try {
            Intent i = manager.getLaunchIntentForPackage(packageName);
            if (i == null) {
                throw new PackageManager.NameNotFoundException();
            }
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity(i);
        } catch (PackageManager.NameNotFoundException e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
            }
        }
    }

    private void checkStoredAccount() {
        String storedAccountName = PreferenceManager.getDefaultSharedPreferences(this).getString(Settings.PREF_ACCOUNT_NAME, null);

        Account storedAccount = null;
        if (storedAccountName != null) {
            for (Account account : AccountManager.get(this).getAccountsByType(getString(R.string.account_type))) {
                if (account.name.equals(storedAccountName)) {
                    storedAccount = account;
                    break;
                }
            }
            useAccount(storedAccount);
        } else {
            showAccounts();
        }
    }

    private void showAccounts() {

        Account[] accounts = AccountManager.get(this).getAccountsByType(getString(R.string.account_type));

        List<String> accountNames = new ArrayList<>();

        for (Account account: accounts){
            accountNames.add(account.name);
        }

        if (accounts.length == 0) {
            emptyButton.animate().alpha(1);
            listView.animate().alpha(0);
        } else {
            listView.animate().alpha(1);
            listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, accountNames));
        }
    }

    private void useAccount(Account selectedAccount) {
        if (PreferenceManager.getDefaultSharedPreferences(this).contains(Settings.PREF_ACCOUNT_PASSWORD)) {
            new CheckAccountAsyncTask().execute(selectedAccount);
        } else {
            queryPassword(selectedAccount);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String accountName = listView.getAdapter().getItem(position).toString();
        PreferenceManager.getDefaultSharedPreferences(LoginActivity.this).edit().putString(Settings.PREF_ACCOUNT_NAME, accountName).apply();
        PreferenceManager.getDefaultSharedPreferences(LoginActivity.this).edit().remove(Settings.PREF_ACCOUNT_PASSWORD).apply();
        checkStoredAccount();
    }

    private class CheckAccountAsyncTask extends AsyncTask<Account, Void, Account> {
        protected void onPreExecute() {
            // Runs on the UI thread before doInBackground
            // Good for toggling visibility of a progress indicator
            progressBar.animate().alpha(1);
            listView.animate().alpha(0);
        }

        @Override
        protected Account doInBackground(Account... selectedAccounts) {

            Account selectedAccount = selectedAccounts[0];
            Uri baseUrl = Settings.getAccountURL(selectedAccount.name);
            String username = Settings.getAccountUsername(selectedAccount.name);
            String password = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this).getString(Settings.PREF_ACCOUNT_PASSWORD, null);
            if (password == null) {
                return null;
            }

            OwnCloudClient client = OwnCloudClientFactory.createOwnCloudClient(baseUrl, LoginActivity.this, false);

            client.setCredentials(OwnCloudCredentialsFactory.newBasicCredentials(username, password));

            if (!Settings.checkRemoteAccess(client)) {
                return null;
            }

            if (!Settings.checkRemoteAccess(client)) {
                return null;
            }

            return selectedAccount;
        }

        @Override
        protected void onPostExecute(Account result) {

            if (result != null) {
                String accountName = result.name;
                Snackbar.make(coordinatorLayout, accountName, Snackbar.LENGTH_LONG).show();

                startActivity(new Intent(LoginActivity.this, NoteListActivity.class));
                finish();

            } else {
                PreferenceManager.getDefaultSharedPreferences(LoginActivity.this).edit().remove(Settings.PREF_ACCOUNT_PASSWORD).apply();
                Snackbar.make(coordinatorLayout, R.string.toast_check_username_password, Snackbar.LENGTH_LONG).show();
                showAccounts();
            }
            progressBar.animate().alpha(0);
        }
    }
}
