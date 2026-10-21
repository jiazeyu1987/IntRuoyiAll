package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import java.util.List;

public record DccWorkingApplicationTypeChangePreviewVO(String controlledFileId,int sourceAttributeRound,
        String sourceCategoryId,String sourceTaxonomyId,String sourceSha256,String sourceMatrixDigest,
        String targetCategoryId,String targetTaxonomyId,String targetTypePath,
        DccApprovalMatrixBaselineVO beforeMatrix,DccApprovalMatrixBaselineVO targetMatrix,
        List<String> selectedSignoffDepartmentIds,List<String> selectedDistributionDepartmentIds,
        DccControlledFileRouteReadinessRespVO readiness) {}
