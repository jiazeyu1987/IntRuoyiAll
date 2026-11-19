package cn.iocoder.yudao.module.dcc.registrationcertificate.service.query;

import org.flowable.engine.HistoryService;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DccRegistrationCertificateEvidenceOpinionTest {
    @Test
    void readsHistoricOpinionsEvenWhenBindingOpinionIsAbsentWithTenantAndProcessScope() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        HistoryService history = mock(HistoryService.class);
        HistoricTaskInstanceQuery query = mock(HistoricTaskInstanceQuery.class, RETURNS_SELF);
        HistoricTaskInstance approved = mock(HistoricTaskInstance.class);
        HistoricTaskInstance rejected = mock(HistoricTaskInstance.class);
        HistoricTaskInstance cancelled = mock(HistoricTaskInstance.class);
        Map<String,Object> application = new LinkedHashMap<>();
        application.put("id", 573L);
        application.put("bpmProcessInstanceId", "process-573");
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            return sql.contains("SELECT r.id,r.request_type") ? List.of(application) : List.of();
        });
        when(history.createHistoricTaskInstanceQuery()).thenReturn(query);
        when(query.list()).thenReturn(List.of(approved, rejected, cancelled));
        when(approved.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS", 2, "TASK_REASON", "verified opinion"));
        when(rejected.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS", 3, "TASK_REASON", "revision requested"));
        when(cancelled.getTaskLocalVariables()).thenReturn(Map.of("TASK_STATUS", 4, "TASK_REASON", "cancel reason"));
        DccRegistrationCertificateEvidenceService service = new DccRegistrationCertificateEvidenceService(jdbc);
        ReflectionTestUtils.setField(service, "historyService", history);
        service.getEvidence(1L, 2L, 3L);
        verify(query).taskTenantId("1");
        verify(query).processInstanceId("process-573");
        List<?> reviews = (List<?>) application.get("reviews");
        assertEquals(2, reviews.size());
        assertEquals("revision requested", ((Map<?,?>) reviews.get(1)).get("reason"));
        verify(query).finished();
        verify(query).orderByHistoricTaskInstanceEndTime();
        verify(query).asc();
        assertEquals("verified opinion", ((Map<?,?>) reviews.get(0)).get("reason"));
    }
}
