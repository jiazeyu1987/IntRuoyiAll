package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - eDHR 交付项目 Response VO")
@Data
@Accessors(chain = true)
public class MesProEdhrDeliveryProjectRespVO {

    private Long id;
    private String projectCode;
    private String projectName;
    private String customerName;
    private String siteName;
    private String systemScope;
    private String validationScope;
    private String releaseTag;
    private String schemaVersion;
    private String targetEnvironment;
    private String projectStatus;
    private Boolean signoffAllowed;
    private String ownerName;
    private String ownerDepartment;
    private String blockedReason;
    private String gateSummaryJson;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
