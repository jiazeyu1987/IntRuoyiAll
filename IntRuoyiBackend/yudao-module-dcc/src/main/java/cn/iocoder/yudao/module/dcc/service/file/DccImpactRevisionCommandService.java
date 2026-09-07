package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMajorRevisionReqVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DccImpactRevisionCommandService {

    @Resource
    private DccRelatedFileImpactAssessmentService impactService;
    @Resource
    private DccControlledFileWorkflowService workflowService;

    @Transactional(rollbackFor = Exception.class)
    public Long createAndLinkMajorRevision(Long actorId, Long taskId, Integer expectedVersion,
                                           Long sourceControlledFileId, String reason) {
        String normalizedReason = StrUtil.trim(reason);
        impactService.assertRevisionCreationAllowed(actorId, taskId, expectedVersion,
                sourceControlledFileId, normalizedReason);
        DccControlledFileMajorRevisionReqVO request = new DccControlledFileMajorRevisionReqVO();
        request.setSourceControlledFileId(sourceControlledFileId);
        request.setReason(normalizedReason);
        Long revisionId = workflowService.createMajorRevision(actorId, request);
        impactService.linkExistingMajorRevision(actorId, taskId, expectedVersion, revisionId, normalizedReason);
        return revisionId;
    }
}
