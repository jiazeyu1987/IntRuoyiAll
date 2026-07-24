package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormFillLogPageRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrFormFillLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrFormFillLogControllerTest {

    @Mock
    private MesProEdhrFormFillLogService formFillLogService;

    @InjectMocks
    private MesProEdhrFormFillLogController controller;

    @Test
    void getPage_delegatesReadOnlyQueryAndReturnsBatchContext() {
        MesProEdhrFormFillLogPageReqVO reqVO = new MesProEdhrFormFillLogPageReqVO()
                .setBatchRecordReportId("RPT-PUMP")
                .setBatchCode("BATCH-001")
                .setWorkOrderCode("WO-001")
                .setActorId(101L)
                .setChangedAtStart(LocalDateTime.of(2026, 7, 13, 8, 0))
                .setChangedAtEnd(LocalDateTime.of(2026, 7, 13, 18, 0));
        MesProEdhrFormFillLogPageRespVO row = new MesProEdhrFormFillLogPageRespVO()
                .setAuditBatchId(1001L)
                .setExecutionId(2001L)
                .setExecutionCode("EXEC-001")
                .setBatchRecordReportId("RPT-PUMP")
                .setFormName("压力泵生产记录")
                .setBatchExecutionId(9001L)
                .setBatchCode("BATCH-001")
                .setWorkOrderCode("WO-001")
                .setActorId(101L)
                .setActorName("测试填写人")
                .setChangedAt(LocalDateTime.of(2026, 7, 13, 9, 30))
                .setFieldCount(2)
                .setCellSummary("温度=37.5；压力=1.2")
                .setContextStatus("COMPLETE")
                .setHashStatus("VALID");
        when(formFillLogService.getPage(reqVO)).thenReturn(new PageResult<>(List.of(row), 1L));

        CommonResult<PageResult<MesProEdhrFormFillLogPageRespVO>> response = controller.getPage(reqVO);

        assertEquals(1L, response.getData().getTotal());
        assertEquals("BATCH-001", response.getData().getList().get(0).getBatchCode());
        assertEquals("WO-001", response.getData().getList().get(0).getWorkOrderCode());
        assertEquals(9001L, response.getData().getList().get(0).getBatchExecutionId());
        verify(formFillLogService).getPage(reqVO);
    }

    @Test
    void getDetail_delegatesByAuditBatchIdWithoutReasonColumns() {
        MesProEdhrFormFillLogDetailRespVO detail = new MesProEdhrFormFillLogDetailRespVO()
                .setAuditBatchId(1001L)
                .setExecutionId(2001L)
                .setExecutionCode("EXEC-001")
                .setBatchCode("BATCH-001")
                .setWorkOrderCode("WO-001")
                .setItems(List.of(new MesProEdhrFormFillLogItemRespVO()
                        .setAuditItemId(3001L)
                        .setFieldPath("sheet[0].rows[1].cells[2].temperature")
                        .setFieldKey("temperature")
                        .setFieldLabel("温度")
                        .setRowIndex(1)
                        .setColumnIndex(2)
                        .setOldValueDisplay("36.6")
                        .setNewValueDisplay("37.5")
                        .setChangedAt(LocalDateTime.of(2026, 7, 13, 9, 30))));
        when(formFillLogService.getDetail(1001L)).thenReturn(detail);

        CommonResult<MesProEdhrFormFillLogDetailRespVO> response = controller.getDetail(1001L);

        assertEquals(1001L, response.getData().getAuditBatchId());
        assertEquals("温度", response.getData().getItems().get(0).getFieldLabel());
        assertNull(findGetter(MesProEdhrFormFillLogPageRespVO.class, "getReasonText"));
        assertNull(findGetter(MesProEdhrFormFillLogPageRespVO.class, "getReasonCategory"));
        verify(formFillLogService).getDetail(1001L);
    }

    @Test
    void mappingsAndPermissions_matchFormFillLogReadOnlyContract() throws Exception {
        assertNull(MesProEdhrFormFillLogController.class.getAnnotation(TenantIgnore.class));
        assertMethod("getPage", new Class[]{MesProEdhrFormFillLogPageReqVO.class},
                "/page", "mes:pro-edhr-form-fill-log:query");
        assertMethod("getDetail", new Class[]{Long.class},
                "/detail", "mes:pro-edhr-form-fill-log:query");
    }

    private static void assertMethod(String methodName, Class<?>[] parameterTypes,
                                     String mappingValue, String permission) throws Exception {
        Method method = MesProEdhrFormFillLogController.class.getDeclaredMethod(methodName, parameterTypes);
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        assertArrayEquals(new String[]{mappingValue}, mapping.value());
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals("@ss.hasPermission('" + permission + "')", preAuthorize.value());
    }

    private static Method findGetter(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
