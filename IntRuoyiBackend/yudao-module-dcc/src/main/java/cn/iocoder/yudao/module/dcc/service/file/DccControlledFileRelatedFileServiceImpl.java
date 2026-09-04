package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRelatedFileRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRelatedFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileRelatedFileMapper;
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

@Service
@Validated
public class DccControlledFileRelatedFileServiceImpl implements DccControlledFileRelatedFileService {

    private static final String RELATION_SOURCE_UPLOAD = "UPLOAD";

    @Resource
    private DccControlledFileRelatedFileMapper relatedFileMapper;
    @Resource
    private DccControlledFileMapper controlledFileMapper;

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
