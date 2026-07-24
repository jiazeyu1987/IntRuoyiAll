package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccProjectCodeImportRowRespVO {

    private Integer rowNo;
    private String docControlNo;
    private String projectName;
    private String projectCode;
    private String category;
    private String commissionedProduction;
    private String projectLeader;
    private String projectEngineer;
    private String storageLocation;
    private String priority;
    private String currentStatus;
    private String importAction;
    private String failureReason;
}
