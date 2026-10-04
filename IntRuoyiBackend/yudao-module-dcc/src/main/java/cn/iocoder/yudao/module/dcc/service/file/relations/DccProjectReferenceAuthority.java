package cn.iocoder.yudao.module.dcc.service.file.relations;

/** B adapter must use the single formal project leader account, with no admin/OWNER bypass. */
public interface DccProjectReferenceAuthority {
    void assertProjectLeader(Long userId, Long projectId);
    void assertFolderBelongsToProject(Long projectId, Long folderId);
}
