package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.DccApprovalMatrixExceptionAdvice;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalMatrixBaselineVO;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.ExceptionHandler;
import static org.junit.jupiter.api.Assertions.*;

class DccApprovalMatrixExceptionAdviceTest {
    @Test void controllerLocalMatrixHandlerTakesPrecedenceOverTheHighestOrderGlobalExceptionHandler() throws Exception {
        var method=DccControlledFileController.class.getDeclaredMethod("approvalMatrixChanged",DccApprovalMatrixChangeException.class);
        assertArrayEquals(new Class<?>[]{DccApprovalMatrixChangeException.class},method.getAnnotation(ExceptionHandler.class).value());
    }
    @Test void preservesAuthoritativeComparisonAndNeverReportsSuccess() {
        var before=new DccApprovalMatrixBaselineVO();before.setRouteId(9007199254740993L);before.setRouteVersionNo(1);
        var after=new DccApprovalMatrixBaselineVO();after.setRouteId(9007199254740995L);after.setRouteVersionNo(2);
        var result=new DccApprovalMatrixExceptionAdvice().changed(new DccApprovalMatrixChangeException(
                "APPROVAL_MATRIX_EXPECTATION_STALE",before,after,"original-token","exact-confirmation-token"));
        assertEquals(1_080_000_363,result.getCode());assertSame(before,result.getData().matrixBefore());
        assertSame(after,result.getData().matrixAfter());assertEquals("exact-confirmation-token",result.getData().approvalMatrixConfirmationToken());
        assertTrue(result.getMsg().contains("APPROVAL_MATRIX_EXPECTATION_STALE"));
    }
    @Test void scopesOnlyTheFileControllerAndRejectsAnUnknownMatrixReason() {
        assertThrows(IllegalStateException.class,()->new DccApprovalMatrixExceptionAdvice().changed(
                new DccApprovalMatrixChangeException("UNKNOWN",null,null,null,null)));
    }
}
