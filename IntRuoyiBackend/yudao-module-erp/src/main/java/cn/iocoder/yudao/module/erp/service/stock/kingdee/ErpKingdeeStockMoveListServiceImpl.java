package cn.iocoder.yudao.module.erp.service.stock.kingdee;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.kingdee.move.ErpKingdeeStockMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.kingdee.move.ErpKingdeeStockMoveRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.kingdee.ErpKingdeeStockMoveListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.kingdee.ErpKingdeeStockMoveListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.kingdee.ErpKingdeeStockMoveListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.kingdee.ErpKingdeeStockMoveListMapper;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.erp.service.stock.sync.ErpKingdeeStockMove;
import cn.iocoder.yudao.module.erp.service.stock.sync.ErpKingdeeStockMoveClient;
import cn.iocoder.yudao.module.erp.service.stock.sync.ErpKingdeeStockMoveSyncResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Service
@Validated
public class ErpKingdeeStockMoveListServiceImpl implements ErpKingdeeStockMoveListService {

    @Resource
    private ErpKingdeeStockMoveClient stockMoveClient;
    @Resource
    private ErpKingdeeConfigService kingdeeConfigService;
    @Resource
    private ErpKingdeeStockMoveListMapper stockMoveListMapper;
    @Resource
    private ErpKingdeeStockMoveListItemMapper stockMoveListItemMapper;

    @Override
    public PageResult<ErpKingdeeStockMoveRespVO> getPage(ErpKingdeeStockMovePageReqVO pageReqVO) {
        PageResult<ErpKingdeeStockMoveListDO> pageResult = stockMoveListMapper.selectPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpKingdeeStockMoveListItemDO> itemList = stockMoveListItemMapper.selectListByStockMoveIds(
                convertSet(pageResult.getList(), ErpKingdeeStockMoveListDO::getId));
        Map<Long, List<ErpKingdeeStockMoveListItemDO>> itemMap = convertMultiMap(itemList,
                ErpKingdeeStockMoveListItemDO::getStockMoveId);
        return BeanUtils.toBean(pageResult, ErpKingdeeStockMoveRespVO.class, stockMove -> {
            List<ErpKingdeeStockMoveRespVO.Item> items = BeanUtils.toBean(itemMap.get(stockMove.getId()),
                    ErpKingdeeStockMoveRespVO.Item.class);
            stockMove.setItems(items);
            stockMove.setMaterialNames(CollUtil.join(items, "，",
                    ErpKingdeeStockMoveRespVO.Item::getMaterialName));
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeStockMoveSyncResult syncAll() {
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        properties.validateBaseConfig();
        return syncMoves(stockMoveClient.fetchStockMoves(properties));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeStockMoveSyncResult syncModifiedBetween(LocalDateTime windowStart, LocalDateTime windowEnd) {
        if (stockMoveListMapper.selectCount() == 0) {
            return syncAll();
        }
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        properties.validateBaseConfig();
        return syncMoves(stockMoveClient.fetchStockMovesModifiedBetween(properties, windowStart, windowEnd));
    }

    private ErpKingdeeStockMoveSyncResult syncMoves(List<ErpKingdeeStockMove> stockMoves) {
        LocalDateTime now = LocalDateTime.now();
        ErpKingdeeStockMoveSyncResult result = new ErpKingdeeStockMoveSyncResult();
        for (ErpKingdeeStockMove stockMove : stockMoves) {
            ErpKingdeeStockMoveListDO existing = stockMoveListMapper.selectBySource(
                    ErpKingdeeStockMove.FORM_ID, stockMove.getFid());
            ErpKingdeeStockMoveListDO record = buildRecord(stockMove, now);
            if (existing == null) {
                stockMoveListMapper.insert(record);
                result.addCreated();
            } else {
                record.setId(existing.getId());
                stockMoveListMapper.updateById(record);
                result.addUpdated();
            }
            upsertItems(record.getId(), stockMove, now);
        }
        return result;
    }

    private ErpKingdeeStockMoveListDO buildRecord(ErpKingdeeStockMove stockMove, LocalDateTime now) {
        return ErpKingdeeStockMoveListDO.builder()
                .sourceFormId(ErpKingdeeStockMove.FORM_ID)
                .sourceFid(stockMove.getFid())
                .sourceBillNo(stockMove.getBillNo())
                .billDate(stockMove.getBillDate())
                .documentStatus(stockMove.getDocumentStatus())
                .transferDirect(stockMove.getTransferDirect())
                .transferBizType(stockMove.getTransferBizType())
                .remark(stockMove.getRemark())
                .sourceModifyTime(stockMove.getSourceModifyTime())
                .lastSyncTime(now)
                .rawPayload(JsonUtils.toJsonString(stockMove))
                .build();
    }

    private void upsertItems(Long stockMoveId, ErpKingdeeStockMove stockMove, LocalDateTime now) {
        for (ErpKingdeeStockMove.Line line : stockMove.getLines()) {
            String sourceLineKey = stockMove.getFid() + "|" + line.getEntryId();
            ErpKingdeeStockMoveListItemDO existing = stockMoveListItemMapper.selectBySourceLineKey(sourceLineKey);
            ErpKingdeeStockMoveListItemDO record = buildItemRecord(stockMoveId, stockMove, line, sourceLineKey, now);
            if (existing == null) {
                stockMoveListItemMapper.insert(record);
                continue;
            }
            record.setId(existing.getId());
            stockMoveListItemMapper.updateById(record);
        }
    }

    private ErpKingdeeStockMoveListItemDO buildItemRecord(Long stockMoveId,
                                                          ErpKingdeeStockMove stockMove,
                                                          ErpKingdeeStockMove.Line line,
                                                          String sourceLineKey,
                                                          LocalDateTime now) {
        return ErpKingdeeStockMoveListItemDO.builder()
                .stockMoveId(stockMoveId)
                .sourceFormId(ErpKingdeeStockMove.FORM_ID)
                .sourceFid(stockMove.getFid())
                .sourceEntryId(line.getEntryId())
                .sourceLineKey(sourceLineKey)
                .sourceBillNo(stockMove.getBillNo())
                .materialNumber(line.getMaterialNumber())
                .materialName(line.getMaterialName())
                .materialSpecification(line.getMaterialSpecification())
                .unitName(line.getUnitName())
                .quantity(line.getQuantity())
                .fromWarehouseNumber(line.getFromWarehouseNumber())
                .fromWarehouseName(line.getFromWarehouseName())
                .toWarehouseNumber(line.getToWarehouseNumber())
                .toWarehouseName(line.getToWarehouseName())
                .fromStockLocation(line.getFromStockLocation())
                .toStockLocation(line.getToStockLocation())
                .lotNumber(line.getLotNumber())
                .sourceModifyTime(stockMove.getSourceModifyTime())
                .lastSyncTime(now)
                .rawPayload(JsonUtils.toJsonString(line))
                .build();
    }

}
