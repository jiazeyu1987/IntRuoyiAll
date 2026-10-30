package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import cn.iocoder.yudao.module.erp.service.product.sync.ErpKingdeeProductSyncService;
import cn.iocoder.yudao.module.erp.service.production.kingdee.ErpKingdeeProductionPickListService;
import cn.iocoder.yudao.module.erp.service.production.kingdee.ErpKingdeeProductionReplenishmentListService;
import cn.iocoder.yudao.module.erp.service.stock.kingdee.ErpKingdeeInventoryListService;
import cn.iocoder.yudao.module.erp.service.stock.sync.ErpKingdeeStockSyncService;
import cn.iocoder.yudao.module.mes.service.md.item.kingdee.MesKingdeeBomListService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID;

@Service
@RequiredArgsConstructor
public class MesErpCodeSyncService {

    private final ErpKingdeeProductSyncService productService;
    private final ErpKingdeeStockSyncService stockService;
    private final ErpKingdeeInventoryListService inventoryService;
    private final MesKingdeeBomListService bomService;
    private final MesKingdeeProductionMaterialListSyncService materialListService;
    private final ErpKingdeeProductionPickListService pickListService;
    private final ErpKingdeeProductionReplenishmentListService replenishmentListService;
    private final MesKingdeeProductionOrderSyncService productionOrderService;

    // Targeted reads must not advance the incremental watermark or start a full sync.
    @Transactional(rollbackFor = Exception.class)
    public Result sync(String syncType, String rawCode) {
        String code = rawCode.trim();
        if (code.isEmpty() || code.length() > 100 || code.chars().anyMatch(Character::isISOControl)) {
            throw failure("请输入有效的唯一编号");
        }
        int count = switch (syncType) {
            case "PRODUCT" -> {
                var result = productService.syncProductsByNumbers(List.of(code));
                yield result.getCreatedCount() + result.getUpdatedCount() + result.getSkippedCount();
            }
            case "STOCK" -> stockService.syncByCode(code);
            case "INVENTORY_DETAIL" -> inventoryService.syncByCode(code);
            case "BOM" -> bomService.syncByCode(code);
            case "PRODUCTION_MATERIAL_LIST" -> materialListService.syncByCode(code);
            case "PRODUCTION_PICK_LIST" -> pickListService.syncByCode(code);
            case "PRODUCTION_REPLENISHMENT_LIST" -> replenishmentListService.syncByCode(code);
            case "PRODUCTION_ORDER" -> productionOrderService.syncByCode(code);
            default -> throw failure("不支持的ERP同步类型：" + syncType);
        };
        if (count == 0) {
            throw failure("ERP中未找到可同步的数据，编号：" + code);
        }
        return new Result(syncType, code, count);
    }

    private RuntimeException failure(String message) {
        return exception0(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID.getCode(), message);
    }

    public record Result(String syncType, String code, int syncedCount) {}
}
