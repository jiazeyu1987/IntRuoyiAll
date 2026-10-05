package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

/** Explicit context of the exact member bytes and review round displayed to the reviewer. */
@Data
@Accessors(chain=true)
public class MesSubmissionReviewExpectedContext {
    private Long eventId;
    private String payloadHash;
    private Long revisionId;
    private Long reviewId;
    private Integer reviewRound;
    private String reviewStatus;
    private Integer allocationVersion;
}
