package cn.iocoder.yudao.module.mes.service.pro.workorder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/** 锁必须覆盖校验与关联写入，自动提交会使清理校验后的并发保护失效。 */
class MesProWorkOrderErpCleanupWriteGuardTest {
    static Stream<String> writers() {
        return Stream.of(
            "pro.andon.MesProAndonRecordServiceImpl#createAndonRecord",
            "pro.card.MesProCardServiceImpl#createCard", "pro.card.MesProCardServiceImpl#updateCard",
            "pro.feedback.MesProFeedbackServiceImpl#createFeedback",
            "pro.feedback.MesProFeedbackServiceImpl#createFeedbackWithScheduleSnapshot",
            "pro.feedback.MesProFeedbackServiceImpl#createFrontlineFeedback",
            "pro.feedback.MesProFeedbackServiceImpl#updateFeedback",
            "qc.ipqc.MesQcIpqcServiceImpl#createIpqc", "qc.ipqc.MesQcIpqcServiceImpl#updateIpqc",
            "wm.packages.MesWmPackageLineServiceImpl#createPackageLine", "wm.packages.MesWmPackageLineServiceImpl#updatePackageLine",
            "wm.productreceipt.MesWmProductReceiptServiceImpl#createProductReceipt", "wm.productreceipt.MesWmProductReceiptServiceImpl#updateProductReceipt",
            "wm.productissue.MesWmProductIssueServiceImpl#createProductIssue", "wm.productissue.MesWmProductIssueServiceImpl#updateProductIssue",
            "wm.returnissue.MesWmReturnIssueServiceImpl#createReturnIssue", "wm.returnissue.MesWmReturnIssueServiceImpl#updateReturnIssue",
            "wm.outsourceissue.MesWmOutsourceIssueServiceImpl#createOutsourceIssue", "wm.outsourceissue.MesWmOutsourceIssueServiceImpl#updateOutsourceIssue",
            "wm.outsourcereceipt.MesWmOutsourceReceiptServiceImpl#createOutsourceReceipt", "wm.outsourcereceipt.MesWmOutsourceReceiptServiceImpl#updateOutsourceReceipt",
            "wm.sn.MesWmSnServiceImpl#generateSnCodes",
            "pro.workorder.MesProWorkOrderBomServiceImpl#createWorkOrderBom", "pro.workorder.MesProWorkOrderBomServiceImpl#updateWorkOrderBom",
            "pro.workorder.MesProWorkOrderServiceImpl#createWorkOrder", "pro.workorder.MesProWorkOrderServiceImpl#updateWorkOrder",
            "pro.workorder.MesProWorkOrderServiceImpl#confirmWorkOrder", "pro.workorder.MesProWorkOrderServiceImpl#finishWorkOrder",
            "pro.workorder.sync.MesKingdeeWorkOrderBomSyncServiceImpl#syncErpBom",
            "pro.workorder.sync.MesKingdeeProductionMaterialListSyncServiceImpl#syncAllSkipExisting",
            "pro.workorder.sync.MesKingdeeProductionMaterialListSyncServiceImpl#syncModifiedBetween",
            "pro.workorder.sync.MesKingdeeProductionMaterialListSyncServiceImpl#syncByProductionOrderNos");
    }

    @ParameterizedTest @MethodSource("writers")
    void relatedWriteKeepsWorkOrderLockUntilCommit(String writer) throws Exception {
        String[] parts = writer.split("#");
        Class<?> type = Class.forName("cn.iocoder.yudao.module.mes.service." + parts[0]);
        var method = Arrays.stream(type.getDeclaredMethods()).filter(item -> item.getName().equals(parts[1]))
                .findFirst().orElseThrow();
        var transaction = method.getAnnotation(Transactional.class);
        assertNotNull(transaction, writer);
        assertFalse(transaction.readOnly(), writer);
        assertTrue(Arrays.asList(transaction.rollbackFor()).contains(Exception.class), writer);
    }
}
