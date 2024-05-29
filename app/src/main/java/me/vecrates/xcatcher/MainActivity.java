package me.vecrates.xcatcher;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import me.vecrates.xcatcher.core.CrashHandle;
import me.vecrates.xcatcher.core.LogcatReader;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InitConfig config = new InitConfig.Builder()
                .setCatchNative(true)
                .setReadLogcatLines(10)
                .setCollector((e, log) -> {
                    runOnUiThread(() -> Toast.makeText(this, "发生崩溃", Toast.LENGTH_SHORT).show());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    //Log.e(TAG, "logcat=\n" + log);
                    return CrashHandle.EXIT;
                })
                .build();

        XCatcher.init(config);

        findViewById(R.id.tv_java_crash).setOnClickListener(v -> {
            new Thread(() -> {
                View view = null;
                view.getLeft();
            }).start();
        });

        findViewById(R.id.tv_native_crash).setOnClickListener(v -> {
            new Thread(XCatcher::triggerNativeCash).start();
            //XCatcher.triggerNativeCash();
        });

        findViewById(R.id.tv_read_logcat).setOnClickListener(v -> {
            new Thread(() -> LogcatReader.readLogcat(50)).start();
        });

    }

}
