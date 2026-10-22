package cn.iocoder.yudao.module.bpm.approval.service.ownership;

import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalRequestMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Exercises the new mapper default methods, including their exact query predicates. */
class NativeApprovalOwnershipMapperTest {
    @Test void actionRequestQueryUsesExactTenantProcessDeletedCurrentRead() {
        var mapper = mock(BusinessApprovalRequestMapper.class, CALLS_REAL_METHODS);
        doReturn(java.util.List.of()).when(mapper).selectList(any(Wrapper.class));
        mapper.selectOwnershipByTenantAndProcessIdForUpdate(122L, "process-1");
        ArgumentCaptor<Wrapper> query = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectList(query.capture());
        assertQuery(query.getValue(), "process_instance_id");
        assertTrue(query.getValue().getSqlSegment().endsWith("FOR UPDATE"));
    }
    @Test void requestOwnershipQueryHasExplicitTenantProcessDeleteAndNoLimitOrOrder() {
        var mapper = mock(BusinessApprovalRequestMapper.class, CALLS_REAL_METHODS);
        doReturn(java.util.List.of()).when(mapper).selectList(any(Wrapper.class));
        mapper.selectOwnershipByTenantAndProcessId(122L, "process-1");
        ArgumentCaptor<Wrapper> query = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectList(query.capture());
        assertQuery(query.getValue(), "process_instance_id");
    }

    @Test void formOwnershipQueryHasExplicitTenantProcessDeleteAndNoLimitOrOrder() {
        var mapper = mock(FormActionInstanceMapper.class, CALLS_REAL_METHODS);
        doReturn(java.util.List.of()).when(mapper).selectList(any(Wrapper.class));
        mapper.selectOwnershipByTenantAndProcessId(122L, "process-1");
        ArgumentCaptor<Wrapper> query = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper).selectList(query.capture());
        assertQuery(query.getValue(), "bpm_process_instance_id");
    }

    private void assertQuery(Wrapper query, String processColumn) {
        String sql = query.getSqlSegment();
        assertTrue(sql.contains("tenant_id") && sql.contains(processColumn) && sql.contains("deleted"));
        assertFalse(sql.toUpperCase().contains("LIMIT") || sql.toUpperCase().contains("ORDER BY"));
        var values = ((com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<?>) query).getParamNameValuePairs();
        assertTrue(values.containsValue(122L) && values.containsValue("process-1") && values.containsValue(false));
    }
}
