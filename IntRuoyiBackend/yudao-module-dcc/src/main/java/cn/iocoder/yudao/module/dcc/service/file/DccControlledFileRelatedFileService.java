package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRelatedFileRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRelatedFileDO;

import java.util.List;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.FileVersion;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.RelationChange;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.CurrentRelations;

public interface DccControlledFileRelatedFileService {

    void validateAndBindRelatedFiles(Long controlledFileId, Long projectCodeId, List<Long> relatedControlledFileIds);

    void inheritRelatedFiles(Long sourceControlledFileId, Long targetControlledFileId);

    /** A newly allocated formal revision freezes its controlled baseline's current relations. */
    void freezeCurrentRelationsForRevision(Long actorId, Long baselineControlledFileId, Long candidateControlledFileId);

    List<DccControlledFileRelatedFileRespVO> listRelatedFiles(Long controlledFileId);

    List<DccControlledFileRelatedFileDO> listForwardRelations(Long controlledFileId);

    List<Long> resolveCurrentActiveRelatedFileIds(Long controlledFileId, Long projectCodeId);

    List<DccControlledFileRelatedFileDO> listReverseCurrentActiveRelations(Long tenantId, Long relatedMasterId);

    List<FileVersion> listCurrentRelatedFiles(Long actorId, Long sourceFileId);
    CurrentRelations getCurrentRelationView(Long actorId,Long sourceFileId);

    List<DccControlledFileRelatedFileRespVO> listHistoricalRelatedFiles(Long actorId, Long sourceFileId);

    RelationChange replaceCurrentRelations(Long actorId, Long sourceFileId, List<Long> selectedFileIds,
                                 List<Long> expectedMasterIds, Long expectedVersion,String idempotencyKey,String reason);

    void assertRelatedContentReadable(Long actorId, Long relatedFileId);

}
