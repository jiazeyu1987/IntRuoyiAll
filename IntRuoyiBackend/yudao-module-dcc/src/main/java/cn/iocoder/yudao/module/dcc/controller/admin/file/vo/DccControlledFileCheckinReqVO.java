package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class DccControlledFileCheckinReqVO {

    /** Only MINOR is accepted: check-in creates a hyphen working iteration. */
    private String versionChangeType = "MINOR";

    /** Formal intent belongs to the separate revision submission, never check-in. */
    private String revisionChangeType;

    private String uploadTicket;

    /** Required when the uploaded replacement source is an engineering drawing. */
    private String drawingPdfUploadTicket;

    private String sessionId;

    /** Each ordinary attachment has its own scoped CHECKIN session and ATTACHMENT ticket slot. */
    @Valid
    private List<@NotNull DccControlledFileAttachmentUploadTicketReqVO> attachmentUploadTickets;

    private String changeDescription;

    private Boolean needTraining;

    /** Optional metadata-only change. Only the remark field is mutable in P2. */
    private String remark;
}
