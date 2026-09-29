package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventRelationMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GxpAuditQueryServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private GxpAuditEventMapper auditEventMapper;

    @Mock
    private GxpAuditEventRelationMapper auditEventRelationMapper;

    @InjectMocks
    private GxpAuditQueryServiceImpl queryService;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void get_shouldReadOnlyWithinCurrentTenant() {
        TenantContextHolder.setTenantId(1L);
        GxpAuditEventDO event = new GxpAuditEventDO();
        event.setId(9L);
        event.setTenantId(1L);
        when(auditEventMapper.selectByTenantIdAndId(1L, 9L)).thenReturn(event);

        assertSame(event, queryService.get(1L, 9L));
        verify(auditEventMapper).selectByTenantIdAndId(1L, 9L);
    }

    @Test
    void get_shouldRejectCrossTenantReadWithoutQueryingMapper() {
        TenantContextHolder.setTenantId(1L);

        assertNull(queryService.get(2L, 9L));
        verify(auditEventMapper, never()).selectByTenantIdAndId(1L, 9L);
    }

    @Test
    void page_shouldUseCurrentTenantAndScopeRelations() {
        TenantContextHolder.setTenantId(1L);
        GxpAuditEventPageQuery query = new GxpAuditEventPageQuery()
                .setScopeType("ACTIVE_ORDER")
                .setScopeId(100L)
                .setPageNo(1)
                .setPageSize(20);
        when(auditEventRelationMapper.selectEventIdsByTargets(1L, List.of("ACTIVE_ORDER"), "100"))
                .thenReturn(List.of(9L));
        when(auditEventMapper.selectPageByScope(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.same(query),
                org.mockito.ArgumentMatchers.eq(List.of(9L)),
                org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(new cn.iocoder.yudao.framework.common.pojo.PageResult<>(List.of(), 0L));

        queryService.page(query);

        verify(auditEventRelationMapper).selectEventIdsByTargets(1L, List.of("ACTIVE_ORDER"), "100");
        verify(auditEventMapper).selectPageByScope(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.same(query),
                org.mockito.ArgumentMatchers.eq(List.of(9L)),
                org.mockito.ArgumentMatchers.eq(List.of("ACTIVE_ORDER:100", "MES_ACTIVE_ORDER:100")));
    }
}
