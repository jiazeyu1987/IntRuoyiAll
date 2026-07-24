package cn.iocoder.yudao.module.mes.service.pro.route.importer;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessFlowEdgeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionRuleCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeSaveCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteOwnerPermissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_PROCESS_STATUS_INVALID;
import static java.util.stream.Collectors.groupingBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Sheet1RouteExcelImportServiceImplTest {

    private static final Path FIXTURE = Path.of("D:\\ocr2\\resource\\球囊扩张导管工序(1).xlsx");

    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProProcessMapper processMapper;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProRouteProcessFlowEdgeMapper routeProcessFlowEdgeMapper;
    @Mock
    private MesProEdhrPermissionScopeService permissionScopeService;

    private Sheet1RouteExcelImportServiceImpl importService;

    @BeforeEach
    void setUp() {
        importService = new Sheet1RouteExcelImportServiceImpl(
                new Sheet1RouteExcelParser(), routeMapper, processMapper, routeProcessMapper,
                routeProcessFlowEdgeMapper, new MesProRouteOwnerPermissionServiceImpl(permissionScopeService));
    }

    @Test
    void importFixture_createsTwoDisabledRoutesAndDeduplicatedRouteProcesses() throws Exception {
        AtomicLong routeId = new AtomicLong(900100L);
        AtomicLong processId = new AtomicLong(910000L);
        AtomicLong routeProcessId = new AtomicLong(920000L);
        when(routeMapper.selectListByCodePrefix("ROUTE-XLSX-"))
                .thenReturn(List.of(MesProRouteDO.builder().id(1L).code("ROUTE-XLSX-00012").build()));
        when(processMapper.selectListByCodePrefix("PROC-XLSX-"))
                .thenReturn(List.of(MesProProcessDO.builder().id(2L).code("PROC-XLSX-00005").build()));
        when(processMapper.selectByName(anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0, String.class);
            if ("吹球囊成型".equals(name)) {
                return MesProProcessDO.builder().id(80001L).code("LOCAL-P001").name("吹球囊成型").status(0).build();
            }
            return null;
        });
        when(routeMapper.insert(any(MesProRouteDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProRouteDO.class).setId(routeId.incrementAndGet());
            return 1;
        });
        when(processMapper.insert(any(MesProProcessDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProProcessDO.class).setId(processId.incrementAndGet());
            return 1;
        });
        when(routeProcessMapper.insert(any(MesProRouteProcessDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProRouteProcessDO.class).setId(routeProcessId.incrementAndGet());
            return 1;
        });

        MockMultipartFile file = new MockMultipartFile("file", "球囊扩张导管工序(1).xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", Files.readAllBytes(FIXTURE));

        Long creatorUserId = 701L;
        Sheet1RouteExcelImportResult result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(creatorUserId);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("sheet1-importer");
            result = importService.importExcel(file, CommonStatusEnum.ENABLE.getStatus());
        }

        assertEquals(2, result.getRouteCount());
        assertEquals(50, result.getRouteProcessCount());
        assertEquals(32, result.getProcessCreatedCount());
        assertEquals(1, result.getProcessReusedCount());
        assertIterableEquals(List.of("ROUTE-XLSX-00013", "ROUTE-XLSX-00014"), result.getRouteCodes());

        ArgumentCaptor<MesProRouteDO> routeCaptor = ArgumentCaptor.forClass(MesProRouteDO.class);
        verify(routeMapper, times(2)).insert(routeCaptor.capture());
        assertIterableEquals(List.of("ROUTE-XLSX-00013", "ROUTE-XLSX-00014"),
                routeCaptor.getAllValues().stream().map(MesProRouteDO::getCode).toList());
        assertTrue(routeCaptor.getAllValues().stream()
                .allMatch(route -> CommonStatusEnum.DISABLE.getStatus().equals(route.getStatus())));
        assertTrue(routeCaptor.getAllValues().stream()
                .allMatch(route -> route.getDescription().contains("Sheet1")));
        assertTrue(routeCaptor.getAllValues().stream()
                .allMatch(route -> route.getRemark() != null && !route.getRemark().isBlank()));

        ArgumentCaptor<MesProProcessDO> processCaptor = ArgumentCaptor.forClass(MesProProcessDO.class);
        verify(processMapper, times(32)).insert(processCaptor.capture());
        assertTrue(processCaptor.getAllValues().stream()
                .allMatch(process -> process.getCode().startsWith("PROC-XLSX-")));
        assertTrue(processCaptor.getAllValues().stream()
                .allMatch(process -> CommonStatusEnum.ENABLE.getStatus().equals(process.getStatus())));

        ArgumentCaptor<MesProRouteProcessDO> routeProcessCaptor = ArgumentCaptor.forClass(MesProRouteProcessDO.class);
        verify(routeProcessMapper, times(50)).insert(routeProcessCaptor.capture());
        assertTrue(routeProcessCaptor.getAllValues().stream().allMatch(routeProcess -> !routeProcess.getKeyFlag()));
        assertTrue(routeProcessCaptor.getAllValues().stream().allMatch(routeProcess -> !routeProcess.getCheckFlag()));
        assertEquals(List.of(24, 26), routeProcessCaptor.getAllValues().stream()
                .collect(groupingBy(MesProRouteProcessDO::getRouteId)).values().stream()
                .map(List::size)
                .sorted()
                .toList());
        assertFalse(routeProcessCaptor.getAllValues().stream()
                .anyMatch(routeProcess -> routeProcess.getSort() == null || routeProcess.getSort() <= 0));

        ArgumentCaptor<MesProRouteProcessFlowEdgeDO> edgeCaptor =
                ArgumentCaptor.forClass(MesProRouteProcessFlowEdgeDO.class);
        verify(routeProcessFlowEdgeMapper, times(48)).insert(edgeCaptor.capture());
        assertTrue(edgeCaptor.getAllValues().stream()
                .allMatch(edge -> edge.getGraphVersion() == 1L && "NORMAL".equals(edge.getRelationType())));

        ArgumentCaptor<MesProEdhrPermissionScopeSaveCommand> permissionCaptor =
                ArgumentCaptor.forClass(MesProEdhrPermissionScopeSaveCommand.class);
        verify(permissionScopeService, times(2)).saveRules(permissionCaptor.capture());
        assertOwnerPermissions(permissionCaptor.getAllValues(), List.of(900101L, 900102L),
                creatorUserId, "sheet1-importer");
    }

    @Test
    void importFixture_invalidProcessStatusFailsBeforeInsert() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "球囊扩张导管工序(1).xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", Files.readAllBytes(FIXTURE));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> importService.importExcel(file, 99));

        assertEquals(PRO_ROUTE_IMPORT_PROCESS_STATUS_INVALID.getCode(), exception.getCode());
        verify(routeMapper, never()).insert(any(MesProRouteDO.class));
        verify(processMapper, never()).insert(any(MesProProcessDO.class));
        verify(routeProcessMapper, never()).insert(any(MesProRouteProcessDO.class));
        verify(routeProcessFlowEdgeMapper, never()).insert(any(MesProRouteProcessFlowEdgeDO.class));
        verifyNoInteractions(permissionScopeService);
    }

    private void assertOwnerPermissions(List<MesProEdhrPermissionScopeSaveCommand> commands,
                                        List<Long> routeIds,
                                        Long actorUserId,
                                        String actorUsername) {
        assertEquals(routeIds, commands.stream()
                .map(command -> Long.valueOf(command.getObjectId()))
                .toList());
        for (int index = 0; index < commands.size(); index++) {
            MesProEdhrPermissionScopeSaveCommand command = commands.get(index);
            Long routeId = routeIds.get(index);
            assertEquals("route-" + routeId, command.getScopeName());
            assertEquals("ROUTE", command.getObjectType());
            assertEquals(actorUserId, command.getActorUserId());
            assertEquals(actorUsername, command.getActorUsername());
            assertEquals(List.of("VIEW", "ROUTE_EDIT", "PERMISSION_ADMIN"),
                    command.getRules().stream().map(MesProEdhrPermissionRuleCommand::getAbility).toList());
            command.getRules().forEach(rule -> {
                assertEquals("USER", rule.getSubjectType());
                assertEquals(actorUserId, rule.getSubjectId());
                assertEquals("ALLOW", rule.getDecision());
                assertEquals("ENABLED", rule.getStatus());
            });
        }
    }
}
