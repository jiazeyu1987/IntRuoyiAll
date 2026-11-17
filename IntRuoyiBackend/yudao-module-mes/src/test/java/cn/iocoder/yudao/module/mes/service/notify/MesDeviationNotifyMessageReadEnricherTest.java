package cn.iocoder.yudao.module.mes.service.notify;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyMessageDO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesDeviationNotifyMessageReadEnricherTest {
    private final MesProEdhrWorkTaskMapper tasks = mock(MesProEdhrWorkTaskMapper.class);
    private final MesDeviationNotifyMessageReadEnricher enricher = new MesDeviationNotifyMessageReadEnricher();
    MesDeviationNotifyMessageReadEnricherTest() { ReflectionTestUtils.setField(enricher, "taskMapper", tasks); }
    private NotifyMessageDO message(String code, Object id) {
        var params = new LinkedHashMap<String, Object>(); params.put("workTaskId", id);
        params.put("actionUrl", "/detail?deviationId=13"); params.put("reason", "QA");
        return new NotifyMessageDO().setTenantId(7L).setUserId(42L).setTemplateCode(code).setTemplateParams(params);
    }
    @Test void historicalReviewUsesFormalDutyAndOrderWithoutChangingRecipientOrTask() {
        var message = message("MES_EDHR_DEVIATION_REVIEW_ASSIGNED", "91");
        when(tasks.selectNotificationSource(91L, 7L)).thenReturn(new MesProEdhrWorkTaskDO()
            .setId(91L).setTaskType("DEVIATION_APPROVE").setSignatureCellKey("QUALITY_OWNER")
            .setAssigneeUserId(99L).setWorkOrderCode("WO-1").setBatchCode(null));
        enricher.enrich(message);
        assertEquals("完成质量负责人批准", message.getTemplateParams().get("taskTitle"));
        assertEquals("WO-1", message.getTemplateParams().get("workOrderCode"));
        assertEquals(42L, message.getUserId());
        verify(tasks).selectNotificationSource(91L, 7L); verifyNoMoreInteractions(tasks);
    }
    @Test void completeHistoricalSnapshotDoesNotDependOnCurrentTaskOwnerOrExistence() {
        var message = message("MES_EDHR_DEVIATION_RECEIVE_ASSIGNED", "91");
        message.getTemplateParams().put("taskTitle", "开展偏差调查");
        message.getTemplateParams().put("workOrderCode", "WO-1");
        enricher.enrich(message);
        assertEquals("开展偏差调查", message.getTemplateParams().get("taskTitle"));
        verifyNoInteractions(tasks);
    }
    @Test void invalidFormalTaskProducesExplicitItemErrorAndNoExecutableEntry() {
        var message = message("MES_EDHR_DEVIATION_CORRECT_ASSIGNED", "91");
        when(tasks.selectNotificationSource(91L, 7L)).thenReturn(new MesProEdhrWorkTaskDO()
            .setId(91L).setTaskType("DEVIATION_RECEIVE").setWorkOrderCode("WO-1"));
        enricher.enrich(message);
        assertTrue(message.getTemplateParams().get("presentationError").toString().contains("类型不一致"));
        assertFalse(message.getTemplateParams().containsKey("workTaskId"));
        assertFalse(message.getTemplateParams().containsKey("actionUrl"));
        assertFalse(message.getTemplateContent().contains("91"));
    }
    @Test void malformedAndMissingIdentityDoNotPreventReadingOtherMessages() {
        for (Object id : List.of("bad", 1.5, "9223372036854775808", -1L)) {
            var message = message("MES_EDHR_DEVIATION_REVIEW_ASSIGNED", id);
            enricher.enrich(message);
            assertNotNull(message.getTemplateParams().get("presentationError"));
        }
        var missing = message("MES_EDHR_DEVIATION_REVIEW_ASSIGNED", null);
        enricher.enrich(missing);
        assertNotNull(missing.getTemplateParams().get("presentationError"));
        verifyNoInteractions(tasks);
    }
    @Test void missingTaskOrDutyIsAnExplicitReadFailureForThatItem() {
        var missing = message("MES_EDHR_DEVIATION_REVIEW_ASSIGNED", "91");
        enricher.enrich(missing);
        assertTrue(missing.getTemplateContent().contains("已失效"));
        when(tasks.selectNotificationSource(91L, 7L)).thenReturn(new MesProEdhrWorkTaskDO()
            .setTaskType("DEVIATION_SIGN").setWorkOrderCode("WO-1").setSignatureCellKey("BAD"));
        var invalid = message("MES_EDHR_DEVIATION_REVIEW_ASSIGNED", "91");
        enricher.enrich(invalid);
        assertTrue(invalid.getTemplateContent().contains("有效职责"));
    }
    @Test void unrelatedNotificationDoesNotReadMesTables() {
        enricher.enrich(new NotifyMessageDO());
        enricher.enrich(new NotifyMessageDO().setTemplateCode("OTHER"));
        verifyNoInteractions(tasks);
    }
}
