package cn.iocoder.yudao.module.dcc.service.directory;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectoryAccessRuleSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectorySaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccDirectoryAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAccessLogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionRecipientDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMessageJobDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileObsoleteAuditDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileStampDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingProgressDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingViewSessionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureFailureAuditDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccExternalFileReviewDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryDirectoryBindingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccDirectoryAccessRuleMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMessageJobMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileObsoleteAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRouteSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileStampMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingProgressMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingViewSessionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureFailureAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccExternalFileReviewMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentMapper;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_DIRECTORY_DELETE_CONFIRM_TEXT_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_DIRECTORY_DELETE_INFRA_FILE_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_DIRECTORY_DELETE_INFRA_FILE_REFERENCED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_DIRECTORY_DELETE_MASTER_OUT_OF_SCOPE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_DIRECTORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_DIRECTORY_PARENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_DIRECTORY_IMPORT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.INTAUTH_DIRECTORY_IMPORT_SOURCE_INVALID;

@Service
@Validated
public class DccDirectoryAdminServiceImpl implements DccDirectoryAdminService {

    private static final String INTAUTH_IMPORTED_REMARK = "Imported from IntAuth baseline directory snapshot";
    private static final String INTAUTH_CODE_PREFIX = "INTAUTH-";
    private static final String DELETE_SUBTREE_CONFIRM_TEXT = "PROD";
    private static final String MESSAGE_BUSINESS_TYPE_DISTRIBUTION = "DISTRIBUTION";
    private static final String MESSAGE_BUSINESS_TYPE_TRAINING = "TRAINING";
    private static final String MESSAGE_BUSINESS_TYPE_OBSOLETE = "OBSOLETE";

    @Resource
    private DccFileDirectoryMapper directoryMapper;
    @Resource
    private DccDirectoryAccessRuleMapper accessRuleMapper;
    @Resource
    private DccCategoryDirectoryBindingMapper categoryDirectoryBindingMapper;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Resource
    private DccControlledFileAccessLogMapper accessLogMapper;
    @Resource
    private DccControlledFileRouteSnapshotMapper routeSnapshotMapper;
    @Resource
    private DccControlledFileSignatureMapper signatureMapper;
    @Resource
    private DccElectronicSignatureFailureAuditMapper signatureFailureAuditMapper;
    @Resource
    private DccExternalFileReviewMapper externalFileReviewMapper;
    @Resource
    private DccControlledFileStampMapper stampMapper;
    @Resource
    private DccControlledFileObsoleteAuditMapper obsoleteAuditMapper;
    @Resource
    private DccControlledFileDistributionMapper distributionMapper;
    @Resource
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Resource
    private DccControlledFileTrainingMapper trainingMapper;
    @Resource
    private DccControlledFileTrainingAssignmentMapper trainingAssignmentMapper;
    @Resource
    private DccControlledFileTrainingProgressMapper trainingProgressMapper;
    @Resource
    private DccControlledFileTrainingViewSessionMapper trainingViewSessionMapper;
    @Resource
    private DccControlledFileMessageJobMapper messageJobMapper;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private FileService fileService;
    @Resource
    private DccDirectoryAccessPermissionService accessPermissionService;
    @Resource
    private DccProjectCodeAssignmentMapper projectCodeAssignmentMapper;
    @Resource
    private DccProjectCodeAssignmentFileMapper projectCodeAssignmentFileMapper;
    @Resource
    private DccIntAuthDirectoryClient intAuthDirectoryClient;
    @Resource
    private DccDirectoryNasTransferGuardService nasTransferGuardService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDirectory(DccDirectorySaveReqVO reqVO) {
        validateParentExists(reqVO.getParentId());
        DccFileDirectoryDO directory = BeanUtils.toBean(reqVO, DccFileDirectoryDO.class);
        directory.setAccessRuleManuallyBound(Boolean.FALSE);
        directoryMapper.insert(directory);
        return directory.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDirectory(DccDirectorySaveReqVO reqVO) {
        validateDirectoryExists(reqVO.getId());
        validateParentExists(reqVO.getParentId());
        directoryMapper.updateById(BeanUtils.toBean(reqVO, DccFileDirectoryDO.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccDirectoryImportResult importDirectoriesFromIntAuth() {
        if (directoryMapper.selectCount() > 0) {
            throw exception(INTAUTH_DIRECTORY_IMPORT_NOT_ALLOWED);
        }

        List<DccIntAuthDirectoryClient.IntAuthDirectoryNode> sourceNodes = intAuthDirectoryClient.listBaselineDirectories();
        if (sourceNodes.isEmpty()) {
            throw exception(INTAUTH_DIRECTORY_IMPORT_SOURCE_INVALID, "baseline_directory_nodes");
        }

        Map<String, List<DccIntAuthDirectoryClient.IntAuthDirectoryNode>> childrenByParentNodeId = new LinkedHashMap<>();
        for (DccIntAuthDirectoryClient.IntAuthDirectoryNode node : sourceNodes) {
            childrenByParentNodeId.computeIfAbsent(node.parentNodeId(), key -> new ArrayList<>()).add(node);
        }
        List<DccIntAuthDirectoryClient.IntAuthDirectoryNode> roots = childrenByParentNodeId.getOrDefault(null, List.of());
        if (roots.isEmpty()) {
            throw exception(INTAUTH_DIRECTORY_IMPORT_SOURCE_INVALID, "baseline_directory_root");
        }

        int importedCount = 0;
        for (int index = 0; index < roots.size(); index++) {
            importedCount += importSubtree(roots.get(index), null, index + 1, childrenByParentNodeId);
        }
        return new DccDirectoryImportResult(importedCount, roots.size());
    }

    @Override
    public List<DccFileDirectoryDO> getDirectoryTree(Long userId) {
        List<DccFileDirectoryDO> directories = directoryMapper.selectEnabledList();
        Set<Long> assignedDirectoryIds = resolveActiveAssignedDirectoryIds(userId);
        if (assignedDirectoryIds != null) {
            return filterDirectoriesByVisibleIds(directories, assignedDirectoryIds);
        }
        if (accessPermissionService.hasDirectoryManagementPermission(userId)) {
            return directories;
        }
        Set<Long> visibleIds = accessPermissionService.getAuthorizedDirectoryIds(userId, DccAccessTypeEnum.QUERY);
        if (visibleIds.isEmpty()) {
            return List.of();
        }
        Set<Long> keepIds = collectVisibleAndAncestorIds(directories, visibleIds);
        return directories.stream().filter(item -> keepIds.contains(item.getId())).toList();
    }

    @Override
    public List<DccVisibleDirectoryNode> listVisibleChildDirectories(Long userId, Long parentId) {
        Set<Long> assignedDirectoryIds = resolveActiveAssignedDirectoryIds(userId);
        if (assignedDirectoryIds == null && accessPermissionService.hasDirectoryManagementPermission(userId)) {
            return listManagedChildDirectories(parentId);
        }
        List<DccFileDirectoryDO> directories = directoryMapper.selectEnabledList();
        if (!isRootParentId(parentId) && directories.stream().noneMatch(item -> Objects.equals(item.getId(), parentId))) {
            throw exception(FILE_DIRECTORY_NOT_EXISTS);
        }
        VisibleDirectoryContext context = buildVisibleDirectoryContext(userId, directories, assignedDirectoryIds);
        if (!isRootParentId(parentId) && !context.keepIds().contains(parentId)) {
            return List.of();
        }
        return directories.stream()
                .filter(item -> isDirectChildOf(item, parentId))
                .filter(item -> context.keepIds().contains(item.getId()))
                .map(item -> toVisibleDirectoryNode(item, context))
                .toList();
    }

    @Override
    public List<DccVisibleDirectoryNode> searchVisibleDirectories(Long userId, String keyword, Integer limit) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            return List.of();
        }
        int normalizedLimit = normalizeSearchLimit(limit);
        Set<Long> assignedDirectoryIds = resolveActiveAssignedDirectoryIds(userId);
        if (assignedDirectoryIds == null && accessPermissionService.hasDirectoryManagementPermission(userId)) {
            return searchManagedDirectories(normalizedKeyword, normalizedLimit);
        }
        List<DccFileDirectoryDO> directories = directoryMapper.selectEnabledList();
        VisibleDirectoryContext context = buildVisibleDirectoryContext(userId, directories, assignedDirectoryIds);
        return directories.stream()
                .filter(item -> context.keepIds().contains(item.getId()))
                .filter(item -> item.getName().contains(normalizedKeyword) || item.getCode().contains(normalizedKeyword))
                .sorted(Comparator.comparing(DccFileDirectoryDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccFileDirectoryDO::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(normalizedLimit)
                .map(item -> toVisibleDirectoryNode(item, context))
                .toList();
    }

    @Override
    public DccFileDirectoryDO getDirectory(Long userId, Long id) {
        DccFileDirectoryDO directory = validateDirectoryExists(id);
        if (canAccessDirectory(userId, id)) {
            return directory;
        }
        throw exception(FILE_DIRECTORY_NOT_EXISTS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccDirectoryDeleteSubtreeResult deleteDirectorySubtree(Long id, String confirmText) {
        validateDeleteConfirmText(confirmText);
        nasTransferGuardService.assertNoActiveTransfer(id);
        Set<Long> directoryIds = collectDirectorySubtreeIds(id);
        List<DccControlledFileDO> controlledFiles = controlledFileMapper.selectList(
                new LambdaQueryWrapperX<DccControlledFileDO>().in(DccControlledFileDO::getDirectoryId, directoryIds));
        Set<Long> controlledFileIds = controlledFiles.stream()
                .map(DccControlledFileDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> masterIds = validateAndCollectMasterIds(controlledFiles, controlledFileIds);

        List<DccExternalFileReviewDO> externalReviews = listExternalReviews(controlledFileIds);
        List<DccControlledFileStampDO> stamps = listStamps(controlledFileIds);
        Set<Long> infraFileIds = collectInfraFileIds(controlledFiles, externalReviews, stamps);
        validateInfraFilesExist(infraFileIds);
        validateInfraFileReferencesWithinTarget(infraFileIds, controlledFileIds);

        deleteInfraFiles(infraFileIds);
        deleteDirectoryBusinessGraph(directoryIds, controlledFileIds, masterIds);
        return new DccDirectoryDeleteSubtreeResult(
                directoryIds.size(), controlledFileIds.size(), masterIds.size(), infraFileIds.size());
    }

    @Override
    public List<DccDirectoryAccessRuleDirectorySummary> listAccessRuleDirectories() {
        List<DccDirectoryAccessRuleDO> rules = accessRuleMapper.selectList();
        if (rules.isEmpty()) {
            return List.of();
        }
        List<DccFileDirectoryDO> directories = directoryMapper.selectList();
        Map<Long, DccFileDirectoryDO> directoryMap = directories.stream()
                .collect(Collectors.toMap(DccFileDirectoryDO::getId, Function.identity()));
        Set<Long> ruleDirectoryIds = rules.stream()
                .map(DccDirectoryAccessRuleDO::getDirectoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Long directoryId : ruleDirectoryIds) {
            if (!directoryMap.containsKey(directoryId)) {
                throw exception(FILE_DIRECTORY_NOT_EXISTS);
            }
        }
        Set<Long> manualBoundDirectoryIds = ruleDirectoryIds.stream()
                .filter(directoryId -> Boolean.TRUE.equals(directoryMap.get(directoryId).getAccessRuleManuallyBound()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (manualBoundDirectoryIds.isEmpty()) {
            return List.of();
        }
        List<DccDirectoryAccessRuleDirectorySummary> result = new ArrayList<>();
        appendAccessRuleDirectorySummaries(listRootDirectories(directories), directoryMap, manualBoundDirectoryIds, "", result);
        return result;
    }

    @Override
    public List<DccDirectoryAccessRuleDO> getAccessRules(Long directoryId) {
        validateDirectoryExists(directoryId);
        return accessRuleMapper.selectList(DccDirectoryAccessRuleDO::getDirectoryId, directoryId).stream()
                .peek(this::normalizeMergedReadPermission)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccessRules(Long directoryId) {
        DccFileDirectoryDO directory = validateDirectoryExists(directoryId);
        accessRuleMapper.delete(DccDirectoryAccessRuleDO::getDirectoryId, directoryId);
        if (!Boolean.FALSE.equals(directory.getAccessRuleManuallyBound())) {
            directory.setAccessRuleManuallyBound(Boolean.FALSE);
            directoryMapper.updateById(directory);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceAccessRules(Long directoryId, List<DccDirectoryAccessRuleSaveReqVO> rules) {
        DccFileDirectoryDO directory = validateDirectoryExists(directoryId);
        accessRuleMapper.delete(DccDirectoryAccessRuleDO::getDirectoryId, directoryId);
        List<DccDirectoryAccessRuleDO> entities = convertList(rules, rule -> {
            DccDirectoryAccessRuleDO entity = BeanUtils.toBean(rule, DccDirectoryAccessRuleDO.class);
            entity.setDirectoryId(directoryId);
            normalizeMergedReadPermission(entity);
            return entity;
        });
        entities.forEach(accessRuleMapper::insert);
        if (!Boolean.TRUE.equals(directory.getAccessRuleManuallyBound())) {
            directory.setAccessRuleManuallyBound(Boolean.TRUE);
            directoryMapper.updateById(directory);
        }
    }

    private void validateParentExists(Long parentId) {
        if (parentId != null && directoryMapper.selectById(parentId) == null) {
            throw exception(FILE_DIRECTORY_PARENT_NOT_EXISTS);
        }
    }

    private void normalizeMergedReadPermission(DccDirectoryAccessRuleDO rule) {
        if (rule == null) {
            return;
        }
        boolean mergedReadAllowed = Boolean.TRUE.equals(rule.getCanQuery()) || Boolean.TRUE.equals(rule.getCanPreview());
        rule.setCanQuery(mergedReadAllowed);
        rule.setCanPreview(mergedReadAllowed);
    }

    private DccFileDirectoryDO validateDirectoryExists(Long id) {
        DccFileDirectoryDO directory = directoryMapper.selectById(id);
        if (directory == null) {
            throw exception(FILE_DIRECTORY_NOT_EXISTS);
        }
        return directory;
    }

    private void validateDeleteConfirmText(String confirmText) {
        String normalized = confirmText == null ? null : confirmText.trim();
        if (!DELETE_SUBTREE_CONFIRM_TEXT.equals(normalized)) {
            throw exception(FILE_DIRECTORY_DELETE_CONFIRM_TEXT_INVALID);
        }
    }

    private Set<Long> collectDirectorySubtreeIds(Long rootId) {
        validateDirectoryExists(rootId);
        List<DccFileDirectoryDO> directories = directoryMapper.selectList(
                new LambdaQueryWrapperX<DccFileDirectoryDO>()
                        .orderByAsc(DccFileDirectoryDO::getSort)
                        .orderByDesc(DccFileDirectoryDO::getId));
        Map<Long, List<DccFileDirectoryDO>> childrenByParentId = directories.stream()
                .filter(item -> item.getParentId() != null)
                .collect(Collectors.groupingBy(DccFileDirectoryDO::getParentId, LinkedHashMap::new, Collectors.toList()));
        Set<Long> directoryIds = new LinkedHashSet<>();
        List<Long> pendingIds = new ArrayList<>();
        pendingIds.add(rootId);
        while (!pendingIds.isEmpty()) {
            Long currentId = pendingIds.remove(0);
            if (!directoryIds.add(currentId)) {
                continue;
            }
            childrenByParentId.getOrDefault(currentId, List.of()).stream()
                    .map(DccFileDirectoryDO::getId)
                    .forEach(pendingIds::add);
        }
        return directoryIds;
    }

    private Set<Long> validateAndCollectMasterIds(List<DccControlledFileDO> controlledFiles, Set<Long> controlledFileIds) {
        Set<Long> masterIds = new LinkedHashSet<>();
        for (DccControlledFileDO file : controlledFiles) {
            if (file.getMasterId() == null) {
                throw exception(FILE_DIRECTORY_DELETE_MASTER_OUT_OF_SCOPE);
            }
            masterIds.add(file.getMasterId());
        }
        if (masterIds.isEmpty()) {
            return masterIds;
        }
        Set<Long> existingMasterIds = controlledFileMasterMapper.selectBatchIds(masterIds).stream()
                .map(DccControlledFileMasterDO::getId)
                .collect(Collectors.toSet());
        if (!existingMasterIds.containsAll(masterIds)) {
            throw exception(FILE_DIRECTORY_DELETE_MASTER_OUT_OF_SCOPE);
        }
        for (Long masterId : masterIds) {
            boolean chainCrossesSubtree = controlledFileMapper.selectListByMasterId(masterId).stream()
                    .map(DccControlledFileDO::getId)
                    .anyMatch(fileId -> !controlledFileIds.contains(fileId));
            if (chainCrossesSubtree) {
                throw exception(FILE_DIRECTORY_DELETE_MASTER_OUT_OF_SCOPE);
            }
        }
        return masterIds;
    }

    private List<DccExternalFileReviewDO> listExternalReviews(Set<Long> controlledFileIds) {
        if (controlledFileIds.isEmpty()) {
            return List.of();
        }
        return externalFileReviewMapper.selectList(
                new LambdaQueryWrapperX<DccExternalFileReviewDO>()
                        .in(DccExternalFileReviewDO::getControlledFileId, controlledFileIds));
    }

    private List<DccControlledFileStampDO> listStamps(Set<Long> controlledFileIds) {
        if (controlledFileIds.isEmpty()) {
            return List.of();
        }
        return stampMapper.selectList(
                new LambdaQueryWrapperX<DccControlledFileStampDO>()
                        .in(DccControlledFileStampDO::getControlledFileId, controlledFileIds));
    }

    private Set<Long> collectInfraFileIds(List<DccControlledFileDO> controlledFiles,
                                          List<DccExternalFileReviewDO> externalReviews,
                                          List<DccControlledFileStampDO> stamps) {
        Set<Long> fileIds = new LinkedHashSet<>();
        controlledFiles.forEach(file -> {
            addFileId(fileIds, file.getSourceFileId());
            addFileId(fileIds, file.getOriginalFileId());
            addFileId(fileIds, file.getDrawingPdfFileId());
            addFileId(fileIds, file.getTrainingRecordFileId());
            addFileId(fileIds, file.getPublishedFileId());
            addFileId(fileIds, file.getStampedFileId());
        });
        externalReviews.forEach(review -> addFileId(fileIds, review.getOutputFileId()));
        stamps.forEach(stamp -> {
            addFileId(fileIds, stamp.getSourceFileId());
            addFileId(fileIds, stamp.getOutputFileId());
        });
        return fileIds;
    }

    private void addFileId(Set<Long> fileIds, Long fileId) {
        if (fileId != null) {
            fileIds.add(fileId);
        }
    }

    private void validateInfraFilesExist(Set<Long> infraFileIds) {
        if (infraFileIds.isEmpty()) {
            return;
        }
        Set<Long> existingFileIds = fileMapper.selectBatchIds(infraFileIds).stream()
                .map(FileDO::getId)
                .collect(Collectors.toSet());
        if (!existingFileIds.containsAll(infraFileIds)) {
            Set<Long> missingFileIds = infraFileIds.stream()
                    .filter(fileId -> !existingFileIds.contains(fileId))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            throw exception(FILE_DIRECTORY_DELETE_INFRA_FILE_MISSING);
        }
    }

    private void validateInfraFileReferencesWithinTarget(Set<Long> infraFileIds, Set<Long> controlledFileIds) {
        if (infraFileIds.isEmpty()) {
            return;
        }
        if (!controlledFileMapper.selectList(buildOutsideControlledFileReferenceQuery(infraFileIds, controlledFileIds)).isEmpty()
                || hasOutsideExternalReviewReference(infraFileIds, controlledFileIds)
                || hasOutsideStampReference(infraFileIds, controlledFileIds)) {
            throw exception(FILE_DIRECTORY_DELETE_INFRA_FILE_REFERENCED);
        }
    }

    private LambdaQueryWrapperX<DccControlledFileDO> buildOutsideControlledFileReferenceQuery(Set<Long> infraFileIds,
                                                                                              Set<Long> controlledFileIds) {
        LambdaQueryWrapperX<DccControlledFileDO> query = new LambdaQueryWrapperX<>();
        query.notIn(!controlledFileIds.isEmpty(), DccControlledFileDO::getId, controlledFileIds);
        query.and(wrapper -> wrapper.in(DccControlledFileDO::getSourceFileId, infraFileIds)
                .or().in(DccControlledFileDO::getOriginalFileId, infraFileIds)
                .or().in(DccControlledFileDO::getDrawingPdfFileId, infraFileIds)
                .or().in(DccControlledFileDO::getTrainingRecordFileId, infraFileIds)
                .or().in(DccControlledFileDO::getPublishedFileId, infraFileIds)
                .or().in(DccControlledFileDO::getStampedFileId, infraFileIds));
        return query;
    }

    private boolean hasOutsideExternalReviewReference(Set<Long> infraFileIds, Set<Long> controlledFileIds) {
        LambdaQueryWrapperX<DccExternalFileReviewDO> query = new LambdaQueryWrapperX<>();
        query.notIn(!controlledFileIds.isEmpty(), DccExternalFileReviewDO::getControlledFileId, controlledFileIds);
        query.in(DccExternalFileReviewDO::getOutputFileId, infraFileIds);
        return !externalFileReviewMapper.selectList(query).isEmpty();
    }

    private boolean hasOutsideStampReference(Set<Long> infraFileIds, Set<Long> controlledFileIds) {
        LambdaQueryWrapperX<DccControlledFileStampDO> query = new LambdaQueryWrapperX<>();
        query.notIn(!controlledFileIds.isEmpty(), DccControlledFileStampDO::getControlledFileId, controlledFileIds);
        query.and(wrapper -> wrapper.in(DccControlledFileStampDO::getSourceFileId, infraFileIds)
                .or().in(DccControlledFileStampDO::getOutputFileId, infraFileIds));
        return !stampMapper.selectList(query).isEmpty();
    }

    private void deleteInfraFiles(Set<Long> infraFileIds) {
        if (infraFileIds.isEmpty()) {
            return;
        }
        try {
            fileService.deleteFileList(new ArrayList<>(infraFileIds));
        } catch (Exception ex) {
            throw new IllegalStateException("DCC directory subtree upload file deletion failed", ex);
        }
    }

    private void deleteDirectoryBusinessGraph(Set<Long> directoryIds,
                                              Set<Long> controlledFileIds,
                                              Set<Long> masterIds) {
        if (!controlledFileIds.isEmpty()) {
            deleteControlledFileChildren(controlledFileIds);
            controlledFileMapper.deleteBatch(DccControlledFileDO::getId, controlledFileIds);
        }
        controlledFileMasterMapper.deleteBatch(DccControlledFileMasterDO::getId, masterIds);
        accessRuleMapper.deleteBatch(DccDirectoryAccessRuleDO::getDirectoryId, directoryIds);
        categoryDirectoryBindingMapper.deleteBatch(DccCategoryDirectoryBindingDO::getDirectoryId, directoryIds);
        directoryMapper.deleteBatch(DccFileDirectoryDO::getId, directoryIds);
    }

    private void deleteControlledFileChildren(Set<Long> controlledFileIds) {
        List<DccControlledFileDistributionDO> distributions = distributionMapper.selectList(
                new LambdaQueryWrapperX<DccControlledFileDistributionDO>()
                        .in(DccControlledFileDistributionDO::getControlledFileId, controlledFileIds));
        Set<Long> distributionIds = distributions.stream()
                .map(DccControlledFileDistributionDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<DccControlledFileTrainingDO> trainings = trainingMapper.selectList(
                new LambdaQueryWrapperX<DccControlledFileTrainingDO>()
                        .in(DccControlledFileTrainingDO::getControlledFileId, controlledFileIds));
        Set<Long> trainingIds = trainings.stream()
                .map(DccControlledFileTrainingDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<DccControlledFileTrainingProgressDO> trainingProgresses = trainingProgressMapper.selectList(
                new LambdaQueryWrapperX<DccControlledFileTrainingProgressDO>()
                        .in(DccControlledFileTrainingProgressDO::getControlledFileId, controlledFileIds));
        Set<Long> trainingProgressIds = trainingProgresses.stream()
                .map(DccControlledFileTrainingProgressDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        deleteMessageJobs(MESSAGE_BUSINESS_TYPE_DISTRIBUTION, distributionIds);
        deleteMessageJobs(MESSAGE_BUSINESS_TYPE_TRAINING, trainingIds);
        deleteMessageJobs(MESSAGE_BUSINESS_TYPE_OBSOLETE, controlledFileIds);
        distributionRecipientMapper.deleteBatch(DccControlledFileDistributionRecipientDO::getDistributionId, distributionIds);
        distributionMapper.deleteBatch(DccControlledFileDistributionDO::getId, distributionIds);
        trainingAssignmentMapper.deleteBatch(DccControlledFileTrainingAssignmentDO::getTrainingId, trainingIds);
        trainingViewSessionMapper.deleteBatch(DccControlledFileTrainingViewSessionDO::getTrainingProgressId, trainingProgressIds);
        trainingProgressMapper.deleteBatch(DccControlledFileTrainingProgressDO::getId, trainingProgressIds);
        trainingMapper.deleteBatch(DccControlledFileTrainingDO::getId, trainingIds);
        routeSnapshotMapper.deleteBatch(DccControlledFileRouteSnapshotDO::getControlledFileId, controlledFileIds);
        signatureMapper.deleteBatch(DccControlledFileSignatureDO::getControlledFileId, controlledFileIds);
        signatureFailureAuditMapper.deleteBatch(DccElectronicSignatureFailureAuditDO::getControlledFileId, controlledFileIds);
        stampMapper.deleteBatch(DccControlledFileStampDO::getControlledFileId, controlledFileIds);
        externalFileReviewMapper.deleteBatch(DccExternalFileReviewDO::getControlledFileId, controlledFileIds);
        obsoleteAuditMapper.deleteBatch(DccControlledFileObsoleteAuditDO::getControlledFileId, controlledFileIds);
        accessLogMapper.deleteBatch(DccControlledFileAccessLogDO::getControlledFileId, controlledFileIds);
    }

    private void deleteMessageJobs(String businessType, Collection<Long> businessIds) {
        if (businessIds.isEmpty()) {
            return;
        }
        messageJobMapper.delete(new LambdaQueryWrapperX<DccControlledFileMessageJobDO>()
                .eq(DccControlledFileMessageJobDO::getBusinessType, businessType)
                .in(DccControlledFileMessageJobDO::getBusinessId, businessIds));
    }

    private int importSubtree(DccIntAuthDirectoryClient.IntAuthDirectoryNode currentNode,
                              Long localParentId,
                              int sort,
                              Map<String, List<DccIntAuthDirectoryClient.IntAuthDirectoryNode>> childrenByParentNodeId) {
        DccFileDirectoryDO directory = DccFileDirectoryDO.builder()
                .parentId(localParentId)
                .code(buildIntAuthCode(currentNode.nodeId()))
                .name(currentNode.name())
                .active(Boolean.TRUE)
                .sort(sort)
                .remark(INTAUTH_IMPORTED_REMARK)
                .accessRuleManuallyBound(Boolean.FALSE)
                .build();
        directoryMapper.insert(directory);

        int importedCount = 1;
        List<DccIntAuthDirectoryClient.IntAuthDirectoryNode> children = childrenByParentNodeId
                .getOrDefault(currentNode.nodeId(), List.of());
        for (int index = 0; index < children.size(); index++) {
            importedCount += importSubtree(children.get(index), directory.getId(), index + 1, childrenByParentNodeId);
        }
        return importedCount;
    }

    private Set<Long> collectVisibleAndAncestorIds(List<DccFileDirectoryDO> directories, Set<Long> visibleIds) {
        Map<Long, DccFileDirectoryDO> directoryMap = directories.stream()
                .collect(Collectors.toMap(DccFileDirectoryDO::getId, Function.identity()));
        Set<Long> keepIds = new HashSet<>(visibleIds);
        for (Long visibleId : visibleIds) {
            DccFileDirectoryDO current = directoryMap.get(visibleId);
            while (current != null && !isRootParentId(current.getParentId())) {
                keepIds.add(current.getParentId());
                current = directoryMap.get(current.getParentId());
            }
        }
        return keepIds;
    }

    private List<DccFileDirectoryDO> filterDirectoriesByVisibleIds(List<DccFileDirectoryDO> directories,
                                                                   Set<Long> visibleIds) {
        if (visibleIds.isEmpty()) {
            return List.of();
        }
        Set<Long> keepIds = collectVisibleAndAncestorIds(directories, visibleIds);
        Map<Long, DccFileDirectoryDO> keptDirectoryMap = directories.stream()
                .filter(item -> keepIds.contains(item.getId()))
                .collect(Collectors.toMap(DccFileDirectoryDO::getId, Function.identity(),
                        (first, second) -> first, LinkedHashMap::new));
        List<DccFileDirectoryDO> orderedDirectories = new ArrayList<>();
        appendVisibleDirectoryTree(listRootDirectories(new ArrayList<>(keptDirectoryMap.values())),
                keptDirectoryMap, orderedDirectories);
        return orderedDirectories;
    }

    private void appendVisibleDirectoryTree(List<DccFileDirectoryDO> nodes,
                                            Map<Long, DccFileDirectoryDO> directoryMap,
                                            List<DccFileDirectoryDO> result) {
        for (DccFileDirectoryDO node : nodes) {
            result.add(node);
            List<DccFileDirectoryDO> children = directoryMap.values().stream()
                    .filter(item -> Objects.equals(item.getParentId(), node.getId()))
                    .sorted(Comparator.comparing(DccFileDirectoryDO::getSort, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(DccFileDirectoryDO::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
            appendVisibleDirectoryTree(children, directoryMap, result);
        }
    }

    private VisibleDirectoryContext buildVisibleDirectoryContext(Long userId, List<DccFileDirectoryDO> directories) {
        return buildVisibleDirectoryContext(userId, directories, resolveActiveAssignedDirectoryIds(userId));
    }

    private VisibleDirectoryContext buildVisibleDirectoryContext(Long userId, List<DccFileDirectoryDO> directories,
                                                                 Set<Long> assignedDirectoryIds) {
        Set<Long> keepIds;
        if (assignedDirectoryIds != null) {
            keepIds = assignedDirectoryIds.isEmpty() ? Set.of() : collectVisibleAndAncestorIds(directories, assignedDirectoryIds);
        } else if (accessPermissionService.hasDirectoryManagementPermission(userId)) {
            keepIds = directories.stream().map(DccFileDirectoryDO::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            Set<Long> visibleIds = accessPermissionService.getAuthorizedDirectoryIds(userId, DccAccessTypeEnum.QUERY);
            if (visibleIds.isEmpty()) {
                keepIds = Set.of();
            } else {
                keepIds = collectVisibleAndAncestorIds(directories, visibleIds);
            }
        }
        Map<Long, DccFileDirectoryDO> directoryMap = directories.stream()
                .collect(Collectors.toMap(DccFileDirectoryDO::getId, Function.identity()));
        Map<Long, List<DccFileDirectoryDO>> childrenByParentId = directories.stream()
                .filter(item -> item.getParentId() != null)
                .collect(Collectors.groupingBy(DccFileDirectoryDO::getParentId, LinkedHashMap::new, Collectors.toList()));
        return new VisibleDirectoryContext(keepIds, directoryMap, childrenByParentId);
    }

    private boolean canAccessDirectory(Long userId, Long directoryId) {
        List<DccFileDirectoryDO> directories = directoryMapper.selectEnabledList();
        if (directories.stream().noneMatch(item -> Objects.equals(item.getId(), directoryId))) {
            return false;
        }
        return buildVisibleDirectoryContext(userId, directories).keepIds().contains(directoryId);
    }

    private Set<Long> resolveActiveAssignedDirectoryIds(Long userId) {
        if (userId == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        if (projectCodeAssignmentMapper.selectActiveProjectCodeIdsByAssigneeUserId(userId, now).isEmpty()) {
            return null;
        }
        return new LinkedHashSet<>(projectCodeAssignmentFileMapper.selectActiveDirectoryIdsByAssigneeUserId(userId, now));
    }

    private List<DccVisibleDirectoryNode> listManagedChildDirectories(Long parentId) {
        String parentPath = "";
        if (!isRootParentId(parentId)) {
            DccFileDirectoryDO parent = validateActiveDirectoryExists(parentId);
            parentPath = buildManagedDirectoryPath(parent);
        }
        List<DccFileDirectoryDO> children = directoryMapper.selectEnabledListByParentId(parentId);
        Set<Long> childIdsWithChildren = listEnabledParentIds(children);
        String resolvedParentPath = parentPath;
        return children.stream()
                .map(child -> new DccVisibleDirectoryNode(child, childIdsWithChildren.contains(child.getId()),
                        resolvedParentPath.isEmpty() ? child.getName() : resolvedParentPath + "/" + child.getName()))
                .toList();
    }

    private List<DccVisibleDirectoryNode> searchManagedDirectories(String keyword, int limit) {
        List<DccFileDirectoryDO> matches = directoryMapper.selectEnabledListByKeyword(keyword, limit);
        Set<Long> idsWithChildren = listEnabledParentIds(matches);
        return matches.stream()
                .map(directory -> new DccVisibleDirectoryNode(directory, idsWithChildren.contains(directory.getId()),
                        buildManagedDirectoryPath(directory)))
                .toList();
    }

    private Set<Long> listEnabledParentIds(List<DccFileDirectoryDO> directories) {
        Set<Long> childIds = directories.stream()
                .map(DccFileDirectoryDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new HashSet<>(directoryMapper.selectEnabledParentIdsByParentIds(childIds));
    }

    private DccFileDirectoryDO validateActiveDirectoryExists(Long id) {
        DccFileDirectoryDO directory = directoryMapper.selectById(id);
        if (directory == null || !Boolean.TRUE.equals(directory.getActive())) {
            throw exception(FILE_DIRECTORY_NOT_EXISTS);
        }
        return directory;
    }

    private String buildManagedDirectoryPath(DccFileDirectoryDO directory) {
        List<String> segments = new ArrayList<>();
        DccFileDirectoryDO current = directory;
        while (current != null) {
            if (!Boolean.TRUE.equals(current.getActive())) {
                throw exception(FILE_DIRECTORY_NOT_EXISTS);
            }
            segments.add(current.getName());
            current = isRootParentId(current.getParentId()) ? null : directoryMapper.selectById(current.getParentId());
            if (current != null && current.getId() == null) {
                throw exception(FILE_DIRECTORY_NOT_EXISTS);
            }
        }
        java.util.Collections.reverse(segments);
        return String.join("/", segments);
    }

    private DccVisibleDirectoryNode toVisibleDirectoryNode(DccFileDirectoryDO directory,
                                                          VisibleDirectoryContext context) {
        boolean hasVisibleChildren = context.childrenByParentId().getOrDefault(directory.getId(), List.of()).stream()
                .anyMatch(child -> context.keepIds().contains(child.getId()));
        return new DccVisibleDirectoryNode(directory, hasVisibleChildren, buildDirectoryPath(directory.getId(), context.directoryMap()));
    }

    private String buildDirectoryPath(Long directoryId, Map<Long, DccFileDirectoryDO> directoryMap) {
        List<String> segments = new ArrayList<>();
        DccFileDirectoryDO current = directoryMap.get(directoryId);
        while (current != null) {
            segments.add(current.getName());
            current = isRootParentId(current.getParentId()) ? null : directoryMap.get(current.getParentId());
        }
        java.util.Collections.reverse(segments);
        return String.join("/", segments);
    }

    private List<DccFileDirectoryDO> listRootDirectories(List<DccFileDirectoryDO> directories) {
        return directories.stream()
                .filter(item -> isRootParentId(item.getParentId()))
                .sorted(Comparator.comparing(DccFileDirectoryDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccFileDirectoryDO::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private void appendAccessRuleDirectorySummaries(List<DccFileDirectoryDO> nodes,
                                                    Map<Long, DccFileDirectoryDO> directoryMap,
                                                    Set<Long> ruleDirectoryIds,
                                                    String parentPath,
                                                    List<DccDirectoryAccessRuleDirectorySummary> result) {
        for (DccFileDirectoryDO node : nodes) {
            String currentPath = parentPath.isEmpty() ? node.getName() : parentPath + "/" + node.getName();
            if (ruleDirectoryIds.contains(node.getId())) {
                result.add(new DccDirectoryAccessRuleDirectorySummary(node.getId(), node.getName(), currentPath));
            }
            List<DccFileDirectoryDO> children = directoryMap.values().stream()
                    .filter(item -> Objects.equals(item.getParentId(), node.getId()))
                    .sorted(Comparator.comparing(DccFileDirectoryDO::getSort, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(DccFileDirectoryDO::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
            appendAccessRuleDirectorySummaries(children, directoryMap, ruleDirectoryIds, currentPath, result);
        }
    }

    private boolean isDirectChildOf(DccFileDirectoryDO directory, Long parentId) {
        if (isRootParentId(parentId)) {
            return isRootParentId(directory.getParentId());
        }
        return Objects.equals(directory.getParentId(), parentId);
    }

    private boolean isRootParentId(Long parentId) {
        return parentId == null || Objects.equals(parentId, 0L);
    }

    private int normalizeSearchLimit(Integer limit) {
        if (limit == null) {
            return 50;
        }
        return Math.max(1, Math.min(limit, 100));
    }

    private record VisibleDirectoryContext(Set<Long> keepIds, Map<Long, DccFileDirectoryDO> directoryMap,
                                           Map<Long, List<DccFileDirectoryDO>> childrenByParentId) {
    }

    private String buildIntAuthCode(String sourceNodeId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] encoded = digest.digest(sourceNodeId.getBytes(StandardCharsets.UTF_8));
            return INTAUTH_CODE_PREFIX + HexFormat.of().formatHex(encoded).toUpperCase();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-1 algorithm unavailable", ex);
        }
    }
}
