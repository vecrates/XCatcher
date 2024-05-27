package me.vecrates.xcatcher;

import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import me.vecrates.xcatcher.core.LogcatReader;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Catcher.init(e -> {
            runOnUiThread(() -> Toast.makeText(this, "发生崩溃", Toast.LENGTH_SHORT).show());
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            return false;
        });

        findViewById(R.id.tv_java_crash).setOnClickListener(v -> {
            View view = null;
            view.getLeft();
        });

        findViewById(R.id.tv_native_crash).setOnClickListener(v -> {
            new Thread(Catcher::triggerNativeCash).start();
        });

        findViewById(R.id.tv_read_logcat).setOnClickListener(v -> {
            new Thread(LogcatReader::readLogcat).start();
        });

    }

}
