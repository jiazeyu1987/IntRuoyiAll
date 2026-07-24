package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.iocoder.yudao.module.dcc.service.download.DccDownloadFileBinary;

public interface DccControlledFileFinalizationService {

    void handleProcessInstanceStatusChanged(BpmProcessInstanceStatusEvent event);

    void retryStamp(Long id);

    void precheckPublishControlledFile(Long userId, Long id);

    void applyApprovedPublishControlledFile(Long userId, Long id, String eventKey);

    void releaseManualDistribution(Long userId, Long id);

    default void activateWithoutApproval(Long id) {
        activateWithoutApproval(id, false);
    }

    void activateWithoutApproval(Long id, boolean skipGovernance);

    DccControlledFileBinary readPreviewFile(Long userId, Long id);

    DccDownloadFileBinary readDownloadFile(Long userId, Long id, Boolean nonControlledWarningConfirmed,
                                           String downloadRequestId, DccRequestAuditContext auditContext);
}
