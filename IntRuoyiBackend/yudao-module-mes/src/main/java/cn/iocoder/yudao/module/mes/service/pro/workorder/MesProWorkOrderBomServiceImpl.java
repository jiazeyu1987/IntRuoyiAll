package cn.iocoder.yudao.module.mes.service.pro.workorder;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.bom.MesProWorkOrderBomPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.bom.MesProWorkOrderBomSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdProductBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdProductBomService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_ITEM_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_PRODUCT_BOM_ITEM_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_BOM_NOT_EXISTS;

/**
 * MES 生产工单 BOM Service 实现类
 *
 * @author 瑛泰源码
 */
@Service
@Validated
public class MesProWorkOrderBomServiceImpl implements MesProWorkOrderBomService {

    @Resource
    private MesProWorkOrderBomMapper workOrderBomMapper;
    @Resource
    private MesProWorkOrderMapper workOrderMapper;

    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private MesProWorkOrderService workOrderService;
    @Resource
    private MesMdItemService itemService;
    @Resource
    private MesMdProductBomService productBomService;

    @Override
    public Long createWorkOrderBom(MesProWorkOrderBomSaveReqVO createReqVO) {
        // 校验数据
        validateWorkOrderBomSaveData(createReqVO);

        // 插入数据
        MesProWorkOrderBomDO workOrderBom = BeanUtils.toBean(createReqVO, MesProWorkOrderBomDO.class);
        workOrderBomMapper.insert(workOrderBom);
        return workOrderBom.getId();
    }

    @Override
    public void updateWorkOrderBom(MesProWorkOrderBomSaveReqVO updateReqVO) {
        // 校验存在
        validateWorkOrderBomExists(updateReqVO.getId());
        // 校验数据
        validateWorkOrderBomSaveData(updateReqVO);

        // 更新数据
        MesProWorkOrderBomDO updateObj = BeanUtils.toBean(updateReqVO, MesProWorkOrderBomDO.class);
        workOrderBomMapper.updateById(updateObj);
    }

    @Override
    public void deleteWorkOrderBom(Long id) {
        // 校验存在
        validateWorkOrderBomExists(id);

        // 删除
        workOrderBomMapper.deleteById(id);
    }

    private void validateWorkOrderBomExists(Long id) {
        if (workOrderBomMapper.selectById(id) == null) {
            throw exception(PRO_WORK_ORDER_BOM_NOT_EXISTS);
        }
    }

    private void validateWorkOrderBomSaveData(MesProWorkOrderBomSaveReqVO reqVO) {
        // 校验工单存在
        MesProWorkOrderDO workOrder = workOrderService.validateWorkOrderExists(reqVO.getWorkOrderId());
        // DONE @AI：增加注释
        // 校验物料存在
        itemService.validateItemExists(reqVO.getItemId());
        // 校验物料属于产品 BOM
        if (!CollUtil.anyMatch(productBomService.getProductBomListByItemId(workOrder.getProductId()),
                productBom -> productBom.getBomItemId().equals(reqVO.getItemId()))) {
            throw exception(MD_PRODUCT_BOM_ITEM_INVALID);
        }
    }

    @Override
    public MesProWorkOrderBomDO getWorkOrderBom(Long id) {
        return workOrderBomMapper.selectById(id);
    }

    @Override
    public PageResult<MesProWorkOrderBomDO> getWorkOrderBomPage(MesProWorkOrderBomPageReqVO pageReqVO) {
        return workOrderBomMapper.selectPage(pageReqVO);
    }

    @Override
    public List<MesProWorkOrderBomDO> getWorkOrderBomListByWorkOrderId(Long workOrderId) {
        return workOrderBomMapper.selectListByWorkOrderId(workOrderId);
    }

    @Override
    public Map<Long, BigDecimal> getWorkOrderMaterialDemandByWorkOrderId(Long workOrderId) {
        List<MesProWorkOrderBomDO> bomList = getWorkOrderBomListByWorkOrderId(workOrderId);
        if (CollUtil.isEmpty(bomList)) {
            return Collections.emptyMap();
        }
        return buildWorkOrderMaterialDemand(bomList);
    }

    @Override
    public Map<Long, Map<Long, BigDecimal>> getWorkOrderMaterialDemandMapByWorkOrderIds(Collection<Long> workOrderIds) {
        if (CollUtil.isEmpty(workOrderIds)) {
            return Collections.emptyMap();
        }
        Map<Long, Map<Long, BigDecimal>> result = new LinkedHashMap<>();
        for (Long workOrderId : workOrderIds) {
            result.put(workOrderId, Collections.emptyMap());
        }
        List<MesProWorkOrderBomDO> bomList = workOrderBomMapper.selectListByWorkOrderIds(workOrderIds);
        if (CollUtil.isEmpty(bomList)) {
            return result;
        }
        Map<Long, List<MesProWorkOrderBomDO>> bomByWorkOrderId = new LinkedHashMap<>();
        for (MesProWorkOrderBomDO bom : bomList) {
            if (bom.getWorkOrderId() == null) {
                continue;
            }
            bomByWorkOrderId.computeIfAbsent(bom.getWorkOrderId(), ignored -> new ArrayList<>()).add(bom);
        }
        bomByWorkOrderId.forEach((workOrderId, rows) -> result.put(workOrderId, buildWorkOrderMaterialDemand(rows)));
        return result;
    }

    @Override
    public void deleteWorkOrderBomByWorkOrderId(Long workOrderId) {
        workOrderBomMapper.deleteByWorkOrderId(workOrderId);
    }

    @Override
    public void generateWorkOrderBom(Long workOrderId, MesProWorkOrderSaveReqVO reqVO, boolean updated) {
        // 1. 如果是更新场景，先清理旧的 BOM 数据
        if (updated) {
            workOrderBomMapper.deleteByWorkOrderId(workOrderId);
        }

        // 2.1 查询产品 BOM
        List<MesMdProductBomDO> productBomList = productBomService.getProductBomListByItemId(reqVO.getProductId());
        if (CollUtil.isEmpty(productBomList)) {
            return;
        }
        // 2.2 批量获取并校验 BOM 物料
        Set<Long> bomItemIds = convertSet(productBomList, MesMdProductBomDO::getBomItemId);
        Map<Long, MesMdItemDO> itemMap = itemService.getItemMap(bomItemIds);
        bomItemIds.forEach(itemId -> {
            if (itemMap.get(itemId) == null) {
                throw exception(MD_ITEM_NOT_EXISTS);
            }
        });

        // 3. 构建工单 BOM 列表并批量插入
        List<MesProWorkOrderBomDO> bomList = new ArrayList<>(productBomList.size());
        for (MesMdProductBomDO productBom : productBomList) {
            bomList.add(new MesProWorkOrderBomDO().setWorkOrderId(workOrderId).setItemId(productBom.getBomItemId())
                    .setQuantity(reqVO.getQuantity().multiply(productBom.getQuantity())));
        }
        workOrderBomMapper.insertBatch(bomList);
    }

    @Override
    public int regenerateOpenWorkOrderBomByProductIds(Collection<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return 0;
        }
        List<MesProWorkOrderDO> workOrders = workOrderMapper.selectListByProductIdsAndStatuses(productIds, List.of(
                MesProWorkOrderStatusEnum.PREPARE.getStatus(),
                MesProWorkOrderStatusEnum.CONFIRMED.getStatus()));
        if (CollUtil.isEmpty(workOrders)) {
            return 0;
        }
        int recalculatedCount = 0;
        for (MesProWorkOrderDO workOrder : workOrders) {
            MesProWorkOrderSaveReqVO reqVO = new MesProWorkOrderSaveReqVO();
            reqVO.setProductId(workOrder.getProductId());
            reqVO.setQuantity(workOrder.getQuantity());
            generateWorkOrderBom(workOrder.getId(), reqVO, true);
            recalculatedCount++;
        }
        return recalculatedCount;
    }

    private Map<Long, BigDecimal> buildWorkOrderMaterialDemand(List<MesProWorkOrderBomDO> bomList) {
        Map<Long, BigDecimal> currentLayer = new LinkedHashMap<>();
        for (MesProWorkOrderBomDO bom : bomList) {
            if (bom.getItemId() == null || bom.getQuantity() == null) {
                continue;
            }
            currentLayer.merge(bom.getItemId(), bom.getQuantity(), BigDecimal::add);
        }
        if (currentLayer.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, BigDecimal> leafItems = new LinkedHashMap<>();
        for (int i = 0; i < Byte.MAX_VALUE && CollUtil.isNotEmpty(currentLayer); i++) {
            List<MesMdProductBomDO> subBomList = productBomService.getProductBomListByItemIds(currentLayer.keySet());
            Map<Long, List<MesMdProductBomDO>> subBomMap = new LinkedHashMap<>();
            for (MesMdProductBomDO subBom : subBomList) {
                if (subBom.getItemId() == null) {
                    continue;
                }
                subBomMap.computeIfAbsent(subBom.getItemId(), ignored -> new ArrayList<>()).add(subBom);
            }

            Map<Long, BigDecimal> nextLayer = new LinkedHashMap<>();
            for (Map.Entry<Long, BigDecimal> entry : currentLayer.entrySet()) {
                Long itemId = entry.getKey();
                BigDecimal quantity = entry.getValue();
                List<MesMdProductBomDO> children = subBomMap.get(itemId);
                if (CollUtil.isEmpty(children)) {
                    leafItems.merge(itemId, quantity, BigDecimal::add);
                    continue;
                }
                for (MesMdProductBomDO child : children) {
                    if (child.getBomItemId() == null || child.getQuantity() == null) {
                        continue;
                    }
                    nextLayer.merge(child.getBomItemId(), quantity.multiply(child.getQuantity()), BigDecimal::add);
                }
            }
            currentLayer = nextLayer;
        }
        return leafItems;
    }

}
