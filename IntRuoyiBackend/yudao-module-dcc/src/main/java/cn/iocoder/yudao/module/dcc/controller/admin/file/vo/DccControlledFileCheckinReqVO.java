package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

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

    private String changeDescription;

    private Boolean needTraining;

    /** Optional metadata-only change. Only the remark field is mutable in P2. */
    private String remark;
}
