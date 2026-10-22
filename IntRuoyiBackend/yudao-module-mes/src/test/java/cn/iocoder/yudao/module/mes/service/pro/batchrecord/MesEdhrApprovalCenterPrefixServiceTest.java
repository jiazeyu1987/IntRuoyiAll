package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesEdhrApprovalCenterPrefixServiceTest {
    private MesProEdhrWorkTaskMapper mapper;
    private MesProEdhrWorkTaskServiceImpl service;

    @BeforeEach
    void setup() {
        TenantContextHolder.setTenantId(1L);
        mapper = mock(MesProEdhrWorkTaskMapper.class);
        service = new MesProEdhrWorkTaskServiceImpl();
        ReflectionTestUtils.setField(service,"workTaskMapper",mapper);
    }

    @AfterEach
    void clear() { TenantContextHolder.clear(); }

    @Test
    void personalAndGlobalUseActualLoginTenantAndIndependent400Window() {
        var filters = new MesProEdhrWorkTaskPageReqVO();
        when(mapper.selectApprovalCenterPrefix(eq(filters),nullable(Long.class),eq(1L),eq(false),eq("TODO"),eq(400L)))
                .thenReturn(new PageResult<MesProEdhrWorkTaskDO>(List.of(),321L));
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(12L);
            assertEquals(321L,service.getApprovalCenterPrefix(filters,false,false,400).getTotal());
            assertEquals(321L,service.getApprovalCenterPrefix(filters,true,false,400).getTotal());
        }
        verify(mapper).selectApprovalCenterPrefix(filters,12L,1L,false,"TODO",400);
        verify(mapper).selectApprovalCenterPrefix(filters,null,1L,false,"TODO",400);
        assertTrue(filters.getPageSize() <= 200);
    }

    @Test
    void overdueStatusRemainsExplicitAndDoneUsesFormalQueryFlag() {
        var filters = new MesProEdhrWorkTaskPageReqVO();
        filters.setStatus("OVERDUE");
        when(mapper.selectApprovalCenterPrefix(eq(filters),eq(12L),eq(1L),anyBoolean(),eq("OVERDUE"),eq(20L)))
                .thenReturn(new PageResult<MesProEdhrWorkTaskDO>(List.of(),0L));
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(12L);
            service.getApprovalCenterPrefix(filters,false,false,20);
            service.getApprovalCenterPrefix(filters,false,true,20);
        }
        verify(mapper).selectApprovalCenterPrefix(filters,12L,1L,false,"OVERDUE",20);
        verify(mapper).selectApprovalCenterPrefix(filters,12L,1L,true,"OVERDUE",20);
    }

    @Test
    void absentLoginTenantOrInvalidTodoStatusFailsBeforeQuery() {
        var filters = new MesProEdhrWorkTaskPageReqVO();
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(null);
            var unauthorized = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                    () -> service.getApprovalCenterPrefix(filters,true,false,20));
            assertEquals(cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.UNAUTHORIZED.getCode(),
                    unauthorized.getCode());
            verifyNoInteractions(mapper);
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(12L);
            filters.setStatus("DONE");
            assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                    () -> service.getApprovalCenterPrefix(filters,false,false,20));
            verifyNoInteractions(mapper);
            filters.setStatus("TODO");
            TenantContextHolder.clear();
            assertThrows(NullPointerException.class,() -> service.getApprovalCenterPrefix(filters,false,false,20));
            verifyNoInteractions(mapper);
        }
        verifyNoInteractions(mapper);
    }
}
