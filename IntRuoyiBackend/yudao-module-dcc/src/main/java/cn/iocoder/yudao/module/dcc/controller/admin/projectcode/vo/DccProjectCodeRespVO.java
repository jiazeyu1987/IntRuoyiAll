package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - DCC 项目代码 Response VO")
@Data
public class DccProjectCodeRespVO {

    private Long id;
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
    private Long associatedFileCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
