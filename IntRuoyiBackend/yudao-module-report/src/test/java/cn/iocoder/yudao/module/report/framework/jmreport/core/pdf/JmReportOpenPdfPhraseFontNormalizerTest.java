package cn.iocoder.yudao.module.report.framework.jmreport.core.pdf;

import com.lowagie.text.Chunk;
import com.lowagie.text.Phrase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class JmReportOpenPdfPhraseFontNormalizerTest {

    @Test
    void phraseAddString_shouldPreventNullFontChunkForStringAdds() {
        Phrase phrase = new Phrase();
        phrase.add("virtual text");

        Chunk chunk = (Chunk) phrase.get(0);
        assertNotNull(chunk.getFont());
    }

    @Test
    void phraseCopy_shouldPreventNullFontChunkWhenSourceChunkHasNoFont() {
        Phrase source = new Phrase();
        source.add(new Chunk("virtual text", null));

        Phrase copied = new Phrase(source);

        Chunk chunk = (Chunk) copied.get(0);
        assertNotNull(chunk.getFont());
    }

}
