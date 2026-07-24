package cn.iocoder.yudao.module.showroom.narration;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.codexcli.CodexCliChatModel;
import cn.iocoder.yudao.module.ai.util.AiUtils;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ShowroomCompanyNarrationCodexService {

    private static final String SCRIPT_SYSTEM_MESSAGE = """
            你是一名医疗器械企业展厅公司讲解稿助手。
            任务：根据用户提供的真实公司资料，生成适合展厅中文语音讲解的正文。
            约束：
            1. 只能使用给定资料中的事实，不得编造公司规模、技术、产品、市场、资质、合作方、经营数据或未来规划
            2. 缺失字段直接跳过，不要猜测，不要输出“未提供”
            3. 语言要自然、专业、适合口播
            4. 尽量贴近用户要求的字数，允许少量浮动，但不要为了凑字硬编内容
            5. 不要输出标题、编号、项目符号、引号或说明文字
            6. 只返回中文正文
            """;

    private final CodexCliChatModel codexCliChatModel;

    public ShowroomCompanyNarrationCodexService(CodexCliChatModel codexCliChatModel) {
        this.codexCliChatModel = codexCliChatModel;
    }

    public String generateScript(String companyType, String displayName, Map<String, String> fields, int targetLength) {
        if (StrUtil.isBlank(companyType)) {
            throw new ShowroomNarrationException("SHOWROOM_SCRIPT_GENERATION_FAILED",
                    "company type is required");
        }
        if (StrUtil.isBlank(displayName)) {
            throw new ShowroomNarrationException("SHOWROOM_SCRIPT_GENERATION_FAILED",
                    "company display name is required");
        }
        if (fields == null || fields.isEmpty()) {
            throw new ShowroomNarrationException("SHOWROOM_SCRIPT_GENERATION_FAILED",
                    "company fields are required");
        }
        if (targetLength <= 0) {
            throw new ShowroomNarrationException("SHOWROOM_SCRIPT_GENERATION_FAILED",
                    "company narration target length must be greater than zero");
        }

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(SCRIPT_SYSTEM_MESSAGE),
                new UserMessage(buildCompanyFactPrompt(companyType, displayName, fields, targetLength))
        ));
        String generated = AiUtils.getChatResponseContent(codexCliChatModel.call(prompt));
        if (StrUtil.isBlank(generated)) {
            throw new ShowroomNarrationException("SHOWROOM_SCRIPT_GENERATION_FAILED",
                    "generated company narration text is empty");
        }
        return generated.trim();
    }

    private static String buildCompanyFactPrompt(String companyType, String displayName, Map<String, String> fields,
                                                 int targetLength) {
        StringBuilder builder = new StringBuilder();
        builder.append("请基于以下公司真实资料生成展厅中文语音介绍：\n");
        builder.append("目标字数：约").append(targetLength).append("字。\n");
        appendLine(builder, "公司名称", displayName);
        appendLine(builder, "公司类型", companyType);
        appendLine(builder, "发展历程", fields.get("development_history"));
        appendLine(builder, "园区介绍", fields.get("park_introduction"));
        appendLine(builder, "孵化平台", fields.get("incubation_platform"));
        appendLine(builder, "子公司概览", fields.get("subsidiary_overview"));
        appendLine(builder, "上市信息", fields.get("stock_info"));
        appendLine(builder, "核心制造能力", fields.get("core_manufacturing_capability"));
        appendLine(builder, "荣誉资质", fields.get("honors_awards"));
        return builder.toString().trim();
    }

    private static void appendLine(StringBuilder builder, String label, String value) {
        if (StrUtil.isBlank(value)) {
            return;
        }
        builder.append(label).append("：").append(value.trim()).append("\n");
    }

}
