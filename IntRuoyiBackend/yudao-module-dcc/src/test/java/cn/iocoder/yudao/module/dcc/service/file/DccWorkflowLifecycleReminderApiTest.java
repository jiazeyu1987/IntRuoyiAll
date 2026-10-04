package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccWorkflowLifecycleController;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DccWorkflowLifecycleReminderApiTest {
    @Test
    void oneReminderListDoesNotMixBusinessDaysWhenTheServerClockCrossesMidnight() {
        var files=mock(DccControlledFileMapper.class);var permissions=mock(PermissionApi.class);
        var categories=mock(DccControlledFileCategoryPermissionSupport.class);
        when(permissions.hasAnyRoles(99L,"doc_control")).thenReturn(true);
        when(categories.hasCategoryPermission(any(),eq(99L),any())).thenReturn(true);
        var dates=spy(new DccWorkflowDatePolicy());dates.setZoneId("Asia/Singapore");dates.setReminderLeadDays(3);
        LocalDate day=LocalDate.of(2026,10,10);
        doReturn(day.atTime(23,59,59),day.plusDays(1).atStartOfDay()).when(dates).now();
        var lifecycle=new DccControlledFileLifecycleService();
        ReflectionTestUtils.setField(lifecycle,"datePolicy",dates);ReflectionTestUtils.setField(lifecycle,"controlledFileMapper",files);
        when(files.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(file(1L,day),file(2L,day)));
        var controller=new DccWorkflowLifecycleController(mock(DccControlledFileWorkflowService.class),
                mock(DccWorkflowDistributionService.class),lifecycle,categories,permissions);
        TenantContextHolder.setTenantId(1L);
        try(var login=mockStatic(SecurityFrameworkUtils.class)) {
            login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            List<Map> response=JsonUtils.parseArray(JsonUtils.toJsonString(controller.pending(false).getData()),Map.class);
            assertEquals(List.of("DUE","DUE"),response.stream().map(row->row.get("distributionReminderStage")).toList());
            verify(dates,times(1)).now();
        } finally { TenantContextHolder.clear(); }
    }
    @Test
    void reminderResponseClassifiesDatesWithTheConfiguredServerPolicy() {
        var files=mock(DccControlledFileMapper.class);
        var permissions=mock(PermissionApi.class);
        var categories=mock(DccControlledFileCategoryPermissionSupport.class);
        when(permissions.hasAnyRoles(99L,"doc_control")).thenReturn(true);
        when(categories.hasCategoryPermission(any(),eq(99L),any())).thenReturn(true);
        var lifecycle=new DccControlledFileLifecycleService();
        var dates=new DccWorkflowDatePolicy();dates.setZoneId("Asia/Singapore");dates.setReminderLeadDays(3);
        ReflectionTestUtils.setField(lifecycle,"datePolicy",dates);
        ReflectionTestUtils.setField(lifecycle,"controlledFileMapper",files);
        LocalDate today=dates.now().toLocalDate();
        when(files.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(
                file(1L,today.minusDays(1)),file(2L,today),file(3L,today.plusDays(3)),file(4L,today.plusDays(4))));
        var controller=new DccWorkflowLifecycleController(mock(DccControlledFileWorkflowService.class),
                mock(DccWorkflowDistributionService.class),lifecycle,categories,permissions);
        TenantContextHolder.setTenantId(1L);
        try(var login=mockStatic(SecurityFrameworkUtils.class)) {
            login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            List<Map> response=JsonUtils.parseArray(JsonUtils.toJsonString(controller.pending(false).getData()),Map.class);
            assertEquals(List.of("OVERDUE","DUE","UPCOMING","FUTURE"),response.stream()
                    .map(row->row.get("distributionReminderStage")).toList());
            dates.setReminderLeadDays(null);
            assertThrows(IllegalStateException.class,()->controller.pending(false));
        } finally { TenantContextHolder.clear(); }
    }
    @Test
    void lifecycleResponseDatesSerializeAsTheDocumentControlStringContract() {
        var response=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO();
        response.setEffectiveDate(LocalDate.of(2026,10,10));
        response.setControlledTime(java.time.LocalDateTime.of(2026,10,1,8,30));
        response.setActivatedTime(java.time.LocalDateTime.of(2026,10,10,0,0));
        response.setDistributedTime(java.time.LocalDateTime.of(2026,10,2,9,45));
        var serialized=JsonUtils.parseObject(JsonUtils.toJsonString(response),Map.class);
        assertEquals("2026-10-10",serialized.get("effectiveDate"));
        assertEquals("2026-10-01 08:30:00",serialized.get("controlledTime"));
        assertEquals("2026-10-10 00:00:00",serialized.get("activatedTime"));
        assertEquals("2026-10-02 09:45:00",serialized.get("distributedTime"));
    }
    private DccControlledFileDO file(Long id,LocalDate effective) {
        return DccControlledFileDO.builder().id(id).tenantId(1L).categoryId(10L).effectiveDate(effective)
                .controlledTime(effective.minusDays(1).atStartOfDay()).status("ACTIVE").fileNumber("SOP-"+id).versionNo("A/1").build();
    }
}
