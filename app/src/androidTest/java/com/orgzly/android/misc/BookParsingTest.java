package com.orgzly.android.misc;

import android.util.Log;

import com.orgzly.android.Book;
import com.orgzly.android.BookName;
import com.orgzly.android.LipsumBookGenerator;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.util.MiscUtils;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

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
    public void testBookContent1() throws IOException {
        onBook("Sample book used for tests\n\n\n* Note")
                .onLoad()
                .isContent("Sample book used for tests")
                .isNoteTitle(1, "Note")
                .isNoteLevel(1, 1)
                .isWhenSaved("Sample book used for tests\n\n* Note\n");
    }

    @Test
    public void testBookContentPrefixedWithSpaces() {
        onBook("  Sample book used for tests\n\n\n* Note")
                .onLoad()
                .isContent("  Sample book used for tests")
                .isNoteTitle(1, "Note")
                .isNoteLevel(1, 1)
                .isWhenSaved("  Sample book used for tests\n\n* Note\n");
    }

    @Test
    public void testBookContent2() throws IOException {
        onBook("\nSample book used for tests\n\n\n* Note")
                .onLoad()
                .isContent("Sample book used for tests")
                .isNoteTitle(1, "Note")
                .isNoteLevel(1, 1)
                .isWhenSaved("Sample book used for tests\n\n* Note\n");
    }

    @Test
    public void testSavingWithPriorities() throws IOException {
        onBook("\nSample book used for tests\n\n\n* Note 1\n* [#B] Note 2\n* [#A] Note 3")
                .onLoad()
                .isContent("Sample book used for tests")
                .isWhenSaved("Sample book used for tests\n\n* Note 1\n* [#B] Note 2\n* [#A] Note 3\n");
    }

    @Test
    public void testNotIndented1() throws IOException {
        onBook("* Note 1\nSCHEDULED: <2015-02-11 Wed +1d>").onLoad()
                .isWhenSaved("* Note 1\nSCHEDULED: <2015-02-11 Wed +1d>\n\n");
    }

    @Test
    public void testNotIndented2() throws IOException {
        onBook("* Note 1\n:LOGBOOK:\n:END:").onLoad()
                .isWhenSaved("* Note 1\n:LOGBOOK:\n:END:\n\n");
    }

    @Test
    public void testIndented1() throws IOException {
        onBook("* Note 1\n  SCHEDULED: <2015-02-11 Wed +1d>").onLoad()
                .isWhenSaved("* Note 1\n  SCHEDULED: <2015-02-11 Wed +1d>\n\n");
    }

    @Test
    public void testIndented2() throws IOException {
        onBook("* Note 1\n  :LOGBOOK:\n  :END:").onLoad()
                .isWhenSaved("* Note 1\n  :LOGBOOK:\n  :END:\n\n");
    }

    @Test
    public void testEmptyProperties() throws IOException {
        onBook("* Note 1\n  :PROPERTIES:\n  :END:").onLoad()
                .isWhenSaved("* Note 1\n");

    }

    @Test
    public void testProperties() throws IOException {
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
    public void testPropertiesMultiple() throws IOException {
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
    public void testPropertiesOrder() throws IOException {
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
    public void testPropertiesEmpty() throws IOException {
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
    public void testLoadingTextDv() throws IOException {
        onBookFile("html/dv.org").onLoad().onGet();
    }

    @Test
    public void testLoadingTextZh() throws IOException {
        onBookFile("html/zh.org").onLoad().onGet();
    }

    @Test
    public void testLoadingHuge() throws IOException {
        if (runResourcesDemandingTest()) {
            onBookFile("org/org-issues.10x.org").onLoad();
        }
    }

    /*
     * Files generated using http://generator.lorem-ipsum.info/ ...
     */

    @Test
    public void testLoadingArabicLipsum() throws IOException {
        onBookFile("lipsum/Arabic-Lipsum.org").onLoad().onGet();
    }

    @Test
    public void testLoadingChineseLipsum() throws IOException {
        onBookFile("lipsum/Chinese-Lipsum.org").onLoad().onGet();
    }

    @Test
    public void testLoadingGreekLipsum() throws IOException {
        onBookFile("lipsum/Greek-Lipsum.org").onLoad().onGet();
    }

    @Test
    public void testLoadingHebrewLipsum() throws IOException {
        onBookFile("lipsum/Hebrew-Lipsum.org").onLoad().onGet();
    }

    @Test
    public void testLoadingHindiLipsum() throws IOException {
        onBookFile("lipsum/Hindi-Lipsum.org").onLoad().onGet();
    }

    @Test
    public void testLoadingJapaneseLipsum() throws IOException {
        onBookFile("lipsum/Japanese-Lipsum.org").onLoad().onGet();
    }

    @Test
    public void testLoadingL33TspeakLipsum() throws IOException {
        onBookFile("lipsum/L33tspeak-Lipsum.org").onLoad().onGet();
    }

    @Test
    public void testLoadingRussianLipsum() throws IOException {
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

    @Test
    public void testLion() {
        if (runResourcesDemandingTest()) {
            onBookFile("images/lion-wide.org").onLoad();
        }
    }

    /*
     * Generated lipsum ...
     */

    @Test
    public void testLoadingLatinLipsumHugeBookContentWithTitleTitleAndNoteContent() {
        if (runResourcesDemandingTest()) {
            String data = LipsumBookGenerator.generateOrgString(1000000, new int[] { 1000000, 1000000 });
            onBook(data).onLoadFailed();
        }
    }

    @Test
    public void testLoadingLatinLipsumHugeNoteTitle() {
        if (runResourcesDemandingTest()) {
            String data = LipsumBookGenerator.generateOrgString(0, new int[] { 2000000, 0 });
            onBook(data).onLoadFailed();
        }
    }

    @Test
    public void testLoadingLatinLipsumLargeNoteTitles() {
        if (runResourcesDemandingTest()) {
            String data = LipsumBookGenerator.generateOrgString(0, new int[] { 1000000, 0, 1500000, 1 });
            onBook(data).onLoad();
        }
    }

    @Test
    public void testLoadingLatinLipsumHugeNoteTitleWithContent() {
        if (runResourcesDemandingTest()) {
            String data = LipsumBookGenerator.generateOrgString(0, new int[] { 1000000, 1000000 });
            onBook(data).onLoadFailed();
        }
    }

    @Test
    public void testBookContentTooBig() {
        if (runResourcesDemandingTest()) {
            String data = LipsumBookGenerator.generateOrgString(2000000, null);
            onBook(data).onLoadFailed();
        }
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
            shelf.clearDatabase();

            File tmpFile = bookSource.getTmpFile();
            try {
                /* Load from file. */
                shelf.loadBookFromFile("Notebook", BookName.Format.ORG, tmpFile);

            } catch (IOException e) {
                e.printStackTrace();
                fail(e.toString());

            } finally {
                tmpFile.delete();
            }

            book = shelf.getBooks().get(0);

            return this;
        }

        public TestedBook onGet() {
            try {
                book = shelf.getBooks().get(0);

            } catch (Exception e) {
                e.printStackTrace();
                fail(e.toString());
            }

            return this;
        }

        public TestedBook onGetFailed() {
            try {
                book = shelf.getBooks().get(0);
                fail("Book should fail to load");

            } catch (Exception e) {
                e.printStackTrace();
            }

            return this;
        }

        public TestedBook onLoadFailed() {
            shelf.clearDatabase();

            File tmpFile = bookSource.getTmpFile();
            try {
                /* Load from file. */
                shelf.loadBookFromFile("Notebook", BookName.Format.ORG, tmpFile);
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

            Book book = shelf.getBooks().get(0);
            assertEquals("Content", str, book.getPreface());
            return this;
        }

        public TestedBook isNoteTitle(int note, String str) {
            if (book == null) fail("Notebook not loaded. Call onLoad()");

            assertEquals("Title", str, shelf.getNote(note).getHead().getTitle());
            return this;
        }

        public TestedBook isNoteLevel(int note, int level) {
            if (book == null) fail("Notebook not loaded. Call onLoad()");

            assertEquals("Note level", level, shelf.getNote(note).getPosition().getLevel());
            return this;
        }

        public TestedBook isWhenSaved(String expacted) {
            if (book == null) fail("Notebook not loaded. Call onLoad()");

            try {
                /* Write from db -> temp file. */
                File file = shelf.getTempBookFile();
                try {
                    shelf.writeBookToFile(book, BookName.Format.ORG, file);
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
                file = shelf.getTempBookFile();
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
                file = shelf.getTempBookFile();
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
