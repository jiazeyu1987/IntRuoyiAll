package cn.iocoder.yudao.module.dcc;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileApproveTaskReqVO;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DccApprovalReasonValidationTest {

    @Test
    void blankApprovalReasonIsRejectedByBeanValidation() {
        DccControlledFileApproveTaskReqVO request = new DccControlledFileApproveTaskReqVO();
        request.setTaskId("task-1");
        request.setPassword("secret");
        request.setReason("   ");

        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        assertFalse(validator.validate(request).isEmpty());
    }
}
