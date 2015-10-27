package edu.whu.cs.nlp.mts.nlp;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import edu.whu.cs.nlp.mts.domain.CoreferenceElement;

public class StanfordNLPToolsTest {

    @Test
    public void testCr() throws IOException {
        String input = FileUtils.readFileToString(FileUtils.getFile("src/test/resources/text.txt"), "UTF-8");
        Map<String, CoreferenceElement> map = StanfordNLPTools.cr(input);
        for (Entry<String, CoreferenceElement> entry : map.entrySet()) {
            System.out.println(entry.getKey() + "\t" + entry.getValue());
        }
    }

}
