package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.MesProFeedbackRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackImportRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.unitmeasure.MesMdUnitMeasureService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackImportRecordService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.importer.ThirdPartyFeedbackImportPayload;
import cn.iocoder.yudao.module.mes.service.pro.feedback.importer.ThirdPartyFeedbackImportService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.service.pro.task.MesProTaskService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProFeedbackControllerHistoryDisplayTest {

    @Mock
    private MesProFeedbackService feedbackService;
    @Mock
    private MesMdWorkstationService workstationService;
    @Mock
    private MesProRouteService routeService;
    @Mock
    private MesProProcessService processService;
    @Mock
    private MesProWorkOrderService workOrderService;
    @Mock
    private MesMdItemService itemService;
    @Mock
    private MesMdUnitMeasureService unitMeasureService;
    @Mock
    private MesProRouteProcessService routeProcessService;
    @Mock
    private MesProTaskService taskService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private ThirdPartyFeedbackImportService thirdPartyFeedbackImportService;
    @Mock
    private MesProFeedbackImportRecordService feedbackImportRecordService;

    @InjectMocks
    private MesProFeedbackController controller;

    @Test
    @SuppressWarnings("unchecked")
    void buildFeedbackRespVOList_usesIgnoreDeletedRouteDataForHistoryRows() throws Exception {
        MesProFeedbackDO feedback = new MesProFeedbackDO();
        feedback.setId(1L);
        feedback.setRouteId(11L);
        feedback.setProcessId(22L);

        MesProRouteDO deletedRoute = new MesProRouteDO();
        deletedRoute.setId(11L);
        deletedRoute.setCode("TPFBRT-DELETED");

        MesProRouteProcessDO deletedRouteProcess = new MesProRouteProcessDO();
        deletedRouteProcess.setRouteId(11L);
        deletedRouteProcess.setProcessId(22L);
        deletedRouteProcess.setCheckFlag(Boolean.TRUE);

        when(taskService.getTaskMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(itemService.getItemMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(unitMeasureService.getUnitMeasureMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(workstationService.getWorkstationMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(routeService.getRouteMapIgnoreDeleted(Set.of(11L))).thenReturn(Map.of(11L, deletedRoute));
        when(processService.getProcessMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(routeProcessService.getRouteProcessListByRouteIdsIgnoreDeleted(Set.of(11L)))
                .thenReturn(List.of(deletedRouteProcess));
        when(adminUserApi.getUserMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(feedbackImportRecordService.getImportRecordMapByFeedbacks(List.of(feedback)))
                .thenReturn(Collections.emptyMap());

        Method method = MesProFeedbackController.class.getDeclaredMethod("buildFeedbackRespVOList", List.class);
        method.setAccessible(true);

        List<?> result = (List<?>) method.invoke(controller, List.of(feedback));
        assertEquals(1, result.size());

        Object vo = result.get(0);
        Method getRouteCode = vo.getClass().getMethod("getRouteCode");
        Method getCheckFlag = vo.getClass().getMethod("getCheckFlag");

        assertEquals("TPFBRT-DELETED", getRouteCode.invoke(vo));
        assertTrue((Boolean) getCheckFlag.invoke(vo));
        verify(routeService).getRouteMapIgnoreDeleted(Set.of(11L));
        verify(routeProcessService).getRouteProcessListByRouteIdsIgnoreDeleted(Set.of(11L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildFeedbackRespVOList_mapsExcelPayloadFieldsForFormalFeedbackList() throws Exception {
        MesProFeedbackDO feedback = new MesProFeedbackDO();
        feedback.setId(1001L);

        LocalDateTime feedbackTime = LocalDateTime.of(2026, 7, 8, 9, 30);
        ThirdPartyFeedbackImportPayload payload = new ThirdPartyFeedbackImportPayload();
        payload.setItemCode("YXN.069.001.1005");
        payload.setItemName("冠状动脉棘突球囊扩张导管");
        payload.setProcessCode("Z2976");
        payload.setProcessName("棘突丝切割");
        payload.setDepartment("组装");
        payload.setFeedbackUserCode("A2020070");
        payload.setFeedbackUserName("苗庆红");
        payload.setApproverName("李萍");
        payload.setFeedbackTime(feedbackTime);

        MesProFeedbackImportRecordDO importRecord = new MesProFeedbackImportRecordDO();
        importRecord.setId(2002L);
        importRecord.setSourceFileName("report.xlsx");
        importRecord.setSheetName("报工");
        importRecord.setRowNo(3);
        importRecord.setSourcePayloadJson(JsonUtils.toJsonString(payload));

        when(taskService.getTaskMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(itemService.getItemMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(unitMeasureService.getUnitMeasureMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(workstationService.getWorkstationMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(routeService.getRouteMapIgnoreDeleted(Collections.emptySet())).thenReturn(Collections.emptyMap());
        when(processService.getProcessMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(routeProcessService.getRouteProcessListByRouteIdsIgnoreDeleted(Collections.emptySet()))
                .thenReturn(Collections.emptyList());
        when(adminUserApi.getUserMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(feedbackImportRecordService.getImportRecordMapByFeedbacks(List.of(feedback)))
                .thenReturn(Map.of(1001L, importRecord));

        Method method = MesProFeedbackController.class.getDeclaredMethod("buildFeedbackRespVOList", List.class);
        method.setAccessible(true);

        List<MesProFeedbackRespVO> result = (List<MesProFeedbackRespVO>) method.invoke(controller, List.of(feedback));
        MesProFeedbackRespVO vo = result.get(0);

        assertEquals("YXN.069.001.1005", vo.getExcelProductCode());
        assertEquals("冠状动脉棘突球囊扩张导管", vo.getExcelProductName());
        assertEquals("Z2976", vo.getExcelProcessCode());
        assertEquals("棘突丝切割", vo.getExcelProcessName());
        assertEquals("组装", vo.getExcelDepartment());
        assertEquals("A2020070", vo.getExcelEmployeeNo());
        assertEquals("苗庆红", vo.getExcelEmployeeName());
        assertEquals("李萍", vo.getExcelSectionLeader());
        assertEquals(feedbackTime, vo.getExcelFeedbackTime());
        assertEquals(2002L, vo.getSourceImportRecordId());
        assertEquals("report.xlsx", vo.getSourceImportFileName());
        assertEquals("报工", vo.getSourceImportSheetName());
        assertEquals(3, vo.getSourceImportRowNo());
    }
}
