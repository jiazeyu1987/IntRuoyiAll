package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRelatedFileRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRelatedFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRelatedFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_RELATED_FILE_DUPLICATE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_RELATED_FILE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum.ACTIVE;

@Service
@Validated
public class DccControlledFileRelatedFileServiceImpl implements DccControlledFileRelatedFileService {

    private static final String RELATION_SOURCE_UPLOAD = "UPLOAD";

    @Resource
    private DccControlledFileRelatedFileMapper relatedFileMapper;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileMasterMapper controlledFileMasterMapper;

    @Override
    public void validateAndBindRelatedFiles(Long controlledFileId, Long projectCodeId,
                                            List<Long> relatedControlledFileIds) {
        List<Long> normalizedIds = normalizeRelatedFileIds(controlledFileId, projectCodeId, relatedControlledFileIds);
        if (normalizedIds.isEmpty()) {
            return;
        }
        Map<Long, DccControlledFileDO> fileMap = controlledFileMapper
                .selectAssociatedFilesByProjectCodeId(projectCodeId, normalizedIds)
                .stream()
                .collect(Collectors.toMap(DccControlledFileDO::getId, Function.identity()));
        if (fileMap.size() != normalizedIds.size()) {
            throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
        }
        if (fileMap.values().stream().anyMatch(file -> file.getMasterId() == null)) {
            throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
        }
        Map<Long, DccControlledFileMasterDO> masterMap = controlledFileMasterMapper.selectBatchIds(
                        fileMap.values().stream().map(DccControlledFileDO::getMasterId).filter(Objects::nonNull)
                                .distinct().toList()).stream()
                .collect(Collectors.toMap(DccControlledFileMasterDO::getId, Function.identity()));
        DccControlledFileDO owner = controlledFileMapper.selectById(controlledFileId);
        boolean containsInvalidCandidate = fileMap.values().stream()
                .anyMatch(file -> !ACTIVE.getStatus().equals(file.getStatus()) || file.getMasterId() == null
                        || !Objects.equals(masterMap.get(file.getMasterId()) == null ? null
                                : masterMap.get(file.getMasterId()).getCurrentActiveControlledFileId(), file.getId())
                        || owner != null && Objects.equals(owner.getMasterId(), file.getMasterId()));
        if (containsInvalidCandidate) {
            throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
        }
        for (Long relatedFileId : normalizedIds) {
            DccControlledFileDO relatedFile = fileMap.get(relatedFileId);
            relatedFileMapper.insert(DccControlledFileRelatedFileDO.builder()
                    .controlledFileId(controlledFileId)
                    .relatedControlledFileId(relatedFile.getId())
                    .projectCodeId(projectCodeId)
                    .relatedMasterId(relatedFile.getMasterId())
                    .relatedFileNumberSnapshot(relatedFile.getFileNumber())
                    .relatedFileNameSnapshot(relatedFile.getFileName())
                    .relatedVersionNoSnapshot(relatedFile.getVersionNo())
                    .relationSource(RELATION_SOURCE_UPLOAD)
                    .build());
        }
    }

    @Override
    public List<DccControlledFileRelatedFileRespVO> listRelatedFiles(Long controlledFileId) {
        List<DccControlledFileRelatedFileDO> relations = relatedFileMapper.selectListByControlledFileId(controlledFileId);
        if (relations.isEmpty()) {
            return List.of();
        }
        List<Long> relatedFileIds = relations.stream()
                .map(DccControlledFileRelatedFileDO::getRelatedControlledFileId)
                .toList();
        Map<Long, DccControlledFileDO> currentFileMap = controlledFileMapper.selectBatchIds(relatedFileIds)
                .stream()
                .collect(Collectors.toMap(DccControlledFileDO::getId, Function.identity()));
        return relations.stream()
                .map(relation -> toRespVO(relation, currentFileMap.get(relation.getRelatedControlledFileId())))
                .toList();
    }

    @Override
    public List<DccControlledFileRelatedFileDO> listForwardRelations(Long controlledFileId) {
        return relatedFileMapper.selectListByControlledFileId(controlledFileId);
    }

    @Override
    public List<Long> resolveCurrentActiveRelatedFileIds(Long controlledFileId, Long projectCodeId) {
        if (controlledFileId == null || projectCodeId == null) {
            throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
        }
        List<DccControlledFileRelatedFileDO> relations = relatedFileMapper.selectListByControlledFileId(controlledFileId);
        if (relations.isEmpty()) {
            return List.of();
        }
        List<Long> masterIds = relations.stream().map(DccControlledFileRelatedFileDO::getRelatedMasterId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, DccControlledFileMasterDO> masterMap = controlledFileMasterMapper.selectBatchIds(masterIds).stream()
                .collect(Collectors.toMap(DccControlledFileMasterDO::getId, Function.identity()));
        if (masterMap.size() != masterIds.size()) {
            throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
        }
        List<Long> currentIds = relations.stream().map(relation -> {
            DccControlledFileMasterDO master = masterMap.get(relation.getRelatedMasterId());
            return master == null ? null : master.getCurrentActiveControlledFileId();
        }).toList();
        if (currentIds.stream().anyMatch(Objects::isNull) || new LinkedHashSet<>(currentIds).size() != currentIds.size()) {
            throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
        }
        Map<Long, DccControlledFileDO> currentFileMap = controlledFileMapper
                .selectAssociatedFilesByProjectCodeId(projectCodeId, currentIds).stream()
                .collect(Collectors.toMap(DccControlledFileDO::getId, Function.identity()));
        boolean invalid = currentIds.stream().anyMatch(id -> {
            DccControlledFileDO file = currentFileMap.get(id);
            return file == null || !ACTIVE.getStatus().equals(file.getStatus());
        });
        if (invalid) {
            throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
        }
        return List.copyOf(currentIds);
    }

    @Override
    public List<DccControlledFileRelatedFileDO> listReverseCurrentActiveRelations(Long tenantId,
                                                                                  Long relatedMasterId) {
        if (tenantId == null || relatedMasterId == null) {
            throw new IllegalArgumentException("tenantId and relatedMasterId are required for reverse relation lookup");
        }
        return relatedFileMapper.selectReverseCurrentActiveRelations(tenantId, relatedMasterId);
    }

    private List<Long> normalizeRelatedFileIds(Long controlledFileId, Long projectCodeId,
                                               List<Long> relatedControlledFileIds) {
        if (relatedControlledFileIds == null || relatedControlledFileIds.isEmpty()) {
            return List.of();
        }
        if (projectCodeId == null) {
            throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long fileId : relatedControlledFileIds) {
            if (fileId == null || fileId <= 0 || Objects.equals(fileId, controlledFileId)) {
                throw exception(CONTROLLED_FILE_RELATED_FILE_INVALID);
            }
            if (!uniqueIds.add(fileId)) {
                throw exception(CONTROLLED_FILE_RELATED_FILE_DUPLICATE);
            }
        }
        return List.copyOf(uniqueIds);
    }

    private DccControlledFileRelatedFileRespVO toRespVO(DccControlledFileRelatedFileDO relation,
                                                        DccControlledFileDO currentFile) {
        DccControlledFileRelatedFileRespVO respVO = new DccControlledFileRelatedFileRespVO();
        respVO.setControlledFileId(relation.getRelatedControlledFileId());
        respVO.setMasterId(relation.getRelatedMasterId());
        respVO.setProjectCodeId(relation.getProjectCodeId());
        respVO.setFileNumber(currentFile == null ? relation.getRelatedFileNumberSnapshot() : currentFile.getFileNumber());
        respVO.setFileName(currentFile == null ? relation.getRelatedFileNameSnapshot() : currentFile.getFileName());
        respVO.setVersionNo(currentFile == null ? relation.getRelatedVersionNoSnapshot() : currentFile.getVersionNo());
        respVO.setStatus(currentFile == null ? null : currentFile.getStatus());
        return respVO;
    }

}
