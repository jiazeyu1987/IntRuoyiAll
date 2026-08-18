package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordRouteCandidateGovernanceTest {

    private static final Path ROUTE_GENERATION_SERVICE = Path.of(
            "src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/"
                    + "MesProBatchRecordRouteGenerationServiceImpl.java");

    @Test
    void uploadedWordExistingRouteCreatesCandidateAndDoesNotMutateActiveRoute() throws Exception {
        String source = Files.readString(ROUTE_GENERATION_SERVICE, StandardCharsets.UTF_8);

        assertFalse(source.contains("refreshExistingRouteForUploadedWord"),
                "Word 重建已有路线不得直接删除 active 工序、流转关系和绑定。");
        assertFalse(source.contains("createNextActiveRouteVersion"),
                "Word 重建已有路线不得直接创建下一 active 路线版本。");
        assertFalse(source.contains("deleteByRouteIdAndUseType"),
                "Word 重建候选版本不得删除 active 批记录路线用途配置。");
        assertFalse(source.contains("routeProcessMapper.deleteByRouteId"),
                "Word 重建候选版本不得删除 active 路线工序。");
        assertTrue(source.contains("createOrUpdateCandidateRouteVersion"),
                "Word 重建已有路线必须创建或更新唯一候选路线版本。");
        assertTrue(source.contains("selectOpenCandidateByRouteId") && source.contains("updateById"),
                "已有 DRAFT 候选时必须复用并更新现有候选，不能继续创建下一版本。");
        assertTrue(source.contains("STATUS_DRAFT"),
                "候选路线版本必须是 DRAFT，不能作为生产 active 使用。");
        assertTrue(source.contains("\"configSnapshots\""),
                "Word 重建候选版本必须生成发布门禁所需的 configSnapshots。");
        assertTrue(source.contains("\"flowGraph\""),
                "Word 重建候选版本必须包含 flowGraph 快照。");
        assertTrue(source.contains("\"products\""),
                "Word 重建候选版本必须包含 products 快照。");
        assertTrue(source.contains("\"scheduleConfigs\""),
                "Word 重建候选版本必须包含 scheduleConfigs 快照。");
        assertTrue(source.contains("\"batchUseConfigs\""),
                "Word 重建候选版本必须包含 batchUseConfigs 快照。");
    }

    @Test
    void uploadedWordRouteVersionsMustRegisterControlledContentRefsBeforePublish() throws Exception {
        String source = Files.readString(ROUTE_GENERATION_SERVICE, StandardCharsets.UTF_8);

        assertTrue(source.contains("MesProRouteControlledContentAdapter"),
                "Word 路线生成必须接入统一受控内容生命周期适配器。");
        assertTrue(source.contains("recordActiveRegistered(routeVersion"),
                "Word 首次创建 ACTIVE V1 后必须立即登记生效引用。");
        assertTrue(source.contains("recordActiveRegisteredIfMissing(sourceVersion"),
                "Word 升版必须确认来源 ACTIVE 引用；历史缺失时在导入事务内补登记。");
        assertTrue(source.contains("recordDraftCandidateRegisteredIfMissing(sourceVersion, candidateVersion"),
                "Word 新建或更新 DRAFT 候选后必须确认候选引用存在。");
    }
}
