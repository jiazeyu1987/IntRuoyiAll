package cn.iocoder.yudao.module.showroom.narration;

import cn.iocoder.yudao.module.ai.framework.ai.core.model.codexcli.CodexCliChatModel;
import cn.iocoder.yudao.module.showroom.keyword.service.ShowroomKeywordGlossaryService;
import cn.iocoder.yudao.module.showroom.dal.mysql.keyword.ShowroomKeywordMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShowroomTranslationPromptGlossaryTest {

    @Test
    void companyTranslationPromptShouldRequireIntMedicalGlossary() {
        CodexCliChatModel chatModel = mock(CodexCliChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("__SHOWROOM_TERM_1__ company introduction"));
        ShowroomKeywordMapper keywordMapper = mock(ShowroomKeywordMapper.class);
        when(keywordMapper.selectListOrdered()).thenReturn(List.of());
        ShowroomKeywordGlossaryService glossaryService = new ShowroomKeywordGlossaryService(keywordMapper);
        ShowroomCompanyNarrationTranslationService service =
                new ShowroomCompanyNarrationTranslationService(chatModel, glossaryService);

        String translated = service.translateZhToEn("瑛泰医疗公司介绍");

        assertEquals("int-medical company introduction", translated);
        Prompt prompt = capturePrompt(chatModel);
        assertPromptContainsGlossaryRule(prompt, "__SHOWROOM_TERM_1__公司介绍");
    }

    @Test
    void productTranslationPromptShouldRequireIntMedicalGlossary() {
        CodexCliChatModel chatModel = mock(CodexCliChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("__SHOWROOM_TERM_1__ product introduction"));
        ShowroomKeywordMapper keywordMapper = mock(ShowroomKeywordMapper.class);
        when(keywordMapper.selectListOrdered()).thenReturn(List.of());
        ShowroomKeywordGlossaryService glossaryService = new ShowroomKeywordGlossaryService(keywordMapper);
        ShowroomProductNarrationCodexService service =
                new ShowroomProductNarrationCodexService(chatModel, glossaryService);

        String translated = service.translateZhToEn("瑛泰医疗产品介绍");

        assertEquals("int-medical product introduction", translated);
        Prompt prompt = capturePrompt(chatModel);
        assertPromptContainsGlossaryRule(prompt, "__SHOWROOM_TERM_1__产品介绍");
    }

    @Test
    void companyTranslationShouldRestoreTenantKeywordGlossaryTerm() {
        CodexCliChatModel chatModel = mock(CodexCliChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("__SHOWROOM_TERM_1__ catheter company"));
        ShowroomKeywordMapper keywordMapper = mock(ShowroomKeywordMapper.class);
        when(keywordMapper.selectListOrdered()).thenReturn(List.of(keyword("翰凌", "Healing")));
        ShowroomKeywordGlossaryService glossaryService = new ShowroomKeywordGlossaryService(keywordMapper);
        ShowroomCompanyNarrationTranslationService service =
                new ShowroomCompanyNarrationTranslationService(chatModel, glossaryService);

        String translated = service.translateZhToEn("翰凌导管公司");

        assertEquals("Healing catheter company", translated);
        Prompt prompt = capturePrompt(chatModel);
        assertPromptContainsKeywordPrompt(prompt, "__SHOWROOM_TERM_1__导管公司", "翰凌", "Healing");
    }

    @Test
    void companyTranslationShouldPreferLongestKeywordMatch() {
        CodexCliChatModel chatModel = mock(CodexCliChatModel.class);
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse("__SHOWROOM_TERM_1__"));
        ShowroomKeywordMapper keywordMapper = mock(ShowroomKeywordMapper.class);
        when(keywordMapper.selectListOrdered()).thenReturn(List.of(
                keyword("上海翰凌医疗器械有限公司", "Shanghai Healing Medical Instruments Co., Ltd."),
                keyword("翰凌", "Healing")));
        ShowroomKeywordGlossaryService glossaryService = new ShowroomKeywordGlossaryService(keywordMapper);
        ShowroomCompanyNarrationTranslationService service =
                new ShowroomCompanyNarrationTranslationService(chatModel, glossaryService);

        String translated = service.translateZhToEn("上海翰凌医疗器械有限公司");

        assertEquals("Shanghai Healing Medical Instruments Co., Ltd.", translated);
        Prompt prompt = capturePrompt(chatModel);
        assertPromptContainsKeywordPrompt(prompt, "__SHOWROOM_TERM_1__",
                "上海翰凌医疗器械有限公司", "Shanghai Healing Medical Instruments Co., Ltd.");
    }

    private static Prompt capturePrompt(CodexCliChatModel chatModel) {
        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        return promptCaptor.getValue();
    }

    private static void assertPromptContainsGlossaryRule(Prompt prompt, String expectedUserText) {
        SystemMessage systemMessage = (SystemMessage) prompt.getInstructions().get(0);
        UserMessage userMessage = (UserMessage) prompt.getInstructions().get(1);

        assertTrue(systemMessage.getText().contains("瑛泰医疗"));
        assertTrue(systemMessage.getText().contains("int-medical"));
        assertTrue(systemMessage.getText().contains("Yingtai Medical"));
        assertTrue(systemMessage.getText().contains("__SHOWROOM_TERM_1__"));
        assertTrue(systemMessage.getText().contains("占位符"));
        assertEquals(expectedUserText, userMessage.getText());
    }

    private static void assertPromptContainsKeywordPrompt(Prompt prompt, String expectedUserText,
                                                          String nameZh, String nameEn) {
        SystemMessage systemMessage = (SystemMessage) prompt.getInstructions().get(0);
        UserMessage userMessage = (UserMessage) prompt.getInstructions().get(1);

        assertTrue(systemMessage.getText().contains(nameZh));
        assertTrue(systemMessage.getText().contains(nameEn));
        assertTrue(systemMessage.getText().contains("__SHOWROOM_TERM_1__"));
        assertTrue(systemMessage.getText().contains("占位符"));
        assertEquals(expectedUserText, userMessage.getText());
    }

    private static ChatResponse chatResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    private static cn.iocoder.yudao.module.showroom.dal.dataobject.keyword.ShowroomKeywordDO keyword(String nameZh,
                                                                                                      String nameEn) {
        cn.iocoder.yudao.module.showroom.dal.dataobject.keyword.ShowroomKeywordDO keyword =
                new cn.iocoder.yudao.module.showroom.dal.dataobject.keyword.ShowroomKeywordDO();
        keyword.setNameZh(nameZh);
        keyword.setNameEn(nameEn);
        return keyword;
    }
}
