package cn.iocoder.yudao.module.dcc.service.download;

import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class DccDownloadPolicyService {

    public DccDownloadPolicyDecision decide(DccDownloadPolicyContext context) {
        Objects.requireNonNull(context, "context");
        if (context.publishedFileId() == null) {
            return DccDownloadPolicyDecision.deny("PUBLISHED_FILE_MISSING");
        }
        if (!DccControlledFileStatusEnum.ACTIVE.getStatus().equals(context.status())) {
            return DccDownloadPolicyDecision.deny("STATUS_NOT_ACTIVE");
        }
        if (!context.categoryDownloadAllowed()) {
            return DccDownloadPolicyDecision.deny("CATEGORY_DOWNLOAD_DENIED");
        }
        if (!context.directoryDownloadAllowed()) {
            return DccDownloadPolicyDecision.deny("DIRECTORY_DOWNLOAD_DENIED");
        }
        return DccDownloadPolicyDecision.allow();
    }
}
