package cn.iocoder.yudao.module.showroom.narration;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ai.framework.ai.core.model.codexcli.CodexCliChatModel;
import cn.iocoder.yudao.module.ai.util.AiUtils;
import cn.iocoder.yudao.module.showroom.keyword.service.ShowroomKeywordGlossaryService;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShowroomCompanyNarrationTranslationService {

    private static final String TRANSLATION_SYSTEM_MESSAGE = """
            你是一名专业的中英双语医疗企业展厅讲解稿翻译助手。
            任务：把用户提供的中文展厅公司介绍翻译成自然、正式、适合语音讲解的英文正文。
            约束：
            1. 不要扩写，不要总结，不要解释
            2. 不要输出标题、标签、引号或说明文字
            3. 只返回英文正文
            """;

    private final CodexCliChatModel codexCliChatModel;
    private final ShowroomKeywordGlossaryService keywordGlossaryService;

    public ShowroomCompanyNarrationTranslationService(CodexCliChatModel codexCliChatModel,
                                                      ShowroomKeywordGlossaryService keywordGlossaryService) {
        this.codexCliChatModel = codexCliChatModel;
        this.keywordGlossaryService = keywordGlossaryService;
    }

    public String translateZhToEn(String introTextZh) {
        if (StrUtil.isBlank(introTextZh)) {
            throw new ShowroomNarrationException("SHOWROOM_TRANSLATION_FAILED",
                    "company narration introduction text is required");
        }
        ShowroomKeywordGlossaryService.PreparedGlossary preparedGlossary = keywordGlossaryService.prepare(introTextZh.trim());
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(ShowroomTranslationPromptSupport.withKeywordGlossary(
                        TRANSLATION_SYSTEM_MESSAGE, preparedGlossary.glossaryPromptBlock())),
                new UserMessage(preparedGlossary.protectedText())
        ));
        String translated = AiUtils.getChatResponseContent(codexCliChatModel.call(prompt));
        if (StrUtil.isBlank(translated)) {
            throw new ShowroomNarrationException("SHOWROOM_TRANSLATION_FAILED",
                    "translated company narration text is empty");
        }
        return preparedGlossary.restore(translated.trim()).trim();
    }

}
