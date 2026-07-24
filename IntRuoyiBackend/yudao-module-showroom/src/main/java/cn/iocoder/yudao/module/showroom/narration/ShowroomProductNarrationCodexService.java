package cn.iocoder.yudao.module.showroom.narration;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.codexcli.CodexCliChatModel;
import cn.iocoder.yudao.module.ai.util.AiUtils;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductSnapshot;
import cn.iocoder.yudao.module.showroom.keyword.service.ShowroomKeywordGlossaryService;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShowroomProductNarrationCodexService {

    private static final String SALES_COUNTRIES_SYSTEM_MESSAGE = """
            你是一名医疗器械企业产品在售国家资料助手。
            任务：根据用户提供的真实产品资料，整理适合填写在产品“在售国家”字段中的中文国家/地区文本。
            约束：
            1. 只能使用给定资料中明确出现的国家或地区，不得根据适应症、注册信息或目标市场猜测
            2. 缺少明确国家或地区时返回空内容，让调用方失败可见
            3. 多个国家或地区用中文顿号或分号分隔
            4. 不要输出标题、编号、项目符号、引号或说明文字
            5. 只返回在售国家/地区正文
            """;

    private static final String SCRIPT_SYSTEM_MESSAGE = """
            你是一名医疗器械企业展厅产品讲解稿助手。
            任务：根据用户提供的真实产品资料，生成适合展厅中文语音讲解的正文。
            约束：
            1. 只能使用给定资料中的事实，不得编造参数、适应症、注册信息或临床结论
            2. 缺失字段直接跳过，不要猜测，不要输出“未提供”
            3. 语言要自然、专业、适合口播
            4. 不要输出标题、编号、项目符号、引号或说明文字
            5. 只返回中文正文
            """;

    private static final String TRANSLATION_SYSTEM_MESSAGE = """
            你是一名专业的中英双语医疗器械展厅讲解稿翻译助手。
            任务：把用户提供的中文产品讲解稿翻译成自然、正式、适合语音讲解的英文正文。
            约束：
            1. 不要扩写，不要总结，不要解释
            2. 不要输出标题、标签、引号或说明文字
            3. 只返回英文正文
            """;

    private final CodexCliChatModel codexCliChatModel;
    private final ShowroomKeywordGlossaryService keywordGlossaryService;

    public ShowroomProductNarrationCodexService(CodexCliChatModel codexCliChatModel,
                                                ShowroomKeywordGlossaryService keywordGlossaryService) {
        this.codexCliChatModel = codexCliChatModel;
        this.keywordGlossaryService = keywordGlossaryService;
    }

    public String generateSalesCountries(ShowroomProductSnapshot snapshot, ShowroomProductRevision revision) {
        if (snapshot == null || revision == null) {
            throw new ShowroomNarrationException("SHOWROOM_SALES_COUNTRIES_GENERATION_FAILED",
                    "product snapshot and revision are required");
        }
        return callCodex(SALES_COUNTRIES_SYSTEM_MESSAGE, buildProductSalesCountryPrompt(snapshot, revision),
                "SHOWROOM_SALES_COUNTRIES_GENERATION_FAILED", "generated product sales countries are empty");
    }

    public String generateScript(ShowroomProductSnapshot snapshot, ShowroomProductRevision revision) {
        if (snapshot == null || revision == null) {
            throw new ShowroomNarrationException("SHOWROOM_SCRIPT_GENERATION_FAILED",
                    "product snapshot and revision are required");
        }
        return callCodex(SCRIPT_SYSTEM_MESSAGE, buildProductFactPrompt(snapshot, revision),
                "SHOWROOM_SCRIPT_GENERATION_FAILED", "generated product narration text is empty");
    }

    public String translateZhToEn(String scriptTextZh) {
        if (StrUtil.isBlank(scriptTextZh)) {
            throw new ShowroomNarrationException("SHOWROOM_TRANSLATION_FAILED",
                    "product narration script text is required");
        }
        ShowroomKeywordGlossaryService.PreparedGlossary preparedGlossary =
                keywordGlossaryService.prepare(scriptTextZh.trim());
        String translated = callCodex(
                ShowroomTranslationPromptSupport.withKeywordGlossary(
                        TRANSLATION_SYSTEM_MESSAGE, preparedGlossary.glossaryPromptBlock()),
                preparedGlossary.protectedText(),
                "SHOWROOM_TRANSLATION_FAILED", "translated product narration text is empty");
        return preparedGlossary.restore(translated).trim();
    }

    private String callCodex(String systemMessage, String userMessage, String code, String emptyMessage) {
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemMessage),
                new UserMessage(userMessage)
        ));
        String content = AiUtils.getChatResponseContent(codexCliChatModel.call(prompt));
        if (StrUtil.isBlank(content)) {
            throw new ShowroomNarrationException(code, emptyMessage);
        }
        return content.trim();
    }

    private static String buildProductFactPrompt(ShowroomProductSnapshot snapshot, ShowroomProductRevision revision) {
        StringBuilder builder = new StringBuilder();
        builder.append("请基于以下产品真实资料生成展厅中文讲解稿：\n");
        appendLine(builder, "产品编码", snapshot.productCode());
        appendLine(builder, "中文名称", revision.nameCn());
        appendLine(builder, "英文名称", revision.nameEn());
        appendLine(builder, "产品归属/类型", mapOwnerType(revision.fields().get("product_owner_type")));
        appendLine(builder, "生命周期", mapLifecycleStage(revision.fields().get("lifecycle_stage")));
        appendLine(builder, "在售国家", revision.fields().get("target_market"));
        appendLine(builder, "BU", revision.fields().get("pipeline_layout"));
        appendLine(builder, "适应症", revision.fields().get("indication_content"));
        appendLine(builder, "卖点文案", revision.fields().get("core_selling_points"));
        appendLine(builder, "型号规格", revision.fields().get("model_specification"));
        appendLine(builder, "注册证", revision.fields().get("registration_certificate"));
        appendLine(builder, "临床效果", revision.fields().get("clinical_effect"));
        appendLine(builder, "FIM状态", revision.fields().get("fim_status"));
        return builder.toString().trim();
    }

    private static String buildProductSalesCountryPrompt(ShowroomProductSnapshot snapshot,
                                                         ShowroomProductRevision revision) {
        StringBuilder builder = new StringBuilder();
        builder.append("请基于以下产品真实资料整理中文在售国家：\n");
        appendLine(builder, "产品编码", snapshot.productCode());
        appendLine(builder, "中文名称", revision.nameCn());
        appendLine(builder, "英文名称", revision.nameEn());
        appendLine(builder, "产品归属/类型", mapOwnerType(revision.fields().get("product_owner_type")));
        appendLine(builder, "生命周期", mapLifecycleStage(revision.fields().get("lifecycle_stage")));
        appendLine(builder, "在售国家", revision.fields().get("target_market"));
        appendLine(builder, "BU", revision.fields().get("pipeline_layout"));
        appendLine(builder, "卖点文案", revision.fields().get("core_selling_points"));
        appendLine(builder, "适应症", revision.fields().get("indication_content"));
        appendLine(builder, "型号规格", revision.fields().get("model_specification"));
        appendLine(builder, "注册证", revision.fields().get("registration_certificate"));
        appendLine(builder, "临床效果", revision.fields().get("clinical_effect"));
        appendLine(builder, "FIM状态", revision.fields().get("fim_status"));
        return builder.toString().trim();
    }

    private static void appendLine(StringBuilder builder, String label, String value) {
        if (StrUtil.isBlank(value)) {
            return;
        }
        builder.append(label).append("：").append(value.trim()).append("\n");
    }

    private static String mapOwnerType(String ownerType) {
        if ("YINGTAI".equals(ownerType)) {
            return "瑛泰医疗";
        }
        if ("SUBSIDIARY".equals(ownerType)) {
            return "瑛泰医疗";
        }
        return ownerType;
    }

    private static String mapLifecycleStage(String lifecycleStage) {
        if ("REGISTERED".equals(lifecycleStage)) {
            return "已注册";
        }
        if ("R_AND_D".equals(lifecycleStage)) {
            return "研发中";
        }
        return lifecycleStage;
    }

}
