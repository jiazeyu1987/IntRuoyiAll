package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProductionReleaseReportStageInitializerTest {

    private static final List<String> NODE_TYPES = List.of(
            "INCOMING_INSPECTION_REPORT",
            "STERILIZATION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_RECORD");

    @Mock private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Mock private MesProEdhrWorkTaskMapper workTaskMapper;
    @Mock private PermissionApi permissionApi;
    @Mock private AdminUserApi adminUserApi;

    private MesProductionReleaseReportStageInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new MesProductionReleaseReportStageInitializerImpl(
                batchExecutionMapper, batchTaskMapper, workTaskMapper, permissionApi, adminUserApi);
    }

    @Test
    void createsExactlyFourFrozenOwnerFillTasks() {
        when(batchExecutionMapper.selectById(901L)).thenReturn(batch(true));
        when(batchTaskMapper.selectListByBatchExecutionId(901L)).thenReturn(batchTasks());
        when(adminUserApi.getUserMap(any())).thenReturn(Map.of(
                8101L, enabledUser(8101L), 8102L, enabledUser(8102L), 8103L, enabledUser(8103L)));
        AtomicLong ids = new AtomicLong(950L);
        when(workTaskMapper.insert(any(MesProEdhrWorkTaskDO.class))).thenAnswer(invocation -> {
            MesProEdhrWorkTaskDO task = invocation.getArgument(0);
            task.setId(ids.incrementAndGet());
            return 1;
        });

        MesProductionReleaseReportStageInitializationResult result =
                initializer.initializeRequiredReportStage(command());

        assertEquals(NODE_TYPES, result.getReportUploadTasks().stream()
                .map(MesProductionReleaseReportUploadTaskReceipt::getNodeType).toList());
        assertEquals(4, result.getReportUploadTasks().size());
        assertNotNull(result.getReportSnapshotHash());
        ArgumentCaptor<MesProEdhrWorkTaskDO> captor = ArgumentCaptor.forClass(MesProEdhrWorkTaskDO.class);
        verify(workTaskMapper, org.mockito.Mockito.times(4)).insert(captor.capture());
        assertFalse(captor.getAllValues().stream().anyMatch(task -> StrUtil.isBlank(task.getTaskCode())));
    }

    @Test
    void missingFrozenOwnerStopsBeforeWorkTaskWrite() {
        when(batchExecutionMapper.selectById(901L)).thenReturn(batch(false));

        MesReleaseFlowBlockerException failure = assertThrows(
                MesReleaseFlowBlockerException.class,
                () -> initializer.initializeRequiredReportStage(command()));

        assertEquals(MesReleaseFlowBlockerType.REPORT_OWNER_REQUIRED,
                failure.getFailure().getBlockers().get(0).getBlockerType());
        verify(workTaskMapper, never()).insert(any(MesProEdhrWorkTaskDO.class));
    }

    private MesProductionReleaseReportStageInitializationCommand command() {
        return new MesProductionReleaseReportStageInitializationCommand()
                .setApplicationId(701L)
                .setBatchExecutionId(901L)
                .setRouteId(401L)
                .setRouteVersionId(402L)
                .setSourceSnapshotHash("source-hash")
                .setExpectedApplicationVersion(1);
    }

    private MesProEdhrBatchExecutionDO batch(boolean allOwners) {
        List<Map<String, Object>> owners = NODE_TYPES.stream()
                .limit(allOwners ? 4 : 3)
                .map(nodeType -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("attachmentCode", nodeType);
                    item.put("candidateSourceType", "USERS");
                    item.put("candidateSourceIds", nodeType.startsWith("FINISHED")
                            ? List.of(8103L) : List.of(nodeType.startsWith("INCOMING") ? 8101L : 8102L));
                    return item;
                }).toList();
        return new MesProEdhrBatchExecutionDO()
                .setId(901L)
                .setWorkOrderId(301L)
                .setWorkOrderCode("WO-001")
                .setBatchCode("BATCH-001")
                .setRouteId(401L)
                .setRouteVersionId(402L)
                .setRouteSnapshotJson(JSON.toJSONString(Map.of(
                        "configSnapshots", Map.of("batchRecordAttachmentOwners", owners))));
    }

    private List<MesProEdhrBatchExecutionTaskDO> batchTasks() {
        AtomicLong ids = new AtomicLong(910L);
        AtomicLong sort = new AtomicLong();
        return NODE_TYPES.stream().map(nodeType -> new MesProEdhrBatchExecutionTaskDO()
                .setId(ids.incrementAndGet())
                .setBatchExecutionId(901L)
                .setNodeType(nodeType)
                .setProcessName(nodeType)
                .setRouteProcessSort((int) sort.getAndIncrement()))
                .toList();
    }

    private AdminUserRespDTO enabledUser(Long id) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setStatus(0);
        return user;
    }
}
