package cn.iocoder.yudao.module.mes.service.pro.process;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.MesProProcessPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.MesProProcessMachineryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.MesProProcessRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo.MesProProcessSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.scheduleconfig.MesProRouteScheduleConfigRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.repair.MesDvRepairMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteScheduleConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_CODE_EXISTS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProProcessServiceImplTest {

    @Mock
    private MesProProcessMapper processMapper;
    @Mock
    private MesDvMachineryProcessMapper machineryProcessMapper;
    @Mock
    private MesDvMachineryMapper machineryMapper;
    @Mock
    private MesDvRepairMapper repairMapper;
    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Mock
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteScheduleConfigService routeScheduleConfigService;
    @Mock
    private MesProBatchRecordReportMapper batchRecordReportMapper;
    @Mock
    private MesMdWorkstationService workstationService;
    @Mock
    private MesProProcessContentService processContentService;
    @Mock
    private MesProRouteProcessService routeProcessService;
    @InjectMocks
    private MesProProcessServiceImpl processService;

    @BeforeEach
    void setUpProcessIdentity() {
        org.mockito.Mockito.lenient().when(routeProcessService.getProcessIdentityMap(anyCollection()))
                .thenAnswer(invocation -> {
                    Map<Long, Long> result = new java.util.LinkedHashMap<>();
                    java.util.Collection<Long> processIds = invocation.getArgument(0);
                    processIds.stream().filter(java.util.Objects::nonNull).forEach(id -> result.put(id, id));
                    return result;
                });
        org.mockito.Mockito.lenient().when(workstationService.getWorkstationListByProcessIds(anyCollection()))
                .thenReturn(List.of());
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildMachineryRespMap_mapsLegacyProcessBindingToCurrentIdentity() throws Exception {
        when(routeProcessService.getProcessIdentityMap(List.of(901L)))
                .thenReturn(Map.of(900L, 901L, 901L, 901L));
        when(machineryProcessMapper.selectListByProcessIds(anyCollection())).thenReturn(List.of(
                MesDvMachineryProcessDO.builder()
                        .machineryId(11L)
                        .processId(900L)
                        .tenHalfHourDailyCapacity(new BigDecimal("80"))
                        .build()));
        when(machineryMapper.selectBatchIds(Set.of(11L))).thenReturn(List.of(
                MesDvMachineryDO.builder().id(11L).code("M-11").name("设备 11").status(0).build()));
        when(repairMapper.selectListByMachineryIdsAndStatuses(anyCollection(), anyCollection()))
                .thenReturn(List.of());
        Method method = MesProProcessServiceImpl.class.getDeclaredMethod(
                "buildMachineryRespMap", java.util.Collection.class);
        method.setAccessible(true);

        Map<Long, List<MesProProcessMachineryRespVO>> result =
                (Map<Long, List<MesProProcessMachineryRespVO>>) method.invoke(processService, List.of(901L));

        assertEquals(1, result.get(901L).size());
        assertEquals(11L, result.get(901L).get(0).getMachineryId());
    }

    @Test
    void createProcess_shouldAllowDuplicateNameWhenCodeIsUnique() {
        MesProProcessSaveReqVO reqVO = baseReq("Balloon", "PROC-CLEAN-002", "清洗");
        when(processMapper.selectByProductNameAndCode("Balloon", "PROC-CLEAN-002")).thenReturn(null);

        assertDoesNotThrow(() -> processService.createProcess(reqVO));

        ArgumentCaptor<MesProProcessDO> processCaptor = ArgumentCaptor.forClass(MesProProcessDO.class);
        verify(processMapper).insert(processCaptor.capture());
        assertEquals("PROC-CLEAN-002", processCaptor.getValue().getCode());
        assertEquals("清洗", processCaptor.getValue().getName());
        verify(processMapper, never()).selectByProductNameAndName("Balloon", "清洗");
    }

    @Test
    void createProcess_shouldRejectDuplicateCodeEvenWhenNameDiffers() {
        MesProProcessSaveReqVO reqVO = baseReq("Balloon", "PROC-CLEAN-001", "终检");
        when(processMapper.selectByProductNameAndCode("Balloon", "PROC-CLEAN-001"))
                .thenReturn(MesProProcessDO.builder()
                        .id(1L)
                        .productName("Balloon")
                        .code("PROC-CLEAN-001")
                        .name("清洗")
                        .build());

        ServiceException exception = assertThrows(ServiceException.class, () -> processService.createProcess(reqVO));

        assertEquals(PRO_PROCESS_CODE_EXISTS.getCode(), exception.getCode());
        verify(processMapper, never()).insert(org.mockito.ArgumentMatchers.any(MesProProcessDO.class));
    }

    @Test
    void getProcessPageWithCapacity_shouldAttachAllRouteNamesForPageProcesses() {
        MesProProcessPageReqVO reqVO = new MesProProcessPageReqVO();
        MesProProcessDO processWithRoutes = MesProProcessDO.builder()
                .id(10L)
                .code("PROC-PRESS-001")
                .name("压力泵装配")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        MesProProcessDO processWithoutRoutes = MesProProcessDO.builder()
                .id(20L)
                .code("PROC-FREE-001")
                .name("独立工序")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        when(processMapper.selectPage(reqVO)).thenReturn(new PageResult<>(List.of(processWithRoutes, processWithoutRoutes), 2L));
        when(machineryProcessMapper.selectListByProcessIds(anyCollection())).thenReturn(List.of());
        when(routeProcessService.getRouteProcessListByProcessIds(anyCollection())).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(1001L).routeId(101L).processId(10L).sort(1).build(),
                MesProRouteProcessDO.builder().id(1002L).routeId(102L).processId(10L).sort(2).build()
        ));
        when(routeMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                MesProRouteDO.builder().id(101L).code("ROUTE-PUMP").name("压力泵").build(),
                MesProRouteDO.builder().id(102L).code("ROUTE-TEST").name("测试路线").build()
        ));

        PageResult<MesProProcessRespVO> result = processService.getProcessPageWithCapacity(reqVO);

        assertEquals(2L, result.getTotal());
        assertEquals(2, result.getList().get(0).getRouteList().size());
        assertEquals(101L, result.getList().get(0).getRouteList().get(0).getId());
        assertEquals("ROUTE-PUMP", result.getList().get(0).getRouteList().get(0).getCode());
        assertEquals("压力泵", result.getList().get(0).getRouteList().get(0).getName());
        assertEquals("测试路线", result.getList().get(0).getRouteList().get(1).getName());
        assertEquals(List.of(), result.getList().get(1).getRouteList());
    }

    @Test
    void getProcessPageWithCapacity_shouldAttachWorkstationsForDisplayColumn() {
        MesProProcessPageReqVO reqVO = new MesProProcessPageReqVO();
        MesProProcessDO processWithWorkstations = MesProProcessDO.builder()
                .id(10L)
                .code("PROC-WS-001")
                .name("工作站绑定工序")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        MesProProcessDO processWithoutWorkstations = MesProProcessDO.builder()
                .id(20L)
                .code("PROC-WS-002")
                .name("未绑定工作站工序")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        when(processMapper.selectPage(reqVO))
                .thenReturn(new PageResult<>(List.of(processWithWorkstations, processWithoutWorkstations), 2L));
        when(machineryProcessMapper.selectListByProcessIds(anyCollection())).thenReturn(List.of());
        when(routeProcessService.getRouteProcessListByProcessIds(anyCollection())).thenReturn(List.of());
        when(workstationService.getWorkstationListByProcessIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationDO.builder()
                        .id(301L)
                        .processId(10L)
                        .code("WS-A")
                        .name("一号工作站")
                        .build(),
                MesMdWorkstationDO.builder()
                        .id(302L)
                        .processId(10L)
                        .code("WS-B")
                        .name("二号工作站")
                        .build()
        ));

        PageResult<MesProProcessRespVO> result = processService.getProcessPageWithCapacity(reqVO);

        MesProProcessRespVO row = result.getList().get(0);
        assertEquals("WS-A 一号工作站、WS-B 二号工作站", row.getWorkstationNames());
        assertEquals(2, row.getWorkstations().size());
        assertEquals(301L, row.getWorkstations().get(0).getId());
        assertEquals("WS-A", row.getWorkstations().get(0).getCode());
        assertEquals("一号工作站", row.getWorkstations().get(0).getName());
        assertEquals("", result.getList().get(1).getWorkstationNames());
        assertEquals(List.of(), result.getList().get(1).getWorkstations());
        verify(workstationService).getWorkstationListByProcessIds(
                argThat(ids -> ids.containsAll(List.of(10L, 20L))));
    }

    @Test
    void getProcessPageWithCapacity_shouldAttachRouteProcessBoundWorkstationsForDisplayColumn() {
        MesProProcessPageReqVO reqVO = new MesProProcessPageReqVO();
        MesProProcessDO processWithRouteBoundWorkstation = MesProProcessDO.builder()
                .id(10L)
                .code("PROC-ROUTE-WS-001")
                .name("路线工序绑定工作站工序")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        when(processMapper.selectPage(reqVO))
                .thenReturn(new PageResult<>(List.of(processWithRouteBoundWorkstation), 1L));
        when(machineryProcessMapper.selectListByProcessIds(anyCollection())).thenReturn(List.of());
        when(routeProcessService.getRouteProcessListByProcessIds(anyCollection())).thenReturn(List.of(
                MesProRouteProcessDO.builder()
                        .id(9001L)
                        .routeId(101L)
                        .processId(10L)
                        .workstationId(301L)
                        .sort(1)
                        .build()
        ));
        when(routeMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                MesProRouteDO.builder().id(101L).code("ROUTE-WS").name("工作站绑定路线").build()
        ));
        when(workstationService.getWorkstationListByProcessIds(anyCollection())).thenReturn(List.of());
        when(workstationService.getWorkstationList(argThat(ids -> ids.contains(301L)))).thenReturn(List.of(
                MesMdWorkstationDO.builder()
                        .id(301L)
                        .processId(10L)
                        .code("WS-ROUTE")
                        .name("路线绑定工作站")
                        .build()
        ));

        PageResult<MesProProcessRespVO> result = processService.getProcessPageWithCapacity(reqVO);

        MesProProcessRespVO row = result.getList().get(0);
        assertEquals("WS-ROUTE 路线绑定工作站", row.getWorkstationNames());
        assertEquals(1, row.getWorkstations().size());
        assertEquals(301L, row.getWorkstations().get(0).getId());
        assertEquals("WS-ROUTE", row.getWorkstations().get(0).getCode());
        assertEquals("路线绑定工作站", row.getWorkstations().get(0).getName());
        verify(workstationService).getWorkstationList(argThat(ids -> ids.contains(301L)));
    }

    @Test
    void getProcessPageWithCapacity_shouldAcceptRouteProcessBoundWorkstationAliasForSummary() {
        MesProProcessPageReqVO reqVO = new MesProProcessPageReqVO();
        reqVO.setRouteId(101L);
        MesProProcessDO process = MesProProcessDO.builder()
                .id(10L)
                .code("PROC-ROUTE-WS-ALIAS")
                .name("路线绑定工作站别名工序")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        when(processMapper.selectPage(reqVO))
                .thenReturn(new PageResult<>(List.of(process), 1L));
        when(routeProcessService.getProcessIdentityMap(argThat(ids -> ids.size() == 1 && ids.contains(10L))))
                .thenReturn(Map.of(10L, 10L, 110L, 10L));
        when(machineryProcessMapper.selectListByProcessIds(anyCollection())).thenReturn(List.of());
        when(routeProcessService.getRouteProcessListByProcessIds(anyCollection())).thenReturn(List.of(
                MesProRouteProcessDO.builder()
                        .id(9002L)
                        .routeId(101L)
                        .processId(10L)
                        .workstationId(303L)
                        .sort(1)
                        .build()
        ));
        when(routeMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                MesProRouteDO.builder().id(101L).code("ROUTE-WS-ALIAS").name("工作站别名路线").build()
        ));
        when(workstationService.getWorkstationList(argThat(ids -> ids.contains(303L)))).thenReturn(List.of(
                MesMdWorkstationDO.builder()
                        .id(303L)
                        .processId(110L)
                        .code("WS-ALIAS")
                        .name("路线绑定别名工作站")
                        .build()
        ));

        PageResult<MesProProcessRespVO> result =
                assertDoesNotThrow(() -> processService.getProcessPageWithCapacity(reqVO));

        MesProProcessRespVO row = result.getList().get(0);
        assertEquals("WS-ALIAS 路线绑定别名工作站", row.getWorkstationNames());
        assertEquals(1, row.getWorkstations().size());
        assertEquals(303L, row.getWorkstations().get(0).getId());
        assertEquals("WS-ALIAS", row.getWorkstations().get(0).getCode());
    }

    @Test
    void getProcessPageWithCapacity_shouldOnlyShowCurrentRouteProcessWorkstationWhenRouteBindingExists() {
        MesProProcessPageReqVO reqVO = new MesProProcessPageReqVO();
        reqVO.setRouteId(101L);
        MesProProcessDO process = MesProProcessDO.builder()
                .id(10L)
                .code("PROC-ONLY-CURRENT-WS")
                .name("只显示当前工序工作站")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        when(processMapper.selectPage(reqVO))
                .thenReturn(new PageResult<>(List.of(process), 1L));
        when(machineryProcessMapper.selectListByProcessIds(anyCollection())).thenReturn(List.of());
        when(routeProcessService.getRouteProcessListByProcessIds(anyCollection())).thenReturn(List.of(
                MesProRouteProcessDO.builder()
                        .id(9001L)
                        .routeId(101L)
                        .processId(10L)
                        .workstationId(302L)
                        .sort(1)
                        .build()
        ));
        when(routeMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                MesProRouteDO.builder().id(101L).code("ROUTE-CURRENT").name("当前路线").build()
        ));
        when(workstationService.getWorkstationListByProcessIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationDO.builder()
                        .id(301L)
                        .processId(10L)
                        .code("WS-OTHER")
                        .name("同工序其它工作站")
                        .build()
        ));
        when(workstationService.getWorkstationList(argThat(ids -> ids.contains(302L)))).thenReturn(List.of(
                MesMdWorkstationDO.builder()
                        .id(302L)
                        .processId(10L)
                        .code("WS-CURRENT")
                        .name("当前工序工作站")
                        .build()
        ));

        PageResult<MesProProcessRespVO> result = processService.getProcessPageWithCapacity(reqVO);

        MesProProcessRespVO row = result.getList().get(0);
        assertEquals("WS-CURRENT 当前工序工作站", row.getWorkstationNames());
        assertEquals(1, row.getWorkstations().size());
        assertEquals(302L, row.getWorkstations().get(0).getId());
        assertEquals("WS-CURRENT", row.getWorkstations().get(0).getCode());
        assertEquals("当前工序工作站", row.getWorkstations().get(0).getName());
    }

    @Test
    void getProcessPageWithCapacity_shouldNotAttachHistoricalWorkstationsWithoutCurrentBinding() {
        MesProProcessPageReqVO reqVO = new MesProProcessPageReqVO();
        MesProProcessDO currentProcess = MesProProcessDO.builder()
                .id(922936L)
                .code("Z2774")
                .name("棘突远端锥度焊接")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        when(processMapper.selectPage(reqVO)).thenReturn(new PageResult<>(List.of(currentProcess), 1L));
        when(machineryProcessMapper.selectListByProcessIds(anyCollection())).thenReturn(List.of());
        when(routeProcessService.getRouteProcessListByProcessIds(anyCollection())).thenReturn(List.of(
                MesProRouteProcessDO.builder()
                        .id(922502L)
                        .routeId(101L)
                        .processId(922936L)
                        .sort(1)
                        .build()
        ));
        when(routeMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                MesProRouteDO.builder().id(101L).code("ROUTE-SPINE").name("棘突球囊扩张方案").build()
        ));
        when(workstationService.getWorkstationListByProcessIds(anyCollection())).thenAnswer(invocation -> {
            java.util.Collection<Long> processIds = invocation.getArgument(0);
            if (processIds.contains(900385L)) {
                return List.of(MesMdWorkstationDO.builder()
                        .id(900119L)
                        .processId(900385L)
                        .code("WS-PROC-XLSX-00016")
                        .name("棘突远端锥度焊接-工位")
                        .build());
            }
            return List.of();
        });

        PageResult<MesProProcessRespVO> result = processService.getProcessPageWithCapacity(reqVO);

        MesProProcessRespVO row = result.getList().get(0);
        assertEquals("", row.getWorkstationNames());
        assertEquals(0, row.getWorkstations().size());
        verify(processMapper, never()).selectListByCodesIgnoreDeleted(anyCollection());
    }

    @Test
    void getProcessPageWithCapacity_shouldPassRouteIdToMapperForRouteFilter() {
        MesProProcessPageReqVO reqVO = new MesProProcessPageReqVO();
        reqVO.setRouteId(101L);
        when(processMapper.selectPage(reqVO)).thenReturn(new PageResult<>(List.of(), 0L));

        PageResult<MesProProcessRespVO> result = processService.getProcessPageWithCapacity(reqVO);

        assertEquals(0L, result.getTotal());
        verify(processMapper).selectPage(reqVO);
        assertNull(reqVO.getCode());
    }

    @Test
    void getProcessPageWithCapacity_shouldIgnoreEmptyMachineryBindingRows() {
        MesProProcessPageReqVO reqVO = new MesProProcessPageReqVO();
        MesProProcessDO process = MesProProcessDO.builder()
                .id(922841L)
                .code("PROC-EMPTY-MACHINERY")
                .name("空设备绑定工序")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .manualShiftCapacity(new BigDecimal("120.000000"))
                .build();
        when(processMapper.selectPage(reqVO)).thenReturn(new PageResult<>(List.of(process), 1L));
        when(machineryProcessMapper.selectListByProcessIds(anyCollection())).thenReturn(List.of(
                MesDvMachineryProcessDO.builder()
                        .id(778L)
                        .processId(922841L)
                        .machineryId(null)
                        .tenHalfHourDailyCapacity(new BigDecimal("80.000000"))
                        .build()));
        when(routeProcessService.getRouteProcessListByProcessIds(anyCollection())).thenReturn(List.of());

        PageResult<MesProProcessRespVO> result =
                assertDoesNotThrow(() -> processService.getProcessPageWithCapacity(reqVO));

        MesProProcessRespVO row = result.getList().get(0);
        assertNull(row.getMachineryQuantityTotal());
        assertEquals(new BigDecimal("120.000000"), row.getAvailableShiftCapacityTotal());
        assertEquals("WORKER", row.getCapacitySource());
        verify(machineryMapper, never()).selectBatchIds(anyCollection());
    }

    @Test
    void getProcessPageWithCapacity_shouldAttachStructuredBatchRecordFormsOnly() {
        MesProProcessPageReqVO reqVO = new MesProProcessPageReqVO();
        MesProProcessDO process = MesProProcessDO.builder()
                .id(30L)
                .code("PROC-RT000006-001")
                .name("组装Ⅰ工序生产记录")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        when(processMapper.selectPage(reqVO)).thenReturn(new PageResult<>(List.of(process), 1L));
        when(machineryProcessMapper.selectListByProcessIds(anyCollection())).thenReturn(List.of());
        when(routeProcessService.getRouteProcessListByProcessIds(anyCollection())).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(922665L).routeId(922067L).processId(30L).sort(5).build()
        ));
        when(routeMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                MesProRouteDO.builder().id(922067L).code("RT000006").name("球囊扩张压力泵").build()
        ));
        when(routeFlowProcessConfigMapper.selectListByRouteProcessIdsAndUseType(anyCollection(), org.mockito.ArgumentMatchers.eq("BATCH")))
                .thenReturn(List.of(MesProRouteFlowProcessConfigDO.builder()
                        .id(9001L)
                        .routeId(922067L)
                        .routeProcessId(922665L)
                        .useType("BATCH")
                        .enabled(true)
                        .build()));
        when(routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType(anyCollection(), org.mockito.ArgumentMatchers.eq("BATCH")))
                .thenReturn(List.of(MesProRouteFlowProcessBatchRecordDO.builder()
                        .routeFlowProcessConfigId(9001L)
                        .routeId(922067L)
                        .routeProcessId(922665L)
                        .useType("BATCH")
                        .batchRecordReportId("report-assembly-1")
                        .reportSort(1)
                        .build()));
        when(batchRecordReportMapper.selectListByReportIds(anyCollection())).thenReturn(List.of(
                report("report-assembly-1", "组装Ⅰ工序生产记录")
        ));
        PageResult<MesProProcessRespVO> result = processService.getProcessPageWithCapacity(reqVO);

        MesProProcessRespVO row = result.getList().get(0);
        assertEquals("组装Ⅰ工序生产记录", row.getBatchRecordFormNames());
        assertEquals("report-assembly-1", row.getBatchRecordForms().get(0).getReportId());
        assertEquals("组装Ⅰ工序生产记录", row.getBatchRecordForms().get(0).getReportName());
    }

    @Test
    void getProcessPageWithCapacity_whenRouteFiltered_shouldAttachOnlyCurrentRouteBatchRecords() {
        MesProProcessPageReqVO reqVO = new MesProProcessPageReqVO();
        reqVO.setRouteId(922067L);
        MesProProcessDO process = MesProProcessDO.builder()
                .id(30L)
                .code("ER130A41E19498")
                .name("大包装工序生产记录")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        when(processMapper.selectPage(reqVO)).thenReturn(new PageResult<>(List.of(process), 1L));
        when(machineryProcessMapper.selectListByProcessIds(anyCollection())).thenReturn(List.of());
        when(routeProcessService.getRouteProcessListByProcessIds(anyCollection())).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(922051L).routeId(922051L).processId(30L).sort(1).build(),
                MesProRouteProcessDO.builder().id(922784L).routeId(922067L).processId(30L).sort(14).build()
        ));
        when(routeMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                MesProRouteDO.builder().id(922051L).code("RT000001").name("E2E-WORD-ROUTE-20260707174807").build(),
                MesProRouteDO.builder().id(922067L).code("RT000006").name("球囊扩张压力泵").build()
        ));
        when(routeFlowProcessConfigMapper.selectListByRouteProcessIdsAndUseType(anyCollection(), org.mockito.ArgumentMatchers.eq("BATCH")))
                .thenReturn(List.of(
                        MesProRouteFlowProcessConfigDO.builder()
                                .id(9001L)
                                .routeId(922051L)
                                .routeProcessId(922051L)
                                .useType("BATCH")
                                .enabled(true)
                                .build(),
                        MesProRouteFlowProcessConfigDO.builder()
                                .id(9002L)
                                .routeId(922067L)
                                .routeProcessId(922784L)
                                .useType("BATCH")
                                .enabled(true)
                                .build()
                ));
        when(routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType(anyCollection(), org.mockito.ArgumentMatchers.eq("BATCH")))
                .thenReturn(List.of(
                        MesProRouteFlowProcessBatchRecordDO.builder()
                                .routeFlowProcessConfigId(9001L)
                                .routeId(922051L)
                                .routeProcessId(922051L)
                                .useType("BATCH")
                                .batchRecordReportId("report-e2e")
                                .reportSort(1)
                                .build(),
                        MesProRouteFlowProcessBatchRecordDO.builder()
                                .routeFlowProcessConfigId(9002L)
                                .routeId(922067L)
                                .routeProcessId(922784L)
                                .useType("BATCH")
                                .batchRecordReportId("report-pressure-pump")
                                .reportSort(1)
                                .build()
                ));
        when(batchRecordReportMapper.selectListByReportIds(anyCollection())).thenReturn(List.of(
                report("report-e2e", "大包装工序生产记录"),
                report("report-pressure-pump", "大包装工序生产记录")
        ));
        PageResult<MesProProcessRespVO> result = processService.getProcessPageWithCapacity(reqVO);

        MesProProcessRespVO row = result.getList().get(0);
        assertEquals("大包装工序生产记录", row.getBatchRecordFormNames());
        assertEquals(1, row.getBatchRecordForms().size());
        assertEquals("report-pressure-pump", row.getBatchRecordForms().get(0).getReportId());
        verify(routeFlowProcessBatchRecordMapper).selectListByRouteProcessIdsAndUseType(
                argThat(ids -> ids.size() == 1 && ids.contains(922784L)), org.mockito.ArgumentMatchers.eq("BATCH"));
    }

    @Test
    void getProcessWithCapacity_shouldIgnoreUnrelatedDeletedRouteWhenRouteScoped() {
        Long processId = 55L;
        Long routeId = 941000L;
        Long deletedRouteId = 941900L;
        MesProProcessDO process = MesProProcessDO.builder()
                .id(processId)
                .code("PROC-ROUTE-SCOPED")
                .name("路线内工序详情")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .manualShiftCapacity(new BigDecimal("120"))
                .build();
        when(processMapper.selectById(processId)).thenReturn(process);
        when(machineryProcessMapper.selectListByProcessIds(anyCollection())).thenReturn(List.of());
        when(routeProcessService.getRouteProcessListByProcessIds(anyCollection())).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(941901L).routeId(deletedRouteId).processId(processId).sort(1).build(),
                MesProRouteProcessDO.builder().id(941001L).routeId(routeId).processId(processId).sort(2).build()
        ));
        when(routeMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                MesProRouteDO.builder().id(routeId).code("RT-SCOPED").name("当前路线").build()
        ));

        MesProProcessRespVO detail = processService.getProcessWithCapacity(processId, routeId);

        assertEquals(1, detail.getRouteList().size());
        assertEquals(routeId, detail.getRouteList().get(0).getId());
        assertEquals(941001L, detail.getRouteList().get(0).getRouteProcessId());
        verify(routeMapper, never()).selectBatchIds(argThat(ids -> ids != null && ids.contains(deletedRouteId)));
    }

    @Test
    void getProcessWithCapacity_shouldIgnoreDeletedRouteWhenRouteUnscoped() {
        Long processId = 56L;
        Long routeId = 942000L;
        Long deletedRouteId = 942900L;
        MesProProcessDO process = MesProProcessDO.builder()
                .id(processId)
                .code("PROC-UNSCOPED-DETAIL")
                .name("无路线筛选工序详情")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .manualShiftCapacity(new BigDecimal("120"))
                .build();
        when(processMapper.selectById(processId)).thenReturn(process);
        when(machineryProcessMapper.selectListByProcessIds(anyCollection())).thenReturn(List.of());
        when(routeProcessService.getRouteProcessListByProcessIds(anyCollection())).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(942001L).routeId(routeId).processId(processId).sort(1).build(),
                MesProRouteProcessDO.builder().id(942901L).routeId(deletedRouteId).processId(processId).sort(2).build()
        ));
        when(routeMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                MesProRouteDO.builder().id(routeId).code("RT-ACTIVE").name("有效路线").build()
        ));

        MesProProcessRespVO detail = processService.getProcessWithCapacity(processId, null);

        assertEquals(1, detail.getRouteList().size());
        assertEquals(routeId, detail.getRouteList().get(0).getId());
        assertEquals(942001L, detail.getRouteList().get(0).getRouteProcessId());
    }

    @Test
    void getProcessPageWithCapacity_shouldIgnoreDeletedRouteWhenRouteUnscoped() {
        Long processId = 57L;
        Long routeId = 943000L;
        Long deletedRouteId = 943900L;
        MesProProcessPageReqVO reqVO = new MesProProcessPageReqVO();
        MesProProcessDO process = MesProProcessDO.builder()
                .id(processId)
                .code("PROC-UNSCOPED-PAGE")
                .name("无路线筛选分页工序")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .manualShiftCapacity(new BigDecimal("120"))
                .build();
        when(processMapper.selectPage(reqVO)).thenReturn(new PageResult<>(List.of(process), 1L));
        when(machineryProcessMapper.selectListByProcessIds(anyCollection())).thenReturn(List.of());
        when(routeProcessService.getRouteProcessListByProcessIds(anyCollection())).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(943001L).routeId(routeId).processId(processId).sort(1).build(),
                MesProRouteProcessDO.builder().id(943901L).routeId(deletedRouteId).processId(processId).sort(2).build()
        ));
        when(routeMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                MesProRouteDO.builder().id(routeId).code("RT-ACTIVE-PAGE").name("有效分页路线").build()
        ));

        PageResult<MesProProcessRespVO> result = processService.getProcessPageWithCapacity(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().get(0).getRouteList().size());
        assertEquals(routeId, result.getList().get(0).getRouteList().get(0).getId());
        assertEquals(943001L, result.getList().get(0).getRouteList().get(0).getRouteProcessId());
    }

    @Test
    void getProcessWithCapacity_shouldAttachRouteScopedBatchRecordFormsOnly() {
        Long processId = 50L;
        Long routeId = 940000L;
        MesProProcessDO process = MesProProcessDO.builder()
                .id(processId)
                .code("PROC-JINGXI")
                .name("精洗工序")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .manualShiftCapacity(new BigDecimal("740"))
                .build();
        when(processMapper.selectById(processId)).thenReturn(process);
        when(machineryProcessMapper.selectListByProcessIds(anyCollection())).thenReturn(List.of());
        when(routeProcessService.getRouteProcessListByProcessIds(anyCollection())).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(940901L).routeId(940900L).processId(processId).sort(1).build(),
                MesProRouteProcessDO.builder().id(940001L).routeId(routeId).processId(processId).sort(2).build()
        ));
        when(routeMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                MesProRouteDO.builder().id(940900L).code("RT-OTHER").name("其他路线").build(),
                MesProRouteDO.builder().id(routeId).code("RT-JINGXI").name("精洗路线").build()
        ));
        when(routeFlowProcessConfigMapper.selectListByRouteProcessIdsAndUseType(anyCollection(), org.mockito.ArgumentMatchers.eq("BATCH")))
                .thenReturn(List.of(
                        MesProRouteFlowProcessConfigDO.builder()
                                .id(940010L)
                                .routeId(routeId)
                                .routeProcessId(940001L)
                                .useType("BATCH")
                                .enabled(true)
                                .build(),
                        MesProRouteFlowProcessConfigDO.builder()
                                .id(940910L)
                                .routeId(940900L)
                                .routeProcessId(940901L)
                                .useType("BATCH")
                                .enabled(true)
                                .build()
                ));
        when(routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType(anyCollection(), org.mockito.ArgumentMatchers.eq("BATCH")))
                .thenReturn(List.of(
                        MesProRouteFlowProcessBatchRecordDO.builder()
                                .routeId(940900L)
                                .routeProcessId(940901L)
                                .useType("BATCH")
                                .formSlotType("MAIN")
                                .batchRecordReportId("main-other")
                                .reportSort(1)
                                .build(),
                        MesProRouteFlowProcessBatchRecordDO.builder()
                                .routeId(routeId)
                                .routeProcessId(940001L)
                                .useType("BATCH")
                                .formSlotType("MAIN")
                                .batchRecordReportId("main-target")
                                .reportSort(1)
                                .build(),
                        MesProRouteFlowProcessBatchRecordDO.builder()
                                .routeId(routeId)
                                .routeProcessId(940001L)
                                .useType("BATCH")
                                .formSlotType("LOSS_REPORT")
                                .batchRecordReportId("loss-target")
                                .reportSort(2)
                                .build(),
                        MesProRouteFlowProcessBatchRecordDO.builder()
                                .routeId(routeId)
                                .routeProcessId(940001L)
                                .useType("BATCH")
                                .formSlotType("PROCESS_INSPECTION")
                                .batchRecordReportId("inspection-target")
                                .reportSort(3)
                                .build(),
                        MesProRouteFlowProcessBatchRecordDO.builder()
                                .routeId(routeId)
                                .routeProcessId(940001L)
                                .useType("BATCH")
                                .formSlotType("PARAMETER_RECORD")
                                .batchRecordReportId("parameter-target")
                                .reportSort(4)
                                .build()
                ));
        when(batchRecordReportMapper.selectListByReportIds(anyCollection())).thenReturn(List.of(
                report("main-other", "其他路线生产记录"),
                report("main-target", "精洗工序生产记录"),
                report("loss-target", "精洗损耗单"),
                report("inspection-target", "精洗过程检验单"),
                report("parameter-target", "精洗参数记录表")
        ));
        MesProProcessRespVO detail = processService.getProcessWithCapacity(processId, routeId);

        assertEquals("精洗工序生产记录", detail.getBatchRecordFormNames());
        assertEquals("main-target", detail.getBatchRecordForms().get(0).getReportId());
        assertEquals("精洗损耗单", detail.getLossReportFormNames());
        assertEquals("精洗过程检验单", detail.getProcessInspectionFormNames());
        assertEquals("精洗参数记录表", detail.getParameterRecordFormNames());
        assertEquals(new BigDecimal("740"), detail.getAvailableShiftCapacityTotal());
        assertEquals("WORKER", detail.getCapacitySource());
        verify(routeFlowProcessBatchRecordMapper).selectListByRouteProcessIdsAndUseType(
                argThat(ids -> ids.size() == 1 && ids.contains(940001L)), org.mockito.ArgumentMatchers.eq("BATCH"));
    }

    @Test
    void getProcessPageWithCapacity_shouldReturnSingleMainBatchRecordPerProcess() {
        MesProProcessPageReqVO reqVO = new MesProProcessPageReqVO();
        MesProProcessDO process = MesProProcessDO.builder()
                .id(30L)
                .code("ER130A41E19498")
                .name("大包装工序生产记录")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        when(processMapper.selectPage(reqVO)).thenReturn(new PageResult<>(List.of(process), 1L));
        when(machineryProcessMapper.selectListByProcessIds(anyCollection())).thenReturn(List.of());
        when(routeProcessService.getRouteProcessListByProcessIds(anyCollection())).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(922051L).routeId(922051L).processId(30L).sort(1).build(),
                MesProRouteProcessDO.builder().id(922784L).routeId(922067L).processId(30L).sort(14).build()
        ));
        when(routeMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                MesProRouteDO.builder().id(922051L).code("RT000001").name("E2E-WORD-ROUTE-20260707174807").build(),
                MesProRouteDO.builder().id(922067L).code("RT000006").name("球囊扩张压力泵").build()
        ));
        when(routeFlowProcessConfigMapper.selectListByRouteProcessIdsAndUseType(anyCollection(), org.mockito.ArgumentMatchers.eq("BATCH")))
                .thenReturn(List.of(
                        MesProRouteFlowProcessConfigDO.builder()
                                .id(9001L)
                                .routeId(922051L)
                                .routeProcessId(922051L)
                                .useType("BATCH")
                                .enabled(true)
                                .build(),
                        MesProRouteFlowProcessConfigDO.builder()
                                .id(9002L)
                                .routeId(922067L)
                                .routeProcessId(922784L)
                                .useType("BATCH")
                                .enabled(true)
                                .build()
                ));
        when(routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType(anyCollection(), org.mockito.ArgumentMatchers.eq("BATCH")))
                .thenReturn(List.of(
                        MesProRouteFlowProcessBatchRecordDO.builder()
                                .routeId(922051L)
                                .routeProcessId(922051L)
                                .useType("BATCH")
                                .formSlotType("MAIN")
                                .batchRecordReportId("report-e2e")
                                .reportSort(1)
                                .build(),
                        MesProRouteFlowProcessBatchRecordDO.builder()
                                .routeId(922067L)
                                .routeProcessId(922784L)
                                .useType("BATCH")
                                .formSlotType("MAIN")
                                .batchRecordReportId("report-pressure-pump")
                                .reportSort(1)
                                .build(),
                        MesProRouteFlowProcessBatchRecordDO.builder()
                                .routeId(922067L)
                                .routeProcessId(922784L)
                                .useType("BATCH")
                                .formSlotType("LOSS_REPORT")
                                .batchRecordReportId("loss-report")
                                .reportSort(2)
                                .build()
                ));
        when(batchRecordReportMapper.selectListByReportIds(anyCollection())).thenReturn(List.of(
                report("report-e2e", "大包装工序生产记录"),
                report("report-pressure-pump", "大包装工序生产记录"),
                report("loss-report", "损耗单")
        ));

        PageResult<MesProProcessRespVO> result = processService.getProcessPageWithCapacity(reqVO);

        MesProProcessRespVO row = result.getList().get(0);
        assertEquals("大包装工序生产记录", row.getBatchRecordFormNames());
        assertEquals(1, row.getBatchRecordForms().size());
        assertEquals("report-pressure-pump", row.getBatchRecordForms().get(0).getReportId());
    }

    @Test
    void getProcessPageWithCapacity_shouldAttachExtraFormSlotColumns() {
        MesProProcessPageReqVO reqVO = new MesProProcessPageReqVO();
        MesProProcessDO process = MesProProcessDO.builder()
                .id(40L)
                .code("PROC-EXTRA-SLOT")
                .name("附属表单工序")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        when(processMapper.selectPage(reqVO)).thenReturn(new PageResult<>(List.of(process), 1L));
        when(machineryProcessMapper.selectListByProcessIds(anyCollection())).thenReturn(List.of());
        when(routeProcessService.getRouteProcessListByProcessIds(anyCollection())).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(930001L).routeId(930000L).processId(40L).sort(1).build()
        ));
        when(routeMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                MesProRouteDO.builder().id(930000L).code("RT-EXTRA").name("附属表单路线").build()
        ));
        when(routeFlowProcessConfigMapper.selectListByRouteProcessIdsAndUseType(anyCollection(), org.mockito.ArgumentMatchers.eq("BATCH")))
                .thenReturn(List.of(MesProRouteFlowProcessConfigDO.builder()
                        .id(930010L)
                        .routeId(930000L)
                        .routeProcessId(930001L)
                        .useType("BATCH")
                        .enabled(true)
                        .build()));
        when(routeFlowProcessBatchRecordMapper.selectListByRouteProcessIdsAndUseType(anyCollection(), org.mockito.ArgumentMatchers.eq("BATCH")))
                .thenReturn(List.of(
                        MesProRouteFlowProcessBatchRecordDO.builder()
                                .routeId(930000L)
                                .routeProcessId(930001L)
                                .useType("BATCH")
                                .formSlotType("MAIN")
                                .batchRecordReportId("main-report")
                                .reportSort(1)
                                .build(),
                        MesProRouteFlowProcessBatchRecordDO.builder()
                                .routeId(930000L)
                                .routeProcessId(930001L)
                                .useType("BATCH")
                                .formSlotType("LOSS_REPORT")
                                .batchRecordReportId("loss-report")
                                .reportSort(2)
                                .build(),
                        MesProRouteFlowProcessBatchRecordDO.builder()
                                .routeId(930000L)
                                .routeProcessId(930001L)
                                .useType("BATCH")
                                .formSlotType("PROCESS_INSPECTION")
                                .batchRecordReportId("inspection-report")
                                .reportSort(3)
                                .build(),
                        MesProRouteFlowProcessBatchRecordDO.builder()
                                .routeId(930000L)
                                .routeProcessId(930001L)
                                .useType("BATCH")
                                .formSlotType("PARAMETER_RECORD")
                                .batchRecordReportId("parameter-report")
                                .reportSort(4)
                                .build()
                ));
        when(batchRecordReportMapper.selectListByReportIds(anyCollection())).thenReturn(List.of(
                report("main-report", "生产主表"),
                report("loss-report", "损耗单"),
                report("inspection-report", "过程检验单"),
                report("parameter-report", "参数记录表")
        ));

        PageResult<MesProProcessRespVO> result = processService.getProcessPageWithCapacity(reqVO);

        MesProProcessRespVO row = result.getList().get(0);
        assertEquals("生产主表", row.getBatchRecordFormNames());
        assertEquals("main-report", row.getBatchRecordForms().get(0).getReportId());
        assertEquals("损耗单", row.getLossReportFormNames());
        assertEquals("loss-report", row.getLossReportForms().get(0).getReportId());
        assertEquals("过程检验单", row.getProcessInspectionFormNames());
        assertEquals("inspection-report", row.getProcessInspectionForms().get(0).getReportId());
        assertEquals("参数记录表", row.getParameterRecordFormNames());
        assertEquals("parameter-report", row.getParameterRecordForms().get(0).getReportId());
    }

    @Test
    void getProcessPageWithCapacity_shouldAttachRouteScopedScheduleColumns() {
        MesProProcessPageReqVO reqVO = new MesProProcessPageReqVO();
        reqVO.setRouteId(950000L);
        MesProProcessDO process = MesProProcessDO.builder()
                .id(60L)
                .code("PROC-SCHEDULE")
                .name("排产属性工序")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        when(processMapper.selectPage(reqVO)).thenReturn(new PageResult<>(List.of(process), 1L));
        when(machineryProcessMapper.selectListByProcessIds(anyCollection())).thenReturn(List.of());
        when(routeProcessService.getRouteProcessListByProcessIds(anyCollection())).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(950001L).routeId(950000L).processId(60L).sort(1).build(),
                MesProRouteProcessDO.builder().id(951001L).routeId(951000L).processId(60L).sort(2).build()
        ));
        when(routeMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                MesProRouteDO.builder().id(950000L).code("RT-SCHEDULE").name("正式路线").build(),
                MesProRouteDO.builder().id(951000L).code("RT-OTHER").name("其他路线").build()
        ));
        doReturn(List.of()).when(routeFlowProcessConfigMapper).selectListByRouteProcessIdsAndUseType(
                anyCollection(), org.mockito.ArgumentMatchers.eq("BATCH"));
        doReturn(List.of(
                        MesProRouteFlowProcessConfigDO.builder()
                                .routeId(950000L)
                                .routeProcessId(950001L)
                                .useType("SCHEDULE")
                                .enabled(true)
                                .productionQuantityFactor(new BigDecimal("1.250000"))
                                .build(),
                        MesProRouteFlowProcessConfigDO.builder()
                                .routeId(951000L)
                                .routeProcessId(951001L)
                                .useType("SCHEDULE")
                                .enabled(true)
                                .productionQuantityFactor(new BigDecimal("9.000000"))
                                .build()
                )).when(routeFlowProcessConfigMapper).selectListByRouteProcessIdsAndUseType(
                anyCollection(), org.mockito.ArgumentMatchers.eq("SCHEDULE"));
        when(routeVersionMapper.selectListByRouteIds(anyCollection())).thenReturn(List.of(
                MesProRouteVersionDO.builder().id(950010L).routeId(950000L).active(Boolean.TRUE).build(),
                MesProRouteVersionDO.builder().id(951010L).routeId(951000L).active(Boolean.TRUE).build()
        ));
        MesProRouteScheduleConfigRespVO scheduleConfig = new MesProRouteScheduleConfigRespVO();
        scheduleConfig.setRouteProcessId(950001L);
        scheduleConfig.setStandardShiftCapacity(new BigDecimal("315.000000"));
        when(routeScheduleConfigService.getConfigRespListByRouteVersionId(950010L))
                .thenReturn(List.of(scheduleConfig));

        PageResult<MesProProcessRespVO> result = processService.getProcessPageWithCapacity(reqVO);

        MesProProcessRespVO row = result.getList().get(0);
        assertEquals(new BigDecimal("1.250000"), row.getProductionQuantityFactor());
        assertEquals(new BigDecimal("315.000000"), row.getShiftCapacity());
        verify(routeScheduleConfigService, never()).getConfigRespListByRouteVersionId(951010L);
    }

    @Test
    void getProcessPageWithCapacity_shouldMarkCapacityConflictAcrossMultipleRoutes() {
        MesProProcessPageReqVO reqVO = new MesProProcessPageReqVO();
        MesProProcessDO process = MesProProcessDO.builder()
                .id(70L)
                .code("PROC-MULTI-CAPACITY")
                .name("多路线产能工序")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        when(processMapper.selectPage(reqVO)).thenReturn(new PageResult<>(List.of(process), 1L));
        when(machineryProcessMapper.selectListByProcessIds(anyCollection())).thenReturn(List.of());
        when(routeProcessService.getRouteProcessListByProcessIds(anyCollection())).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(970001L).routeId(970000L).processId(70L).sort(1).build(),
                MesProRouteProcessDO.builder().id(971001L).routeId(971000L).processId(70L).sort(2).build()
        ));
        when(routeMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                MesProRouteDO.builder().id(970000L).code("RT-CAP-A").name("产能路线 A").build(),
                MesProRouteDO.builder().id(971000L).code("RT-CAP-B").name("产能路线 B").build()
        ));
        doReturn(List.of()).when(routeFlowProcessConfigMapper).selectListByRouteProcessIdsAndUseType(
                anyCollection(), org.mockito.ArgumentMatchers.eq("BATCH"));
        doReturn(List.of(
                MesProRouteFlowProcessConfigDO.builder()
                        .routeId(970000L)
                        .routeProcessId(970001L)
                        .useType("SCHEDULE")
                        .enabled(true)
                        .productionQuantityFactor(new BigDecimal("1.000000"))
                        .build(),
                MesProRouteFlowProcessConfigDO.builder()
                        .routeId(971000L)
                        .routeProcessId(971001L)
                        .useType("SCHEDULE")
                        .enabled(true)
                        .productionQuantityFactor(new BigDecimal("1.000000"))
                        .build()
        )).when(routeFlowProcessConfigMapper).selectListByRouteProcessIdsAndUseType(
                anyCollection(), org.mockito.ArgumentMatchers.eq("SCHEDULE"));
        when(routeVersionMapper.selectListByRouteIds(anyCollection())).thenReturn(List.of(
                MesProRouteVersionDO.builder().id(970010L).routeId(970000L).active(Boolean.TRUE).build(),
                MesProRouteVersionDO.builder().id(971010L).routeId(971000L).active(Boolean.TRUE).build()
        ));
        MesProRouteScheduleConfigRespVO firstConfig = new MesProRouteScheduleConfigRespVO();
        firstConfig.setRouteProcessId(970001L);
        firstConfig.setStandardShiftCapacity(new BigDecimal("300.000000"));
        MesProRouteScheduleConfigRespVO secondConfig = new MesProRouteScheduleConfigRespVO();
        secondConfig.setRouteProcessId(971001L);
        secondConfig.setStandardShiftCapacity(new BigDecimal("420.000000"));
        when(routeScheduleConfigService.getConfigRespListByRouteVersionId(970010L))
                .thenReturn(List.of(firstConfig));
        when(routeScheduleConfigService.getConfigRespListByRouteVersionId(971010L))
                .thenReturn(List.of(secondConfig));

        PageResult<MesProProcessRespVO> result = processService.getProcessPageWithCapacity(reqVO);

        MesProProcessRespVO row = result.getList().get(0);
        assertEquals(2, row.getRouteList().size());
        assertEquals(970001L, row.getRouteList().get(0).getRouteProcessId());
        assertEquals(new BigDecimal("300.000000"), row.getRouteList().get(0).getShiftCapacity());
        assertEquals(971001L, row.getRouteList().get(1).getRouteProcessId());
        assertEquals(new BigDecimal("420.000000"), row.getRouteList().get(1).getShiftCapacity());
        assertEquals(Boolean.TRUE, row.getRouteCapacityConflict());
        assertTrue(row.getRouteCapacityConflictMessage().contains("覆盖产能"));
    }

    @Test
    void getProcessPageWithCapacity_shouldCompareRouteOverrideAgainstProcessDefaultCapacity() {
        MesProProcessPageReqVO reqVO = new MesProProcessPageReqVO();
        MesProProcessDO process = MesProProcessDO.builder()
                .id(80L)
                .code("PROC-DEFAULT-OVERRIDE")
                .name("默认共用产能工序")
                .manualShiftCapacity(new BigDecimal("300.000000"))
                .status(CommonStatusEnum.ENABLE.getStatus())
                .build();
        when(processMapper.selectPage(reqVO)).thenReturn(new PageResult<>(List.of(process), 1L));
        when(machineryProcessMapper.selectListByProcessIds(anyCollection())).thenReturn(List.of());
        when(routeProcessService.getRouteProcessListByProcessIds(anyCollection())).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(980001L).routeId(980000L).processId(80L).sort(1).build(),
                MesProRouteProcessDO.builder().id(981001L).routeId(981000L).processId(80L).sort(2).build()
        ));
        when(routeMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                MesProRouteDO.builder().id(980000L).code("RT-DEFAULT").name("默认路线").build(),
                MesProRouteDO.builder().id(981000L).code("RT-OVERRIDE").name("覆盖路线").build()
        ));
        doReturn(List.of()).when(routeFlowProcessConfigMapper).selectListByRouteProcessIdsAndUseType(
                anyCollection(), org.mockito.ArgumentMatchers.eq("BATCH"));
        doReturn(List.of(
                MesProRouteFlowProcessConfigDO.builder()
                        .routeId(980000L)
                        .routeProcessId(980001L)
                        .useType("SCHEDULE")
                        .enabled(true)
                        .productionQuantityFactor(new BigDecimal("1.000000"))
                        .build(),
                MesProRouteFlowProcessConfigDO.builder()
                        .routeId(981000L)
                        .routeProcessId(981001L)
                        .useType("SCHEDULE")
                        .enabled(true)
                        .productionQuantityFactor(new BigDecimal("1.000000"))
                        .build()
        )).when(routeFlowProcessConfigMapper).selectListByRouteProcessIdsAndUseType(
                anyCollection(), org.mockito.ArgumentMatchers.eq("SCHEDULE"));
        when(routeVersionMapper.selectListByRouteIds(anyCollection())).thenReturn(List.of(
                MesProRouteVersionDO.builder().id(980010L).routeId(980000L).active(Boolean.TRUE).build(),
                MesProRouteVersionDO.builder().id(981010L).routeId(981000L).active(Boolean.TRUE).build()
        ));
        MesProRouteScheduleConfigRespVO overrideConfig = new MesProRouteScheduleConfigRespVO();
        overrideConfig.setRouteProcessId(981001L);
        overrideConfig.setStandardShiftCapacity(new BigDecimal("420.000000"));
        when(routeScheduleConfigService.getConfigRespListByRouteVersionId(980010L))
                .thenReturn(List.of());
        when(routeScheduleConfigService.getConfigRespListByRouteVersionId(981010L))
                .thenReturn(List.of(overrideConfig));

        PageResult<MesProProcessRespVO> result = processService.getProcessPageWithCapacity(reqVO);

        MesProProcessRespVO row = result.getList().get(0);
        assertEquals(2, row.getRouteList().size());
        assertEquals(new BigDecimal("300.000000"), row.getRouteList().get(0).getShiftCapacity());
        assertEquals(new BigDecimal("420.000000"), row.getRouteList().get(1).getShiftCapacity());
        assertEquals(Boolean.TRUE, row.getRouteCapacityConflict());
        assertTrue(row.getRouteCapacityConflictMessage().contains("覆盖产能"));
    }

    private MesProProcessSaveReqVO baseReq(String productName, String code, String name) {
        MesProProcessSaveReqVO reqVO = new MesProProcessSaveReqVO();
        reqVO.setProductName(productName);
        reqVO.setCode(code);
        reqVO.setName(name);
        reqVO.setAttention("按最新工艺要求执行");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setManualShiftCapacity(new BigDecimal("740"));
        reqVO.setRemark("同名工序测试");
        return reqVO;
    }

    private MesProBatchRecordReportDO report(String reportId, String reportName) {
        MesProBatchRecordReportDO report = new MesProBatchRecordReportDO();
        report.setReportId(reportId);
        report.setReportName(reportName);
        return report;
    }
}
