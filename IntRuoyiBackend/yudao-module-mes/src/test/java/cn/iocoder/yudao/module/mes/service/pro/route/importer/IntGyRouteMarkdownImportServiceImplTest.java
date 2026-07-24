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
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_FINAL_PROCESS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_CHECK_PROCESS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_INVALID_MARKDOWN;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_PROCESS_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_PROCESS_NAME_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_PROCESS_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_ROUTE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_ROUTE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_IMPORT_SEQUENCE_DUPLICATE;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntGyRouteMarkdownImportServiceImplTest {

    private static final Path CURRENT_EXPORT_FIXTURE = Path.of(
            "D:\\ProjectPackage\\Int\\IntGY\\doc\\exports\\current-two-imported-process-routes-20260512.md");

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

    private IntGyRouteMarkdownImportServiceImpl importService;

    @BeforeEach
    void setUp() {
        importService = new IntGyRouteMarkdownImportServiceImpl(
                new IntGyRouteMarkdownParser(), routeMapper, processMapper, routeProcessMapper,
                routeProcessFlowEdgeMapper, new MesProRouteOwnerPermissionServiceImpl(permissionScopeService));
    }

    @Test
    void importCurrentExport_createsDisabledRoutesProcessesAndRouteProcessesWithFinalKeyFlags() throws Exception {
        String markdown = readFixture();
        AtomicLong routeId = new AtomicLong(1000L);
        AtomicLong processId = new AtomicLong(2000L);
        AtomicLong routeProcessId = new AtomicLong(3000L);
        when(routeMapper.selectByCode(anyString())).thenReturn(null);
        when(processMapper.selectByCode(anyString())).thenReturn(null);
        when(processMapper.selectByName(anyString())).thenReturn(null);
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

        Long creatorUserId = 702L;
        IntGyRouteMarkdownImportResult result;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(creatorUserId);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("intgy-importer");
            result = importService.importMarkdown(markdown, CommonStatusEnum.ENABLE.getStatus(), null);
        }

        assertEquals(2, result.getRouteCount());
        assertEquals(51, result.getRouteProcessCount());
        assertEquals(51, result.getProcessCreatedCount());
        assertEquals(0, result.getProcessReusedCount());
        assertIterableEquals(List.of("ROUTE-YXN.044.02.1020", "ROUTE-YXN.069.001.1001"), result.getRouteCodes());

        ArgumentCaptor<MesProRouteDO> routeCaptor = ArgumentCaptor.forClass(MesProRouteDO.class);
        verify(routeMapper, times(2)).insert(routeCaptor.capture());
        assertTrue(routeCaptor.getAllValues().stream()
                .allMatch(route -> CommonStatusEnum.DISABLE.getStatus().equals(route.getStatus())));

        ArgumentCaptor<MesProProcessDO> processCaptor = ArgumentCaptor.forClass(MesProProcessDO.class);
        verify(processMapper, times(51)).insert(processCaptor.capture());
        assertTrue(processCaptor.getAllValues().stream()
                .allMatch(process -> CommonStatusEnum.ENABLE.getStatus().equals(process.getStatus())));
        Map<Long, MesProProcessDO> processById = processCaptor.getAllValues().stream()
                .collect(toMap(MesProProcessDO::getId, Function.identity()));

        ArgumentCaptor<MesProRouteProcessDO> routeProcessCaptor = ArgumentCaptor.forClass(MesProRouteProcessDO.class);
        verify(routeProcessMapper, times(51)).insert(routeProcessCaptor.capture());
        List<String> keyProcessCodes = routeProcessCaptor.getAllValues().stream()
                .filter(routeProcess -> Boolean.TRUE.equals(routeProcess.getKeyFlag()))
                .map(routeProcess -> processById.get(routeProcess.getProcessId()).getCode())
                .toList();
        assertIterableEquals(List.of("W030", "B320"), keyProcessCodes);

        ArgumentCaptor<MesProRouteProcessFlowEdgeDO> edgeCaptor =
                ArgumentCaptor.forClass(MesProRouteProcessFlowEdgeDO.class);
        verify(routeProcessFlowEdgeMapper, times(49)).insert(edgeCaptor.capture());
        assertTrue(edgeCaptor.getAllValues().stream()
                .allMatch(edge -> edge.getGraphVersion() == 1L && "NORMAL".equals(edge.getRelationType())));

        ArgumentCaptor<MesProEdhrPermissionScopeSaveCommand> permissionCaptor =
                ArgumentCaptor.forClass(MesProEdhrPermissionScopeSaveCommand.class);
        verify(permissionScopeService, times(2)).saveRules(permissionCaptor.capture());
        assertOwnerPermissions(permissionCaptor.getAllValues(), List.of(1001L, 1002L),
                creatorUserId, "intgy-importer");
    }

    @Test
    void importCurrentExport_existingProcessCodeWithDifferentNameFailsBeforeAnyInsert() throws Exception {
        when(routeMapper.selectByCode(anyString())).thenReturn(null);
        when(processMapper.selectByCode(anyString())).thenAnswer(invocation -> {
            if ("Z470".equals(invocation.getArgument(0, String.class))) {
                return MesProProcessDO.builder().id(7L).code("Z470").name("不同工序名称").build();
            }
            return null;
        });

        assertServiceExceptionCode(() -> importService.importMarkdown(
                        readFixture(), CommonStatusEnum.ENABLE.getStatus(), null),
                PRO_ROUTE_IMPORT_PROCESS_CONFLICT.getCode());
        verifyNoInsert();
    }

    @Test
    void importCurrentExport_existingProcessNameWithDifferentCodeFailsBeforeAnyInsert() throws Exception {
        when(routeMapper.selectByCode(anyString())).thenReturn(null);
        when(processMapper.selectByCode(anyString())).thenReturn(null);
        when(processMapper.selectByName(anyString())).thenAnswer(invocation -> {
            if ("造影导管切导管".equals(invocation.getArgument(0, String.class))) {
                return MesProProcessDO.builder().id(8L).code("LOCAL-Z470").name("造影导管切导管").build();
            }
            return null;
        });

        assertServiceExceptionCode(() -> importService.importMarkdown(
                        readFixture(), CommonStatusEnum.ENABLE.getStatus(), null),
                PRO_ROUTE_IMPORT_PROCESS_NAME_EXISTS.getCode());
        verifyNoInsert();
    }

    @Test
    void importCurrentExport_existingProcessCodeWithSameNameIsReused() throws Exception {
        String markdown = readFixture();
        AtomicLong routeId = new AtomicLong(4000L);
        AtomicLong processId = new AtomicLong(5000L);
        AtomicLong routeProcessId = new AtomicLong(6000L);
        when(routeMapper.selectByCode(anyString())).thenReturn(null);
        when(processMapper.selectByCode(anyString())).thenAnswer(invocation -> {
            if ("Z470".equals(invocation.getArgument(0, String.class))) {
                return MesProProcessDO.builder().id(7000L).code("Z470").name("造影导管切导管").build();
            }
            return null;
        });
        when(processMapper.selectByName(anyString())).thenReturn(null);
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

        IntGyRouteMarkdownImportResult result = importService.importMarkdown(
                markdown, CommonStatusEnum.ENABLE.getStatus(), null);

        assertEquals(2, result.getRouteCount());
        assertEquals(50, result.getProcessCreatedCount());
        assertEquals(1, result.getProcessReusedCount());
        verify(processMapper, times(50)).insert(any(MesProProcessDO.class));
    }

    @Test
    void importCurrentExport_existingRouteCodeFailsBeforeAnyInsert() throws Exception {
        when(routeMapper.selectByCode(anyString())).thenAnswer(invocation -> {
            if ("ROUTE-YXN.044.02.1020".equals(invocation.getArgument(0, String.class))) {
                return MesProRouteDO.builder().id(9L).code("ROUTE-YXN.044.02.1020").build();
            }
            return null;
        });

        assertServiceExceptionCode(() -> importService.importMarkdown(
                        readFixture(), CommonStatusEnum.ENABLE.getStatus(), null),
                PRO_ROUTE_IMPORT_ROUTE_EXISTS.getCode());
        verifyNoInsert();
    }

    @Test
    void importDuplicateRouteCodeInMarkdown_failsBeforeAnyInsert() throws Exception {
        String duplicateRouteMarkdown = readFixture()
                .replace("## 2. ROUTE-YXN.069.001.1001", "## 2. ROUTE-YXN.044.02.1020")
                .replace("| routeCode | ROUTE-YXN.069.001.1001 |",
                        "| routeCode | ROUTE-YXN.044.02.1020 |");

        assertServiceExceptionCode(() -> importService.importMarkdown(
                        duplicateRouteMarkdown, CommonStatusEnum.ENABLE.getStatus(), null),
                PRO_ROUTE_IMPORT_ROUTE_DUPLICATE.getCode());
        verifyNoInsert();
    }

    @Test
    void importDuplicateSequenceNoInMarkdown_failsBeforeAnyInsert() throws Exception {
        String duplicateSequenceMarkdown = readFixture()
                .replace("| 2 | `Z3910` |", "| 1 | `Z3910` |");

        assertServiceExceptionCode(() -> importService.importMarkdown(
                        duplicateSequenceMarkdown, CommonStatusEnum.ENABLE.getStatus(), null),
                PRO_ROUTE_IMPORT_SEQUENCE_DUPLICATE.getCode());
        verifyNoInsert();
    }

    @Test
    void importRouteWithNoFinalProcess_failsBeforeAnyInsert() throws Exception {
        String noFinalProcessMarkdown = readFixture()
                .replace(" | FS | 1 |", " | FS | 0 |");

        assertServiceExceptionCode(() -> importService.importMarkdown(
                        noFinalProcessMarkdown, CommonStatusEnum.ENABLE.getStatus(), null),
                PRO_ROUTE_IMPORT_FINAL_PROCESS_INVALID.getCode());
        verifyNoInsert();
    }

    @Test
    void importRouteWithMultipleFinalProcesses_failsBeforeAnyInsert() throws Exception {
        String multipleFinalProcessMarkdown = readFixture()
                .replace("| 29 | `W150` | W导管中盒(说明书) | `node-xlsx-route-yxn-044-02-1020-29-w150-73edc3a2b9` | FS | 0 |",
                        "| 29 | `W150` | W导管中盒(说明书) | `node-xlsx-route-yxn-044-02-1020-29-w150-73edc3a2b9` | FS | 1 |");

        assertServiceExceptionCode(() -> importService.importMarkdown(
                        multipleFinalProcessMarkdown, CommonStatusEnum.ENABLE.getStatus(), null),
                PRO_ROUTE_IMPORT_FINAL_PROCESS_INVALID.getCode());
        verifyNoInsert();
    }

    @Test
    void importInvalidFinalProcessFlag_failsAsMalformedBeforeAnyInsert() throws Exception {
        String invalidFinalFlagMarkdown = readFixture()
                .replace(" | FS | 0 |", " | FS | 2 |");

        assertServiceExceptionCode(() -> importService.importMarkdown(
                        invalidFinalFlagMarkdown, CommonStatusEnum.ENABLE.getStatus(), null),
                PRO_ROUTE_IMPORT_INVALID_MARKDOWN.getCode());
        verifyNoInsert();
    }

    @Test
    void importInvalidProcessStatus_failsBeforeAnyInsert() throws Exception {
        assertServiceExceptionCode(() -> importService.importMarkdown(readFixture(), 9, null),
                PRO_ROUTE_IMPORT_PROCESS_STATUS_INVALID.getCode());
        verifyNoInsert();
    }

    @Test
    void importInvalidCheckProcessJsonNull_failsBeforeAnyInsert() throws Exception {
        assertServiceExceptionCode(() -> importService.importMarkdown(
                        readFixture(), CommonStatusEnum.ENABLE.getStatus(), "null"),
                PRO_ROUTE_IMPORT_CHECK_PROCESS_INVALID.getCode());
        verifyNoInsert();
    }

    @Test
    void importInvalidCheckProcessCode_failsBeforeAnyInsert() throws Exception {
        String checkJson = "{\"ROUTE-YXN.044.02.1020\":[\"NOT-IN-ROUTE\"]}";

        assertServiceExceptionCode(() -> importService.importMarkdown(
                        readFixture(), CommonStatusEnum.ENABLE.getStatus(), checkJson),
                PRO_ROUTE_IMPORT_CHECK_PROCESS_INVALID.getCode());
        verifyNoInsert();
    }

    @Test
    void importValidCheckProcessJson_setsCheckFlagForMappedProcess() throws Exception {
        String markdown = readFixture();
        String checkJson = "{\"ROUTE-YXN.044.02.1020\":[\"W030\"]}";
        AtomicLong routeId = new AtomicLong(8000L);
        AtomicLong processId = new AtomicLong(9000L);
        AtomicLong routeProcessId = new AtomicLong(10000L);
        when(routeMapper.selectByCode(anyString())).thenReturn(null);
        when(processMapper.selectByCode(anyString())).thenReturn(null);
        when(processMapper.selectByName(anyString())).thenReturn(null);
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

        IntGyRouteMarkdownImportResult result = importService.importMarkdown(
                markdown, CommonStatusEnum.ENABLE.getStatus(), checkJson);

        assertEquals(2, result.getRouteCount());
        ArgumentCaptor<MesProProcessDO> processCaptor = ArgumentCaptor.forClass(MesProProcessDO.class);
        verify(processMapper, times(51)).insert(processCaptor.capture());
        Map<Long, MesProProcessDO> processById = processCaptor.getAllValues().stream()
                .collect(toMap(MesProProcessDO::getId, Function.identity()));

        ArgumentCaptor<MesProRouteProcessDO> routeProcessCaptor = ArgumentCaptor.forClass(MesProRouteProcessDO.class);
        verify(routeProcessMapper, times(51)).insert(routeProcessCaptor.capture());
        long checkedCount = routeProcessCaptor.getAllValues().stream()
                .filter(routeProcess -> "W030".equals(processById.get(routeProcess.getProcessId()).getCode()))
                .filter(MesProRouteProcessDO::getCheckFlag)
                .count();
        assertEquals(1L, checkedCount);
    }

    @Test
    void importMalformedMarkdown_failsBeforeAnyInsert() {
        String malformedMarkdown = """
                # Broken
                ## 1. ROUTE-BROKEN
                | 字段 | 值 |
                | --- | --- |
                | routeCode | ROUTE-BROKEN |
                | routeName | Broken route |
                """;

        assertServiceExceptionCode(() -> importService.importMarkdown(
                        malformedMarkdown, CommonStatusEnum.ENABLE.getStatus(), null),
                PRO_ROUTE_IMPORT_INVALID_MARKDOWN.getCode());
        verifyNoInsert();
    }

    private String readFixture() throws Exception {
        return Files.readString(CURRENT_EXPORT_FIXTURE, StandardCharsets.UTF_8);
    }

    private void verifyNoInsert() {
        verify(routeMapper, never()).insert(any(MesProRouteDO.class));
        verify(processMapper, never()).insert(any(MesProProcessDO.class));
        verify(routeProcessMapper, never()).insert(any(MesProRouteProcessDO.class));
        verify(routeProcessFlowEdgeMapper, never()).insert(any(MesProRouteProcessFlowEdgeDO.class));
        verifyNoInteractions(permissionScopeService);
    }

    private void assertServiceExceptionCode(Executable executable, Integer expectedCode) {
        ServiceException serviceException = assertThrows(ServiceException.class, executable);
        assertEquals(expectedCode, serviceException.getCode());
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
