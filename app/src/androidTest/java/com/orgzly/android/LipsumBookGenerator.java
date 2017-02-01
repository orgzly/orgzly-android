package com.orgzly.android;

import com.orgzly.org.OrgHead;
import com.orgzly.org.parser.OrgParserWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import de.svenjacobs.loremipsum.LoremIpsum;

/**
 * Org files generator
 *
 * TODO: Move to a separate project, might be useful to some
 */
public class LipsumBookGenerator {
    private static final int CHARS_PER_WORD = 6;

    public static String generateOrgString(int content, int[] notesAndContent) {
        StringBuilder result = new StringBuilder();

        LoremIpsum loremIpsum = new LoremIpsum();

        OrgParserWriter parserWriter = new OrgParserWriter();

        result.append(parserWriter.whiteSpacedFilePreface(loremIpsum.getWords(content / CHARS_PER_WORD)));

        if (notesAndContent != null) {
            for (int i = 0; i < notesAndContent.length; i += 2) {
                OrgHead head = new OrgHead();
                head.setTitle(loremIpsum.getWords(notesAndContent[i] / CHARS_PER_WORD));
                head.setContent(loremIpsum.getWords(notesAndContent[i + 1] / CHARS_PER_WORD));

                result.append(parserWriter.whiteSpacedHead(head, 1, false));
            }
        }

        return result.toString();
    }

    private static void generateOrgFile(int content, int[] notesAndContent, File file) throws FileNotFoundException {
        String str = generateOrgString(content, notesAndContent);

        PrintWriter out = new PrintWriter(file);
        try {
            out.write(str);
        } finally {
            out.close();
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        LipsumBookGenerator.generateOrgFile(5, new int[] { 100, 2, 3, 3 }, new File("/tmp/lipsum_generated.org"));
    }
}
