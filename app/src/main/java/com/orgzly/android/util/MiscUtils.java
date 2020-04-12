
package com.orgzly.android.util;


import android.net.Uri;
import com.google.android.material.textfield.TextInputLayout;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.TextView;

import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPDataValidationException;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPMarker;
import org.bouncycastle.openpgp.PGPPBEEncryptedData;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.bc.BcPGPObjectFactory;
import org.bouncycastle.openpgp.operator.bc.BcPBEDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.bc.BcPBEKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.util.io.Streams;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

public class MiscUtils {
    /**
     * Counts lines in a given string. Empty string counts as one line.
     *
     * @param str string
     * @return number of lines in a string
     */
    public static int lineCount(String str) {
        if (str == null) {
            return 0;
        }

        int lines = 1;
        int pos = 0;

        while ((pos = str.indexOf("\n", pos)) != -1) {
            lines++;
            pos++;
        }

        return lines;
    }

    public static String readStringFromFile(File file) throws IOException {
        StringBuffer fileData = new StringBuffer();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        char[] buf = new char[1024];
        int numRead;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
        }
        reader.close();
        return fileData.toString();
    }

    public static void writeStringToFile(String str, File file) throws FileNotFoundException {
        try (PrintWriter out = new PrintWriter(file)) {
            out.write(str);
        }
    }

    /**
     * Compares content of two text files.
     * @return null if files match, difference if they don't
     */
    public static String compareFiles(File file1, File file2) throws IOException {
        String result;

        BufferedReader reader1 = new BufferedReader(new FileReader(file1));
        BufferedReader reader2 = new BufferedReader(new FileReader(file2));

        while (true) {
            String line1 = reader1.readLine();
            String line2 = reader2.readLine();

            if (line1 == null && line2 == null) {
                result = null;
                break;

            } else if (line1 == null && line2 != null) {
                result = "file2 has more lines then file1";
                break;

            } else if (line1 != null && line2 == null) {
                result = "file1 has more lines then file2";
                break;

            } else if (! line1.equals(line2)) {
                result = "Files differ:\n" + line1 + "\n" + line2 + "\n";
                break;
            }
        }

        reader1.close();
        reader2.close();

        return result;
    }

    public static String readStream(InputStream in) throws IOException {
        final int bufferSize = 1024;
        final char[] buffer = new char[bufferSize];
        final StringBuilder out = new StringBuilder();
        Reader reader = new InputStreamReader(in, "UTF-8");
        int len;
        while ((len = reader.read(buffer, 0, buffer.length)) != -1) {
            out.append(buffer, 0, len);
        }
        return out.toString();
    }

    public static void writeStreamToFile(InputStream in, File file) throws IOException {
        OutputStream out = new FileOutputStream(file);
        try {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer, 0, buffer.length)) != -1) {
                out.write(buffer, 0, len);
            }
        } finally {
            out.close();
        }
    }

    public static void writeFileToStream(File file, OutputStream out) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer, 0, buffer.length)) != -1) {
                out.write(buffer, 0, len);
            }

        } finally {
            in.close();
        }
    }

    /**
     * Surround string with curly quotes (a.k.a. smart quotes).
     */
    public static String quotedString(String str) {
        return "“" + str + "”";
    }

    public static long sha1(String s) {
        long l = 0L;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(s.getBytes("UTF-8"));
            byte[] digest = md.digest();

            /* First 64 bits only */
            for (int i = 0; i < 8; ++i) {
                l <<= 8;
                l |= (0xff & digest[i]);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return l;
    }

    public static void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    /**
     * Encrypt data with AES-256 symmetric encryption and wrap it in a PGP packet.
     *
     * This code is based on the encryption code in OpenKeychain. It uses the bouncycastle cryptography provider
     * as an extra library since some features were removed in recent Android APIs.
     * See https://android-developers.googleblog.com/2018/03/cryptography-changes-in-android-p.html for details
     *
     * @param src The input stream to be encrypted.
     * @param dst The output stream to receive the PGP data
     * @param originalFilename The filename to store as PGP metadata.
     * @param passphrase The passphrase to derive the encryption key from.
     * @throws IOException
     * @throws PGPException
     */
    public static void pgpEncrypt(
            InputStream src,
            OutputStream dst,
            String originalFilename,
            String passphrase) throws IOException, PGPException {
        int symmetricEncryptionAlgorithm = SymmetricKeyAlgorithmTags.AES_256;

        // a PGP generator must be created and a passphrase to key generation method must be set
        BcPGPDataEncryptorBuilder encryptorBuilder = new BcPGPDataEncryptorBuilder(symmetricEncryptionAlgorithm)
                .setSecureRandom(new SecureRandom())
                .setWithIntegrityPacket(true);
        PGPEncryptedDataGenerator encGen =
                new PGPEncryptedDataGenerator(encryptorBuilder);
        BcPBEKeyEncryptionMethodGenerator symmetricEncryptionGenerator =
                new BcPBEKeyEncryptionMethodGenerator(passphrase.toCharArray());
        encGen.addMethod(symmetricEncryptionGenerator);

        OutputStream encryptionOut = encGen.open(dst, new byte[1 << 16]);
        // since books contain text, we also use compression
        PGPCompressedDataGenerator compressedGen = new PGPCompressedDataGenerator(PGPCompressedDataGenerator.ZIP);
        BCPGOutputStream bcpgOut = new BCPGOutputStream(compressedGen.open(encryptionOut));
        PGPLiteralDataGenerator literalGen = new PGPLiteralDataGenerator();
        char literalDataFormatTag = PGPLiteralData.BINARY;
        OutputStream pOut = literalGen.open(bcpgOut, literalDataFormatTag,
                        originalFilename, new Date(), new byte[1 << 16]);
        Streams.pipeAll(src, pOut);

        literalGen.close();
        compressedGen.close();
        encryptionOut.close();
        // close to write PGP footer
        //encGen.close();
        dst.close();
    }

    /**
     * Skip PGP marker packets while iterating PGP objects
     */
    public static Object nextObjectSkipMarker(BcPGPObjectFactory fact) throws IOException{
        Object o = fact.nextObject();
        while (o instanceof PGPMarker) {
            o = fact.nextObject();
        }
        return o;
    }

    /**
     * Decrypt a symmetrically encrypted PGP packet.
     *
     * This code is based on the encryption code in OpenKeychain. It uses the bouncycastle cryptography provider
     * as an extra library since some features were removed in recent Android APIs.
     * See https://android-developers.googleblog.com/2018/03/cryptography-changes-in-android-p.html for details
     *
     * @param src The symetrycally encrypted PGP packet data.
     * @param dst Output plaintext stream.
     * @param passphrase The passphrase to derive the decryption key from.
     * @throws IOException
     * @throws PGPException
     */
    public static void pgpDecrypt(InputStream src, OutputStream dst, String passphrase) throws IOException, PGPException {
        InputStream pgpIn = PGPUtil.getDecoderStream(src);

        if (pgpIn instanceof ArmoredInputStream) {
            throw new PGPException("ASCII Armored PGP data is not supported.");
        }

        BcPGPObjectFactory pgpF = new BcPGPObjectFactory(pgpIn);

        Object obj = nextObjectSkipMarker(pgpF);
        if (!(obj instanceof PGPEncryptedDataList)) {
            throw new PGPException("Unencrypted PGP data is not supported.");
        }
        PGPEncryptedDataList enc = (PGPEncryptedDataList) obj;

        // if there are more than one symmetric encrypted packet, get only the first
        PGPPBEEncryptedData encryptedDataSymmetric = null;
        Iterator<?> it = enc.getEncryptedDataObjects();
        while (it.hasNext()) {
            Object packetObj = it.next();
            if (!(packetObj instanceof PGPPBEEncryptedData)) {
                continue;
            }
            encryptedDataSymmetric = (PGPPBEEncryptedData) packetObj;
            break;
        }

        if (encryptedDataSymmetric == null) {
            throw new PGPException("Asymmetrically encrypted PGP data is not supported.");
        }

        // decrypt
        InputStream cleartextStream;
        BcPGPDigestCalculatorProvider digestCalcProvider = new BcPGPDigestCalculatorProvider();
        BcPBEDataDecryptorFactory decryptorFactory = new BcPBEDataDecryptorFactory(passphrase.toCharArray(), digestCalcProvider);
        try {
            cleartextStream = encryptedDataSymmetric.getDataStream(decryptorFactory);
        } catch (PGPDataValidationException ex) {
            throw new PGPException("Failed to decrypt data. Wrong password?");
        }

        BcPGPObjectFactory plainFact = new BcPGPObjectFactory(cleartextStream);
        Object dataChunk = nextObjectSkipMarker(plainFact);

        // if we're trying to read a file generated by someone other than us
        // the data might not be compressed, so we check the return type from
        // the factory and behave accordingly.
        if (dataChunk instanceof PGPCompressedData) {
            PGPCompressedData compressedData = (PGPCompressedData) dataChunk;
            plainFact = new BcPGPObjectFactory(compressedData.getDataStream());
            dataChunk = nextObjectSkipMarker(plainFact);
        }

        if (!(dataChunk instanceof PGPLiteralData)) {
            throw new UnsupportedOperationException("Encountered an error reading input data!");
        }
        PGPLiteralData literalData = (PGPLiteralData) dataChunk;
        InputStream dataIn = literalData.getInputStream();

        // write out
        Streams.pipeAll(dataIn, dst);

        // require integrity check (must be done after piping out the data)
        if (!encryptedDataSymmetric.isIntegrityProtected()) {
            throw new PGPException("Missing the Modification Detection Code (MDC) packet!");
        }
        if (!encryptedDataSymmetric.verify()) {
            throw new PGPException("Integrity check error!");
        }
    }

    /**
     * Add a .gpg file extension to a file name if not already present.
     */
    public static String ensureGpgExtensionFileName(String fileName) {
        if ((fileName.length() > 4)
                && fileName.substring(fileName.length() - 4).equals(".gpg")) {
            return fileName;
        } else {
            return "$fileName.gpg";
        }
    }

    /**
     * Remove a .gpg file extension from a file name if present.
     */
    public static String ensureNoGpgExtensionFileName(String fileName) {
        if ((fileName.length() > 4) && fileName.substring(fileName.length() - 4).equals(".gpg")) {
            return fileName.substring(0, fileName.length() - 4);
        } else {
            return fileName;
        }
    }

    /**
     * Clear {@link TextInputLayout} error after its text has been modified.
     */
    public static void clearErrorOnTextChange(final TextView tv, final TextInputLayout til) {
        tv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                til.setError(null);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private static boolean isAllowedInUrlEncoded(char c, String allow) {
        return (c >= 'A' && c <= 'Z')
               || (c >= 'a' && c <= 'z')
               || (c >= '0' && c <= '9')
               || "_-!.~'()*".indexOf(c) != -1
               || (allow != null && allow.indexOf(c) != -1);
    }

    private static boolean isHexChar(char c) {
        return (c >= 'A' && c <= 'F') ||
               (c >= 'a' && c <= 'f') ||
               (c >= '0' && c <= '9');
    }

    static boolean uriPathNeedsEncoding(String str) {
        int percentHexCharsLeft = 0;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (! isAllowedInUrlEncoded(c, "/%")) {
                return true;
            }

            if (percentHexCharsLeft-- > 0) {
                // This char must be digit
                if (! isHexChar(c)) {
                    return true;
                }
            }

            if (c == '%') {
                percentHexCharsLeft = 2;
                // Next two characters must be hex now
            }
        }

        return percentHexCharsLeft > 0;

    }

    public static String encodeUri(String str) {
        /* Scheme part. */
        int i = str.indexOf(":");

        if (i != -1) {
            String rest = str.substring(i + 1);

            if (uriPathNeedsEncoding(rest)) {
                return str.substring(0, i + 1) + Uri.encode(str.substring(i + 1), "/");
            }
        }

        return str;
    }

    public static Spanned fromHtml(String html){
        Spanned result;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(html);
        }
        return result;
    }

    public static ArrayList<String> toArrayList(Map<String, String> map) {
        ArrayList<String> list = new ArrayList<>();

        for (String key: map.keySet()) {
            String value = map.get(key);

            list.add(key);
            list.add(value);
        }

        return list;
    }
}
