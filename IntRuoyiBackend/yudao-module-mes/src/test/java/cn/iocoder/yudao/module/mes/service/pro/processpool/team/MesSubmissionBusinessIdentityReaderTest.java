package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesSubmissionBusinessIdentityReaderTest {
    private MesProProcessPoolEventDO event() {
        var event = new MesProProcessPoolEventDO().setId(9007199254740993L).setEventType("PRODUCTION_SUBMIT")
                .setActualEmployeeId(31L).setDeviceAccountId(41L).setSignatureId(51L)
                .setRouteProcessId(102L).setProcessId(103L).setEventIdempotencyKey("original-submit")
                .setRawPayload("{\"activeOrderId\":9007199254740997,\"signatureIdentityDomain\":\"MES_EMPLOYEE_PROFILE\",\"quantity\":4}")
                .setSignatureSnapshot("{\"actorId\":999,\"operatorId\":999,\"domain\":\"SYSTEM_USER\"}");
        event.setTenantId(0L); return event;
    }
    @Test void preservesIndependentOriginalBusinessFieldsAndLongsInsteadOfSignatureSnapshot() {
        var source = JSON.parseObject(MesSubmissionBusinessIdentityReader.project(event(), 52L));
        assertEquals("9007199254740993", source.getString("eventId"));
        assertEquals("9007199254740997", source.getString("activeOrderId"));
        assertEquals("31", source.getString("actorId"));
        assertEquals("41", source.getString("operatorId"));
        assertEquals("0", source.getString("tenantId"));
        assertEquals("MES_EMPLOYEE_PROFILE", source.getString("identityDomain"));
        assertEquals("51", source.getString("signatureId"));
        assertEquals("52", source.getString("reviewSignatureId"));
        assertTrue(source.getString("payloadHash").matches("[a-f0-9]{64}"));
    }
    @Test void projectsPqcTaskFromOriginalFeedbackSourceAndNeverInfersFromSignature() {
        var event = event().setEventType("PQC_INSPECTION").setFeedbackSourceType("MES_PQC_INSPECTION_TASK").setFeedbackSourceId(88L);
        var source = JSON.parseObject(MesSubmissionBusinessIdentityReader.project(event, 52L));
        assertEquals("SYSTEM_USER", source.getString("identityDomain")); assertEquals("88", source.getString("pqcTaskId"));
        event.setFeedbackSourceType("WRONG");
        assertNull(JSON.parseObject(MesSubmissionBusinessIdentityReader.project(event, 52L)).getString("pqcTaskId"));
    }
    @Test void rejectsMissingOrCrossTenantEvents() {
        var events = mock(MesProProcessPoolEventMapper.class);
        var reviews = mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper.class);
        var reader = new MesSubmissionBusinessIdentityReader(events, reviews);
        TenantContextHolder.setTenantId(1L);
        try {
            assertThrows(IllegalStateException.class, () -> reader.read(99L, 52L));
            when(events.selectById(9007199254740993L)).thenReturn(event());
            assertThrows(IllegalStateException.class, () -> reader.read(9007199254740993L, 52L));
        } finally { TenantContextHolder.clear(); }
    }
    @Test void readsTheUniqueOriginalReviewRatherThanItsSignatureSnapshot() {
        var events = mock(MesProProcessPoolEventMapper.class);
        var reviews = mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper.class);
        var reader = new MesSubmissionBusinessIdentityReader(events, reviews);
        var original = event(); var review = new cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO()
                .setId(61L).setEventId(original.getId()).setLeaderUserId(42L).setLeaderType("PRODUCTION")
                .setReviewStatus("APPROVED").setReviewSignatureId(52L)
                .setReviewSignatureSnapshotJson("{\"actorId\":999}");
        review.setTenantId(0L); when(events.selectById(original.getId())).thenReturn(original);
        when(reviews.selectListByEventId(original.getId())).thenReturn(java.util.List.of(review));
        TenantContextHolder.setTenantId(0L);
        try {
            var value = JSON.parseObject(reader.read(original.getId(), 52L));
            assertEquals("42", value.getString("reviewActorId")); assertEquals("61", value.getString("reviewId"));
            assertEquals("PRODUCTION", value.getString("leaderType")); assertEquals("APPROVED", value.getString("reviewStatus"));
            assertThrows(IllegalStateException.class, () -> reader.read(original.getId(), 53L));
            when(reviews.selectListByEventId(original.getId())).thenReturn(java.util.List.of(review, review));
            assertThrows(IllegalStateException.class, () -> reader.read(original.getId(), 52L));
        } finally { TenantContextHolder.clear(); }
    }
}
