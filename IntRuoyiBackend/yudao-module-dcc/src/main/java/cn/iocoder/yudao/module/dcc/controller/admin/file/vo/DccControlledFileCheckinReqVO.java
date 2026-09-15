package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

@Data
public class DccControlledFileCheckinReqVO {

    /** MINOR advances the iteration; MAJOR advances the revision and requires project OWNER. */
    private String versionChangeType = "MINOR";

    private String uploadTicket;

    /** Required when the uploaded replacement source is an engineering drawing. */
    private String drawingPdfUploadTicket;

    private String sessionId;

    private String changeDescription;

    /** Optional metadata-only change. Only the remark field is mutable in P2. */
    private String remark;
}
