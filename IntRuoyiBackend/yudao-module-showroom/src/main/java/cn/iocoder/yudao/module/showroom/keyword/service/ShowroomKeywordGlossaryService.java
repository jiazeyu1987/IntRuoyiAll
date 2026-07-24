package cn.iocoder.yudao.module.showroom.keyword.service;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.showroom.dal.dataobject.keyword.ShowroomKeywordDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.keyword.ShowroomKeywordMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
public class ShowroomKeywordGlossaryService {

    private static final String PLACEHOLDER_PREFIX = "__SHOWROOM_TERM_";
    private static final String PLACEHOLDER_SUFFIX = "__";
    private static final List<MatchedTerm> BUILT_IN_TERMS = List.of(
            new MatchedTerm(null, "瑛泰医疗", "int-medical")
    );

    private final ShowroomKeywordMapper keywordMapper;

    public ShowroomKeywordGlossaryService(ShowroomKeywordMapper keywordMapper) {
        this.keywordMapper = keywordMapper;
    }

    public PreparedGlossary prepare(String sourceText) {
        if (StrUtil.isBlank(sourceText)) {
            return PreparedGlossary.empty(sourceText);
        }
        List<MatchedTerm> keywords = buildMatchedTerms();
        if (keywords.isEmpty()) {
            return PreparedGlossary.empty(sourceText);
        }

        String protectedText = sourceText;
        List<MatchedTerm> matchedTerms = new ArrayList<>();
        int placeholderIndex = 1;
        for (MatchedTerm keyword : keywords) {
            String nameZh = keyword.nameZh().trim();
            String nameEn = keyword.nameEn().trim();
            if (!protectedText.contains(nameZh)) {
                continue;
            }
            String placeholder = PLACEHOLDER_PREFIX + placeholderIndex + PLACEHOLDER_SUFFIX;
            protectedText = protectedText.replace(nameZh, placeholder);
            matchedTerms.add(new MatchedTerm(placeholder, nameZh, nameEn));
            placeholderIndex++;
        }
        if (matchedTerms.isEmpty()) {
            return PreparedGlossary.empty(sourceText);
        }
        return new PreparedGlossary(sourceText, protectedText, List.copyOf(matchedTerms));
    }

    private List<MatchedTerm> buildMatchedTerms() {
        Map<String, MatchedTerm> termMap = new LinkedHashMap<>();
        keywordMapper.selectListOrdered().stream()
                .filter(keyword -> StrUtil.isNotBlank(keyword.getNameZh()) && StrUtil.isNotBlank(keyword.getNameEn()))
                .forEach(keyword -> termMap.put(keyword.getNameZh().trim(),
                        new MatchedTerm(null, keyword.getNameZh().trim(), keyword.getNameEn().trim())));
        for (MatchedTerm builtInTerm : BUILT_IN_TERMS) {
            termMap.putIfAbsent(builtInTerm.nameZh(), builtInTerm);
        }
        return termMap.values().stream()
                .sorted(Comparator
                        .comparingInt((MatchedTerm keyword) -> keyword.nameZh().length()).reversed()
                        .thenComparing(MatchedTerm::nameZh))
                .toList();
    }

    public record PreparedGlossary(String sourceText, String protectedText, List<MatchedTerm> matchedTerms) {

        static PreparedGlossary empty(String sourceText) {
            String normalized = sourceText == null ? "" : sourceText;
            return new PreparedGlossary(normalized, normalized, List.of());
        }

        public boolean hasMatches() {
            return !matchedTerms.isEmpty();
        }

        public String restore(String translatedText) {
            if (translatedText == null) {
                return null;
            }
            String restored = translatedText;
            for (MatchedTerm matchedTerm : matchedTerms) {
                restored = restored.replace(matchedTerm.placeholder(), matchedTerm.nameEn());
            }
            return restored;
        }

        public String glossaryPromptBlock() {
            if (matchedTerms.isEmpty()) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            builder.append("5. 对以下占位符必须原样保留，不要翻译、拆分、删除或改写，并按对应术语理解上下文：\n");
            for (MatchedTerm matchedTerm : matchedTerms) {
                builder.append("   - ")
                        .append(matchedTerm.placeholder())
                        .append(" 对应中文“")
                        .append(matchedTerm.nameZh())
                        .append("”，最终英文固定为“")
                        .append(matchedTerm.nameEn())
                        .append("”\n");
            }
            return builder.toString().trim();
        }
    }

    public record MatchedTerm(String placeholder, String nameZh, String nameEn) {
    }

}
