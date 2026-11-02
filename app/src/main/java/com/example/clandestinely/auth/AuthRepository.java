package com.example.clandestinely.auth;

import android.app.Activity;
import android.content.Context;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.example.clandestinely.R;
import com.example.clandestinely.data.model.UserProfile;
import com.example.clandestinely.data.repository.UserRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * Firebase Auth for CuacaUM (FR6.1): sign in with Google, or continue as a guest.
 *
 * Both sign-in methods also make sure the user has a document in the "users" collection,
 * and return their UserProfile.
 *
 * <pre>
 * authRepository.signInWithGoogle(this)
 *     .addOnSuccessListener(profile -> goToHome())
 *     .addOnFailureListener(e -> showError(AuthRepository.friendlyMessage(e)));
 * </pre>
 */
public class AuthRepository {

    /** A sign-in problem with a message that's safe to show to the user. */
    public static class AuthException extends Exception {
        public AuthException(String message) {
            super(message);
        }

        public AuthException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final FirebaseAuth auth;
    private final UserRepository users;

    public AuthRepository() {
        this(FirebaseAuth.getInstance(), new UserRepository());
    }

    public AuthRepository(FirebaseAuth auth, UserRepository users) {
        this.auth = auth;
        this.users = users;
    }

    /** The signed-in user, or null if nobody is signed in. */
    @Nullable
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isSignedIn() {
        return auth.getCurrentUser() != null;
    }

    /** "Continue as guest": Firebase anonymous sign-in. */
    public Task<UserProfile> signInAsGuest() {
        return auth.signInAnonymously().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw new AuthException("Couldn't continue as guest. Check your internet connection.",
                        task.getException());
            }
            return users.ensureProfile(task.getResult().getUser());
        });
    }

    /**
     * "Sign in with Google": shows Android's account picker, then signs into Firebase.
     * Needs the Google provider turned on in the Firebase console, and this computer's SHA-1
     * fingerprint added to the Android app in Project settings.
     */
    public Task<UserProfile> signInWithGoogle(@NonNull Activity activity) {
        return pickGoogleAccount(activity)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return auth.signInWithCredential(task.getResult());
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();
                        if (e instanceof AuthException) throw e;
                        throw new AuthException("Google sign-in failed. Please try again.", e);
                    }
                    return users.ensureProfile(task.getResult().getUser());
                });
    }

    /**
     * Signs out of Firebase and forgets the chosen Google account on this phone.
     * Note: a guest who signs out can't get the same guest account back.
     */
    public void signOut(@NonNull Context context) {
        auth.signOut();
        CredentialManager.create(context).clearCredentialStateAsync(
                new ClearCredentialStateRequest(),
                new CancellationSignal(),
                ContextCompat.getMainExecutor(context),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(Void result) {
                        // Nothing else to do.
                    }

                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        // Firebase is already signed out; this only affects the account picker.
                    }
                });
    }

    /** A message that's safe to show the user for any sign-in failure. */
    public static String friendlyMessage(@Nullable Exception e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof AuthException) return t.getMessage();
            t = t.getCause();
        }
        return "Sign-in failed. Check your internet connection and try again.";
    }

    /** Shows the Google account picker and turns the chosen account into a Firebase credential. */
    private Task<AuthCredential> pickGoogleAccount(Activity activity) {
        TaskCompletionSource<AuthCredential> result = new TaskCompletionSource<>();

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // show every Google account on the phone
                .setServerClientId(activity.getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        CredentialManager.create(activity).getCredentialAsync(
                activity,
                request,
                new CancellationSignal(),
                ContextCompat.getMainExecutor(activity),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        Credential credential = response.getCredential();
                        if (credential instanceof CustomCredential
                                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                            try {
                                GoogleIdTokenCredential google = GoogleIdTokenCredential.createFrom(credential.getData());
                                result.setResult(GoogleAuthProvider.getCredential(google.getIdToken(), null));
                            } catch (Exception e) {
                                result.setException(new AuthException("Couldn't read the Google account. Please try again.", e));
                            }
                        } else {
                            result.setException(new AuthException("Please pick a Google account."));
                        }
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        if (e instanceof GetCredentialCancellationException) {
                            result.setException(new AuthException("Sign-in was cancelled.", e));
                        } else if (e instanceof NoCredentialException) {
                            result.setException(new AuthException(
                                    "No Google account found on this phone. Add one in Settings > Accounts, or continue as guest.", e));
                        } else {
                            result.setException(new AuthException("Google sign-in failed. Please try again.", e));
                        }
                    }
                });

        return result.getTask();
    }
}
