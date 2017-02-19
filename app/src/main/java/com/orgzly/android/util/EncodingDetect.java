package com.orgzly.android.util;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class EncodingDetect {

    public enum Method {
        // ICU,              // Too big. Maybe strip it?
        // JCHARDET,         // Did not detect encoding for worg/org-blog-articles.org
        JUNIVERSALCHARDET
    }

    public static final Method USED_METHOD = Method.JUNIVERSALCHARDET;

    private InputStream fileInputStream;

    private boolean hasRan = false;
    private String detectedCharset;


    public static EncodingDetect getInstance(InputStream fileInputStream) {
        EncodingDetect detect = new EncodingDetect();

        detect.fileInputStream = fileInputStream;

        return detect;
    }

    public String getEncoding() {
        detect();
        return detectedCharset;
    }

    public boolean isDetected() {
        detect();
        return detectedCharset != null;
    }

    /**
     * Sets charset to detected value.
     */
    private void detect() {
        if (! hasRan) {
            switch (USED_METHOD) {
//                case ICU:
//                    icuDetect();
//                    break;

//                case JCHARDET:
//                    nsDetect();
//                    break;

                case JUNIVERSALCHARDET:
                    universalDetect();
                    break;
            }
        }

        hasRan = true;
    }

    /**
     * Detect charset using ICU.
     * International Components for Unicode (http://site.icu-project.org/)
     *
     * Source code available at
     * http://source.icu-project.org/repos/icu/icu4j/trunk/
     * (it's a SVN repo - it can be checked out)
     */
//    private void icuDetect() {
//        BufferedInputStream imp = null;
//
//        try {
//            imp = new BufferedInputStream(fileInputStream);
//
//            CharsetMatch match = new CharsetDetector().setText(imp).detect();
//
//            System.out.println(name + ": " + match.getLanguage() + " " + match.getName() + " " + match.getConfidence());
//
//            detectedCharset = match.getName();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//
//        } finally {
//            if (imp != null) {
//                try {
//                    imp.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }

    /**
     * Detect charset using Java port of Mozilla charset detector
     * (http://sourceforge.net/projects/jchardet/?source=typ_redirect)
     */
//    private void nsDetect() {
//        detectedCharset = null;
//
//        // Initialize the nsDetector() ;
//        nsDetector det = new nsDetector();
//
//        // Set an observer...
//        // The Notify() will be called when a matching charset is found.
//        det.Init(new nsICharsetDetectionObserver() {
//            public void Notify(String charset) {
//                System.out.println(name + ": " + charset);
//                detectedCharset = charset;
//            }
//        });
//
//
//        byte[] buf = new byte[1024];
//        int len;
//        boolean done = false;
//        boolean isAscii = true;
//        BufferedInputStream imp = null;
//
//        try {
//            imp = new BufferedInputStream(fileInputStream);
//
//            while ((len = imp.read(buf, 0, buf.length)) != -1) {
//
//                // Check if the stream is only ascii.
//                if (isAscii) {
//                    isAscii = det.isAscii(buf, len);
//                }
//
//                // DoIt if non-ascii and not done yet.
//                if (!isAscii && !done) {
//                    done = det.DoIt(buf, len, false);
//                }
//
//                if (detectedCharset != null) {
//                    return;
//                }
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//            return;
//
//        } finally {
//            if (imp != null) {
//                try {
//                    imp.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        det.DataEnd();
//
//        if (isAscii) {
//            System.out.println(name + ": ASCII");
//            detectedCharset = "ASCII";
//        }
//    }

    /**
     * juniversalchardet is a Java port of 'universalchardet',
     * that is the encoding detector library of Mozilla.
     * (https://code.google.com/p/juniversalchardet/)
     */
    private void universalDetect() {
        byte[] buf = new byte[4096];

        // (1)
        UniversalDetector detector = new UniversalDetector(null);

        // (2)
        int n;
        BufferedInputStream imp = null;
        try {
            imp = new BufferedInputStream(fileInputStream);

            while ((n = imp.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, n);
            }

        } catch (IOException e) {
            e.printStackTrace();
            return;

        } finally {
            if (imp != null) {
                try {
                    imp.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // (3)
        detector.dataEnd();

        // (4)
        String charset = detector.getDetectedCharset();

        if (charset != null) {
            // System.out.println(name + ": " + charset);
            detectedCharset = charset;
        }

        // (5)
        detector.reset();
    }
}
