package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - eDHR 交付证据包 Response VO")
@Data
@Accessors(chain = true)
public class MesProEdhrEvidencePackageRespVO {

    private Long id;
    private Long projectId;
    private String packageCode;
    private String packageName;
    private String packageType;
    private String packageStatus;
    private String evidenceStatus;
    private String ownerName;
    private String ownerDepartment;
    private String requiredEvidenceJson;
    private String availableEvidenceJson;
    private String missingEvidenceJson;
    private String signoffImpact;
    private String nextAction;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
