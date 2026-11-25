package cn.iocoder.yudao.module.mes.service.pro.schedule;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProReplanRequestMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProReplanRequestDO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesProReplanIdempotencyTest {
    @Test
    void completedRequestReplaysWithoutRecomputingAndDifferentPayloadIsRejected() {
        var service = new MesProAutoScheduleServiceImpl();
        var mapper = mock(MesProReplanRequestMapper.class);
        ReflectionTestUtils.setField(service, "replanRequestMapper", mapper);
        var request = new MesProAutoScheduleReplanReqVO();
        request.setIdempotencyKey("same-key");
        var receipt = new MesProReplanRequestDO();
        receipt.setPayloadHash(DigestUtil.sha256Hex(JsonUtils.toJsonString(request)));
        var original = new MesProAutoScheduleApplyRespVO();
        original.setApplied(true);
        receipt.setResultJson(JsonUtils.toJsonString(original));
        when(mapper.lock(1L, 9L, "same-key")).thenReturn(receipt);
        TenantContextHolder.setTenantId(1L);
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(9L);
            assertEquals(true, service.replanApply(request).getApplied());
            verify(mapper, never()).complete(any(), any(), any(), any());
            request.setReason("changed");
            assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                    () -> service.replanApply(request));
            TenantContextHolder.setTenantId(2L);
            when(mapper.lock(2L, 9L, "same-key")).thenReturn(receipt);
            request.setReason(null);
            assertEquals(true, service.replanApply(request).getApplied());
            verify(mapper).claim(eq(2L), eq(9L), eq("same-key"), any());
        } finally {
            TenantContextHolder.clear();
        }
    }
}
