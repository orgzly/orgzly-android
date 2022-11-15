package com.orgzly.android.misc;

import android.util.Log;

import com.orgzly.android.BookFormat;
import com.orgzly.android.LipsumBookGenerator;
import com.orgzly.android.NotesOrgExporter;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.db.entity.Book;
import com.orgzly.android.util.MiscUtils;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public class BookParsingTest extends OrgzlyTest {
    private static final String TAG = BookParsingTest.class.getName();

    private boolean runResourcesDemandingTest() {
        if (true) return false;

        Log.d(TAG, "Deciding whether to run test for" +
                   " MODEL:" + android.os.Build.MODEL +
                   " FINGERPRINT:" + android.os.Build.FINGERPRINT +
                   " BRAND:" + android.os.Build.BRAND +
                   " DEVICE:" + android.os.Build.DEVICE +
                   " HARDWARE:" + android.os.Build.HARDWARE
        );

        switch (android.os.Build.MODEL) {
            /* Do not run test for these. */
            case "Full Android on x86 Emulator":
            case "Android SDK built for x86_64":
            case "GT-I5500":
            case "GT-I9300":
            case "HTC One V":
                return false;
        }

        return true;
    }

    @Test
    public void testSavingWithPriorities() {
        onBook("\nSample book used for tests\n\n\n* Note 1\n* [#B] Note 2\n* [#A] Note 3")
                .onLoad()
                .isContent("Sample book used for tests")
                .isWhenSaved("Sample book used for tests\n\n* Note 1\n* [#B] Note 2\n* [#A] Note 3\n");
    }

    @Test
    public void testNotIndented1() {
        onBook("* Note 1\nSCHEDULED: <2015-02-11 Wed +1d>").onLoad()
                .isWhenSaved("* Note 1\nSCHEDULED: <2015-02-11 Wed +1d>\n\n");
    }

    @Test
    public void testNotIndented2() {
        onBook("* Note 1\n:LOGBOOK:\n:END:").onLoad()
                .isWhenSaved("* Note 1\n:LOGBOOK:\n:END:\n\n");
    }

    @Test
    public void testIndented1() {
        onBook("* Note 1\n  SCHEDULED: <2015-02-11 Wed +1d>").onLoad()
                .isWhenSaved("* Note 1\n  SCHEDULED: <2015-02-11 Wed +1d>\n\n");
    }

    @Test
    public void testIndented2() {
        onBook("* Note 1\n  :LOGBOOK:\n  :END:").onLoad()
                .isWhenSaved("* Note 1\n  :LOGBOOK:\n  :END:\n\n");
    }

    @Test
    public void testEmptyProperties() {
        onBook("* Note 1\n  :PROPERTIES:\n  :END:").onLoad()
                .isWhenSaved("* Note 1\n");

    }

    @Test
    public void testProperties() {
        onBook("* Note 1\n" +
               "  :PROPERTIES:\n" +
               "  :name: value\n" +
               "  :END:").onLoad()
                .isWhenSaved("* Note 1\n" +
                             "  :PROPERTIES:\n" +
                             "  :name:     value\n" +
                             "  :END:\n\n");
    }

    @Test
    public void testPropertiesMultiple() {
        onBook("* Note 1\n" +
               "  :PROPERTIES:\n" +
               "  :name2: value2\n" +
               "  :name1: value1\n" +
               "  :END:").onLoad()
                .isWhenSaved("* Note 1\n" +
                             "  :PROPERTIES:\n" +
                             "  :name2:    value2\n" +
                             "  :name1:    value1\n" +
                             "  :END:\n\n");
    }

    @Test
    public void testPropertiesOrder() {
        onBook("* Note 1\n" +
               "  :PROPERTIES:\n" +
               "  :LAST_REPEAT: [2017-04-03 Mon 10:26]\n" +
               "  :STYLE:    habit\n" +
               "  :CREATED:  [2015-11-23 Mon 01:33]\n" +
               "  :END:\n" +
               "* Note 2\n" +
               "  :PROPERTIES:\n" +
               "  :CREATED:  [2015-11-23 Mon 01:33]\n" +
               "  :LAST_REPEAT: [2017-04-03 Mon 10:26]\n" +
               "  :STYLE:    habit\n" +
               "  :END:\n").onLoad()
                .isWhenSaved("* Note 1\n" +
                             "  :PROPERTIES:\n" +
                             "  :LAST_REPEAT: [2017-04-03 Mon 10:26]\n" +
                             "  :STYLE:    habit\n" +
                             "  :CREATED:  [2015-11-23 Mon 01:33]\n" +
                             "  :END:\n" +
                             "\n" +
                             "* Note 2\n" +
                             "  :PROPERTIES:\n" +
                             "  :CREATED:  [2015-11-23 Mon 01:33]\n" +
                             "  :LAST_REPEAT: [2017-04-03 Mon 10:26]\n" +
                             "  :STYLE:    habit\n" +
                             "  :END:\n" +
                             "\n");
    }

    @Test
    public void testPropertiesEmpty() {
        onBook("* Note 1\n" +
               "  :PROPERTIES:\n" +
               "  :END:").onLoad()
                .isWhenSaved("* Note 1\n");
    }

    @Test
    public void testParsingClockTimesOutsideLogbook() {
        onBook("* STARTED Test times\n" +
               "  CLOCK: [2016-10-27 Thu 18:40]--[2016-10-27 Thu 18:51] =>  0:11\n" +
               "  CLOCK: [2016-10-27 Thu 18:10]--[2016-10-27 Thu 18:25] =>  0:15\n" +
               "  CLOCK: [2016-10-27 Thu 17:50]--[2016-10-27 Thu 18:05] =>  0:15\n")
                .onLoad()
                .isWhenSaved("* STARTED Test times\n" +
                             "  CLOCK: [2016-10-27 Thu 18:40]--[2016-10-27 Thu 18:51] =>  0:11\n" +
                             "  CLOCK: [2016-10-27 Thu 18:10]--[2016-10-27 Thu 18:25] =>  0:15\n" +
                             "  CLOCK: [2016-10-27 Thu 17:50]--[2016-10-27 Thu 18:05] =>  0:15\n\n");
    }

    /*
     * Books in different languages, different sizes and formats ...
     */

    @Test
    public void testLoadingTextDv() {
        onBookFile("html/dv.org").onLoad().onGet();
    }

    @Test
    public void  testLoadingTextZh() {
        onBookFile("html/zh.org").onLoad().onGet();
    }

    @Test
    public void testLoadingHuge() {
        assumeTrue(runResourcesDemandingTest());
        onBookFile("org/large.org").onLoad();
    }

    /*
     * Files generated using http://generator.lorem-ipsum.info/ ...
     */

    @Test
    public void testLoadingArabicLipsum() {
        onBookFile("lipsum/Arabic-Lipsum.org").onLoad().onGet();
    }

    @Test
    public void testLoadingChineseLipsum() {
        onBookFile("lipsum/Chinese-Lipsum.org").onLoad().onGet();
    }

    @Test
    public void testLoadingGreekLipsum() {
        onBookFile("lipsum/Greek-Lipsum.org").onLoad().onGet();
    }

    @Test
    public void testLoadingHebrewLipsum() {
        onBookFile("lipsum/Hebrew-Lipsum.org").onLoad().onGet();
    }

    @Test
    public void testLoadingHindiLipsum() {
        onBookFile("lipsum/Hindi-Lipsum.org").onLoad().onGet();
    }

    @Test
    public void testLoadingJapaneseLipsum() {
        onBookFile("lipsum/Japanese-Lipsum.org").onLoad().onGet();
    }

    @Test
    public void testLoadingL33TspeakLipsum() {
        onBookFile("lipsum/L33tspeak-Lipsum.org").onLoad().onGet();
    }

    @Test
    public void testLoadingRussianLipsum() {
        onBookFile("lipsum/Russian-Lipsum.org").onLoad().onGet();
    }

    /*
     * Loading images as books ...
     */

//    @Test
//    public void testLoadingLargerJpg() {
//        if (runResourcesDemandingTest()) {
//            onBookFile("images/IMG_2932.org").onLoadFailed();
//        }
//    }

    @Test
    public void testLoadingSmallPng() {
        onBookFile("images/logo11w.org").onLoad();
    }

    /*
     * Generated lipsum ...
     */

    @Test
    public void testLoadingLatinLipsumHugeBookContentWithTitleTitleAndNoteContent() {
        assumeTrue(runResourcesDemandingTest());
        String data = LipsumBookGenerator.generateOrgString(1000000, new int[] { 1000000, 1000000 });
        onBook(data).onLoadFailed();
    }

    @Test
    public void testLoadingLatinLipsumHugeNoteTitle() {
        assumeTrue(runResourcesDemandingTest());
        String data = LipsumBookGenerator.generateOrgString(0, new int[] { 2000000, 0 });
        onBook(data).onLoadFailed();
    }

    @Test
    public void testLoadingLatinLipsumLargeNoteTitles() {
        assumeTrue(runResourcesDemandingTest());
        String data = LipsumBookGenerator.generateOrgString(0, new int[] { 1000000, 0, 1500000, 1 });
        onBook(data).onLoad();
    }

    @Test
    public void testLoadingLatinLipsumHugeNoteTitleWithContent() {
        assumeTrue(runResourcesDemandingTest());
        String data = LipsumBookGenerator.generateOrgString(0, new int[] { 1000000, 1000000 });
        onBook(data).onLoadFailed();
    }

    @Test
    public void testBookContentTooBig() {
        assumeTrue(runResourcesDemandingTest());
        String data = LipsumBookGenerator.generateOrgString(2000000, null);
        onBook(data).onLoadFailed();
    }

    public TestedBook onBookFile(String resourceName) {
        return new TestedBook(new TestedBookSourceFromResource(resourceName));
    }

    public TestedBook onBook(String content) {
        return new TestedBook(new TestedBookSourceFromContent(content));
    }

    class TestedBook {
        private final TestedBookSource bookSource;

        private Book book;

        public TestedBook(TestedBookSource bookSource) {
            this.bookSource = bookSource;
        }

        public TestedBook onLoad() {
            dataRepository.clearDatabase();

            File tmpFile = bookSource.getTmpFile();
            try {
                /* Load from file. */
                dataRepository.loadBookFromFile("Notebook", BookFormat.ORG, tmpFile);

            } catch (IOException e) {
                e.printStackTrace();
                fail(e.toString());

            } finally {
                tmpFile.delete();
            }

            book = dataRepository.getBooks().get(0).getBook();

            return this;
        }

        public TestedBook onGet() {
            try {
                book = dataRepository.getBooks().get(0).getBook();

            } catch (Exception e) {
                e.printStackTrace();
                fail(e.toString());
            }

            return this;
        }

        public TestedBook onGetFailed() {
            try {
                book = dataRepository.getBooks().get(0).getBook();
                fail("Book should fail to load");

            } catch (Exception e) {
                e.printStackTrace();
            }

            return this;
        }

        public TestedBook onLoadFailed() {
            dataRepository.clearDatabase();

            File tmpFile = bookSource.getTmpFile();
            try {
                /* Load from file. */
                dataRepository.loadBookFromFile("Notebook", BookFormat.ORG, tmpFile);
                fail("Book should fail to load");

            } catch (IOException e) {
                e.printStackTrace();

            } finally {
                tmpFile.delete();
            }

            return this;
        }

        public TestedBook isContent(String str) {
            if (book == null) fail("Notebook not loaded. Call onLoad().");

            Book book = dataRepository.getBooks().get(0).getBook();
            assertEquals("Content", str, book.getPreface());
            return this;
        }

        public TestedBook isWhenSaved(String expacted) {
            if (book == null) fail("Notebook not loaded. Call onLoad()");

            try {
                /* Write from db -> temp file. */
                File file = dataRepository.getTempBookFile();
                try {
                    new NotesOrgExporter(dataRepository).exportBook(book, file);
                    assertEquals("Notebook", expacted, MiscUtils.readStringFromFile(file));
                } finally {
                    file.delete();
                }

            } catch (IOException e) {
                e.printStackTrace();
                fail(e.toString());
            }

            return this;
        }
    }

    private interface TestedBookSource {
        File getTmpFile();
    }

    private class TestedBookSourceFromContent implements TestedBookSource {
        private String content;

        public TestedBookSourceFromContent(String content) {
            this.content = content;
        }

        @Override
        public File getTmpFile() {
            File file = null;

            try {
                file = dataRepository.getTempBookFile();
                MiscUtils.writeStringToFile(content, file);
            } catch (IOException e) {
                e.printStackTrace();
                fail(e.toString());
            }

            return file;
        }
    }

    private class TestedBookSourceFromResource implements TestedBookSource {
        private String resourceName;

        public TestedBookSourceFromResource(String resourceName) {
            this.resourceName = resourceName;
        }

        @Override
        public File getTmpFile() {
            InputStream stream = BookParsingTest.class.getClassLoader().getResourceAsStream("assets/" + resourceName);

            if (stream == null) {
                fail("Resource " + resourceName + " not found inside assets/");
            }

            File file = null;

            try {
                file = dataRepository.getTempBookFile();
                MiscUtils.writeStreamToFile(stream, file);
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
                fail(e.toString());
            }

            return file;
        }
    }
}
