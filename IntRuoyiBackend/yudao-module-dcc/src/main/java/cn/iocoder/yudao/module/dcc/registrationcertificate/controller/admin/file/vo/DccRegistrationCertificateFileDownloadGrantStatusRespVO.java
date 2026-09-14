package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.file.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DccRegistrationCertificateFileDownloadGrantStatusRespVO {

    private Long businessFileId;
    private Boolean canDownload;
    private Long pendingRequestId;

    public static DccRegistrationCertificateFileDownloadGrantStatusRespVO of(Long businessFileId,
                                                                             boolean canDownload,
                                                                             Long pendingRequestId) {
        return DccRegistrationCertificateFileDownloadGrantStatusRespVO.builder()
                .businessFileId(businessFileId)
                .canDownload(canDownload)
                .pendingRequestId(pendingRequestId)
                .build();
    }
}
