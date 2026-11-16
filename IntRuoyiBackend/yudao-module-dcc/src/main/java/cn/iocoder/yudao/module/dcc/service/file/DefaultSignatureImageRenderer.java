package cn.iocoder.yudao.module.dcc.service.file;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.RenderingHints;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Render the persisted real name with the bundled Kai font; never use an OS font substitute. */
@Component
public class DefaultSignatureImageRenderer {

    private volatile Font font;

    private synchronized Font font() {
        if (font != null) return font;
        try (var input = getClass().getResourceAsStream("/fonts/LXGWWenKai-Regular.ttf")) {
            if (input == null) throw new IllegalStateException("默认签名楷体字体文件缺失");
            font = Font.createFont(Font.TRUETYPE_FONT, input).deriveFont(96f);
            return font;
        } catch (IOException | FontFormatException ex) {
            throw new IllegalStateException("默认签名楷体字体加载失败", ex);
        }
    }

    public byte[] render(String realName) {
        if (realName == null || realName.isBlank()) {
            throw new IllegalArgumentException("请先维护本人姓名，再生成默认签名图片");
        }
        String name = realName.strip();
        if (name.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("本人姓名不能包含控制字符");
        }
        Font kai = font();
        if (kai.canDisplayUpTo(name) != -1) {
            throw new IllegalArgumentException("默认签名楷体字体不支持姓名中的部分文字，请上传签名图片");
        }
        var context = new java.awt.font.FontRenderContext(null, true, true);
        var layout = new TextLayout(name, kai, context);
        var bounds = layout.getBounds();
        int padding = 24;
        int width = (int) Math.ceil(bounds.getWidth()) + padding * 2;
        int height = (int) Math.ceil(bounds.getHeight()) + padding * 2;
        if (bounds.isEmpty() || width > 8192 || height > 512) {
            throw new IllegalArgumentException("本人姓名无法生成有效签名图片，请检查姓名");
        }
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            graphics.setColor(Color.BLACK);
            layout.draw(graphics, (float) (padding - bounds.getX()), (float) (padding - bounds.getY()));
        } finally {
            graphics.dispose();
        }
        try (var output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", output)) throw new IllegalStateException("默认签名 PNG 编码器不可用");
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("默认签名图片生成失败", ex);
        }
    }
}
