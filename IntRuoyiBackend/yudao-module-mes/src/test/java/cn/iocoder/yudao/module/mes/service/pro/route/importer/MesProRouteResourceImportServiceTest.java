package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.LinkedHashMap;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesProRouteResourceImportServiceTest {
    private final MesProRouteResourceImportService service = new MesProRouteResourceImportService();
    private final MesProRouteResourceTransferService reader = mock(MesProRouteResourceTransferService.class);
    private final Workbook workbook = mock(Workbook.class);

    @BeforeEach void prepare() {
        TenantContextHolder.setTenantId(189L);
        ReflectionTestUtils.setField(service, "reader", reader);
        var resources = new LinkedHashMap<String, JSONObject>();
        resources.put("mes_pro_route:11", JSON.parseObject("{\"id\":11,\"code\":\"ROUTE-001\"}"));
        resources.put("mes_pro_route_version:12", JSON.parseObject("{\"id\":12,\"route_id\":11,\"active\":true,\"lifecycle_status\":\"ACTIVE\"}"));
        resources.put("mes_pro_route_dcc_project_binding:13", JSON.parseObject("{\"id\":13,\"route_id\":11,\"dcc_project_code_id\":14}"));
        resources.put("dcc_project_code:14", JSON.parseObject("{\"id\":14,\"project_code\":\"PROJECT-001\"}"));
        when(reader.readResources(workbook)).thenReturn(resources);
    }

    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    private JdbcTemplate jdbcWithConflict(String table, boolean deleted) {
        JdbcTemplate jdbc = mock(JdbcTemplate.class, call -> {
            if (call.getMethod().getName().equals("queryForList")) {
                String sql = call.getArgument(0);
                Object[] args = (Object[]) call.getRawArguments()[2];
                assertEquals(189L, args[1]);
                return sql.contains("FROM " + table + " WHERE") && sql.contains("deleted=" + (deleted ? "1" : "0"))
                    ? List.of(901L) : List.of();
            }
            return RETURNS_DEFAULTS.answer(call);
        });
        ReflectionTestUtils.setField(service, "jdbc", jdbc);
        return jdbc;
    }

    @Test void deletedRouteCodeFailsWithActionableReasonBeforeAnyInsert() {
        JdbcTemplate jdbc = jdbcWithConflict("mes_pro_route", true);
        ServiceException error = assertThrows(ServiceException.class, () -> service.importWorkbook(workbook));
        assertTrue(error.getMessage().contains("已删除的工艺流程仍占用编码：ROUTE-001"));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test void deletedProjectCodeAlsoFailsBeforeAnyInsert() {
        JdbcTemplate jdbc = jdbcWithConflict("dcc_project_code", true);
        ServiceException error = assertThrows(ServiceException.class, () -> service.importWorkbook(workbook));
        assertTrue(error.getMessage().contains("已删除的项目仍占用编码：PROJECT-001"));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test void liveRouteStillRejectsDuplicateImportBeforeAnyInsert() {
        JdbcTemplate jdbc = jdbcWithConflict("mes_pro_route", false);
        ServiceException error = assertThrows(ServiceException.class, () -> service.importWorkbook(workbook));
        assertTrue(error.getMessage().contains("路线编码已存在：ROUTE-001"));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test void sharedWorkstationMachineRelationUsesMappedTargetIdentity() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class, call -> {
            if (call.getMethod().getName().equals("queryForList")) {
                assertTrue(((String) call.getArgument(0)).contains("workstation_id <=> ? AND machinery_id <=> ?"));
                assertArrayEquals(new Object[]{101L, 102L, 189L}, (Object[]) call.getRawArguments()[2]);
                return List.of(901L);
            }
            return RETURNS_DEFAULTS.answer(call);
        });
        ReflectionTestUtils.setField(service, "jdbc", jdbc);
        var rows = java.util.Map.of("mes_md_workstation_machine:3", JSON.parseObject("{\"id\":3,\"workstation_id\":1,\"machinery_id\":2}"));
        var ids = new java.util.HashMap<>(java.util.Map.of("mes_md_workstation:1",101L,"mes_dv_machinery:2",102L,"mes_md_workstation_machine:3",103L));
        var existing = new java.util.HashSet<String>();
        service.reuseSharedRelations(rows, ids, existing, 189L);
        assertEquals(901L, ids.get("mes_md_workstation_machine:3"));
        assertTrue(existing.contains("mes_md_workstation_machine:3"));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test void ambiguousExistingRelationsFailInsteadOfAddingAnother() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class, call -> call.getMethod().getName().equals("queryForList")
            ? List.of(901L,902L) : RETURNS_DEFAULTS.answer(call));
        ReflectionTestUtils.setField(service, "jdbc", jdbc);
        var rows = java.util.Map.of("mes_md_workstation_machine:3", JSON.parseObject("{\"id\":3,\"workstation_id\":1,\"machinery_id\":2}"));
        var ids = new java.util.HashMap<>(java.util.Map.of("mes_md_workstation:1",101L,"mes_dv_machinery:2",102L));
        assertThrows(ServiceException.class, () -> service.reuseSharedRelations(rows, ids, new java.util.HashSet<>(), 189L));
    }
}
