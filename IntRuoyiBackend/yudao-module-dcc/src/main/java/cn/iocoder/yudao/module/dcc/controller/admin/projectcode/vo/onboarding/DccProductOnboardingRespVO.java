package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.onboarding;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DccProductOnboardingRespVO {

    private Long id;
    private Long productMasterId;
    private String productCode;
    private String dccProductCode;
    private String productNameCn;
    private String productNameEn;
    private String modelSpecification;
    private String productCategory;
    private String docControlNo;
    private String projectName;
    private String projectCode;
    private String category;
    private String commissionedProduction;
    private String projectLeader;
    private String projectEngineer;
    private String storageLocation;
    private String priority;
    private String status;
    private Long applicantUserId;
    private Long approverUserId;
    private LocalDateTime approvedTime;
    private Long generatedProjectCodeId;
    private String rejectReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
