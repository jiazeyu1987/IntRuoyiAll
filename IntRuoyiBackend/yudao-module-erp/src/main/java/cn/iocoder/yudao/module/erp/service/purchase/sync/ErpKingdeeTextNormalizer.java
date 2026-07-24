package cn.iocoder.yudao.module.erp.service.purchase.sync;

import cn.hutool.core.util.StrUtil;

import java.nio.charset.StandardCharsets;

final class ErpKingdeeTextNormalizer {

    private ErpKingdeeTextNormalizer() {
    }

    static String normalize(String text) {
        String trimmed = StrUtil.trim(text);
        if (StrUtil.isEmpty(trimmed) || containsCjk(trimmed) || trimmed.chars().allMatch(ch -> ch < 0x80)) {
            return StrUtil.nullToEmpty(trimmed);
        }

        String candidate = new String(trimmed.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8).trim();
        if (StrUtil.isEmpty(candidate) || candidate.indexOf('\uFFFD') >= 0 || containsControlCharacter(candidate)) {
            return trimmed;
        }
        return containsCjk(candidate) ? candidate : trimmed;
    }

    private static boolean containsCjk(String text) {
        return text.codePoints().anyMatch(codePoint -> {
            Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
            return script == Character.UnicodeScript.HAN
                    || script == Character.UnicodeScript.HIRAGANA
                    || script == Character.UnicodeScript.KATAKANA;
        });
    }

    private static boolean containsControlCharacter(String text) {
        return text.codePoints().anyMatch(codePoint ->
                Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint));
    }

}
