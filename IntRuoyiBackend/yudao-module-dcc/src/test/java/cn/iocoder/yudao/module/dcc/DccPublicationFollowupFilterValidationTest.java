package cn.iocoder.yudao.module.dcc;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPublicationFollowupPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPublicationImpactTaskPageReqVO;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccPublicationFollowupFilterValidationTest {
    @Test
    void invalidStatusAndAssigneeFiltersAreRejectedByBeanValidation() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        DccPublicationFollowupPageReqVO management = new DccPublicationFollowupPageReqVO();
        management.setAssigneeUserId("not-a-number");
        management.setBatchStatus("UNKNOWN");
        management.setTaskStatus("DONE-ish");
        management.setNotificationStatus("SKIPPED");
        assertFalse(validator.validate(management).isEmpty());

        DccPublicationImpactTaskPageReqVO mine = new DccPublicationImpactTaskPageReqVO();
        mine.setTaskStatus("COMPLETED-ish");
        mine.setRevisionTrackingStatus("LINK-ish");
        assertFalse(validator.validate(mine).isEmpty());

        for (String invalid : new String[]{"9999999999999999999", "0", "-1", "not-a-number"}) {
            DccPublicationFollowupPageReqVO request = new DccPublicationFollowupPageReqVO();
            request.setAssigneeUserId(invalid);
            assertFalse(validator.validate(request).isEmpty(), "must reject assigneeUserId=" + invalid);
        }
        DccPublicationFollowupPageReqVO maximum = new DccPublicationFollowupPageReqVO();
        maximum.setAssigneeUserId(String.valueOf(Long.MAX_VALUE));
        assertTrue(validator.validate(maximum).isEmpty());
    }
}
