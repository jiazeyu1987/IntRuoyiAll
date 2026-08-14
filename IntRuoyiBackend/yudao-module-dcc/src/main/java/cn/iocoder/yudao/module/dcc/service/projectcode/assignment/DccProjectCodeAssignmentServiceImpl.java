package cn.iocoder.yudao.module.dcc.service.projectcode.assignment;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentCreateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentRevokeReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeAssignmentFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileMetadataUpdateService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.module.dcc.enums.DccProjectCodeAssignmentConstants.SCOPE_PROJECT_CODE_CURRENT_FILES;
import static cn.iocoder.yudao.module.dcc.enums.DccProjectCodeAssignmentConstants.SCOPE_SELECTED_FILES;
import static cn.iocoder.yudao.module.dcc.enums.DccProjectCodeAssignmentConstants.STATUS_ACTIVE;
import static cn.iocoder.yudao.module.dcc.enums.DccProjectCodeAssignmentConstants.STATUS_REVOKED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSIGNMENT_ASSIGNEE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSIGNMENT_ASSIGNEE_PERMISSION_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSIGNMENT_FILE_SCOPE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSIGNMENT_INACTIVE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSIGNMENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSIGNMENT_REVOKE_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSIGNMENT_SCOPE_EMPTY;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_NOT_EXISTS;

@Service
public class DccProjectCodeAssignmentServiceImpl implements DccProjectCodeAssignmentService {

    private static final DateTimeFormatter ASSIGNMENT_NO_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String ASSIGNMENT_EXECUTE_PERMISSION = "dcc:project-code-assignment:execute";

    @Resource
    private DccProjectCodeAssignmentMapper assignmentMapper;
    @Resource
    private DccProjectCodeAssignmentFileMapper assignmentFileMapper;
    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccProjectCodeAssignmentRespVO createAssignment(Long operatorUserId, Long projectCodeId,
                                                           DccProjectCodeAssignmentCreateReqVO reqVO) {
        DccProjectCodeDO projectCode = validateProjectCode(projectCodeId);
        validateAssignee(reqVO.getAssigneeUserId());
        List<Long> selectedFileIds = normalizeSelectedFileIds(reqVO);
        List<DccControlledFileDO> files = SCOPE_SELECTED_FILES.equals(reqVO.getScopeMode())
                ? controlledFileMapper.selectCurrentApprovedFilesByIds(selectedFileIds)
                : controlledFileMapper.selectAssociatedFilesByProjectCodeId(projectCodeId, null);
        validateScope(reqVO, selectedFileIds, files);

        LocalDateTime now = LocalDateTime.now();
        DccProjectCodeAssignmentDO assignment = DccProjectCodeAssignmentDO.builder()
                .assignmentNo(createAssignmentNo())
                .projectCodeId(projectCodeId)
                .scopeMode(reqVO.getScopeMode())
                .assigneeUserId(reqVO.getAssigneeUserId())
                .assignedBy(operatorUserId)
                .assignedTime(now)
                .expireTime(reqVO.getExpireTime())
                .status(STATUS_ACTIVE)
                .assignmentReason(StrUtil.trimToNull(reqVO.getAssignmentReason()))
                .fileCount(files.size())
                .changedFileCount(0)
                .changedFieldCount(0)
                .build();
        assignmentMapper.insert(assignment);
        for (DccControlledFileDO file : files) {
            assignmentFileMapper.insert(toAssignmentFile(assignment.getId(), projectCodeId, file));
        }
        return toRespVO(assignment, Map.of(projectCode.getId(), projectCode), selectUserMap(List.of(reqVO.getAssigneeUserId())));
    }

    @Override
    public PageResult<DccProjectCodeAssignmentRespVO> getProjectCodeAssignmentPage(Long projectCodeId,
                                                                                   DccProjectCodeAssignmentPageReqVO reqVO) {
        validateProjectCode(projectCodeId);
        return toAssignmentRespPage(assignmentMapper.selectPage(projectCodeId, null, reqVO));
    }

    @Override
    public PageResult<DccProjectCodeAssignmentRespVO> getMyAssignmentPage(Long userId,
                                                                          DccProjectCodeAssignmentPageReqVO reqVO) {
        return toAssignmentRespPage(assignmentMapper.selectPage(null, userId, reqVO));
    }

    @Override
    public PageResult<DccProjectCodeAssignmentFileRespVO> getAssignmentFilePage(Long userId, Long assignmentId,
                                                                                DccProjectCodeAssignmentFilePageReqVO reqVO) {
        DccProjectCodeAssignmentDO assignment = validateReadableAssignment(userId, assignmentId);
        PageResult<DccProjectCodeAssignmentFileDO> pageResult = assignmentFileMapper.selectPage(assignmentId, reqVO);
        Map<Long, DccControlledFileDO> fileMap = selectLatestApprovedFileMapByMasterId(pageResult.getList());
        boolean editable = isDocControl(userId) || isAssignmentActiveForUser(assignment, userId);
        List<DccProjectCodeAssignmentFileRespVO> list = pageResult.getList().stream()
                .map(item -> toAssignmentFileRespVO(item, fileMap.get(item.getMasterId()), editable))
                .toList();
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeAssignment(Long operatorUserId, Long assignmentId, DccProjectCodeAssignmentRevokeReqVO reqVO) {
        DccProjectCodeAssignmentDO assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw exception(PROJECT_CODE_ASSIGNMENT_NOT_EXISTS);
        }
        if (!STATUS_ACTIVE.equals(assignment.getStatus())) {
            throw exception(PROJECT_CODE_ASSIGNMENT_REVOKE_NOT_ALLOWED);
        }
        assignmentMapper.updateById(DccProjectCodeAssignmentDO.builder()
                .id(assignmentId)
                .status(STATUS_REVOKED)
                .revokedBy(operatorUserId)
                .revokedTime(LocalDateTime.now())
                .revokeReason(StrUtil.trimToNull(reqVO.getRevokeReason()))
                .build());
    }

    @Override
    public DccProjectCodeAssignmentAuthorization assertMetadataUpdateAllowed(Long userId, Long fileId,
                                                                            Long assignmentId) {
        if (assignmentId == null) {
            throw exception(CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED);
        }
        DccProjectCodeAssignmentDO assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw exception(PROJECT_CODE_ASSIGNMENT_NOT_EXISTS);
        }
        if (!Objects.equals(assignment.getAssigneeUserId(), userId)) {
            throw exception(CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED);
        }
        if (!isAssignmentActive(assignment)) {
            throw exception(PROJECT_CODE_ASSIGNMENT_INACTIVE);
        }
        resolveCurrentApprovedAssignmentFile(assignmentId, fileId);
        return DccProjectCodeAssignmentAuthorization.assignedUser(assignmentId, assignment.getProjectCodeId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAssignmentFileChanged(Long assignmentId, Long controlledFileId, int changedFieldCount) {
        if (assignmentId == null) {
            return;
        }
        DccProjectCodeAssignmentFileDO assignmentFile =
                resolveCurrentApprovedAssignmentFile(assignmentId, controlledFileId);
        boolean alreadyChanged = Boolean.TRUE.equals(assignmentFile.getChanged());
        int totalFileChangedFieldCount = safeInt(assignmentFile.getChangedFieldCount()) + changedFieldCount;
        assignmentFileMapper.updateById(DccProjectCodeAssignmentFileDO.builder()
                .id(assignmentFile.getId())
                .changed(Boolean.TRUE)
                .changedFieldCount(totalFileChangedFieldCount)
                .lastChangedTime(LocalDateTime.now())
                .build());

        DccProjectCodeAssignmentDO assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw exception(PROJECT_CODE_ASSIGNMENT_NOT_EXISTS);
        }
        assignmentMapper.updateById(DccProjectCodeAssignmentDO.builder()
                .id(assignmentId)
                .changedFileCount(safeInt(assignment.getChangedFileCount()) + (alreadyChanged ? 0 : 1))
                .changedFieldCount(safeInt(assignment.getChangedFieldCount()) + changedFieldCount)
                .build());
    }

    private DccProjectCodeAssignmentFileDO resolveCurrentApprovedAssignmentFile(Long assignmentId,
                                                                                Long controlledFileId) {
        DccControlledFileDO requestedFile = controlledFileMapper.selectById(controlledFileId);
        if (requestedFile == null || requestedFile.getMasterId() == null) {
            throw exception(PROJECT_CODE_ASSIGNMENT_FILE_SCOPE_INVALID);
        }
        DccControlledFileDO latestApprovedFile =
                controlledFileMapper.selectLatestApprovedByMasterId(requestedFile.getMasterId());
        if (latestApprovedFile == null || !Objects.equals(latestApprovedFile.getId(), controlledFileId)) {
            throw exception(PROJECT_CODE_ASSIGNMENT_FILE_SCOPE_INVALID);
        }
        DccProjectCodeAssignmentFileDO assignmentFile = assignmentFileMapper
                .selectByAssignmentIdAndMasterId(assignmentId, requestedFile.getMasterId());
        if (assignmentFile == null) {
            throw exception(PROJECT_CODE_ASSIGNMENT_FILE_SCOPE_INVALID);
        }
        return assignmentFile;
    }

    private Map<Long, DccControlledFileDO> selectLatestApprovedFileMapByMasterId(
            List<DccProjectCodeAssignmentFileDO> assignmentFiles) {
        Map<Long, DccControlledFileDO> fileMap = new LinkedHashMap<>();
        for (DccProjectCodeAssignmentFileDO assignmentFile : assignmentFiles) {
            Long masterId = assignmentFile.getMasterId();
            if (masterId == null) {
                throw exception(PROJECT_CODE_ASSIGNMENT_FILE_SCOPE_INVALID);
            }
            if (fileMap.containsKey(masterId)) {
                continue;
            }
            DccControlledFileDO latestApprovedFile = controlledFileMapper.selectLatestApprovedByMasterId(masterId);
            if (latestApprovedFile == null) {
                throw exception(PROJECT_CODE_ASSIGNMENT_FILE_SCOPE_INVALID);
            }
            fileMap.put(masterId, latestApprovedFile);
        }
        return fileMap;
    }

    private DccProjectCodeDO validateProjectCode(Long projectCodeId) {
        DccProjectCodeDO projectCode = projectCodeMapper.selectById(projectCodeId);
        if (projectCode == null) {
            throw exception(PROJECT_CODE_NOT_EXISTS);
        }
        return projectCode;
    }

    private void validateAssignee(Long assigneeUserId) {
        AdminUserRespDTO user = adminUserApi.getUser(assigneeUserId);
        if (user == null || !CommonStatusEnum.isEnable(user.getStatus())) {
            throw exception(PROJECT_CODE_ASSIGNMENT_ASSIGNEE_INVALID);
        }
        if (!permissionApi.hasAnyPermissions(assigneeUserId, ASSIGNMENT_EXECUTE_PERMISSION)) {
            throw exception(PROJECT_CODE_ASSIGNMENT_ASSIGNEE_PERMISSION_MISSING);
        }
    }

    private List<Long> normalizeSelectedFileIds(DccProjectCodeAssignmentCreateReqVO reqVO) {
        String scopeMode = reqVO.getScopeMode();
        if (!SCOPE_PROJECT_CODE_CURRENT_FILES.equals(scopeMode) && !SCOPE_SELECTED_FILES.equals(scopeMode)) {
            throw exception(PROJECT_CODE_ASSIGNMENT_SCOPE_EMPTY);
        }
        if (!SCOPE_SELECTED_FILES.equals(scopeMode)) {
            return List.of();
        }
        if (reqVO.getFileIds() == null || reqVO.getFileIds().isEmpty()) {
            throw exception(PROJECT_CODE_ASSIGNMENT_SCOPE_EMPTY);
        }
        return reqVO.getFileIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), List::copyOf));
    }

    private void validateScope(DccProjectCodeAssignmentCreateReqVO reqVO, List<Long> selectedFileIds,
                               List<DccControlledFileDO> files) {
        if (files.isEmpty()) {
            throw exception(PROJECT_CODE_ASSIGNMENT_SCOPE_EMPTY);
        }
        if (SCOPE_SELECTED_FILES.equals(reqVO.getScopeMode())) {
            Set<Long> actualIds = files.stream().map(DccControlledFileDO::getId).collect(Collectors.toSet());
            if (actualIds.size() != selectedFileIds.size() || !actualIds.containsAll(selectedFileIds)) {
                throw exception(PROJECT_CODE_ASSIGNMENT_FILE_SCOPE_INVALID);
            }
        }
    }

    private DccProjectCodeAssignmentFileDO toAssignmentFile(Long assignmentId, Long projectCodeId,
                                                            DccControlledFileDO file) {
        return DccProjectCodeAssignmentFileDO.builder()
                .assignmentId(assignmentId)
                .projectCodeId(projectCodeId)
                .controlledFileId(file.getId())
                .masterId(file.getMasterId())
                .fileNumberSnapshot(file.getFileNumber())
                .fileNameSnapshot(file.getFileName())
                .categoryIdSnapshot(file.getCategoryId())
                .directoryIdSnapshot(file.getDirectoryId())
                .initialFileTypeLevel1(file.getFileTypeLevel1())
                .initialFileTypeLevel2(file.getFileTypeLevel2())
                .initialFileTypeLevel3(file.getFileTypeLevel3())
                .initialFileTypeLevel4(file.getFileTypeLevel4())
                .initialFileTypeLevel5(file.getFileTypeLevel5())
                .changed(Boolean.FALSE)
                .changedFieldCount(0)
                .build();
    }

    private DccProjectCodeAssignmentDO validateReadableAssignment(Long userId, Long assignmentId) {
        DccProjectCodeAssignmentDO assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw exception(PROJECT_CODE_ASSIGNMENT_NOT_EXISTS);
        }
        if (isDocControl(userId) || Objects.equals(assignment.getAssigneeUserId(), userId)) {
            return assignment;
        }
        throw exception(CONTROLLED_FILE_METADATA_UPDATE_NOT_ALLOWED);
    }

    private boolean isAssignmentActiveForUser(DccProjectCodeAssignmentDO assignment, Long userId) {
        return Objects.equals(assignment.getAssigneeUserId(), userId)
                && isAssignmentActive(assignment);
    }

    private boolean isAssignmentActive(DccProjectCodeAssignmentDO assignment) {
        return STATUS_ACTIVE.equals(assignment.getStatus())
                && (assignment.getExpireTime() == null || assignment.getExpireTime().isAfter(LocalDateTime.now()));
    }

    private boolean isDocControl(Long userId) {
        return permissionApi.hasAnyRoles(userId, DccControlledFileMetadataUpdateService.DOC_CONTROL_ROLE_CODE);
    }

    private PageResult<DccProjectCodeAssignmentRespVO> toAssignmentRespPage(
            PageResult<DccProjectCodeAssignmentDO> pageResult) {
        List<DccProjectCodeAssignmentDO> assignments = pageResult.getList();
        Map<Long, DccProjectCodeDO> projectCodeMap = assignments.isEmpty()
                ? Map.of()
                : convertMap(projectCodeMapper.selectBatchIds(convertList(assignments,
                        DccProjectCodeAssignmentDO::getProjectCodeId)), DccProjectCodeDO::getId);
        Map<Long, AdminUserRespDTO> userMap = selectUserMap(convertList(assignments,
                DccProjectCodeAssignmentDO::getAssigneeUserId));
        return new PageResult<>(assignments.stream()
                .map(item -> toRespVO(item, projectCodeMap, userMap))
                .toList(), pageResult.getTotal());
    }

    private DccProjectCodeAssignmentRespVO toRespVO(DccProjectCodeAssignmentDO assignment,
                                                    Map<Long, DccProjectCodeDO> projectCodeMap,
                                                    Map<Long, AdminUserRespDTO> userMap) {
        DccProjectCodeAssignmentRespVO respVO = BeanUtils.toBean(assignment, DccProjectCodeAssignmentRespVO.class);
        DccProjectCodeDO projectCode = projectCodeMap.get(assignment.getProjectCodeId());
        if (projectCode != null) {
            respVO.setProjectName(projectCode.getProjectName());
            respVO.setProjectCode(projectCode.getProjectCode());
        }
        AdminUserRespDTO user = userMap.get(assignment.getAssigneeUserId());
        if (user != null) {
            respVO.setAssigneeNickname(user.getNickname());
        }
        if (STATUS_ACTIVE.equals(respVO.getStatus()) && respVO.getExpireTime() != null
                && !respVO.getExpireTime().isAfter(LocalDateTime.now())) {
            respVO.setStatus("EXPIRED");
        }
        return respVO;
    }

    private DccProjectCodeAssignmentFileRespVO toAssignmentFileRespVO(DccProjectCodeAssignmentFileDO assignmentFile,
                                                                      DccControlledFileDO file,
                                                                      boolean editable) {
        DccProjectCodeAssignmentFileRespVO respVO = file == null
                ? new DccProjectCodeAssignmentFileRespVO()
                : BeanUtils.toBean(file, DccProjectCodeAssignmentFileRespVO.class);
        if (file == null) {
            respVO.setId(assignmentFile.getControlledFileId());
            respVO.setFileName(assignmentFile.getFileNameSnapshot());
            respVO.setFileNumber(assignmentFile.getFileNumberSnapshot());
            respVO.setCategoryId(assignmentFile.getCategoryIdSnapshot());
            respVO.setDirectoryId(assignmentFile.getDirectoryIdSnapshot());
            respVO.setFileTypeLevel1(assignmentFile.getInitialFileTypeLevel1());
            respVO.setFileTypeLevel2(assignmentFile.getInitialFileTypeLevel2());
            respVO.setFileTypeLevel3(assignmentFile.getInitialFileTypeLevel3());
            respVO.setFileTypeLevel4(assignmentFile.getInitialFileTypeLevel4());
            respVO.setFileTypeLevel5(assignmentFile.getInitialFileTypeLevel5());
        }
        respVO.setMetadataEditable(editable);
        respVO.setMetadataEditAssignmentId(assignmentFile.getAssignmentId());
        respVO.setChangedFieldCount(safeInt(assignmentFile.getChangedFieldCount()));
        respVO.setLastChangedTime(assignmentFile.getLastChangedTime());
        return respVO;
    }

    private Map<Long, AdminUserRespDTO> selectUserMap(Collection<Long> userIds) {
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        return ids.isEmpty() ? Map.of() : convertMap(adminUserApi.getUserList(ids), AdminUserRespDTO::getId);
    }

    private String createAssignmentNo() {
        return "DCC-PC-A-" + LocalDate.now().format(ASSIGNMENT_NO_DATE_FORMATTER)
                + "-" + Math.abs(System.nanoTime() % 1_000_000L);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

}
