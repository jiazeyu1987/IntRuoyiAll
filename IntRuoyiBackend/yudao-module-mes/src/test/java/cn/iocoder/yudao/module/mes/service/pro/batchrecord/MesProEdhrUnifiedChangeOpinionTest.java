package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import com.alibaba.fastjson.JSON;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeApproveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrUnifiedChangeEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrUnifiedChangeRequestDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrUnifiedChangeEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrUnifiedChangeRequestMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.*;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrUnifiedChangeServiceImpl.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesProEdhrUnifiedChangeOpinionTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t\n", "\u3000", " 有效意见 "})
    void approvalPersistsOptionalOpinionAndAuditEventWithSignoffEvidence(String opinion) {
        Fixture f = new Fixture();
        String expected = opinion == null || opinion.isBlank() ? "" : opinion.trim();
        MesProEdhrUnifiedChangeRequestDO approved = submitted()
                .setChangeStatus(STATUS_APPROVED).setApprovalOpinion(expected);
        when(f.requests.selectById(10L)).thenReturn(f.submitted, approved);

        var result = f.service.approve(request(opinion));

        ArgumentCaptor<MesProEdhrUnifiedChangeRequestDO> update = ArgumentCaptor.forClass(MesProEdhrUnifiedChangeRequestDO.class);
        verify(f.requests).updateById(update.capture());
        assertEquals(STATUS_APPROVED, update.getValue().getChangeStatus());
        assertEquals(expected, update.getValue().getApprovalOpinion());
        assertEquals("signoff-hash", update.getValue().getApprovalSignoffEvidenceHash());
        assertNotNull(update.getValue().getImpactRecalculationHash());
        assertNotNull(update.getValue().getEvidenceHash());
        assertEquals(STATUS_APPROVED, result.getChangeStatus());
        assertEquals(expected, result.getApprovalOpinion());

        ArgumentCaptor<MesProEdhrUnifiedChangeEventDO> event = ArgumentCaptor.forClass(MesProEdhrUnifiedChangeEventDO.class);
        verify(f.events).insert(event.capture());
        assertEquals(EVENT_TYPE_APPROVE, event.getValue().getEventType());
        assertEquals(STATUS_SUBMITTED, event.getValue().getFromStatus());
        assertEquals(STATUS_APPROVED, event.getValue().getToStatus());
        assertEquals(expected, event.getValue().getReason());
        assertEquals("signoff-hash", event.getValue().getSignoffEvidenceHash());
        assertEquals("request-key", event.getValue().getIdempotencyKey());
        assertEquals(expected, JSON.parseObject(event.getValue().getEventSnapshotJson()).getString("reason"));
        assertNotNull(event.getValue().getEvidenceHash());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SIGNOFF", "IDEMPOTENCY", "IMPACT", "STATUS"})
    void optionalOpinionDoesNotBypassEvidenceIdempotencyImpactOrState(String missing) {
        Fixture f = new Fixture();
        MesProEdhrUnifiedChangeApproveReqVO request = request(null);
        switch (missing) {
            case "SIGNOFF" -> {
                request.setSignoffEvidenceHash(" ");
                assertServiceException(() -> f.service.approve(request), PRO_EDHR_UNIFIED_CHANGE_SIGNOFF_REQUIRED);
            }
            case "IDEMPOTENCY" -> {
                request.setIdempotencyKey("");
                assertServiceException(() -> f.service.approve(request), PRO_EDHR_UNIFIED_CHANGE_IDEMPOTENCY_KEY_REQUIRED);
            }
            case "IMPACT" -> {
                f.submitted.setImpactSummaryJson("{}");
                assertServiceException(() -> f.service.approve(request), PRO_EDHR_UNIFIED_CHANGE_IMPACT_REQUIRED);
            }
            case "STATUS" -> {
                f.submitted.setChangeStatus(STATUS_DRAFT);
                assertServiceException(() -> f.service.approve(request), PRO_EDHR_UNIFIED_CHANGE_STATUS_INVALID);
            }
            default -> throw new AssertionError(missing);
        }
        verify(f.requests, never()).updateById(any(MesProEdhrUnifiedChangeRequestDO.class));
        verifyNoInteractions(f.events);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t\n", "\u3000"})
    void requestValidationAllowsOptionalOpinionAndRetainsOtherRequiredFields(String opinion) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            MesProEdhrUnifiedChangeApproveReqVO request = request(opinion);
            assertTrue(validator.validate(request).isEmpty());
            request.setSignoffEvidenceHash(" ");
            request.setIdempotencyKey(null);
            request.setChangeRequestId(null);
            var paths = validator.validate(request).stream()
                    .map(violation -> violation.getPropertyPath().toString()).toList();
            assertTrue(paths.containsAll(java.util.List.of("signoffEvidenceHash", "idempotencyKey", "changeRequestId")));
            assertFalse(paths.contains("approvalOpinion"));
        }
    }

    private static MesProEdhrUnifiedChangeApproveReqVO request(String opinion) {
        return new MesProEdhrUnifiedChangeApproveReqVO().setChangeRequestId(10L)
                .setApprovalOpinion(opinion).setSignoffEvidenceHash("signoff-hash").setIdempotencyKey("request-key");
    }

    private static MesProEdhrUnifiedChangeRequestDO submitted() {
        return new MesProEdhrUnifiedChangeRequestDO().setId(10L).setChangeCode("CHANGE-10")
                .setControlledObjectType(OBJECT_TYPE_FORM_TEMPLATE).setControlledObjectId("20")
                .setControlledObjectCode("FORM-20").setCurrentVersion("1").setTargetVersion("2")
                .setChangeType("REVISION").setChangeStatus(STATUS_SUBMITTED).setReason("调整表单")
                .setDiffSnapshotJson("{\"field\":\"changed\"}").setImpactSummaryJson("{\"scope\":\"form\"}");
    }

    private static class Fixture {
        final MesProEdhrUnifiedChangeServiceImpl service = new MesProEdhrUnifiedChangeServiceImpl();
        final MesProEdhrUnifiedChangeRequestMapper requests = mock(MesProEdhrUnifiedChangeRequestMapper.class);
        final MesProEdhrUnifiedChangeEventMapper events = mock(MesProEdhrUnifiedChangeEventMapper.class);
        final MesProEdhrUnifiedChangeRequestDO submitted = submitted();

        Fixture() {
            when(requests.selectById(10L)).thenReturn(submitted);
            ReflectionTestUtils.setField(service, "unifiedChangeRequestMapper", requests);
            ReflectionTestUtils.setField(service, "unifiedChangeEventMapper", events);
        }
    }
}
