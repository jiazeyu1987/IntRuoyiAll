package cn.iocoder.yudao.module.showroom.narration;

final class ShowroomTranslationPromptSupport {

    private static final String INT_MEDICAL_GLOSSARY_RULE = """
            4. 遇到“瑛泰医疗”时，统一翻译成“int-medical”，不要翻译为“Yingtai Medical”或其它英文写法
            """;

    private ShowroomTranslationPromptSupport() {
    }

    static String withIntMedicalGlossaryRule(String systemMessage) {
        return systemMessage.stripTrailing() + "\n" + INT_MEDICAL_GLOSSARY_RULE.strip() + "\n";
    }

    static String withKeywordGlossary(String systemMessage, String glossaryRuleBlock) {
        if (glossaryRuleBlock == null || glossaryRuleBlock.isBlank()) {
            return withIntMedicalGlossaryRule(systemMessage);
        }
        return withIntMedicalGlossaryRule(systemMessage).stripTrailing() + "\n" + glossaryRuleBlock.strip() + "\n";
    }

}
