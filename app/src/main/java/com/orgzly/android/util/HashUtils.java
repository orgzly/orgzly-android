package com.orgzly.android.util;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;

import android.support.v4.provider.DocumentFile;

import com.orgzly.BuildConfig;
import com.orgzly.android.util.LogUtils;

public enum HashUtils {

    MD5("MD5"),
    SHA1("SHA1"),
    SHA256("SHA-256"),
    SHA512("SHA-512");

    private String name;

    HashUtils(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String checksum(InputStream in) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(getName());
            byte[] block = new byte[4096];
            int length;
            while ((length = in.read(block)) > 0) {
                digest.update(block, 0, length);
            }
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public String checksum(File file) throws IOException {
        InputStream instream = new FileInputStream(file);
        return this.checksum(instream);
    }
}
