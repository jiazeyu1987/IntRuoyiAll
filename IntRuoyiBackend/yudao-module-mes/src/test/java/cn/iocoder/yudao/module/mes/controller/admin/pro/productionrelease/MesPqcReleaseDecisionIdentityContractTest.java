package cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.productionrelease.vo.MesPqcProductionReleaseApproveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowIdempotency;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcProductionReleaseService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcProductionReleaseServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MesPqcReleaseDecisionIdentityContractTest {

    @Test
    void decisionHashMatchesFrontendUtf8LengthFramedVector() {
        assertEquals("a916b4a5f964f94a0ba7d4e133819a30f8ed4b54cde52163f99a380edb5d93f2",
                MesReleaseFlowIdempotency.payloadHash("APPROVE", "9007199254740993", "9007199254740995",
                        "3", "7", "批准意见🙂", "UDI-01"));
    }

    @Test
    void formalGetParsesSavedIdentityAndControllerPreservesItWithoutIncomingKey() {
        verifyIdentity("stored-key", "a".repeat(64));
    }

    @Test
    void historicalMissingOrEmptyDecisionIdentityRemainsMissingOrEmpty() {
        verifyIdentity(null, null);
        verifyIdentity("", "");
        verifyIdentity("stored-key", null);
        verifyIdentity(null, "a".repeat(64));
    }

    private void verifyIdentity(String savedKey, String savedHash) {
        var applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
        var tasks = mock(MesProEdhrWorkTaskMapper.class);
        var application = new MesProcessPoolActiveOrderReleaseApplicationDO()
                .setId(9007199254740993L).setPqcReleaseWorkTaskId(9007199254740995L);
        String identity = (savedKey == null ? "" : ",\"decisionIdempotencyKey\":\"" + savedKey + "\"")
                + (savedHash == null ? "" : ",\"decisionPayloadHash\":\"" + savedHash + "\"");
        application.setDossierSummaryJson("{\"applicationId\":9007199254740993,"
                + "\"pqcReleaseWorkTaskId\":9007199254740995,\"decision\":\"APPROVE\","
                + "\"decidedBy\":7" + identity + "}");
        var task = new MesProEdhrWorkTaskDO().setId(application.getPqcReleaseWorkTaskId())
                .setTaskType("PQC_PRODUCTION_RELEASE").setBusinessScopeType("RELEASE_APPLICATION")
                .setBusinessScopeId(application.getId()).setCandidateUserSnapshot("7");
        when(applications.selectById(application.getId())).thenReturn(application);
        when(tasks.selectById(task.getId())).thenReturn(task);
        var readService = new MesPqcProductionReleaseServiceImpl(applications, null, null, tasks,
                null, null, null, null, null, null, null, null);
        var stored = readService.get(7L, application.getId());
        assertEquals(savedKey, stored.getDecisionIdempotencyKey());
        assertEquals(savedHash, stored.getDecisionPayloadHash());
        var postService = mock(MesPqcProductionReleaseService.class);
        when(postService.approve(eq(7L), any())).thenReturn(stored);
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(7L);
            var get = new MesProductionReleaseController(readService).get(application.getId()).getData();
            var post = new MesProductionReleaseController(postService).approve(
                    new MesPqcProductionReleaseApproveReqVO().setApplicationId(application.getId())
                            .setPqcReleaseWorkTaskId(task.getId()).setIdempotencyKey("different-incoming-key")).getData();
            assertEquals(savedKey, get.getDecisionIdempotencyKey());
            assertEquals(savedHash, get.getDecisionPayloadHash());
            assertEquals(savedKey, post.getDecisionIdempotencyKey());
            assertEquals(savedHash, post.getDecisionPayloadHash());
            assertEquals(application.getId(), get.getApplicationId());
            assertEquals(task.getId(), get.getPqcReleaseWorkTaskId());
            assertEquals(7L, get.getDecidedBy());
        }
        verify(applications, org.mockito.Mockito.times(2)).selectById(application.getId());
        verify(tasks, org.mockito.Mockito.times(2)).selectById(task.getId());
        verifyNoMoreInteractions(applications, tasks);
    }
}
