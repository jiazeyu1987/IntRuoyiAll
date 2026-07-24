package cn.iocoder.yudao.module.dcc.controller.admin.category.vo;

import lombok.Data;

@Data
public class DccAdminFullConfigPackageImportRespVO {

    private Integer approvalPositionCount;
    private Integer directoryCount;
    private Integer directoryAccessRuleCount;
    private Integer categoryCount;
    private Integer permissionRuleCount;
    private Integer approvalMatrixRuleCount;
    private Integer viewMatrixRuleCount;
    private Integer distributionRuleCount;
    private Integer trainingRuleCount;
    private Integer removedApprovalPositionCount;
    private Integer removedDirectoryCount;
    private Integer removedCategoryCount;
}
