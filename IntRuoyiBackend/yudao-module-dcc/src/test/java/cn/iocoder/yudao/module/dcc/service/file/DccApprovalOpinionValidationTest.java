package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate.DccProjectProductApprovalActionReqVO;
import jakarta.validation.Validation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class DccApprovalOpinionValidationTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings={"   ","\t\n"})
    void approveDtosAcceptEmptyOpinionWhileRejectAndPasswordRemainRequired(String reason) {
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            var validator=factory.getValidator();
            for(var approve:new DccControlledFileApproveTaskReqVO[]{new DccControlledFileApproveTaskReqVO(),new DccExternalFileReviewApproveTaskReqVO()}) {
                approve.setTaskId("task");approve.setPassword("credential");approve.setReason(reason);
                assertTrue(validator.validate(approve).isEmpty());
                approve.setPassword(" ");assertFalse(validator.validate(approve).isEmpty());
            }
            var project=new DccProjectProductApprovalActionReqVO();project.setReason(reason);
            assertTrue(validator.validate(project).isEmpty());
            var reject=new DccControlledFileRejectTaskReqVO();reject.setTaskId("task");reject.setPassword("credential");reject.setReason(reason);
            assertTrue(validator.validate(reject).stream().anyMatch(violation->"reason".equals(violation.getPropertyPath().toString())));
        }
    }
}
