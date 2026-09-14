package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.accessrequest.vo;

import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateGrantStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DccRegistrationCertificateGrantStatusRespVO {

    private Long grantId;
    private Long requestFileId;
    private Long businessFileId;
    private String grantType;
    private String status;
    private LocalDateTime grantedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;
    private String revokeReason;

    public static DccRegistrationCertificateGrantStatusRespVO of(DccRegistrationCertificateGrantStatus status) {
        return DccRegistrationCertificateGrantStatusRespVO.builder()
                .grantId(status.grantId())
                .requestFileId(status.requestFileId())
                .businessFileId(status.businessFileId())
                .grantType(status.grantType())
                .status(status.status())
                .grantedAt(status.grantedAt())
                .expiresAt(status.expiresAt())
                .revokedAt(status.revokedAt())
                .revokeReason(status.revokeReason())
                .build();
    }
}
