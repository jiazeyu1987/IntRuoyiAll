package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpKingdeePurchaseOrderSyncRecordDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.*;

class ErpKingdeePurchaseOrderSyncRecordMapperTest extends BaseDbUnitTest {

    @Resource
    private ErpKingdeePurchaseOrderSyncRecordMapper syncRecordMapper;

    @Test
    void selectBySourceKey_returnsMatchingRecord() {
        ErpKingdeePurchaseOrderSyncRecordDO record = buildRecord("PUR_PurchaseOrder", "10001", 501L);
        syncRecordMapper.insert(record);

        ErpKingdeePurchaseOrderSyncRecordDO selected =
                syncRecordMapper.selectBySourceKey("PUR_PurchaseOrder", "10001");

        assertNotNull(selected);
        assertEquals(record.getId(), selected.getId());
        assertEquals("PO20260512001", selected.getSourceBillNo());
    }

    @Test
    void insert_rejectsDuplicateSourceKeyInSameTenant() {
        syncRecordMapper.insert(buildRecord("PUR_PurchaseOrder", "10001", 501L));

        assertThrows(DuplicateKeyException.class,
                () -> syncRecordMapper.insert(buildRecord("PUR_PurchaseOrder", "10001", 502L)));
    }

    private static ErpKingdeePurchaseOrderSyncRecordDO buildRecord(String sourceFormId, String sourceFid,
                                                                  Long purchaseOrderId) {
        ErpKingdeePurchaseOrderSyncRecordDO record = new ErpKingdeePurchaseOrderSyncRecordDO();
        record.setSourceFormId(sourceFormId);
        record.setSourceFid(sourceFid);
        record.setSourceBillNo("PO20260512001");
        record.setPurchaseOrderId(purchaseOrderId);
        record.setSyncStatus(ErpKingdeePurchaseOrderSyncRecordDO.SYNC_STATUS_SUCCESS);
        record.setRawPayload("{\"FID\":\"10001\"}");
        return record;
    }

}
