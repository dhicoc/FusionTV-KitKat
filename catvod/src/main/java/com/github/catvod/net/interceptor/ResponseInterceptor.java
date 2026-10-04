package com.github.catvod.net.interceptor;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.catvod.utils.Util;
import com.google.common.net.HttpHeaders;

import java.io.IOException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.Okio;

public class ResponseInterceptor implements Interceptor {

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        String location = response.header(HttpHeaders.LOCATION);
        String encoding = response.header(HttpHeaders.CONTENT_ENCODING);
        if (response.isRedirect() && location != null) checkAuth(response, location);
        if (response.body() != null && "deflate".equals(encoding)) return deflate(response);
        return response;
    }

    private void checkAuth(Response response, String location) {
        try {
            Uri uri = Uri.parse(location);
            if (uri.getUserInfo() == null) return;
            response.header(HttpHeaders.AUTHORIZATION, Util.basic(uri.getUserInfo()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Response deflate(Response response) {
        InflaterInputStream is = new InflaterInputStream(response.body().byteStream(), new Inflater(true));
        return response.newBuilder().headers(response.headers()).body(new ResponseBody() {
            @Nullable
            @Override
            public MediaType contentType() {
                return response.body().contentType();
            }

            @Override
            public long contentLength() {
                return response.body().contentLength();
            }

            @NonNull
            @Override
            public BufferedSource source() {
                return Okio.buffer(Okio.source(is));
            }
        }).build();
    }
}