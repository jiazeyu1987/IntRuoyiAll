package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProScheduleOrderUpdateReqVOTest {

    @Test
    void priorityOnlyUpdate_shouldPassValidationWithoutDates() {
        MesProScheduleOrderUpdateReqVO request = request();
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(request).isEmpty());
        }
    }

    @Test
    void priorityOnlyUpdate_shouldStillRequireReasonAndPositivePriority() {
        MesProScheduleOrderUpdateReqVO request = request();
        request.setPriorityNo(0);
        request.setReason(" ");
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(request);
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("reason")));
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("priorityNo")));
        }
    }

    private MesProScheduleOrderUpdateReqVO request() {
        MesProScheduleOrderUpdateReqVO request = new MesProScheduleOrderUpdateReqVO();
        request.setId(900L);
        request.setPriorityNo(99);
        request.setReason("调整优先级");
        return request;
    }
}
