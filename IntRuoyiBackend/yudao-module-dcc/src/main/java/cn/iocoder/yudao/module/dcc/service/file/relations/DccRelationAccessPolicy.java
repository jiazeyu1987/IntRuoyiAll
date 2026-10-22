package cn.iocoder.yudao.module.dcc.service.file.relations;

public interface DccRelationAccessPolicy {
    void assertNameVisible(Long userId, Long controlledFileId);
    void assertContentReadable(Long userId, Long controlledFileId);
    void assertCanEditRelations(Long userId, Long sourceControlledFileId);
    void assertCanArrange(Long userId, Long sourceControlledFileId, String applicationRound);
    void assertCanReadArrangements(Long userId, Long sourceControlledFileId, String applicationRound);
    void assertAssigneeAvailable(Long userId, Long relatedMasterId);
}
