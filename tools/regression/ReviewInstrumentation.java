package local.fusion.review;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import tv.danmaku.ijk.media.player.ui.IjkVideoView;

/** Runs against the installed Debug APK; does not write the app's config database. */
public final class ReviewInstrumentation extends Instrumentation {
    private ClassLoader loader;
    private String mappedNetworkClass;
    private String results = "";
    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        mappedNetworkClass = arguments == null ? null : arguments.getString("networkClass");
        start();
    }
    @Override public void onStart() {
        loader = getTargetContext().getClassLoader();
        Bundle reply = new Bundle();
        try {
            if (mappedNetworkClass == null) { checkProgress(); checkSearch(); }
            checkTls();
            reply.putString("stream", "PASS\n" + results);
            finish(Activity.RESULT_OK, reply);
        } catch (Throwable error) {
            reply.putString("stream", "FAIL\n" + results + android.util.Log.getStackTraceString(error));
            finish(Activity.RESULT_CANCELED, reply);
        }
    }
    private Class<?> type(String name) throws Exception { return Class.forName(name, true, loader); }
    private static Field field(Class<?> type, String name) throws Exception {
        Field result = type.getDeclaredField(name); result.setAccessible(true); return result;
    }
    private static Method method(Class<?> type, String name, Class<?>... args) throws Exception {
        Method result = type.getDeclaredMethod(name, args); result.setAccessible(true); return result;
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private void checkProgress() throws Exception {
        final Throwable[] failure = new Throwable[1];
        runOnMainSync(new Runnable() { @Override public void run() {
            try {
                Class<?> players = type("com.fongmi.android.tv.player.Players");
                Constructor<?> ctor = players.getDeclaredConstructor(Activity.class); ctor.setAccessible(true);
                Object player = ctor.newInstance(new Object[]{null});
                FakeIjk ijk = new FakeIjk(getTargetContext());
                Class<?> playerView = type("androidx.media3.ui.PlayerView");
                method(players, "init", playerView, IjkVideoView.class).invoke(player, null, ijk);
                require(!(Boolean) method(players, "isRelease").invoke(player), "Initialized IJK reported released");
                field(players, "url").set(player, "http://127.0.0.1/controlled-episode");
                Class<?> seekClass = type("com.fongmi.android.tv.ui.custom.CustomSeekView");
                Object seek = seekClass.getConstructor(Context.class).newInstance(getTargetContext());
                method(seekClass, "setListener", players).invoke(seek, player);
                try {
                    method(seekClass, "refresh").invoke(seek);
                    TextView pos = (TextView) field(seekClass, "positionView").get(seek);
                    TextView duration = (TextView) field(seekClass, "durationView").get(seek);
                    require("00:45".contentEquals(pos.getText()), "Position did not update: " + pos.getText());
                    require("02:00".contentEquals(duration.getText()), "Duration did not update: " + duration.getText());
                    require(field(seekClass, "currentBuffered").getLong(seek) == 60000L, "Buffer did not update");
                } finally {
                    ((android.view.View) seek).removeCallbacks((Runnable) field(seekClass, "refresh").get(seek));
                    method(players, "releaseIjk").invoke(player);
                }
                require((Boolean) method(players, "isRelease").invoke(player), "Released IJK reported active");
            } catch (Throwable e) { failure[0] = e; }
        }});
        if (failure[0] != null) throw new Exception("Progress integration", failure[0]);
        results += "IJK lifecycle + actual seek widget position/duration/buffer: PASS\n";
    }
    private void checkSearch() throws Exception {
        final ServerSocket server = new ServerSocket(0);
        server.setSoTimeout(25000);
        final AtomicInteger requests = new AtomicInteger();
        final List<String> paths = java.util.Collections.synchronizedList(new ArrayList<String>());
        Thread http = new Thread(new Runnable() { @Override public void run() {
            try {
                while (requests.get() < 11) {
                    Socket socket = server.accept(); socket.setSoTimeout(5000);
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                        String request = in.readLine();
                        String line; while ((line = in.readLine()) != null && line.length() != 0) { }
                        paths.add(request); requests.incrementAndGet();
                        byte[] json = "{\"code\":1,\"list\":[]}".getBytes("UTF-8");
                        OutputStream out = socket.getOutputStream();
                        out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: " + json.length + "\r\nConnection: close\r\n\r\n").getBytes("UTF-8"));
                        out.write(json); out.flush();
                    } finally { socket.close(); }
                }
            } catch (Exception ignored) { }
        }}, "controlled-cms");
        http.start();
        Class<?> configClass = type("com.fongmi.android.tv.api.config.VodConfig");
        Object config = method(configClass, "get").invoke(null);
        Field sitesField = field(configClass, "sites"); Object originalSites = sitesField.get(config);
        final Object[] activityHolder = new Object[1];
        final Throwable[] uiFailure = new Throwable[1];
        ExecutorService executor = null;
        try {
            Class<?> siteClass = type("com.fongmi.android.tv.bean.Site");
            List<Object> sites = new ArrayList<Object>();
            for (int i = 1; i <= 11; i++) {
                Object site = siteClass.newInstance();
                field(siteClass, "key").set(site, "fixture" + i);
                field(siteClass, "name").set(site, "fixture" + i);
                field(siteClass, "api").set(site, "http://127.0.0.1:" + server.getLocalPort() + "/s" + i);
                field(siteClass, "type").set(site, Integer.valueOf(1));
                sites.add(site);
            }
            sitesField.set(config, sites);
            runOnMainSync(new Runnable() { @Override public void run() {
                try {
                    Object activity = type("com.fongmi.android.tv.ui.activity.VideoActivity").newInstance();
                    activityHolder[0] = activity;
                    field(activity.getClass(), "autoMode").setBoolean(activity, true);
                    field(activity.getClass(), "mQuickAdapter").set(activity, type("androidx.leanback.widget.ArrayObjectAdapter").newInstance());
                    field(activity.getClass(), "mViewModel").set(activity, type("com.fongmi.android.tv.model.SiteViewModel").newInstance());
                    method(activity.getClass(), "startSearch", String.class).invoke(activity, "controlled-title");
                } catch (Throwable e) { uiFailure[0] = e; }
            }});
            if (uiFailure[0] != null) throw new Exception("Search UI setup", uiFailure[0]);
            Object activity = activityHolder[0];
            executor = (ExecutorService) field(activity.getClass(), "mExecutor").get(activity);
            executor.shutdown();
            require(executor.awaitTermination(25, TimeUnit.SECONDS), "Search queue did not finish");
            require(requests.get() == 11, "Expected 11 actual CMS requests, got " + requests.get());
            boolean eleventh = false; for (String path : paths) if (path.contains("/s11?")) eleventh = true;
            require(eleventh, "Eleventh source was never requested: " + paths);
            results += "Automatic search actual requests to all 11 sources: PASS\n";
        } finally {
            if (executor != null) executor.shutdownNow();
            sitesField.set(config, originalSites); server.close(); http.join(1000);
        }
    }
    private void checkTls() throws Exception {
        X509TrustManager trust = new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            public void checkClientTrusted(X509Certificate[] chain, String auth) { }
            public void checkServerTrusted(X509Certificate[] chain, String auth) { }
        };
        SSLSocketFactory factory;
        if (mappedNetworkClass == null) {
            factory = (SSLSocketFactory) type("xiao.bu.tv.LegacyTlsSocket$Factory").getConstructor(X509TrustManager.class).newInstance(trust);
        } else {
            Object client = null;
            for (Method candidate : type(mappedNetworkClass).getDeclaredMethods()) {
                if (java.lang.reflect.Modifier.isStatic(candidate.getModifiers()) && candidate.getParameterTypes().length == 0
                        && "okhttp3.OkHttpClient".equals(candidate.getReturnType().getName())) {
                    candidate.setAccessible(true); client = candidate.invoke(null); break;
                }
            }
            require(client != null, "Mapped app HTTP client was not found");
            factory = (SSLSocketFactory) client.getClass().getMethod("sslSocketFactory").invoke(client);
        }
        Socket tcp = new Socket(); tcp.connect(new java.net.InetSocketAddress("example.com", 443), 10000); tcp.setSoTimeout(10000);
        SSLSocket socket = (SSLSocket) factory.createSocket(tcp, "example.com", 443, true);
        try {
            socket.startHandshake();
            socket.getOutputStream().write("HEAD / HTTP/1.1\r\nHost: example.com\r\nConnection: close\r\n\r\n".getBytes("UTF-8"));
            String response = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8")).readLine();
            require(response != null && response.startsWith("HTTP/"), "Native TLS returned no HTTP response");
            results += "API19 native TLS handshake + real HTTP response: PASS\n";
        } finally { socket.close(); }
    }
    public static final class FakeIjk extends IjkVideoView {
        public FakeIjk(Context context) { super(context); }
        @Override public IjkVideoView render(int render) { return this; }
        @Override public int getDuration() { return 120000; }
        @Override public int getCurrentPosition() { return 45000; }
        @Override public long getBufferedPosition() { return 60000; }
        @Override public boolean isPlaying() { return false; }
    }
}
