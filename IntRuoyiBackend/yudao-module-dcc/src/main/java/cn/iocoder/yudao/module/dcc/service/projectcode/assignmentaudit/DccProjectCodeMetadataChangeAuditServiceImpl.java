package cn.iocoder.yudao.module.dcc.service.projectcode.assignmentaudit;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentAuditPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentAuditRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeItemDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMetadataChangeItemMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMetadataChangeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.assignment.DccProjectCodeAssignmentService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSIGNMENT_AUDIT_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_CODE_ASSIGNMENT_AUDIT_PERSIST_FAILED;

@Service
public class DccProjectCodeMetadataChangeAuditServiceImpl
        implements DccProjectCodeMetadataChangeAuditService {

    @Resource
    private DccControlledFileMetadataChangeMapper changeMapper;
    @Resource
    private DccControlledFileMetadataChangeItemMapper changeItemMapper;
    @Resource
    private DccProjectCodeAssignmentMapper assignmentMapper;
    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DccProjectCodeAssignmentService assignmentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordMetadataChange(DccProjectCodeMetadataChangeCommand command) {
        try {
            List<FieldDiff> changedFields = diff(command.beforeFile(), command.afterFile());
            LocalDateTime changedTime = LocalDateTime.now();
            Long projectCodeId = command.authorization().projectCodeId() != null
                    ? command.authorization().projectCodeId()
                    : command.beforeFile().getDccProjectCodeId();
            DccControlledFileMetadataChangeDO change = DccControlledFileMetadataChangeDO.builder()
                    .assignmentId(command.authorization().assignmentId())
                    .projectCodeId(projectCodeId)
                    .controlledFileId(command.beforeFile().getId())
                    .masterId(command.beforeFile().getMasterId())
                    .operatorUserId(command.operatorUserId())
                    .source(command.authorization().source())
                    .changeReason(StrUtil.trimToNull(command.changeReason()))
                    .changedFieldCount(changedFields.size())
                    .beforeSnapshotJson(JsonUtils.toJsonString(snapshot(command.beforeFile())))
                    .afterSnapshotJson(JsonUtils.toJsonString(snapshot(command.afterFile())))
                    .changedTime(changedTime)
                    .build();
            if (changeMapper.insert(change) != 1) {
                throw exception(PROJECT_CODE_ASSIGNMENT_AUDIT_PERSIST_FAILED);
            }
            for (FieldDiff changedField : changedFields) {
                DccControlledFileMetadataChangeItemDO item = DccControlledFileMetadataChangeItemDO.builder()
                        .changeId(change.getId())
                        .assignmentId(command.authorization().assignmentId())
                        .projectCodeId(projectCodeId)
                        .controlledFileId(command.beforeFile().getId())
                        .operatorUserId(command.operatorUserId())
                        .fieldName(changedField.fieldName())
                        .fieldLabel(changedField.fieldLabel())
                        .oldValueText(changedField.oldValueText())
                        .newValueText(changedField.newValueText())
                        .changedTime(changedTime)
                        .build();
                if (changeItemMapper.insert(item) != 1) {
                    throw exception(PROJECT_CODE_ASSIGNMENT_AUDIT_PERSIST_FAILED);
                }
            }
            if (command.authorization().assignmentId() != null && !changedFields.isEmpty()) {
                assignmentService.markAssignmentFileChanged(command.authorization().assignmentId(),
                        command.beforeFile().getId(), changedFields.size());
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw exception(PROJECT_CODE_ASSIGNMENT_AUDIT_PERSIST_FAILED);
        }
    }

    @Override
    public PageResult<DccProjectCodeAssignmentAuditRespVO> getAuditPage(DccProjectCodeAssignmentAuditPageReqVO reqVO) {
        Collection<Long> sourceFilteredChangeIds = null;
        String source = StrUtil.trimToNull(reqVO.getSource());
        if (source != null) {
            sourceFilteredChangeIds = convertList(changeMapper.selectListBySource(source),
                    DccControlledFileMetadataChangeDO::getId);
            if (sourceFilteredChangeIds.isEmpty()) {
                return PageResult.empty();
            }
        }
        PageResult<DccControlledFileMetadataChangeItemDO> pageResult =
                changeItemMapper.selectPage(reqVO, sourceFilteredChangeIds);
        List<DccControlledFileMetadataChangeItemDO> items = pageResult.getList();
        Map<Long, DccControlledFileMetadataChangeDO> changeMap = selectMap(items,
                DccControlledFileMetadataChangeItemDO::getChangeId, changeMapper::selectBatchIds,
                DccControlledFileMetadataChangeDO::getId);
        Map<Long, DccProjectCodeAssignmentDO> assignmentMap = selectMap(items,
                DccControlledFileMetadataChangeItemDO::getAssignmentId, assignmentMapper::selectBatchIds,
                DccProjectCodeAssignmentDO::getId);
        Map<Long, DccProjectCodeDO> projectCodeMap = selectMap(items,
                DccControlledFileMetadataChangeItemDO::getProjectCodeId, projectCodeMapper::selectBatchIds,
                DccProjectCodeDO::getId);
        Map<Long, DccControlledFileDO> fileMap = selectMap(items,
                DccControlledFileMetadataChangeItemDO::getControlledFileId, controlledFileMapper::selectBatchIds,
                DccControlledFileDO::getId);
        Map<Long, AdminUserRespDTO> userMap = selectMap(items,
                DccControlledFileMetadataChangeItemDO::getOperatorUserId, adminUserApi::getUserList,
                AdminUserRespDTO::getId);
        List<DccProjectCodeAssignmentAuditRespVO> list = items.stream()
                .map(item -> toRespVO(item, changeMap, assignmentMap, projectCodeMap, fileMap, userMap))
                .toList();
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public List<DccProjectCodeAssignmentAuditRespVO> getAuditChangeItems(Long changeId) {
        DccControlledFileMetadataChangeDO change = changeMapper.selectById(changeId);
        if (change == null) {
            throw exception(PROJECT_CODE_ASSIGNMENT_AUDIT_NOT_EXISTS);
        }
        List<DccControlledFileMetadataChangeItemDO> items = changeItemMapper.selectListByChangeId(changeId);
        Map<Long, DccControlledFileMetadataChangeDO> changeMap = Map.of(change.getId(), change);
        Map<Long, DccProjectCodeAssignmentDO> assignmentMap = selectMap(items,
                DccControlledFileMetadataChangeItemDO::getAssignmentId, assignmentMapper::selectBatchIds,
                DccProjectCodeAssignmentDO::getId);
        Map<Long, DccProjectCodeDO> projectCodeMap = selectMap(items,
                DccControlledFileMetadataChangeItemDO::getProjectCodeId, projectCodeMapper::selectBatchIds,
                DccProjectCodeDO::getId);
        Map<Long, DccControlledFileDO> fileMap = selectMap(items,
                DccControlledFileMetadataChangeItemDO::getControlledFileId, controlledFileMapper::selectBatchIds,
                DccControlledFileDO::getId);
        Map<Long, AdminUserRespDTO> userMap = selectMap(items,
                DccControlledFileMetadataChangeItemDO::getOperatorUserId, adminUserApi::getUserList,
                AdminUserRespDTO::getId);
        return items.stream()
                .map(item -> toRespVO(item, changeMap, assignmentMap, projectCodeMap, fileMap, userMap))
                .toList();
    }

    private List<FieldDiff> diff(DccControlledFileDO beforeFile, DccControlledFileDO afterFile) {
        return java.util.stream.Stream.of(
                        field("productMasterId", "产品主数据", beforeFile.getProductMasterId(),
                                afterFile.getProductMasterId()),
                        field("productCode", "产品编号", beforeFile.getProductCode(), afterFile.getProductCode()),
                        field("productName", "产品名称", beforeFile.getProductName(), afterFile.getProductName()),
                        field("dccProjectCodeId", "DCC基础条目", beforeFile.getDccProjectCodeId(),
                                afterFile.getDccProjectCodeId()),
                        field("needTraining", "培训要求", beforeFile.getNeedTraining(), afterFile.getNeedTraining()),
                        field("fileTypeLevel1", "文件类别 I", beforeFile.getFileTypeLevel1(),
                                afterFile.getFileTypeLevel1()),
                        field("fileTypeLevel2", "文件类别 II", beforeFile.getFileTypeLevel2(),
                                afterFile.getFileTypeLevel2()),
                        field("fileTypeLevel3", "文件类别 III", beforeFile.getFileTypeLevel3(),
                                afterFile.getFileTypeLevel3()),
                        field("fileTypeLevel4", "文件类别 IV", beforeFile.getFileTypeLevel4(),
                                afterFile.getFileTypeLevel4()),
                        field("fileTypeLevel5", "文件类别 V", beforeFile.getFileTypeLevel5(),
                                afterFile.getFileTypeLevel5()),
                        field("fileName", "文件名称", beforeFile.getFileName(), afterFile.getFileName()),
                        field("fileNumber", "文件编号", beforeFile.getFileNumber(), afterFile.getFileNumber()),
                        field("categoryId", "文件类别", beforeFile.getCategoryId(), afterFile.getCategoryId()),
                        field("directoryId", "受控目录", beforeFile.getDirectoryId(), afterFile.getDirectoryId()))
                .filter(Objects::nonNull)
                .toList();
    }

    private FieldDiff field(String fieldName, String fieldLabel, Object oldValue, Object newValue) {
        String oldText = valueText(oldValue);
        String newText = valueText(newValue);
        return Objects.equals(oldText, newText) ? null : new FieldDiff(fieldName, fieldLabel, oldText, newText);
    }

    private Map<String, Object> snapshot(DccControlledFileDO file) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("productMasterId", file.getProductMasterId());
        values.put("productCode", file.getProductCode());
        values.put("productName", file.getProductName());
        values.put("dccProjectCodeId", file.getDccProjectCodeId());
        values.put("needTraining", file.getNeedTraining());
        values.put("fileTypeLevel1", file.getFileTypeLevel1());
        values.put("fileTypeLevel2", file.getFileTypeLevel2());
        values.put("fileTypeLevel3", file.getFileTypeLevel3());
        values.put("fileTypeLevel4", file.getFileTypeLevel4());
        values.put("fileTypeLevel5", file.getFileTypeLevel5());
        values.put("fileName", file.getFileName());
        values.put("fileNumber", file.getFileNumber());
        values.put("categoryId", file.getCategoryId());
        values.put("directoryId", file.getDirectoryId());
        return values;
    }

    private String valueText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private DccProjectCodeAssignmentAuditRespVO toRespVO(DccControlledFileMetadataChangeItemDO item,
                                                         Map<Long, DccControlledFileMetadataChangeDO> changeMap,
                                                         Map<Long, DccProjectCodeAssignmentDO> assignmentMap,
                                                         Map<Long, DccProjectCodeDO> projectCodeMap,
                                                         Map<Long, DccControlledFileDO> fileMap,
                                                         Map<Long, AdminUserRespDTO> userMap) {
        DccProjectCodeAssignmentAuditRespVO respVO = new DccProjectCodeAssignmentAuditRespVO();
        respVO.setId(item.getId());
        respVO.setChangeId(item.getChangeId());
        respVO.setAssignmentId(item.getAssignmentId());
        DccProjectCodeAssignmentDO assignment = assignmentMap.get(item.getAssignmentId());
        if (assignment != null) {
            respVO.setAssignmentNo(assignment.getAssignmentNo());
        }
        respVO.setProjectCodeId(item.getProjectCodeId());
        DccProjectCodeDO projectCode = projectCodeMap.get(item.getProjectCodeId());
        if (projectCode != null) {
            respVO.setProjectName(projectCode.getProjectName());
            respVO.setProjectCode(projectCode.getProjectCode());
        }
        respVO.setControlledFileId(item.getControlledFileId());
        DccControlledFileDO file = fileMap.get(item.getControlledFileId());
        if (file != null) {
            respVO.setFileNumber(file.getFileNumber());
            respVO.setFileName(file.getFileName());
        }
        respVO.setOperatorUserId(item.getOperatorUserId());
        AdminUserRespDTO user = userMap.get(item.getOperatorUserId());
        if (user != null) {
            respVO.setOperatorNickname(user.getNickname());
        }
        respVO.setFieldName(item.getFieldName());
        respVO.setFieldLabel(item.getFieldLabel());
        respVO.setOldValueText(item.getOldValueText());
        respVO.setNewValueText(item.getNewValueText());
        DccControlledFileMetadataChangeDO change = changeMap.get(item.getChangeId());
        if (change != null) {
            respVO.setSource(change.getSource());
            respVO.setChangeReason(change.getChangeReason());
        }
        respVO.setChangedTime(item.getChangedTime());
        return respVO;
    }

    private <T, R> Map<Long, R> selectMap(List<T> sources, java.util.function.Function<T, Long> idGetter,
                                          java.util.function.Function<Collection<Long>, List<R>> selector,
                                          java.util.function.Function<R, Long> resultIdGetter) {
        List<Long> ids = sources.stream().map(idGetter).filter(Objects::nonNull).distinct().toList();
        return ids.isEmpty() ? Map.of() : convertMap(selector.apply(ids), resultIdGetter);
    }

    private record FieldDiff(String fieldName, String fieldLabel, String oldValueText, String newValueText) {
    }

}
