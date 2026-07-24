package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesProEdhrInitBatchRespVO {

    private Long id;

    private String projectCode;

    private String projectName;

    private String targetEnvironment;

    private Long targetTenantId;

    private String dataVersion;

    private Long ownerUserId;

    private Long approvalOwnerUserId;

    private LocalDateTime plannedStartTime;

    private LocalDateTime plannedEndTime;

    private String initScopeJson;

    private String status;

    private Integer manifestCount;

    private Integer blockingIssueCount;

    private LocalDateTime lastPrecheckAt;

    private Integer version;

    private String remark;

    private String latestManifestHash;

    private List<MesProEdhrInitManifestRespVO> manifests;
}
