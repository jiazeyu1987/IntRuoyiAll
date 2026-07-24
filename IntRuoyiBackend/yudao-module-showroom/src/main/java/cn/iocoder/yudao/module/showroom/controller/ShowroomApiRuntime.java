package cn.iocoder.yudao.module.showroom.controller;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import cn.iocoder.yudao.module.ai.service.tts.AiTtsAliyunNlsCredentialService;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCommentAnchorType;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanySnapshot;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardSnapshot;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHall;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallItemMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHallProductMapping;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductComment;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductDraft;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductSnapshot;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomPersistentContentService;
import cn.iocoder.yudao.module.showroom.content.service.ShowroomProductCommentService;
import cn.iocoder.yudao.module.showroom.cover.ShowroomProductCoverBatchTaskService;
import cn.iocoder.yudao.module.showroom.cover.ShowroomAwardCoverImageService;
import cn.iocoder.yudao.module.showroom.cover.ShowroomProductCoverImageService;
import cn.iocoder.yudao.module.showroom.controller.admin.ShowroomAdminController;
import cn.iocoder.yudao.module.showroom.controller.admin.ShowroomProductImportMode;
import cn.iocoder.yudao.module.showroom.controller.admin.excel.ShowroomKeywordExcelImportRow;
import cn.iocoder.yudao.module.showroom.controller.admin.excel.ShowroomKeywordExcelRow;
import cn.iocoder.yudao.module.showroom.controller.admin.excel.ShowroomNarrationExcelImportRow;
import cn.iocoder.yudao.module.showroom.controller.admin.excel.ShowroomNarrationExcelRow;
import cn.iocoder.yudao.module.showroom.controller.admin.excel.ShowroomProductResourcePackage;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.award.AwardDetailRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.award.AwardDraftReqVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.award.AwardPageRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.award.AwardPublishReqVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.award.AwardCoverGenerateRespVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.award.ShowroomAwardExcelExportRow;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.award.ShowroomAwardExcelImportRow;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.product.ShowroomProductExcelVO;
import cn.iocoder.yudao.module.showroom.controller.admin.vo.product.ShowroomProductImportExtra;
import cn.iocoder.yudao.module.showroom.controller.display.ShowroomDisplayController;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetKey;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetDraftCommand;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetFiles;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetOperations;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetTargetType;
import cn.iocoder.yudao.module.showroom.asset.ShowroomPreviewAssetVersion;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductAttachment;
import cn.iocoder.yudao.module.showroom.dal.dataobject.asset.ShowroomPreviewAssetVersionDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.keyword.ShowroomKeywordDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomChangeRequestDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.asset.ShowroomPreviewAssetVersionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductRevisionRelationMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.keyword.ShowroomKeywordMapper;
import cn.iocoder.yudao.module.showroom.dal.dataobject.translate.ShowroomProductTranslatePublishBatchTaskDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.translate.ShowroomProductTranslatePublishBatchTaskItemDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.translate.ShowroomProductTranslatePublishBatchTaskItemMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.translate.ShowroomProductTranslatePublishBatchTaskMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestMapper;
import cn.iocoder.yudao.module.showroom.foundation.enums.ShowroomFieldTierEnum;
import cn.iocoder.yudao.module.showroom.foundation.meta.ShowroomFieldDisplaySupport;
import cn.iocoder.yudao.module.showroom.foundation.meta.ShowroomFieldCatalog;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationAudienceType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationAudioDraftCommand;
import cn.iocoder.yudao.module.showroom.narration.ShowroomCompanyNarrationCodexService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomCompanyNarrationTranslationService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationDraftCommand;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationKey;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationLanguage;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationOperations;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationStatus;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationTargetType;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationVersion;
import cn.iocoder.yudao.module.showroom.narration.ShowroomPersistentNarrationService;
import cn.iocoder.yudao.module.showroom.release.ShowroomReleaseAutoPublishService;
import cn.iocoder.yudao.module.showroom.release.ShowroomPublicReleaseReadbackVerifier;
import cn.iocoder.yudao.module.showroom.release.ShowroomReleasePublisherService;
import cn.iocoder.yudao.module.showroom.release.ShowroomVersionBundleService;
import cn.iocoder.yudao.module.showroom.narration.ShowroomProductNarrationCodexService;
import cn.iocoder.yudao.module.showroom.prompt.ShowroomImagePromptVersion;
import cn.iocoder.yudao.module.showroom.prompt.ShowroomImagePromptVersionService;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomVersionAudit;
import cn.iocoder.yudao.module.showroom.workflow.model.ShowroomAssignmentDetail;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomAssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class ShowroomApiRuntime {

    private static final Logger log = LoggerFactory.getLogger(ShowroomApiRuntime.class);

    private static final String TARGET_COMPANY = "COMPANY";
    private static final String TARGET_PRODUCT = "PRODUCT";
    private static final String TARGET_AWARD = "AWARD";
    private static final String TARGET_HALL = "HALL";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_IN_FILLING = "IN_FILLING";
    private static final String STATUS_PENDING_SUPERVISOR_REVIEW = "PENDING_SUPERVISOR_REVIEW";
    private static final String STATUS_PENDING_SUPERVISOR_APPROVAL = "PENDING_SUPERVISOR_APPROVAL";
    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_RESOLVED = "RESOLVED";
    private static final String PRODUCT_COVER_GENERATION_MODE_ALL = "ALL";
    private static final String PRODUCT_COVER_GENERATION_MODE_MISSING_ONLY = "MISSING_ONLY";
    private static final String PRODUCT_BATCH_AUDIO_AUTO_CHECK_CATEGORY = "showroom";
    private static final String PRODUCT_BATCH_AUDIO_AUTO_CHECK_ENABLED_KEY =
            "showroom.product.batch-audio-auto-check.enabled";
    private static final String PRODUCT_BATCH_AUDIO_AUTO_CHECK_FILTERS_KEY =
            "showroom.product.batch-audio-auto-check.filters";
    private static final String PRODUCT_BATCH_AUDIO_AUTO_CHECK_SUMMARY_KEY =
            "showroom.product.batch-audio-auto-check.summary";
    private static final String PRODUCT_BATCH_AUDIO_AUTO_CHECK_FAILURE_KEY =
            "showroom.product.batch-audio-auto-check.failure";
    private static final String PRODUCT_BATCH_AUDIO_AUTO_CHECK_ENABLED_NAME = "展厅产品批量语音自动检查开关";
    private static final String PRODUCT_BATCH_AUDIO_AUTO_CHECK_FILTERS_NAME = "展厅产品批量语音自动检查筛选";
    private static final String PRODUCT_BATCH_AUDIO_AUTO_CHECK_SUMMARY_NAME = "展厅产品批量语音自动检查汇总";
    private static final String PRODUCT_BATCH_AUDIO_AUTO_CHECK_FAILURE_NAME = "展厅产品批量语音自动检查失败";
    private static final String PRODUCT_BATCH_AUDIO_AUTO_CHECK_RUNNING_MESSAGE =
            "SHOWROOM_AUDIO_BATCH_RUNNING: 批量语音自动检查正在执行，请稍后重试";
    private static final long PRODUCT_TRANSLATE_PUBLISH_BATCH_STALE_GRACE_MS = 60000L;
    private static final String PRODUCT_BATCH_NARRATION_SCRIPT_ACTIVE_KEY =
            "showroom.product.batch-narration-script.active";
    private static final String PRODUCT_BATCH_NARRATION_SCRIPT_RUNNING_KEY =
            "showroom.product.batch-narration-script.running";
    private static final String PRODUCT_BATCH_NARRATION_SCRIPT_FILTERS_KEY =
            "showroom.product.batch-narration-script.filters";
    private static final String PRODUCT_BATCH_NARRATION_SCRIPT_SUMMARY_KEY =
            "showroom.product.batch-narration-script.summary";
    private static final String PRODUCT_BATCH_NARRATION_SCRIPT_FAILURE_KEY =
            "showroom.product.batch-narration-script.failure";
    private static final String PRODUCT_BATCH_NARRATION_SCRIPT_ACTIVE_NAME = "展厅产品批量讲解任务活动状态";
    private static final String PRODUCT_BATCH_NARRATION_SCRIPT_RUNNING_NAME = "展厅产品批量讲解任务运行状态";
    private static final String PRODUCT_BATCH_NARRATION_SCRIPT_FILTERS_NAME = "展厅产品批量讲解任务筛选";
    private static final String PRODUCT_BATCH_NARRATION_SCRIPT_SUMMARY_NAME = "展厅产品批量讲解任务汇总";
    private static final String PRODUCT_BATCH_NARRATION_SCRIPT_FAILURE_NAME = "展厅产品批量讲解任务失败";
    private static final String RUNTIME_CLIENT_SETTINGS_CONFIG_KEY = "showroom.runtime.client.settings";
    private static final String RUNTIME_CLIENT_SETTINGS_CONFIG_NAME = "展厅 Win7 客户端展示设置";
    private static final String RUNTIME_CLIENT_SETTINGS_CONFIG_REMARK =
            "Win7 展厅客户端公司详情设置入口保存的产品 item 横向 / 纵向间距。";
    private static final int RUNTIME_CLIENT_DEFAULT_PRODUCT_ITEM_GAP = 12;
    private static final int RUNTIME_CLIENT_PRODUCT_ITEM_GAP_MIN = 0;
    private static final int RUNTIME_CLIENT_PRODUCT_ITEM_GAP_MAX = 48;

    private static final List<String> COMPANY_FIELD_ORDER = List.of(
            "development_history",
            "park_introduction",
            "incubation_platform",
            "subsidiary_overview",
            "stock_info",
            "core_manufacturing_capability",
            "honors_awards"
    );
    private static final List<String> COMPANY_WEBSITE_FIELD_ORDER = List.of(
            "development_history",
            "park_introduction",
            "incubation_platform",
            "subsidiary_overview",
            "stock_info"
    );
    private static final List<String> PRODUCT_FIELD_ORDER = List.of(
            "owner_company_id",
            "product_owner_type",
            "lifecycle_stage",
            "target_market",
            "pipeline_layout",
            "indication_content",
            "core_selling_points",
            "model_specification"
    );
    private static final List<String> PRODUCT_TRANSLATABLE_FIELD_KEYS = List.of(
            "target_market",
            "pipeline_layout",
            "indication_content",
            "core_selling_points",
            "model_specification",
            "registration_certificate",
            "clinical_effect",
            "fim_status"
    );

    private record ProductBatchCoverTaskResult(boolean succeeded,
                                               ShowroomAdminController.ProductBatchGenerateFailureRespVO failure) {
    }
    private record ProductNarrationPair(ShowroomNarrationVersion zh, ShowroomNarrationVersion en) {
    }
    public record ProductBatchNarrationAutoCheckCriteria(String keyword, String lifecycleStage,
                                                         String incompleteStatus, String approvalStatus) {
    }
    public record ProductBatchNarrationAutoCheckSummaryData(int matchedCount, int publishedCount,
                                                            int skippedUnpublishedCount, int skippedExistingCount,
                                                            int skippedMissingScriptCount, int succeededCount,
                                                            int failedCount, int remainingActionableCount,
                                                            Long lastRunAt) {
    }
    public record ProductBatchNarrationAutoCheckFailureData(String message, Long lastFailureAt) {
    }
    private record ProductBatchNarrationAutoCheckState(boolean enabled,
                                                       ProductBatchNarrationAutoCheckCriteria criteria,
                                                       ProductBatchNarrationAutoCheckSummaryData summary,
                                                       ProductBatchNarrationAutoCheckFailureData failure) {
    }
    public record ProductNarrationScriptBatchTaskCriteria(String keyword, String lifecycleStage,
                                                          String incompleteStatus, String approvalStatus) {
    }
    public record ProductNarrationScriptBatchTaskCurrentProductData(Long productId, String productCode,
                                                                    String nameCn) {
    }
    public record ProductNarrationScriptBatchTaskSummaryData(int matchedCount, int skippedCompletedCount,
                                                             int generatedLanguageCount, int failedCount,
                                                             int remainingCount, Long startedAt,
                                                             Long lastRunAt, Long completedAt,
                                                             ProductNarrationScriptBatchTaskCurrentProductData currentProduct) {
    }
    public record ProductNarrationScriptBatchTaskFailureData(Long productId, String productCode, String nameCn,
                                                             String reason, Long lastFailureAt) {
    }
    private record ProductNarrationScriptBatchTaskState(boolean active, boolean running,
                                                        ProductNarrationScriptBatchTaskCriteria criteria,
                                                        ProductNarrationScriptBatchTaskSummaryData summary,
                                                        ProductNarrationScriptBatchTaskFailureData failure) {
    }
    private record ProductTranslatePublishBatchTaskCriteria(String keyword, String lifecycleStage,
                                                            String incompleteStatus, String approvalStatus) {
    }
    private record ProductTranslatePublishBatchTaskState(boolean active, boolean running,
                                                         ProductTranslatePublishBatchTaskCriteria criteria,
                                                         int matchedCount, int succeededCount, int failedCount,
                                                         int remainingCount, Long startedAt, Long lastRunAt,
                                                         Long completedAt,
                                                         ShowroomAdminController.ProductBatchTaskCurrentProductRespVO currentProduct,
                                                         ShowroomAdminController.ProductBatchGenerateFailureRespVO lastFailure,
                                                         Long lastFailureAt,
                                                         List<ShowroomAdminController.ProductBatchGenerateFailureRespVO> failures) {
    }
    private record ProductBatchNarrationExecutionSummary(int matchedCount, int publishedCount,
                                                         int skippedUnpublishedCount, int skippedExistingCount,
                                                         int skippedMissingScriptCount, int succeededCount,
                                                         int failedCount, int remainingActionableCount,
                                                         List<ShowroomAdminController.ProductBatchGenerateFailureRespVO> failures) {
    }
    private record ProductNarrationScriptBatchExecutionSummary(int matchedCount, int skippedCompletedCount,
                                                               int generatedLanguageCount, int failedCount,
                                                               int remainingCount,
                                                               List<ShowroomAdminController.ProductBatchGenerateFailureRespVO> failures) {
    }
    private record ProductNarrationBatchEvaluation(Long productId, Long sourceRevisionId, boolean existingReady,
                                                   boolean missingScript, boolean zhReady, boolean enReady,
                                                   ShowroomNarrationVersion zhLatest,
                                                   ShowroomNarrationVersion enLatest) {
    }
    private record ProductNarrationScriptBatchEvaluation(Long productId, Long sourceRevisionId, boolean zhReady,
                                                         boolean enReady, ShowroomNarrationVersion zhLatest,
                                                         ShowroomNarrationVersion enLatest) {
    }
    private static final String OWNER_COMPANY_LABEL = "瑛泰医疗";
    private static final String OWNER_TYPE_YINGTAI_CODE = "YINGTAI";
    private static final String OWNER_TYPE_SUBSIDIARY_CODE = "SUBSIDIARY";
    private static final String OWNER_TYPE_YINGTAI_TEXT = "盈泰产品";
    private static final String OWNER_TYPE_SUBSIDIARY_TEXT = "子公司产品";
    private static final String LIFECYCLE_REGISTERED_CODE = "REGISTERED";
    private static final String LIFECYCLE_REGISTERED_TEXT = "已注册";
    private static final String LIFECYCLE_R_AND_D_CODE = "R_AND_D";
    private static final String LIFECYCLE_R_AND_D_TEXT = "研发中";
    private static final List<String> PRODUCT_IMPORT_FIELD_KEYS = List.of(
            "product_owner_type",
            "lifecycle_stage",
            "target_market",
            "pipeline_layout",
            "indication_content",
            "core_selling_points",
            "model_specification",
            "registration_certificate",
            "clinical_effect",
            "fim_status",
            "cover_image"
    );
    private static final Pattern LEGACY_IMPORTED_COVER_FILE_NAME_PATTERN = Pattern.compile(
            "^product-.+-imported-cover\\.[A-Za-z0-9]+$");

    private final ShowroomPersistentContentService contentService;
    private final ShowroomProductCommentService commentService;
    private final ShowroomProductCoverBatchTaskService productCoverBatchTaskService;
    private final ShowroomProductCoverImageService productCoverImageService;
    private final ShowroomAwardCoverImageService awardCoverImageService;
    private final ShowroomImagePromptVersionService imagePromptVersionService;
    private final ShowroomNarrationOperations narrationService;
    private final ShowroomCompanyNarrationCodexService narrationCodexService;
    private final ShowroomCompanyNarrationTranslationService narrationTranslationService;
    private final ShowroomProductNarrationCodexService productNarrationCodexService;
    private final ShowroomPreviewAssetOperations previewAssetService;
    private final ShowroomPreviewAssetVersionMapper previewAssetVersionMapper;
    private final FileMapper fileMapper;
    private final FileService fileService;
    private final ShowroomKeywordMapper keywordMapper;
    private final ConfigService configService;
    private final AiTtsAliyunNlsCredentialService aliyunNlsCredentialService;
    private final YudaoAiProperties yudaoAiProperties;
    private final ShowroomProductRevisionRelationMapper productRevisionRelationMapper;
    private final ShowroomProductTranslatePublishBatchTaskMapper translatePublishBatchTaskMapper;
    private final ShowroomProductTranslatePublishBatchTaskItemMapper translatePublishBatchTaskItemMapper;
    private final ShowroomChangeRequestMapper changeRequestMapper;
    private final ShowroomAssignmentService assignmentService;
    private final ShowroomVersionBundleService versionBundleService;
    private final ShowroomReleasePublisherService releasePublisherService;
    private final ShowroomReleaseAutoPublishService releaseAutoPublishService;
    private final ShowroomPublicReleaseReadbackVerifier publicReleaseReadbackVerifier;
    private final ReentrantLock productBatchNarrationAutoCheckLock = new ReentrantLock();
    private final ReentrantLock productBatchNarrationScriptTaskLock = new ReentrantLock();
    private final ReentrantLock productTranslatePublishBatchTaskLock = new ReentrantLock();
    private volatile ProductTranslatePublishBatchTaskState productTranslatePublishBatchTaskState =
            emptyProductTranslatePublishBatchTaskState();

    @Autowired
    public ShowroomApiRuntime(ShowroomPersistentContentService contentService,
                              ShowroomProductCommentService commentService,
                              ShowroomProductCoverBatchTaskService productCoverBatchTaskService,
                              ShowroomProductCoverImageService productCoverImageService,
                              ObjectProvider<ShowroomAwardCoverImageService> awardCoverImageServiceProvider,
                              ShowroomImagePromptVersionService imagePromptVersionService,
                              ShowroomPersistentNarrationService narrationService,
                              ShowroomCompanyNarrationCodexService narrationCodexService,
                              ShowroomCompanyNarrationTranslationService narrationTranslationService,
                              ShowroomProductNarrationCodexService productNarrationCodexService,
                              ShowroomPreviewAssetOperations previewAssetService,
                              ShowroomPreviewAssetVersionMapper previewAssetVersionMapper,
                              FileMapper fileMapper,
                              FileService fileService,
                              ShowroomKeywordMapper keywordMapper,
                              ConfigService configService,
                              AiTtsAliyunNlsCredentialService aliyunNlsCredentialService,
                              YudaoAiProperties yudaoAiProperties,
                              ShowroomProductRevisionRelationMapper productRevisionRelationMapper,
                              ObjectProvider<ShowroomProductTranslatePublishBatchTaskMapper> translatePublishBatchTaskMapperProvider,
                              ObjectProvider<ShowroomProductTranslatePublishBatchTaskItemMapper> translatePublishBatchTaskItemMapperProvider,
                              ShowroomChangeRequestMapper changeRequestMapper,
                              ShowroomAssignmentService assignmentService,
                              ShowroomVersionBundleService versionBundleService,
                              ObjectProvider<ShowroomReleasePublisherService> releasePublisherServiceProvider,
                              ObjectProvider<ShowroomReleaseAutoPublishService> releaseAutoPublishServiceProvider,
                              ObjectProvider<ShowroomPublicReleaseReadbackVerifier> publicReleaseReadbackVerifierProvider) {
        this(contentService, commentService, productCoverBatchTaskService, productCoverImageService,
                awardCoverImageServiceProvider == null ? null : awardCoverImageServiceProvider.getIfAvailable(),
                imagePromptVersionService,
                narrationService, narrationCodexService, narrationTranslationService, productNarrationCodexService,
                previewAssetService, previewAssetVersionMapper, fileMapper, fileService, keywordMapper, configService,
                aliyunNlsCredentialService, yudaoAiProperties, productRevisionRelationMapper,
                translatePublishBatchTaskMapperProvider, translatePublishBatchTaskItemMapperProvider,
                changeRequestMapper, assignmentService, versionBundleService,
                releasePublisherServiceProvider, releaseAutoPublishServiceProvider, publicReleaseReadbackVerifierProvider);
    }

    public ShowroomApiRuntime(ShowroomPersistentContentService contentService,
                              ShowroomProductCommentService commentService,
                              ShowroomProductCoverBatchTaskService productCoverBatchTaskService,
                              ShowroomProductCoverImageService productCoverImageService,
                              ShowroomImagePromptVersionService imagePromptVersionService,
                              ShowroomPersistentNarrationService narrationService,
                              ShowroomCompanyNarrationCodexService narrationCodexService,
                              ShowroomCompanyNarrationTranslationService narrationTranslationService,
                              ShowroomProductNarrationCodexService productNarrationCodexService,
                              ShowroomPreviewAssetOperations previewAssetService,
                              ShowroomPreviewAssetVersionMapper previewAssetVersionMapper,
                              FileMapper fileMapper,
                              ConfigService configService,
                              AiTtsAliyunNlsCredentialService aliyunNlsCredentialService,
                              YudaoAiProperties yudaoAiProperties,
                              ShowroomProductRevisionRelationMapper productRevisionRelationMapper,
                              ShowroomChangeRequestMapper changeRequestMapper,
                              ShowroomAssignmentService assignmentService,
                              ShowroomVersionBundleService versionBundleService) {
        this(contentService, commentService, productCoverBatchTaskService, productCoverImageService,
                imagePromptVersionService, narrationService, narrationCodexService, narrationTranslationService,
                productNarrationCodexService, previewAssetService, previewAssetVersionMapper, fileMapper,
                null, null, configService, aliyunNlsCredentialService, yudaoAiProperties,
                productRevisionRelationMapper, changeRequestMapper, assignmentService, versionBundleService);
    }

    public ShowroomApiRuntime(ShowroomPersistentContentService contentService,
                              ShowroomProductCommentService commentService,
                              ShowroomProductCoverBatchTaskService productCoverBatchTaskService,
                              ShowroomProductCoverImageService productCoverImageService,
                              ShowroomImagePromptVersionService imagePromptVersionService,
                              ShowroomPersistentNarrationService narrationService,
                              ShowroomCompanyNarrationCodexService narrationCodexService,
                              ShowroomCompanyNarrationTranslationService narrationTranslationService,
                              ShowroomProductNarrationCodexService productNarrationCodexService,
                              ShowroomPreviewAssetOperations previewAssetService,
                              ShowroomPreviewAssetVersionMapper previewAssetVersionMapper,
                              FileMapper fileMapper,
                              FileService fileService,
                              ShowroomKeywordMapper keywordMapper,
                              ConfigService configService,
                              AiTtsAliyunNlsCredentialService aliyunNlsCredentialService,
                              YudaoAiProperties yudaoAiProperties,
                              ShowroomProductRevisionRelationMapper productRevisionRelationMapper,
                              ShowroomChangeRequestMapper changeRequestMapper,
                              ShowroomAssignmentService assignmentService,
                              ShowroomVersionBundleService versionBundleService) {
        this(contentService, commentService, productCoverBatchTaskService, productCoverImageService,
                (ShowroomAwardCoverImageService) null,
                imagePromptVersionService,
                narrationService, narrationCodexService, narrationTranslationService, productNarrationCodexService,
                previewAssetService, previewAssetVersionMapper, fileMapper, fileService, keywordMapper, configService,
                aliyunNlsCredentialService, yudaoAiProperties, productRevisionRelationMapper,
                changeRequestMapper, assignmentService, versionBundleService);
    }

    public ShowroomApiRuntime(ShowroomPersistentContentService contentService,
                              ShowroomProductCommentService commentService,
                              ShowroomProductCoverBatchTaskService productCoverBatchTaskService,
                              ShowroomProductCoverImageService productCoverImageService,
                              ShowroomAwardCoverImageService awardCoverImageService,
                              ShowroomImagePromptVersionService imagePromptVersionService,
                              ShowroomPersistentNarrationService narrationService,
                              ShowroomCompanyNarrationCodexService narrationCodexService,
                              ShowroomCompanyNarrationTranslationService narrationTranslationService,
                              ShowroomProductNarrationCodexService productNarrationCodexService,
                              ShowroomPreviewAssetOperations previewAssetService,
                              ShowroomPreviewAssetVersionMapper previewAssetVersionMapper,
                              FileMapper fileMapper,
                              ConfigService configService,
                              AiTtsAliyunNlsCredentialService aliyunNlsCredentialService,
                              YudaoAiProperties yudaoAiProperties,
                              ShowroomProductRevisionRelationMapper productRevisionRelationMapper,
                              ShowroomChangeRequestMapper changeRequestMapper,
                              ShowroomAssignmentService assignmentService,
                              ShowroomVersionBundleService versionBundleService) {
        this(contentService, commentService, productCoverBatchTaskService, productCoverImageService,
                awardCoverImageService, imagePromptVersionService, narrationService, narrationCodexService,
                narrationTranslationService, productNarrationCodexService, previewAssetService,
                previewAssetVersionMapper, fileMapper, null, null, configService,
                aliyunNlsCredentialService, yudaoAiProperties, productRevisionRelationMapper,
                changeRequestMapper, assignmentService, versionBundleService);
    }

    public ShowroomApiRuntime(ShowroomPersistentContentService contentService,
                              ShowroomProductCommentService commentService,
                              ShowroomProductCoverBatchTaskService productCoverBatchTaskService,
                              ShowroomProductCoverImageService productCoverImageService,
                              ShowroomAwardCoverImageService awardCoverImageService,
                              ShowroomImagePromptVersionService imagePromptVersionService,
                              ShowroomPersistentNarrationService narrationService,
                              ShowroomCompanyNarrationCodexService narrationCodexService,
                              ShowroomCompanyNarrationTranslationService narrationTranslationService,
                              ShowroomProductNarrationCodexService productNarrationCodexService,
                              ShowroomPreviewAssetOperations previewAssetService,
                              ShowroomPreviewAssetVersionMapper previewAssetVersionMapper,
                              FileMapper fileMapper,
                              FileService fileService,
                              ShowroomKeywordMapper keywordMapper,
                              ConfigService configService,
                              AiTtsAliyunNlsCredentialService aliyunNlsCredentialService,
                               YudaoAiProperties yudaoAiProperties,
                               ShowroomProductRevisionRelationMapper productRevisionRelationMapper,
                               ObjectProvider<ShowroomProductTranslatePublishBatchTaskMapper> translatePublishBatchTaskMapperProvider,
                               ObjectProvider<ShowroomProductTranslatePublishBatchTaskItemMapper> translatePublishBatchTaskItemMapperProvider,
                               ShowroomChangeRequestMapper changeRequestMapper,
                              ShowroomAssignmentService assignmentService,
                              ShowroomVersionBundleService versionBundleService,
                              ObjectProvider<ShowroomReleasePublisherService> releasePublisherServiceProvider,
                              ObjectProvider<ShowroomReleaseAutoPublishService> releaseAutoPublishServiceProvider,
                              ObjectProvider<ShowroomPublicReleaseReadbackVerifier> publicReleaseReadbackVerifierProvider) {
        this.contentService = contentService;
        this.commentService = commentService;
        this.productCoverBatchTaskService = productCoverBatchTaskService;
        this.productCoverImageService = productCoverImageService;
        this.awardCoverImageService = awardCoverImageService;
        this.imagePromptVersionService = imagePromptVersionService;
        this.narrationService = narrationService;
        this.narrationCodexService = narrationCodexService;
        this.narrationTranslationService = narrationTranslationService;
        this.productNarrationCodexService = productNarrationCodexService;
        this.previewAssetService = previewAssetService;
        this.previewAssetVersionMapper = previewAssetVersionMapper;
        this.fileMapper = fileMapper;
        this.fileService = fileService;
        this.keywordMapper = keywordMapper;
        this.configService = configService;
        this.aliyunNlsCredentialService = aliyunNlsCredentialService;
        this.yudaoAiProperties = yudaoAiProperties;
        this.productRevisionRelationMapper = productRevisionRelationMapper;
        this.translatePublishBatchTaskMapper = translatePublishBatchTaskMapperProvider == null ? null
                : translatePublishBatchTaskMapperProvider.getIfAvailable();
        this.translatePublishBatchTaskItemMapper = translatePublishBatchTaskItemMapperProvider == null ? null
                : translatePublishBatchTaskItemMapperProvider.getIfAvailable();
        this.changeRequestMapper = changeRequestMapper;
        this.assignmentService = assignmentService;
        this.versionBundleService = versionBundleService;
        this.releasePublisherService = releasePublisherServiceProvider == null ? null
                : releasePublisherServiceProvider.getIfAvailable();
        this.releaseAutoPublishService = releaseAutoPublishServiceProvider == null ? null
                : releaseAutoPublishServiceProvider.getIfAvailable();
        this.publicReleaseReadbackVerifier = publicReleaseReadbackVerifierProvider == null ? null
                : publicReleaseReadbackVerifierProvider.getIfAvailable();
    }

    public ShowroomApiRuntime(ShowroomPersistentContentService contentService,
                              ShowroomProductCommentService commentService,
                              ShowroomProductCoverBatchTaskService productCoverBatchTaskService,
                              ShowroomProductCoverImageService productCoverImageService,
                              ShowroomAwardCoverImageService awardCoverImageService,
                              ShowroomImagePromptVersionService imagePromptVersionService,
                              ShowroomPersistentNarrationService narrationService,
                              ShowroomCompanyNarrationCodexService narrationCodexService,
                              ShowroomCompanyNarrationTranslationService narrationTranslationService,
                              ShowroomProductNarrationCodexService productNarrationCodexService,
                              ShowroomPreviewAssetOperations previewAssetService,
                              ShowroomPreviewAssetVersionMapper previewAssetVersionMapper,
                              FileMapper fileMapper,
                              FileService fileService,
                              ShowroomKeywordMapper keywordMapper,
                              ConfigService configService,
                              AiTtsAliyunNlsCredentialService aliyunNlsCredentialService,
                              YudaoAiProperties yudaoAiProperties,
                              ShowroomProductRevisionRelationMapper productRevisionRelationMapper,
                              ShowroomChangeRequestMapper changeRequestMapper,
                              ShowroomAssignmentService assignmentService,
                              ShowroomVersionBundleService versionBundleService) {
        this(contentService, commentService, productCoverBatchTaskService, productCoverImageService,
                awardCoverImageService,
                imagePromptVersionService,
                narrationService, narrationCodexService, narrationTranslationService, productNarrationCodexService,
                previewAssetService, previewAssetVersionMapper, fileMapper, fileService, keywordMapper, configService,
                aliyunNlsCredentialService, yudaoAiProperties, productRevisionRelationMapper,
                null, null, changeRequestMapper, assignmentService, versionBundleService,
                null, null, null);
    }

    public ShowroomCompanyRevision saveCompanyDraft(ShowroomAdminController.CompanyDraftReqVO req) {
        return contentService.saveCompanyDraft(new ShowroomCompanyDraft(req.companyId(), req.companyType(),
                req.displayName(), req.displayNameEn(),
                requireMap(req.fields(), "SHOWROOM_REQUIRED_FIELD_MISSING: company fields are required")));
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomAdminController.CompanyCurrentRespVO publishCompany(ShowroomAdminController.CompanyDraftReqVO req,
                                                                       Long operatorUserId) {
        ShowroomCompanyRevision draft = saveCompanyDraft(req);
        carryForwardLiveCompanyNarrationsIfPresent(draft.companyId(), draft.revisionId());
        ShowroomCompanyRevision published = contentService.publishCompanyRevision(draft.revisionId(), operatorUserId);
        if (hasPublishedCompanyNarrationPair(published.companyId(), published.revisionId())) {
            Long releasePreviewAssetVersionId = previewAssetService.live(new ShowroomPreviewAssetKey(
                            ShowroomPreviewAssetTargetType.COMPANY, published.companyId()))
                    .map(version -> version.id())
                    .orElse(null);
            versionBundleService.ensureBundleForPublishedRevision(TARGET_COMPANY, published.companyId(),
                    published.revisionId(), operatorUserId, null, releasePreviewAssetVersionId);
        }
        ShowroomCompanySnapshot snapshot = contentService.getCompany(published.companyId());
        return toCompanyCurrentResp(snapshot, published);
    }

    private boolean hasPublishedCompanyNarrationPair(Long companyId, Long revisionId) {
        return hasPublishedCompanyNarration(companyId, revisionId, ShowroomNarrationLanguage.ZH)
                && hasPublishedCompanyNarration(companyId, revisionId, ShowroomNarrationLanguage.EN);
    }

    private boolean hasPublishedCompanyNarration(Long companyId, Long revisionId,
                                                 ShowroomNarrationLanguage language) {
        return narrationService.live(new ShowroomNarrationKey(
                        ShowroomNarrationTargetType.COMPANY, companyId,
                        ShowroomNarrationAudienceType.PUBLIC, language))
                .filter(version -> revisionId.equals(version.sourceRevisionId()))
                .isPresent();
    }

    public ShowroomAdminController.CompanyCurrentRespVO getCompanyCurrent() {
        ShowroomCompanyRevision revision = contentService.findCurrentOrLatestCompanyRevision().orElse(null);
        if (revision == null) {
            return new ShowroomAdminController.CompanyCurrentRespVO(0L, null, 0, "DRAFT", Map.of(),
                    "", "", "", false);
        }
        ShowroomCompanySnapshot snapshot = contentService.getCompany(revision.companyId());
        return toCompanyCurrentResp(snapshot, revision);
    }

    public ShowroomAdminController.ReleasePublishRespVO publishRelease(Long operatorUserId) {
        throw new IllegalStateException("SHOWROOM_SITE_SELECTOR_REQUIRED: siteKey and stage are required");
    }

    public ShowroomAdminController.ReleasePublishRespVO publishRelease(ShowroomAdminController.ReleasePublishReqVO req,
                                                                       Long operatorUserId) {
        if (releasePublisherService == null) {
            throw new IllegalStateException(
                    "SHOWROOM_RELEASE_PUBLISHER_UNAVAILABLE: release publisher bean is required");
        }
        if (publicReleaseReadbackVerifier == null) {
            throw new IllegalStateException(
                    "SHOWROOM_RELEASE_PUBLIC_READBACK_UNAVAILABLE: public readback verifier bean is required");
        }
        var release = releaseAutoPublishService == null
                ? releasePublisherService.publishRelease(operatorUserId, Instant.now(), req.siteKey(), req.stage())
                : releaseAutoPublishService.publishNow(operatorUserId, Instant.now(), req.siteKey(), req.stage());
        publicReleaseReadbackVerifier.verify(req.siteKey(), req.stage(), release.releaseId(), release.manifestHash(),
                release.rootDocumentId());
        return new ShowroomAdminController.ReleasePublishRespVO(
                release.releaseId(),
                release.manifestHash(),
                release.rootDocumentId(),
                release.documentCount(),
                release.assetCount(),
                release.installBytes(),
                release.publishedAt());
    }

    public ShowroomAdminController.CompanyCurrentRespVO getCompany(Long companyId, Long revisionId) {
        Long resolvedCompanyId = requireId(companyId, "SHOWROOM_TARGET_NOT_FOUND: company id is required");
        ShowroomCompanySnapshot snapshot = contentService.getCompany(resolvedCompanyId);
        if (revisionId == null) {
            ShowroomCompanyRevision currentOrLatest = contentService.findCurrentOrLatestCompanyRevision()
                    .orElseThrow(() -> new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: company revision not found"));
            if (!resolvedCompanyId.equals(currentOrLatest.companyId())) {
                throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: company revision does not belong to company");
            }
            return toCompanyCurrentResp(snapshot, currentOrLatest);
        }
        ShowroomCompanyRevision revision = contentService.getCompanyRevision(revisionId);
        if (!resolvedCompanyId.equals(revision.companyId())) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: company revision does not belong to company");
        }
        return toCompanyCurrentResp(snapshot, revision);
    }

    public ShowroomAdminController.ImagePromptCurrentRespVO getImagePromptCurrent(String sceneCode) {
        return toImagePromptCurrentResp(imagePromptVersionService.requireCurrent(sceneCode));
    }

    public List<ShowroomAdminController.ImagePromptHistoryItemRespVO> getImagePromptHistory(String sceneCode) {
        ShowroomImagePromptVersion currentVersion = imagePromptVersionService.requireCurrent(sceneCode);
        return imagePromptVersionService.history(sceneCode).stream()
                .map(version -> toImagePromptHistoryItemResp(version, Objects.equals(version.id(), currentVersion.id())))
                .toList();
    }

    public ShowroomAdminController.ImagePromptCurrentRespVO saveImagePromptVersion(
            ShowroomAdminController.ImagePromptVersionSaveReqVO req) {
        return toImagePromptCurrentResp(
                imagePromptVersionService.saveNewVersion(req.sceneCode(), req.templateText(), req.changeNote()));
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomAdminController.CompanyCurrentRespVO restoreCompanyRevision(
            ShowroomAdminController.CompanyRevisionRestoreReqVO req, Long operatorUserId) {
        Long companyId = requireId(req.companyId(), "SHOWROOM_TARGET_NOT_FOUND: company id is required");
        Long sourceRevisionId = requireId(req.sourceRevisionId(),
                "SHOWROOM_TARGET_NOT_FOUND: company source revision id is required");
        ShowroomCompanySnapshot snapshot = contentService.getCompany(companyId);
        ShowroomCompanyRevision sourceRevision = contentService.getCompanyRevision(sourceRevisionId);
        if (!companyId.equals(sourceRevision.companyId())) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: company restore source revision mismatch");
        }
        ShowroomCompanyRevision draft = contentService.saveCompanyDraft(new ShowroomCompanyDraft(companyId,
                snapshot.companyType(), snapshot.displayName(), snapshot.displayNameEn(), sourceRevision.fields()));
        carryForwardCompanyNarrationsForRevisionIfPresent(companyId, sourceRevisionId, draft.revisionId());
        ShowroomCompanyRevision published = contentService.publishCompanyRevision(draft.revisionId(), operatorUserId);
        ShowroomCompanySnapshot refreshedSnapshot = contentService.getCompany(companyId);
        return toCompanyCurrentResp(refreshedSnapshot, published);
    }

    public ShowroomAdminController.CompanyNarrationScriptGenerateRespVO generateCompanyNarrationScript(
            ShowroomAdminController.CompanyNarrationScriptGenerateReqVO req) {
        ShowroomCompanySnapshot company = contentService.getCompany(requireId(req.companyId(),
                "SHOWROOM_TARGET_NOT_FOUND: company id is required"));
        Long sourceRevisionId = requireId(req.sourceRevisionId(),
                "SHOWROOM_TARGET_NOT_FOUND: company source revision id is required");
        ShowroomCompanyRevision sourceRevision = contentService.getCompanyRevision(sourceRevisionId);
        if (!sourceRevision.companyId().equals(company.companyId())) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: company narration source revision mismatch");
        }
        String companyType = requireText(req.companyType(),
                "SHOWROOM_SCRIPT_GENERATION_FAILED: company type is required");
        String displayName = requireText(req.displayName(),
                "SHOWROOM_SCRIPT_GENERATION_FAILED: company display name is required");
        Map<String, String> fields = requireMap(req.fields(),
                "SHOWROOM_SCRIPT_GENERATION_FAILED: company fields are required");
        int targetLength = requirePositiveInt(req.targetLength(),
                "SHOWROOM_SCRIPT_GENERATION_FAILED: company narration target length is required");
        String introTextZh = narrationCodexService.generateScript(companyType, displayName, fields, targetLength);
        return new ShowroomAdminController.CompanyNarrationScriptGenerateRespVO(company.companyId(), sourceRevisionId,
                introTextZh);
    }

    public ShowroomAdminController.CompanyFieldTranslateRespVO translateCompanyFieldsToEn(
            ShowroomAdminController.CompanyFieldTranslateReqVO req) {
        ShowroomCompanySnapshot company = contentService.getCompany(requireId(req.companyId(),
                "SHOWROOM_TARGET_NOT_FOUND: company id is required"));
        List<String> fieldCodes = requireList(req.fieldCodes(),
                "SHOWROOM_TRANSLATION_FAILED: company field codes are required");
        Map<String, String> fields = requireMap(req.fields(),
                "SHOWROOM_TRANSLATION_FAILED: company fields are required");

        LinkedHashMap<String, String> translatedFields = new LinkedHashMap<>();
        for (String fieldCode : fieldCodes) {
            String normalizedFieldCode = requireText(fieldCode,
                    "SHOWROOM_TRANSLATION_FAILED: company field code is required");
            String sourceText = nullToEmpty(fields.get(normalizedFieldCode)).trim();
            if (!hasText(sourceText)) {
                continue;
            }
            translatedFields.put(companyEnglishFieldKey(normalizedFieldCode),
                    narrationTranslationService.translateZhToEn(sourceText));
        }
        String introTextEn = hasText(req.introTextZh())
                ? narrationTranslationService.translateZhToEn(req.introTextZh().trim())
                : "";
        if (translatedFields.isEmpty() && !hasText(introTextEn)) {
            throw new IllegalStateException("SHOWROOM_TRANSLATION_FAILED: at least one company field text is required");
        }
        return new ShowroomAdminController.CompanyFieldTranslateRespVO(company.companyId(), Map.copyOf(translatedFields),
                introTextEn);
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomAdminController.CompanyNarrationGenerateRespVO generateCompanyNarrationAudio(
            ShowroomAdminController.CompanyNarrationGenerateReqVO req) {
        ShowroomCompanySnapshot company = contentService.getCompany(requireId(req.companyId(),
                "SHOWROOM_TARGET_NOT_FOUND: company id is required"));
        Long sourceRevisionId = requireId(req.sourceRevisionId(),
                "SHOWROOM_TARGET_NOT_FOUND: company source revision id is required");
        ShowroomCompanyRevision sourceRevision = contentService.getCompanyRevision(sourceRevisionId);
        if (!sourceRevision.companyId().equals(company.companyId())) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: company narration source revision mismatch");
        }
        ShowroomNarrationLanguage language = ShowroomNarrationLanguage.valueOf(requireText(req.language(),
                "SHOWROOM_SCRIPT_MISSING: company narration language is required"));
        String scriptText = requireText(req.scriptText(),
                "SHOWROOM_SCRIPT_MISSING: company narration text is required");

        ShowroomNarrationVersion draft = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.COMPANY, company.companyId(), sourceRevisionId,
                ShowroomNarrationAudienceType.PUBLIC, language, scriptText, false));
        ShowroomNarrationVersion generated = narrationService.generateAudio(draft.id());

        return new ShowroomAdminController.CompanyNarrationGenerateRespVO(company.companyId(), sourceRevisionId,
                scriptText, toCompanyNarrationVersionResp(generated), nullToEmpty(generated.voice()));
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomAdminController.CompanyNarrationPublishRespVO publishCompanyNarration(
            ShowroomAdminController.CompanyNarrationPublishReqVO req) {
        if (req.zhNarrationVersionId() == null && req.enNarrationVersionId() == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: at least one company narration version id is required");
        }
        ShowroomNarrationVersion zhNarration = req.zhNarrationVersionId() == null ? null
                : narrationService.version(requireId(req.zhNarrationVersionId(),
                "SHOWROOM_TARGET_NOT_FOUND: zh narration version id is required"));
        ShowroomNarrationVersion enNarration = req.enNarrationVersionId() == null ? null
                : narrationService.version(requireId(req.enNarrationVersionId(),
                "SHOWROOM_TARGET_NOT_FOUND: en narration version id is required"));
        validateCompanyNarrationPublishRequest(zhNarration, enNarration);
        ShowroomNarrationVersion publishedZh = zhNarration == null ? null : narrationService.publishDirectly(zhNarration.id());
        ShowroomNarrationVersion publishedEn = enNarration == null ? null : narrationService.publishDirectly(enNarration.id());
        markReleaseDirtyForNarrationIfCurrent(publishedZh, null, "COMPANY_PUBLIC_NARRATION_PUBLISHED");
        markReleaseDirtyForNarrationIfCurrent(publishedEn, null, "COMPANY_PUBLIC_NARRATION_PUBLISHED");
        Long companyId = publishedZh != null ? publishedZh.key().targetId() : publishedEn.key().targetId();
        return new ShowroomAdminController.CompanyNarrationPublishRespVO(companyId,
                publishedZh == null ? null : publishedZh.id(),
                publishedEn == null ? null : publishedEn.id());
    }

    public ShowroomProductRevision saveProductDraft(ShowroomAdminController.ProductDraftReqVO req) {
        return contentService.saveProductDraft(new ShowroomProductDraft(req.productId(), req.productMasterId(),
                req.productCode(), req.nameCn(), req.nameEn(), req.legacyProductCode(), requireMap(req.fields(),
                "SHOWROOM_REQUIRED_FIELD_MISSING: product fields are required"),
                toProductAttachments(req.attachments())));
    }

    public ShowroomAdminController.ProductFieldTranslateRespVO translateProductFieldsToEn(
            ShowroomAdminController.ProductFieldTranslateReqVO req) {
        Long productId = requireId(req.productId(), "SHOWROOM_TARGET_NOT_FOUND: product id is required");
        contentService.getProduct(productId);
        Map<String, String> fields = requireMap(req.fields(),
                "SHOWROOM_TRANSLATION_FAILED: product fields are required");

        boolean hasTranslatableContent = false;
        String nameEn = null;
        String nameCn = nullToEmpty(req.nameCn()).trim();
        if (hasText(nameCn)) {
            hasTranslatableContent = true;
            nameEn = productNarrationCodexService.translateZhToEn(nameCn);
        }

        LinkedHashMap<String, String> translatedFields = new LinkedHashMap<>();
        for (String fieldCode : PRODUCT_TRANSLATABLE_FIELD_KEYS) {
            String sourceText = nullToEmpty(fields.get(fieldCode)).trim();
            if (!hasText(sourceText)) {
                continue;
            }
            hasTranslatableContent = true;
            translatedFields.put(productEnglishFieldKey(fieldCode),
                    productNarrationCodexService.translateZhToEn(sourceText));
        }

        String narrationScriptZh = nullToEmpty(req.narrationScriptZh()).trim();
        String narrationScriptEn = null;
        if (hasText(narrationScriptZh)) {
            hasTranslatableContent = true;
            narrationScriptEn = productNarrationCodexService.translateZhToEn(narrationScriptZh);
        }

        if (!hasTranslatableContent) {
            throw new IllegalStateException("SHOWROOM_TRANSLATION_FAILED: at least one product chinese text is required");
        }
        return new ShowroomAdminController.ProductFieldTranslateRespVO(productId, nullToEmpty(nameEn),
                Map.copyOf(translatedFields), narrationScriptEn);
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomAdminController.ProductDetailRespVO publishProduct(ShowroomAdminController.ProductPublishReqVO req,
                                                                     Long operatorUserId) {
        validateProductPublishCoreFields(req);
        ShowroomProductRevision savedDraft = saveProductDraft(new ShowroomAdminController.ProductDraftReqVO(
                req.productId(), req.productMasterId(), req.productCode(), req.nameCn(), req.nameEn(), req.fields(),
                req.attachments()));
        ShowroomProductRevision published = contentService.publishProductRevision(savedDraft.revisionId(),
                operatorUserId);
        if (shouldPublishProductNarration(req, published.productId())) {
            Long narrationSourceRevisionId = resolveProductNarrationSourceRevisionId(published.productId(),
                    req.sourceRevisionId());
            ProductNarrationPair sourcePair = prepareProductNarrationPairForRevision(published.productId(),
                    narrationSourceRevisionId);
            ProductNarrationPair draftPair = draftProductNarrationPair(published.productId(),
                    published.revisionId(), sourcePair);
            ProductNarrationPair publishPair = canCarryForwardProductNarrationAudio(published.productId(), sourcePair)
                    ? carryForwardProductNarrationAudioPair(draftPair, sourcePair)
                    : generateProductNarrationAudioPair(draftPair);
            publishProductNarrationPair(publishPair);
        }
        versionBundleService.ensureBundleForPublishedRevision(TARGET_PRODUCT, published.productId(),
                published.revisionId(), operatorUserId, null);
        assignmentService.markWholeProductAssignmentDirectPublished(published.productId(), operatorUserId,
                published.revisionId());
        return buildProductDetail(contentService.getProduct(published.productId()), published, true);
    }

    private void validateImportedNewProductPublishFields(ShowroomAdminController.ProductDraftReqVO req) {
        requireText(req.productCode(), "SHOWROOM_REQUIRED_FIELD_MISSING: product code is required");
        requireText(req.nameCn(), "SHOWROOM_REQUIRED_FIELD_MISSING: product name_cn is required");
        requireText(req.nameEn(), "SHOWROOM_REQUIRED_FIELD_MISSING: product name_en is required");
        Map<String, String> fields = requireMap(req.fields(), "SHOWROOM_REQUIRED_FIELD_MISSING: product fields are required");
        String lifecycleStage = requireText(fields.get("lifecycle_stage"),
                "SHOWROOM_REQUIRED_FIELD_MISSING: lifecycle_stage is required");
        if (LIFECYCLE_R_AND_D_CODE.equalsIgnoreCase(lifecycleStage)) {
            return;
        }
        requireText(fields.get("owner_company_id"), "SHOWROOM_REQUIRED_FIELD_MISSING: product owner_company_id is required");
        requireText(fields.get("product_owner_type"), "SHOWROOM_REQUIRED_FIELD_MISSING: product_owner_type is required");
    }

    private void validateProductPublishCoreFields(ShowroomAdminController.ProductPublishReqVO req) {
        requireId(req.productId(), "SHOWROOM_TARGET_NOT_FOUND: product id is required");
        requireText(req.productCode(), "SHOWROOM_REQUIRED_FIELD_MISSING: product code is required");
        requireText(req.nameCn(), "SHOWROOM_REQUIRED_FIELD_MISSING: product name_cn is required");
        requireText(req.nameEn(), "SHOWROOM_REQUIRED_FIELD_MISSING: product name_en is required");
        Map<String, String> fields = requireMap(req.fields(), "SHOWROOM_REQUIRED_FIELD_MISSING: product fields are required");
        requireText(fields.get("owner_company_id"), "SHOWROOM_REQUIRED_FIELD_MISSING: product owner_company_id is required");
        requireText(fields.get("product_owner_type"), "SHOWROOM_REQUIRED_FIELD_MISSING: product_owner_type is required");
        requireText(fields.get("lifecycle_stage"), "SHOWROOM_REQUIRED_FIELD_MISSING: lifecycle_stage is required");
    }

    public ShowroomAdminController.ProductBatchGenerateRespVO batchPublishProducts(
            ShowroomAdminController.ProductBatchGenerateReqVO req, Long operatorUserId) {
        List<ShowroomAdminController.ProductPageRespVO> matchedRows = listProductsForBatch(req);
        List<ShowroomAdminController.ProductBatchGenerateFailureRespVO> failures = new ArrayList<>();
        int publishableCount = 0;
        int skippedStatusCount = 0;
        int succeededCount = 0;
        for (ShowroomAdminController.ProductPageRespVO row : matchedRows) {
            if (!isDirectPublishableProductStatus(row.revision().status())) {
                skippedStatusCount++;
                continue;
            }
            publishableCount++;
            try {
                publishProduct(toBatchPublishReq(row), operatorUserId);
                succeededCount++;
            } catch (RuntimeException exception) {
                failures.add(toBatchFailure(row, exception));
            }
        }
        return new ShowroomAdminController.ProductBatchGenerateRespVO(
                matchedRows.size(),
                publishableCount,
                skippedStatusCount,
                succeededCount,
                failures.size(),
                List.copyOf(failures)
        );
    }

    public ShowroomAdminController.ProductSalesCountryBatchGenerateRespVO batchGenerateProductSalesCountries(
            ShowroomAdminController.ProductBatchGenerateReqVO req) {
        List<ShowroomAdminController.ProductPageRespVO> matchedRows = listProductsForBatch(req);
        List<ShowroomAdminController.ProductBatchGenerateFailureRespVO> failures = new ArrayList<>();
        int skippedCompletedCount = 0;
        int updatedProductCount = 0;
        int generatedLanguageCount = 0;
        for (ShowroomAdminController.ProductPageRespVO row : matchedRows) {
            try {
                int generatedCount = generateMissingProductSalesCountries(row);
                if (generatedCount == 0) {
                    skippedCompletedCount++;
                    continue;
                }
                updatedProductCount++;
                generatedLanguageCount += generatedCount;
            } catch (RuntimeException exception) {
                failures.add(toBatchFailure(row, exception));
            }
        }
        return new ShowroomAdminController.ProductSalesCountryBatchGenerateRespVO(
                matchedRows.size(),
                skippedCompletedCount,
                updatedProductCount,
                generatedLanguageCount,
                failures.size(),
                List.copyOf(failures)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomAdminController.ProductNarrationGenerateRespVO generateProductNarrationAudio(Long productId,
                                                                                                Long sourceRevisionId) {
        Long resolvedProductId = requireId(productId, "SHOWROOM_TARGET_NOT_FOUND: product id is required");
        Long resolvedSourceRevisionId = resolveRequestedProductRevisionId(resolvedProductId, sourceRevisionId);
        ProductNarrationPair narrationPair = prepareProductNarrationPairForRevision(resolvedProductId,
                resolvedSourceRevisionId);
        ProductNarrationPair generatedPair = generateProductNarrationAudioPair(narrationPair);

        return new ShowroomAdminController.ProductNarrationGenerateRespVO(resolvedProductId,
                generatedPair.zh().id(), generatedPair.en().id(), nullToEmpty(generatedPair.zh().voice()));
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomAdminController.HallNarrationGenerateRespVO generateHallNarrationAudio(
            ShowroomAdminController.HallNarrationGenerateReqVO req) {
        Long hallId = requireId(req.hallId(), "SHOWROOM_TARGET_NOT_FOUND: hall id is required");
        ShowroomHall hall = contentService.getHall(hallId);
        ProductNarrationPair draftPair = draftHallNarrationPair(hall);
        ProductNarrationPair generatedPair = generateNarrationAudioPair(draftPair);
        ProductNarrationPair publishedPair = publishHallNarrationPair(generatedPair);
        return new ShowroomAdminController.HallNarrationGenerateRespVO(hallId,
                publishedPair.zh().id(), publishedPair.en().id(), nullToEmpty(publishedPair.zh().voice()));
    }

    public ShowroomAdminController.HallNarrationBatchGenerateRespVO batchGenerateHallNarrationAudio() {
        List<ShowroomHall> halls = contentService.listHalls();
        List<ShowroomAdminController.HallNarrationBatchGenerateFailureRespVO> failures = new ArrayList<>();
        int succeededCount = 0;
        for (ShowroomHall hall : halls) {
            try {
                generateHallNarrationAudio(new ShowroomAdminController.HallNarrationGenerateReqVO(hall.hallId()));
                succeededCount++;
            } catch (RuntimeException exception) {
                failures.add(new ShowroomAdminController.HallNarrationBatchGenerateFailureRespVO(
                        hall.hallId(), hall.hallCode(), hall.name(), batchFailureReason(exception)));
            }
        }
        return new ShowroomAdminController.HallNarrationBatchGenerateRespVO(halls.size(), succeededCount,
                failures.size(), List.copyOf(failures));
    }

    public ShowroomNarrationVersion generateProductNarrationScript(Long productId) {
        Long resolvedProductId = requireId(productId, "SHOWROOM_TARGET_NOT_FOUND: product id is required");
        ShowroomProductSnapshot snapshot = contentService.getProduct(resolvedProductId);
        ShowroomProductRevision revision = contentService.getCurrentOrLatestProductRevision(resolvedProductId);
        String generatedScript = productNarrationCodexService.generateScript(snapshot, revision);
        ShowroomNarrationVersion generatedZh = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.PRODUCT, resolvedProductId, revision.revisionId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH, generatedScript, true));
        String translatedScript = productNarrationCodexService.translateZhToEn(generatedZh.scriptText());
        narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.PRODUCT, resolvedProductId, revision.revisionId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN, translatedScript, true));
        return generatedZh;
    }

    public ShowroomAdminController.ProductCoverGenerateRespVO generateProductCoverImage(
            ShowroomAdminController.ProductCoverGenerateReqVO req, Long operatorUserId) {
        Long productId = requireId(req.productId(), "SHOWROOM_TARGET_NOT_FOUND: product id is required");
        ShowroomProductRevision revision = contentService.getLatestProductRevision(productId);
        String approvalStatus = resolveProductStatus(productId, revision);
        if (!isApprovedProductStatus(approvalStatus)) {
            throw new IllegalStateException(
                    "SHOWROOM_COVER_GENERATION_NOT_APPROVED: 需要产品基础信息经过审核之后才可以AI生成封面");
        }
        String coverImage = generateSingleProductCoverImage(
                productId,
                requireText(req.productCode(), "SHOWROOM_COVER_GENERATION_FAILED: product code is required"),
                requireText(req.nameCn(), "SHOWROOM_COVER_GENERATION_FAILED: product chinese name is required"),
                requireText(req.nameEn(), "SHOWROOM_COVER_GENERATION_FAILED: product english name is required"),
                requireMap(req.fields(), "SHOWROOM_COVER_GENERATION_FAILED: product fields are required"));
        return new ShowroomAdminController.ProductCoverGenerateRespVO(coverImage);
    }

    public ShowroomAdminController.ProductBatchGenerateRespVO batchGenerateProductNarrationAudio(
            ShowroomAdminController.ProductBatchGenerateReqVO req) {
        ProductBatchNarrationAutoCheckCriteria criteria = toProductBatchNarrationAutoCheckCriteria(req);
        return executeManualProductBatchNarrationAudio(criteria);
    }

    public ShowroomAdminController.ProductBatchGenerateStateRespVO getProductBatchGenerateNarrationAudioState() {
        return toProductBatchGenerateStateResp(loadProductBatchNarrationAutoCheckState());
    }

    public void runScheduledProductBatchNarrationAudioAutoCheck() {
        ProductBatchNarrationAutoCheckState currentState = loadProductBatchNarrationAutoCheckState();
        if (!currentState.enabled()) {
            return;
        }
        if (!productBatchNarrationAutoCheckLock.tryLock()) {
            return;
        }
        try {
            executeProductBatchNarrationAudioLocked(currentState);
        } finally {
            productBatchNarrationAutoCheckLock.unlock();
        }
    }

    public ShowroomAdminController.ProductNarrationScriptBatchTaskRespVO startBatchGenerateNarrationScript(
            ShowroomAdminController.ProductBatchGenerateReqVO req) {
        ProductNarrationScriptBatchTaskState currentState = loadProductNarrationScriptBatchTaskState();
        if (currentState.active()) {
            return toProductNarrationScriptBatchTaskResp(currentState);
        }
        ProductNarrationScriptBatchTaskCriteria criteria = toProductNarrationScriptBatchTaskCriteria(req);
        ProductNarrationScriptBatchExecutionSummary initialSummary =
                summarizeProductNarrationScriptBatch(criteria);
        long startedAt = Instant.now().toEpochMilli();
        boolean active = initialSummary.remainingCount() > 0;
        ProductNarrationScriptBatchTaskState initialState = new ProductNarrationScriptBatchTaskState(
                active,
                false,
                normalizeProductNarrationScriptBatchTaskCriteria(criteria),
                new ProductNarrationScriptBatchTaskSummaryData(initialSummary.matchedCount(),
                        initialSummary.skippedCompletedCount(), 0, 0, initialSummary.remainingCount(),
                        startedAt, null, active ? null : startedAt, null),
                emptyProductNarrationScriptBatchTaskFailure());
        saveProductNarrationScriptBatchTaskState(initialState);
        if (active) {
            triggerProductBatchNarrationScriptTaskAsync();
        }
        return toProductNarrationScriptBatchTaskResp(loadProductNarrationScriptBatchTaskState());
    }

    public ShowroomAdminController.ProductNarrationScriptBatchTaskRespVO getProductBatchGenerateNarrationScriptStatus() {
        return toProductNarrationScriptBatchTaskResp(loadProductNarrationScriptBatchTaskState());
    }

    public void runScheduledProductBatchNarrationScriptAutoCheck() {
        ProductNarrationScriptBatchTaskState currentState = loadProductNarrationScriptBatchTaskState();
        if (!currentState.active()) {
            return;
        }
        if (!productBatchNarrationScriptTaskLock.tryLock()) {
            return;
        }
        try {
            executeProductNarrationScriptBatchTaskLocked(currentState);
        } finally {
            productBatchNarrationScriptTaskLock.unlock();
        }
    }

    public ShowroomAdminController.ProductBatchGenerateRespVO batchGenerateProductCoverImage(
            ShowroomAdminController.ProductBatchGenerateReqVO req, Long operatorUserId) {
        List<ShowroomAdminController.ProductPageRespVO> matchedRows = listProductsForBatch(req);
        List<ShowroomAdminController.ProductPageRespVO> publishedRows = matchedRows.stream()
                .filter(row -> STATUS_PUBLISHED.equalsIgnoreCase(row.revision().status()))
                .toList();
        String coverGenerationMode = resolveProductCoverGenerationMode(req.coverGenerationMode());
        List<ShowroomAdminController.ProductPageRespVO> candidateRows = publishedRows;
        int skippedExistingCount = 0;
        if (PRODUCT_COVER_GENERATION_MODE_MISSING_ONLY.equals(coverGenerationMode)) {
            candidateRows = publishedRows.stream()
                    .filter(row -> !hasText(row.revision().fields().get("cover_image")))
                    .toList();
            skippedExistingCount = publishedRows.size() - candidateRows.size();
        }
        return productCoverBatchTaskService.startTask(
                new ShowroomProductCoverBatchTaskService.StartTaskCommand(
                        operatorUserId,
                        normalizeKeyword(req.keyword()),
                        req.lifecycleStage(),
                        req.incompleteStatus(),
                        req.approvalStatus(),
                        coverGenerationMode,
                        imagePromptVersionService.requireCurrentVersionId(
                                ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER),
                        matchedRows.size(),
                        publishedRows.size(),
                        matchedRows.size() - publishedRows.size(),
                        skippedExistingCount,
                        candidateRows.stream()
                                .map(this::toProductCoverTaskItemSnapshot)
                                .toList()
                )
        );
    }

    public ShowroomAdminController.ProductCoverBatchTaskStateRespVO getProductBatchGenerateCoverImageState() {
        return productCoverBatchTaskService.getTaskState();
    }

    public ShowroomAdminController.ProductTranslatePublishBatchTaskRespVO startBatchTranslatePublishProducts(
            ShowroomAdminController.ProductBatchGenerateReqVO req, Long operatorUserId) {
        ShowroomProductTranslatePublishBatchTaskDO activeTask = translatePublishBatchTaskMapper == null
                ? null : translatePublishBatchTaskMapper.selectActiveTask();
        if (activeTask != null) {
            activeTask = recoverStaleProductTranslatePublishBatchTask(activeTask);
        }
        ProductTranslatePublishBatchTaskState persisted = loadPersistedProductTranslatePublishBatchTaskState();
        ProductTranslatePublishBatchTaskState current = persisted == null ? productTranslatePublishBatchTaskState : persisted;
        productTranslatePublishBatchTaskState = current;
        if (current.active() || current.running()) {
            throw new IllegalStateException("SHOWROOM_TRANSLATION_BATCH_RUNNING: 已存在未完成的一键翻译任务");
        }
        if (activeTask != null) {
            throw new IllegalStateException("SHOWROOM_TRANSLATION_BATCH_RUNNING: 已存在未完成的一键翻译任务 "
                    + activeTask.getId());
        }
        ProductTranslatePublishBatchTaskCriteria criteria = new ProductTranslatePublishBatchTaskCriteria(
                normalizeKeyword(req.keyword()), req.lifecycleStage(), req.incompleteStatus(), req.approvalStatus());
        List<ShowroomAdminController.ProductPageRespVO> matchedRows = listProductsForBatch(req);
        long startedAt = Instant.now().toEpochMilli();
        ProductTranslatePublishBatchTaskState initial = new ProductTranslatePublishBatchTaskState(
                !matchedRows.isEmpty(), false, criteria, matchedRows.size(), 0, 0, matchedRows.size(),
                startedAt, null, matchedRows.isEmpty() ? startedAt : null, null, null, null, List.of());
        productTranslatePublishBatchTaskState = initial;
        Long taskId = persistProductTranslatePublishBatchTask(initial, operatorUserId, matchedRows);
        if (!matchedRows.isEmpty()) {
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            CompletableFuture.runAsync(() -> executeProductTranslatePublishBatchTaskInTenant(matchedRows,
                    operatorUserId, tenantId, taskId))
                    .whenComplete((ignored, throwable) -> {
                        if (throwable != null) {
                            completeProductTranslatePublishBatchTaskAfterFailure(taskId, tenantId, throwable);
                        }
                    });
        }
        return toProductTranslatePublishBatchTaskResp(productTranslatePublishBatchTaskState);
    }

    public ShowroomAdminController.ProductTranslatePublishBatchTaskRespVO getProductBatchTranslatePublishStatus() {
        ProductTranslatePublishBatchTaskState persisted = loadPersistedProductTranslatePublishBatchTaskState();
        if (persisted != null) {
            productTranslatePublishBatchTaskState = persisted;
        }
        return toProductTranslatePublishBatchTaskResp(productTranslatePublishBatchTaskState);
    }

    public List<ShowroomAdminController.ProductPageRespVO> listProducts() {
        return contentService.listProducts().stream()
                .map(snapshot -> toProductPageRow(snapshot, true))
                .toList();
    }

    public PageResult<ShowroomAdminController.ProductPageRespVO> listProducts(ShowroomAdminController.PageQueryReqVO req) {
        return listProductPage(req, null, null);
    }

    public PageResult<ShowroomAdminController.ProductPageRespVO> listProducts(ShowroomAdminController.PageQueryReqVO req,
                                                                               Set<Long> visibleProductIds,
                                                                               Set<Long> editableProductIds) {
        return listProductPage(req, visibleProductIds, editableProductIds);
    }

    private PageResult<ShowroomAdminController.ProductPageRespVO> listProductPage(
            ShowroomAdminController.PageQueryReqVO req,
            Set<Long> visibleProductIds,
            Set<Long> editableProductIds) {
        if (!hasProductPageFilters(req)) {
            return listUnfilteredProductPage(req, visibleProductIds, editableProductIds);
        }
        List<ProductPageCandidate> matchedCandidates = productPageCandidates(visibleProductIds, editableProductIds)
                .stream()
                .filter(candidate -> matchesProductCandidate(candidate, req))
                .toList();
        List<ShowroomAdminController.ProductPageRespVO> rows = productPageRows(
                page(matchedCandidates, req.pageNo(), req.pageSize()));
        return new PageResult<>(rows, (long) matchedCandidates.size());
    }

    private PageResult<ShowroomAdminController.ProductPageRespVO> listUnfilteredProductPage(
            ShowroomAdminController.PageQueryReqVO req,
            Set<Long> visibleProductIds,
            Set<Long> editableProductIds) {
        PageResult<ShowroomProductSnapshot> snapshotPage;
        if (visibleProductIds == null) {
            snapshotPage = contentService.pageProducts(req.pageNo(), req.pageSize());
        } else {
            List<ShowroomProductSnapshot> visibleProducts = contentService.listProducts().stream()
                    .filter(snapshot -> visibleProductIds.contains(snapshot.productId()))
                    .toList();
            snapshotPage = new PageResult<>(page(visibleProducts, req.pageNo(), req.pageSize()),
                    (long) visibleProducts.size());
        }
        List<ShowroomAdminController.ProductPageRespVO> rows = productPageRows(
                productPageCandidates(snapshotPage.getList(), editableProductIds));
        return new PageResult<>(rows, snapshotPage.getTotal());
    }

    private List<ProductPageCandidate> productPageCandidates(Set<Long> visibleProductIds,
                                                             Set<Long> editableProductIds) {
        List<ShowroomProductSnapshot> snapshots = contentService.listProducts().stream()
                .filter(snapshot -> visibleProductIds == null || visibleProductIds.contains(snapshot.productId()))
                .toList();
        if (snapshots.isEmpty()) {
            return List.of();
        }
        return productPageCandidates(snapshots, editableProductIds);
    }

    private List<ProductPageCandidate> productPageCandidates(List<ShowroomProductSnapshot> snapshots,
                                                             Set<Long> editableProductIds) {
        if (snapshots.isEmpty()) {
            return List.of();
        }
        List<Long> productIds = snapshots.stream()
                .map(ShowroomProductSnapshot::productId)
                .toList();
        Map<Long, ShowroomProductRevision> latestRevisionsByProductId =
                contentService.latestProductRevisions(productIds);
        List<Long> currentRevisionIds = snapshots.stream()
                .map(ShowroomProductSnapshot::currentRevisionId)
                .flatMap(Optional::stream)
                .toList();
        Map<Long, ShowroomProductRevision> currentRevisionsById = contentService.productRevisions(currentRevisionIds);
        List<ProductPageCandidate> candidates = new ArrayList<>();
        for (ShowroomProductSnapshot snapshot : snapshots) {
            ShowroomProductRevision latestRevision = latestRevisionsByProductId.get(snapshot.productId());
            if (latestRevision == null) {
                throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: product revision not found");
            }
            ShowroomProductRevision displayRevision = resolveDisplayProductRevision(snapshot, latestRevision,
                    currentRevisionsById);
            candidates.add(new ProductPageCandidate(snapshot, latestRevision, displayRevision,
                    isProductEditable(snapshot, editableProductIds)));
        }
        return List.copyOf(candidates);
    }

    private List<ShowroomAdminController.ProductPageRespVO> productPageRows(List<ProductPageCandidate> candidates) {
        return candidates.stream()
                .map(candidate -> toProductPageRow(candidate.snapshot(), candidate.latestRevision(),
                        candidate.displayRevision(), candidate.editable()))
                .toList();
    }

    public List<ShowroomProductExcelVO> listProductExcelRows(ShowroomAdminController.PageQueryReqVO req) {
        Map<Long, String> hallNamesByProductId = resolveExportHallNamesByProductId();
        return contentService.listProducts().stream()
                .map(snapshot -> toProductPageRow(snapshot, true))
                .filter(row -> matchesProduct(row, req))
                .map(row -> toProductExcelRow(row, hallNamesByProductId.get(row.productId())))
                .toList();
    }

    public List<ShowroomProductExcelVO> listProductExcelRows(ShowroomAdminController.PageQueryReqVO req,
                                                             Set<Long> visibleProductIds,
                                                             Set<Long> editableProductIds) {
        Map<Long, String> hallNamesByProductId = resolveExportHallNamesByProductId();
        return contentService.listProducts().stream()
                .filter(snapshot -> visibleProductIds.contains(snapshot.productId()))
                .map(snapshot -> toProductPageRow(snapshot, editableProductIds.contains(snapshot.productId())))
                .filter(row -> matchesProduct(row, req))
                .map(row -> toProductExcelRow(row, hallNamesByProductId.get(row.productId())))
                .toList();
    }

    private Map<Long, String> resolveExportHallNamesByProductId() {
        Map<Long, String> hallNamesByProductId = new LinkedHashMap<>();
        for (ShowroomHall hall : contentService.listHalls()) {
            String hallName = normalizeExcelCell(hall.name());
            if (!hasText(hallName)) {
                continue;
            }
            for (ShowroomHallProductMapping mapping : hall.productMappings()) {
                hallNamesByProductId.putIfAbsent(mapping.productId(), hallName);
            }
        }
        return hallNamesByProductId;
    }

    public List<ShowroomProductExcelVO> buildProductImportTemplateRows() {
        return List.of(ShowroomProductExcelVO.builder()
                .productCode("product_001")
                .nameCn("示例产品")
                .nameEn("Sample Product")
                .hallName("示例展柜")
                .ownerCompanyName("瑛泰")
                .lifecycleStage(LIFECYCLE_REGISTERED_TEXT)
                .pipelineLayout("示例BU")
                .coreSellingPoints("中国")
                .indicationContent("示例适应症")
                .modelSpecification("示例型号规格")
                .registrationCertificate("示例注册证信息")
                .sellingPointsCopy("示例卖点文案")
                .productImage("")
                .awards("")
                .rawMaterialSheet("")
                .build());
    }

    public List<ShowroomKeywordExcelRow> listKeywordExcelRows() {
        return keywordMapper.selectListOrdered().stream()
                .map(keyword -> new ShowroomKeywordExcelRow(
                        normalizeExcelCell(keyword.getNameZh()),
                        normalizeExcelCell(keyword.getNameEn())))
                .toList();
    }

    public List<ShowroomAwardExcelExportRow> listAwardExcelRows() {
        List<ShowroomAwardExcelExportRow> rows = contentService.listAwards().stream()
                .map(snapshot -> {
                    ShowroomAwardRevision revision = contentService.getCurrentOrLatestAwardRevision(snapshot.awardId());
                    return new ShowroomAwardExcelExportRow(
                            revision.awardCode(),
                            awardSequenceText(revision.awardCode(), revision.nameCn()),
                            revision.nameCn(),
                            revision.fields().get("award_date_text"),
                            revision.fields().get("issuer"),
                            revision.fields().get("cover_image"),
                            null);
                })
                .toList();
        if (rows.isEmpty()) {
            throw new IllegalStateException("SHOWROOM_AWARD_EXPORT_EMPTY: 当前租户没有可导出的奖项，无法生成可回导文件");
        }
        return rows;
    }

    public List<ShowroomNarrationExcelRow> listNarrationExcelRows(ShowroomAdminController.PageQueryReqVO req) {
        return listNarrationExcelRows(req, null);
    }

    public List<ShowroomNarrationExcelRow> listNarrationExcelRows(ShowroomAdminController.PageQueryReqVO req,
                                                                  List<ShowroomProductExcelVO> productRows) {
        List<ShowroomNarrationExcelRow> rows = new ArrayList<>();
        Set<String> exportedProductCodes = productRows == null ? null : productRows.stream()
                .map(ShowroomProductExcelVO::getProductCode)
                .map(ShowroomApiRuntime::trimText)
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        contentService.listProducts().stream()
                .map(snapshot -> toProductPageRow(snapshot, true))
                .filter(row -> matchesProduct(row, req))
                .filter(row -> exportedProductCodes == null || exportedProductCodes.contains(trimText(row.productCode())))
                .forEach(row -> rows.addAll(productNarrationExcelRows(row)));
        contentService.listAwards().stream()
                .forEach(snapshot -> rows.addAll(awardNarrationExcelRows(snapshot)));
        return List.copyOf(rows);
    }

    public void deleteProduct(Long productId) {
        contentService.deleteProduct(productId);
    }

    public ShowroomAdminController.ProductDetailRespVO getProductDetail(Long productId, Long revisionId,
                                                                        boolean editable) {
        ShowroomProductSnapshot snapshot = contentService.getProduct(productId);
        ShowroomProductRevision latestRevision = contentService.getLatestProductRevision(productId);
        ShowroomProductRevision targetRevision = resolveTargetProductRevision(productId, revisionId, latestRevision);
        boolean revisionEditable = editable && latestRevision.revisionId().equals(targetRevision.revisionId());
        return buildProductDetail(snapshot, targetRevision, revisionEditable);
    }

    public PageResult<AwardPageRespVO> listAwards(
            ShowroomAdminController.PageQueryReqVO req) {
        List<AwardPageRespVO> rows = contentService.listAwards().stream()
                .filter(snapshot -> matchesAward(snapshot, req.keyword()))
                .map(this::toAwardPageRow)
                .toList();
        return pageResult(rows, req.pageNo(), req.pageSize());
    }

    public AwardDetailRespVO getAwardDetail(Long awardId, Long revisionId, boolean editable) {
        ShowroomAwardSnapshot snapshot = contentService.getAward(awardId);
        ShowroomAwardRevision latestRevision = contentService.getLatestAwardRevision(awardId);
        ShowroomAwardRevision targetRevision = resolveTargetAwardRevision(awardId, revisionId, latestRevision);
        boolean revisionEditable = editable && latestRevision.revisionId().equals(targetRevision.revisionId());
        return buildAwardDetail(snapshot, targetRevision, revisionEditable);
    }

    private static String awardSequenceText(String awardCode, String nameCn) {
        String normalizedCode = trimText(awardCode);
        Matcher matcher = Pattern.compile("^AWARD-(\\d+)$").matcher(normalizedCode);
        if (!matcher.matches()) {
            throw new IllegalStateException("SHOWROOM_AWARD_EXPORT_CODE_INVALID: 奖项编码无法导出序号，奖项编码 "
                    + (normalizedCode.isEmpty() ? "<unknown>" : normalizedCode) + "，奖项名称 "
                    + (trimText(nameCn).isEmpty() ? "<unknown>" : trimText(nameCn)));
        }
        return matcher.group(1);
    }

    private static String trimText(String value) {
        return value == null ? "" : value.trim();
    }

    private List<ShowroomNarrationExcelRow> productNarrationExcelRows(
            ShowroomAdminController.ProductPageRespVO row) {
        ShowroomAdminController.ProductDetailRespVO displayRevision = row.displayRevision();
        return narrationExcelRows(TARGET_PRODUCT, row.productCode(), displayRevision.nameCn(),
                row.productId(), displayRevision.revisionId());
    }

    private List<ShowroomNarrationExcelRow> awardNarrationExcelRows(ShowroomAwardSnapshot snapshot) {
        ShowroomAwardRevision revision = contentService.getCurrentOrLatestAwardRevision(snapshot.awardId());
        return narrationExcelRows(TARGET_AWARD, revision.awardCode(), revision.nameCn(),
                snapshot.awardId(), revision.revisionId());
    }

    private List<ShowroomNarrationExcelRow> narrationExcelRows(String targetType,
                                                               String targetCode,
                                                               String targetName,
                                                               Long targetId,
                                                               Long sourceRevisionId) {
        List<ShowroomNarrationExcelRow> rows = new ArrayList<>();
        for (ShowroomNarrationLanguage language : List.of(ShowroomNarrationLanguage.ZH, ShowroomNarrationLanguage.EN)) {
            ShowroomNarrationKey key = new ShowroomNarrationKey(ShowroomNarrationTargetType.valueOf(targetType),
                    targetId, ShowroomNarrationAudienceType.PUBLIC, language);
            ShowroomNarrationVersion version = narrationService.latestPublished(key, sourceRevisionId).orElse(null);
            if (version == null || !hasText(version.scriptText())) {
                continue;
            }
            rows.add(new ShowroomNarrationExcelRow(
                    targetType,
                    normalizeExcelCell(targetCode),
                    normalizeExcelCell(targetName),
                    language.name(),
                    normalizeExcelCell(version.scriptText()),
                    version.audioFileId(),
                    version.audioFileId() == null ? "" : fileUrl(version.audioFileId()),
                    version.audioDurationSeconds(),
                    normalizeExcelCell(version.voice())
            ));
        }
        return rows;
    }

    public ShowroomAwardRevision saveAwardDraft(AwardDraftReqVO req) {
        return contentService.saveAwardDraft(new ShowroomAwardDraft(req.awardId(), req.awardCode(), req.nameCn(),
                req.nameEn(), req.descriptionZh(), req.descriptionEn(), req.issuer(), req.awardDateText(),
                req.coverImage()));
    }

    public AwardDetailRespVO publishAward(AwardPublishReqVO req,
                                                                  Long operatorUserId) {
        Long revisionId = requireId(req.revisionId(),
                "SHOWROOM_REQUIRED_FIELD_MISSING: award publish requires revisionId");
        ShowroomAwardRevision revision = contentService.getAwardRevision(revisionId);
        validateAwardPublishRequestMatchesRevision(req, revision);
        validateAwardNarrationReadyForPublish(revision);
        ShowroomAwardRevision published = contentService.publishAwardRevision(revision.revisionId(), operatorUserId);
        return buildAwardDetail(contentService.getAward(published.awardId()), published, true);
    }

    public AwardCoverGenerateRespVO generateAwardCoverImage(ShowroomAdminController.AwardCoverGenerateReqVO req,
                                                            Long operatorUserId) {
        if (awardCoverImageService == null) {
            throw new IllegalStateException(
                    "SHOWROOM_AWARD_COVER_GENERATION_FAILED: award cover image service is not configured");
        }
        Long awardId = requireId(req.awardId(), "SHOWROOM_TARGET_NOT_FOUND: award id is required");
        ShowroomAwardSnapshot snapshot = contentService.getAward(awardId);
        ShowroomAwardRevision currentRevision = contentService.requireCurrentAwardRevision(awardId);
        validateAwardNarrationReadyForPublish(currentRevision);
        String currentCoverImage = requireText(currentRevision.fields().get("cover_image"),
                "SHOWROOM_AWARD_COVER_GENERATION_FAILED: current award cover image is required");
        Long promptVersionId = imagePromptVersionService.requireCurrentVersionId(
                ShowroomImagePromptVersionService.SCENE_AWARD_COVER);
        String renderedPrompt = imagePromptVersionService.renderAwardCoverPrompt(
                promptVersionId,
                requireText(currentRevision.nameCn(), "SHOWROOM_AWARD_COVER_GENERATION_FAILED: award chinese name is required"),
                nullToEmpty(currentRevision.nameEn()),
                nullToEmpty(currentRevision.fields().get("issuer")),
                nullToEmpty(currentRevision.fields().get("award_date_text")),
                nullToEmpty(currentRevision.fields().get("description_zh")));
        String generatedCoverImage = awardCoverImageService.generateCoverImage(
                requireText(snapshot.awardCode(), "SHOWROOM_AWARD_COVER_GENERATION_FAILED: award code is required"),
                renderedPrompt,
                currentCoverImage);
        generatedCoverImage = requireText(generatedCoverImage,
                "SHOWROOM_AWARD_COVER_GENERATION_FAILED: generated award cover image is empty");
        ShowroomAwardRevision savedDraft = contentService.saveAwardDraft(new ShowroomAwardDraft(
                currentRevision.awardId(),
                currentRevision.awardCode(),
                currentRevision.nameCn(),
                currentRevision.nameEn(),
                currentRevision.fields().get("description_zh"),
                currentRevision.fields().get("description_en"),
                currentRevision.fields().get("issuer"),
                currentRevision.fields().get("award_date_text"),
                generatedCoverImage));
        cloneAwardPublishedNarrationsToRevision(currentRevision, savedDraft.revisionId());
        ShowroomAwardRevision published = contentService.publishAwardRevision(savedDraft.revisionId(), operatorUserId);
        imagePromptVersionService.recordUsage(promptVersionId);
        return new AwardCoverGenerateRespVO(published.awardId(), published.revisionId(), published.revisionNo(),
                nullToEmpty(published.fields().get("cover_image")));
    }

    private void validateAwardPublishRequestMatchesRevision(AwardPublishReqVO req, ShowroomAwardRevision revision) {
        if (!Objects.equals(req.awardId(), revision.awardId())) {
            throw new IllegalStateException("SHOWROOM_AWARD_PUBLISH_STALE: awardId does not match revision");
        }
        requireSameText(req.awardCode(), revision.awardCode(), "awardCode");
        requireSameText(req.nameCn(), revision.nameCn(), "nameCn");
        requireSameText(req.nameEn(), revision.nameEn(), "nameEn");
        requireSameText(req.descriptionZh(), revision.fields().get("description_zh"), "descriptionZh");
        requireSameText(req.descriptionEn(), revision.fields().get("description_en"), "descriptionEn");
        requireSameText(req.issuer(), revision.fields().get("issuer"), "issuer");
        requireSameText(req.awardDateText(), revision.fields().get("award_date_text"), "awardDateText");
        requireSameText(req.coverImage(), revision.fields().get("cover_image"), "coverImage");
    }

    private void validateAwardNarrationReadyForPublish(ShowroomAwardRevision revision) {
        for (ShowroomNarrationLanguage language : List.of(ShowroomNarrationLanguage.ZH, ShowroomNarrationLanguage.EN)) {
            ShowroomNarrationVersion latest = narrationService.latest(new ShowroomNarrationKey(
                    ShowroomNarrationTargetType.AWARD, revision.awardId(), ShowroomNarrationAudienceType.PUBLIC,
                    language), revision.revisionId()).orElse(null);
            if (latest == null || latest.audioFileId() == null) {
                throw new IllegalStateException("AWARD_NARRATION_" + language.name()
                        + "_MISSING: award narration audio is required before award publish");
            }
            if (latest.status() != ShowroomNarrationStatus.PUBLISHED) {
                narrationService.publishDirectly(latest.id());
            }
        }
    }

    private void cloneAwardPublishedNarrationsToRevision(ShowroomAwardRevision sourceRevision, Long targetRevisionId) {
        for (ShowroomNarrationLanguage language : List.of(ShowroomNarrationLanguage.ZH, ShowroomNarrationLanguage.EN)) {
            ShowroomNarrationKey key = new ShowroomNarrationKey(
                    ShowroomNarrationTargetType.AWARD,
                    sourceRevision.awardId(),
                    ShowroomNarrationAudienceType.PUBLIC,
                    language);
            ShowroomNarrationVersion sourceNarration = narrationService.latestPublished(key, sourceRevision.revisionId())
                    .orElseThrow(() -> new IllegalStateException("AWARD_NARRATION_" + language.name()
                            + "_MISSING: award narration audio is required before award cover generation"));
            ShowroomNarrationVersion cloned = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                    ShowroomNarrationTargetType.AWARD,
                    sourceRevision.awardId(),
                    targetRevisionId,
                    ShowroomNarrationAudienceType.PUBLIC,
                    language,
                    sourceNarration.scriptText(),
                    sourceNarration.generatedByAi()));
            narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(
                    cloned.id(),
                    requireId(sourceNarration.audioFileId(), "AWARD_NARRATION_" + language.name()
                            + "_MISSING: award narration audio file is required"),
                    sourceNarration.audioDurationSeconds() == null ? 1 : sourceNarration.audioDurationSeconds(),
                    sourceNarration.voice()));
            narrationService.publishDirectly(cloned.id());
        }
    }

    private static void requireSameText(String requestValue, String revisionValue, String fieldName) {
        if (!Objects.equals(normalizeComparableText(requestValue), normalizeComparableText(revisionValue))) {
            throw new IllegalStateException("SHOWROOM_AWARD_PUBLISH_STALE: award " + fieldName
                    + " differs from saved revision");
        }
    }

    private static String normalizeComparableText(String value) {
        return value == null ? "" : value.trim();
    }

    public void deleteAward(Long awardId) {
        contentService.deleteAward(awardId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomAdminController.ShowroomProductImportRespVO importProductExcel(List<ShowroomProductExcelVO> rows,
                                                                                  Map<Integer, ShowroomProductImportExtra> extrasByRowNo,
                                                                                  List<ShowroomAwardExcelImportRow> awardRows,
                                                                                  List<ShowroomNarrationExcelImportRow> narrationRows,
                                                                                  List<ShowroomKeywordExcelImportRow> keywordRows,
                                                                                  Map<String, Long> productMasterIdsByCode,
                                                                                  Long operatorUserId,
                                                                                  ShowroomAdminController.ShowroomProductImportSameAction sameProductAction,
                                                                                  ShowroomProductImportMode importMode,
                                                                                  boolean allowMissingNarration) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalStateException("SHOWROOM_REQUIRED_FIELD_MISSING: 产品导入文件不能为空");
        }
        if (awardRows == null || awardRows.isEmpty()) {
            throw new IllegalStateException("SHOWROOM_AWARD_IMPORT_EMPTY: 奖项页签没有可导入数据");
        }
        if (sameProductAction == null) {
            throw new IllegalArgumentException("相同产品处理方式不能为空");
        }
        ShowroomProductImportMode resolvedImportMode = importMode == null
                ? ShowroomProductImportMode.STANDARD
                : importMode;
        Map<String, Long> safeProductMasterIdsByCode = productMasterIdsByCode == null
                ? Map.of()
                : productMasterIdsByCode;
        Map<Integer, ShowroomProductImportExtra> safeExtrasByRowNo = extrasByRowNo == null ? Map.of() : extrasByRowNo;
        Map<String, ShowroomProductSnapshot> productsByCode = new LinkedHashMap<>();
        Map<String, ShowroomProductSnapshot> productsByLegacyCode = new LinkedHashMap<>();
        for (ShowroomProductSnapshot snapshot : contentService.listProducts()) {
            productsByCode.put(snapshot.productCode(), snapshot);
            String legacyProductCode = normalizeExcelCell(snapshot.legacyProductCode());
            if (hasText(legacyProductCode)) {
                ShowroomProductSnapshot previous = productsByLegacyCode.putIfAbsent(legacyProductCode, snapshot);
                if (previous != null) {
                    throw new IllegalStateException("SHOWROOM_PRODUCT_LEGACY_CODE_DUPLICATE: 旧产品编码重复，无法导入底表："
                            + legacyProductCode);
                }
            }
        }

        List<String> successProductCodes = new ArrayList<>();
        List<String> skippedProductCodes = new ArrayList<>();
        List<ShowroomAdminController.ShowroomProductImportFailureRespVO> failures = new ArrayList<>();
        List<String> successAwardCodes = new ArrayList<>();
        List<String> awardWarnings = new ArrayList<>();
        List<ShowroomAdminController.ShowroomAwardImportFailureRespVO> awardFailures = new ArrayList<>();
        Map<String, ShowroomNarrationExcelImportRow> narrationByTargetAndLanguage =
                indexNarrationRows(narrationRows, resolvedImportMode);
        Map<String, ShowroomHall> importHallsByName = loadImportHallsByName();
        List<OwnerCompanyExcelContract> ownerCompanyContracts = loadOwnerCompanyExcelContracts();
        List<ImportedHallProductMapping> importedHallProductMappings = new ArrayList<>();
        Set<Long> preservedHallProductIds = new LinkedHashSet<>();

        for (int index = 0; index < rows.size(); index++) {
            ShowroomProductExcelVO row = rows.get(index);
            int rowNo = index + 2;
            String productCode = normalizeExcelCell(row == null ? null : row.getProductCode());
            try {
                ShowroomProductImportExtra rowExtra = safeExtrasByRowNo.get(rowNo);
                validateImportRow(row, rowExtra, rowNo);
                if (shouldSkipUnmappedBaseWorkbookLegacyProduct(productCode, productsByCode, productsByLegacyCode,
                        resolvedImportMode, rowNo)) {
                    skippedProductCodes.add(productCode);
                    continue;
                }
                ShowroomProductSnapshot snapshot = resolveImportProductSnapshot(row, productCode, productsByCode,
                        productsByLegacyCode, resolvedImportMode, rowNo);
                String resolvedProductCode = snapshot == null ? productCode : snapshot.productCode();
                ShowroomHall importHall = resolveImportHall(row.getHallName(), importHallsByName, rowNo,
                        resolvedProductCode);
                if (snapshot == null) {
                    ShowroomAdminController.ProductDraftReqVO createDraft = buildImportDraftForMissingProduct(
                            row, rowExtra, rowNo, ownerCompanyContracts, safeProductMasterIdsByCode);
                    ShowroomAdminController.ProductDetailRespVO created = publishImportedNewProductTextOnly(
                            new ShowroomAdminController.ProductDraftReqVO(
                                    createDraft.productId(),
                                    createDraft.productMasterId(),
                                    createDraft.productCode(),
                                    createDraft.nameCn(),
                                    createDraft.nameEn(),
                                    createDraft.legacyProductCode(),
                                    createDraft.fields()
                            ), operatorUserId, resolveProductNarrationImportPair(productCode,
                                    createDraft.nameCn(), narrationByTargetAndLanguage));
                    ShowroomProductSnapshot createdSnapshot = contentService.getProduct(created.productId());
                    productsByCode.put(createdSnapshot.productCode(), createdSnapshot);
                    if (importHall != null) {
                        importedHallProductMappings.add(new ImportedHallProductMapping(importHall.hallId(),
                                createdSnapshot.productId(), rowNo));
                    }
                    successProductCodes.add(productCode);
                    continue;
                }
                if (importHall == null) {
                    preservedHallProductIds.add(snapshot.productId());
                } else {
                    importedHallProductMappings.add(new ImportedHallProductMapping(importHall.hallId(),
                            snapshot.productId(), rowNo));
                }
                ShowroomProductRevision latestRevision = contentService.getLatestProductRevision(snapshot.productId());
                ShowroomProductRevision displayRevision = resolveDisplayProductRevision(snapshot, latestRevision);
                ShowroomAdminController.ProductDetailRespVO currentDetail = buildProductDetail(snapshot,
                        displayRevision, latestRevision.revisionId().equals(displayRevision.revisionId()));
                ShowroomAdminController.ProductDraftReqVO importDraft = buildImportDraft(row, rowExtra, currentDetail,
                        rowNo, ownerCompanyContracts, safeProductMasterIdsByCode);
                if (!hasImportChanges(currentDetail, importDraft)
                        && sameProductAction == ShowroomAdminController.ShowroomProductImportSameAction.SKIP) {
                    skippedProductCodes.add(resolvedProductCode);
                    continue;
                }
                publishImportedProductTextOnly(new ShowroomAdminController.ProductDraftReqVO(
                        importDraft.productId(),
                        importDraft.productMasterId(),
                        importDraft.productCode(),
                        importDraft.nameCn(),
                        importDraft.nameEn(),
                        resolveImportedLegacyProductCode(productCode, importDraft.legacyProductCode(),
                                resolvedImportMode),
                        importDraft.fields()
                ), operatorUserId, resolveProductNarrationImportPair(importDraft.productCode(),
                        importDraft.nameCn(), narrationByTargetAndLanguage), allowMissingNarration);
                successProductCodes.add(resolvedProductCode);
            } catch (RuntimeException exception) {
                log.warn("Showroom product Excel import row failed. rowNo={}, productCode={}, message={}",
                        rowNo, productCode, exception.getMessage(), exception);
                failures.add(new ShowroomAdminController.ShowroomProductImportFailureRespVO(
                        rowNo, productCode, exception.getMessage()));
            }
        }
        if (failures.isEmpty()) {
            replaceHallMappingsFromImportedProductList(importedHallProductMappings, preservedHallProductIds);
        }

        for (ShowroomAwardExcelImportRow awardRow : awardRows) {
            try {
                ShowroomAwardRevision published = publishImportedAward(awardRow, operatorUserId, resolvedImportMode,
                        resolveAwardNarrationImportPair(awardRow.awardCode(), awardRow.nameCn(),
                                narrationByTargetAndLanguage));
                successAwardCodes.add(published.awardCode());
                if (awardRow.extraImageCount() > 0) {
                    awardWarnings.add("第 " + awardRow.rowNo() + " 行奖项 " + awardRow.awardCode()
                            + " 存在 " + awardRow.extraImageCount() + " 张额外图片，本次仅导入 E 列首图封面");
                }
            } catch (RuntimeException exception) {
                log.warn("Showroom award Excel import row failed. rowNo={}, awardCode={}, message={}",
                        awardRow == null ? null : awardRow.rowNo(),
                        awardRow == null ? null : awardRow.awardCode(),
                        exception.getMessage(), exception);
                awardFailures.add(new ShowroomAdminController.ShowroomAwardImportFailureRespVO(
                        awardRow == null ? null : awardRow.rowNo(),
                        awardRow == null ? null : awardRow.awardCode(),
                        exception.getMessage()));
            }
        }

        replaceKeywords(keywordRows, resolvedImportMode);

        return new ShowroomAdminController.ShowroomProductImportRespVO(
                rows.size(),
                successProductCodes.size(),
                skippedProductCodes.size(),
                failures.size(),
                List.copyOf(successProductCodes),
                List.copyOf(skippedProductCodes),
                List.copyOf(failures),
                awardRows.size(),
                successAwardCodes.size(),
                awardFailures.size(),
                List.copyOf(successAwardCodes),
                List.copyOf(awardWarnings),
                List.copyOf(awardFailures)
        );
    }

    private ShowroomProductSnapshot resolveImportProductSnapshot(ShowroomProductExcelVO row,
                                                                 String productCode,
                                                                 Map<String, ShowroomProductSnapshot> productsByCode,
                                                                 Map<String, ShowroomProductSnapshot> productsByLegacyCode,
                                                                 ShowroomProductImportMode importMode,
                                                                 int rowNo) {
        if (ShowroomProductImportMode.BASE_WORKBOOK.equals(importMode) && isLegacyProductCode(productCode)) {
            ShowroomProductSnapshot legacyMapped = productsByLegacyCode.get(productCode);
            if (legacyMapped == null) {
                ShowroomProductSnapshot exactOldProduct = productsByCode.get(productCode);
                if (exactOldProduct != null && !isCurrentIntProductCode(exactOldProduct.productCode())) {
                    throw new IllegalStateException("SHOWROOM_PRODUCT_LEGACY_CODE_INT_COUNT_MISMATCH: 第 " + rowNo
                            + " 行旧产品编码没有映射到当前 INT 产品，导入后 INT 产品数量会少于 product 产品数量："
                            + productCode);
                }
                throw new IllegalStateException("SHOWROOM_PRODUCT_LEGACY_CODE_MAPPING_MISSING: 第 " + rowNo
                        + " 行旧产品编码未配置到当前 INT 产品：" + productCode);
            }
            String currentProductCode = normalizeExcelCell(legacyMapped.productCode());
            if (!isCurrentIntProductCode(currentProductCode)) {
                throw new IllegalStateException("SHOWROOM_PRODUCT_LEGACY_CODE_TARGET_INVALID: 第 " + rowNo
                        + " 行旧产品编码 " + productCode + " 映射到非 INT 产品：" + currentProductCode);
            }
            return legacyMapped;
        }
        return productsByCode.get(productCode);
    }

    private boolean shouldSkipUnmappedBaseWorkbookLegacyProduct(String productCode,
                                                                Map<String, ShowroomProductSnapshot> productsByCode,
                                                                Map<String, ShowroomProductSnapshot> productsByLegacyCode,
                                                                ShowroomProductImportMode importMode,
                                                                int rowNo) {
        if (!ShowroomProductImportMode.BASE_WORKBOOK.equals(importMode) || !isLegacyProductCode(productCode)
                || productsByLegacyCode.containsKey(productCode)) {
            return false;
        }
        ShowroomProductSnapshot exactOldProduct = productsByCode.get(productCode);
        if (exactOldProduct != null && !isCurrentIntProductCode(exactOldProduct.productCode())) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_LEGACY_CODE_INT_COUNT_MISMATCH: 第 " + rowNo
                    + " 行旧产品编码没有映射到当前 INT 产品，导入后 INT 产品数量会少于 product 产品数量："
                    + productCode);
        }
        return true;
    }

    private String resolveImportedLegacyProductCode(String importedProductCode, String currentLegacyProductCode,
                                                    ShowroomProductImportMode importMode) {
        if (ShowroomProductImportMode.BASE_WORKBOOK.equals(importMode) && isLegacyProductCode(importedProductCode)) {
            return importedProductCode;
        }
        return currentLegacyProductCode;
    }

    private boolean isLegacyProductCode(String productCode) {
        String normalized = normalizeExcelCell(productCode);
        return normalized.startsWith("product_") || normalized.startsWith("PRODUCT_");
    }

    private boolean isCurrentIntProductCode(String productCode) {
        String normalized = normalizeExcelCell(productCode);
        return normalized.startsWith("INT-") || normalized.startsWith("Int-") || normalized.startsWith("int-");
    }

    public ShowroomAdminController.ShowroomProductImportRespVO importProductExcel(List<ShowroomProductExcelVO> rows,
                                                                                  Map<Integer, ShowroomProductImportExtra> extrasByRowNo,
                                                                                  Long operatorUserId,
                                                                                  ShowroomAdminController.ShowroomProductImportSameAction sameProductAction) {
        return importProductExcel(rows, extrasByRowNo, List.of(), List.of(), List.of(),
                Map.of(), operatorUserId, sameProductAction,
                ShowroomProductImportMode.STANDARD, false);
    }

    private ShowroomAwardRevision publishImportedAward(ShowroomAwardExcelImportRow awardRow, Long operatorUserId,
                                                       ShowroomProductImportMode importMode,
                                                       ImportedNarrationPair importedNarrationPair) {
        if (awardRow == null) {
            throw new IllegalStateException("SHOWROOM_AWARD_IMPORT_REQUIRED_FIELD_MISSING: 奖项行不能为空");
        }
        ShowroomProductImportMode resolvedImportMode = importMode == null
                ? ShowroomProductImportMode.STANDARD
                : importMode;
        ShowroomAwardRevision existingRevision = findExistingAwardRevisionByCode(awardRow.awardCode());
        if (awardRow.coverImage() == null
                && resolvedImportMode == ShowroomProductImportMode.STANDARD) {
            throw new IllegalStateException("SHOWROOM_AWARD_IMPORT_COVER_MISSING: 第 " + awardRow.rowNo()
                    + " 行奖项必须提供封面");
        }
        String coverImage = resolveImportedAwardCoverImage(awardRow, existingRevision, resolvedImportMode);
        ShowroomAwardRevision draft = contentService.saveAwardDraft(new ShowroomAwardDraft(
                existingRevision == null ? null : existingRevision.awardId(), awardRow.awardCode(), awardRow.nameCn(),
                existingRevision == null ? "" : nullToEmpty(existingRevision.nameEn()),
                existingRevision == null ? "" : nullToEmpty(existingRevision.fields().get("description_zh")),
                existingRevision == null ? "" : nullToEmpty(existingRevision.fields().get("description_en")),
                awardRow.issuer(), awardRow.awardDateText(), coverImage));
        if (importedNarrationPair == null && existingRevision != null) {
            cloneAwardPublishedNarrationsToRevision(existingRevision, draft.revisionId());
        }
        publishImportedAwardNarrationPair(draft, importedNarrationPair);
        return contentService.publishAwardRevision(draft.revisionId(), operatorUserId);
    }

    private ShowroomAwardRevision findExistingAwardRevisionByCode(String awardCode) {
        String normalizedAwardCode = normalizeExcelCell(awardCode);
        if (!hasText(normalizedAwardCode)) {
            return null;
        }
        for (ShowroomAwardSnapshot award : contentService.listAwards()) {
            if (!normalizedAwardCode.equals(normalizeExcelCell(award.awardCode()))) {
                continue;
            }
            try {
                return contentService.getCurrentOrLatestAwardRevision(award.awardId());
            } catch (RuntimeException exception) {
                throw new IllegalStateException("SHOWROOM_AWARD_IMPORT_EXISTING_REVISION_MISSING: 奖项 "
                        + normalizedAwardCode + " 缺少可回导的现有版本", exception);
            }
        }
        return null;
    }

    private String resolveImportedAwardCoverImage(ShowroomAwardExcelImportRow awardRow,
                                                  ShowroomAwardRevision existingRevision,
                                                  ShowroomProductImportMode importMode) {
        ShowroomProductImportExtra.ImportedCoverImage coverImage = awardRow.coverImage();
        if (coverImage == null) {
            if (importMode == ShowroomProductImportMode.BASE_WORKBOOK && existingRevision != null) {
                String currentCoverImage = normalizeExcelCell(existingRevision.fields().get("cover_image"));
                if (!hasText(currentCoverImage)) {
                    throw new IllegalStateException("SHOWROOM_AWARD_IMPORT_COVER_MISSING: 第 " + awardRow.rowNo()
                            + " 行奖项 " + awardRow.awardCode() + " 缺少现有封面，无法沿用");
                }
                return currentCoverImage;
            }
            if (importMode == ShowroomProductImportMode.BASE_WORKBOOK) {
                throw new IllegalStateException("SHOWROOM_AWARD_IMPORT_COVER_MISSING: 第 " + awardRow.rowNo()
                        + " 行新奖项 " + awardRow.awardCode() + " 缺少封面，无法创建");
            }
            throw new IllegalStateException("SHOWROOM_AWARD_IMPORT_COVER_MISSING: 第 " + awardRow.rowNo()
                    + " 行奖项必须提供封面");
        }
        if (existingRevision != null) {
            String currentCoverImage = normalizeExcelCell(existingRevision.fields().get("cover_image"));
            boolean currentCoverMatchesImportedCover;
            try {
                currentCoverMatchesImportedCover = productCoverImageService.importedCoverImageMatchesCurrentCover(
                        currentCoverImage, coverImage.content());
            } catch (IllegalStateException exception) {
                throw new IllegalStateException("SHOWROOM_AWARD_IMPORT_CURRENT_COVER_UNREADABLE: 奖项 "
                        + awardRow.awardCode() + " 现有封面不可读取，无法保证导出回导数据一致："
                        + exception.getMessage(), exception);
            }
            if (currentCoverMatchesImportedCover) {
                return currentCoverImage;
            }
        }
        return productCoverImageService.uploadImportedCoverImage(awardRow.awardCode(),
                coverImage.content(), coverImage.fileExtension(), coverImage.mimeType());
    }

    private Map<String, ShowroomNarrationExcelImportRow> indexNarrationRows(
            List<ShowroomNarrationExcelImportRow> narrationRows,
            ShowroomProductImportMode importMode) {
        if (narrationRows == null || narrationRows.isEmpty()) {
            return Map.of();
        }
        Map<String, ShowroomNarrationExcelImportRow> index = new LinkedHashMap<>();
        for (ShowroomNarrationExcelImportRow row : narrationRows) {
            String key = narrationRowKey(row.targetType(), row.targetCode(), row.language());
            ShowroomNarrationExcelImportRow previous = index.putIfAbsent(key, row);
            if (previous != null) {
                throw new IllegalStateException("SHOWROOM_NARRATION_IMPORT_DUPLICATE: 讲解音频重复，目标类型="
                        + row.targetType() + "，目标编码=" + row.targetCode() + "，语言=" + row.language());
            }
        }
        return Map.copyOf(index);
    }

    private ImportedNarrationPair resolveProductNarrationImportPair(String productCode,
                                                                    String productName,
                                                                    Map<String, ShowroomNarrationExcelImportRow> narrationByTargetAndLanguage) {
        return resolveImportedNarrationPair(TARGET_PRODUCT, productCode, productName, narrationByTargetAndLanguage);
    }

    private ImportedNarrationPair resolveAwardNarrationImportPair(String awardCode,
                                                                  String awardName,
                                                                  Map<String, ShowroomNarrationExcelImportRow> narrationByTargetAndLanguage) {
        return resolveImportedNarrationPair(TARGET_AWARD, awardCode, awardName, narrationByTargetAndLanguage);
    }

    private ImportedNarrationPair resolveImportedNarrationPair(String targetType,
                                                               String targetCode,
                                                               String targetName,
                                                               Map<String, ShowroomNarrationExcelImportRow> narrationByTargetAndLanguage) {
        ShowroomNarrationExcelImportRow zh = narrationByTargetAndLanguage.get(
                narrationRowKey(targetType, targetCode, ShowroomNarrationLanguage.ZH.name()));
        ShowroomNarrationExcelImportRow en = narrationByTargetAndLanguage.get(
                narrationRowKey(targetType, targetCode, ShowroomNarrationLanguage.EN.name()));
        if (zh == null && en == null) {
            return null;
        }
        if (zh == null || en == null) {
            throw new IllegalStateException("SHOWROOM_NARRATION_IMPORT_REQUIRED_FIELD_MISSING: "
                    + targetType + " " + targetCode + " 缺少中英文讲解音频行");
        }
        if (!normalizeExcelCell(targetName).isEmpty()) {
            validateNarrationTargetName(zh, targetName);
            validateNarrationTargetName(en, targetName);
        }
        return new ImportedNarrationPair(zh, en);
    }

    private void validateNarrationTargetName(ShowroomNarrationExcelImportRow row, String expectedName) {
        String providedName = normalizeExcelCell(row.targetName());
        if (hasText(providedName) && !providedName.equals(normalizeExcelCell(expectedName))) {
            throw new IllegalStateException("SHOWROOM_NARRATION_IMPORT_TARGET_NAME_MISMATCH: 第 " + row.rowNo()
                    + " 行目标名称与主数据不一致，期望=" + normalizeExcelCell(expectedName) + "，实际=" + providedName);
        }
    }

    private void replaceKeywords(List<ShowroomKeywordExcelImportRow> keywordRows,
                                 ShowroomProductImportMode importMode) {
        if (ShowroomProductImportMode.BASE_WORKBOOK.equals(importMode)) {
            return;
        }
        keywordMapper.deleteByTenantId(TenantContextHolder.getRequiredTenantId());
        for (ShowroomKeywordExcelImportRow row : keywordRows == null ? List.<ShowroomKeywordExcelImportRow>of() : keywordRows) {
            ShowroomKeywordDO keyword = new ShowroomKeywordDO();
            keyword.setTenantId(TenantContextHolder.getRequiredTenantId());
            keyword.setNameZh(normalizeExcelCell(row.nameZh()));
            keyword.setNameEn(normalizeExcelCell(row.nameEn()));
            keywordMapper.insert(keyword);
        }
    }

    private ShowroomAdminController.ProductDetailRespVO publishImportedProductTextOnly(
            ShowroomAdminController.ProductDraftReqVO req,
            Long operatorUserId,
            ImportedNarrationPair importedNarrationPair,
            boolean allowMissingNarration) {
        ShowroomProductRevision savedDraft = saveProductDraft(req);
        ProductNarrationPair publishPair = null;
        if (importedNarrationPair == null) {
            if (allowMissingNarration) {
                publishPair = findReusableProductNarrationPairForImport(savedDraft.productId(), savedDraft.revisionId());
            } else {
                Long sourceRevisionId = resolveProductNarrationSourceRevisionId(savedDraft.productId(), null);
                ProductNarrationPair sourcePair = requireProductNarrationPairForRevision(savedDraft.productId(),
                        sourceRevisionId);
                publishPair = sourcePair;
                if (!sourceRevisionId.equals(savedDraft.revisionId())) {
                    ProductNarrationPair draftPair = draftProductNarrationPair(savedDraft.productId(),
                            savedDraft.revisionId(), sourcePair);
                    publishPair = carryForwardProductNarrationAudioPair(draftPair, sourcePair);
                }
            }
        } else {
            publishPair = draftImportedProductNarrationPair(savedDraft, importedNarrationPair);
        }
        if (publishPair != null) {
            publishProductNarrationPair(publishPair);
        }
        ShowroomProductRevision published = contentService.publishProductRevision(savedDraft.revisionId(),
                operatorUserId);
        if (publishPair != null) {
            versionBundleService.ensureBundleForPublishedRevision(TARGET_PRODUCT, published.productId(),
                    published.revisionId(), operatorUserId, null);
        }
        assignmentService.markWholeProductAssignmentDirectPublished(published.productId(), operatorUserId,
                published.revisionId());
        return buildProductDetail(contentService.getProduct(published.productId()), published, true);
    }

    private ProductNarrationPair findReusableProductNarrationPairForImport(Long productId, Long draftRevisionId) {
        try {
            Long sourceRevisionId = resolveProductNarrationSourceRevisionId(productId, null);
            ProductNarrationPair sourcePair = requireProductNarrationPairForRevision(productId, sourceRevisionId);
            if (!hasReusableProductNarrationAudio(sourcePair.zh()) || !hasReusableProductNarrationAudio(sourcePair.en())) {
                return null;
            }
            if (!sourceRevisionId.equals(draftRevisionId)) {
                ProductNarrationPair draftPair = draftProductNarrationPair(productId, draftRevisionId, sourcePair);
                return carryForwardProductNarrationAudioPair(draftPair, sourcePair);
            }
            return sourcePair;
        } catch (IllegalStateException exception) {
            return null;
        }
    }

    private ShowroomAdminController.ProductDetailRespVO publishImportedNewProductTextOnly(
            ShowroomAdminController.ProductDraftReqVO req,
            Long operatorUserId,
            ImportedNarrationPair importedNarrationPair) {
        validateImportedNewProductPublishFields(req);
        ShowroomProductRevision savedDraft = saveProductDraft(req);
        if (importedNarrationPair != null) {
            ProductNarrationPair publishPair = draftImportedProductNarrationPair(savedDraft, importedNarrationPair);
            publishProductNarrationPair(publishPair);
        }
        ShowroomProductRevision published = contentService.publishProductRevision(savedDraft.revisionId(),
                operatorUserId);
        if (importedNarrationPair != null) {
            versionBundleService.ensureBundleForPublishedRevision(TARGET_PRODUCT, published.productId(),
                    published.revisionId(), operatorUserId, null);
        }
        return buildProductDetail(contentService.getProduct(published.productId()), published, true);
    }

    private ProductNarrationPair draftImportedProductNarrationPair(ShowroomProductRevision revision,
                                                                   ImportedNarrationPair importedNarrationPair) {
        ShowroomNarrationVersion zhDraft = draftImportedNarrationVersion(TARGET_PRODUCT, revision.productId(),
                revision.revisionId(), importedNarrationPair.zh(), ShowroomNarrationLanguage.ZH);
        ShowroomNarrationVersion enDraft = draftImportedNarrationVersion(TARGET_PRODUCT, revision.productId(),
                revision.revisionId(), importedNarrationPair.en(), ShowroomNarrationLanguage.EN);
        return new ProductNarrationPair(zhDraft, enDraft);
    }

    private void publishImportedAwardNarrationPair(ShowroomAwardRevision draft,
                                                   ImportedNarrationPair importedNarrationPair) {
        if (importedNarrationPair == null) {
            return;
        }
        ShowroomNarrationVersion zhDraft = draftImportedNarrationVersion(TARGET_AWARD, draft.awardId(),
                draft.revisionId(), importedNarrationPair.zh(), ShowroomNarrationLanguage.ZH);
        ShowroomNarrationVersion enDraft = draftImportedNarrationVersion(TARGET_AWARD, draft.awardId(),
                draft.revisionId(), importedNarrationPair.en(), ShowroomNarrationLanguage.EN);
        narrationService.publishDirectly(zhDraft.id());
        narrationService.publishDirectly(enDraft.id());
    }

    private ShowroomNarrationVersion draftImportedNarrationVersion(String targetType,
                                                                   Long targetId,
                                                                   Long sourceRevisionId,
                                                                   ShowroomNarrationExcelImportRow importedRow,
                                                                   ShowroomNarrationLanguage expectedLanguage) {
        if (!expectedLanguage.name().equalsIgnoreCase(normalizeExcelCell(importedRow.language()))) {
            throw new IllegalStateException("SHOWROOM_NARRATION_IMPORT_LANGUAGE_INVALID: 第 " + importedRow.rowNo()
                    + " 行语言不匹配，期望=" + expectedLanguage.name() + "，实际="
                    + normalizeExcelCell(importedRow.language()));
        }
        ShowroomNarrationVersion draft = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.valueOf(targetType),
                targetId,
                sourceRevisionId,
                ShowroomNarrationAudienceType.PUBLIC,
                expectedLanguage,
                normalizeExcelCell(importedRow.scriptText()),
                false));
        Long audioFileId = resolveImportedNarrationAudioFileId(importedRow);
        return narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(
                draft.id(),
                audioFileId,
                importedRow.audioDurationSeconds(),
                normalizeExcelCell(importedRow.voice())
        ));
    }

    private Long resolveImportedNarrationAudioFileId(ShowroomNarrationExcelImportRow importedRow) {
        if (importedRow.audioContent() != null) {
            if (importedRow.audioContent().length == 0) {
                throw new IllegalStateException("SHOWROOM_PRODUCT_RESOURCE_PACKAGE_ASSET_EMPTY: 第 "
                        + importedRow.rowNo() + " 行音频内容为空");
            }
            String audioUrl = normalizeExcelCell(importedRow.audioUrl());
            String fileName = ShowroomProductResourcePackage.isPackageUrl(audioUrl)
                    ? fileNameOf(ShowroomProductResourcePackage.packageAssetPath(audioUrl))
                    : fileNameOf(audioUrl);
            String mimeType = resolveNarrationMimeType(fileName);
            return fileService.createFileAndReturnId(importedRow.audioContent(), fileName,
                    "showroom/narration/imported", mimeType);
        }
        if (importedRow.audioFileId() != null) {
            return importedRow.audioFileId();
        }
        String audioUrl = normalizeExcelCell(importedRow.audioUrl());
        if (ShowroomProductResourcePackage.isPackageUrl(audioUrl)) {
            String assetPath = ShowroomProductResourcePackage.packageAssetPath(audioUrl);
            throw new IllegalStateException("SHOWROOM_PRODUCT_RESOURCE_PACKAGE_ASSET_MISSING: " + assetPath);
        }
        ParsedAdminFileUrl parsed = parseAdminFileUrl(audioUrl, importedRow.rowNo(), "音频地址");
        byte[] content;
        try {
            content = fileService.getFileContent(parsed.configId(), parsed.path());
        } catch (Exception ex) {
            throw new IllegalStateException("SHOWROOM_NARRATION_IMPORT_AUDIO_READ_FAILED: 第 " + importedRow.rowNo()
                    + " 行音频地址读取失败：" + importedRow.audioUrl(), ex);
        }
        if (content == null || content.length == 0) {
            throw new IllegalStateException("SHOWROOM_NARRATION_IMPORT_AUDIO_READ_FAILED: 第 " + importedRow.rowNo()
                    + " 行音频内容为空");
        }
        String fileName = fileNameOf(parsed.path());
        String mimeType = resolveNarrationMimeType(fileName);
        return fileService.createFileAndReturnId(content, fileName, "showroom/narration/imported", mimeType);
    }

    private ParsedAdminFileUrl parseAdminFileUrl(String rawUrl, int rowNo, String fieldLabel) {
        String normalized = normalizeExcelCell(rawUrl);
        Matcher matcher = Pattern.compile("^/admin-api/infra/file/(\\d+)/get/(.+)$").matcher(normalized);
        if (!matcher.matches()) {
            throw new IllegalStateException("SHOWROOM_NARRATION_IMPORT_REQUIRED_FIELD_MISSING: 第 " + rowNo
                    + " 行" + fieldLabel + "必须是系统导出的 /admin-api/infra/file/... 地址");
        }
        return new ParsedAdminFileUrl(Long.valueOf(matcher.group(1)),
                java.net.URLDecoder.decode(matcher.group(2), StandardCharsets.UTF_8));
    }

    private String resolveNarrationMimeType(String fileName) {
        String lower = fileName == null ? "" : fileName.toLowerCase();
        if (lower.endsWith(".wav")) {
            return "audio/wav";
        }
        if (lower.endsWith(".mp3")) {
            return "audio/mpeg";
        }
        if (lower.endsWith(".m4a")) {
            return "audio/mp4";
        }
        throw new IllegalStateException("SHOWROOM_NARRATION_IMPORT_AUDIO_UNSUPPORTED: 音频文件格式不支持：" + fileName);
    }

    private static String narrationRowKey(String targetType, String targetCode, String language) {
        return normalizeNarrationKeyPart(targetType) + "|" + normalizeNarrationKeyPart(targetCode) + "|"
                + normalizeNarrationKeyPart(language);
    }

    private static String normalizeNarrationKeyPart(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private static String fileNameOf(String path) {
        String normalized = path == null ? "" : path.trim();
        int separatorIndex = normalized.lastIndexOf('/');
        return separatorIndex >= 0 ? normalized.substring(separatorIndex + 1) : normalized;
    }

    private record ImportedNarrationPair(ShowroomNarrationExcelImportRow zh,
                                         ShowroomNarrationExcelImportRow en) {
    }

    private record ParsedAdminFileUrl(Long configId, String path) {
    }

    private Map<String, ShowroomHall> loadImportHallsByName() {
        Map<String, ShowroomHall> hallsByName = new LinkedHashMap<>();
        for (ShowroomHall hall : contentService.listHalls()) {
            String hallName = normalizeExcelCell(hall.name());
            if (!hasText(hallName)) {
                continue;
            }
            ShowroomHall previous = hallsByName.putIfAbsent(hallName, hall);
            if (previous != null) {
                throw new IllegalStateException("SHOWROOM_PRODUCT_IMPORT_HALL_AMBIGUOUS: 展柜名称重复，无法导入发布："
                        + hallName);
            }
        }
        return hallsByName;
    }

    private ShowroomHall resolveImportHall(String rawHallName, Map<String, ShowroomHall> importHallsByName,
                                           int rowNo, String productCode) {
        String hallName = normalizeExcelCell(rawHallName);
        if (!hasText(hallName)) {
            return null;
        }
        ShowroomHall hall = importHallsByName.get(hallName);
        if (hall == null) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_IMPORT_HALL_NOT_FOUND: 第 " + rowNo
                    + " 行展柜名称不存在，产品编码 " + productCode + "，展柜名称=" + hallName);
        }
        return hall;
    }

    private void replaceHallMappingsFromImportedProductList(List<ImportedHallProductMapping> importedMappings,
                                                            Set<Long> preservedProductIds) {
        Map<Long, List<ImportedHallProductMapping>> mappingsByHallId = new LinkedHashMap<>();
        for (ImportedHallProductMapping mapping : importedMappings) {
            mappingsByHallId.computeIfAbsent(mapping.hallId(), ignored -> new ArrayList<>()).add(mapping);
        }
        for (Map.Entry<Long, List<ImportedHallProductMapping>> entry : mappingsByHallId.entrySet()) {
            LinkedHashSet<Long> productIds = new LinkedHashSet<>();
            Map<Long, ImportedHallProductMapping> importedByProductId = new LinkedHashMap<>();
            for (ImportedHallProductMapping imported : entry.getValue()) {
                if (!productIds.add(imported.productId())) {
                    throw new IllegalStateException("SHOWROOM_PRODUCT_IMPORT_DUPLICATE_HALL_PRODUCT: 第 "
                            + imported.rowNo() + " 行产品在同一展柜重复，无法导入发布");
                }
                importedByProductId.put(imported.productId(), imported);
            }
            List<Long> orderedProductIds = new ArrayList<>();
            for (ShowroomHallProductMapping current : contentService.getHall(entry.getKey()).productMappings()) {
                Long productId = current.productId();
                if (preservedProductIds.contains(productId) || importedByProductId.containsKey(productId)) {
                    orderedProductIds.add(productId);
                }
            }
            for (ImportedHallProductMapping imported : entry.getValue()) {
                if (!orderedProductIds.contains(imported.productId())) {
                    orderedProductIds.add(imported.productId());
                }
            }
            List<ShowroomHallProductMapping> mappings = new ArrayList<>();
            int displayOrder = 1;
            for (Long productId : orderedProductIds) {
                mappings.add(new ShowroomHallProductMapping(productId, displayOrder++));
            }
            contentService.replaceHallProductMappings(entry.getKey(), mappings);
        }
    }

    private record ImportedHallProductMapping(Long hallId, Long productId, int rowNo) {
    }

    public ShowroomHall createHall(ShowroomAdminController.HallSaveReqVO req) {
        return contentService.createHall(req.hallCode(), req.name(), req.nameEn(),
                req.description(), req.descriptionEn());
    }

    public ShowroomHall updateHall(ShowroomAdminController.HallUpdateReqVO req) {
        return contentService.updateHall(req.hallId(), req.name(), req.nameEn(),
                req.description(), req.descriptionEn());
    }

    public ShowroomHall updateHallProductMapping(ShowroomAdminController.HallMappingReqVO req) {
        List<ShowroomHallProductMapping> mappings = requireList(req.products(),
                "SHOWROOM_REQUIRED_FIELD_MISSING: hall product mappings are required")
                .stream()
                .map(this::toHallProductMapping)
                .toList();
        return contentService.replaceHallProductMappings(req.hallId(), mappings);
    }

    public ShowroomHall updateHallCanvasLayout(ShowroomAdminController.HallMappingReqVO req) {
        List<ShowroomHallProductMapping> mappings = requireList(req.products(),
                "SHOWROOM_REQUIRED_FIELD_MISSING: hall product mappings are required")
                .stream()
                .map(this::toHallProductMapping)
                .toList();
        return contentService.replaceHallCanvasLayout(req.hallId(), mappings);
    }

    public ShowroomHall updateHallItemMapping(ShowroomAdminController.HallItemMappingReqVO req) {
        List<ShowroomHallItemMapping> mappings = requireList(req.items(),
                "SHOWROOM_REQUIRED_FIELD_MISSING: hall item mappings are required")
                .stream()
                .map(this::toHallItemMapping)
                .toList();
        return contentService.replaceHallItemMappings(req.hallId(), mappings);
    }

    public ShowroomHall updateHallItemCanvasLayout(ShowroomAdminController.HallItemMappingReqVO req) {
        List<ShowroomHallItemMapping> mappings = requireList(req.items(),
                "SHOWROOM_REQUIRED_FIELD_MISSING: hall item mappings are required")
                .stream()
                .map(this::toHallItemMapping)
                .toList();
        return contentService.replaceHallItemCanvasLayout(req.hallId(), mappings);
    }

    public ShowroomAdminController.HallItemMappingReqVO calculateHallBuCanvasLayout(
            ShowroomAdminController.HallItemMappingReqVO req) {
        Objects.requireNonNull(req, "SHOWROOM_REQUIRED_FIELD_MISSING: hall item mappings request is required");
        Long hallId = requireId(req.hallId(), "SHOWROOM_TARGET_NOT_FOUND: hall id is required");
        List<ShowroomAdminController.HallItemMappingItemReqVO> items = requireList(req.items(),
                "SHOWROOM_REQUIRED_FIELD_MISSING: hall item mappings are required");
        List<HallBuLayoutItem> products = new ArrayList<>();
        List<ShowroomAdminController.HallItemMappingItemReqVO> awards = new ArrayList<>();
        Map<String, Integer> buOrder = new LinkedHashMap<>();
        int originalIndex = 0;
        for (ShowroomAdminController.HallItemMappingItemReqVO item : items) {
            Objects.requireNonNull(item, "SHOWROOM_REQUIRED_FIELD_MISSING: hall item mapping is required");
            requireText(item.itemType(), "SHOWROOM_REQUIRED_FIELD_MISSING: hall item type is required");
            requireId(item.itemId(), "SHOWROOM_TARGET_NOT_FOUND: hall item id is required");
            if ("PRODUCT".equals(item.itemType())) {
                ShowroomProductRevision revision = contentService.getCurrentOrLatestProductRevision(item.itemId());
                String bu = normalizeBu(revision.fields().get("pipeline_layout"));
                buOrder.computeIfAbsent(bu, ignored -> buOrder.size());
                products.add(new HallBuLayoutItem(item, bu, originalIndex));
            } else {
                awards.add(item);
            }
            originalIndex++;
        }
        if (products.isEmpty()) {
            List<ShowroomAdminController.HallItemMappingItemReqVO> calculated = new ArrayList<>();
            awards.stream()
                    .sorted(Comparator
                            .comparingInt((ShowroomAdminController.HallItemMappingItemReqVO item) ->
                                    item.displayOrder() == null ? Integer.MAX_VALUE : item.displayOrder())
                            .thenComparing(ShowroomAdminController.HallItemMappingItemReqVO::itemId))
                    .forEach(item -> calculated.add(new ShowroomAdminController.HallItemMappingItemReqVO(
                            item.itemType(), item.itemId(), calculated.size() + 1,
                            item.layoutX(), item.layoutY(), item.layoutWidth(), item.layoutHeight())));
            return new ShowroomAdminController.HallItemMappingReqVO(hallId, calculated);
        }
        products.sort(Comparator
                .comparingInt((HallBuLayoutItem item) -> buOrder.get(item.bu()))
                .thenComparingInt(item -> item.mapping().displayOrder() == null
                        ? Integer.MAX_VALUE : item.mapping().displayOrder())
                .thenComparingInt(HallBuLayoutItem::originalIndex)
                .thenComparingLong(item -> item.mapping().itemId()));
        List<ShowroomAdminController.HallItemMappingItemReqVO> calculated = new ArrayList<>();
        List<CanvasRect> rects = equalGrid(products.size());
        for (int index = 0; index < products.size(); index++) {
            ShowroomAdminController.HallItemMappingItemReqVO source = products.get(index).mapping();
            CanvasRect rect = rects.get(index);
            calculated.add(new ShowroomAdminController.HallItemMappingItemReqVO(
                    source.itemType(), source.itemId(), calculated.size() + 1,
                    rect.x(), rect.y(), rect.width(), rect.height()));
        }
        awards.stream()
                .sorted(Comparator
                        .comparingInt((ShowroomAdminController.HallItemMappingItemReqVO item) ->
                                item.displayOrder() == null ? Integer.MAX_VALUE : item.displayOrder())
                        .thenComparing(ShowroomAdminController.HallItemMappingItemReqVO::itemId))
                .forEach(item -> calculated.add(new ShowroomAdminController.HallItemMappingItemReqVO(
                        item.itemType(), item.itemId(), calculated.size() + 1,
                        item.layoutX(), item.layoutY(), item.layoutWidth(), item.layoutHeight())));
        return new ShowroomAdminController.HallItemMappingReqVO(hallId, calculated);
    }

    public ShowroomHall updateHallCanvasBackground(ShowroomAdminController.HallCanvasBackgroundReqVO req) {
        Objects.requireNonNull(req, "SHOWROOM_REQUIRED_FIELD_MISSING: hall canvas background request is required");
        Objects.requireNonNull(req.hallId(), "SHOWROOM_REQUIRED_FIELD_MISSING: hall id is required");
        return contentService.updateHallCanvasBackground(req.hallId(), req.canvasBackgroundImageUrl());
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowroomAdminController.HallPreviewAssetPublishRespVO publishHallPreviewAsset(
            ShowroomAdminController.HallPreviewAssetPublishReqVO req) {
        Objects.requireNonNull(req, "SHOWROOM_REQUIRED_FIELD_MISSING: hall preview asset request is required");
        Long hallId = requireId(req.hallId(), "SHOWROOM_TARGET_NOT_FOUND: hall id is required");
        Long imageFileId = requireId(req.imageFileId(),
                "SHOWROOM_PREVIEW_STATIC_ASSET_MISSING: hall preview imageFileId is required");
        ShowroomHall hall = contentService.getHall(hallId);
        requirePreviewFile(imageFileId);
        ShowroomPreviewAssetDraftCommand command = new ShowroomPreviewAssetDraftCommand(
                ShowroomPreviewAssetTargetType.HALL,
                hall.hallId(),
                hall.hallId(),
                new ShowroomPreviewAssetFiles(imageFileId, imageFileId, imageFileId));
        ShowroomPreviewAssetVersion draft = previewAssetService.bindStaticPreviewAssets(command);
        ShowroomPreviewAssetVersion published = previewAssetService.publishDirectly(draft.id());
        return new ShowroomAdminController.HallPreviewAssetPublishRespVO(
                hall.hallId(),
                published.id(),
                published.files().desktopFileId(),
                fileUrl(published.files().desktopFileId()),
                published.live());
    }

    private ShowroomHallProductMapping toHallProductMapping(
            ShowroomAdminController.HallProductMappingReqVO item) {
        return new ShowroomHallProductMapping(item.productId(), item.displayOrder(),
                item.layoutX(), item.layoutY(), item.layoutWidth(), item.layoutHeight());
    }

    private ShowroomHallItemMapping toHallItemMapping(
            ShowroomAdminController.HallItemMappingItemReqVO item) {
        return new ShowroomHallItemMapping(item.itemType(), item.itemId(), item.displayOrder(),
                item.layoutX(), item.layoutY(), item.layoutWidth(), item.layoutHeight());
    }

    private static List<CanvasRect> equalGrid(int itemCount) {
        int rows = Math.max(1, (int) Math.floor(Math.sqrt(itemCount)));
        int columns = (int) Math.ceil((double) itemCount / rows);
        List<CanvasRect> rects = new ArrayList<>();
        int index = 0;
        for (int row = 0; row < rows && index < itemCount; row++) {
            int remaining = itemCount - index;
            int rowCount = Math.min(columns, remaining);
            BigDecimal y = ratio(row, rows);
            BigDecimal nextY = ratio(row + 1, rows);
            BigDecimal height = nextY.subtract(y).setScale(6, RoundingMode.HALF_UP);
            for (int column = 0; column < rowCount; column++) {
                BigDecimal x = ratio(column, rowCount);
                BigDecimal nextX = ratio(column + 1, rowCount);
                rects.add(new CanvasRect(x, y,
                        nextX.subtract(x).setScale(6, RoundingMode.HALF_UP), height));
                index++;
            }
        }
        return rects;
    }

    private static BigDecimal ratio(int numerator, int denominator) {
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 6, RoundingMode.HALF_UP);
    }

    private static String normalizeBu(String value) {
        String normalized = nullToEmpty(value).trim();
        return hasText(normalized) ? normalized : "未分类";
    }

    private record HallBuLayoutItem(ShowroomAdminController.HallItemMappingItemReqVO mapping,
                                    String bu,
                                    int originalIndex) {
    }

    private record CanvasRect(BigDecimal x, BigDecimal y, BigDecimal width, BigDecimal height) {
    }

    public List<ShowroomAdminController.HallProductOptionRespVO> listHallProductOptions() {
        return contentService.listHallProductOptions().stream()
                .map(option -> new ShowroomAdminController.HallProductOptionRespVO(option.productId(),
                        option.productMasterId(), option.productCode(), option.nameCn(), option.revisionNo(), option.incomplete(),
                        option.previewImageUrl(),
                        option.hallIds()))
                .toList();
    }

    public List<ShowroomAdminController.HallItemOptionRespVO> listHallItemOptions() {
        return contentService.listHallItemOptions().stream()
                .map(option -> new ShowroomAdminController.HallItemOptionRespVO(option.itemType(), option.itemId(),
                        option.itemCode(), option.nameCn(), option.nameEn(), option.revisionNo(), option.incomplete(),
                        option.previewImageUrl(), option.hallIds()))
                .toList();
    }

    public List<ShowroomAdminController.HallPageRespVO> listHalls() {
        return contentService.listHalls().stream()
                .map(this::toHallPageRow)
                .toList();
    }

    public List<ShowroomAdminController.HallPageRespVO> listHalls(ShowroomAdminController.PageQueryReqVO req) {
        List<ShowroomAdminController.HallPageRespVO> halls = contentService.listHalls().stream()
                .filter(hall -> matchesHall(hall, req.keyword()))
                .map(this::toHallPageRow)
                .toList();
        return page(halls, req.pageNo(), req.pageSize());
    }

    public void deleteHall(Long hallId) {
        contentService.deleteHall(hallId);
    }

    public List<ShowroomAdminController.VersionHistoryRespVO> versionHistory(String targetType, Long targetId) {
        Map<Long, List<ShowroomVersionAudit>> auditsByRevision = new LinkedHashMap<>();
        for (ShowroomVersionAudit audit : contentService.versionAudits(targetType, targetId)) {
            auditsByRevision.computeIfAbsent(audit.revisionId(), ignored -> new ArrayList<>()).add(audit);
        }
        return auditsByRevision.entrySet().stream()
                .map(entry -> toVersionHistory(targetType, entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(ShowroomAdminController.VersionHistoryRespVO::revisionNo,
                        Comparator.nullsLast(Integer::compareTo)).reversed())
                .toList();
    }

    public ShowroomAdminController.NarrationVersionRespVO getNarration(String targetType, Long targetId,
                                                                       String audienceType, String language) {
        ShowroomNarrationVersion version = narrationService.latest(new ShowroomNarrationKey(
                        ShowroomNarrationTargetType.valueOf(targetType), targetId,
                        ShowroomNarrationAudienceType.valueOf(audienceType), ShowroomNarrationLanguage.valueOf(language)))
                .orElseThrow(() -> ServiceExceptionUtil.exception0(GlobalErrorCodeConstants.BAD_REQUEST.getCode(),
                        "SHOWROOM_TARGET_NOT_FOUND: narration not found"));
        return toNarrationVersionResp(version);
    }

    public ShowroomProductComment createComment(ShowroomAdminController.ProductCommentCreateReqVO req) {
        return commentService.createThread(req.productId(), req.targetRevisionId(), req.changeRequestId(),
                ShowroomCommentAnchorType.valueOf(req.anchorType()), req.anchorKey(), req.createdBy(), req.content());
    }

    public List<ShowroomProductComment> pageComments(ShowroomAdminController.ProductCommentPageReqVO req) {
        ShowroomCommentAnchorType anchorType = req.anchorType() == null ? null
                : ShowroomCommentAnchorType.valueOf(req.anchorType());
        return commentService.pageByProduct(req.productId(), anchorType, req.anchorKey(), req.changeRequestId());
    }

    public ShowroomNarrationVersion saveNarrationDraft(ShowroomAdminController.NarrationDraftReqVO req) {
        ShowroomNarrationVersion version = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.valueOf(req.targetType()), req.targetId(), req.sourceRevisionId(),
                ShowroomNarrationAudienceType.valueOf(req.audienceType()),
                ShowroomNarrationLanguage.valueOf(req.language()), req.scriptText(), req.generatedByAi()));
        if (req.audioFileId() != null) {
            if (req.audioDurationSeconds() == null || req.audioDurationSeconds() <= 0) {
                throw new IllegalStateException("SHOWROOM_AUDIO_GENERATION_FAILED: audio duration is required");
            }
            version = narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(version.id(),
                    req.audioFileId(), req.audioDurationSeconds()));
        }
        return version;
    }

    public ShowroomNarrationVersion generateNarrationAudio(ShowroomAdminController.NarrationAudioGenerateReqVO req) {
        return narrationService.generateAudio(req.narrationVersionId());
    }

    public ShowroomNarrationVersion submitNarration(ShowroomAdminController.NarrationSubmitReqVO req) {
        if (!req.manualConfirmed()) {
            throw new IllegalStateException(
                    "SHOWROOM_MANUAL_CONFIRMATION_REQUIRED: narration assets must be manually confirmed before submit");
        }
        return narrationService.submit(req.narrationVersionId());
    }

    public ShowroomNarrationVersion supervisorApproveNarration(ShowroomAdminController.NarrationApprovalReqVO req) {
        return narrationService.supervisorApprove(req.narrationVersionId(), req.reviewerUserId());
    }

    public ShowroomNarrationVersion gaoxinApproveNarration(ShowroomAdminController.NarrationApprovalReqVO req) {
        return narrationService.gaoxinApprove(req.narrationVersionId(), req.reviewerUserId());
    }

    public ShowroomNarrationVersion publishNarration(ShowroomAdminController.NarrationPublishReqVO req) {
        ShowroomNarrationVersion published = narrationService.publish(req.narrationVersionId());
        markReleaseDirtyForNarrationIfCurrent(published, null, "PUBLIC_NARRATION_PUBLISHED");
        return published;
    }

    public ShowroomAdminController.NarrationTtsDefaultsRespVO getNarrationTtsDefaults() {
        AiTtsAliyunNlsCredentialService.VoiceStatus voiceStatus =
                aliyunNlsCredentialService.getVoiceStatus(yudaoAiProperties.getTts());
        AiTtsAliyunNlsCredentialService.AppKeyStatus appKeyStatus =
                aliyunNlsCredentialService.getAppKeyStatus(yudaoAiProperties.getTts());
        AiTtsAliyunNlsCredentialService.AccessTokenStatus tokenStatus =
                aliyunNlsCredentialService.getAccessTokenStatus(yudaoAiProperties.getTts());
        return new ShowroomAdminController.NarrationTtsDefaultsRespVO(voiceStatus.voice(),
                voiceStatus.saved(), voiceStatus.configured(), voiceStatus.source(), appKeyStatus.saved(),
                appKeyStatus.configured(), appKeyStatus.source(), appKeyStatus.maskedAppKey(),
                tokenStatus.saved(), tokenStatus.configured(), tokenStatus.source(), tokenStatus.maskedAccessToken());
    }

    public boolean saveNarrationTtsDefaultVoice(String voice) {
        aliyunNlsCredentialService.saveVoice(voice);
        return true;
    }

    public boolean saveNarrationTtsDefaultToken(String accessToken) {
        aliyunNlsCredentialService.saveAccessToken(accessToken);
        return true;
    }

    public boolean saveNarrationTtsDefaultAppKey(String appKey) {
        aliyunNlsCredentialService.saveAppKey(appKey);
        return true;
    }

    public ShowroomDisplayController.RuntimeClientSettingsRespVO getRuntimeClientSettings() {
        String value = getConfigValue(RUNTIME_CLIENT_SETTINGS_CONFIG_KEY, null);
        if (value == null || value.isBlank()) {
            return defaultRuntimeClientSettings();
        }
        ShowroomDisplayController.RuntimeClientSettingsRespVO parsed =
                JsonUtils.parseObjectQuietly(value, ShowroomDisplayController.RuntimeClientSettingsRespVO.class);
        if (parsed == null) {
            throw new IllegalStateException(
                    "SHOWROOM_RUNTIME_CLIENT_SETTINGS_INVALID: saved runtime client settings JSON is invalid");
        }
        return normalizeRuntimeClientSettings(parsed.companyDetailSettings());
    }

    public ShowroomDisplayController.RuntimeClientSettingsRespVO saveRuntimeClientSettings(
            ShowroomDisplayController.RuntimeClientSettingsSaveReqVO reqVO) {
        ShowroomDisplayController.RuntimeClientSettingsRespVO normalized =
                normalizeRuntimeClientSettings(reqVO == null ? null : reqVO.companyDetailSettings());
        upsertHiddenConfig(RUNTIME_CLIENT_SETTINGS_CONFIG_KEY, RUNTIME_CLIENT_SETTINGS_CONFIG_NAME,
                JsonUtils.toJsonString(normalized), RUNTIME_CLIENT_SETTINGS_CONFIG_REMARK);
        return normalized;
    }

    private ShowroomDisplayController.RuntimeClientSettingsRespVO defaultRuntimeClientSettings() {
        return normalizeRuntimeClientSettings(null);
    }

    private ShowroomDisplayController.RuntimeClientSettingsRespVO normalizeRuntimeClientSettings(
            ShowroomDisplayController.RuntimeClientCompanyDetailSettings settings) {
        return new ShowroomDisplayController.RuntimeClientSettingsRespVO(
                new ShowroomDisplayController.RuntimeClientCompanyDetailSettings(
                        normalizeRuntimeClientProductItemGap(
                                settings == null ? null : settings.productItemHorizontalGap(),
                                "productItemHorizontalGap"),
                        normalizeRuntimeClientProductItemGap(
                                settings == null ? null : settings.productItemVerticalGap(),
                                "productItemVerticalGap")));
    }

    private int normalizeRuntimeClientProductItemGap(Integer value, String fieldName) {
        int normalized = value == null ? RUNTIME_CLIENT_DEFAULT_PRODUCT_ITEM_GAP : value;
        if (normalized < RUNTIME_CLIENT_PRODUCT_ITEM_GAP_MIN || normalized > RUNTIME_CLIENT_PRODUCT_ITEM_GAP_MAX) {
            throw new IllegalArgumentException("SHOWROOM_RUNTIME_CLIENT_SETTINGS_INVALID: " + fieldName
                    + " must be between " + RUNTIME_CLIENT_PRODUCT_ITEM_GAP_MIN + " and "
                    + RUNTIME_CLIENT_PRODUCT_ITEM_GAP_MAX + ".");
        }
        return normalized;
    }

    public ShowroomDisplayController.HomePayload displayHome() {
        ShowroomCompanyRevision company = contentService.requireCurrentCompanyRevision();
        List<ShowroomDisplayController.DisplayCard> hallEntries = contentService.listHalls().stream()
                .map(hall -> new ShowroomDisplayController.DisplayCard(hall.hallId(), hall.name(), "",
                        false, previewImageUrl(TARGET_HALL, hall.hallId())))
                .toList();
        return new ShowroomDisplayController.HomePayload(companySummary(company), hallEntries,
                narrationSummary(TARGET_COMPANY, company.companyId()));
    }

    public ShowroomDisplayController.HallPayload displayHall(Long hallId) {
        ShowroomHall hall = contentService.getHall(hallId);
        List<ShowroomDisplayController.DisplayCard> productCards = hall.productMappings().stream()
                .map(mapping -> productCard(mapping.productId()))
                .toList();
        return new ShowroomDisplayController.HallPayload(new ShowroomDisplayController.HallInfo(hall.hallId(),
                hall.name(), hall.description()), productCards, List.of(), narrationSummary(TARGET_HALL, hall.hallId()));
    }

    public ShowroomDisplayController.NarrationPayload displayNarration(String targetType, Long targetId,
                                                                       String audienceType, String language) {
        ShowroomNarrationVersion version = narrationService.live(new ShowroomNarrationKey(
                ShowroomNarrationTargetType.valueOf(targetType), targetId,
                ShowroomNarrationAudienceType.valueOf(audienceType), ShowroomNarrationLanguage.valueOf(language)))
                .orElseThrow(() -> new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: live narration not found"));
        return new ShowroomDisplayController.NarrationPayload(version.scriptText(), fileUrl(version.audioFileId()));
    }

    private ShowroomDisplayController.WebsiteConfigCompany toWebsiteConfigCompany(ShowroomCompanySnapshot snapshot,
                                                                                  ShowroomCompanyRevision revision) {
        AppConfigNarrationPair narrations = requireLiveNarrationPair(ShowroomNarrationTargetType.COMPANY,
                revision.companyId(), revision.revisionId(), "company");
        return new ShowroomDisplayController.WebsiteConfigCompany(revision.companyId(),
                requireText(snapshot.displayName(), "SHOWROOM_REQUIRED_FIELD_MISSING: company display name is required"),
                requireText(snapshot.displayNameEn(),
                        "SHOWROOM_REQUIRED_FIELD_MISSING: company display name_en is required"),
                requireText(revision.fields().get("cover_image"),
                        "SHOWROOM_REQUIRED_FIELD_MISSING: company cover_image is required"),
                narrations.zh().scriptText(), narrations.en().scriptText(),
                fileUrl(narrations.zh().audioFileId()), fileUrl(narrations.en().audioFileId()),
                companyFields(revision.fields()),
                companyBilingualFields(revision.fields()));
    }

    private ShowroomDisplayController.WebsiteConfigShowroom toWebsiteConfigShowroom(ShowroomHall hall) {
        AppConfigNarrationPair narrations = requireLiveNarrationPair(ShowroomNarrationTargetType.HALL,
                hall.hallId(), hall.hallId(), "hall");
        List<ShowroomDisplayController.WebsiteConfigProduct> products = new ArrayList<>();
        for (ShowroomHallProductMapping mapping : requireList(hall.productMappings(),
                "SHOWROOM_TARGET_NOT_FOUND: hall product mappings are required for website config")) {
            products.add(toWebsiteConfigProduct(mapping.productId()));
        }
        return new ShowroomDisplayController.WebsiteConfigShowroom(hall.hallId(),
                requireText(hall.hallCode(), "SHOWROOM_REQUIRED_FIELD_MISSING: hall code is required"),
                requireText(hall.name(), "SHOWROOM_REQUIRED_FIELD_MISSING: hall name is required"),
                requireText(hall.nameEn(), "SHOWROOM_REQUIRED_FIELD_MISSING: hall name_en is required"),
                nullToEmpty(hall.description()),
                nullToEmpty(hall.descriptionEn()),
                requirePreviewImageUrl(TARGET_HALL, hall.hallId(), null, "hall"),
                fileUrl(narrations.zh().audioFileId()), fileUrl(narrations.en().audioFileId()),
                products);
    }

    private ShowroomDisplayController.WebsiteConfigProduct toWebsiteConfigProduct(Long productId) {
        ShowroomProductSnapshot snapshot = contentService.getProduct(productId);
        ShowroomProductRevision revision = contentService.requireCurrentProductRevision(productId);
        AppConfigNarrationPair narrations = requireLiveNarrationPair(ShowroomNarrationTargetType.PRODUCT,
                productId, revision.revisionId(), "product");
        return new ShowroomDisplayController.WebsiteConfigProduct(productId,
                requireText(snapshot.productCode(), "SHOWROOM_REQUIRED_FIELD_MISSING: product code is required"),
                nullToEmpty(revision.nameCn()),
                requireText(revision.nameEn(), "SHOWROOM_REQUIRED_FIELD_MISSING: product name_en is required"),
                revision.incomplete(),
                resolveProductDisplayImageUrl(productId, revision),
                narrations.zh().scriptText(), narrations.en().scriptText(),
                fileUrl(narrations.zh().audioFileId()), fileUrl(narrations.en().audioFileId()),
                productFields(revision), productBilingualFields(revision));
    }

    private ShowroomAdminController.ProductPageRespVO toProductPageRow(ShowroomProductSnapshot snapshot,
                                                                       boolean editable) {
        ShowroomProductRevision latestRevision = contentService.getLatestProductRevision(snapshot.productId());
        ShowroomProductRevision displayRevision = resolveDisplayProductRevision(snapshot, latestRevision);
        return toProductPageRow(snapshot, latestRevision, displayRevision, editable);
    }

    private ShowroomAdminController.ProductPageRespVO toProductPageRow(ShowroomProductSnapshot snapshot,
                                                                       ShowroomProductRevision latestRevision,
                                                                       ShowroomProductRevision displayRevision,
                                                                       boolean editable) {
        ShowroomAdminController.ProductDetailRespVO detail = buildProductDetail(snapshot, latestRevision, editable);
        ShowroomAdminController.ProductDetailRespVO displayDetail = buildProductDetail(snapshot, displayRevision,
                editable && latestRevision.revisionId().equals(displayRevision.revisionId()));
        return new ShowroomAdminController.ProductPageRespVO(snapshot.productId(), snapshot.productMasterId(),
                snapshot.productCode(), snapshot.legacyProductCode(), displayDetail.currentRevisionId(),
                displayDetail.incomplete(), displayDetail.live(), detail,
                displayDetail, latestNarrationSummary(snapshot.productId(), displayRevision.revisionId()), editable);
    }

    private ShowroomAdminController.ProductDetailRespVO buildProductDetail(ShowroomProductSnapshot snapshot,
                                                                          ShowroomProductRevision revision,
                                                                          boolean editable) {
        List<ShowroomProductComment> comments = commentService.pageByProduct(snapshot.productId(), null, null, null);
        return new ShowroomAdminController.ProductDetailRespVO(snapshot.productId(), snapshot.productMasterId(),
                snapshot.productCode(), snapshot.legacyProductCode(),
                snapshot.currentRevisionId().orElse(revision.revisionId()), revision.incomplete(), snapshot.live(),
                revision.revisionId(), revision.revisionNo(), resolveProductStatus(snapshot.productId(), revision),
                revision.nameCn(), revision.nameEn(), revision.fields(), relatedProductIds(revision.revisionId()),
                discussionSummary(comments), narrationAvailabilities(ShowroomNarrationTargetType.PRODUCT,
                snapshot.productId()), activeProductAssignment(snapshot.productId()), editable,
                toProductAttachmentRespVOs(revision.attachments()), productMaterialBlockers(revision));
    }

    private AwardPageRespVO toAwardPageRow(ShowroomAwardSnapshot snapshot) {
        ShowroomAwardRevision latestRevision = contentService.getLatestAwardRevision(snapshot.awardId());
        ShowroomAwardRevision displayRevision = snapshot.currentRevisionId()
                .map(contentService::getAwardRevision)
                .orElse(latestRevision);
        return new AwardPageRespVO(snapshot.awardId(), snapshot.awardCode(),
                snapshot.currentRevisionId().orElse(null), snapshot.incomplete(), snapshot.live(),
                buildAwardDetail(snapshot, latestRevision, true),
                buildAwardDetail(snapshot, displayRevision, latestRevision.revisionId().equals(displayRevision.revisionId())));
    }

    private AwardDetailRespVO buildAwardDetail(ShowroomAwardSnapshot snapshot,
                                                                       ShowroomAwardRevision revision,
                                                                       boolean editable) {
        return new AwardDetailRespVO(snapshot.awardId(), snapshot.awardCode(),
                snapshot.currentRevisionId().orElse(revision.revisionId()), revision.incomplete(), snapshot.live(),
                revision.revisionId(), revision.revisionNo(), revision.status(), revision.nameCn(), revision.nameEn(),
                nullToEmpty(revision.fields().get("description_zh")),
                nullToEmpty(revision.fields().get("description_en")),
                nullToEmpty(revision.fields().get("issuer")),
                nullToEmpty(revision.fields().get("award_date_text")),
                nullToEmpty(revision.fields().get("cover_image")),
                narrationAvailabilities(ShowroomNarrationTargetType.AWARD, snapshot.awardId()), editable,
                awardMaterialBlockers(revision));
    }

    private boolean matchesAward(ShowroomAwardSnapshot snapshot, String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        if (!hasText(normalizedKeyword)) {
            return true;
        }
        if (containsIgnoreCase(snapshot.awardCode(), normalizedKeyword)) {
            return true;
        }
        ShowroomAwardRevision revision = contentService.getCurrentOrLatestAwardRevision(snapshot.awardId());
        return containsIgnoreCase(revision.nameCn(), normalizedKeyword)
                || containsIgnoreCase(revision.nameEn(), normalizedKeyword)
                || containsIgnoreCase(revision.fields().get("issuer"), normalizedKeyword);
    }

    private ShowroomAwardRevision resolveTargetAwardRevision(Long awardId, Long revisionId,
                                                             ShowroomAwardRevision latestRevision) {
        if (revisionId == null) {
            return latestRevision;
        }
        ShowroomAwardRevision revision = contentService.getAwardRevision(revisionId);
        if (!awardId.equals(revision.awardId())) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: award revision not found");
        }
        return revision;
    }

    private List<ShowroomAdminController.MaterialBlockerRespVO> awardMaterialBlockers(
            ShowroomAwardRevision revision) {
        List<ShowroomAdminController.MaterialBlockerRespVO> blockers = new ArrayList<>();
        if (!hasText(revision.nameCn())) {
            blockers.add(new ShowroomAdminController.MaterialBlockerRespVO(
                    "AWARD_NAME_CN_MISSING", "award name_cn is missing", TARGET_AWARD, revision.awardId(),
                    null, List.of("name_cn"), null, null, null, "AWARD_NAME_CN_MISSING"));
        }
        if (!hasText(revision.fields().get("cover_image"))) {
            blockers.add(new ShowroomAdminController.MaterialBlockerRespVO(
                    "AWARD_COVER_MISSING", "award cover_image is missing", TARGET_AWARD, revision.awardId(),
                    null, List.of("cover_image"), null, "award-" + revision.awardId() + "-preview",
                    null, "AWARD_COVER_MISSING"));
        }
        appendAwardNarrationMaterialBlocker(blockers, revision, ShowroomNarrationLanguage.ZH);
        appendAwardNarrationMaterialBlocker(blockers, revision, ShowroomNarrationLanguage.EN);
        return List.copyOf(blockers);
    }

    private void appendAwardNarrationMaterialBlocker(List<ShowroomAdminController.MaterialBlockerRespVO> blockers,
                                                     ShowroomAwardRevision revision,
                                                     ShowroomNarrationLanguage language) {
        ShowroomNarrationVersion live = narrationService.live(new ShowroomNarrationKey(
                ShowroomNarrationTargetType.AWARD, revision.awardId(),
                ShowroomNarrationAudienceType.PUBLIC, language)).orElse(null);
        if (live == null || !revision.revisionId().equals(live.sourceRevisionId()) || live.audioFileId() == null) {
            blockers.add(new ShowroomAdminController.MaterialBlockerRespVO(
                    "AWARD_NARRATION_" + language.name() + "_MISSING",
                    "award " + language.name() + " live narration audio is missing",
                    TARGET_AWARD, revision.awardId(), language.name(), List.of(), null,
                    "award-" + revision.awardId() + "-audio-" + language.name().toLowerCase(),
                    null, "AWARD_NARRATION_MISSING"));
        }
    }

    private static List<ShowroomProductAttachment> toProductAttachments(
            List<ShowroomAdminController.ProductAttachmentReqVO> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        return attachments.stream()
                .map(attachment -> new ShowroomProductAttachment(null, null, null, attachment.assetType(),
                        attachment.fileId(), attachment.originalName(), attachment.mimeType(), attachment.size(),
                        attachment.displayOrder() == null ? 0 : attachment.displayOrder()))
                .toList();
    }

    private List<ShowroomAdminController.ProductAttachmentRespVO> toProductAttachmentRespVOs(
            List<ShowroomProductAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        return attachments.stream()
                .map(attachment -> new ShowroomAdminController.ProductAttachmentRespVO(attachment.id(),
                        attachment.assetType(), attachment.fileId(), fileUrl(attachment.fileId()),
                        attachment.originalName(), attachment.mimeType(), attachment.fileSize(),
                        attachment.displayOrder()))
                .toList();
    }

    private List<ShowroomAdminController.MaterialBlockerRespVO> productMaterialBlockers(
            ShowroomProductRevision revision) {
        List<ShowroomAdminController.MaterialBlockerRespVO> blockers = new ArrayList<>();
        if (!hasText(revision.fields().get("cover_image"))) {
            blockers.add(new ShowroomAdminController.MaterialBlockerRespVO(
                    "PRODUCT_COVER_MISSING",
                    "product cover_image is missing",
                    TARGET_PRODUCT,
                    revision.productId(),
                    null,
                    List.of("cover_image"),
                    null,
                    "product-" + revision.productId() + "-preview",
                    null,
                    "PRODUCT_COVER_MISSING"));
        }
        appendProductNarrationMaterialBlocker(blockers, revision, ShowroomNarrationLanguage.ZH);
        appendProductNarrationMaterialBlocker(blockers, revision, ShowroomNarrationLanguage.EN);
        return List.copyOf(blockers);
    }

    private void appendProductNarrationMaterialBlocker(List<ShowroomAdminController.MaterialBlockerRespVO> blockers,
                                                       ShowroomProductRevision revision,
                                                       ShowroomNarrationLanguage language) {
        Optional<ShowroomNarrationVersion> liveNarration = narrationService.live(new ShowroomNarrationKey(
                ShowroomNarrationTargetType.PRODUCT, revision.productId(),
                ShowroomNarrationAudienceType.PUBLIC, language));
        if (liveNarration.isPresent()
                && revision.revisionId().equals(liveNarration.get().sourceRevisionId())
                && liveNarration.get().audioFileId() != null) {
            return;
        }
        blockers.add(new ShowroomAdminController.MaterialBlockerRespVO(
                "PRODUCT_NARRATION_AUDIO_MISSING",
                "product " + language.name() + " narration audio is missing",
                TARGET_PRODUCT,
                revision.productId(),
                language.name(),
                List.of("audioFileId"),
                liveNarration.map(ShowroomNarrationVersion::audioFileId).orElse(null),
                "product-" + revision.productId() + "-audio-" + language.name().toLowerCase(),
                null,
                "PRODUCT_NARRATION_AUDIO_MISSING"));
    }

    private ShowroomProductExcelVO toProductExcelRow(ShowroomAdminController.ProductPageRespVO row, String hallName) {
        Map<String, String> fields = row.displayRevision().fields();
        return ShowroomProductExcelVO.builder()
                .productCode(row.productCode())
                .legacyProductCode(nullToEmpty(row.legacyProductCode()))
                .nameCn(row.displayRevision().nameCn())
                .nameEn(row.displayRevision().nameEn())
                .hallName(hallName)
                .ownerCompanyName(resolveOwnerCompanyExcelTextForExport(fields))
                .lifecycleStage(resolveLifecycleStageExcelText(fields.get("lifecycle_stage")))
                .pipelineLayout(nullToEmpty(fields.get("pipeline_layout")))
                .coreSellingPoints(nullToEmpty(fields.get("target_market")))
                .indicationContent(nullToEmpty(fields.get("indication_content")))
                .modelSpecification(nullToEmpty(fields.get("model_specification")))
                .registrationCertificate(nullToEmpty(fields.get("registration_certificate")))
                .sellingPointsCopy(nullToEmpty(fields.get("core_selling_points")))
                .productImage("")
                .coverImage(nullToEmpty(fields.get("cover_image")))
                .awards("")
                .rawMaterialSheet("")
                .build();
    }

    private ShowroomAdminController.ProductDraftReqVO buildImportDraft(ShowroomProductExcelVO row,
                                                                       ShowroomProductImportExtra rowExtra,
                                                                       ShowroomAdminController.ProductDetailRespVO currentDetail,
                                                                       int rowNo,
                                                                       List<OwnerCompanyExcelContract> ownerCompanyContracts,
                                                                       Map<String, Long> productMasterIdsByCode) {
        String lifecycleStage = resolveImportLifecycleStage(row, currentDetail);
        ImportedOwnerCompany importedOwnerCompany = resolveImportedOwnerCompany(row.getOwnerCompanyName(),
                currentDetail, rowNo, ownerCompanyContracts, lifecycleStage);
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("owner_company_id", importedOwnerCompany.ownerCompanyId());
        fields.put("product_owner_type", importedOwnerCompany.productOwnerType());
        fields.put("lifecycle_stage", lifecycleStage);
        fields.put("target_market", resolveImportCell(row.getCoreSellingPoints(), currentDetail.fields().get("target_market")));
        fields.put("pipeline_layout", resolveImportCell(row.getPipelineLayout(), currentDetail.fields().get("pipeline_layout")));
        fields.put("indication_content", resolveImportCell(row.getIndicationContent(), currentDetail.fields().get("indication_content")));
        fields.put("core_selling_points", resolveImportSellingPoints(row, rowExtra, currentDetail));
        fields.put("model_specification", resolveImportCell(row.getModelSpecification(), currentDetail.fields().get("model_specification")));
        fields.put("registration_certificate", resolveImportCell(row.getRegistrationCertificate(), currentDetail.fields().get("registration_certificate")));
        fields.put("clinical_effect", normalizeExcelCell(currentDetail.fields().get("clinical_effect")));
        fields.put("fim_status", normalizeExcelCell(currentDetail.fields().get("fim_status")));
        for (String fieldKey : PRODUCT_TRANSLATABLE_FIELD_KEYS) {
            String englishFieldKey = productEnglishFieldKey(fieldKey);
            if (currentDetail.fields().containsKey(englishFieldKey)) {
                fields.put(englishFieldKey, normalizeExcelCell(currentDetail.fields().get(englishFieldKey)));
            }
        }
        fields.put("cover_image", resolveImportCoverImage(row, rowExtra, currentDetail));
        Long importedProductMasterId = requireImportedProductMasterId(currentDetail.productCode(), row.getProductCode(),
                productMasterIdsByCode, rowNo);
        return new ShowroomAdminController.ProductDraftReqVO(currentDetail.productId(), importedProductMasterId,
                currentDetail.productCode(),
                resolveImportNameCn(row, rowExtra, rowNo, currentDetail), resolveImportCell(row.getNameEn(),
                currentDetail.nameEn()), resolveImportLegacyProductCode(row, currentDetail), fields);
    }

    private String resolveImportLegacyProductCode(ShowroomProductExcelVO row,
                                                  ShowroomAdminController.ProductDetailRespVO currentDetail) {
        String importedLegacyProductCode = normalizeExcelCell(row.getLegacyProductCode());
        if (hasText(importedLegacyProductCode)) {
            return importedLegacyProductCode;
        }
        return currentDetail.legacyProductCode();
    }

    private ShowroomAdminController.ProductDraftReqVO buildImportDraftForMissingProduct(ShowroomProductExcelVO row,
                                                                                          ShowroomProductImportExtra rowExtra,
                                                                                          int rowNo,
                                                                                         List<OwnerCompanyExcelContract> ownerCompanyContracts,
                                                                                         Map<String, Long> productMasterIdsByCode) {
        String lifecycleStage = resolveImportLifecycleStage(row, null);
        ImportedOwnerCompany importedOwnerCompany = resolveImportedOwnerCompany(row.getOwnerCompanyName(), null,
                rowNo, ownerCompanyContracts, lifecycleStage);
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put("owner_company_id", importedOwnerCompany.ownerCompanyId());
        fields.put("product_owner_type", importedOwnerCompany.productOwnerType());
        fields.put("lifecycle_stage", lifecycleStage);
        fields.put("target_market", resolveImportCell(row.getCoreSellingPoints(), null));
        fields.put("pipeline_layout", resolveImportCell(row.getPipelineLayout(), null));
        fields.put("indication_content", resolveImportCell(row.getIndicationContent(), null));
        fields.put("core_selling_points", resolveImportSellingPoints(row, rowExtra, null));
        fields.put("model_specification", resolveImportCell(row.getModelSpecification(), null));
        fields.put("registration_certificate", resolveImportCell(row.getRegistrationCertificate(), null));
        fields.put("clinical_effect", "");
        fields.put("fim_status", "");
        fields.put("cover_image", resolveImportCoverImage(row, rowExtra, null));
        String productCode = requireExcelText(row.getProductCode(), "展品编码", rowNo);
        Long productMasterId = requireImportedProductMasterId(productCode, null, productMasterIdsByCode, rowNo);
        return new ShowroomAdminController.ProductDraftReqVO(
                null,
                productMasterId,
                productCode,
                resolveImportNameCn(row, rowExtra, rowNo, null),
                requireExcelText(row.getNameEn(), "产品名-英文", rowNo),
                normalizeExcelCell(row.getLegacyProductCode()),
                fields
        );
    }

    private Long requireImportedProductMasterId(String productCode, String importedProductCode,
                                                Map<String, Long> productMasterIdsByCode, int rowNo) {
        String normalizedProductCode = normalizeExcelCell(productCode);
        Long productMasterId = productMasterIdsByCode.get(normalizedProductCode);
        if (productMasterId == null) {
            String normalizedImportedProductCode = normalizeExcelCell(importedProductCode);
            if (hasText(normalizedImportedProductCode)) {
                productMasterId = productMasterIdsByCode.get(normalizedImportedProductCode);
            }
        }
        if (productMasterId == null) {
            throw new IllegalStateException("SHOWROOM_PRODUCT_MASTER_DATA_MISSING: 第 " + rowNo
                    + " 行展品编码未在产品主数据中导入：" + normalizedProductCode);
        }
        return productMasterId;
    }

    private String resolveImportNameCn(ShowroomProductExcelVO row, ShowroomProductImportExtra rowExtra, int rowNo,
                                       ShowroomAdminController.ProductDetailRespVO currentDetail) {
        String nameCn = normalizeExcelCell(row.getNameCn());
        validateLegacyProductNameConflict(nameCn, rowExtra, rowNo);
        if (!hasText(nameCn)) {
            if (currentDetail == null) {
                throw new IllegalStateException("第 " + rowNo + " 行产品名-中文不能为空");
            }
            return normalizeExcelCell(currentDetail.nameCn());
        }
        return nameCn;
    }

    private void validateLegacyProductNameConflict(String nameCn, ShowroomProductImportExtra rowExtra, int rowNo) {
        String legacyProductName = normalizeExcelCell(rowExtra == null ? null : rowExtra.productName());
        if (hasText(legacyProductName) && hasText(nameCn) && !legacyProductName.equals(nameCn)) {
            throw new IllegalStateException("第 " + rowNo + " 行产品列与产品名-中文不一致，产品="
                    + legacyProductName + "，产品名-中文=" + nameCn);
        }
    }

    private String resolveImportCoverImage(ShowroomProductExcelVO row, ShowroomProductImportExtra rowExtra,
                                           ShowroomAdminController.ProductDetailRespVO currentDetail) {
        String currentCoverImage = currentDetail == null ? "" : normalizeExcelCell(currentDetail.fields().get("cover_image"));
        if (rowExtra == null || rowExtra.coverImage() == null) {
            return currentCoverImage;
        }
        ShowroomProductImportExtra.ImportedCoverImage coverImage = rowExtra.coverImage();
        boolean currentCoverMatchesImportedCover;
        try {
            currentCoverMatchesImportedCover = productCoverImageService.importedCoverImageMatchesCurrentCover(
                    currentCoverImage, coverImage.content());
        } catch (IllegalStateException exception) {
            if (!isCurrentCoverReadFailure(exception)) {
                throw exception;
            }
            log.warn("Current product cover image is unreadable during Excel import; replacing it with imported cover. "
                    + "productCode={}, currentCoverImage={}, reason={}", normalizeExcelCell(row.getProductCode()),
                    currentCoverImage, exception.getMessage());
            return uploadImportedCoverImage(row, coverImage);
        }
        if (currentCoverMatchesImportedCover) {
            if (isLegacyImportedCoverImageUrl(currentCoverImage)
                    && !productCoverImageService.importedCoverImageUrlMatchesContentHash(
                    currentCoverImage, coverImage.content())) {
                return uploadImportedCoverImage(row, coverImage);
            }
            return currentCoverImage;
        }
        return uploadImportedCoverImage(row, coverImage);
    }

    private String uploadImportedCoverImage(ShowroomProductExcelVO row,
                                            ShowroomProductImportExtra.ImportedCoverImage coverImage) {
        return productCoverImageService.uploadImportedCoverImage(normalizeExcelCell(row.getProductCode()),
                coverImage.content(), coverImage.fileExtension(), coverImage.mimeType());
    }

    private boolean isCurrentCoverReadFailure(IllegalStateException exception) {
        String message = exception.getMessage();
        return hasText(message)
                && (message.contains("current product cover image is empty")
                || message.contains("failed to read current product cover image"));
    }

    private boolean isLegacyImportedCoverImageUrl(String coverImageUrl) {
        if (!hasText(coverImageUrl)) {
            return false;
        }
        String normalized = coverImageUrl.trim();
        int fragmentIndex = normalized.indexOf('#');
        if (fragmentIndex >= 0) {
            normalized = normalized.substring(0, fragmentIndex);
        }
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        int separatorIndex = normalized.lastIndexOf('/');
        String fileName = separatorIndex >= 0 ? normalized.substring(separatorIndex + 1) : normalized;
        return LEGACY_IMPORTED_COVER_FILE_NAME_PATTERN.matcher(fileName).matches();
    }

    private String resolveImportSellingPoints(ShowroomProductExcelVO row, ShowroomProductImportExtra rowExtra,
                                              ShowroomAdminController.ProductDetailRespVO currentDetail) {
        String sellingPointsCopy = normalizeExcelCell(rowExtra == null ? null : rowExtra.sellingPointsCopy());
        if (!hasText(sellingPointsCopy) && hasText(row.getSellingPointsCopy())) {
            sellingPointsCopy = normalizeExcelCell(row.getSellingPointsCopy());
        } else if (!hasText(sellingPointsCopy)) {
            sellingPointsCopy = currentDetail == null
                    ? ""
                    : normalizeExcelCell(currentDetail.fields().get("core_selling_points"));
        }
        return sellingPointsCopy;
    }

    private String resolveImportLifecycleStage(ShowroomProductExcelVO row,
                                               ShowroomAdminController.ProductDetailRespVO currentDetail) {
        if (!hasText(row.getLifecycleStage())) {
            if (currentDetail == null) {
                throw new IllegalStateException("产品缺少在售/在研，无法导入创建");
            }
            return normalizeExcelCell(currentDetail.fields().get("lifecycle_stage"));
        }
        return parseLifecycleStageExcelText(row.getLifecycleStage());
    }

    private String resolveImportCell(String importedValue, String currentValue) {
        String normalizedImportedValue = normalizeExcelCell(importedValue);
        if (hasText(normalizedImportedValue)) {
            return normalizedImportedValue;
        }
        return normalizeExcelCell(currentValue);
    }

    private boolean hasImportChanges(ShowroomAdminController.ProductDetailRespVO currentDetail,
                                     ShowroomAdminController.ProductDraftReqVO importDraft) {
        if (!nullToEmpty(currentDetail.nameCn()).equals(importDraft.nameCn())) {
            return true;
        }
        if (!nullToEmpty(currentDetail.nameEn()).equals(importDraft.nameEn())) {
            return true;
        }
        for (String fieldKey : PRODUCT_IMPORT_FIELD_KEYS) {
            if (!nullToEmpty(currentDetail.fields().get(fieldKey))
                    .equals(nullToEmpty(importDraft.fields().get(fieldKey)))) {
                return true;
            }
        }
        return false;
    }

    private void validateImportRow(ShowroomProductExcelVO row, ShowroomProductImportExtra rowExtra, int rowNo) {
        if (row == null) {
            throw new IllegalStateException("第 " + rowNo + " 行为空，无法导入");
        }
        requireExcelText(row.getProductCode(), "展品编码", rowNo);
        validateLegacyProductNameConflict(normalizeExcelCell(row.getNameCn()), rowExtra, rowNo);
    }

    private String requireExcelText(String value, String fieldLabel, int rowNo) {
        if (!hasText(value)) {
            throw new IllegalStateException("第 " + rowNo + " 行" + fieldLabel + "不能为空");
        }
        return value.trim();
    }

    private ImportedOwnerCompany resolveImportedOwnerCompany(String excelOwnerCompanyName,
                                                             ShowroomAdminController.ProductDetailRespVO currentDetail,
                                                             int rowNo,
                                                             List<OwnerCompanyExcelContract> ownerCompanyContracts,
                                                             String lifecycleStage) {
        if (hasText(excelOwnerCompanyName)) {
            OwnerCompanyExcelContract contract = resolveOwnerCompanyExcelContractByName(excelOwnerCompanyName,
                    rowNo, ownerCompanyContracts);
            return new ImportedOwnerCompany(String.valueOf(contract.companyId()),
                    resolveProductOwnerTypeByCompanyType(contract.companyType()));
        }
        if (currentDetail == null) {
            if (LIFECYCLE_R_AND_D_CODE.equalsIgnoreCase(normalizeExcelCell(lifecycleStage))) {
                return new ImportedOwnerCompany("", "");
            }
            throw new IllegalStateException("第 " + rowNo + " 行持证公司不能为空");
        }
        String ownerCompanyId = normalizeExcelCell(currentDetail.fields().get("owner_company_id"));
        if (!hasText(ownerCompanyId)) {
            if (LIFECYCLE_R_AND_D_CODE.equalsIgnoreCase(normalizeExcelCell(lifecycleStage))) {
                return new ImportedOwnerCompany("", "");
            }
            throw new IllegalStateException("当前产品缺少所属公司，无法导入发布");
        }
        return new ImportedOwnerCompany(ownerCompanyId,
                normalizeExcelCell(currentDetail.fields().get("product_owner_type")));
    }

    private OwnerCompanyExcelContract resolveOwnerCompanyExcelContractByName(String excelOwnerCompanyName, int rowNo,
                                                                             List<OwnerCompanyExcelContract> contracts) {
        String normalizedExcelName = normalizeCompanyCompareText(excelOwnerCompanyName);
        List<OwnerCompanyExcelContract> matches = contracts.stream()
                .filter(contract -> contract.aliases().stream()
                        .map(this::normalizeCompanyCompareText)
                        .anyMatch(alias -> alias.equals(normalizedExcelName)))
                .toList();
        if (matches.isEmpty()) {
            throw new IllegalStateException("第 " + rowNo + " 行持证公司不存在，无法导入发布："
                    + normalizeExcelCell(excelOwnerCompanyName) + ownerCompanyOptionsText(contracts));
        }
        if (matches.size() > 1) {
            throw new IllegalStateException("第 " + rowNo + " 行持证公司匹配多个公司，无法导入发布："
                    + normalizeExcelCell(excelOwnerCompanyName) + "，匹配公司="
                    + String.join("、", matches.stream().map(OwnerCompanyExcelContract::displayName).toList()));
        }
        return matches.get(0);
    }

    private String resolveOwnerCompanyExcelText(String ownerCompanyId) {
        String normalizedOwnerCompanyId = normalizeExcelCell(ownerCompanyId);
        if (!hasText(normalizedOwnerCompanyId)) {
            return "";
        }
        return resolveOwnerCompanyExcelContract(normalizedOwnerCompanyId).displayName();
    }

    private String resolveOwnerCompanyExcelTextForExport(Map<String, String> fields) {
        String ownerCompanyId = normalizeExcelCell(fields.get("owner_company_id"));
        if (!hasText(ownerCompanyId)) {
            return "";
        }
        try {
            ShowroomCompanySnapshot company = contentService.getCompany(Long.valueOf(ownerCompanyId));
            return toOwnerCompanyExcelContract(company).displayName();
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("当前产品所属公司不是有效数字，无法导出产品资料：" + ownerCompanyId, exception);
        } catch (IllegalStateException exception) {
            if (!isCompanyNotFound(exception)) {
                throw exception;
            }
            if (isYingtaiOwnerType(fields.get("product_owner_type"))) {
                return resolveUniqueYingtaiOwnerCompanyForExport().displayName();
            }
            throw new IllegalStateException("当前产品所属公司不存在，无法导出产品资料：" + ownerCompanyId, exception);
        }
    }

    private OwnerCompanyExcelContract resolveOwnerCompanyExcelContract(String ownerCompanyId) {
        String normalizedOwnerCompanyId = normalizeExcelCell(ownerCompanyId);
        if (!hasText(normalizedOwnerCompanyId)) {
            throw new IllegalStateException("当前产品缺少所属公司，无法导入发布");
        }
        try {
            ShowroomCompanySnapshot company = contentService.getCompany(Long.valueOf(normalizedOwnerCompanyId));
            return toOwnerCompanyExcelContract(company);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("当前产品所属公司不是有效数字，无法导入发布：" + ownerCompanyId, exception);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("当前产品所属公司不存在，无法导入发布：" + ownerCompanyId, exception);
        }
    }

    private List<OwnerCompanyExcelContract> loadOwnerCompanyExcelContracts() {
        return contentService.listCompanies().stream()
                .map(this::toOwnerCompanyExcelContract)
                .toList();
    }

    private OwnerCompanyExcelContract resolveUniqueYingtaiOwnerCompanyForExport() {
        List<ShowroomCompanySnapshot> matches = contentService.listCompanies().stream()
                .filter(this::isYingtaiCompany)
                .toList();
        if (matches.isEmpty()) {
            throw new IllegalStateException("当前租户瑛泰公司不存在，无法导出产品资料");
        }
        if (matches.size() > 1) {
            throw new IllegalStateException("当前租户瑛泰公司不唯一，无法导出产品资料："
                    + String.join("、", matches.stream().map(ShowroomCompanySnapshot::displayName).toList()));
        }
        return toOwnerCompanyExcelContract(matches.get(0));
    }

    private OwnerCompanyExcelContract toOwnerCompanyExcelContract(ShowroomCompanySnapshot company) {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        addCompanyAlias(aliases, company.displayName());
        addCompanyAlias(aliases, company.displayNameEn());
        if (isYingtaiCompany(company)) {
            addYingtaiCompanyAliases(aliases);
        }
        return new OwnerCompanyExcelContract(company.companyId(), company.companyType(), company.displayName(), aliases);
    }

    private boolean isYingtaiCompany(ShowroomCompanySnapshot company) {
        String text = normalizeCompanyCompareText(company.displayName()) + normalizeCompanyCompareText(company.displayNameEn());
        return "MAIN".equalsIgnoreCase(nullToEmpty(company.companyType()))
                && (text.contains("瑛泰") || text.contains("盈泰") || text.toLowerCase().contains("yingtai"));
    }

    private boolean isYingtaiOwnerType(String productOwnerType) {
        return OWNER_TYPE_YINGTAI_CODE.equalsIgnoreCase(normalizeExcelCell(productOwnerType));
    }

    private boolean isCompanyNotFound(IllegalStateException exception) {
        return nullToEmpty(exception.getMessage()).contains("SHOWROOM_TARGET_NOT_FOUND: company not found");
    }

    private String resolveProductOwnerTypeByCompanyType(String companyType) {
        if ("MAIN".equalsIgnoreCase(nullToEmpty(companyType))) {
            return OWNER_TYPE_YINGTAI_CODE;
        }
        return OWNER_TYPE_SUBSIDIARY_CODE;
    }

    private void addYingtaiCompanyAliases(LinkedHashSet<String> aliases) {
        addCompanyAlias(aliases, OWNER_COMPANY_LABEL);
        addCompanyAlias(aliases, "瑛泰");
        addCompanyAlias(aliases, "盈泰医疗");
        addCompanyAlias(aliases, "盈泰");
        addCompanyAlias(aliases, "Yingtai Medical");
    }

    private void addCompanyAlias(LinkedHashSet<String> aliases, String value) {
        if (hasText(value)) {
            aliases.add(value.trim());
        }
    }

    private String normalizeCompanyCompareText(String value) {
        return normalizeExcelCell(value).replaceAll("\\s+", "");
    }

    private String ownerCompanyOptionsText(List<OwnerCompanyExcelContract> contracts) {
        List<String> options = contracts.stream()
                .map(OwnerCompanyExcelContract::displayName)
                .filter(ShowroomApiRuntime::hasText)
                .distinct()
                .limit(10)
                .toList();
        if (options.isEmpty()) {
            return "";
        }
        return "；可用持证公司：" + String.join("、", options);
    }

    private String parseProductOwnerTypeExcelText(String value) {
        String normalized = normalizeExcelCell(value);
        if (!hasText(normalized)) {
            return normalized;
        }
        if (OWNER_TYPE_YINGTAI_CODE.equalsIgnoreCase(normalized)
                || OWNER_TYPE_YINGTAI_TEXT.equals(normalized)
                || "瑛泰产品".equals(normalized)
                || OWNER_COMPANY_LABEL.equals(normalized)) {
            return OWNER_TYPE_YINGTAI_CODE;
        }
        if (OWNER_TYPE_SUBSIDIARY_CODE.equalsIgnoreCase(normalized)
                || OWNER_TYPE_SUBSIDIARY_TEXT.equals(normalized)) {
            return OWNER_TYPE_SUBSIDIARY_CODE;
        }
        throw new IllegalStateException("持证人不支持：" + value);
    }

    private String parseLifecycleStageExcelText(String value) {
        String normalized = normalizeExcelCell(value);
        if (!hasText(normalized)) {
            return normalized;
        }
        if (LIFECYCLE_REGISTERED_CODE.equalsIgnoreCase(normalized) || LIFECYCLE_REGISTERED_TEXT.equals(normalized)) {
            return LIFECYCLE_REGISTERED_CODE;
        }
        if (LIFECYCLE_R_AND_D_CODE.equalsIgnoreCase(normalized) || LIFECYCLE_R_AND_D_TEXT.equals(normalized)) {
            return LIFECYCLE_R_AND_D_CODE;
        }
        throw new IllegalStateException("生命周期不支持：" + value);
    }

    private String resolveProductOwnerTypeExcelText(String value) {
        if (OWNER_TYPE_SUBSIDIARY_CODE.equalsIgnoreCase(nullToEmpty(value))) {
            return OWNER_TYPE_SUBSIDIARY_TEXT;
        }
        if (OWNER_TYPE_YINGTAI_CODE.equalsIgnoreCase(nullToEmpty(value))) {
            return OWNER_TYPE_YINGTAI_TEXT;
        }
        return nullToEmpty(value);
    }

    private String resolveLifecycleStageExcelText(String value) {
        if (LIFECYCLE_R_AND_D_CODE.equalsIgnoreCase(nullToEmpty(value))) {
            return LIFECYCLE_R_AND_D_TEXT;
        }
        if (LIFECYCLE_REGISTERED_CODE.equalsIgnoreCase(nullToEmpty(value))) {
            return LIFECYCLE_REGISTERED_TEXT;
        }
        return nullToEmpty(value);
    }

    private String normalizeExcelCell(String value) {
        return value == null ? "" : value.replace("\r\n", "\n").replace("\r", "\n").trim();
    }

    private record ImportedOwnerCompany(String ownerCompanyId, String productOwnerType) {
    }

    private record OwnerCompanyExcelContract(Long companyId, String companyType, String displayName,
                                             LinkedHashSet<String> aliases) {
    }

    private record ProductExcelSellingPointsSplit(String salesCountry, String sellingPointsCopy) {
    }

    private ShowroomAdminController.HallPageRespVO toHallPageRow(ShowroomHall hall) {
        List<ShowroomAdminController.HallProductMappingReqVO> productMappings = hall.productMappings().stream()
                .map(mapping -> new ShowroomAdminController.HallProductMappingReqVO(mapping.productId(),
                        mapping.displayOrder(), mapping.layoutX(), mapping.layoutY(),
                        mapping.layoutWidth(), mapping.layoutHeight()))
                .toList();
        List<ShowroomAdminController.HallItemMappingItemReqVO> itemMappings = hall.itemMappings().stream()
                .map(mapping -> new ShowroomAdminController.HallItemMappingItemReqVO(mapping.itemType(),
                        mapping.itemId(), mapping.displayOrder(), mapping.layoutX(), mapping.layoutY(),
                        mapping.layoutWidth(), mapping.layoutHeight()))
                .toList();
        return new ShowroomAdminController.HallPageRespVO(hall.hallId(), hall.hallCode(), hall.name(),
                hall.nameEn(), nullToEmpty(hall.description()), nullToEmpty(hall.descriptionEn()),
                nullToEmpty(hall.canvasBackgroundImageUrl()),
                productMappings, itemMappings.size(), itemMappings,
                latestNarrationSummary(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                        ShowroomNarrationLanguage.ZH),
                latestNarrationSummary(ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                        ShowroomNarrationLanguage.EN));
    }

    private ShowroomAdminController.VersionHistoryRespVO toVersionHistory(String targetType, Long revisionId,
                                                                          List<ShowroomVersionAudit> audits) {
        Integer revisionNo;
        String status;
        if (TARGET_COMPANY.equals(targetType)) {
            ShowroomCompanyRevision revision = contentService.getCompanyRevision(revisionId);
            revisionNo = revision.revisionNo();
            status = normalizeApprovalStatus(revision.status());
        } else if (TARGET_PRODUCT.equals(targetType)) {
            ShowroomProductRevision revision = contentService.getProductRevision(revisionId);
            revisionNo = revision.revisionNo();
            status = normalizeApprovalStatus(revision.status());
        } else {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: unsupported history target type " + targetType);
        }
        List<ShowroomAdminController.VersionDiffItemRespVO> diffItems = audits.stream()
                .map(audit -> new ShowroomAdminController.VersionDiffItemRespVO(audit.fieldCode(),
                        fieldLabel(targetType, audit.fieldCode()), audit.oldValueJson(), audit.newValueJson(),
                        audit.operatorId(), audit.operatorAction(), audit.createdAt()))
                .toList();
        return new ShowroomAdminController.VersionHistoryRespVO(revisionId, revisionNo, status, diffItems);
    }

    private ShowroomAdminController.DiscussionSummaryRespVO discussionSummary(List<ShowroomProductComment> comments) {
        int total = comments.size();
        int open = (int) comments.stream().filter(comment -> STATUS_OPEN.equals(comment.status())).count();
        int resolved = (int) comments.stream().filter(comment -> STATUS_RESOLVED.equals(comment.status())).count();
        return new ShowroomAdminController.DiscussionSummaryRespVO(total, open, resolved);
    }

    private ShowroomAdminController.LatestNarrationRespVO latestNarrationSummary(Long productId, Long sourceRevisionId) {
        return latestNarrationSummary(ShowroomNarrationTargetType.PRODUCT, productId, sourceRevisionId,
                ShowroomNarrationLanguage.ZH);
    }

    private ShowroomAdminController.LatestNarrationRespVO latestNarrationSummary(
            ShowroomNarrationTargetType targetType, Long targetId, Long sourceRevisionId,
            ShowroomNarrationLanguage language) {
        ShowroomNarrationKey key = new ShowroomNarrationKey(targetType, targetId,
                ShowroomNarrationAudienceType.PUBLIC, language);
        return narrationService.latest(key, sourceRevisionId)
                .map(version -> {
                    boolean audioReady = version.audioFileId() != null;
                    return new ShowroomAdminController.LatestNarrationRespVO(version.id(),
                            version.key().language().name(), version.key().audienceType().name(),
                            version.status().name(), version.live(), audioReady,
                            audioReady ? fileUrl(version.audioFileId()) : "",
                            audioReady ? nullToEmpty(version.voice()) : "");
                })
                .orElse(null);
    }

    private List<ShowroomAdminController.ProductPageRespVO> listProductsForBatch(
            ShowroomAdminController.ProductBatchGenerateReqVO req) {
        ShowroomAdminController.PageQueryReqVO pageQuery = new ShowroomAdminController.PageQueryReqVO(
                req.keyword(), null, null, null, null, req.lifecycleStage(), req.incompleteStatus(),
                req.approvalStatus(), null);
        return listProductsForBatch(pageQuery);
    }

    private List<ShowroomAdminController.ProductPageRespVO> listProductsForBatch(
            ProductBatchNarrationAutoCheckCriteria criteria) {
        return listProductsForBatch(new ShowroomAdminController.PageQueryReqVO(
                criteria.keyword(), null, null, null, null, criteria.lifecycleStage(),
                criteria.incompleteStatus(), criteria.approvalStatus(), null));
    }

    private List<ShowroomAdminController.ProductPageRespVO> listProductsForBatch(
            ShowroomAdminController.PageQueryReqVO pageQuery) {
        return contentService.listProducts().stream()
                .map(snapshot -> {
                    ShowroomProductRevision latestRevision = contentService.getLatestProductRevision(snapshot.productId());
                    ShowroomProductRevision displayRevision = resolveDisplayProductRevision(snapshot, latestRevision);
                    ShowroomAdminController.ProductDetailRespVO detail = buildProductDetail(snapshot, latestRevision, true);
                    ShowroomAdminController.ProductDetailRespVO displayDetail = buildProductDetail(snapshot,
                            displayRevision, latestRevision.revisionId().equals(displayRevision.revisionId()));
                    return new ShowroomAdminController.ProductPageRespVO(snapshot.productId(), snapshot.productMasterId(),
                            snapshot.productCode(), snapshot.legacyProductCode(),
                            displayDetail.currentRevisionId(), displayDetail.incomplete(), displayDetail.live(),
                            detail, displayDetail, null, true);
                })
                .filter(row -> matchesProduct(row, pageQuery))
                .toList();
    }

    private ShowroomAdminController.ProductBatchGenerateRespVO executeManualProductBatchNarrationAudio(
            ProductBatchNarrationAutoCheckCriteria criteria) {
        if (!productBatchNarrationAutoCheckLock.tryLock()) {
            throw new IllegalStateException(PRODUCT_BATCH_AUDIO_AUTO_CHECK_RUNNING_MESSAGE);
        }
        try {
            ProductBatchNarrationAutoCheckState initialState = new ProductBatchNarrationAutoCheckState(
                    true,
                    normalizeProductBatchNarrationAutoCheckCriteria(criteria),
                    emptyProductBatchNarrationAutoCheckSummary(),
                    emptyProductBatchNarrationAutoCheckFailure());
            return executeProductBatchNarrationAudioLocked(initialState);
        } finally {
            productBatchNarrationAutoCheckLock.unlock();
        }
    }

    private ShowroomAdminController.ProductBatchGenerateRespVO executeProductBatchNarrationAudioLocked(
            ProductBatchNarrationAutoCheckState initialState) {
        ProductBatchNarrationAutoCheckState preparedState = new ProductBatchNarrationAutoCheckState(
                true,
                normalizeProductBatchNarrationAutoCheckCriteria(initialState.criteria()),
                emptyProductBatchNarrationAutoCheckSummary(),
                emptyProductBatchNarrationAutoCheckFailure());
        saveProductBatchNarrationAutoCheckState(preparedState);
        try {
            ProductBatchNarrationExecutionSummary summary = runProductBatchNarrationAudioBatch(preparedState.criteria());
            ProductBatchNarrationAutoCheckState nextState = buildProductBatchNarrationAutoCheckState(
                    preparedState.criteria(), summary);
            saveProductBatchNarrationAutoCheckState(nextState);
            return toProductBatchGenerateResp(summary, nextState.enabled());
        } catch (RuntimeException exception) {
            ProductBatchNarrationAutoCheckState failedState = new ProductBatchNarrationAutoCheckState(
                    true,
                    preparedState.criteria(),
                    preparedState.summary(),
                    new ProductBatchNarrationAutoCheckFailureData(
                            truncate(batchFailureReason(exception), 160),
                            Instant.now().toEpochMilli()));
            saveProductBatchNarrationAutoCheckState(failedState);
            throw exception;
        }
    }

    private ProductBatchNarrationExecutionSummary runProductBatchNarrationAudioBatch(
            ProductBatchNarrationAutoCheckCriteria criteria) {
        List<ShowroomAdminController.ProductPageRespVO> matchedRows = listProductsForBatch(criteria);
        List<ShowroomAdminController.ProductBatchGenerateFailureRespVO> failures = new ArrayList<>();
        int publishedCount = 0;
        int skippedUnpublishedCount = 0;
        int skippedExistingCount = 0;
        int skippedMissingScriptCount = 0;
        int succeededCount = 0;
        for (ShowroomAdminController.ProductPageRespVO row : matchedRows) {
            if (!STATUS_PUBLISHED.equalsIgnoreCase(row.revision().status())) {
                skippedUnpublishedCount++;
                continue;
            }
            publishedCount++;
            try {
                ProductNarrationBatchEvaluation evaluation = evaluateCurrentProductNarrationBatch(row.productId());
                if (evaluation.existingReady()) {
                    skippedExistingCount++;
                    continue;
                }
                if (evaluation.missingScript()) {
                    skippedMissingScriptCount++;
                    continue;
                }
                generateAndPublishMissingProductNarrationAudio(evaluation);
                succeededCount++;
            } catch (RuntimeException exception) {
                failures.add(toBatchFailure(row, exception));
            }
        }
        int remainingActionableCount = countRemainingActionableProducts(criteria);
        return new ProductBatchNarrationExecutionSummary(
                matchedRows.size(),
                publishedCount,
                skippedUnpublishedCount,
                skippedExistingCount,
                skippedMissingScriptCount,
                succeededCount,
                failures.size(),
                remainingActionableCount,
                List.copyOf(failures));
    }

    private ProductNarrationBatchEvaluation evaluateCurrentProductNarrationBatch(Long productId) {
        ShowroomProductRevision currentRevision = contentService.requireCurrentProductRevision(productId);
        Long sourceRevisionId = currentRevision.revisionId();
        ShowroomNarrationVersion zhLive = findLiveNarrationForRevision(productId, sourceRevisionId,
                ShowroomNarrationLanguage.ZH).orElse(null);
        ShowroomNarrationVersion enLive = findLiveNarrationForRevision(productId, sourceRevisionId,
                ShowroomNarrationLanguage.EN).orElse(null);
        boolean zhReady = zhLive != null && zhLive.audioFileId() != null;
        boolean enReady = enLive != null && enLive.audioFileId() != null;
        ShowroomNarrationVersion zhLatest = findLatestNarrationForRevision(productId, sourceRevisionId,
                ShowroomNarrationLanguage.ZH).orElse(null);
        ShowroomNarrationVersion enLatest = findLatestNarrationForRevision(productId, sourceRevisionId,
                ShowroomNarrationLanguage.EN).orElse(null);
        boolean missingScript = zhLatest == null || !hasText(zhLatest.scriptText())
                || enLatest == null || !hasText(enLatest.scriptText());
        return new ProductNarrationBatchEvaluation(productId, sourceRevisionId, zhReady && enReady,
                missingScript, zhReady, enReady, zhLatest, enLatest);
    }

    private void generateAndPublishMissingProductNarrationAudio(ProductNarrationBatchEvaluation evaluation) {
        if (!evaluation.zhReady()) {
            generateAndPublishPreparedNarration(evaluation.zhLatest());
        }
        if (!evaluation.enReady()) {
            generateAndPublishPreparedNarration(evaluation.enLatest());
        }
    }

    private void generateAndPublishPreparedNarration(ShowroomNarrationVersion narration) {
        if (narration == null) {
            throw new IllegalStateException("SHOWROOM_SCRIPT_MISSING: narration draft is required");
        }
        ShowroomNarrationVersion current = narration;
        if (current.audioFileId() == null) {
            current = narrationService.generateAudio(current.id());
        }
        ShowroomNarrationVersion published = narrationService.publishDirectly(current.id());
        markReleaseDirtyForNarrationIfCurrent(published, null, "PRODUCT_PUBLIC_NARRATION_PUBLISHED");
    }

    private Optional<ShowroomNarrationVersion> findLatestNarrationForRevision(Long productId, Long sourceRevisionId,
                                                                              ShowroomNarrationLanguage language) {
        ShowroomNarrationKey key = new ShowroomNarrationKey(ShowroomNarrationTargetType.PRODUCT, productId,
                ShowroomNarrationAudienceType.PUBLIC, language);
        return narrationService.latest(key, sourceRevisionId);
    }

    private Optional<ShowroomNarrationVersion> findLiveNarrationForRevision(Long productId, Long sourceRevisionId,
                                                                            ShowroomNarrationLanguage language) {
        ShowroomNarrationKey key = new ShowroomNarrationKey(ShowroomNarrationTargetType.PRODUCT, productId,
                ShowroomNarrationAudienceType.PUBLIC, language);
        return narrationService.live(key)
                .filter(version -> sourceRevisionId.equals(version.sourceRevisionId()));
    }

    private int countRemainingActionableProducts(ProductBatchNarrationAutoCheckCriteria criteria) {
        return (int) listProductsForBatch(criteria).stream()
                .filter(row -> STATUS_PUBLISHED.equalsIgnoreCase(row.revision().status()))
                .filter(row -> {
                    ProductNarrationBatchEvaluation evaluation = evaluateCurrentProductNarrationBatch(row.productId());
                    return !evaluation.existingReady() && !evaluation.missingScript();
                })
                .count();
    }

    private ProductBatchNarrationAutoCheckCriteria toProductBatchNarrationAutoCheckCriteria(
            ShowroomAdminController.ProductBatchGenerateReqVO req) {
        return normalizeProductBatchNarrationAutoCheckCriteria(new ProductBatchNarrationAutoCheckCriteria(
                req.keyword(), req.lifecycleStage(), req.incompleteStatus(), req.approvalStatus()));
    }

    private ProductBatchNarrationAutoCheckCriteria normalizeProductBatchNarrationAutoCheckCriteria(
            ProductBatchNarrationAutoCheckCriteria criteria) {
        if (criteria == null) {
            return new ProductBatchNarrationAutoCheckCriteria("", "", "", "");
        }
        return new ProductBatchNarrationAutoCheckCriteria(
                nullToEmpty(criteria.keyword()).trim(),
                nullToEmpty(criteria.lifecycleStage()).trim(),
                nullToEmpty(criteria.incompleteStatus()).trim(),
                nullToEmpty(criteria.approvalStatus()).trim());
    }

    private ProductBatchNarrationAutoCheckState loadProductBatchNarrationAutoCheckState() {
        boolean enabled = Boolean.parseBoolean(getConfigValue(
                PRODUCT_BATCH_AUDIO_AUTO_CHECK_ENABLED_KEY, "false"));
        ProductBatchNarrationAutoCheckCriteria criteria = JsonUtils.parseObjectQuietly(
                getConfigValue(PRODUCT_BATCH_AUDIO_AUTO_CHECK_FILTERS_KEY, null),
                ProductBatchNarrationAutoCheckCriteria.class);
        ProductBatchNarrationAutoCheckSummaryData summary = JsonUtils.parseObjectQuietly(
                getConfigValue(PRODUCT_BATCH_AUDIO_AUTO_CHECK_SUMMARY_KEY, null),
                ProductBatchNarrationAutoCheckSummaryData.class);
        ProductBatchNarrationAutoCheckFailureData failure = JsonUtils.parseObjectQuietly(
                getConfigValue(PRODUCT_BATCH_AUDIO_AUTO_CHECK_FAILURE_KEY, null),
                ProductBatchNarrationAutoCheckFailureData.class);
        return new ProductBatchNarrationAutoCheckState(enabled,
                normalizeProductBatchNarrationAutoCheckCriteria(criteria),
                summary == null ? emptyProductBatchNarrationAutoCheckSummary() : summary,
                failure == null ? emptyProductBatchNarrationAutoCheckFailure() : failure);
    }

    private void saveProductBatchNarrationAutoCheckState(ProductBatchNarrationAutoCheckState state) {
        ProductBatchNarrationAutoCheckCriteria criteria =
                normalizeProductBatchNarrationAutoCheckCriteria(state.criteria());
        ProductBatchNarrationAutoCheckSummaryData summary =
                state.summary() == null ? emptyProductBatchNarrationAutoCheckSummary() : state.summary();
        ProductBatchNarrationAutoCheckFailureData failure =
                state.failure() == null ? emptyProductBatchNarrationAutoCheckFailure() : state.failure();
        upsertHiddenConfig(PRODUCT_BATCH_AUDIO_AUTO_CHECK_ENABLED_KEY,
                PRODUCT_BATCH_AUDIO_AUTO_CHECK_ENABLED_NAME, String.valueOf(state.enabled()),
                "展厅产品批量语音自动检查开关");
        upsertHiddenConfig(PRODUCT_BATCH_AUDIO_AUTO_CHECK_FILTERS_KEY,
                PRODUCT_BATCH_AUDIO_AUTO_CHECK_FILTERS_NAME, JsonUtils.toJsonString(criteria),
                "展厅产品批量语音自动检查筛选快照");
        upsertHiddenConfig(PRODUCT_BATCH_AUDIO_AUTO_CHECK_SUMMARY_KEY,
                PRODUCT_BATCH_AUDIO_AUTO_CHECK_SUMMARY_NAME, JsonUtils.toJsonString(summary),
                "展厅产品批量语音自动检查最近一次汇总");
        upsertHiddenConfig(PRODUCT_BATCH_AUDIO_AUTO_CHECK_FAILURE_KEY,
                PRODUCT_BATCH_AUDIO_AUTO_CHECK_FAILURE_NAME, JsonUtils.toJsonString(failure),
                "展厅产品批量语音自动检查最近一次失败");
    }

    private ProductBatchNarrationAutoCheckState buildProductBatchNarrationAutoCheckState(
            ProductBatchNarrationAutoCheckCriteria criteria,
            ProductBatchNarrationExecutionSummary summary) {
        ProductBatchNarrationAutoCheckFailureData failure = summary.failedCount() > 0 && !summary.failures().isEmpty()
                ? new ProductBatchNarrationAutoCheckFailureData(
                truncate(summary.failures().get(0).reason(), 160), Instant.now().toEpochMilli())
                : emptyProductBatchNarrationAutoCheckFailure();
        return new ProductBatchNarrationAutoCheckState(summary.remainingActionableCount() > 0,
                normalizeProductBatchNarrationAutoCheckCriteria(criteria),
                new ProductBatchNarrationAutoCheckSummaryData(summary.matchedCount(), summary.publishedCount(),
                        summary.skippedUnpublishedCount(), summary.skippedExistingCount(),
                        summary.skippedMissingScriptCount(), summary.succeededCount(), summary.failedCount(),
                        summary.remainingActionableCount(), Instant.now().toEpochMilli()),
                failure);
    }

    private ShowroomAdminController.ProductBatchGenerateRespVO toProductBatchGenerateResp(
            ProductBatchNarrationExecutionSummary summary, boolean autoCheckEnabled) {
        return new ShowroomAdminController.ProductBatchGenerateRespVO(summary.matchedCount(),
                summary.publishedCount(), summary.skippedUnpublishedCount(), summary.skippedExistingCount(),
                summary.skippedMissingScriptCount(), summary.succeededCount(), summary.failedCount(),
                autoCheckEnabled, summary.remainingActionableCount(), null, "", 0, null, summary.failures());
    }

    private ShowroomAdminController.ProductBatchGenerateStateRespVO toProductBatchGenerateStateResp(
            ProductBatchNarrationAutoCheckState state) {
        ProductBatchNarrationAutoCheckCriteria criteria = normalizeProductBatchNarrationAutoCheckCriteria(
                state.criteria());
        ProductBatchNarrationAutoCheckSummaryData summary =
                state.summary() == null ? emptyProductBatchNarrationAutoCheckSummary() : state.summary();
        ProductBatchNarrationAutoCheckFailureData failure =
                state.failure() == null ? emptyProductBatchNarrationAutoCheckFailure() : state.failure();
        return new ShowroomAdminController.ProductBatchGenerateStateRespVO(state.enabled(), criteria.keyword(),
                criteria.lifecycleStage(), criteria.incompleteStatus(), criteria.approvalStatus(),
                summary.matchedCount(), summary.publishedCount(), summary.skippedUnpublishedCount(),
                summary.skippedExistingCount(), summary.skippedMissingScriptCount(), summary.succeededCount(),
                summary.failedCount(), summary.remainingActionableCount(), summary.lastRunAt(),
                nullToEmpty(failure.message()), failure.lastFailureAt());
    }

    private ProductBatchNarrationAutoCheckSummaryData emptyProductBatchNarrationAutoCheckSummary() {
        return new ProductBatchNarrationAutoCheckSummaryData(0, 0, 0, 0, 0, 0, 0, 0, null);
    }

    private ProductBatchNarrationAutoCheckFailureData emptyProductBatchNarrationAutoCheckFailure() {
        return new ProductBatchNarrationAutoCheckFailureData("", null);
    }

    private void triggerProductBatchNarrationScriptTaskAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                runScheduledProductBatchNarrationScriptAutoCheck();
            } catch (RuntimeException ignored) {
                // State is persisted by the locked executor path; callers re-read status from storage.
            }
        });
    }

    private ProductNarrationScriptBatchExecutionSummary summarizeProductNarrationScriptBatch(
            ProductNarrationScriptBatchTaskCriteria criteria) {
        List<ShowroomAdminController.ProductPageRespVO> matchedRows = listProductsForBatch(new ShowroomAdminController.PageQueryReqVO(
                criteria.keyword(), null, null, null, null, criteria.lifecycleStage(),
                criteria.incompleteStatus(), criteria.approvalStatus(), null));
        int skippedCompletedCount = 0;
        for (ShowroomAdminController.ProductPageRespVO row : matchedRows) {
            ProductNarrationScriptBatchEvaluation evaluation = evaluateProductNarrationScriptBatch(
                    row.productId(), row.revision().revisionId());
            if (evaluation.zhReady() && evaluation.enReady()) {
                skippedCompletedCount++;
            }
        }
        return new ProductNarrationScriptBatchExecutionSummary(
                matchedRows.size(),
                skippedCompletedCount,
                0,
                0,
                matchedRows.size() - skippedCompletedCount,
                List.of()
        );
    }

    private void executeProductNarrationScriptBatchTaskLocked(ProductNarrationScriptBatchTaskState initialState) {
        ProductNarrationScriptBatchTaskSummaryData currentSummary =
                initialState.summary() == null ? emptyProductNarrationScriptBatchTaskSummary() : initialState.summary();
        long startedAt = currentSummary.startedAt() == null ? Instant.now().toEpochMilli() : currentSummary.startedAt();
        ProductNarrationScriptBatchTaskState runningState = new ProductNarrationScriptBatchTaskState(
                true,
                true,
                normalizeProductNarrationScriptBatchTaskCriteria(initialState.criteria()),
                new ProductNarrationScriptBatchTaskSummaryData(currentSummary.matchedCount(),
                        currentSummary.skippedCompletedCount(), currentSummary.generatedLanguageCount(),
                        0, currentSummary.remainingCount(), startedAt,
                        currentSummary.lastRunAt(), null, null),
                emptyProductNarrationScriptBatchTaskFailure());
        saveProductNarrationScriptBatchTaskState(runningState);
        try {
            ProductNarrationScriptBatchExecutionSummary summary =
                    runProductNarrationScriptBatch(runningState);
            saveProductNarrationScriptBatchTaskState(buildProductNarrationScriptBatchTaskState(
                    runningState.criteria(), summary, startedAt));
        } catch (RuntimeException exception) {
            long failedAt = Instant.now().toEpochMilli();
            ProductNarrationScriptBatchTaskState persistedState = loadProductNarrationScriptBatchTaskState();
            ProductNarrationScriptBatchTaskSummaryData persistedSummary =
                    persistedState.summary() == null ? emptyProductNarrationScriptBatchTaskSummary()
                            : persistedState.summary();
            ProductNarrationScriptBatchTaskState failedState = new ProductNarrationScriptBatchTaskState(
                    true,
                    false,
                    runningState.criteria(),
                    new ProductNarrationScriptBatchTaskSummaryData(persistedSummary.matchedCount(),
                            persistedSummary.skippedCompletedCount(), persistedSummary.generatedLanguageCount(),
                            persistedSummary.failedCount(), persistedSummary.remainingCount(), startedAt,
                            failedAt, null, null),
                    new ProductNarrationScriptBatchTaskFailureData(null, "", "",
                            truncate(batchFailureReason(exception), 160), failedAt));
            saveProductNarrationScriptBatchTaskState(failedState);
            throw exception;
        }
    }

    private ProductNarrationScriptBatchExecutionSummary runProductNarrationScriptBatch(
            ProductNarrationScriptBatchTaskState runningState) {
        ProductNarrationScriptBatchTaskCriteria criteria = runningState.criteria();
        ProductNarrationScriptBatchTaskSummaryData currentSummary =
                runningState.summary() == null ? emptyProductNarrationScriptBatchTaskSummary() : runningState.summary();
        ProductNarrationScriptBatchTaskFailureData currentFailure =
                runningState.failure() == null ? emptyProductNarrationScriptBatchTaskFailure() : runningState.failure();
        List<ShowroomAdminController.ProductPageRespVO> matchedRows = listProductsForBatch(new ShowroomAdminController.PageQueryReqVO(
                criteria.keyword(), null, null, null, null, criteria.lifecycleStage(),
                criteria.incompleteStatus(), criteria.approvalStatus(), null));
        List<ShowroomAdminController.ProductBatchGenerateFailureRespVO> failures = new ArrayList<>();
        int generatedLanguageCount = currentSummary.generatedLanguageCount();
        int failedCount = 0;
        int remainingCount = currentSummary.remainingCount();
        for (ShowroomAdminController.ProductPageRespVO row : matchedRows) {
            ProductNarrationScriptBatchTaskCurrentProductData currentProduct =
                    toProductNarrationScriptBatchTaskCurrentProductData(row);
            saveProductNarrationScriptBatchTaskState(buildRunningProductNarrationScriptBatchTaskState(
                    criteria, currentSummary, currentFailure, currentProduct,
                    generatedLanguageCount, failedCount, remainingCount));
            try {
                ProductNarrationScriptBatchEvaluation evaluation = evaluateProductNarrationScriptBatch(
                        row.productId(), row.revision().revisionId());
                if (evaluation.zhReady() && evaluation.enReady()) {
                    saveProductNarrationScriptBatchTaskState(buildRunningProductNarrationScriptBatchTaskState(
                            criteria, currentSummary, currentFailure, currentProduct,
                            generatedLanguageCount, failedCount, remainingCount));
                    continue;
                }
                generatedLanguageCount += generateMissingProductNarrationScripts(evaluation);
                remainingCount = Math.max(0, remainingCount - 1);
            } catch (RuntimeException exception) {
                failures.add(toBatchFailure(row, exception));
                failedCount++;
            }
            saveProductNarrationScriptBatchTaskState(buildRunningProductNarrationScriptBatchTaskState(
                    criteria, currentSummary, currentFailure, currentProduct,
                    generatedLanguageCount, failedCount, remainingCount));
        }
        int finalRemainingCount = countRemainingProductNarrationScriptBatch(criteria);
        return new ProductNarrationScriptBatchExecutionSummary(
                matchedRows.size(),
                currentSummary.skippedCompletedCount(),
                generatedLanguageCount,
                failedCount,
                finalRemainingCount,
                List.copyOf(failures)
        );
    }

    private ProductNarrationScriptBatchEvaluation evaluateProductNarrationScriptBatch(Long productId, Long sourceRevisionId) {
        ShowroomNarrationVersion zhLatest = findLatestNarrationForRevision(productId, sourceRevisionId,
                ShowroomNarrationLanguage.ZH)
                .filter(version -> hasText(version.scriptText()))
                .orElse(null);
        ShowroomNarrationVersion enLatest = findLatestNarrationForRevision(productId, sourceRevisionId,
                ShowroomNarrationLanguage.EN)
                .filter(version -> hasText(version.scriptText()))
                .orElse(null);
        return new ProductNarrationScriptBatchEvaluation(productId, sourceRevisionId,
                zhLatest != null, enLatest != null, zhLatest, enLatest);
    }

    private int generateMissingProductNarrationScripts(ProductNarrationScriptBatchEvaluation evaluation) {
        ShowroomProductSnapshot snapshot = contentService.getProduct(evaluation.productId());
        ShowroomProductRevision revision = contentService.getProductRevision(evaluation.sourceRevisionId());
        String zhScriptText = evaluation.zhLatest() == null ? "" : nullToEmpty(evaluation.zhLatest().scriptText()).trim();
        int generatedLanguageCount = 0;
        if (!hasText(zhScriptText)) {
            zhScriptText = requireText(productNarrationCodexService.generateScript(snapshot, revision),
                    "SHOWROOM_SCRIPT_GENERATION_FAILED: generated zh narration script is required");
            narrationService.draftScript(new ShowroomNarrationDraftCommand(
                    ShowroomNarrationTargetType.PRODUCT, evaluation.productId(), evaluation.sourceRevisionId(),
                    ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH, zhScriptText, true));
            generatedLanguageCount++;
        }
        if (!evaluation.enReady()) {
            String translatedScript = requireText(productNarrationCodexService.translateZhToEn(zhScriptText),
                    "SHOWROOM_TRANSLATION_FAILED: generated en narration script is required");
            narrationService.draftScript(new ShowroomNarrationDraftCommand(
                    ShowroomNarrationTargetType.PRODUCT, evaluation.productId(), evaluation.sourceRevisionId(),
                    ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN, translatedScript, true));
            generatedLanguageCount++;
        }
        return generatedLanguageCount;
    }

    private int generateMissingProductSalesCountries(ShowroomAdminController.ProductPageRespVO row) {
        ShowroomAdminController.ProductDetailRespVO latestRevision = row.revision();
        ShowroomProductRevision revision = contentService.getProductRevision(latestRevision.revisionId());
        LinkedHashMap<String, String> fields = new LinkedHashMap<>(revision.fields());
        String zhSalesCountries = nullToEmpty(fields.get("target_market")).trim();
        String enSalesCountries = nullToEmpty(fields.get("target_market_en")).trim();
        if (hasText(zhSalesCountries) && hasText(enSalesCountries)) {
            return 0;
        }
        ShowroomProductSnapshot snapshot = contentService.getProduct(row.productId());
        int generatedLanguageCount = 0;
        if (!hasText(zhSalesCountries)) {
            zhSalesCountries = requireText(
                    productNarrationCodexService.generateSalesCountries(snapshot, revision),
                    "SHOWROOM_SALES_COUNTRIES_GENERATION_FAILED: generated zh sales countries is required");
            fields.put("target_market", zhSalesCountries);
            generatedLanguageCount++;
        }
        if (!hasText(enSalesCountries)) {
            enSalesCountries = requireText(
                    productNarrationCodexService.translateZhToEn(zhSalesCountries),
                    "SHOWROOM_TRANSLATION_FAILED: generated en sales countries is required");
            fields.put("target_market_en", enSalesCountries);
            generatedLanguageCount++;
        }
        contentService.saveProductDraft(new ShowroomProductDraft(
                revision.productId(),
                row.productCode(),
                revision.nameCn(),
                revision.nameEn(),
                fields));
        return generatedLanguageCount;
    }

    private int countRemainingProductNarrationScriptBatch(ProductNarrationScriptBatchTaskCriteria criteria) {
        return (int) listProductsForBatch(new ShowroomAdminController.PageQueryReqVO(
                criteria.keyword(), null, null, null, null, criteria.lifecycleStage(),
                criteria.incompleteStatus(), criteria.approvalStatus(), null))
                .stream()
                .filter(row -> {
                    ProductNarrationScriptBatchEvaluation evaluation = evaluateProductNarrationScriptBatch(
                            row.productId(), row.revision().revisionId());
                    return !evaluation.zhReady() || !evaluation.enReady();
                })
                .count();
    }

    private ProductNarrationScriptBatchTaskCriteria toProductNarrationScriptBatchTaskCriteria(
            ShowroomAdminController.ProductBatchGenerateReqVO req) {
        return normalizeProductNarrationScriptBatchTaskCriteria(new ProductNarrationScriptBatchTaskCriteria(
                req.keyword(), req.lifecycleStage(), req.incompleteStatus(), req.approvalStatus()));
    }

    private ProductNarrationScriptBatchTaskCriteria normalizeProductNarrationScriptBatchTaskCriteria(
            ProductNarrationScriptBatchTaskCriteria criteria) {
        if (criteria == null) {
            return new ProductNarrationScriptBatchTaskCriteria("", "", "", "");
        }
        return new ProductNarrationScriptBatchTaskCriteria(
                nullToEmpty(criteria.keyword()).trim(),
                nullToEmpty(criteria.lifecycleStage()).trim(),
                nullToEmpty(criteria.incompleteStatus()).trim(),
                nullToEmpty(criteria.approvalStatus()).trim());
    }

    private ProductNarrationScriptBatchTaskState loadProductNarrationScriptBatchTaskState() {
        boolean active = Boolean.parseBoolean(getConfigValue(PRODUCT_BATCH_NARRATION_SCRIPT_ACTIVE_KEY, "false"));
        boolean running = Boolean.parseBoolean(getConfigValue(PRODUCT_BATCH_NARRATION_SCRIPT_RUNNING_KEY, "false"));
        ProductNarrationScriptBatchTaskCriteria criteria = JsonUtils.parseObjectQuietly(
                getConfigValue(PRODUCT_BATCH_NARRATION_SCRIPT_FILTERS_KEY, null),
                ProductNarrationScriptBatchTaskCriteria.class);
        ProductNarrationScriptBatchTaskSummaryData summary = JsonUtils.parseObjectQuietly(
                getConfigValue(PRODUCT_BATCH_NARRATION_SCRIPT_SUMMARY_KEY, null),
                ProductNarrationScriptBatchTaskSummaryData.class);
        ProductNarrationScriptBatchTaskFailureData failure = JsonUtils.parseObjectQuietly(
                getConfigValue(PRODUCT_BATCH_NARRATION_SCRIPT_FAILURE_KEY, null),
                ProductNarrationScriptBatchTaskFailureData.class);
        boolean normalizedRunning = active && running && productBatchNarrationScriptTaskLock.isLocked();
        return new ProductNarrationScriptBatchTaskState(
                active,
                normalizedRunning,
                normalizeProductNarrationScriptBatchTaskCriteria(criteria),
                summary == null ? emptyProductNarrationScriptBatchTaskSummary() : summary,
                failure == null ? emptyProductNarrationScriptBatchTaskFailure() : failure
        );
    }

    private void saveProductNarrationScriptBatchTaskState(ProductNarrationScriptBatchTaskState state) {
        ProductNarrationScriptBatchTaskCriteria criteria =
                normalizeProductNarrationScriptBatchTaskCriteria(state.criteria());
        ProductNarrationScriptBatchTaskSummaryData summary =
                state.summary() == null ? emptyProductNarrationScriptBatchTaskSummary() : state.summary();
        ProductNarrationScriptBatchTaskFailureData failure =
                state.failure() == null ? emptyProductNarrationScriptBatchTaskFailure() : state.failure();
        upsertHiddenConfig(PRODUCT_BATCH_NARRATION_SCRIPT_ACTIVE_KEY,
                PRODUCT_BATCH_NARRATION_SCRIPT_ACTIVE_NAME, String.valueOf(state.active()),
                "展厅产品批量讲解任务活动状态");
        upsertHiddenConfig(PRODUCT_BATCH_NARRATION_SCRIPT_RUNNING_KEY,
                PRODUCT_BATCH_NARRATION_SCRIPT_RUNNING_NAME, String.valueOf(state.running()),
                "展厅产品批量讲解任务运行状态");
        upsertHiddenConfig(PRODUCT_BATCH_NARRATION_SCRIPT_FILTERS_KEY,
                PRODUCT_BATCH_NARRATION_SCRIPT_FILTERS_NAME, JsonUtils.toJsonString(criteria),
                "展厅产品批量讲解任务筛选快照");
        upsertHiddenConfig(PRODUCT_BATCH_NARRATION_SCRIPT_SUMMARY_KEY,
                PRODUCT_BATCH_NARRATION_SCRIPT_SUMMARY_NAME, JsonUtils.toJsonString(summary),
                "展厅产品批量讲解任务最近一次汇总");
        upsertHiddenConfig(PRODUCT_BATCH_NARRATION_SCRIPT_FAILURE_KEY,
                PRODUCT_BATCH_NARRATION_SCRIPT_FAILURE_NAME, JsonUtils.toJsonString(failure),
                "展厅产品批量讲解任务最近一次失败");
    }

    private ProductNarrationScriptBatchTaskState buildProductNarrationScriptBatchTaskState(
            ProductNarrationScriptBatchTaskCriteria criteria,
            ProductNarrationScriptBatchExecutionSummary summary,
            long startedAt) {
        long executedAt = Instant.now().toEpochMilli();
        ShowroomAdminController.ProductBatchGenerateFailureRespVO firstFailure =
                summary.failures().isEmpty() ? null : summary.failures().get(0);
        ProductNarrationScriptBatchTaskFailureData failure = firstFailure == null
                ? emptyProductNarrationScriptBatchTaskFailure()
                : new ProductNarrationScriptBatchTaskFailureData(firstFailure.productId(),
                truncate(firstFailure.productCode(), 64),
                truncate(firstFailure.nameCn(), 64),
                truncate(firstFailure.reason(), 160),
                executedAt);
        boolean active = summary.remainingCount() > 0;
        return new ProductNarrationScriptBatchTaskState(
                active,
                false,
                normalizeProductNarrationScriptBatchTaskCriteria(criteria),
                new ProductNarrationScriptBatchTaskSummaryData(summary.matchedCount(),
                        summary.skippedCompletedCount(), summary.generatedLanguageCount(),
                        summary.failedCount(), summary.remainingCount(), startedAt, executedAt,
                        active ? null : executedAt, null),
                failure
        );
    }

    private ProductNarrationScriptBatchTaskState buildRunningProductNarrationScriptBatchTaskState(
            ProductNarrationScriptBatchTaskCriteria criteria,
            ProductNarrationScriptBatchTaskSummaryData baseSummary,
            ProductNarrationScriptBatchTaskFailureData failure,
            ProductNarrationScriptBatchTaskCurrentProductData currentProduct,
            int generatedLanguageCount,
            int failedCount,
            int remainingCount) {
        long touchedAt = Instant.now().toEpochMilli();
        return new ProductNarrationScriptBatchTaskState(
                true,
                true,
                normalizeProductNarrationScriptBatchTaskCriteria(criteria),
                new ProductNarrationScriptBatchTaskSummaryData(baseSummary.matchedCount(),
                        baseSummary.skippedCompletedCount(), generatedLanguageCount, failedCount,
                        Math.max(remainingCount, 0), baseSummary.startedAt(), touchedAt, null, currentProduct),
                failure == null ? emptyProductNarrationScriptBatchTaskFailure() : failure
        );
    }

    private ProductNarrationScriptBatchTaskCurrentProductData toProductNarrationScriptBatchTaskCurrentProductData(
            ShowroomAdminController.ProductPageRespVO row) {
        return new ProductNarrationScriptBatchTaskCurrentProductData(
                row.productId(),
                truncate(row.productCode(), 64),
                truncate(row.revision() == null ? "" : row.revision().nameCn(), 64)
        );
    }

    private ShowroomAdminController.ProductNarrationScriptBatchTaskRespVO toProductNarrationScriptBatchTaskResp(
            ProductNarrationScriptBatchTaskState state) {
        ProductNarrationScriptBatchTaskCriteria criteria =
                normalizeProductNarrationScriptBatchTaskCriteria(state.criteria());
        ProductNarrationScriptBatchTaskSummaryData summary =
                state.summary() == null ? emptyProductNarrationScriptBatchTaskSummary() : state.summary();
        ProductNarrationScriptBatchTaskFailureData failure =
                state.failure() == null ? emptyProductNarrationScriptBatchTaskFailure() : state.failure();
        ProductNarrationScriptBatchTaskCurrentProductData currentProduct = summary.currentProduct();
        ShowroomAdminController.ProductNarrationScriptTaskCurrentProductRespVO currentProductResp =
                currentProduct == null || currentProduct.productId() == null ? null
                        : new ShowroomAdminController.ProductNarrationScriptTaskCurrentProductRespVO(
                        currentProduct.productId(),
                        nullToEmpty(currentProduct.productCode()),
                        nullToEmpty(currentProduct.nameCn()));
        ShowroomAdminController.ProductBatchGenerateFailureRespVO lastFailure =
                failure.productId() == null && !hasText(failure.reason()) ? null
                        : new ShowroomAdminController.ProductBatchGenerateFailureRespVO(
                        failure.productId(), nullToEmpty(failure.productCode()),
                        nullToEmpty(failure.nameCn()), nullToEmpty(failure.reason()));
        return new ShowroomAdminController.ProductNarrationScriptBatchTaskRespVO(
                state.active(),
                state.running(),
                criteria.keyword(),
                criteria.lifecycleStage(),
                criteria.incompleteStatus(),
                criteria.approvalStatus(),
                summary.matchedCount(),
                summary.skippedCompletedCount(),
                summary.generatedLanguageCount(),
                summary.failedCount(),
                summary.remainingCount(),
                summary.startedAt(),
                summary.lastRunAt(),
                summary.completedAt(),
                currentProductResp,
                lastFailure,
                failure.lastFailureAt()
        );
    }

    private ProductNarrationScriptBatchTaskSummaryData emptyProductNarrationScriptBatchTaskSummary() {
        return new ProductNarrationScriptBatchTaskSummaryData(0, 0, 0, 0, 0, null, null, null, null);
    }

    private ProductNarrationScriptBatchTaskFailureData emptyProductNarrationScriptBatchTaskFailure() {
        return new ProductNarrationScriptBatchTaskFailureData(null, "", "", "", null);
    }

    private String getConfigValue(String key, String defaultValue) {
        ConfigDO config = configService.getConfigByKey(key);
        if (config == null || config.getValue() == null) {
            return defaultValue;
        }
        return config.getValue();
    }

    private void upsertHiddenConfig(String key, String name, String value, String remark) {
        ConfigDO existing = configService.getConfigByKey(key);
        ConfigSaveReqVO reqVO = new ConfigSaveReqVO();
        if (existing != null) {
            reqVO.setId(existing.getId());
        }
        reqVO.setCategory(PRODUCT_BATCH_AUDIO_AUTO_CHECK_CATEGORY);
        reqVO.setName(name);
        reqVO.setKey(key);
        reqVO.setValue(value);
        reqVO.setVisible(false);
        reqVO.setRemark(remark);
        if (existing == null) {
            configService.createConfig(reqVO);
            return;
        }
        configService.updateConfig(reqVO);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return nullToEmpty(value);
        }
        return value.substring(0, maxLength);
    }

    private String generateSingleProductCoverImage(Long productId, String productCode, String nameCn, String nameEn,
                                                   Map<String, String> promptFields) {
        contentService.getProduct(productId);
        requireMap(promptFields, "SHOWROOM_COVER_GENERATION_FAILED: product fields are required");
        Long promptVersionId = imagePromptVersionService.requireCurrentVersionId(
                ShowroomImagePromptVersionService.SCENE_PRODUCT_COVER);
        String renderedPrompt = imagePromptVersionService.renderProductCoverPrompt(promptVersionId, nameCn, nameEn);
        String coverImage = productCoverImageService.generateCoverImage(productCode, renderedPrompt);
        imagePromptVersionService.recordUsage(promptVersionId);
        return coverImage;
    }

    private void generateAndPublishProductCoverImage(Long productId, Long operatorUserId) {
        ShowroomProductSnapshot snapshot = contentService.getProduct(productId);
        ShowroomProductRevision currentRevision = contentService.requireCurrentProductRevision(productId);
        generateSingleProductCoverImage(productId,
                requireText(snapshot.productCode(), "SHOWROOM_COVER_GENERATION_FAILED: product code is required"),
                requireText(currentRevision.nameCn(), "SHOWROOM_COVER_GENERATION_FAILED: product chinese name is required"),
                requireText(currentRevision.nameEn(), "SHOWROOM_COVER_GENERATION_FAILED: product english name is required"),
                new LinkedHashMap<>(currentRevision.fields()));
    }

    private ProductBatchCoverTaskResult generateProductCoverInBatch(
            ShowroomAdminController.ProductPageRespVO row, Long operatorUserId) {
        try {
            generateAndPublishProductCoverImage(row.productId(), operatorUserId);
            return new ProductBatchCoverTaskResult(true, null);
        } catch (RuntimeException exception) {
            return new ProductBatchCoverTaskResult(false, toBatchFailure(row, exception));
        }
    }

    private ShowroomProductCoverBatchTaskService.TaskItemSnapshot toProductCoverTaskItemSnapshot(
            ShowroomAdminController.ProductPageRespVO row) {
        ShowroomAdminController.ProductDetailRespVO revision = row.revision();
        return new ShowroomProductCoverBatchTaskService.TaskItemSnapshot(
                row.productId(),
                revision.revisionId(),
                requireText(row.productCode(), "SHOWROOM_COVER_GENERATION_FAILED: product code is required"),
                requireText(revision.nameCn(), "SHOWROOM_COVER_GENERATION_FAILED: product chinese name is required"),
                requireText(revision.nameEn(), "SHOWROOM_COVER_GENERATION_FAILED: product english name is required"),
                new LinkedHashMap<>(revision.fields())
        );
    }

    private ShowroomNarrationVersion requireLatestNarrationForRevision(Long productId, Long sourceRevisionId,
                                                                       ShowroomNarrationLanguage language) {
        ShowroomNarrationKey key = new ShowroomNarrationKey(ShowroomNarrationTargetType.PRODUCT, productId,
                ShowroomNarrationAudienceType.PUBLIC, language);
        ShowroomNarrationVersion version = narrationService.latest(key, sourceRevisionId)
                .orElseThrow(() -> new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: latest "
                        + language.name() + " narration not found for current published product revision"));
        if (!hasText(version.scriptText())) {
            throw new IllegalStateException(
                    "SHOWROOM_SCRIPT_MISSING: latest " + language.name() + " narration script is required");
        }
        return version;
    }

    private ProductNarrationPair requireProductNarrationPairForRevision(Long productId, Long sourceRevisionId) {
        ShowroomNarrationVersion zhNarration = requireLatestNarrationForRevision(productId, sourceRevisionId,
                ShowroomNarrationLanguage.ZH);
        ShowroomNarrationVersion enNarration = requireLatestNarrationForRevision(productId, sourceRevisionId,
                ShowroomNarrationLanguage.EN);
        if (!hasText(enNarration.scriptText())) {
            throw new IllegalStateException(
                    "SHOWROOM_SCRIPT_MISSING: latest EN narration script is required for current product revision");
        }
        return new ProductNarrationPair(zhNarration, enNarration);
    }

    private ProductNarrationPair prepareProductNarrationPairForRevision(Long productId, Long sourceRevisionId) {
        ShowroomProductRevision revision = contentService.getProductRevision(sourceRevisionId);
        if (!revision.productId().equals(productId)) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: product narration source revision mismatch");
        }
        ShowroomNarrationVersion zhNarration = findLatestNarrationForRevision(productId, sourceRevisionId,
                ShowroomNarrationLanguage.ZH)
                .filter(version -> hasText(version.scriptText()))
                .orElseGet(() -> draftGeneratedProductNarrationScript(productId, revision));
        ShowroomNarrationVersion enNarration = findLatestNarrationForRevision(productId, sourceRevisionId,
                ShowroomNarrationLanguage.EN)
                .filter(version -> hasText(version.scriptText()))
                .orElseGet(() -> draftTranslatedProductNarrationScript(productId, sourceRevisionId,
                        zhNarration.scriptText()));
        return new ProductNarrationPair(zhNarration, enNarration);
    }

    private ShowroomNarrationVersion draftGeneratedProductNarrationScript(Long productId,
                                                                          ShowroomProductRevision revision) {
        String generatedScript = requireText(productNarrationCodexService.generateScript(
                        contentService.getProduct(productId), revision),
                "SHOWROOM_SCRIPT_GENERATION_FAILED: generated ZH narration script is required");
        return narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.PRODUCT, productId, revision.revisionId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH, generatedScript, true));
    }

    private ShowroomNarrationVersion draftTranslatedProductNarrationScript(Long productId, Long sourceRevisionId,
                                                                           String zhScriptText) {
        String translatedScript = requireText(productNarrationCodexService.translateZhToEn(zhScriptText),
                "SHOWROOM_TRANSLATION_FAILED: generated EN narration script is required");
        return narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.PRODUCT, productId, sourceRevisionId,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN, translatedScript, true));
    }

    private ProductNarrationPair draftProductNarrationPair(Long productId, Long sourceRevisionId,
                                                           ProductNarrationPair sourcePair) {
        ShowroomNarrationVersion zhDraft = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.PRODUCT, productId, sourceRevisionId,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH,
                sourcePair.zh().scriptText(), sourcePair.zh().generatedByAi()));
        ShowroomNarrationVersion enDraft = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.PRODUCT, productId, sourceRevisionId,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN,
                sourcePair.en().scriptText(), sourcePair.en().generatedByAi()));
        return new ProductNarrationPair(zhDraft, enDraft);
    }

    private ProductNarrationPair draftHallNarrationPair(ShowroomHall hall) {
        String zhScript = requireText(hall.description(),
                "SHOWROOM_SCRIPT_MISSING: hall ZH description is required for HALL:" + hall.hallId());
        String enScript = requireText(hall.descriptionEn(),
                "SHOWROOM_SCRIPT_MISSING: hall EN description is required for HALL:" + hall.hallId());
        ShowroomNarrationVersion zhDraft = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH, zhScript, false));
        ShowroomNarrationVersion enDraft = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.HALL, hall.hallId(), hall.hallId(),
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN, enScript, false));
        return new ProductNarrationPair(zhDraft, enDraft);
    }

    private ProductNarrationPair generateProductNarrationAudioPair(ProductNarrationPair draftPair) {
        return generateNarrationAudioPair(draftPair);
    }

    private ProductNarrationPair generateNarrationAudioPair(ProductNarrationPair draftPair) {
        ShowroomNarrationVersion generatedZh = narrationService.generateAudio(draftPair.zh().id());
        ShowroomNarrationVersion generatedEn = narrationService.generateAudio(draftPair.en().id());
        return new ProductNarrationPair(generatedZh, generatedEn);
    }

    private ProductNarrationPair publishHallNarrationPair(ProductNarrationPair narrationPair) {
        ShowroomNarrationVersion publishedZh = narrationService.publishDirectly(narrationPair.zh().id());
        ShowroomNarrationVersion publishedEn = narrationService.publishDirectly(narrationPair.en().id());
        markReleaseDirtyForNarrationIfCurrent(publishedZh, null, "HALL_PUBLIC_NARRATION_PUBLISHED");
        markReleaseDirtyForNarrationIfCurrent(publishedEn, null, "HALL_PUBLIC_NARRATION_PUBLISHED");
        return new ProductNarrationPair(publishedZh, publishedEn);
    }

    private ProductNarrationPair carryForwardProductNarrationAudioPair(ProductNarrationPair draftPair,
                                                                       ProductNarrationPair sourcePair) {
        ShowroomNarrationVersion zhDraft = carryForwardProductNarrationAudio(draftPair.zh(), sourcePair.zh(),
                ShowroomNarrationLanguage.ZH);
        ShowroomNarrationVersion enDraft = carryForwardProductNarrationAudio(draftPair.en(), sourcePair.en(),
                ShowroomNarrationLanguage.EN);
        return new ProductNarrationPair(zhDraft, enDraft);
    }

    private ShowroomNarrationVersion carryForwardProductNarrationAudio(ShowroomNarrationVersion draft,
                                                                       ShowroomNarrationVersion source,
                                                                       ShowroomNarrationLanguage language) {
        if (source.audioFileId() == null || source.audioDurationSeconds() == null
                || source.audioDurationSeconds() <= 0) {
            throw new IllegalStateException("SHOWROOM_AUDIO_GENERATION_FAILED: latest " + language.name()
                    + " product narration audio is required for import publish source");
        }
        return narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(
                draft.id(), source.audioFileId(), source.audioDurationSeconds(), source.voice()));
    }

    private boolean canCarryForwardPublishedProductNarrationAudio(Long productId, ProductNarrationPair sourcePair) {
        if (!hasReusableProductNarrationAudio(sourcePair.zh()) || !hasReusableProductNarrationAudio(sourcePair.en())) {
            return false;
        }
        return liveProductNarrationMatchesSource(productId, ShowroomNarrationLanguage.ZH, sourcePair.zh())
                && liveProductNarrationMatchesSource(productId, ShowroomNarrationLanguage.EN, sourcePair.en());
    }

    private boolean canCarryForwardProductNarrationAudio(Long productId, ProductNarrationPair sourcePair) {
        return canCarryForwardDraftProductNarrationAudio(sourcePair)
                || canCarryForwardPublishedProductNarrationAudio(productId, sourcePair);
    }

    private boolean canCarryForwardDraftProductNarrationAudio(ProductNarrationPair sourcePair) {
        return ShowroomNarrationStatus.DRAFT.equals(sourcePair.zh().status())
                && ShowroomNarrationStatus.DRAFT.equals(sourcePair.en().status())
                && !sourcePair.zh().generatedByAi()
                && !sourcePair.en().generatedByAi()
                && hasReusableProductNarrationAudio(sourcePair.zh())
                && hasReusableProductNarrationAudio(sourcePair.en());
    }

    private boolean liveProductNarrationMatchesSource(Long productId, ShowroomNarrationLanguage language,
                                                      ShowroomNarrationVersion source) {
        ShowroomNarrationKey key = new ShowroomNarrationKey(ShowroomNarrationTargetType.PRODUCT, productId,
                ShowroomNarrationAudienceType.PUBLIC, language);
        return narrationService.live(key)
                .map(live -> Objects.equals(live.scriptText(), source.scriptText())
                        && Objects.equals(live.audioFileId(), source.audioFileId())
                        && Objects.equals(live.audioDurationSeconds(), source.audioDurationSeconds()))
                .orElse(false);
    }

    private boolean hasReusableProductNarrationAudio(ShowroomNarrationVersion version) {
        return version.audioFileId() != null
                && version.audioDurationSeconds() != null
                && version.audioDurationSeconds() > 0;
    }

    private boolean shouldPublishProductNarration(ShowroomAdminController.ProductPublishReqVO req, Long productId) {
        if (req.sourceRevisionId() != null) {
            return true;
        }
        ShowroomNarrationKey zhKey = new ShowroomNarrationKey(ShowroomNarrationTargetType.PRODUCT, productId,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH);
        ShowroomNarrationKey enKey = new ShowroomNarrationKey(ShowroomNarrationTargetType.PRODUCT, productId,
                ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN);
        return narrationService.latest(zhKey).isPresent() || narrationService.latest(enKey).isPresent();
    }

    private void publishProductNarrationPair(ProductNarrationPair narrationPair) {
        ShowroomNarrationVersion publishedZh = narrationService.publishDirectly(narrationPair.zh().id());
        ShowroomNarrationVersion publishedEn = narrationService.publishDirectly(narrationPair.en().id());
        markReleaseDirtyForNarrationIfCurrent(publishedZh, null, "PRODUCT_PUBLIC_NARRATION_PUBLISHED");
        markReleaseDirtyForNarrationIfCurrent(publishedEn, null, "PRODUCT_PUBLIC_NARRATION_PUBLISHED");
    }

    private Long resolveRequestedProductRevisionId(Long productId, Long sourceRevisionId) {
        if (sourceRevisionId == null) {
            return contentService.getCurrentOrLatestProductRevision(productId).revisionId();
        }
        ShowroomProductRevision revision = contentService.getProductRevision(sourceRevisionId);
        if (!revision.productId().equals(productId)) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: product narration source revision mismatch");
        }
        return revision.revisionId();
    }

    private Long resolveProductNarrationSourceRevisionId(Long productId, Long sourceRevisionId) {
        if (sourceRevisionId == null) {
            return resolveLatestProductNarrationSourceRevisionId(productId);
        }
        ShowroomProductRevision revision = contentService.getProductRevision(sourceRevisionId);
        if (!revision.productId().equals(productId)) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: product narration source revision mismatch");
        }
        return revision.revisionId();
    }

    private Long resolveLatestProductNarrationSourceRevisionId(Long productId) {
        ShowroomNarrationVersion zhNarration = narrationService.latest(new ShowroomNarrationKey(
                        ShowroomNarrationTargetType.PRODUCT, productId,
                        ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH))
                .orElseThrow(() -> new IllegalStateException(
                        "SHOWROOM_TARGET_NOT_FOUND: latest ZH narration not found for product publish source"));
        ShowroomNarrationVersion enNarration = narrationService.latest(new ShowroomNarrationKey(
                        ShowroomNarrationTargetType.PRODUCT, productId,
                        ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN))
                .orElseThrow(() -> new IllegalStateException(
                        "SHOWROOM_TARGET_NOT_FOUND: latest EN narration not found for product publish source"));
        if (!Objects.equals(zhNarration.sourceRevisionId(), enNarration.sourceRevisionId())) {
            throw new IllegalStateException(
                    "SHOWROOM_TARGET_NOT_FOUND: latest product narration source revision mismatch");
        }
        return zhNarration.sourceRevisionId();
    }

    private ShowroomAdminController.ProductBatchGenerateFailureRespVO toBatchFailure(
            ShowroomAdminController.ProductPageRespVO row, RuntimeException exception) {
        return new ShowroomAdminController.ProductBatchGenerateFailureRespVO(row.productId(), row.productCode(),
                nullToEmpty(row.revision().nameCn()), batchFailureReason(exception));
    }

    private void executeProductTranslatePublishBatchTask(List<ShowroomAdminController.ProductPageRespVO> rows,
                                                         Long operatorUserId) {
        executeProductTranslatePublishBatchTask(rows, operatorUserId, null);
    }

    private void executeProductTranslatePublishBatchTask(List<ShowroomAdminController.ProductPageRespVO> rows,
                                                         Long operatorUserId, Long taskId) {
        if (!productTranslatePublishBatchTaskLock.tryLock()) {
            throw new IllegalStateException("SHOWROOM_TRANSLATION_BATCH_RUNNING: 一键翻译任务正在执行");
        }
        try {
            List<ShowroomAdminController.ProductBatchGenerateFailureRespVO> failures = new ArrayList<>();
            int succeededCount = 0;
            int failedCount = 0;
            for (ShowroomAdminController.ProductPageRespVO row : rows) {
                ProductTranslatePublishBatchTaskState before = productTranslatePublishBatchTaskState;
                productTranslatePublishBatchTaskState = new ProductTranslatePublishBatchTaskState(
                        true, true, before.criteria(), before.matchedCount(), succeededCount, failedCount,
                        rows.size() - succeededCount - failedCount, before.startedAt(), Instant.now().toEpochMilli(),
                        null, new ShowroomAdminController.ProductBatchTaskCurrentProductRespVO(
                        row.productId(), row.productCode(), nullToEmpty(row.revision().nameCn())),
                        before.lastFailure(), before.lastFailureAt(), List.copyOf(failures));
                persistProductTranslatePublishBatchTaskState(taskId, productTranslatePublishBatchTaskState);
                persistProductTranslatePublishBatchTaskItemRunning(taskId, row);
                try {
                    ShowroomProductRevision published = translateAndPublishSingleProduct(row, operatorUserId);
                    persistProductTranslatePublishBatchTaskItemCompleted(taskId, row, published.revisionId());
                    succeededCount++;
                } catch (RuntimeException exception) {
                    failedCount++;
                    ShowroomAdminController.ProductBatchGenerateFailureRespVO failure = toBatchFailure(row, exception);
                    failures.add(failure);
                    persistProductTranslatePublishBatchTaskItemFailed(taskId, row, failure.reason());
                    ProductTranslatePublishBatchTaskState failedState = productTranslatePublishBatchTaskState;
                    productTranslatePublishBatchTaskState = new ProductTranslatePublishBatchTaskState(
                            true, true, failedState.criteria(), failedState.matchedCount(), succeededCount,
                            failedCount, rows.size() - succeededCount - failedCount, failedState.startedAt(),
                            Instant.now().toEpochMilli(), null, failedState.currentProduct(), failure,
                            Instant.now().toEpochMilli(), List.copyOf(failures));
                    persistProductTranslatePublishBatchTaskState(taskId, productTranslatePublishBatchTaskState);
                }
            }
            ProductTranslatePublishBatchTaskState current = productTranslatePublishBatchTaskState;
            productTranslatePublishBatchTaskState = new ProductTranslatePublishBatchTaskState(
                    false, false, current.criteria(), current.matchedCount(), succeededCount, failedCount, 0,
                    current.startedAt(), Instant.now().toEpochMilli(), Instant.now().toEpochMilli(), null,
                    current.lastFailure(), current.lastFailureAt(), List.copyOf(failures));
            persistProductTranslatePublishBatchTaskState(taskId, productTranslatePublishBatchTaskState);
        } finally {
            productTranslatePublishBatchTaskLock.unlock();
        }
    }

    private void executeProductTranslatePublishBatchTaskInTenant(
            List<ShowroomAdminController.ProductPageRespVO> rows, Long operatorUserId, Long tenantId, Long taskId) {
        Long oldTenantId = TenantContextHolder.getTenantId();
        Boolean oldIgnore = TenantContextHolder.isIgnore();
        try {
            TenantContextHolder.setTenantId(tenantId);
            TenantContextHolder.setIgnore(false);
            executeProductTranslatePublishBatchTask(rows, operatorUserId, taskId);
        } finally {
            TenantContextHolder.setTenantId(oldTenantId);
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }

    private ShowroomProductRevision translateAndPublishSingleProduct(ShowroomAdminController.ProductPageRespVO row,
                                                                     Long operatorUserId) {
        ShowroomAdminController.ProductDetailRespVO revision = row.revision();
        ShowroomAdminController.ProductFieldTranslateRespVO translation = translateProductFieldsToEn(
                new ShowroomAdminController.ProductFieldTranslateReqVO(row.productId(), revision.nameCn(),
                        revision.fields(), latestProductNarrationText(row.productId(), ShowroomNarrationLanguage.ZH)));
        LinkedHashMap<String, String> translatedFields = new LinkedHashMap<>(revision.fields());
        translatedFields.putAll(translation.translatedFields());
        ShowroomProductRevision published = publishTranslatedProductTextOnly(row.productId(), row.productMasterId(),
                row.productCode(), revision.nameCn(), translation.nameEn(), translatedFields,
                toProductAttachmentReqVOs(revision.attachments()), operatorUserId);
        String zhNarrationText = latestProductNarrationText(row.productId(), ShowroomNarrationLanguage.ZH);
        if (hasText(translation.narrationScriptEn())) {
            saveTranslatedProductNarration(row.productId(), published.revisionId(), zhNarrationText,
                    translation.narrationScriptEn());
        }
        return published;
    }

    private ShowroomProductRevision publishTranslatedProductTextOnly(Long productId, Long productMasterId,
                                                                     String productCode, String nameCn, String nameEn,
                                                                     Map<String, String> fields,
                                                                     List<ShowroomAdminController.ProductAttachmentReqVO> attachments,
                                                                     Long operatorUserId) {
        validateProductPublishCoreFields(new ShowroomAdminController.ProductPublishReqVO(productId, productMasterId,
                productCode, nameCn, nameEn, fields, null, null, false, attachments));
        ShowroomProductRevision savedDraft = saveProductDraft(new ShowroomAdminController.ProductDraftReqVO(
                productId, productMasterId, productCode, nameCn, nameEn, fields, attachments));
        ShowroomProductRevision published = contentService.publishProductRevision(savedDraft.revisionId(),
                operatorUserId);
        versionBundleService.ensureBundleForPublishedRevision(TARGET_PRODUCT, published.productId(),
                published.revisionId(), operatorUserId, null);
        assignmentService.markWholeProductAssignmentDirectPublished(published.productId(), operatorUserId,
                published.revisionId());
        return published;
    }

    private String latestProductNarrationText(Long productId, ShowroomNarrationLanguage language) {
        return narrationService.latest(new ShowroomNarrationKey(ShowroomNarrationTargetType.PRODUCT, productId,
                        ShowroomNarrationAudienceType.PUBLIC, language))
                .map(ShowroomNarrationVersion::scriptText)
                .orElse("");
    }

    private void saveTranslatedProductNarration(Long productId, Long sourceRevisionId, String zhScriptText,
                                                String enScriptText) {
        if (!hasText(zhScriptText) || !hasText(enScriptText)) {
            return;
        }
        narrationService.draftScript(new ShowroomNarrationDraftCommand(ShowroomNarrationTargetType.PRODUCT, productId,
                sourceRevisionId, ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.ZH,
                zhScriptText, false));
        narrationService.draftScript(new ShowroomNarrationDraftCommand(ShowroomNarrationTargetType.PRODUCT, productId,
                sourceRevisionId, ShowroomNarrationAudienceType.PUBLIC, ShowroomNarrationLanguage.EN,
                enScriptText, true));
    }

    private static List<ShowroomAdminController.ProductAttachmentReqVO> toProductAttachmentReqVOs(
            List<ShowroomAdminController.ProductAttachmentRespVO> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        return attachments.stream()
                .map(attachment -> new ShowroomAdminController.ProductAttachmentReqVO(attachment.assetType(),
                        attachment.fileId(), attachment.originalName(), attachment.mimeType(), attachment.size(),
                        attachment.displayOrder()))
                .toList();
    }

    private ShowroomAdminController.ProductTranslatePublishBatchTaskRespVO toProductTranslatePublishBatchTaskResp(
            ProductTranslatePublishBatchTaskState state) {
        return new ShowroomAdminController.ProductTranslatePublishBatchTaskRespVO(
                state.active(), state.running(), nullToEmpty(state.criteria().keyword()),
                nullToEmpty(state.criteria().lifecycleStage()), nullToEmpty(state.criteria().incompleteStatus()),
                nullToEmpty(state.criteria().approvalStatus()), state.matchedCount(), state.succeededCount(),
                state.failedCount(), state.remainingCount(), state.startedAt(), state.lastRunAt(),
                state.completedAt(), state.currentProduct(), state.lastFailure(), state.lastFailureAt(),
                state.failures());
    }

    private Long persistProductTranslatePublishBatchTask(ProductTranslatePublishBatchTaskState state,
                                                         Long operatorUserId,
                                                         List<ShowroomAdminController.ProductPageRespVO> rows) {
        if (translatePublishBatchTaskMapper == null || translatePublishBatchTaskItemMapper == null) {
            return null;
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ShowroomProductTranslatePublishBatchTaskDO task = ShowroomProductTranslatePublishBatchTaskDO.builder()
                .operatorUserId(operatorUserId)
                .status(state.active() ? "WAITING" : "COMPLETED")
                .keyword(normalizeText(state.criteria().keyword()))
                .lifecycleStage(normalizeText(state.criteria().lifecycleStage()))
                .incompleteStatus(normalizeText(state.criteria().incompleteStatus()))
                .approvalStatus(normalizeText(state.criteria().approvalStatus()))
                .matchedCount(state.matchedCount())
                .succeededCount(state.succeededCount())
                .failedCount(state.failedCount())
                .remainingCount(state.remainingCount())
                .completedAt(state.completedAt() == null ? null : LocalDateTime.now())
                .build();
        task.setTenantId(tenantId);
        translatePublishBatchTaskMapper.insert(task);
        for (ShowroomAdminController.ProductPageRespVO row : rows) {
            ShowroomProductTranslatePublishBatchTaskItemDO item =
                    ShowroomProductTranslatePublishBatchTaskItemDO.builder()
                            .taskId(task.getId())
                            .productId(row.productId())
                            .sourceRevisionId(row.revision().revisionId())
                            .productCode(row.productCode())
                            .nameCn(row.revision().nameCn())
                            .nameEn(row.revision().nameEn())
                            .status("WAITING")
                            .attemptCount(0)
                            .build();
            item.setTenantId(tenantId);
            translatePublishBatchTaskItemMapper.insert(item);
        }
        return task.getId();
    }

    private ProductTranslatePublishBatchTaskState loadPersistedProductTranslatePublishBatchTaskState() {
        if (translatePublishBatchTaskMapper == null || translatePublishBatchTaskItemMapper == null) {
            return null;
        }
        ShowroomProductTranslatePublishBatchTaskDO task = translatePublishBatchTaskMapper.selectActiveTask();
        if (task != null) {
            task = recoverStaleProductTranslatePublishBatchTask(task);
        }
        if (task == null) {
            task = translatePublishBatchTaskMapper.selectLatestTask();
        }
        if (task == null) {
            return null;
        }
        List<ShowroomAdminController.ProductBatchGenerateFailureRespVO> failures =
                translatePublishBatchTaskItemMapper.selectListByTaskId(task.getId()).stream()
                        .filter(item -> "FAILED".equals(item.getStatus()) || hasText(item.getLastError()))
                        .map(item -> new ShowroomAdminController.ProductBatchGenerateFailureRespVO(
                                item.getProductId(), nullToEmpty(item.getProductCode()),
                                nullToEmpty(item.getNameCn()), nullToEmpty(item.getLastError())))
                        .toList();
        ShowroomAdminController.ProductBatchGenerateFailureRespVO lastFailure = failures.isEmpty()
                ? null : failures.get(failures.size() - 1);
        ShowroomAdminController.ProductBatchTaskCurrentProductRespVO currentProduct =
                task.getCurrentProductId() == null ? null
                        : new ShowroomAdminController.ProductBatchTaskCurrentProductRespVO(
                        task.getCurrentProductId(), nullToEmpty(task.getCurrentProductCode()),
                        nullToEmpty(task.getCurrentProductNameCn()));
        boolean active = "WAITING".equals(task.getStatus()) || "RUNNING".equals(task.getStatus());
        boolean running = "RUNNING".equals(task.getStatus());
        return new ProductTranslatePublishBatchTaskState(active, running,
                new ProductTranslatePublishBatchTaskCriteria(nullToEmpty(task.getKeyword()),
                        nullToEmpty(task.getLifecycleStage()), nullToEmpty(task.getIncompleteStatus()),
                        nullToEmpty(task.getApprovalStatus())),
                nullSafeInt(task.getMatchedCount()), nullSafeInt(task.getSucceededCount()),
                nullSafeInt(task.getFailedCount()), nullSafeInt(task.getRemainingCount()),
                toEpochMilli(task.getCreateTime()), toEpochMilli(task.getLastRunAt()),
                toEpochMilli(task.getCompletedAt()), currentProduct, lastFailure,
                lastFailure == null ? null : toEpochMilli(task.getLastRunAt()), failures);
    }

    private void persistProductTranslatePublishBatchTaskState(Long taskId,
                                                              ProductTranslatePublishBatchTaskState state) {
        if (taskId == null || translatePublishBatchTaskMapper == null) {
            return;
        }
        ShowroomProductTranslatePublishBatchTaskDO task = translatePublishBatchTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(state.running() ? "RUNNING" : state.active() ? "WAITING" : "COMPLETED");
        task.setSucceededCount(state.succeededCount());
        task.setFailedCount(state.failedCount());
        task.setRemainingCount(state.remainingCount());
        task.setCurrentProductId(state.currentProduct() == null ? null : state.currentProduct().productId());
        task.setCurrentProductCode(state.currentProduct() == null ? null : state.currentProduct().productCode());
        task.setCurrentProductNameCn(state.currentProduct() == null ? null : state.currentProduct().nameCn());
        task.setLastRunAt(state.lastRunAt() == null ? null : LocalDateTime.now());
        task.setCompletedAt(state.completedAt() == null ? null : LocalDateTime.now());
        task.setLastFailureMessage(state.lastFailure() == null ? null : state.lastFailure().reason());
        translatePublishBatchTaskMapper.updateById(task);
    }

    private ShowroomProductTranslatePublishBatchTaskDO recoverStaleProductTranslatePublishBatchTask(
            ShowroomProductTranslatePublishBatchTaskDO task) {
        if (!isProductTranslatePublishTaskStale(task)) {
            return task;
        }
        String reason = "SHOWROOM_TRANSLATION_BATCH_STALE: 一键翻译任务超过 Codex CLI 超时安全窗口未推进，已自动失败收尾";
        completeProductTranslatePublishBatchTaskAfterFailure(task.getId(), task.getTenantId(),
                new IllegalStateException(reason));
        return translatePublishBatchTaskMapper.selectById(task.getId());
    }

    private boolean isProductTranslatePublishTaskStale(ShowroomProductTranslatePublishBatchTaskDO task) {
        if (task == null || !"RUNNING".equals(task.getStatus()) || task.getLastRunAt() == null) {
            return false;
        }
        return Duration.between(task.getLastRunAt(), LocalDateTime.now()).toMillis()
                > productTranslatePublishBatchStaleTimeoutMs();
    }

    private long productTranslatePublishBatchStaleTimeoutMs() {
        YudaoAiProperties.CodexCli codexCli = yudaoAiProperties == null ? null : yudaoAiProperties.getCodexCli();
        long timeoutMs = codexCli == null || codexCli.getTimeoutMs() == null
                ? 240000L : codexCli.getTimeoutMs();
        return Math.max(timeoutMs + PRODUCT_TRANSLATE_PUBLISH_BATCH_STALE_GRACE_MS,
                PRODUCT_TRANSLATE_PUBLISH_BATCH_STALE_GRACE_MS);
    }

    private void completeProductTranslatePublishBatchTaskAfterFailure(Long taskId, Long tenantId, Throwable throwable) {
        if (taskId == null || translatePublishBatchTaskMapper == null || translatePublishBatchTaskItemMapper == null) {
            return;
        }
        Long oldTenantId = TenantContextHolder.getTenantId();
        Boolean oldIgnore = TenantContextHolder.isIgnore();
        try {
            TenantContextHolder.setTenantId(tenantId);
            TenantContextHolder.setIgnore(false);
            ShowroomProductTranslatePublishBatchTaskDO task = translatePublishBatchTaskMapper.selectById(taskId);
            if (task == null || !("WAITING".equals(task.getStatus()) || "RUNNING".equals(task.getStatus()))) {
                return;
            }
            String reason = batchFailureReason(throwable);
            log.error("Showroom product translate publish batch task {} failed unexpectedly", taskId, throwable);
            ShowroomProductTranslatePublishBatchTaskItemDO runningItem =
                    translatePublishBatchTaskItemMapper.selectRunningByTaskId(taskId);
            if (runningItem != null) {
                runningItem.setStatus("FAILED");
                runningItem.setLastError(reason);
                if (runningItem.getLastAttemptAt() == null) {
                    runningItem.setLastAttemptAt(LocalDateTime.now());
                }
                runningItem.setCompletedAt(LocalDateTime.now());
                translatePublishBatchTaskItemMapper.updateById(runningItem);
            }
            List<ShowroomProductTranslatePublishBatchTaskItemDO> items =
                    translatePublishBatchTaskItemMapper.selectListByTaskId(taskId);
            int succeededCount = (int) items.stream().filter(item -> "COMPLETED".equals(item.getStatus())).count();
            int failedCount = (int) items.stream().filter(item -> "FAILED".equals(item.getStatus())).count();
            int remainingCount = Math.max(0, nullSafeInt(task.getMatchedCount()) - succeededCount - failedCount);
            task.setStatus("COMPLETED");
            task.setSucceededCount(succeededCount);
            task.setFailedCount(failedCount);
            task.setRemainingCount(remainingCount);
            task.setCurrentProductId(null);
            task.setCurrentProductCode(null);
            task.setCurrentProductNameCn(null);
            if (task.getLastRunAt() == null) {
                task.setLastRunAt(LocalDateTime.now());
            }
            task.setCompletedAt(LocalDateTime.now());
            task.setLastFailureMessage(reason);
            translatePublishBatchTaskMapper.updateById(task);
            productTranslatePublishBatchTaskState = new ProductTranslatePublishBatchTaskState(
                    false, false,
                    new ProductTranslatePublishBatchTaskCriteria(nullToEmpty(task.getKeyword()),
                            nullToEmpty(task.getLifecycleStage()), nullToEmpty(task.getIncompleteStatus()),
                            nullToEmpty(task.getApprovalStatus())),
                    nullSafeInt(task.getMatchedCount()), succeededCount, failedCount, remainingCount,
                    toEpochMilli(task.getCreateTime()), toEpochMilli(task.getLastRunAt()),
                    toEpochMilli(task.getCompletedAt()), null,
                    new ShowroomAdminController.ProductBatchGenerateFailureRespVO(
                            runningItem == null ? task.getCurrentProductId() : runningItem.getProductId(),
                            runningItem == null ? nullToEmpty(task.getCurrentProductCode())
                                    : nullToEmpty(runningItem.getProductCode()),
                            runningItem == null ? nullToEmpty(task.getCurrentProductNameCn())
                                    : nullToEmpty(runningItem.getNameCn()),
                            reason),
                    toEpochMilli(task.getCompletedAt()),
                    items.stream()
                            .filter(item -> "FAILED".equals(item.getStatus()) || hasText(item.getLastError()))
                            .map(item -> new ShowroomAdminController.ProductBatchGenerateFailureRespVO(
                                    item.getProductId(), nullToEmpty(item.getProductCode()),
                                    nullToEmpty(item.getNameCn()), nullToEmpty(item.getLastError())))
                            .toList());
        } finally {
            TenantContextHolder.setTenantId(oldTenantId);
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }

    private void persistProductTranslatePublishBatchTaskItemRunning(Long taskId,
            ShowroomAdminController.ProductPageRespVO row) {
        updateProductTranslatePublishBatchTaskItem(taskId, row, "RUNNING", null, null);
    }

    private void persistProductTranslatePublishBatchTaskItemCompleted(Long taskId,
            ShowroomAdminController.ProductPageRespVO row, Long publishedRevisionId) {
        updateProductTranslatePublishBatchTaskItem(taskId, row, "COMPLETED", null, publishedRevisionId);
    }

    private void persistProductTranslatePublishBatchTaskItemFailed(Long taskId,
            ShowroomAdminController.ProductPageRespVO row, String reason) {
        updateProductTranslatePublishBatchTaskItem(taskId, row, "FAILED", reason, null);
    }

    private void updateProductTranslatePublishBatchTaskItem(Long taskId,
            ShowroomAdminController.ProductPageRespVO row, String status, String lastError, Long publishedRevisionId) {
        if (taskId == null || translatePublishBatchTaskItemMapper == null) {
            return;
        }
        ShowroomProductTranslatePublishBatchTaskItemDO item =
                translatePublishBatchTaskItemMapper.selectByTaskIdAndProductId(taskId, row.productId());
        if (item == null) {
            return;
        }
        item.setStatus(status);
        if ("RUNNING".equals(status)) {
            item.setAttemptCount(nullSafeInt(item.getAttemptCount()) + 1);
        }
        item.setLastError(lastError);
        item.setPublishedRevisionId(publishedRevisionId);
        item.setLastAttemptAt(LocalDateTime.now());
        if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
            item.setCompletedAt(LocalDateTime.now());
        }
        translatePublishBatchTaskItemMapper.updateById(item);
    }

    private static ProductTranslatePublishBatchTaskState emptyProductTranslatePublishBatchTaskState() {
        return new ProductTranslatePublishBatchTaskState(false, false,
                new ProductTranslatePublishBatchTaskCriteria("", "", "", ""), 0, 0, 0, 0,
                null, null, null, null, null, null, List.of());
    }

    private ShowroomAdminController.ProductPublishReqVO toBatchPublishReq(
            ShowroomAdminController.ProductPageRespVO row) {
        ShowroomAdminController.ProductDetailRespVO revision = row.revision();
        return new ShowroomAdminController.ProductPublishReqVO(
                row.productId(),
                row.productMasterId(),
                row.productCode(),
                revision.nameCn(),
                revision.nameEn(),
                new LinkedHashMap<>(revision.fields()),
                null,
                null,
                false
        );
    }

    private static String batchFailureReason(RuntimeException exception) {
        return batchFailureReason((Throwable) exception);
    }

    private static String batchFailureReason(Throwable exception) {
        if (exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
            return exception.getMessage().trim();
        }
        Throwable cause = exception.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().trim().isEmpty()) {
            return cause.getMessage().trim();
        }
        return exception.getClass().getSimpleName();
    }

    private void validateCompanyNarrationPublishRequest(ShowroomNarrationVersion zhNarration,
                                                        ShowroomNarrationVersion enNarration) {
        if (zhNarration != null) {
            validateSingleCompanyNarrationVersion(zhNarration, ShowroomNarrationLanguage.ZH);
        }
        if (enNarration != null) {
            validateSingleCompanyNarrationVersion(enNarration, ShowroomNarrationLanguage.EN);
        }
        if (zhNarration != null && enNarration != null && !zhNarration.key().targetId().equals(enNarration.key().targetId())) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: company narration target mismatch");
        }
    }

    private void carryForwardLiveCompanyNarrationsIfPresent(Long companyId, Long nextRevisionId) {
        for (ShowroomNarrationLanguage language : ShowroomNarrationLanguage.values()) {
            narrationService.live(new ShowroomNarrationKey(ShowroomNarrationTargetType.COMPANY, companyId,
                            ShowroomNarrationAudienceType.PUBLIC, language))
                    .ifPresent(version -> publishCarriedCompanyNarration(companyId, nextRevisionId, language, version));
        }
    }

    private void carryForwardCompanyNarrationsForRevisionIfPresent(Long companyId, Long sourceRevisionId, Long nextRevisionId) {
        for (ShowroomNarrationLanguage language : ShowroomNarrationLanguage.values()) {
            narrationService.latestPublished(new ShowroomNarrationKey(ShowroomNarrationTargetType.COMPANY, companyId,
                            ShowroomNarrationAudienceType.PUBLIC, language), sourceRevisionId)
                    .ifPresent(version -> publishCarriedCompanyNarration(companyId, nextRevisionId, language, version));
        }
    }

    private void publishCarriedCompanyNarration(Long companyId, Long nextRevisionId,
                                                ShowroomNarrationLanguage language,
                                                ShowroomNarrationVersion liveVersion) {
        String scriptText = requireText(liveVersion.scriptText(),
                "SHOWROOM_SCRIPT_MISSING: live company " + language.name() + " narration text is required");
        Long audioFileId = requireId(liveVersion.audioFileId(),
                "SHOWROOM_AUDIO_GENERATION_FAILED: live company " + language.name() + " narration audio is required");
        Integer audioDurationSeconds = liveVersion.audioDurationSeconds();
        if (audioDurationSeconds == null || audioDurationSeconds <= 0) {
            throw new IllegalStateException("SHOWROOM_AUDIO_GENERATION_FAILED: live company "
                    + language.name() + " narration audio duration is required");
        }
        ShowroomNarrationVersion carriedDraft = narrationService.draftScript(new ShowroomNarrationDraftCommand(
                ShowroomNarrationTargetType.COMPANY, companyId, nextRevisionId,
                ShowroomNarrationAudienceType.PUBLIC, language, scriptText, liveVersion.generatedByAi()));
        narrationService.attachAudio(new ShowroomNarrationAudioDraftCommand(
                carriedDraft.id(), audioFileId, audioDurationSeconds, liveVersion.voice()));
        ShowroomNarrationVersion published = narrationService.publishDirectly(carriedDraft.id());
        markReleaseDirtyForNarrationIfCurrent(published, null, "COMPANY_PUBLIC_NARRATION_PUBLISHED");
    }

    private void validateSingleCompanyNarrationVersion(ShowroomNarrationVersion narration,
                                                       ShowroomNarrationLanguage expectedLanguage) {
        if (narration.key().targetType() != ShowroomNarrationTargetType.COMPANY) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: company narration versions are required");
        }
        if (narration.key().audienceType() != ShowroomNarrationAudienceType.PUBLIC) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: company narration audience mismatch");
        }
        if (narration.key().language() != expectedLanguage) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: company narration language mismatch");
        }
        if (narration.audioFileId() == null) {
            throw new IllegalStateException("SHOWROOM_AUDIO_GENERATION_FAILED: company narration audio is required");
        }
    }

    private ShowroomAdminController.CompanyNarrationVersionRespVO toCompanyNarrationVersionResp(
            ShowroomNarrationVersion narration) {
        return new ShowroomAdminController.CompanyNarrationVersionRespVO(narration.id(),
                narration.key().language().name(), narration.scriptText(), narration.audioFileId(),
                narration.audioDurationSeconds(), fileUrl(narration.audioFileId()), nullToEmpty(narration.voice()));
    }

    private ShowroomAdminController.NarrationVersionRespVO toNarrationVersionResp(ShowroomNarrationVersion narration) {
        String audioUrl = narration.audioFileId() == null ? null : fileUrl(narration.audioFileId());
        return new ShowroomAdminController.NarrationVersionRespVO(
                narration.id(),
                narration.key(),
                narration.sourceRevisionId(),
                narration.versionNo(),
                narration.scriptText(),
                narration.audioFileId(),
                audioUrl,
                narration.audioDurationSeconds(),
                narration.voice(),
                narration.generationStatus().name(),
                narration.status().name(),
                narration.generatedByAi(),
                narration.generatedAt(),
                narration.publishedAt(),
                narration.live()
        );
    }

    private ShowroomAdminController.CompanyCurrentRespVO toCompanyCurrentResp(ShowroomCompanySnapshot snapshot,
                                                                              ShowroomCompanyRevision revision) {
        boolean live = snapshot.currentRevisionId().map(revision.revisionId()::equals).orElse(false);
        return new ShowroomAdminController.CompanyCurrentRespVO(revision.companyId(), revision.revisionId(),
                revision.revisionNo(), revision.status(), revision.fields(), snapshot.companyType(),
                snapshot.displayName(), snapshot.displayNameEn(), live);
    }

    private ShowroomAdminController.ImagePromptCurrentRespVO toImagePromptCurrentResp(
            ShowroomImagePromptVersion version) {
        return new ShowroomAdminController.ImagePromptCurrentRespVO(
                version.id(),
                version.sceneCode(),
                version.versionNo(),
                version.templateText(),
                nullToEmpty(version.changeNote()),
                version.placeholderCodes(),
                Objects.requireNonNullElse(version.useCount(), 0),
                toEpochMilli(version.createTime()),
                nullToEmpty(version.creator()),
                toEpochMilli(version.lastUsedAt())
        );
    }

    private ShowroomAdminController.ImagePromptHistoryItemRespVO toImagePromptHistoryItemResp(
            ShowroomImagePromptVersion version, boolean current) {
        return new ShowroomAdminController.ImagePromptHistoryItemRespVO(
                version.id(),
                version.sceneCode(),
                version.versionNo(),
                version.templateText(),
                nullToEmpty(version.changeNote()),
                version.placeholderCodes(),
                Objects.requireNonNullElse(version.useCount(), 0),
                toEpochMilli(version.createTime()),
                nullToEmpty(version.creator()),
                toEpochMilli(version.lastUsedAt()),
                current
        );
    }

    private List<ShowroomAdminController.NarrationAvailabilityRespVO> narrationAvailabilities(
            ShowroomNarrationTargetType targetType, Long targetId) {
        List<ShowroomAdminController.NarrationAvailabilityRespVO> narrations = new ArrayList<>();
        for (ShowroomNarrationLanguage language : ShowroomNarrationLanguage.values()) {
            narrationService.live(new ShowroomNarrationKey(targetType, targetId,
                            ShowroomNarrationAudienceType.PUBLIC, language))
                    .ifPresent(version -> narrations.add(new ShowroomAdminController.NarrationAvailabilityRespVO(
                            version.id(), version.key().language().name(), version.key().audienceType().name(),
                            version.status().name(), version.live(), version.audioFileId() != null)));
        }
        return List.copyOf(narrations);
    }

    private List<Long> relatedProductIds(Long productRevisionId) {
        return productRevisionRelationMapper.selectListByProductRevisionId(productRevisionId).stream()
                .map(relation -> relation.getRelatedProductId())
                .toList();
    }

    private String resolveProductStatus(Long productId, ShowroomProductRevision revision) {
        var activeAssignment = assignmentService.getLatestOpenWholeProductAssignment(productId);
        if (activeAssignment != null && STATUS_OPEN.equals(activeAssignment.status())) {
            return STATUS_IN_FILLING;
        }
        ShowroomChangeRequestDO changeRequest = latestChangeRequest(TARGET_PRODUCT, productId);
        if (changeRequest != null && revision.revisionId().equals(changeRequest.getTargetRevisionId())) {
            return normalizeApprovalStatus(changeRequest.getStatus());
        }
        return normalizeApprovalStatus(revision.status());
    }

    private static String resolveProductCoverGenerationMode(String rawMode) {
        if (!hasText(rawMode)) {
            return PRODUCT_COVER_GENERATION_MODE_ALL;
        }
        String normalized = rawMode.trim().toUpperCase();
        if (PRODUCT_COVER_GENERATION_MODE_ALL.equals(normalized)
                || PRODUCT_COVER_GENERATION_MODE_MISSING_ONLY.equals(normalized)) {
            return normalized;
        }
        throw new IllegalStateException(
                "SHOWROOM_COVER_GENERATION_FAILED: unsupported product cover generation mode: " + rawMode);
    }

    private ShowroomAdminController.ProductAssignmentRespVO activeProductAssignment(Long productId) {
        var assignment = assignmentService.getLatestOpenWholeProductAssignment(productId);
        if (assignment == null) {
            return null;
        }
        return new ShowroomAdminController.ProductAssignmentRespVO(assignment.assignmentId(),
                assignment.assigneeUserId(), assignment.status());
    }

    private boolean isApprovedProductStatus(String status) {
        return STATUS_APPROVED.equals(status) || STATUS_PUBLISHED.equals(status);
    }

    private boolean isDirectPublishableProductStatus(String status) {
        return STATUS_DRAFT.equals(status) || STATUS_REJECTED.equals(status);
    }

    private ShowroomChangeRequestDO latestChangeRequest(String targetType, Long targetId) {
        List<ShowroomChangeRequestDO> requests = changeRequestMapper.selectListByTarget(targetType, targetId);
        return requests.isEmpty() ? null : requests.get(0);
    }

    private boolean matchesProduct(ShowroomAdminController.ProductPageRespVO row,
                                   ShowroomAdminController.PageQueryReqVO req) {
        ShowroomAdminController.ProductDetailRespVO displayRevision = row.displayRevision();
        if (req.productId() != null && !req.productId().equals(row.productId())) {
            return false;
        }
        String keyword = normalizeKeyword(req.keyword());
        if (hasText(keyword) && !containsIgnoreCase(row.productCode(), keyword)
                && !containsIgnoreCase(row.legacyProductCode(), keyword)
                && !containsIgnoreCase(displayRevision.nameCn(), keyword)
                && !containsIgnoreCase(displayRevision.nameEn(), keyword)) {
            return false;
        }
        Map<String, String> fields = displayRevision.fields();
        if (hasText(req.ownerCompanyId()) && !req.ownerCompanyId().trim().equals(fields.get("owner_company_id"))) {
            return false;
        }
        if (hasText(req.ownerType()) && !req.ownerType().trim().equalsIgnoreCase(fields.get("product_owner_type"))) {
            return false;
        }
        if (hasText(req.lifecycleStage())
                && !req.lifecycleStage().trim().equalsIgnoreCase(fields.get("lifecycle_stage"))) {
            return false;
        }
        if ("COMPLETE".equalsIgnoreCase(req.incompleteStatus()) && row.incomplete()) {
            return false;
        }
        if ("INCOMPLETE".equalsIgnoreCase(req.incompleteStatus()) && !row.incomplete()) {
            return false;
        }
        return !hasText(req.approvalStatus())
                || req.approvalStatus().trim().equalsIgnoreCase(row.revision().status());
    }

    private boolean matchesProductCandidate(ProductPageCandidate candidate,
                                            ShowroomAdminController.PageQueryReqVO req) {
        ShowroomProductSnapshot snapshot = candidate.snapshot();
        ShowroomProductRevision displayRevision = candidate.displayRevision();
        if (req.productId() != null && !req.productId().equals(snapshot.productId())) {
            return false;
        }
        String keyword = normalizeKeyword(req.keyword());
        if (hasText(keyword) && !containsIgnoreCase(snapshot.productCode(), keyword)
                && !containsIgnoreCase(snapshot.legacyProductCode(), keyword)
                && !containsIgnoreCase(displayRevision.nameCn(), keyword)
                && !containsIgnoreCase(displayRevision.nameEn(), keyword)) {
            return false;
        }
        Map<String, String> fields = displayRevision.fields();
        if (hasText(req.ownerCompanyId()) && !req.ownerCompanyId().trim().equals(fields.get("owner_company_id"))) {
            return false;
        }
        if (hasText(req.ownerType()) && !req.ownerType().trim().equalsIgnoreCase(fields.get("product_owner_type"))) {
            return false;
        }
        if (hasText(req.lifecycleStage())
                && !req.lifecycleStage().trim().equalsIgnoreCase(fields.get("lifecycle_stage"))) {
            return false;
        }
        if ("COMPLETE".equalsIgnoreCase(req.incompleteStatus()) && displayRevision.incomplete()) {
            return false;
        }
        if ("INCOMPLETE".equalsIgnoreCase(req.incompleteStatus()) && !displayRevision.incomplete()) {
            return false;
        }
        return !hasText(req.approvalStatus())
                || req.approvalStatus().trim().equalsIgnoreCase(resolveProductStatus(snapshot.productId(),
                candidate.latestRevision()));
    }

    private ShowroomProductRevision resolveTargetProductRevision(Long productId, Long revisionId,
                                                                 ShowroomProductRevision latestRevision) {
        if (revisionId == null) {
            return latestRevision;
        }
        ShowroomProductRevision targetRevision = contentService.getProductRevision(revisionId);
        if (!productId.equals(targetRevision.productId())) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: product revision does not belong to product");
        }
        return targetRevision;
    }

    private ShowroomProductRevision resolveDisplayProductRevision(ShowroomProductSnapshot snapshot,
                                                                  ShowroomProductRevision latestRevision) {
        return snapshot.currentRevisionId()
                .map(contentService::getProductRevision)
                .orElse(latestRevision);
    }

    private ShowroomProductRevision resolveDisplayProductRevision(ShowroomProductSnapshot snapshot,
                                                                  ShowroomProductRevision latestRevision,
                                                                  Map<Long, ShowroomProductRevision> revisionsById) {
        return snapshot.currentRevisionId()
                .map(revisionId -> {
                    ShowroomProductRevision revision = revisionsById.get(revisionId);
                    if (revision == null) {
                        throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: product revision not found");
                    }
                    return revision;
                })
                .orElse(latestRevision);
    }

    private static boolean matchesHall(ShowroomHall hall, String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        return !hasText(normalizedKeyword)
                || containsIgnoreCase(hall.hallCode(), normalizedKeyword)
                || containsIgnoreCase(hall.name(), normalizedKeyword)
                || containsIgnoreCase(hall.description(), normalizedKeyword);
    }

    private ShowroomDisplayController.CompanySummary companySummary(ShowroomCompanyRevision company) {
        ShowroomCompanySnapshot snapshot = contentService.getCompany(company.companyId());
        return new ShowroomDisplayController.CompanySummary(company.companyId(), snapshot.displayName(),
                firstNonBlank(company.fields().get("park_introduction"), company.fields().get("development_history")));
    }

    private ShowroomDisplayController.DisplayCard productCard(Long productId) {
        ShowroomProductSnapshot snapshot = contentService.getProduct(productId);
        ShowroomProductRevision revision = contentService.getCurrentOrLatestProductRevision(productId);
        String previewImageUrl = resolveProductDisplayImageUrl(productId, revision);
        String nameCn = hasText(revision.nameCn()) ? revision.nameCn() : snapshot.productCode();
        return new ShowroomDisplayController.DisplayCard(productId, nameCn, nullToEmpty(revision.nameEn()),
                revision.incomplete(), previewImageUrl);
    }

    private String resolveProductDisplayImageUrl(Long productId, ShowroomProductRevision revision) {
        return requireText(revision.fields().get("cover_image"),
                "SHOWROOM_REQUIRED_FIELD_MISSING: product cover_image is required for PRODUCT:" + productId);
    }

    private List<ShowroomDisplayController.PublicField> companyFields(Map<String, String> fields) {
        List<ShowroomDisplayController.PublicField> publicFields = new ArrayList<>();
        for (String fieldCode : COMPANY_WEBSITE_FIELD_ORDER) {
            String value = fields.get(fieldCode);
            if (hasText(value)) {
                publicFields.add(new ShowroomDisplayController.PublicField(companyFieldLabel(fieldCode), value));
            }
        }
        return List.copyOf(publicFields);
    }

    private List<ShowroomDisplayController.BilingualPublicField> companyBilingualFields(Map<String, String> fields) {
        List<ShowroomDisplayController.BilingualPublicField> bilingualFields = new ArrayList<>();
        for (String fieldCode : COMPANY_WEBSITE_FIELD_ORDER) {
            String valueZh = nullToEmpty(fields.get(fieldCode));
            String valueEn = nullToEmpty(fields.get(companyEnglishFieldKey(fieldCode)));
            if (hasText(valueZh) || hasText(valueEn)) {
                bilingualFields.add(new ShowroomDisplayController.BilingualPublicField(
                        fieldCode,
                        companyFieldLabel(fieldCode),
                        companyFieldLabelEn(fieldCode),
                        valueZh,
                        valueEn
                ));
            }
        }
        return List.copyOf(bilingualFields);
    }

    private List<ShowroomDisplayController.PublicField> productFields(ShowroomProductRevision revision) {
        List<ShowroomDisplayController.PublicField> fields = new ArrayList<>();
        if (hasText(revision.nameCn())) {
            fields.add(new ShowroomDisplayController.PublicField(productFieldLabel("name_cn"), revision.nameCn()));
        }
        if (hasText(revision.nameEn())) {
            fields.add(new ShowroomDisplayController.PublicField(productFieldLabel("name_en"), revision.nameEn()));
        }
        for (String fieldCode : PRODUCT_FIELD_ORDER) {
            String value = revision.fields().get(fieldCode);
            if (hasText(value) && ShowroomFieldCatalog.productField(fieldCode).tier() == ShowroomFieldTierEnum.BASIC) {
                fields.add(new ShowroomDisplayController.PublicField(productFieldLabel(fieldCode),
                        formatProductFieldValue(fieldCode, value)));
            }
        }
        return List.copyOf(fields);
    }

    private List<ShowroomDisplayController.BilingualPublicField> productBilingualFields(
            ShowroomProductRevision revision) {
        List<ShowroomDisplayController.BilingualPublicField> fields = new ArrayList<>();
        if (hasText(revision.nameCn()) || hasText(revision.nameEn())) {
            fields.add(new ShowroomDisplayController.BilingualPublicField(
                    "name",
                    "产品名称",
                    "Product Name",
                    nullToEmpty(revision.nameCn()),
                    nullToEmpty(revision.nameEn())
            ));
        }
        for (String fieldCode : PRODUCT_FIELD_ORDER) {
            if (ShowroomFieldCatalog.productField(fieldCode).tier() != ShowroomFieldTierEnum.BASIC) {
                continue;
            }
            String valueZhRaw = revision.fields().get(fieldCode);
            String valueEnRaw = productBilingualEnglishRawValue(revision, fieldCode, valueZhRaw);
            String valueZh = hasText(valueZhRaw) ? formatProductFieldValue(fieldCode, valueZhRaw) : "";
            String valueEn = hasText(valueEnRaw) ? formatProductFieldValue(fieldCode, valueEnRaw, true) : "";
            if (hasText(valueZh) || hasText(valueEn)) {
                fields.add(new ShowroomDisplayController.BilingualPublicField(
                        fieldCode,
                        productFieldLabel(fieldCode),
                        productFieldLabelEn(fieldCode),
                        valueZh,
                        valueEn
                ));
            }
        }
        return List.copyOf(fields);
    }

    private ShowroomDisplayController.NarrationSummary narrationSummary(String targetType, Long targetId) {
        return new ShowroomDisplayController.NarrationSummary(targetType, targetId);
    }

    private AppConfigNarrationPair requireLiveNarrationPair(ShowroomNarrationTargetType targetType, Long targetId,
                                                            Long sourceRevisionId, String targetLabel) {
        return new AppConfigNarrationPair(
                requireLiveNarration(targetType, targetId, sourceRevisionId, ShowroomNarrationLanguage.ZH, targetLabel),
                requireLiveNarration(targetType, targetId, sourceRevisionId, ShowroomNarrationLanguage.EN, targetLabel));
    }

    private ShowroomNarrationVersion requireLiveNarration(ShowroomNarrationTargetType targetType, Long targetId,
                                                          Long sourceRevisionId, ShowroomNarrationLanguage language,
                                                          String targetLabel) {
        ShowroomNarrationVersion version = narrationService.live(new ShowroomNarrationKey(targetType, targetId,
                        ShowroomNarrationAudienceType.PUBLIC, language))
                .orElseThrow(() -> new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: live "
                        + targetLabel + " " + language.name() + " narration not found"));
        if (!sourceRevisionId.equals(version.sourceRevisionId())) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: live " + targetLabel + " "
                    + language.name() + " narration source revision mismatch");
        }
        requireText(version.scriptText(), "SHOWROOM_SCRIPT_MISSING: live " + targetLabel + " "
                + language.name() + " narration text is required");
        if (version.audioFileId() == null) {
            throw new IllegalStateException("SHOWROOM_AUDIO_GENERATION_FAILED: live " + targetLabel + " "
                    + language.name() + " narration audio is required");
        }
        return version;
    }

    private String requirePreviewImageUrl(String targetType, Long targetId, Long sourceRevisionId, String targetLabel) {
        ShowroomPreviewAssetVersionDO version = previewAssetVersionMapper.selectLatestPublishedByKey(targetType, targetId);
        if (version == null || version.getImageFileId() == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: live " + targetLabel
                    + " preview asset is required");
        }
        if (sourceRevisionId != null && !sourceRevisionId.equals(version.getSourceRevisionId())) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: live " + targetLabel
                    + " preview asset source revision mismatch");
        }
        return fileUrl(version.getImageFileId());
    }

    private String previewImageUrl(String targetType, Long targetId) {
        ShowroomPreviewAssetVersionDO version = previewAssetVersionMapper.selectLatestPublishedByKey(targetType,
                targetId);
        Long imageFileId = version == null ? null : version.getImageFileId();
        return imageFileId == null ? "" : fileUrl(imageFileId);
    }

    private String fileUrl(Long fileId) {
        FileDO file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: file not found: " + fileId);
        }
        return "/admin-api/infra/file/" + file.getConfigId() + "/get/"
                + UriUtils.encodePath(file.getPath(), StandardCharsets.UTF_8);
    }

    private FileDO requirePreviewFile(Long fileId) {
        FileDO file = fileMapper.selectById(fileId);
        if (file == null) {
            throw new IllegalStateException("SHOWROOM_TARGET_NOT_FOUND: file not found: " + fileId);
        }
        if (file.getConfigId() == null || file.getPath() == null || file.getPath().isBlank()) {
            throw new IllegalStateException(
                    "SHOWROOM_PREVIEW_STATIC_ASSET_MISSING: hall preview file metadata is incomplete: " + fileId);
        }
        return file;
    }

    private static String fieldLabel(String targetType, String fieldCode) {
        return ShowroomFieldDisplaySupport.fieldLabel(targetType, fieldCode);
    }

    private static String companyFieldLabel(String fieldCode) {
        return ShowroomFieldDisplaySupport.fieldLabel(TARGET_COMPANY, fieldCode);
    }

    private static String companyFieldLabelEn(String fieldCode) {
        return ShowroomFieldDisplaySupport.fieldLabelEn(TARGET_COMPANY, fieldCode);
    }

    private static String productFieldLabel(String fieldCode) {
        return ShowroomFieldDisplaySupport.fieldLabel(TARGET_PRODUCT, fieldCode);
    }

    private static String productFieldLabelEn(String fieldCode) {
        return ShowroomFieldDisplaySupport.fieldLabelEn(TARGET_PRODUCT, fieldCode);
    }

    private static String companyEnglishFieldKey(String fieldCode) {
        return fieldCode + "_en";
    }

    private static String productEnglishFieldKey(String fieldCode) {
        return fieldCode + "_en";
    }

    private String formatProductFieldValue(String fieldCode, String rawValue) {
        return ShowroomFieldDisplaySupport.formatStoredFieldValue(
                TARGET_PRODUCT, fieldCode, rawValue, contentService);
    }

    private String formatProductFieldValue(String fieldCode, String rawValue, boolean english) {
        return ShowroomFieldDisplaySupport.formatStoredFieldValue(
                TARGET_PRODUCT, fieldCode, rawValue, contentService, english);
    }

    private static String productBilingualEnglishRawValue(ShowroomProductRevision revision, String fieldCode,
                                                          String valueZhRaw) {
        if (!hasText(valueZhRaw)) {
            return "";
        }
        if (PRODUCT_TRANSLATABLE_FIELD_KEYS.contains(fieldCode)) {
            return nullToEmpty(revision.fields().get(productEnglishFieldKey(fieldCode)));
        }
        return valueZhRaw;
    }

    private static String normalizeApprovalStatus(String status) {
        if (STATUS_PENDING_SUPERVISOR_REVIEW.equals(status)) {
            return STATUS_PENDING_SUPERVISOR_APPROVAL;
        }
        return status;
    }

    private record AppConfigNarrationPair(ShowroomNarrationVersion zh, ShowroomNarrationVersion en) {
    }

    private static <T> List<T> page(List<T> values, Integer pageNo, Integer pageSize) {
        int resolvedPageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int resolvedPageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 20);
        int fromIndex = Math.min((resolvedPageNo - 1) * resolvedPageSize, values.size());
        int toIndex = Math.min(fromIndex + resolvedPageSize, values.size());
        return values.subList(fromIndex, toIndex);
    }

    private static <T> PageResult<T> pageResult(List<T> values, Integer pageNo, Integer pageSize) {
        return new PageResult<>(page(values, pageNo, pageSize), (long) values.size());
    }

    private static boolean hasProductPageFilters(ShowroomAdminController.PageQueryReqVO req) {
        return req.productId() != null
                || hasText(req.keyword())
                || hasText(req.ownerCompanyId())
                || hasText(req.ownerType())
                || hasText(req.lifecycleStage())
                || hasText(req.incompleteStatus())
                || hasText(req.approvalStatus());
    }

    private static boolean isProductEditable(ShowroomProductSnapshot snapshot, Set<Long> editableProductIds) {
        return editableProductIds == null || editableProductIds.contains(snapshot.productId());
    }

    private record ProductPageCandidate(ShowroomProductSnapshot snapshot,
                                        ShowroomProductRevision latestRevision,
                                        ShowroomProductRevision displayRevision,
                                        boolean editable) {
    }

    private static Map<String, String> requireMap(Map<String, String> value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
        return Map.copyOf(value);
    }

    private static Long requireId(Long value, String message) {
        if (value == null || value <= 0L) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private static int requirePositiveInt(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private static <T> List<T> requireList(List<T> value, String message) {
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException(message);
        }
        return List.copyOf(value);
    }

    private static String requireText(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }

    private static String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    private static boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword.toLowerCase());
    }

    private void markReleaseDirtyForNarrationIfCurrent(ShowroomNarrationVersion narration,
                                                       Long operatorUserId,
                                                       String reason) {
        if (releaseAutoPublishService == null || narration == null) {
            return;
        }
        if (narration.key().audienceType() != ShowroomNarrationAudienceType.PUBLIC) {
            return;
        }
        if (narration.key().targetType() == ShowroomNarrationTargetType.COMPANY) {
            try {
                ShowroomCompanyRevision currentCompany = contentService.requireCurrentCompanyRevision();
                if (currentCompany.companyId().equals(narration.key().targetId())
                        && currentCompany.revisionId().equals(narration.sourceRevisionId())) {
                    releaseAutoPublishService.markDirty(reason, operatorUserId);
                }
            } catch (RuntimeException ignored) {
                // Keep fail-fast semantics on public readers; dirty marking is best effort only.
            }
            return;
        }
        if (narration.key().targetType() == ShowroomNarrationTargetType.PRODUCT) {
            try {
                ShowroomProductRevision currentProduct = contentService.requireCurrentProductRevision(narration.key().targetId());
                if (currentProduct.revisionId().equals(narration.sourceRevisionId())) {
                    releaseAutoPublishService.markDirty(reason, operatorUserId);
                }
            } catch (RuntimeException ignored) {
                // Keep fail-fast semantics on public readers; dirty marking is best effort only.
            }
            return;
        }
        if (narration.key().targetType() == ShowroomNarrationTargetType.HALL) {
            try {
                ShowroomHall hall = contentService.getHall(narration.key().targetId());
                if (hall.hallId().equals(narration.sourceRevisionId())) {
                    releaseAutoPublishService.markDirty(reason, operatorUserId);
                }
            } catch (RuntimeException ignored) {
                // Keep fail-fast semantics on public readers; dirty marking is best effort only.
            }
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String normalizeText(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static int nullSafeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static Long toEpochMilli(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private static String firstNonBlank(String firstValue, String secondValue) {
        if (hasText(firstValue)) {
            return firstValue;
        }
        return nullToEmpty(secondValue);
    }
}
