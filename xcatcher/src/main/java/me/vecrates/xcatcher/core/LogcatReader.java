package me.vecrates.xcatcher.core;

import android.annotation.SuppressLint;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class LogcatReader {

    private static final String TAG = LogcatReader.class.getSimpleName();

    @NonNull
    public static String readLogcat(int lineCount) {
        try {
            @SuppressLint("DefaultLocale")
            String cmd = String.format("logcat -t %d --pid=%d", lineCount, Process.myPid());
            java.lang.Process exec = Runtime.getRuntime().exec(cmd);
            InputStream is = exec.getInputStream();
            StringBuilder sb = new StringBuilder();
            byte[] buf = new byte[1024];
            while (true) {
                int len = is.read(buf);
                if (len < 0) {
                    break;
                }
                char[] chars = getChars(buf, len);
                sb.append(chars);
            }
            for (int i = sb.length() - 1; i >= 0; i--) {
                if (sb.charAt(i) == '\u0000') {
                    sb.setCharAt(i, ' ');
                }
            }
            return sb.toString();
        } catch (IOException | Error e) {
            Log.e(TAG, "readLogcat: ", e);
        }
        return "";
    }

    private static char[] getChars(byte[] bytes, int len) {
        ByteBuffer bb = ByteBuffer.allocate(len);
        bb.put(bytes, 0, len);
        bb.flip();

        Charset cs = StandardCharsets.UTF_8;
        CharBuffer cb = cs.decode(bb);
        return cb.array();
    }

}
