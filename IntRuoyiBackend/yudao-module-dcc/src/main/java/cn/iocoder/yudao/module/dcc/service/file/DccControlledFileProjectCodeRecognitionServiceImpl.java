package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileProjectCodeRecognitionRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRecognitionClaimDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRecognitionRecordDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeAliasMappingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRecognitionClaimMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRecognitionRecordMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAliasMappingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_AMBIGUOUS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_IN_PROGRESS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_INVALID_CANDIDATE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_NO_CANDIDATE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_SOURCE_MISSING;
@Service
public class DccControlledFileProjectCodeRecognitionServiceImpl
        implements DccControlledFileProjectCodeRecognitionService {

    private static final int MIN_FILE_NAME_SHORTCUT_PROJECT_CODE_LENGTH = 4;
    private static final int MAX_FAILURE_MESSAGE_LENGTH = 2048;
    static final String RECOGNITION_SCOPE_BASIC_INFO = "BASIC_INFO";
    static final String RECOGNITION_METHOD_FILE_NAME_SHORTCUT = "FILE_NAME_SHORTCUT";
    static final String RECOGNITION_METHOD_FILE_NAME_ALIAS = "FILE_NAME_ALIAS";
    static final String RECOGNITION_METHOD_FILE_NAME_RULE = "FILE_NAME_RULE";
    static final String RECOGNITION_METHOD_DIRECTORY_ALIAS = "DIRECTORY_ALIAS";
    static final String RECOGNITION_METHOD_DIRECTORY_RULE = "DIRECTORY_RULE";
    static final String RECOGNITION_METHOD_BATCH_RULE_ONLY = "BATCH_RULE_ONLY";
    static final String RECOGNITION_METHOD_CODEX_CLI_CONTENT = "CODEX_CLI_CONTENT";
    static final String RECOGNITION_STATUS_SUCCESS = "SUCCESS";
    static final String RECOGNITION_STATUS_NO_MATCH = "NO_MATCH";
    static final String RECOGNITION_STATUS_UNKNOWN_DCC_BASIC_DATA = "UNKNOWN_DCC";
    static final String RECOGNITION_STATUS_UNRECOGNIZED_PROJECT_NAME = "NAME_MISMATCH";
    static final String RECOGNITION_STATUS_FAILED = "FAILED";
    private static final Set<String> SUPPORTED_RECOGNITION_RECORD_STATUSES = Set.of(
            RECOGNITION_STATUS_SUCCESS,
            RECOGNITION_STATUS_NO_MATCH,
            RECOGNITION_STATUS_UNKNOWN_DCC_BASIC_DATA,
            RECOGNITION_STATUS_UNRECOGNIZED_PROJECT_NAME,
            RECOGNITION_STATUS_FAILED);

    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource
    private DccProjectCodeAliasMappingMapper projectCodeAliasMappingMapper;
    @Resource
    private DccFileDirectoryMapper directoryMapper;
    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccFileTypeTaxonomyAdminService fileTypeTaxonomyAdminService;
    @Resource
    private FileService fileService;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DccProjectCodeRecognitionCodexCliClient codexCliClient;
    @Resource
    private DccControlledFileRecognitionRecordMapper recognitionRecordMapper;
    @Resource
    private DccControlledFileRecognitionClaimMapper recognitionClaimMapper;
    @Resource
    private DccProjectCodeRecognitionProperties recognitionProperties;
    @Resource
    private PlatformTransactionManager transactionManager;

    @Override
    public DccControlledFileProjectCodeRecognitionRespVO recognizeProjectCode(Long userId, Long id) {
        return recognizeProjectCode(userId, id, null);
    }

    @Override
    public DccControlledFileProjectCodeRecognitionRespVO recognizeProjectCode(Long userId, Long id, Long claimTaskId) {
        validateDocControlRole(userId);
        DccControlledFileDO controlledFile = null;
        FileDO sourceFile = null;
        String recognitionVersion = null;
        String recognitionMethod = RECOGNITION_METHOD_FILE_NAME_SHORTCUT;
        String failureStage = DccRecognitionFailureClassifier.STAGE_PRECONDITION;

        try {
            controlledFile = controlledFileMapper.selectById(id);
            if (controlledFile == null) {
                throw exception(CONTROLLED_FILE_NOT_EXISTS);
            }
            acquireRecognitionClaim(controlledFile, userId, claimTaskId);
            sourceFile = requireSourceFile(controlledFile);
            List<DccProjectCodeDO> candidates = normalizeRecognitionCandidates(projectCodeMapper.selectEnabledList());
            if (CollUtil.isEmpty(candidates)) {
                throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_NO_CANDIDATE);
            }
            recognitionVersion = requireRecognitionVersion();
            failureStage = DccRecognitionFailureClassifier.STAGE_RULE_MATCHING;
            DirectoryContext directoryContext = buildDirectoryContext(controlledFile.getDirectoryId());
            FileTypeLevels fileTypeLevels = resolveFileTypeLevels(directoryContext, sourceFile.getName());
            String sourceEvidenceContext = buildSourceFileContext(directoryContext, sourceFile.getName());
            List<DccProjectCodeAliasMappingDO> aliasMappings = normalizeAliasMappings(
                    projectCodeAliasMappingMapper.selectConfirmedActiveList(), candidates);
            DccProjectCodeRecognitionResult recognitionResult =
                    recognizeProjectCodeFromAlias(sourceFile.getName(), aliasMappings);
            if (recognitionResult != null) {
                recognitionMethod = RECOGNITION_METHOD_FILE_NAME_ALIAS;
            }
            if (recognitionResult == null) {
                recognitionResult = recognizeProjectCodeFromRule(sourceFile.getName(), candidates);
            }
            if (recognitionResult != null && !RECOGNITION_METHOD_FILE_NAME_ALIAS.equals(recognitionMethod)) {
                recognitionMethod = recognitionResult.matchType() == DccProjectCodeRecognitionMatchType.PROJECT_CODE
                        ? RECOGNITION_METHOD_FILE_NAME_SHORTCUT : RECOGNITION_METHOD_FILE_NAME_RULE;
            }
            if (recognitionResult == null) {
                recognitionResult = recognizeProjectCodeFromAlias(directoryContext.path(), aliasMappings);
                if (recognitionResult != null) {
                    recognitionMethod = RECOGNITION_METHOD_DIRECTORY_ALIAS;
                }
            }
            if (recognitionResult == null) {
                recognitionResult = recognizeProjectCodeFromDirectoryRule(directoryContext.path(), candidates);
                if (recognitionResult != null) {
                    recognitionMethod = RECOGNITION_METHOD_DIRECTORY_RULE;
                }
            }
            if (recognitionResult == null && claimTaskId != null) {
                recognitionMethod = RECOGNITION_METHOD_BATCH_RULE_ONLY;
            }
            if (recognitionResult == null && !RECOGNITION_METHOD_BATCH_RULE_ONLY.equals(recognitionMethod)) {
                recognitionMethod = RECOGNITION_METHOD_CODEX_CLI_CONTENT;
                failureStage = DccRecognitionFailureClassifier.STAGE_SOURCE_ACCESS;
                byte[] sourceContent = readSourceContent(sourceFile);
                failureStage = DccRecognitionFailureClassifier.STAGE_AI_CLASSIFICATION;
                recognitionResult = codexCliClient.recognizeProjectCode(
                        new DccProjectCodeRecognitionCommand(
                                controlledFile.getId(),
                                sourceFile.getId(),
                                sourceEvidenceContext,
                                sourceFile.getType(),
                                sourceContent,
                                candidates.stream().map(this::toCandidate).toList()));
            }
            failureStage = DccRecognitionFailureClassifier.STAGE_RESULT_VALIDATION;
            if (recognitionResult == null || recognitionResult.projectCodeId() == null
                    || recognitionResult.matchType() == null) {
                LocalDateTime recognizedTime = LocalDateTime.now();
                DccControlledFileRecognitionRecordDO recognitionRecord = DccControlledFileRecognitionRecordDO.builder()
                        .tenantId(controlledFile.getTenantId())
                        .controlledFileId(controlledFile.getId())
                        .recognitionScope(RECOGNITION_SCOPE_BASIC_INFO)
                        .recognitionMethod(recognitionMethod)
                        .recognitionVersion(recognitionVersion)
                        .status(RECOGNITION_STATUS_UNKNOWN_DCC_BASIC_DATA)
                        .batchTaskId(claimTaskId)
                        .fileTypeTaxonomyId(fileTypeLevels.taxonomyId())
                        .fileTypeLevel1(fileTypeLevels.level1())
                        .fileTypeLevel2(fileTypeLevels.level2())
                        .fileTypeLevel3(fileTypeLevels.level3())
                        .fileTypeLevel4(fileTypeLevels.level4())
                        .fileTypeLevel5(fileTypeLevels.level5())
                        .recognizedBy(userId)
                        .recognizedTime(recognizedTime)
                        .sourceFileId(sourceFile.getId())
                        .build();
                failureStage = DccRecognitionFailureClassifier.STAGE_PERSISTENCE;
                persistNoMatchRecognitionRecord(recognitionRecord);

                DccControlledFileProjectCodeRecognitionRespVO respVO =
                        new DccControlledFileProjectCodeRecognitionRespVO();
                respVO.setControlledFileId(controlledFile.getId());
                respVO.setRecognitionStatus(RECOGNITION_STATUS_UNKNOWN_DCC_BASIC_DATA);
                respVO.setRecognitionMethod(recognitionMethod);
                respVO.setRecognitionVersion(recognitionVersion);
                return respVO;
            }

            DccControlledFileProjectCodeRecognitionRespVO unrecognizedProjectNameResp = resolveUnrecognizedProjectName(
                    controlledFile, sourceFile, recognitionResult, candidates, recognitionMethod, recognitionVersion,
                    claimTaskId, fileTypeLevels, userId);
            if (unrecognizedProjectNameResp != null) {
                return unrecognizedProjectNameResp;
            }

            DccProjectCodeDO matchedProjectCode = validateRecognitionResult(recognitionResult, candidates);
            String matchType = recognitionResult.matchType().name();
            String matchText = StrUtil.trim(recognitionResult.matchText());
            String projectName = normalizeRequiredProjectName(matchedProjectCode);
            String projectCode = StrUtil.nullToEmpty(matchedProjectCode.getProjectCode());
            LocalDateTime recognizedTime = LocalDateTime.now();

            DccControlledFileDO fileUpdate = DccControlledFileDO.builder()
                    .id(controlledFile.getId())
                    .dccProjectCodeId(matchedProjectCode.getId())
                    .productName(projectName)
                    .productCode(projectCode)
                    .projectCodeRecognitionType(matchType)
                    .projectCodeRecognitionText(matchText)
                    .projectCodeRecognizedBy(userId)
                    .projectCodeRecognizedTime(recognizedTime)
                    .fileTypeTaxonomyId(fileTypeLevels.taxonomyId())
                    .fileTypeLevel1(fileTypeLevels.level1())
                    .fileTypeLevel2(fileTypeLevels.level2())
                    .fileTypeLevel3(fileTypeLevels.level3())
                    .fileTypeLevel4(fileTypeLevels.level4())
                    .fileTypeLevel5(fileTypeLevels.level5())
                    .build();
            DccControlledFileRecognitionRecordDO recognitionRecord = DccControlledFileRecognitionRecordDO.builder()
                    .tenantId(controlledFile.getTenantId())
                    .controlledFileId(controlledFile.getId())
                    .recognitionScope(RECOGNITION_SCOPE_BASIC_INFO)
                    .recognitionMethod(recognitionMethod)
                    .recognitionVersion(recognitionVersion)
                    .status(RECOGNITION_STATUS_SUCCESS)
                    .batchTaskId(claimTaskId)
                    .matchedProjectCodeId(matchedProjectCode.getId())
                    .matchedProjectAliasId(recognitionResult.matchedProjectAliasId())
                    .matchedProjectAliasText(recognitionResult.matchedProjectAliasText())
                    .matchedProjectAliasSource(recognitionResult.matchedProjectAliasSource())
                    .recognizedProductCode(projectCode)
                    .recognizedProductName(projectName)
                    .matchType(matchType)
                    .matchText(matchText)
                    .fileTypeTaxonomyId(fileTypeLevels.taxonomyId())
                    .fileTypeLevel1(fileTypeLevels.level1())
                    .fileTypeLevel2(fileTypeLevels.level2())
                    .fileTypeLevel3(fileTypeLevels.level3())
                    .fileTypeLevel4(fileTypeLevels.level4())
                    .fileTypeLevel5(fileTypeLevels.level5())
                    .recognizedBy(userId)
                    .recognizedTime(recognizedTime)
                    .sourceFileId(sourceFile.getId())
                    .build();
            failureStage = DccRecognitionFailureClassifier.STAGE_PERSISTENCE;
            persistSuccessfulRecognition(fileUpdate, recognitionRecord);

            DccControlledFileProjectCodeRecognitionRespVO respVO =
                    new DccControlledFileProjectCodeRecognitionRespVO();
            respVO.setControlledFileId(controlledFile.getId());
            respVO.setRecognitionStatus(RECOGNITION_STATUS_SUCCESS);
            respVO.setDccProjectCodeId(matchedProjectCode.getId());
            respVO.setProjectName(matchedProjectCode.getProjectName());
            respVO.setProjectCode(projectCode);
            respVO.setMatchType(matchType);
            respVO.setMatchText(matchText);
            respVO.setRecognitionMethod(recognitionMethod);
            respVO.setRecognitionVersion(recognitionVersion);
            respVO.setMatchedProjectAliasId(recognitionResult.matchedProjectAliasId());
            respVO.setMatchedProjectAliasText(recognitionResult.matchedProjectAliasText());
            respVO.setMatchedProjectAliasSource(recognitionResult.matchedProjectAliasSource());
            return respVO;
        } catch (RuntimeException ex) {
            if (controlledFile != null && sourceFile != null && recognitionVersion != null) {
                DccRecognitionFailureClassifier.FailureMetadata failureMetadata =
                        DccRecognitionFailureClassifier.classify(
                                ex,
                                failureStage,
                                DccRecognitionFailureClassifier.defaultCodeForStage(failureStage));
                persistFailedRecognitionRecord(DccControlledFileRecognitionRecordDO.builder()
                        .tenantId(controlledFile.getTenantId())
                        .controlledFileId(controlledFile.getId())
                        .recognitionScope(RECOGNITION_SCOPE_BASIC_INFO)
                        .recognitionMethod(recognitionMethod)
                        .recognitionVersion(recognitionVersion)
                        .status(RECOGNITION_STATUS_FAILED)
                        .batchTaskId(claimTaskId)
                        .failureStage(failureMetadata.stage())
                        .failureCode(failureMetadata.code())
                        .failureMessage(resolveFailureMessage(ex))
                        .recognizedBy(userId)
                        .recognizedTime(LocalDateTime.now())
                        .sourceFileId(sourceFile.getId())
                        .build());
            }
            throw ex;
        } finally {
            releaseRecognitionClaim(id, userId, claimTaskId);
        }
    }

    private void acquireRecognitionClaim(DccControlledFileDO controlledFile, Long userId, Long claimTaskId) {
        if (recognitionClaimMapper.tryClaimBasicInfo(
                controlledFile.getTenantId(),
                controlledFile.getId(),
                RECOGNITION_SCOPE_BASIC_INFO,
                userId,
                claimTaskId,
                LocalDateTime.now()) > 0) {
            return;
        }
        DccControlledFileRecognitionClaimDO currentClaim =
                recognitionClaimMapper.selectByFileAndScope(controlledFile.getId(), RECOGNITION_SCOPE_BASIC_INFO);
        if (currentClaim != null
                && Objects.equals(currentClaim.getClaimedBy(), userId)
                && Objects.equals(currentClaim.getClaimTaskId(), claimTaskId)) {
            return;
        }
        throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_IN_PROGRESS, controlledFile.getId());
    }

    private void releaseRecognitionClaim(Long controlledFileId, Long userId, Long claimTaskId) {
        recognitionClaimMapper.releaseClaim(controlledFileId, RECOGNITION_SCOPE_BASIC_INFO, userId, claimTaskId);
    }

    private String requireRecognitionVersion() {
        String version = StrUtil.trimToNull(recognitionProperties.getVersion());
        if (version == null || isUnresolvedConfigPlaceholder(version)) {
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_CONFIG_MISSING,
                    "version is required");
        }
        return version;
    }

    private boolean isUnresolvedConfigPlaceholder(String value) {
        return value.contains("${");
    }

    private void upsertRecognitionRecord(DccControlledFileRecognitionRecordDO record) {
        validateRecognitionRecordStatus(record);
        recognitionRecordMapper.upsert(record);
    }

    private void validateRecognitionRecordStatus(DccControlledFileRecognitionRecordDO record) {
        String status = record == null ? null : StrUtil.trimToNull(record.getStatus());
        if (status == null || !SUPPORTED_RECOGNITION_RECORD_STATUSES.contains(status)) {
            throw new IllegalStateException("unsupported DCC recognition record status: "
                    + StrUtil.nullToEmpty(status));
        }
        record.setStatus(status);
    }

    private void persistSuccessfulRecognition(DccControlledFileDO fileUpdate,
                                               DccControlledFileRecognitionRecordDO recognitionRecord) {
        tx().executeWithoutResult(status -> {
            int updatedRows = controlledFileMapper.updateById(fileUpdate);
            if (updatedRows != 1) {
                throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED,
                        "database update affected " + updatedRows + " rows");
            }
            upsertRecognitionRecord(recognitionRecord);
        });
    }

    private void persistNoMatchRecognitionRecord(DccControlledFileRecognitionRecordDO recognitionRecord) {
        tx().executeWithoutResult(status -> upsertRecognitionRecord(recognitionRecord));
    }

    private void persistFailedRecognitionRecord(DccControlledFileRecognitionRecordDO recognitionRecord) {
        tx().executeWithoutResult(status -> upsertRecognitionRecord(recognitionRecord));
    }

    private TransactionTemplate tx() {
        return new TransactionTemplate(transactionManager);
    }

    private String resolveFailureMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        if (current.getSuppressed().length > 0) {
            return normalizeFailureMessage(StrUtil.blankToDefault(current.getSuppressed()[0].getMessage(),
                    current.getSuppressed()[0].getClass().getSimpleName()));
        }
        return normalizeFailureMessage(StrUtil.blankToDefault(current.getMessage(), current.getClass().getSimpleName()));
    }

    private String normalizeFailureMessage(String message) {
        String normalized = StrUtil.trimToNull(message);
        if (normalized == null || normalized.length() <= MAX_FAILURE_MESSAGE_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_FAILURE_MESSAGE_LENGTH);
    }

    private DccProjectCodeRecognitionCommand.Candidate toCandidate(DccProjectCodeDO projectCode) {
        return new DccProjectCodeRecognitionCommand.Candidate(
                projectCode.getId(),
                projectCode.getProjectName(),
                StrUtil.nullToEmpty(projectCode.getProjectCode()),
                projectCode.getCategory(),
                projectCode.getPriority());
    }

    private List<DccProjectCodeDO> normalizeRecognitionCandidates(List<DccProjectCodeDO> candidates) {
        if (CollUtil.isEmpty(candidates)) {
            return List.of();
        }
        Map<String, DccProjectCodeDO> uniqueCandidates = new LinkedHashMap<>();
        for (DccProjectCodeDO candidate : candidates) {
            if (candidate == null || candidate.getId() == null) {
                continue;
            }
            uniqueCandidates.putIfAbsent(buildCandidateDedupKey(candidate), candidate);
        }
        return new ArrayList<>(uniqueCandidates.values());
    }

    private List<DccProjectCodeAliasMappingDO> normalizeAliasMappings(List<DccProjectCodeAliasMappingDO> mappings,
                                                                      List<DccProjectCodeDO> candidates) {
        if (CollUtil.isEmpty(mappings)) {
            return List.of();
        }
        Map<Long, DccProjectCodeDO> candidateById = candidates.stream()
                .collect(Collectors.toMap(DccProjectCodeDO::getId, Function.identity(), (left, right) -> left));
        return mappings.stream()
                .filter(Objects::nonNull)
                .filter(mapping -> mapping.getId() != null && mapping.getProjectCodeId() != null)
                .filter(mapping -> candidateById.containsKey(mapping.getProjectCodeId()))
                .filter(mapping -> normalizeAliasText(mapping) != null)
                .toList();
    }

    private String buildCandidateDedupKey(DccProjectCodeDO candidate) {
        return StrUtil.nullToEmpty(StrUtil.trim(candidate.getProjectCode()))
                + "\u0001"
                + StrUtil.nullToEmpty(StrUtil.trim(candidate.getProjectName()));
    }

    private DccProjectCodeRecognitionResult recognizeProjectCodeFromRule(String sourceText,
                                                                         List<DccProjectCodeDO> candidates) {
        String normalizedSourceText = StrUtil.trimToNull(sourceText);
        if (normalizedSourceText == null) {
            return null;
        }
        DccProjectCodeRecognitionResult codeResult = recognizeProjectCodeFromCodeRule(normalizedSourceText, candidates);
        if (codeResult != null) {
            return codeResult;
        }
        return recognizeProjectCodeFromNameRule(normalizedSourceText, candidates);
    }

    private DccProjectCodeRecognitionResult recognizeProjectCodeFromAlias(
            String sourceText, List<DccProjectCodeAliasMappingDO> aliasMappings) {
        String normalizedSource = normalizeProjectNameAlias(sourceText);
        if (normalizedSource == null || CollUtil.isEmpty(aliasMappings)) {
            return null;
        }
        List<ProjectAliasRuleMatch> matchedAliases = aliasMappings.stream()
                .map(mapping -> buildProjectAliasRuleMatch(mapping, normalizedSource))
                .filter(Objects::nonNull)
                .toList();
        if (CollUtil.isEmpty(matchedAliases)) {
            return null;
        }
        int longestAliasLength = matchedAliases.stream()
                .map(match -> match.normalizedAliasText().length())
                .max(Integer::compareTo)
                .orElse(0);
        List<ProjectAliasRuleMatch> longestMatches = matchedAliases.stream()
                .filter(match -> match.normalizedAliasText().length() == longestAliasLength)
                .toList();
        if (longestMatches.stream().map(match -> match.mapping().getProjectCodeId()).distinct().count() != 1) {
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_AMBIGUOUS,
                    "projectAlias=" + longestMatches.get(0).mapping().getAliasText());
        }
        DccProjectCodeAliasMappingDO matchedMapping = longestMatches.get(0).mapping();
        return new DccProjectCodeRecognitionResult(
                matchedMapping.getProjectCodeId(),
                DccProjectCodeRecognitionMatchType.PROJECT_NAME,
                StrUtil.trim(matchedMapping.getAliasText()),
                matchedMapping.getId(),
                StrUtil.trim(matchedMapping.getAliasText()),
                StrUtil.trim(matchedMapping.getAliasSource()));
    }

    private ProjectAliasRuleMatch buildProjectAliasRuleMatch(DccProjectCodeAliasMappingDO mapping,
                                                             String normalizedSource) {
        String normalizedAlias = normalizeAliasText(mapping);
        if (normalizedAlias == null || !normalizedSource.contains(normalizedAlias)) {
            return null;
        }
        return new ProjectAliasRuleMatch(mapping, normalizedAlias);
    }

    private String normalizeAliasText(DccProjectCodeAliasMappingDO mapping) {
        String stored = normalizeProjectNameAlias(mapping.getNormalizedAliasText());
        return stored != null ? stored : normalizeProjectNameAlias(mapping.getAliasText());
    }

    private DccProjectCodeRecognitionResult recognizeProjectCodeFromDirectoryRule(String sourceText,
                                                                                  List<DccProjectCodeDO> candidates) {
        String normalizedSourceText = StrUtil.trimToNull(sourceText);
        if (normalizedSourceText == null) {
            return null;
        }
        DccProjectCodeRecognitionResult nameResult = recognizeProjectCodeFromNameRule(normalizedSourceText, candidates);
        if (nameResult != null) {
            return nameResult;
        }
        return recognizeProjectCodeFromCodeRule(normalizedSourceText, candidates);
    }

    private DccProjectCodeRecognitionResult recognizeProjectCodeFromCodeRule(String sourceText,
                                                                             List<DccProjectCodeDO> candidates) {
        List<DccProjectCodeDO> matchedCandidates = candidates.stream()
                .filter(item -> StrUtil.isNotBlank(item.getProjectCode()))
                .filter(item -> isHighConfidenceProjectCodeShortcut(item.getProjectCode()))
                .filter(item -> containsWholeProjectCode(sourceText, item.getProjectCode()))
                .toList();
        if (CollUtil.isEmpty(matchedCandidates)) {
            return null;
        }
        int longestCodeLength = matchedCandidates.stream()
                .map(item -> StrUtil.length(StrUtil.trim(item.getProjectCode())))
                .max(Integer::compareTo)
                .orElse(0);
        if (longestCodeLength <= 0) {
            return null;
        }
        List<DccProjectCodeDO> longestMatchedCandidates = matchedCandidates.stream()
                .filter(item -> StrUtil.length(StrUtil.trim(item.getProjectCode())) == longestCodeLength)
                .toList();
        if (longestMatchedCandidates.size() != 1) {
            return null;
        }
        DccProjectCodeDO matchedCandidate = longestMatchedCandidates.get(0);
        return new DccProjectCodeRecognitionResult(
                matchedCandidate.getId(),
                DccProjectCodeRecognitionMatchType.PROJECT_CODE,
                StrUtil.trim(matchedCandidate.getProjectCode()));
    }

    private DccProjectCodeRecognitionResult recognizeProjectCodeFromNameRule(String sourceText,
                                                                             List<DccProjectCodeDO> candidates) {
        String normalizedSource = normalizeProjectNameAlias(sourceText);
        if (normalizedSource == null) {
            return null;
        }
        List<ProjectNameRuleMatch> matchedCandidates = candidates.stream()
                .map(candidate -> buildProjectNameRuleMatch(candidate, normalizedSource, sourceText))
                .filter(Objects::nonNull)
                .toList();
        if (CollUtil.isEmpty(matchedCandidates)) {
            return null;
        }
        int longestNameLength = matchedCandidates.stream()
                .map(match -> match.normalizedProjectName().length())
                .max(Integer::compareTo)
                .orElse(0);
        List<ProjectNameRuleMatch> longestMatches = matchedCandidates.stream()
                .filter(match -> match.normalizedProjectName().length() == longestNameLength)
                .toList();
        if (longestMatches.size() != 1) {
            return null;
        }
        ProjectNameRuleMatch match = longestMatches.get(0);
        return new DccProjectCodeRecognitionResult(
                match.projectCode().getId(),
                DccProjectCodeRecognitionMatchType.PROJECT_NAME,
                match.matchText());
    }

    private ProjectNameRuleMatch buildProjectNameRuleMatch(DccProjectCodeDO candidate,
                                                           String normalizedSource,
                                                           String sourceText) {
        String normalizedProjectName = normalizeProjectNameAlias(candidate.getProjectName());
        if (normalizedProjectName == null || !normalizedSource.contains(normalizedProjectName)) {
            return null;
        }
        return new ProjectNameRuleMatch(candidate, normalizedProjectName,
                resolveMatchedProjectNameText(sourceText, candidate));
    }

    private String resolveMatchedProjectNameText(String sourceText, DccProjectCodeDO candidate) {
        String projectName = StrUtil.trimToNull(candidate.getProjectName());
        String category = StrUtil.trimToNull(candidate.getCategory());
        if (projectName != null && category != null) {
            String withCategory = projectName + "（" + category + "）";
            if (sourceText.contains(withCategory)) {
                return withCategory;
            }
        }
        if (projectName != null) {
            String sourceMatchedName = resolveSourceMatchedProjectName(sourceText, projectName);
            if (sourceMatchedName != null) {
                return sourceMatchedName;
            }
        }
        if (projectName != null && sourceText.contains(projectName)) {
            return projectName;
        }
        return projectName;
    }

    private String resolveSourceMatchedProjectName(String sourceText, String projectName) {
        int startIndex = sourceText.indexOf(projectName);
        if (startIndex < 0) {
            return null;
        }
        int nextIndex = startIndex + projectName.length();
        if (nextIndex < sourceText.length() && sourceText.charAt(nextIndex) == '（') {
            int endIndex = sourceText.indexOf('）', nextIndex);
            if (endIndex > nextIndex) {
                return sourceText.substring(startIndex, endIndex + 1);
            }
        }
        return projectName;
    }

    private String buildSourceFileContext(DirectoryContext directoryContext, String sourceFileName) {
        String normalizedFileName = StrUtil.blankToDefault(StrUtil.trim(sourceFileName), "source-file");
        String directoryPath = directoryContext.path();
        if (directoryPath == null) {
            return normalizedFileName;
        }
        return directoryPath + "/" + normalizedFileName;
    }

    private DirectoryContext buildDirectoryContext(Long directoryId) {
        if (directoryId == null) {
            return new DirectoryContext(null, List.of());
        }
        List<String> segments = new ArrayList<>();
        Long currentId = directoryId;
        while (currentId != null) {
            DccFileDirectoryDO current = directoryMapper.selectById(currentId);
            if (current == null) {
                break;
            }
            String name = StrUtil.trimToNull(current.getName());
            if (name != null) {
                segments.add(0, name);
            }
            currentId = current.getParentId();
        }
        return new DirectoryContext(CollUtil.isEmpty(segments) ? null : String.join("/", segments), segments);
    }

    private boolean isHighConfidenceProjectCodeShortcut(String projectCode) {
        String normalizedProjectCode = StrUtil.trimToNull(projectCode);
        if (normalizedProjectCode == null) {
            return false;
        }
        int effectiveLength = 0;
        for (int index = 0; index < normalizedProjectCode.length(); index++) {
            if (isAsciiLetterOrDigit(normalizedProjectCode.charAt(index))) {
                effectiveLength++;
            }
        }
        return effectiveLength >= MIN_FILE_NAME_SHORTCUT_PROJECT_CODE_LENGTH;
    }

    private boolean containsWholeProjectCode(String sourceFileName, String projectCode) {
        String normalizedProjectCode = StrUtil.trimToNull(projectCode);
        if (normalizedProjectCode == null) {
            return false;
        }
        int codeLength = normalizedProjectCode.length();
        int lastStartIndex = sourceFileName.length() - codeLength;
        for (int startIndex = 0; startIndex <= lastStartIndex; startIndex++) {
            if (!sourceFileName.regionMatches(true, startIndex, normalizedProjectCode, 0, codeLength)) {
                continue;
            }
            if (isWholeProjectCodeMatch(sourceFileName, startIndex, codeLength)) {
                return true;
            }
        }
        return false;
    }

    private boolean isWholeProjectCodeMatch(String sourceFileName, int startIndex, int codeLength) {
        int previousIndex = startIndex - 1;
        if (previousIndex >= 0 && !isProjectCodeBoundary(sourceFileName.charAt(previousIndex))) {
            return false;
        }
        int nextIndex = startIndex + codeLength;
        return nextIndex >= sourceFileName.length()
                || isProjectCodeBoundary(sourceFileName.charAt(nextIndex));
    }

    private boolean isProjectCodeBoundary(char value) {
        return !isAsciiLetterOrDigit(value);
    }

    private boolean isAsciiLetterOrDigit(char value) {
        return (value >= '0' && value <= '9')
                || (value >= 'A' && value <= 'Z')
                || (value >= 'a' && value <= 'z');
    }

    private DccProjectCodeDO validateRecognitionResult(DccProjectCodeRecognitionResult result,
                                                       List<DccProjectCodeDO> candidates) {
        Map<Long, DccProjectCodeDO> candidateById = candidates.stream()
                .collect(Collectors.toMap(DccProjectCodeDO::getId, Function.identity(), (left, right) -> left));
        DccProjectCodeDO matched = candidateById.get(result.projectCodeId());
        if (matched == null) {
            matched = resolveMatchedCandidateByEvidence(result, candidates);
        }
        return switch (result.matchType()) {
            case PROJECT_NAME -> validateProjectNameMatch(result, matched, candidates);
            case PROJECT_CODE -> validateProjectCodeMatch(result, matched, candidates);
        };
    }

    private DccControlledFileProjectCodeRecognitionRespVO resolveUnrecognizedProjectName(
            DccControlledFileDO controlledFile, FileDO sourceFile, DccProjectCodeRecognitionResult result,
            List<DccProjectCodeDO> candidates, String recognitionMethod, String recognitionVersion,
            Long claimTaskId, FileTypeLevels fileTypeLevels, Long userId) {
        if (result.matchType() != DccProjectCodeRecognitionMatchType.PROJECT_NAME || result.hasAliasEvidence()) {
            return null;
        }
        DccProjectCodeDO matched = resolveProjectNameCandidateForNoMatchStatus(result, candidates);
        if (matched == null || isProjectNameMatchRecognized(result, matched)) {
            return null;
        }
        LocalDateTime recognizedTime = LocalDateTime.now();
        DccControlledFileRecognitionRecordDO recognitionRecord = DccControlledFileRecognitionRecordDO.builder()
                .tenantId(controlledFile.getTenantId())
                .controlledFileId(controlledFile.getId())
                .recognitionScope(RECOGNITION_SCOPE_BASIC_INFO)
                .recognitionMethod(recognitionMethod)
                .recognitionVersion(recognitionVersion)
                .status(RECOGNITION_STATUS_UNRECOGNIZED_PROJECT_NAME)
                .batchTaskId(claimTaskId)
                .matchedProjectCodeId(matched.getId())
                .matchType(result.matchType().name())
                .matchText(StrUtil.trim(result.matchText()))
                .fileTypeTaxonomyId(fileTypeLevels.taxonomyId())
                .fileTypeLevel1(fileTypeLevels.level1())
                .fileTypeLevel2(fileTypeLevels.level2())
                .fileTypeLevel3(fileTypeLevels.level3())
                .fileTypeLevel4(fileTypeLevels.level4())
                .fileTypeLevel5(fileTypeLevels.level5())
                .recognizedBy(userId)
                .recognizedTime(recognizedTime)
                .sourceFileId(sourceFile.getId())
                .build();
        persistNoMatchRecognitionRecord(recognitionRecord);

        DccControlledFileProjectCodeRecognitionRespVO respVO = new DccControlledFileProjectCodeRecognitionRespVO();
        respVO.setControlledFileId(controlledFile.getId());
        respVO.setRecognitionStatus(RECOGNITION_STATUS_UNRECOGNIZED_PROJECT_NAME);
        respVO.setRecognitionMethod(recognitionMethod);
        respVO.setRecognitionVersion(recognitionVersion);
        respVO.setMatchType(result.matchType().name());
        respVO.setMatchText(StrUtil.trim(result.matchText()));
        return respVO;
    }

    private boolean isProjectNameMatchRecognized(DccProjectCodeRecognitionResult result, DccProjectCodeDO matched) {
        String projectName = StrUtil.trimToNull(matched.getProjectName());
        String matchText = StrUtil.trimToNull(result.matchText());
        return projectName != null && Objects.equals(normalizeProjectNameAlias(projectName),
                normalizeProjectNameAlias(matchText));
    }

    private DccProjectCodeDO resolveMatchedCandidateByEvidence(DccProjectCodeRecognitionResult result,
                                                               List<DccProjectCodeDO> candidates) {
        String matchText = StrUtil.trimToNull(result.matchText());
        if (matchText == null) {
            throw invalidCandidateException(result, "candidate is not in enabled current-tenant DCC basic data");
        }
        return switch (result.matchType()) {
            case PROJECT_CODE -> resolveUniqueCandidateByProjectCode(result, candidates);
            case PROJECT_NAME -> resolveUniqueCandidateByExactProjectName(result, candidates);
        };
    }

    private DccProjectCodeDO resolveProjectNameCandidateForNoMatchStatus(DccProjectCodeRecognitionResult result,
                                                                         List<DccProjectCodeDO> candidates) {
        DccProjectCodeDO matchedById = candidates.stream()
                .filter(candidate -> Objects.equals(candidate.getId(), result.projectCodeId()))
                .findFirst()
                .orElse(null);
        if (matchedById != null) {
            return matchedById;
        }
        return resolveUniqueCandidateByContainedProjectName(result, candidates);
    }

    private DccProjectCodeDO resolveUniqueCandidateByProjectCode(DccProjectCodeRecognitionResult result,
                                                                 List<DccProjectCodeDO> candidates) {
        String normalizedMatchText = normalizeProjectCodeKey(result.matchText());
        if (normalizedMatchText == null) {
            throw invalidCandidateException(result, "candidate is not in enabled current-tenant DCC basic data");
        }
        List<DccProjectCodeDO> matchedCandidates = candidates.stream()
                .filter(candidate -> Objects.equals(normalizeProjectCodeKey(candidate.getProjectCode()),
                        normalizedMatchText))
                .toList();
        if (CollUtil.isEmpty(matchedCandidates)) {
            throw invalidCandidateException(result, "candidate is not in enabled current-tenant DCC basic data");
        }
        if (matchedCandidates.size() != 1) {
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_AMBIGUOUS,
                    "projectCode=" + StrUtil.trim(result.matchText()));
        }
        return matchedCandidates.get(0);
    }

    private DccProjectCodeDO resolveUniqueCandidateByExactProjectName(DccProjectCodeRecognitionResult result,
                                                                      List<DccProjectCodeDO> candidates) {
        String normalizedMatchText = normalizeProjectNameAlias(result.matchText());
        if (normalizedMatchText == null) {
            throw invalidCandidateException(result, "candidate is not in enabled current-tenant DCC basic data");
        }
        List<DccProjectCodeDO> matchedCandidates = candidates.stream()
                .filter(candidate -> Objects.equals(normalizeProjectNameAlias(candidate.getProjectName()),
                        normalizedMatchText))
                .toList();
        if (CollUtil.isEmpty(matchedCandidates)) {
            throw invalidCandidateException(result, "candidate is not in enabled current-tenant DCC basic data");
        }
        if (matchedCandidates.size() != 1) {
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_AMBIGUOUS,
                    "projectName=" + StrUtil.trim(result.matchText()));
        }
        return matchedCandidates.get(0);
    }

    private DccProjectCodeDO resolveUniqueCandidateByContainedProjectName(DccProjectCodeRecognitionResult result,
                                                                         List<DccProjectCodeDO> candidates) {
        String normalizedMatchText = normalizeProjectNameAlias(result.matchText());
        if (normalizedMatchText == null) {
            return null;
        }
        List<ProjectNameRuleMatch> matchedCandidates = candidates.stream()
                .map(candidate -> buildProjectNameRuleMatch(candidate, normalizedMatchText, result.matchText()))
                .filter(Objects::nonNull)
                .toList();
        if (CollUtil.isEmpty(matchedCandidates)) {
            return null;
        }
        int longestNameLength = matchedCandidates.stream()
                .map(match -> match.normalizedProjectName().length())
                .max(Integer::compareTo)
                .orElse(0);
        List<ProjectNameRuleMatch> longestMatches = matchedCandidates.stream()
                .filter(match -> match.normalizedProjectName().length() == longestNameLength)
                .toList();
        if (longestMatches.size() != 1) {
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_AMBIGUOUS,
                    "projectName=" + StrUtil.trim(result.matchText()));
        }
        return longestMatches.get(0).projectCode();
    }

    private DccProjectCodeDO validateProjectNameMatch(DccProjectCodeRecognitionResult result,
                                                      DccProjectCodeDO matched,
                                                      List<DccProjectCodeDO> candidates) {
        String projectName = StrUtil.trimToNull(matched.getProjectName());
        String matchText = StrUtil.trimToNull(result.matchText());
        if (!result.hasAliasEvidence() && (projectName == null || !Objects.equals(normalizeProjectNameAlias(projectName),
                normalizeProjectNameAlias(matchText)))) {
            throw invalidCandidateException(result, "project-name match text does not equal enabled candidate name");
        }
        if (!result.hasAliasEvidence()) {
            long sameNameCount = candidates.stream()
                    .filter(item -> Objects.equals(item.getProjectName(), projectName))
                    .count();
            if (sameNameCount != 1) {
                throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_AMBIGUOUS,
                        "projectName=" + projectName);
            }
        }
        return matched;
    }

    private DccProjectCodeDO validateProjectCodeMatch(DccProjectCodeRecognitionResult result,
                                                      DccProjectCodeDO matched,
                                                      List<DccProjectCodeDO> candidates) {
        String projectCode = StrUtil.trimToNull(matched.getProjectCode());
        String matchText = StrUtil.trimToNull(result.matchText());
        String normalizedProjectCode = normalizeProjectCodeKey(projectCode);
        String normalizedMatchText = normalizeProjectCodeKey(matchText);
        if (normalizedProjectCode == null || !Objects.equals(normalizedProjectCode, normalizedMatchText)) {
            throw invalidCandidateException(result, "project-code match text does not equal enabled candidate code");
        }
        long sameCodeCount = candidates.stream()
                .filter(item -> Objects.equals(normalizeProjectCodeKey(item.getProjectCode()), normalizedProjectCode))
                .count();
        if (sameCodeCount != 1) {
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_AMBIGUOUS,
                    "projectCode=" + projectCode);
        }
        return matched;
    }

    private String normalizeProjectCodeKey(String value) {
        String normalized = StrUtil.trimToNull(value);
        if (normalized == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(normalized.length());
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (isAsciiLetterOrDigit(current)) {
                builder.append(current);
                continue;
            }
            if (current >= '０' && current <= '９') {
                builder.append((char) ('0' + current - '０'));
                continue;
            }
            if (current >= 'Ａ' && current <= 'Ｚ') {
                builder.append((char) ('A' + current - 'Ａ'));
                continue;
            }
            if (current >= 'ａ' && current <= 'ｚ') {
                builder.append((char) ('a' + current - 'ａ'));
            }
        }
        String key = builder.toString().toUpperCase(Locale.ROOT);
        return key.isEmpty() ? null : key;
    }

    private RuntimeException invalidCandidateException(DccProjectCodeRecognitionResult result, String reason) {
        String detail = "reason=" + reason
                + ", projectCodeId=" + result.projectCodeId()
                + ", matchType=" + result.matchType()
                + ", matchText=" + StrUtil.nullToEmpty(result.matchText());
        RuntimeException exception = exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_INVALID_CANDIDATE);
        exception.addSuppressed(new IllegalArgumentException(detail));
        return exception;
    }

    private String normalizeRequiredProjectName(DccProjectCodeDO projectCode) {
        String projectName = StrUtil.trimToNull(projectCode.getProjectName());
        if (projectName == null) {
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_INVALID_CANDIDATE);
        }
        return projectName;
    }

    private void validateDocControlRole(Long userId) {
        if (!permissionApi.hasAnyRoles(userId, DccControlledFileMetadataUpdateService.DOC_CONTROL_ROLE_CODE)) {
            throw exception(CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED);
        }
    }

    private FileDO requireSourceFile(DccControlledFileDO controlledFile) {
        if (controlledFile.getSourceFileId() == null) {
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_SOURCE_MISSING);
        }
        FileDO sourceFile = fileService.getFile(controlledFile.getSourceFileId());
        if (sourceFile == null || sourceFile.getConfigId() == null || StrUtil.isBlank(sourceFile.getPath())) {
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_SOURCE_MISSING);
        }
        return sourceFile;
    }

    private byte[] readSourceContent(FileDO sourceFile) {
        try {
            byte[] content = fileService.getFileContent(sourceFile.getConfigId(), sourceFile.getPath());
            if (ArrayUtil.isEmpty(content)) {
                throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_SOURCE_MISSING);
            }
            return content;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exception(CONTROLLED_FILE_PROJECT_CODE_RECOGNITION_FAILED,
                    "source file content read failed: "
                            + StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()));
        }
    }

    private FileTypeLevels resolveFileTypeLevels(DirectoryContext directoryContext, String sourceFileName) {
        if (directoryContext.segments().stream().anyMatch(this::isQmsRootDirectory)) {
            return new FileTypeLevels(null, "QMS文档", null, null, null, null);
        }
        if (directoryContext.segments().stream().anyMatch(this::isTechnicalRootDirectory)) {
            return resolveTechnicalFileTypeLevels(directoryContext, sourceFileName);
        }
        return new FileTypeLevels(null, null, null, null, null, null);
    }

    private boolean isQmsRootDirectory(String segment) {
        String normalized = normalizeAscii(segment);
        return normalized != null && normalized.contains("QMSDOCUMENTS");
    }

    private boolean isTechnicalRootDirectory(String segment) {
        String normalized = normalizeAscii(segment);
        return normalized != null && (normalized.contains("DMR") || normalized.contains("DHF"));
    }

    private FileTypeLevels resolveTechnicalFileTypeLevels(DirectoryContext directoryContext, String sourceFileName) {
        List<DccFileCategoryDO> categories = categoryMapper.selectList();
        if (CollUtil.isEmpty(categories)) {
            return new FileTypeLevels(null, "技术文档", null, null, null, null);
        }
        DccFileCategoryDO matchedCategory = categories.stream()
                .filter(category -> Boolean.TRUE.equals(category.getActive()))
                .filter(category -> matchesFileCategory(directoryContext, sourceFileName, category))
                .findFirst()
                .orElse(null);
        if (matchedCategory == null) {
            return new FileTypeLevels(null, "技术文档", null, null, null, null);
        }
        if (matchedCategory.getFileTypeTaxonomyId() != null) {
            DccFileTypeTaxonomyPath path =
                    fileTypeTaxonomyAdminService.resolveActivePath(matchedCategory.getFileTypeTaxonomyId());
            return new FileTypeLevels(path.id(), path.level1(), path.level2(), path.level3(),
                    path.level4(), path.level5());
        }
        return new FileTypeLevels(null, "技术文档", null, null, null, null);
    }

    private boolean matchesFileCategory(DirectoryContext directoryContext, String sourceFileName,
                                        DccFileCategoryDO category) {
        String categoryName = StrUtil.trimToNull(category.getName());
        if (categoryName == null) {
            return false;
        }
        String directoryPath = StrUtil.nullToEmpty(directoryContext.path());
        String normalizedCategoryName = normalizeCategoryMatchText(categoryName);
        if (normalizedCategoryName == null) {
            return false;
        }
        return normalizeCategoryMatchText(directoryPath).contains(normalizedCategoryName)
                || normalizeCategoryMatchText(sourceFileName).contains(normalizedCategoryName);
    }

    private String normalizeCategoryMatchText(String value) {
        String normalized = StrUtil.trimToNull(value);
        if (normalized == null) {
            return "";
        }
        return normalized
                .replace("（", "(")
                .replace("）", ")")
                .replace(" ", "")
                .replace("\u3000", "");
    }

    private String normalizeProjectNameAlias(String value) {
        String normalized = StrUtil.trimToNull(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized
                .replace("（", "(")
                .replace("）", ")")
                .replace(" ", "")
                .replace("\u3000", "");
        normalized = normalized.replaceAll("\\([^)]*类\\)", "");
        normalized = normalized.replaceAll("^[0-9]+", "");
        return StrUtil.trimToNull(normalized);
    }

    private String normalizeAscii(String value) {
        String normalized = StrUtil.trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.replace(" ", "").replace(".", "").toUpperCase();
    }

    private record DirectoryContext(String path, List<String> segments) {
    }

    private record ProjectNameRuleMatch(DccProjectCodeDO projectCode, String normalizedProjectName, String matchText) {
    }

    private record ProjectAliasRuleMatch(DccProjectCodeAliasMappingDO mapping, String normalizedAliasText) {
    }

    private record FileTypeLevels(Long taxonomyId, String level1, String level2, String level3,
                                  String level4, String level5) {
    }
}
