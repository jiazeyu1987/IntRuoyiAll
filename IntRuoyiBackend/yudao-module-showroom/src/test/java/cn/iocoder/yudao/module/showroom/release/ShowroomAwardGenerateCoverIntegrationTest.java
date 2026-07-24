package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import cn.iocoder.yudao.module.ai.service.tts.AiTtsAliyunNlsCredentialService;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardRevision;
import cn.iocoder.yudao.module.showroom.controller.ShowroomApiRuntime;
import cn.iocoder.yudao.module.showroom.controller.admin.ShowroomAdminController;
import cn.iocoder.yudao.module.showroom.cover.ShowroomAwardCoverImageService;
import cn.iocoder.yudao.module.showroom.cover.ShowroomProductCoverBatchTaskService;
import cn.iocoder.yudao.module.showroom.cover.ShowroomProductCoverImageService;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomAwardRevisionDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomAwardRevisionMapper;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationLanguage;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationTargetType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomCompanyNarrationCodexService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomCompanyNarrationTranslationService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomProductNarrationCodexService;
import cn.iocoder.yudao.module.showroom.prompt.ShowroomImagePromptVersionService;
import cn.iocoder.yudao.module.showroom.release.ShowroomVersionBundleService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomApprovalActorResolver;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomAssignmentService;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomWorkflowFacade;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({
        ShowroomAdminController.class,
        ShowroomApiRuntime.class
})
class ShowroomAwardGenerateCoverIntegrationTest extends AbstractShowroomReleaseDbTest {

    @Resource
    private ShowroomAdminController adminController;
    @Resource
    private ShowroomAwardRevisionMapper awardRevisionMapper;
    @Resource
    private ShowroomApiRuntime runtime;

    @MockBean
    private ShowroomWorkflowFacade workflowFacade;
    @MockBean
    private ShowroomApprovalActorResolver approvalActorResolver;
    @MockBean
    private ShowroomAssignmentService assignmentService;
    @MockBean
    private cn.iocoder.yudao.module.showroom.content.service.ShowroomProductCommentService commentService;
    @MockBean
    private SecurityFrameworkService securityFrameworkService;
    @MockBean
    private ShowroomProductCoverBatchTaskService productCoverBatchTaskService;
    @MockBean
    private ShowroomProductCoverImageService productCoverImageService;
    @MockBean
    private ShowroomAwardCoverImageService awardCoverImageService;
    @MockBean
    private ShowroomCompanyNarrationCodexService narrationCodexService;
    @MockBean
    private ShowroomCompanyNarrationTranslationService narrationTranslationService;
    @MockBean
    private ShowroomProductNarrationCodexService productNarrationCodexService;
    @MockBean
    private ShowroomImagePromptVersionService imagePromptVersionService;
    @MockBean
    private ShowroomVersionBundleService versionBundleService;
    @MockBean
    private cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductRevisionRelationMapper productRevisionRelationMapper;
    @MockBean
    private cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestMapper changeRequestMapper;
    @MockBean
    private AiTtsAliyunNlsCredentialService aliyunNlsCredentialService;
    @MockBean
    private YudaoAiProperties yudaoAiProperties;
    @MockBean
    private ConfigService configService;
    @MockBean
    private ShowroomPublicReleaseReadbackVerifier publicReleaseReadbackVerifier;

    @BeforeEach
    void setUpSecurityRoles() {
        when(securityFrameworkService.hasRole(anyString())).thenAnswer(invocation -> {
            String roleCode = invocation.getArgument(0);
            return ShowroomAdminController.SHOWROOM_PUBLICITY_ROLE_CODE.equals(roleCode);
        });
    }

    @Test
    void generateAwardCoverImageShouldCreateAndPublishNewRevision() {
        ShowroomAwardRevision draft = contentService.saveAwardDraft(new ShowroomAwardDraft(
                null, "AWARD-GEN-001", "生图奖项", "Generated Award",
                "中文讲解", "English narration", "颁发单位", "2026",
                "/admin-api/infra/file/11/get/showroom/award/source-award.png"));
        ShowroomAwardRevision published = contentService.publishAwardRevision(draft.revisionId(), 300L);
        publishNarration(ShowroomNarrationTargetType.AWARD, published.awardId(), published.revisionId(),
                ShowroomNarrationLanguage.ZH, published.fields().get("description_zh"), 2301L);
        publishNarration(ShowroomNarrationTargetType.AWARD, published.awardId(), published.revisionId(),
                ShowroomNarrationLanguage.EN, published.fields().get("description_en"), 2302L);
        Object runtimeTarget = AopTestUtils.getUltimateTargetObject(runtime);
        Object injectedService = ReflectionTestUtils.getField(runtimeTarget, "awardCoverImageService");
        assertSame(awardCoverImageService, injectedService);
        doReturn("/admin-api/infra/file/28/get/showroom/award/award-gen-001-cover.png")
                .when(awardCoverImageService).generateCoverImage(any(), any(), any());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(300L);

            var response = adminController.generateAwardCoverImage(
                    new ShowroomAdminController.AwardCoverGenerateReqVO(published.awardId()));

            assertEquals(0, response.getCode());
            assertEquals(published.awardId(), response.getData().awardId());
            assertEquals(published.revisionNo() + 1, response.getData().revisionNo());
            assertEquals("/admin-api/infra/file/28/get/showroom/award/award-gen-001-cover.png",
                    response.getData().coverImageUrl());
            ShowroomAwardRevision current = contentService.requireCurrentAwardRevision(published.awardId());
            assertEquals(response.getData().revisionId(), current.revisionId());
            assertEquals(response.getData().revisionNo(), current.revisionNo());
            assertEquals("/admin-api/infra/file/28/get/showroom/award/award-gen-001-cover.png",
                    current.fields().get("cover_image"));
        }
    }

    @Test
    void awardCoverImageServiceShouldReuseProductStylePromptOnlyGenerationAfterValidatingSourceCover() throws Exception {
        cn.iocoder.yudao.module.infra.service.file.FileService fileService =
                org.mockito.Mockito.mock(cn.iocoder.yudao.module.infra.service.file.FileService.class);
        cn.iocoder.yudao.module.showroom.cover.ShowroomNativeImageGenerationService nativeService =
                org.mockito.Mockito.mock(cn.iocoder.yudao.module.showroom.cover.ShowroomNativeImageGenerationService.class);
        cn.iocoder.yudao.module.showroom.cover.ShowroomAwardCoverImageService service =
                new cn.iocoder.yudao.module.showroom.cover.ShowroomAwardCoverImageService(fileService, nativeService);

        byte[] sourceBytes = new byte[] {1, 2, 3};
        byte[] generatedBytes = new byte[] {4, 5, 6};
        Path generatedFile;
        try {
            generatedFile = java.nio.file.Files.createTempFile("award-cover-generated-", ".png");
            java.nio.file.Files.write(generatedFile, generatedBytes);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        doReturn(sourceBytes).when(fileService).getFileContent(28L, "showroom/product/cover/award-source.png");
        doReturn(generatedFile).when(nativeService).generatePng(anyString(),
                eq("SHOWROOM_AWARD_COVER_GENERATION_FAILED"), eq("award cover"));
        doReturn(9001L).when(fileService).createFileAndReturnId(generatedBytes, "award-AWARD-003-cover.png",
                "showroom/award", "image/png");
        cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO file =
                new cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO();
        file.setConfigId(28L);
        file.setPath("showroom/award/award-AWARD-003-cover.png");
        doReturn(file).when(fileService).getFile(9001L);

        String url = service.generateCoverImage("AWARD-003", "prompt text",
                "/admin-api/infra/file/28/get/showroom/product/cover/award-source.png");

        assertEquals("/admin-api/infra/file/28/get/showroom/award/award-AWARD-003-cover.png", url);
        verify(fileService).getFileContent(28L, "showroom/product/cover/award-source.png");
        verify(nativeService).generatePng(eq("prompt text"),
                eq("SHOWROOM_AWARD_COVER_GENERATION_FAILED"), eq("award cover"));
        try {
            java.nio.file.Files.deleteIfExists(generatedFile);
        } catch (java.io.IOException ignored) {
        }
    }

    @Test
    void generateAwardCoverImageShouldFailFastWhenCurrentAwardNarrationMissing() {
        ShowroomAwardRevision draft = contentService.saveAwardDraft(new ShowroomAwardDraft(
                null, "AWARD-GEN-002", "缺语音奖项", "Narration Missing Award",
                "中文讲解", "English narration", "颁发单位", "2026",
                "/admin-api/infra/file/11/get/showroom/award/source-award.png"));
        ShowroomAwardRevision published = contentService.publishAwardRevision(draft.revisionId(), 300L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(300L);

            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> adminController.generateAwardCoverImage(
                            new ShowroomAdminController.AwardCoverGenerateReqVO(published.awardId())));

            assertTrue(error.getMessage().contains("AWARD_NARRATION_"));
            assertEquals(published.revisionId(), contentService.requireCurrentAwardRevision(published.awardId()).revisionId());
        }
    }

    @Test
    void generateAwardCoverImageShouldFailFastWhenCurrentCoverMissing() {
        ShowroomAwardRevision draft = contentService.saveAwardDraft(new ShowroomAwardDraft(
                null, "AWARD-GEN-003", "缺封面奖项", "Cover Missing Award",
                "中文讲解", "English narration", "颁发单位", "2026",
                "/admin-api/infra/file/11/get/showroom/award/source-award.png"));
        ShowroomAwardRevision published = contentService.publishAwardRevision(draft.revisionId(), 300L);
        ShowroomAwardRevisionDO persistedRevision = awardRevisionMapper.selectById(published.revisionId());
        persistedRevision.setCoverImage("");
        awardRevisionMapper.updateById(persistedRevision);
        publishNarration(ShowroomNarrationTargetType.AWARD, published.awardId(), published.revisionId(),
                ShowroomNarrationLanguage.ZH, published.fields().get("description_zh"), 2301L);
        publishNarration(ShowroomNarrationTargetType.AWARD, published.awardId(), published.revisionId(),
                ShowroomNarrationLanguage.EN, published.fields().get("description_en"), 2302L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(300L);

            IllegalStateException error = assertThrows(IllegalStateException.class,
                    () -> adminController.generateAwardCoverImage(
                            new ShowroomAdminController.AwardCoverGenerateReqVO(published.awardId())));

            assertTrue(error.getMessage().contains("SHOWROOM_AWARD_COVER_GENERATION_FAILED"));
        }
    }
}
