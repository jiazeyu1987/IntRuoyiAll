package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryPermissionRuleDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryPermissionRuleMapper;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** The existing real signing/Flowable/official notification transaction host now uses actual distinct category rules. */
class DccTrainingRecordDutyApprovalTransactionTest extends DccOfflineTrainingApprovalTransactionTest {
    @Resource DccFileCategoryPermissionRuleMapper rules;
    private DccControlledFileCategoryPermissionSupport actualCategories;

    @BeforeEach
    void realCategoryDutyRules() {
        var training=(DccOfflineTrainingRecordService)ReflectionTestUtils.getField(this,"training");
        var permissions=(PermissionApi)ReflectionTestUtils.getField(training,"permissions");
        actualCategories=new DccControlledFileCategoryPermissionSupport();
        wire(actualCategories,"permissionRuleMapper",rules,"adminUserApi",accounts,"permissionApi",permissions,
                "deptApi",mock(cn.iocoder.yudao.module.system.api.dept.DeptApi.class));
        rules.insert(DccFileCategoryPermissionRuleDO.builder().categoryId(2L).actionType("TRAINING_RECORD")
                .subjectType("USER").subjectId(88L).scopeType("GLOBAL").active(true).build());
        rules.insert(DccFileCategoryPermissionRuleDO.builder().categoryId(2L).actionType("APPROVE")
                .subjectType("USER").subjectId(99L).scopeType("GLOBAL").active(true).build());
        wire(training,"categories",actualCategories);
    }

    @Test
    void independentTrainingDutyCommitsAuthenticApprovalAndOfficialMessage() {
        assertFalse(actualCategories.hasCategoryPermission(2L,88L,DccFileCategoryPermissionActionEnum.APPROVE));
        signedApprovalCommitsWaitingExecutionAndOneMessageToIndependentDocumentControl();
    }

    @Test
    void missingOfficialTemplateStillRollsBackAuthenticSignaturesAndReceiveTransition() {
        missingTemplateRollsBackBothAuthenticSignatureRowsOwnerAndRealApproval();
    }

    @Test
    void lateOfficialMessageFailureStillRollsBackTheWholeSignedTransaction() {
        failureAfterOfficialStationInsertRollsBackAuthenticSignaturesAndReceiveTransition();
    }
}
