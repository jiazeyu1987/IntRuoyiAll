package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesEdhrStatic021InventoryEvidenceChainContractTest {

    @Test
    void activeOrderCompletionMustRecordFormalProductIssueTraceBeforeReceipt() throws Exception {
        String source = source("src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/"
                + "MesTeamLeaderActiveOrderCompletionServiceImpl.java");

        assertTrue(source.contains("MesActiveOrderTransferTraceService activeOrderTransferTraceService"),
                "active-order completion must depend on the formal inventory trace service");
        assertTrue(source.contains(
                        "activeOrderTransferTraceService.recordProductIssueInventoryTracesForActiveOrder(activeOrder);"),
                "normal completion must write inventory trace evidence from formal product issue details");

        int backfillWrite = source.indexOf("backfillPort.write(draft, activeOrder.getId());");
        int traceWrite = source.indexOf(
                "activeOrderTransferTraceService.recordProductIssueInventoryTracesForActiveOrder(activeOrder);");
        int markCompleted = source.indexOf("activeOrderMapper.markCompleted(");
        assertTrue(backfillWrite >= 0 && traceWrite > backfillWrite && traceWrite < markCompleted,
                "trace evidence must be written after formal source backfill succeeds and before active-order completion");
    }

    @Test
    void traceServiceMustProjectFormalProductIssueDetailsIntoRequiredInventoryReadinessTypes() throws Exception {
        String serviceInterface = source("src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/"
                + "MesActiveOrderTransferTraceService.java");
        String serviceImpl = source("src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/"
                + "MesActiveOrderTransferTraceServiceImpl.java");

        assertTrue(serviceInterface.contains("recordProductIssueInventoryTracesForActiveOrder"),
                "trace service API must expose the normal product-issue inventory evidence producer");
        assertTrue(serviceImpl.contains("MesWmProductIssueDO")
                        && serviceImpl.contains("MesWmProductIssueDetailDO"),
                "trace service must read the formal finished product-issue headers and details");
        assertTrue(serviceImpl.contains("SOURCE_TYPE_TRANSFER")
                        && serviceImpl.contains("SOURCE_TYPE_SHIPMENT")
                        && serviceImpl.contains("SOURCE_TYPE_BATCH_TRACE"),
                "formal product issue details must generate transfer, shipment and batch-trace readiness evidence");
        assertTrue(serviceImpl.contains("\"WM_PRODUCT_ISSUE_DETAIL\""),
                "trace rows must point to the formal product issue detail source object");
        assertTrue(serviceImpl.contains(".materialStockId(detail.getMaterialStockId())")
                        && serviceImpl.contains(".batchId(detail.getBatchId())")
                        && serviceImpl.contains(".itemId(detail.getItemId())")
                        && serviceImpl.contains(".sourceObjectCode(issue.getCode())"),
                "each generated trace must retain stock, material, batch and source document identity");
        assertTrue(serviceImpl.contains("\"active-order-\" + activeOrder.getId() + \"-product-issue-\""),
                "product issue trace rows must use a stable active-order scoped idempotency key");
    }

    private static String source(String relative) throws Exception {
        return Files.readString(resolveBackendPath(relative), StandardCharsets.UTF_8);
    }

    private static Path resolveBackendPath(String relative) {
        Path cwd = Paths.get("").toAbsolutePath();
        if ("yudao-module-mes".equals(cwd.getFileName().toString())) {
            return cwd.resolve(relative);
        }
        if ("IntRuoyiBackend".equals(cwd.getFileName().toString())) {
            return cwd.resolve("yudao-module-mes").resolve(relative);
        }
        return cwd.resolve("IntRuoyiBackend").resolve("yudao-module-mes").resolve(relative);
    }
}
