package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderReleaseApplyReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditRecorder;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseApplicationPersistenceService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseApplicationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MesTeamLeaderReleaseReceiptContractTest {

    @Test
    void persistedRequestKeyPassesThroughEveryPostAndGetWithoutUsingIncomingKey() {
        var applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        var tasks = mock(MesProEdhrWorkTaskMapper.class);
        var audit = mock(MesReleaseFlowAuditRecorder.class);
        var persistence = new MesTeamLeaderActiveOrderReleaseApplicationPersistenceService(applications, tasks, audit);
        var service = mock(MesTeamLeaderActiveOrderReleaseApplicationService.class);
        var controller = new MesProcessPoolTeamLeaderController(null, null, null, null, null, null,
                null, null, null, null, null, null, service, null, null, null, null, null, null);
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(7L);
            for (String storedKey : new String[]{"stored-request", null, ""}) {
                var application = new MesProcessPoolActiveOrderReleaseApplicationDO()
                        .setId(9007199254740993L).setActiveOrderId(9007199254740995L)
                        .setWorkOrderId(41L).setRouteId(42L).setRouteVersionId(43L)
                        .setRequestIdempotencyKey(storedKey);
                var result = persistence.toResult(application);
                when(service.apply(eq(7L), any())).thenReturn(result);
                when(service.applyGenerated(eq(7L), any())).thenReturn(result);
                when(service.get(7L, application.getActiveOrderId())).thenReturn(result);
                var request = new MesTeamLeaderActiveOrderReleaseApplyReqVO()
                        .setActiveOrderId(application.getActiveOrderId()).setIdempotencyKey("different-incoming-key");
                var post = controller.applyActiveOrderRelease(request).getData();
                var generated = controller.pushGeneratedActiveOrderRelease(request).getData();
                var get = controller.getActiveOrderRelease(application.getActiveOrderId()).getData();
                assertEquals(storedKey, result.getRequestIdempotencyKey());
                assertEquals(storedKey, post.getRequestIdempotencyKey());
                assertEquals(storedKey, generated.getRequestIdempotencyKey());
                assertEquals(storedKey, get.getRequestIdempotencyKey());
                assertEquals(application.getActiveOrderId(), get.getActiveOrderId());
                assertEquals(application.getId(), get.getApplicationId());
                assertEquals(storedKey, application.getRequestIdempotencyKey());
            }
        }
        verifyNoInteractions(applications, tasks, audit);
    }

    @Test
    void historicalReceiptDoesNotAcquireAnIdempotencyKeyDuringMapping() {
        var persistence = new MesTeamLeaderActiveOrderReleaseApplicationPersistenceService(null, null, null);
        var application = new MesProcessPoolActiveOrderReleaseApplicationDO().setId(1L);
        assertNull(persistence.toResult(application).getRequestIdempotencyKey());
        assertNull(application.getRequestIdempotencyKey());
    }
}
