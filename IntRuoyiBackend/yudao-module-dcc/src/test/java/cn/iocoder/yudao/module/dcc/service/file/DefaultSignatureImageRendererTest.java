package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import static org.junit.jupiter.api.Assertions.*;

class DefaultSignatureImageRendererTest {
    private final DefaultSignatureImageRenderer renderer = new DefaultSignatureImageRenderer();

    @Test
    void rendersRealKaiFontAsTransparentBlackPng() throws Exception {
        byte[] bytes = renderer.render("张三");
        assertArrayEquals(bytes, renderer.render("张三"));
        assertFalse(java.util.Arrays.equals(bytes, renderer.render("李四")));
        var image = ImageIO.read(new ByteArrayInputStream(bytes));
        assertNotNull(image);
        assertTrue(image.getColorModel().hasAlpha());
        assertEquals(0, image.getRGB(0, 0));
        int ink = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                if ((pixel >>> 24) != 0) {
                    assertEquals(0, pixel & 0xffffff);
                    ink++;
                }
            }
        }
        assertTrue(ink > 500);
    }

    @Test
    void rendersLongAndMixedNamesWithoutClipping() throws Exception {
        for (var name : java.util.List.of("欧阳娜娜", "阿卜杜拉·买买提", "张三 Alex", "王")) {
            var image = ImageIO.read(new ByteArrayInputStream(renderer.render(name)));
            for (int x = 0; x < image.getWidth(); x++) {
                assertEquals(0, image.getRGB(x, 0));
                assertEquals(0, image.getRGB(x, image.getHeight() - 1));
            }
            for (int y = 0; y < image.getHeight(); y++) {
                assertEquals(0, image.getRGB(0, y));
                assertEquals(0, image.getRGB(image.getWidth() - 1, y));
            }
        }
    }

    @Test
    void rejectsMissingNamesControlsAndUnsupportedGlyphs() {
        for (var name : new String[]{null, "", " ", "张\n三", "\uDBFF\uDFFF"}) {
            assertThrows(IllegalArgumentException.class, () -> renderer.render(name));
        }
    }
}
