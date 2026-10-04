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
}
