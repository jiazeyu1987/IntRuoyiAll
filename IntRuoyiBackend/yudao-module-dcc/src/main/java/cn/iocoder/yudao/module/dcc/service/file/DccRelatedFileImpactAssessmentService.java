package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;

public interface DccRelatedFileImpactAssessmentService {

    void materializeForPublicationBatch(Long batchId);

    void startTask(Long actorId, Long taskId, Integer expectedVersion);

    void submitDecision(Long actorId, Long taskId, Integer expectedVersion, String decision, String reason);

    void reassignTask(Long actorId, Long taskId, Integer expectedVersion, Long newAssigneeId, String reason);

    void reopenTask(Long actorId, Long taskId, Integer expectedVersion, String reason);

    void linkExistingMajorRevision(Long actorId, Long taskId, Integer expectedVersion,
                                   Long revisionId, String reason);

    void assertRevisionCreationAllowed(Long actorId, Long taskId, Integer expectedVersion,
                                       Long sourceControlledFileId, String reason);

    void resolveLinkedRevisionAfterPublication(DccControlledFileDO publishedRevision);
}
