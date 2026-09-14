package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.accessrequest.vo;

import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateAccessRequestStatus;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateGrantStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DccRegistrationCertificateAccessRequestStatusRespVO {

    private Long requestId;
    private Long certificateId;
    private Long ownerCompanyId;
    private Long requesterUserId;
    private String requestType;
    private String purpose;
    private Long projectCodeId;
    private String requestStatus;
    private String bpmProcessInstanceId;
    private String bpmBindingStatus;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private LocalDateTime withdrawnAt;
    private String withdrawReason;
    private String rejectReason;
    private List<DccRegistrationCertificateGrantStatusRespVO> grants;

    public static DccRegistrationCertificateAccessRequestStatusRespVO of(
            DccRegistrationCertificateAccessRequestStatus status) {
        return DccRegistrationCertificateAccessRequestStatusRespVO.builder()
                .requestId(status.requestId())
                .certificateId(status.certificateId())
                .ownerCompanyId(status.ownerCompanyId())
                .requesterUserId(status.requesterUserId())
                .requestType(status.requestType())
                .purpose(status.purpose())
                .projectCodeId(status.projectCodeId())
                .requestStatus(status.requestStatus())
                .bpmProcessInstanceId(status.bpmProcessInstanceId())
                .bpmBindingStatus(status.bpmBindingStatus())
                .requestedAt(status.requestedAt())
                .completedAt(status.completedAt())
                .withdrawnAt(status.withdrawnAt())
                .withdrawReason(status.withdrawReason())
                .rejectReason(status.rejectReason())
                .grants(status.grants().stream().map(DccRegistrationCertificateGrantStatusRespVO::of).toList())
                .build();
    }
}
