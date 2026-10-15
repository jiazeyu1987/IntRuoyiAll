package cn.iocoder.yudao.module.dcc.service.file.relations;

import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectLeaderService;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccFolderTemplateService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class DccProjectReferenceAuthorityImpl implements DccProjectReferenceAuthority {
    @Resource private DccProjectLeaderService leaderService;
    @Resource private DccFolderTemplateService folderService;
    public void assertProjectLeader(Long userId,Long projectId) { leaderService.assertProjectLeader(userId,projectId); }
    public void assertFolderBelongsToProject(Long projectId,Long folderId) { folderService.requireProjectFolder(projectId,folderId); }
    @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY,rollbackFor=Exception.class)
    public void assertFolderBelongsToProjectForUpdate(Long projectId,Long folderId) { folderService.requireProjectFolderForUpdate(projectId,folderId); }
}
