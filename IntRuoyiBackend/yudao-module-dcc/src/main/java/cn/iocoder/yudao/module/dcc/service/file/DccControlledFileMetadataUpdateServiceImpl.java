package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMetadataUpdateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccCategoryDirectoryBindingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccCategoryDirectoryBindingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath;
import cn.iocoder.yudao.module.dcc.service.projectcode.assignment.DccProjectCodeAssignmentAuthorization;
import cn.iocoder.yudao.module.dcc.service.projectcode.assignment.DccProjectCodeAssignmentService;
import cn.iocoder.yudao.module.dcc.service.projectcode.assignmentaudit.DccProjectCodeMetadataChangeAuditService;
import cn.iocoder.yudao.module.dcc.service.projectcode.assignmentaudit.DccProjectCodeMetadataChangeCommand;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_CATEGORY_DISABLED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_FILE_NUMBER_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SUBMIT_DIRECTORY_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_DISABLED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_NOT_EXISTS;

@Service
public class DccControlledFileMetadataUpdateServiceImpl implements DccControlledFileMetadataUpdateService {

    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccCategoryDirectoryBindingMapper categoryDirectoryBindingMapper;
    @Resource
    private DccFileDirectoryMapper directoryMapper;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DccFileTypeTaxonomyAdminService fileTypeTaxonomyAdminService;
    @Resource
    private DccProjectCodeAssignmentService projectCodeAssignmentService;
    @Resource
    private DccProjectCodeMetadataChangeAuditService metadataChangeAuditService;
    @Resource
    private DccControlledFilePendingActionGuard pendingActionGuard;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMetadata(Long userId, Long id, DccControlledFileMetadataUpdateReqVO reqVO) {
        boolean docControl = hasDocControlRole(userId);
        if (!docControl && (reqVO == null || reqVO.getAssignmentId() == null)) {
            throw exception(CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED);
        }
        DccProjectCodeAssignmentAuthorization authorization = docControl
                ? null
                : projectCodeAssignmentService.assertMetadataUpdateAllowed(userId, id, reqVO.getAssignmentId());
        NormalizedMetadata metadata = validateAndNormalize(reqVO);

        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (!DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())) {
            throw exception(CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED);
        }
        if (docControl) {
            authorization = DccProjectCodeAssignmentAuthorization.docControlDirect(
                    file.getDccProjectCodeId() == null ? metadata.dccProjectCodeId() : file.getDccProjectCodeId());
        }
        pendingActionGuard.assertNoPendingBusinessAction(file);
        validateCategory(metadata.categoryId());
        Long selectedDirectoryId = resolveSelectedDirectoryId(metadata.categoryId(), metadata.directoryId());

        DccControlledFileMasterDO master = validateMasterLink(file);
        List<DccControlledFileDO> chainFiles = controlledFileMapper.selectListByMasterId(master.getId());
        validateNoTargetChainConflict(master, file, metadata, selectedDirectoryId);
        validateChainContainsFile(chainFiles, file);

        controlledFileMasterMapper.updateById(DccControlledFileMasterDO.builder()
                .id(master.getId())
                .categoryId(metadata.categoryId())
                .directoryId(selectedDirectoryId)
                .fileName(metadata.fileName())
                .fileNumber(metadata.fileNumber())
                .currentActiveControlledFileId(resolveCurrentActiveControlledFileId(master, file))
                .build());
        DccControlledFileDO afterFile = DccControlledFileDO.builder()
                .id(file.getId())
                .masterId(master.getId())
                .categoryId(metadata.categoryId())
                .directoryId(selectedDirectoryId)
                .fileName(metadata.fileName())
                .title(metadata.fileName())
                .fileNumber(metadata.fileNumber())
                .productMasterId(metadata.productMasterId())
                .productCode(metadata.productCode())
                .productName(metadata.productName())
                .dccProjectCodeId(metadata.dccProjectCodeId())
                .needTraining(metadata.needTraining())
                .fileTypeTaxonomyId(metadata.fileTypeTaxonomyId())
                .fileTypeLevel1(metadata.fileTypeLevel1())
                .fileTypeLevel2(metadata.fileTypeLevel2())
                .fileTypeLevel3(metadata.fileTypeLevel3())
                .fileTypeLevel4(metadata.fileTypeLevel4())
                .fileTypeLevel5(metadata.fileTypeLevel5())
                .build();
        controlledFileMapper.updateById(afterFile);
        metadataChangeAuditService.recordMetadataChange(new DccProjectCodeMetadataChangeCommand(
                userId, authorization, file, afterFile, reqVO.getChangeReason()));
    }

    private boolean hasDocControlRole(Long userId) {
        return permissionApi.hasAnyRoles(userId, DOC_CONTROL_ROLE_CODE);
    }

    private void validateDocControlRole(Long userId) {
        if (!hasDocControlRole(userId)) {
            throw exception(CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED);
        }
    }

    private NormalizedMetadata validateAndNormalize(DccControlledFileMetadataUpdateReqVO reqVO) {
        if (reqVO == null || reqVO.getCategoryId() == null
                || reqVO.getNeedTraining() == null || StrUtil.isBlank(reqVO.getFileName())) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        String fileNumber = StrUtil.trimToEmpty(reqVO.getFileNumber());
        DccProjectCodeDO projectCode = resolveEnabledProjectCode(reqVO.getDccProjectCodeId());
        ResolvedFileTypeTaxonomy fileTypeTaxonomy = resolveFileTypeTaxonomy(reqVO);
        FileTypeLevels fileTypeLevels = fileTypeTaxonomy.levels();
        return new NormalizedMetadata(
                null,
                StrUtil.trim(projectCode.getProjectName()),
                StrUtil.trim(reqVO.getFileName()),
                StrUtil.trim(projectCode.getProjectCode()),
                fileNumber,
                reqVO.getCategoryId(),
                reqVO.getDirectoryId(),
                projectCode.getId(),
                reqVO.getNeedTraining(),
                fileTypeTaxonomy.id(),
                fileTypeLevels.level1(),
                fileTypeLevels.level2(),
                fileTypeLevels.level3(),
                fileTypeLevels.level4(),
                fileTypeLevels.level5());
    }

    private DccProjectCodeDO resolveEnabledProjectCode(Long dccProjectCodeId) {
        if (dccProjectCodeId == null) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        DccProjectCodeDO projectCode = projectCodeMapper.selectById(dccProjectCodeId);
        if (projectCode == null) {
            throw exception(PROJECT_CODE_NOT_EXISTS);
        }
        if (!DccProjectCodeStatusConstants.ENABLE.equals(projectCode.getStatus())) {
            throw exception(PROJECT_CODE_DISABLED);
        }
        if (StrUtil.isBlank(projectCode.getProjectCode()) || StrUtil.isBlank(projectCode.getProjectName())) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        return projectCode;
    }

    private FileTypeLevels normalizeFileTypeLevels(DccControlledFileMetadataUpdateReqVO reqVO) {
        return new FileTypeLevels(
                StrUtil.trimToNull(reqVO.getFileTypeLevel1()),
                StrUtil.trimToNull(reqVO.getFileTypeLevel2()),
                StrUtil.trimToNull(reqVO.getFileTypeLevel3()),
                StrUtil.trimToNull(reqVO.getFileTypeLevel4()),
                StrUtil.trimToNull(reqVO.getFileTypeLevel5()));
    }

    private ResolvedFileTypeTaxonomy resolveFileTypeTaxonomy(DccControlledFileMetadataUpdateReqVO reqVO) {
        if (reqVO.getFileTypeTaxonomyId() == null) {
            return new ResolvedFileTypeTaxonomy(null, normalizeFileTypeLevels(reqVO));
        }
        DccFileTypeTaxonomyPath path = fileTypeTaxonomyAdminService.resolveActivePath(reqVO.getFileTypeTaxonomyId());
        return new ResolvedFileTypeTaxonomy(reqVO.getFileTypeTaxonomyId(), new FileTypeLevels(
                path.level1(), path.level2(), path.level3(), path.level4(), path.level5()));
    }

    private void validateCategory(Long categoryId) {
        DccFileCategoryDO category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw exception(FILE_CATEGORY_NOT_EXISTS);
        }
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw exception(CONTROLLED_FILE_CATEGORY_DISABLED);
        }
    }

    private Long resolveSelectedDirectoryId(Long categoryId, Long selectedDirectoryId) {
        DccCategoryDirectoryBindingDO binding = categoryDirectoryBindingMapper.selectActiveByCategoryId(categoryId);
        if (binding == null || binding.getDirectoryId() == null) {
            return DccUploadDirectoryResolver.resolveUnclassifiedUploadDirectory(directoryMapper.selectEnabledList()).getId();
        }
        return validateSelectedDirectory(binding.getDirectoryId(), selectedDirectoryId);
    }

    private Long validateSelectedDirectory(Long bindingDirectoryId, Long selectedDirectoryId) {
        List<DccFileDirectoryDO> directories = directoryMapper.selectEnabledList();
        boolean bindingExists = directories.stream().anyMatch(item -> Objects.equals(item.getId(), bindingDirectoryId));
        if (!bindingExists) {
            throw exception(CONTROLLED_FILE_SUBMIT_DIRECTORY_INVALID);
        }
        Map<Long, List<DccFileDirectoryDO>> childrenByParentId = groupChildrenByParentId(directories);
        LinkedHashSet<Long> subtreeIds = new LinkedHashSet<>();
        collectDirectoryIds(bindingDirectoryId, childrenByParentId, subtreeIds);
        if (subtreeIds.isEmpty() || !subtreeIds.contains(selectedDirectoryId)) {
            throw exception(CONTROLLED_FILE_SUBMIT_DIRECTORY_INVALID);
        }
        return selectedDirectoryId;
    }

    private DccControlledFileMasterDO validateMasterLink(DccControlledFileDO file) {
        if (file.getMasterId() == null) {
            throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
        }
        DccControlledFileMasterDO master = controlledFileMasterMapper.selectById(file.getMasterId());
        if (master == null) {
            throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
        }
        return master;
    }

    private void validateNoTargetChainConflict(DccControlledFileMasterDO currentMaster, DccControlledFileDO file,
                                               NormalizedMetadata metadata, Long selectedDirectoryId) {
        DccControlledFileMasterDO targetMaster = controlledFileMasterMapper.selectByCategoryIdAndDirectoryIdAndFileName(
                metadata.categoryId(), selectedDirectoryId, metadata.fileName());
        if (targetMaster == null || Objects.equals(targetMaster.getId(), currentMaster.getId())) {
            return;
        }
        throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
    }

    private void validateChainContainsFile(List<DccControlledFileDO> chainFiles, DccControlledFileDO file) {
        if (chainFiles.stream().noneMatch(item -> Objects.equals(item.getId(), file.getId()))) {
            throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
        }
    }

    private Long resolveCurrentActiveControlledFileId(DccControlledFileMasterDO master, DccControlledFileDO file) {
        if (DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())) {
            return file.getId();
        }
        return master.getCurrentActiveControlledFileId();
    }

    private Map<Long, List<DccFileDirectoryDO>> groupChildrenByParentId(List<DccFileDirectoryDO> directories) {
        Map<Long, List<DccFileDirectoryDO>> childrenByParentId = new LinkedHashMap<>();
        for (DccFileDirectoryDO directory : directories) {
            childrenByParentId.computeIfAbsent(directory.getParentId(), key -> new java.util.ArrayList<>())
                    .add(directory);
        }
        return childrenByParentId;
    }

    private void collectDirectoryIds(Long directoryId, Map<Long, List<DccFileDirectoryDO>> childrenByParentId,
                                     LinkedHashSet<Long> container) {
        if (directoryId == null || !container.add(directoryId)) {
            return;
        }
        for (DccFileDirectoryDO child : childrenByParentId.getOrDefault(directoryId, List.of())) {
            collectDirectoryIds(child.getId(), childrenByParentId, container);
        }
    }

    private record NormalizedMetadata(Long productMasterId, String productName, String fileName, String productCode,
                                      String fileNumber, Long categoryId, Long directoryId, Long dccProjectCodeId,
                                      Boolean needTraining, Long fileTypeTaxonomyId, String fileTypeLevel1,
                                      String fileTypeLevel2, String fileTypeLevel3, String fileTypeLevel4,
                                      String fileTypeLevel5) {
    }

    private record FileTypeLevels(String level1, String level2, String level3, String level4, String level5) {
    }

    private record ResolvedFileTypeTaxonomy(Long id, FileTypeLevels levels) {
    }

}
