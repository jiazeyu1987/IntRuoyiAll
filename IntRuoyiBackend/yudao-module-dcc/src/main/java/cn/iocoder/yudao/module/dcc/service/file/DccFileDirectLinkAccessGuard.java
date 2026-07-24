package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.service.audit.DccControlledFileAccessAuditService;
import cn.iocoder.yudao.module.dcc.service.audit.DccDirectLinkDeniedLogCreateCommand;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.access.FileDirectLinkAccessContext;
import cn.iocoder.yudao.module.infra.service.file.access.FileDirectLinkAccessGuard;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class DccFileDirectLinkAccessGuard implements FileDirectLinkAccessGuard {

    private static final String ACTION_DIRECT_LINK = "DIRECT_LINK";
    private static final String PURPOSE_INFRA_DIRECT_LINK = "INFRA_DIRECT_LINK";
    private static final String RESULT_DENIED = "DENIED";
    private static final String FAILURE_DCC_DIRECT_LINK_BLOCKED = "DCC_DIRECT_LINK_BLOCKED";

    @Resource
    private DccControlledFileQueryService controlledFileQueryService;
    @Resource
    private DccControlledFileAccessAuditService accessAuditService;

    @Override
    public void assertAllowed(FileDO file, FileDirectLinkAccessContext context) {
        DccControlledFileScope scope = controlledFileQueryService.identifyControlledFileScope(file.getId());
        if (scope.controlled()) {
            for (DccControlledFileArtifactReference reference : scope.references()) {
                accessAuditService.recordDirectLinkDeniedLog(new DccDirectLinkDeniedLogCreateCommand(
                        reference.tenantId(), reference.controlledFileId(), file.getId(), reference.role().name(), ACTION_DIRECT_LINK,
                        PURPOSE_INFRA_DIRECT_LINK, RESULT_DENIED, FAILURE_DCC_DIRECT_LINK_BLOCKED,
                        "DCC controlled file direct link is blocked: infraFileId=" + file.getId()
                                + ", artifactRole=" + reference.role().name(),
                        context.sourceIp(), context.requestId(), context.userAgent()));
            }
            throw new ControlledFileDirectLinkBlockedException(scope.references().get(0).controlledFileId());
        }
    }
}
