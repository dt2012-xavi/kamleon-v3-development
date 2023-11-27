package com.dynatech2012.kamleonuserapp.repositories;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.dynatech2012.kamleonuserapp.R;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * <p><font color="teal">
 * Edu: <br/>
 * Http request to notify firebase that an invitation has been accepted or denied.
 * This will fire a cloud function in firebase
 */
public class CloudFunctions {

    public static final String TAG = CloudFunctions.class.getSimpleName();

    public LiveData<Response<Boolean>> responseToInvitation(Context context, String invitationId, boolean accept) {
        MutableLiveData<Response<Boolean>> observable = new MutableLiveData<>();

        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {

            String url = "";
            if(!accept) {
                url = "https://europe-west1-testkamenv.cloudfunctions.net/declineInvitation?invitationID=" + invitationId;
            } else {
                url = "https://europe-west1-testkamenv.cloudfunctions.net/acceptInvitation?invitationID=" + invitationId;
            }
            RequestBody formBody = new FormBody.Builder().build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build();
            //client
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                    if (response.isSuccessful()) {
                        observable.postValue(new Response.Success<>(true));
                    } else {
                        observable.postValue(new Response.Failure(new Exception(context.getString(R.string.error_login))));
                    }
                }

                @Override
                public void onFailure(@NotNull okhttp3.Call call, @NotNull IOException e) {
                    observable.postValue(new Response.Failure(e));
                }

            });
        });
        return observable;
    }
}
