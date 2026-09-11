package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteCandidateConfigService;
import com.alibaba.fastjson.JSONArray;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProBatchRecordRecognitionDeviceSyncServiceTest {

    @Mock
    private MesRouteDccProjectBindingMapper routeDccProjectBindingMapper;
    @Mock
    private MesProRouteProcessMapper routeProcessMapper;
    @Mock
    private MesProProcessMapper processMapper;
    @Mock
    private MesProcessPoolTeamDeviceMapper deviceMapper;
    @Mock
    private MesProRouteVersionMapper routeVersionMapper;
    @Mock
    private MesProRouteCandidateConfigService candidateConfigService;

    @Test
    void syncWritesRecognitionDevicesToCandidateRouteProductionConfig() {
        MesProBatchRecordRecognitionDeviceSyncService service =
                new MesProBatchRecordRecognitionDeviceSyncService(routeDccProjectBindingMapper, routeProcessMapper,
                        processMapper, deviceMapper, routeVersionMapper, candidateConfigService);
        when(routeDccProjectBindingMapper.selectCurrentListByDccProjectCodeId(9001L))
                .thenReturn(List.of(MesRouteDccProjectBindingDO.builder().routeId(1001L).build()));
        when(routeProcessMapper.selectListByRouteId(1001L)).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(2001L).routeId(1001L).processId(3001L).sort(1).build()));
        MesProProcessDO process = new MesProProcessDO();
        process.setId(3001L);
        process.setName("清洗工序");
        when(processMapper.selectById(3001L)).thenReturn(process);
        when(routeVersionMapper.selectOpenCandidateByRouteId(1001L)).thenReturn(MesProRouteVersionDO.builder()
                .id(4001L)
                .routeId(1001L)
                .lifecycleStatus("DRAFT")
                .routeSnapshotSha256("candidate-hash")
                .routeSnapshotJson("{\"configSnapshots\":{\"productionProcessConfigs\":[]}}")
                .build());
        when(deviceMapper.selectList(any())).thenReturn(List.of(MesProcessPoolTeamDeviceDO.builder()
                .id(5001L)
                .deviceCode("B09393")
                .deviceName("超声波清洗机")
                .deviceStatus("ENABLED")
                .enabled(Boolean.TRUE)
                .build()));

        service.sync(9001L, """
                {"schemaVersion":3,"processes":[{"routeProcessId":2001,"equipmentGroups":[{
                  "deviceGroupKey":"CLEAN-WASHER","sort":1,
                  "selectionMode":"MULTIPLE",
                  "equipmentOptions":[{"code":"B09393","name":"超声波清洗机"}],
                  "parameters":[{"parameterCode":"wash_medium","sort":1,"name":"清洗介质","referenceValue":"注射用水",
                    "ui":{"control":"select","defaultValue":"注射用水","options":["注射用水"]}}]
                }]}]}
                """);

        ArgumentCaptor<Map<String, Object>> configCaptor = ArgumentCaptor.forClass(Map.class);
        verify(candidateConfigService).saveConfigSnapshots(
                org.mockito.ArgumentMatchers.eq(4001L),
                org.mockito.ArgumentMatchers.eq("candidate-hash"),
                configCaptor.capture());
        JSONArray productionConfigs = (JSONArray) configCaptor.getValue().get("productionProcessConfigs");
        assertEquals(1, productionConfigs.size());
        assertEquals(2001L, productionConfigs.getJSONObject(0).getLongValue("routeProcessId"));
        assertEquals(1, productionConfigs.getJSONObject(0).getIntValue("sort"));
        assertEquals("CLEAN-WASHER", productionConfigs.getJSONObject(0).getJSONArray("deviceSelectionGroups")
                .getJSONObject(0).getString("deviceGroupKey"));
        assertEquals(5001L, productionConfigs.getJSONObject(0).getJSONArray("deviceSelectionGroups")
                .getJSONObject(0).getJSONArray("deviceIds").getLongValue(0));
        assertEquals("wash_medium", productionConfigs.getJSONObject(0).getJSONArray("parameterRules")
                .getJSONObject(0).getString("parameterCode"));
        assertEquals("SELECT", productionConfigs.getJSONObject(0).getJSONArray("parameterRules")
                .getJSONObject(0).getString("valueType"));
        assertTrue(productionConfigs.getJSONObject(0).getJSONArray("parameterRules")
                .getJSONObject(0).getString("optionValuesJson").contains("注射用水"));
    }

    @Test
    void syncRejectsLegacySchemaVersionTwoBecauseItHasNoFormalRouteProcessIdentity() {
        MesProBatchRecordRecognitionDeviceSyncService service =
                new MesProBatchRecordRecognitionDeviceSyncService(routeDccProjectBindingMapper, routeProcessMapper,
                        processMapper, deviceMapper, routeVersionMapper, candidateConfigService);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.sync(9001L, """
                {"schemaVersion":2,"processes":[{"name":"清洗工序","equipmentGroups":[]}]}
                """));

        assertTrue(ex.getMessage().contains("schemaVersion=3"));
        verify(candidateConfigService, never()).saveConfigSnapshots(any(), any());
    }

    @Test
    void syncRejectsWhenRecognitionDoesNotCoverEveryRouteProcess() {
        MesProBatchRecordRecognitionDeviceSyncService service =
                new MesProBatchRecordRecognitionDeviceSyncService(routeDccProjectBindingMapper, routeProcessMapper,
                        processMapper, deviceMapper, routeVersionMapper, candidateConfigService);
        when(routeDccProjectBindingMapper.selectCurrentListByDccProjectCodeId(9001L))
                .thenReturn(List.of(MesRouteDccProjectBindingDO.builder().routeId(1001L).build()));
        when(routeProcessMapper.selectListByRouteId(1001L)).thenReturn(List.of(
                MesProRouteProcessDO.builder().id(2001L).routeId(1001L).processId(3001L).sort(1).build(),
                MesProRouteProcessDO.builder().id(2002L).routeId(1001L).processId(3002L).sort(2).build()));
        MesProProcessDO process = new MesProProcessDO();
        process.setId(3001L);
        process.setName("清洗工序");
        when(processMapper.selectById(3001L)).thenReturn(process);
        MesProProcessDO process2 = new MesProProcessDO();
        process2.setId(3002L);
        process2.setName("烘干工序");
        when(processMapper.selectById(3002L)).thenReturn(process2);
        when(routeVersionMapper.selectOpenCandidateByRouteId(1001L)).thenReturn(MesProRouteVersionDO.builder()
                .id(4001L)
                .routeId(1001L)
                .lifecycleStatus("DRAFT")
                .routeSnapshotJson("{\"configSnapshots\":{\"productionProcessConfigs\":[]}}")
                .build());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.sync(9001L, """
                {"schemaVersion":3,"processes":[{"routeProcessId":2001,"equipmentGroups":[]}]}
                """));

        assertTrue(ex.getMessage().contains("必须覆盖当前路线全部工序"));
        verify(candidateConfigService, never()).saveConfigSnapshots(any(), any());
    }
}
