package cn.iocoder.yudao.module.mes.service.qc.oqc;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchDossierItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qc.oqc.MesQcOqcDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchDossierItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qc.oqc.MesQcOqcMapper;
import cn.iocoder.yudao.module.mes.enums.MesBizTypeConstants;
import cn.iocoder.yudao.module.mes.enums.qc.MesQcCheckResultEnum;
import cn.iocoder.yudao.module.mes.enums.qc.MesQcStatusEnum;
import cn.iocoder.yudao.module.mes.service.md.client.MesMdClientService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.qc.defectrecord.MesQcDefectRecordService;
import cn.iocoder.yudao.module.mes.service.qc.template.MesQcTemplateItemService;
import cn.iocoder.yudao.module.mes.service.wm.productsales.MesWmProductSalesLineService;
import cn.iocoder.yudao.module.mes.service.wm.productsales.MesWmProductSalesService;
import cn.iocoder.yudao.module.mes.service.qc.indicatorresult.MesQcIndicatorResultService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link MesQcOqcServiceImpl} 的单元测试类
 *
 * @author 瑛泰源码
 */
@Import(MesQcOqcServiceImpl.class)
public class MesQcOqcServiceImplTest extends BaseDbUnitTest {

    @Resource
    private MesQcOqcServiceImpl oqcService;

    @Resource
    private MesQcOqcMapper oqcMapper;
    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProEdhrBatchDossierItemMapper dossierItemMapper;

    @MockitoBean
    private MesWmProductSalesLineService productSalesLineService;
    @MockitoBean
    private MesWmProductSalesService productSalesService;
    @MockitoBean
    private MesQcOqcLineService oqcLineService;
    @MockitoBean
    private MesMdClientService clientService;
    @MockitoBean
    private MesMdItemService itemService;
    @MockitoBean
    private MesQcTemplateItemService templateItemService;
    @MockitoBean
    private MesQcDefectRecordService defectRecordService;
    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private MesQcIndicatorResultService indicatorResultService;

    @Test
    public void testFinishOqc_writeBack_productSales() {
        // 准备数据：插入一条草稿状态的 OQC 单，来源为销售出库单
        Long sourceLineId = randomLongId();
        Integer checkResult = 1;
        MesQcOqcDO oqc = randomPojo(MesQcOqcDO.class, o -> {
            o.setStatus(MesQcStatusEnum.DRAFT.getStatus());
            o.setSourceDocType(MesBizTypeConstants.WM_PRODUCT_SALES);
            o.setSourceLineId(sourceLineId);
            o.setCheckResult(checkResult);
        });
        oqcMapper.insert(oqc);

        // 调用
        oqcService.finishOqc(oqc.getId());

        // 断言：验证状态更新为已完成
        MesQcOqcDO updatedOqc = oqcMapper.selectById(oqc.getId());
        assertEquals(MesQcStatusEnum.FINISHED.getStatus(), updatedOqc.getStatus());
        // 断言：验证回写销售出库单行
        verify(productSalesLineService).updateProductSalesLineWhenOqcFinish(
                eq(sourceLineId), eq(oqc.getId()), eq(checkResult));
    }

    @Test
    public void testFinishOqc_writeBack_noSourceDoc() {
        // 准备数据：sourceDocType 为空，不需要回写
        MesQcOqcDO oqc = randomPojo(MesQcOqcDO.class, o -> {
            o.setStatus(MesQcStatusEnum.DRAFT.getStatus());
            o.setCheckResult(1);
            o.setSourceDocType(null);
            o.setSourceLineId(null);
        });
        oqcMapper.insert(oqc);

        // 调用
        oqcService.finishOqc(oqc.getId());

        // 断言：验证状态更新为已完成
        MesQcOqcDO updatedOqc = oqcMapper.selectById(oqc.getId());
        assertEquals(MesQcStatusEnum.FINISHED.getStatus(), updatedOqc.getStatus());
        // 断言：不应该调用任何回写
        verify(productSalesLineService, never()).updateProductSalesLineWhenOqcFinish(
                anyLong(), anyLong(), anyInt());
    }

    @Test
    public void testFinishOqc_writeBack_sourceDocTypeDirtyData() {
        // 准备数据：sourceDocType 非空但 sourceLineId 为空，应该抛异常
        MesQcOqcDO oqc = randomPojo(MesQcOqcDO.class, o -> {
            o.setStatus(MesQcStatusEnum.DRAFT.getStatus());
            o.setCheckResult(1);
            o.setSourceDocType(MesBizTypeConstants.WM_PRODUCT_SALES);
            o.setSourceLineId(null); // 脏数据
        });
        oqcMapper.insert(oqc);

        // 调用，并断言异常
        assertThrows(IllegalArgumentException.class, () -> oqcService.finishOqc(oqc.getId()));
    }

    @Test
    public void testFinishOqc_writeBack_unknownSourceDocType() {
        // 准备数据：sourceDocType 无法识别
        MesQcOqcDO oqc = randomPojo(MesQcOqcDO.class, o -> {
            o.setStatus(MesQcStatusEnum.DRAFT.getStatus());
            o.setCheckResult(1);
            o.setSourceDocType(99999); // 无法识别的类型
            o.setSourceLineId(randomLongId());
        });
        oqcMapper.insert(oqc);

        // 调用，并断言异常
        assertThrows(IllegalArgumentException.class, () -> oqcService.finishOqc(oqc.getId()));
    }

    @Test
    public void testFinishOqc_syncEdhrFinalInspection_pass() {
        // 准备数据：同批次同物料存在 eDHR 成品检卷宗项
        String batchCode = "BATCH-OQC-PASS-" + System.nanoTime();
        Long itemId = randomLongId();
        MesProEdhrBatchExecutionDO batch = createEdhrBatchExecution(batchCode, itemId);
        createPendingFinalInspectionDossier(batch.getId());
        MesQcOqcDO oqc = createDraftOqc(batchCode, itemId, MesQcCheckResultEnum.PASS.getType());

        // 调用
        oqcService.finishOqc(oqc.getId());

        // 断言：OQC 合格完成后，成品检卷宗项被正式完成并保留来源证据
        MesProEdhrBatchDossierItemDO dossier = dossierItemMapper.selectRequiredFinalInspection(batch.getId());
        assertEquals("COMPLETED", dossier.getItemStatus());
        assertEquals("OQC", dossier.getSourceDocType());
        assertEquals(oqc.getId(), dossier.getSourceDocId());
        assertEquals(oqc.getCode(), dossier.getSourceDocCode());
        assertEquals("FINISHED", dossier.getSourceDocStatus());
        assertEquals("PASS", dossier.getSourceDocResult());
        assertNotNull(dossier.getSourceDocHash());
        assertEquals(64, dossier.getSourceDocHash().length());
        assertNotNull(dossier.getCompletedAt());
        assertNotNull(dossier.getVerifiedAt());
        assertNull(dossier.getBlockerCode());
        assertNull(dossier.getBlockerMessage());
    }

    @Test
    public void testFinishOqc_syncEdhrFinalInspection_failBlocksDossier() {
        // 准备数据：同批次同物料存在 eDHR 成品检卷宗项
        String batchCode = "BATCH-OQC-FAIL-" + System.nanoTime();
        Long itemId = randomLongId();
        MesProEdhrBatchExecutionDO batch = createEdhrBatchExecution(batchCode, itemId);
        createPendingFinalInspectionDossier(batch.getId());
        MesQcOqcDO oqc = createDraftOqc(batchCode, itemId, MesQcCheckResultEnum.FAIL.getType());

        // 调用
        oqcService.finishOqc(oqc.getId());

        // 断言：OQC 不合格不会放行成品检卷宗，而是记录阻塞原因
        MesProEdhrBatchDossierItemDO dossier = dossierItemMapper.selectRequiredFinalInspection(batch.getId());
        assertEquals("BLOCKED", dossier.getItemStatus());
        assertEquals("OQC", dossier.getSourceDocType());
        assertEquals(oqc.getId(), dossier.getSourceDocId());
        assertEquals(oqc.getCode(), dossier.getSourceDocCode());
        assertEquals("FINISHED", dossier.getSourceDocStatus());
        assertEquals("FAIL", dossier.getSourceDocResult());
        assertNotNull(dossier.getSourceDocHash());
        assertEquals(64, dossier.getSourceDocHash().length());
        assertNull(dossier.getCompletedAt());
        assertNotNull(dossier.getVerifiedAt());
        assertEquals("OQC_RESULT_FAIL", dossier.getBlockerCode());
        assertEquals("OQC检验结果不合格", dossier.getBlockerMessage());
    }

    @Test
    public void testFinishOqc_notExists() {
        // 准备参数
        Long oqcId = randomLongId();

        // 调用，并断言异常
        assertServiceException(() -> oqcService.finishOqc(oqcId), QC_OQC_NOT_EXISTS);
    }

    @Test
    public void testFinishOqc_statusNotDraft() {
        // 准备参数：状态为已完成
        MesQcOqcDO oqc = randomPojo(MesQcOqcDO.class, o -> {
            o.setStatus(MesQcStatusEnum.FINISHED.getStatus());
        });
        oqcMapper.insert(oqc);

        // 调用，并断言异常
        assertServiceException(() -> oqcService.finishOqc(oqc.getId()), QC_OQC_NOT_PREPARE);
    }

    @Test
    public void testFinishOqc_checkResultEmpty() {
        // 准备数据：checkResult 为空，应该报错
        MesQcOqcDO oqc = randomPojo(MesQcOqcDO.class, o -> {
            o.setStatus(MesQcStatusEnum.DRAFT.getStatus());
            o.setCheckResult(null); // 检测结论为空
            o.setSourceDocType(null);
        });
        oqcMapper.insert(oqc);

        // 调用，应该抛异常（检测结论必填）
        assertServiceException(() -> oqcService.finishOqc(oqc.getId()), QC_OQC_CHECK_RESULT_EMPTY);
    }

    @Test
    public void testDeleteOqc_success() {
        // 准备数据：草稿状态
        MesQcOqcDO oqc = randomPojo(MesQcOqcDO.class, o -> {
            o.setStatus(MesQcStatusEnum.DRAFT.getStatus());
        });
        oqcMapper.insert(oqc);

        // 调用
        oqcService.deleteOqc(oqc.getId());

        // 断言：已删除
        assertNull(oqcMapper.selectById(oqc.getId()));
    }

    @Test
    public void testDeleteOqc_notExists() {
        assertServiceException(() -> oqcService.deleteOqc(randomLongId()), QC_OQC_NOT_EXISTS);
    }

    @Test
    public void testDeleteOqc_statusNotDraft() {
        MesQcOqcDO oqc = randomPojo(MesQcOqcDO.class, o -> {
            o.setStatus(MesQcStatusEnum.FINISHED.getStatus());
        });
        oqcMapper.insert(oqc);

        assertServiceException(() -> oqcService.deleteOqc(oqc.getId()), QC_OQC_NOT_PREPARE);
    }

    private MesProEdhrBatchExecutionDO createEdhrBatchExecution(String batchCode, Long productId) {
        MesProEdhrBatchExecutionDO batch = randomPojo(MesProEdhrBatchExecutionDO.class, o -> {
            o.setBatchExecutionCode("EDHR-OQC-" + System.nanoTime());
            o.setWorkOrderId(randomLongId());
            o.setBatchCode(batchCode);
            o.setProductId(productId);
            o.setRouteId(randomLongId());
            o.setStatus(20);
            o.setTaskTotal(1);
            o.setTaskApprovedCount(1);
            o.setBlockedCount(0);
        });
        batch.setDeleted(false);
        batchExecutionMapper.insert(batch);
        return batch;
    }

    private void createPendingFinalInspectionDossier(Long batchExecutionId) {
        MesProEdhrBatchDossierItemDO dossier = new MesProEdhrBatchDossierItemDO()
                .setBatchExecutionId(batchExecutionId)
                .setItemType("FINAL_INSPECTION")
                .setItemKey("FINAL_INSPECTION")
                .setItemName("成品检")
                .setRequiredFlag(Boolean.TRUE)
                .setItemStatus("PENDING");
        dossier.setDeleted(false);
        dossierItemMapper.insert(dossier);
    }

    private MesQcOqcDO createDraftOqc(String batchCode, Long itemId, Integer checkResult) {
        MesQcOqcDO oqc = randomPojo(MesQcOqcDO.class, o -> {
            o.setCode("OQC-EDHR-" + System.nanoTime());
            o.setName("OQC成品检");
            o.setTemplateId(randomLongId());
            o.setClientId(randomLongId());
            o.setBatchCode(batchCode);
            o.setItemId(itemId);
            o.setOutQuantity(BigDecimal.ONE);
            o.setCheckQuantity(BigDecimal.ONE);
            o.setCheckResult(checkResult);
            o.setStatus(MesQcStatusEnum.DRAFT.getStatus());
            o.setSourceDocType(null);
            o.setSourceDocId(null);
            o.setSourceLineId(null);
            o.setSourceDocCode(null);
        });
        oqc.setDeleted(false);
        oqcMapper.insert(oqc);
        return oqc;
    }

}
