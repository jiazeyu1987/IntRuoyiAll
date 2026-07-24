package cn.iocoder.yudao.module.srm.controller.admin.tender.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SrmTenderProjectRespVO {

    private Long id;
    private String projectNo;
    private String projectTitle;
    private String projectType;
    private String projectTypeLabel;
    private String projectStatus;
    private String projectStatusLabel;
    private Long sourcePlanId;
    private String sourcePlanNo;
    private BigDecimal expectedAmount;
    private LocalDateTime submissionStartTime;
    private LocalDateTime submissionEndTime;
    private Long dealSupplierId;
    private String dealSupplierName;
    private BigDecimal dealAmount;
    private Long contractId;
    private LocalDateTime createTime;

    private Notice notice;
    private Document document;
    private List<Line> lines = new ArrayList<>();
    private List<Submission> submissions = new ArrayList<>();
    private List<CommitteeMember> committeeMembers = new ArrayList<>();
    private List<Candidate> candidates = new ArrayList<>();
    private WinningResult winningResult;

    @Data
    public static class Line {
        private Long id;
        private Long sourcePlanLineId;
        private String lineNo;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private BigDecimal quantity;
        private String unit;
    }

    @Data
    public static class Notice {
        private Long id;
        private String noticeTitle;
        private String noticeAttachmentUrl;
        private LocalDateTime publishedTime;
    }

    @Data
    public static class Document {
        private Long id;
        private String documentName;
        private String documentAttachmentUrl;
    }

    @Data
    public static class Submission {
        private Long id;
        private Long supplierId;
        private String supplierName;
        private BigDecimal bidAmount;
        private String submissionStatus;
        private String attachmentUrl;
        private String submittedName;
        private LocalDateTime submittedTime;
    }

    @Data
    public static class CommitteeMember {
        private Long id;
        private Long applicationId;
        private Long expertId;
        private String expertName;
        private String specialtyType;
    }

    @Data
    public static class Candidate {
        private Long id;
        private Long submissionId;
        private Long supplierId;
        private String supplierName;
        private BigDecimal bidAmount;
        private Integer rankNo;
        private String candidateStatus;
    }

    @Data
    public static class WinningResult {
        private Long id;
        private Long candidateId;
        private Long supplierId;
        private String supplierName;
        private BigDecimal winningAmount;
        private String winningRemark;
        private String confirmedName;
        private LocalDateTime confirmedTime;
    }
}
