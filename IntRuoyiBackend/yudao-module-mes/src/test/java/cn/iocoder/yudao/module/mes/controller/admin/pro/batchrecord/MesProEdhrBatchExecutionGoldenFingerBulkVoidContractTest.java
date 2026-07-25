package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionGoldenFingerBulkVoidReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionGoldenFingerBulkVoidRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProEdhrBatchExecutionGoldenFingerBulkVoidContractTest {

    @Test
    void controllerContract_exposesGoldenFingerBulkVoidEndpointAndPermission() throws Exception {
        RequestMapping mapping = MesProEdhrBatchExecutionController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-batch-execution"}, mapping.value());

        Method method = MesProEdhrBatchExecutionController.class.getDeclaredMethod("goldenFingerBulkVoid",
                EdhrBatchExecutionGoldenFingerBulkVoidReqVO.class);
        assertArrayEquals(new String[]{"/golden-finger/bulk-void"},
                method.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-batch-record-execution:golden-finger')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void serviceAndVoContract_exposeCurrentFilterBulkVoidShape() throws Exception {
        MesProEdhrBatchExecutionService.class.getDeclaredMethod("goldenFingerBulkVoid",
                EdhrBatchExecutionGoldenFingerBulkVoidReqVO.class);

        requireGetter(EdhrBatchExecutionGoldenFingerBulkVoidReqVO.class, "getFilter");
        requireGetter(EdhrBatchExecutionGoldenFingerBulkVoidReqVO.class, "getReasonCategory");
        requireGetter(EdhrBatchExecutionGoldenFingerBulkVoidReqVO.class, "getReasonText");
        requireGetter(EdhrBatchExecutionGoldenFingerBulkVoidReqVO.class, "getPassword");
        requireGetter(EdhrBatchExecutionGoldenFingerBulkVoidReqVO.class, "getComment");
        EdhrBatchExecutionGoldenFingerBulkVoidReqVO.class.getDeclaredMethod("setFilter",
                EdhrBatchExecutionPageReqVO.class);

        requireGetter(EdhrBatchExecutionGoldenFingerBulkVoidRespVO.class, "getMatchedCount");
        requireGetter(EdhrBatchExecutionGoldenFingerBulkVoidRespVO.class, "getVoidedCount");
        requireGetter(EdhrBatchExecutionGoldenFingerBulkVoidRespVO.class, "getSkippedCount");
        requireGetter(EdhrBatchExecutionGoldenFingerBulkVoidRespVO.class, "getItems");
    }

    private static void requireGetter(Class<?> type, String getterName) throws Exception {
        type.getDeclaredMethod(getterName);
    }
}
