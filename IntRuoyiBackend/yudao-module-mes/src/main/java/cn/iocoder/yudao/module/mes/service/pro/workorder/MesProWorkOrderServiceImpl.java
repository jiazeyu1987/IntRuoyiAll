package cn.iocoder.yudao.module.mes.service.pro.workorder;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.QuickFilter;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderTemporaryFreezeStatusRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemBatchConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleCalendarRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProductMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleCalendarRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleIssueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskDependencyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskScheduleExtMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProTaskStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.wm.BarcodeBizTypeEnum;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemBatchConfigService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.pro.task.MesProTaskService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationOrderChangeService;
import cn.iocoder.yudao.module.mes.service.wm.barcode.MesWmBarcodeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

/**
 * MES 生产工单 Service 实现类
 *
 * @author 瑛泰源码
 */
@Service
@Validated
public class MesProWorkOrderServiceImpl implements MesProWorkOrderService {

    @Resource
    private MesProWorkOrderMapper workOrderMapper;

    @Resource
    private MesProWorkOrderBomService workOrderBomService;
    @Resource
    private MesMdItemService itemService;
    @Resource
    private MesMdItemMapper itemMapper;
    @Resource
    private MesMdItemBatchConfigService itemBatchConfigService;
    @Resource
    private MesWmBarcodeService barcodeService;
    @Resource
    private MesProTaskService taskService;
    @Resource
    private MesProRouteMapper routeMapper;
    @Resource
    private MesProRouteProductMapper routeProductMapper;
    @Resource
    private MesProTaskMapper taskMapper;
    @Resource
    private MesProTaskScheduleExtMapper taskScheduleExtMapper;
    @Resource
    private MesProTaskDependencyMapper taskDependencyMapper;
    @Resource
    private MesProScheduleIssueMapper scheduleIssueMapper;
    @Resource
    private MesProScheduleCalendarRuleMapper scheduleCalendarRuleMapper;
    @Resource
    private MesReportAllocationOrderChangeService reportAllocationOrderChangeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorkOrder(MesProWorkOrderSaveReqVO createReqVO) {
        // 1. 校验数据
        validateWorkOrderSaveData(null, createReqVO);

        // 2.1 设置默认值
        if (createReqVO.getParentId() == null) {
            createReqVO.setParentId(MesProWorkOrderDO.PARENT_ID_NULL);
        }
        // 2.2 插入工单
        MesProWorkOrderDO workOrder = BeanUtils.toBean(createReqVO, MesProWorkOrderDO.class);
        workOrder.setStatus(MesProWorkOrderStatusEnum.PREPARE.getStatus());
        workOrder.setTemporaryFrozen(Boolean.FALSE);
        workOrderMapper.insert(workOrder);

        // 3. 自动生成 BOM：根据产品 BOM 生成工单 BOM
        workOrderBomService.generateWorkOrderBom(workOrder.getId(), createReqVO, false);

        // 4. 自动生成条码
        barcodeService.autoGenerateBarcode(BarcodeBizTypeEnum.WORKORDER.getValue(),
                workOrder.getId(), workOrder.getCode(), workOrder.getName());
        return workOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkOrder(MesProWorkOrderSaveReqVO updateReqVO) {
        // 1.1 校验存在 + 只有草稿状态才能编辑
        MesProWorkOrderDO oldWorkOrder = validateWorkOrderExists(updateReqVO.getId());
        if (ObjUtil.notEqual(oldWorkOrder.getStatus(), MesProWorkOrderStatusEnum.PREPARE.getStatus())) {
            throw exception(PRO_WORK_ORDER_NOT_PREPARE);
        }
        // 1.2 校验数据
        validateWorkOrderSaveData(updateReqVO.getId(), updateReqVO);

        // 2. 判断产品或数量是否变更，如果变更则重新生成 BOM（updated=true 会先清理旧数据）
        boolean productChanged = ObjUtil.notEqual(oldWorkOrder.getProductId(), updateReqVO.getProductId());
        boolean quantityChanged = oldWorkOrder.getQuantity().compareTo(updateReqVO.getQuantity()) != 0;
        if (oldWorkOrder.getQuantity().compareTo(updateReqVO.getQuantity()) > 0) {
            reportAllocationOrderChangeService.reduceWorkOrderAllocations(updateReqVO.getId(),
                    updateReqVO.getQuantity(), SecurityFrameworkUtils.getLoginUserId(), "工单数量减少");
        }
        if (productChanged || quantityChanged) {
            workOrderBomService.generateWorkOrderBom(updateReqVO.getId(), updateReqVO, true);
        }

        // 3. 更新
        MesProWorkOrderDO updateObj = BeanUtils.toBean(updateReqVO, MesProWorkOrderDO.class);
        workOrderMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkOrder(Long id) {
        // 1.1 校验存在
        MesProWorkOrderDO workOrder = validateWorkOrderExists(id);
        // 1.2 只能删除草稿状态的工单
        if (ObjUtil.notEqual(workOrder.getStatus(), MesProWorkOrderStatusEnum.PREPARE.getStatus())) {
            throw exception(PRO_WORK_ORDER_NOT_PREPARE);
        }
        // 1.3 校验是否有子工单
        Long childCount = workOrderMapper.selectCount(MesProWorkOrderDO::getParentId, id);
        if (childCount > 0) {
            throw exception(PRO_WORK_ORDER_HAS_CHILDREN);
        }

        // 2. 删除工单 + BOM
        workOrderMapper.deleteById(id);
        workOrderBomService.deleteWorkOrderBomByWorkOrderId(id);
    }

    @Override
    public MesProWorkOrderDO validateWorkOrderExists(Long id) {
        MesProWorkOrderDO workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw exception(PRO_WORK_ORDER_NOT_EXISTS);
        }
        return workOrder;
    }

    @Override
    public MesProWorkOrderDO getWorkOrder(Long id) {
        return workOrderMapper.selectById(id);
    }

    @Override
    public MesProWorkOrderDO getWorkOrder(String code) {
        return workOrderMapper.selectByCode(code);
    }

    @Override
    public PageResult<MesProWorkOrderDO> getWorkOrderPage(MesProWorkOrderPageReqVO pageReqVO) {
        List<Long> productIds = resolveProductSearchIds(pageReqVO);
        QuickFilter originalQuickFilter = pageReqVO.getQuickFilter();
        if (isProductQuickFilter(originalQuickFilter)) {
            pageReqVO.setQuickFilter(null);
        }
        try {
            return workOrderMapper.selectPageByProductIds(pageReqVO, productIds);
        } finally {
            pageReqVO.setQuickFilter(originalQuickFilter);
        }
    }

    @Override
    public List<String> getWorkOrderProductNameOptions(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        List<MesMdItemDO> matchedProducts = itemMapper.selectListByNameLike(normalizedKeyword);
        if (CollUtil.isEmpty(matchedProducts)) {
            return Collections.emptyList();
        }
        Map<Long, MesMdItemDO> productMap = matchedProducts.stream()
                .collect(Collectors.toMap(MesMdItemDO::getId, item -> item, (left, right) -> left));
        Set<Long> usedProductIds = workOrderMapper.selectListByProductIds(productMap.keySet()).stream()
                .map(MesProWorkOrderDO::getProductId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return usedProductIds.stream()
                .map(productMap::get)
                .filter(ObjUtil::isNotNull)
                .map(MesMdItemDO::getName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private List<Long> resolveProductSearchIds(MesProWorkOrderPageReqVO pageReqVO) {
        List<Long> selectedProductIds = resolveSelectedProductIds(pageReqVO);
        List<Long> keywordProductIds = resolveKeywordProductIds(pageReqVO);
        if (selectedProductIds.isEmpty()) {
            return keywordProductIds;
        }
        if (!keywordProductIds.isEmpty()) {
            selectedProductIds.retainAll(keywordProductIds);
            return selectedProductIds.isEmpty() ? List.of(-1L) : selectedProductIds;
        }
        if (selectedProductIds.isEmpty()) {
            return Collections.emptyList();
        }
        Long firstProductId = selectedProductIds.get(0);
        boolean allSame = selectedProductIds.stream().allMatch(firstProductId::equals);
        if (!allSame) {
            return List.of(-1L);
        }
        if (pageReqVO.getProductId() == null) {
            return List.of(firstProductId);
        }
        MesMdItemDO selectedProduct = itemMapper.selectById(firstProductId);
        if (selectedProduct == null || selectedProduct.getName() == null || selectedProduct.getName().isBlank()) {
            return List.of(firstProductId);
        }
        List<Long> productIds = itemMapper.selectListByNameLike(selectedProduct.getName()).stream()
                .map(MesMdItemDO::getId)
                .collect(Collectors.toCollection(ArrayList::new));
        if (!productIds.contains(firstProductId)) {
            productIds.add(firstProductId);
        }
        return productIds;
    }

    private List<Long> resolveSelectedProductIds(MesProWorkOrderPageReqVO pageReqVO) {
        List<Long> selectedProductIds = new ArrayList<>();
        addSelectedProductId(selectedProductIds, pageReqVO.getProductId());
        addSelectedProductId(selectedProductIds, pageReqVO.getProductNameFilterId());
        addSelectedProductId(selectedProductIds, pageReqVO.getProductCodeFilterId());
        return selectedProductIds;
    }

    private List<Long> resolveKeywordProductIds(MesProWorkOrderPageReqVO pageReqVO) {
        List<Long> productNameIds = resolveNameKeywordProductIds(pageReqVO.getProductNameKeyword());
        List<Long> productCodeIds = resolveCodeKeywordProductIds(pageReqVO.getProductCodeKeyword());
        List<Long> quickFilterProductIds = resolveQuickFilterProductIds(pageReqVO.getQuickFilter());
        List<Long> productIds = intersectProductIds(productNameIds, productCodeIds);
        return intersectProductIds(productIds, quickFilterProductIds);
    }

    private List<Long> intersectProductIds(List<Long> left, List<Long> right) {
        if (left.isEmpty()) {
            return right;
        }
        if (right.isEmpty()) {
            return left;
        }
        left.retainAll(right);
        return left.isEmpty() ? List.of(-1L) : left;
    }

    private List<Long> resolveQuickFilterProductIds(QuickFilter quickFilter) {
        if (quickFilter == null || quickFilter.getValue() == null || quickFilter.getValue().trim().isEmpty()) {
            return Collections.emptyList();
        }
        String value = quickFilter.getValue().trim();
        return switch (StrUtil.blankToDefault(quickFilter.getFieldKey(), "")) {
            case "productName" -> toProductIds(itemMapper.selectListByNameLike(value));
            case "productCode" -> toProductIds(itemMapper.selectListByCodeLike(value));
            case "productSpecification" -> toProductIds(itemMapper.selectListBySpecificationLike(value));
            default -> Collections.emptyList();
        };
    }

    private boolean isProductQuickFilter(QuickFilter quickFilter) {
        if (quickFilter == null) {
            return false;
        }
        return switch (StrUtil.blankToDefault(quickFilter.getFieldKey(), "")) {
            case "productName", "productCode", "productSpecification" -> true;
            default -> false;
        };
    }

    private List<Long> resolveNameKeywordProductIds(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return toProductIds(itemMapper.selectListByNameLike(keyword.trim()));
    }

    private List<Long> resolveCodeKeywordProductIds(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return toProductIds(itemMapper.selectListByCodeLike(keyword.trim()));
    }

    private List<Long> toProductIds(List<MesMdItemDO> matchedProducts) {
        if (CollUtil.isEmpty(matchedProducts)) {
            return List.of(-1L);
        }
        return matchedProducts.stream()
                .map(MesMdItemDO::getId)
                .toList();
    }

    private void addSelectedProductId(List<Long> selectedProductIds, Long productId) {
        if (productId != null) {
            selectedProductIds.add(productId);
        }
    }

    @Override
    public List<MesProWorkOrderDO> getWorkOrderList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return workOrderMapper.selectByIds(ids);
    }

    @Override
    public MesProWorkOrderDO validateWorkOrderConfirmed(Long id) {
        MesProWorkOrderDO workOrder = validateWorkOrderExists(id);
        if (ObjUtil.notEqual(workOrder.getStatus(), MesProWorkOrderStatusEnum.CONFIRMED.getStatus())) {
            throw exception(PRO_WORK_ORDER_NOT_CONFIRMED);
        }
        return workOrder;
    }

    @Override
    public void confirmWorkOrder(Long id) {
        // 1.1 校验存在
        MesProWorkOrderDO workOrder = validateWorkOrderExists(id);
        // 1.2 只有草稿状态才能确认
        if (ObjUtil.notEqual(workOrder.getStatus(), MesProWorkOrderStatusEnum.PREPARE.getStatus())) {
            throw exception(PRO_WORK_ORDER_NOT_PREPARE);
        }

        // 2. 更新状态为已确认
        workOrderMapper.updateById(new MesProWorkOrderDO().setId(id)
                .setStatus(MesProWorkOrderStatusEnum.CONFIRMED.getStatus()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishWorkOrder(Long id) {
        // 1. 校验存在 + 只有已确认状态才能完成
        MesProWorkOrderDO workOrder = validateWorkOrderExists(id);
        if (ObjUtil.notEqual(workOrder.getStatus(), MesProWorkOrderStatusEnum.CONFIRMED.getStatus())) {
            throw exception(PRO_WORK_ORDER_NOT_CONFIRMED);
        }

        // 2. 级联完成所有关联任务
        taskService.finishTaskByOrderId(id);

        // 3. 更新工单状态为已完成
        workOrderMapper.updateById(new MesProWorkOrderDO().setId(id)
                .setStatus(MesProWorkOrderStatusEnum.FINISHED.getStatus())
                .setFinishDate(LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishWorkOrderForRelease(Long id, Long releaseDecisionId, Long actorUserId) {
        if (id == null || releaseDecisionId == null || actorUserId == null) {
            throw exception(PRO_WORK_ORDER_NOT_EXISTS, id);
        }
        MesProWorkOrderDO workOrder = workOrderMapper.selectByIdForUpdate(id);
        if (workOrder == null) {
            throw exception(PRO_WORK_ORDER_NOT_EXISTS, id);
        }
        if (MesProWorkOrderStatusEnum.FINISHED.getStatus().equals(workOrder.getStatus())) {
            if (workOrder.getReleaseDecisionId() != null
                    && !releaseDecisionId.equals(workOrder.getReleaseDecisionId())) {
                throw exception(PRO_WORK_ORDER_NOT_CONFIRMED);
            }
            workOrderMapper.updateById(new MesProWorkOrderDO().setId(id)
                    .setReleaseDecisionId(releaseDecisionId)
                    .setReleasedBy(actorUserId)
                    .setReleasedAt(workOrder.getReleasedAt() == null ? LocalDateTime.now() : workOrder.getReleasedAt()));
            return;
        }
        if (!MesProWorkOrderStatusEnum.CONFIRMED.getStatus().equals(workOrder.getStatus())) {
            throw exception(PRO_WORK_ORDER_NOT_CONFIRMED);
        }
        taskService.finishTaskByOrderId(id);
        LocalDateTime releasedAt = LocalDateTime.now();
        workOrderMapper.updateById(new MesProWorkOrderDO().setId(id)
                .setStatus(MesProWorkOrderStatusEnum.FINISHED.getStatus())
                .setFinishDate(releasedAt)
                .setReleaseDecisionId(releaseDecisionId)
                .setReleasedBy(actorUserId)
                .setReleasedAt(releasedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelWorkOrder(Long id) {
        // 1. 校验存在 + 只有已确认状态才能取消
        MesProWorkOrderDO workOrder = validateWorkOrderExists(id);
        if (ObjUtil.notEqual(workOrder.getStatus(), MesProWorkOrderStatusEnum.CONFIRMED.getStatus())) {
            throw exception(PRO_WORK_ORDER_NOT_CONFIRMED);
        }

        reportAllocationOrderChangeService.invalidateWorkOrder(id, SecurityFrameworkUtils.getLoginUserId(),
                "工单取消");
        // 2. 级联取消所有关联任务
        taskService.cancelTaskByOrderId(id);

        // 3. 更新工单状态为已取消
        workOrderMapper.updateById(new MesProWorkOrderDO().setId(id)
                .setStatus(MesProWorkOrderStatusEnum.CANCELED.getStatus())
                .setCancelDate(LocalDateTime.now()));
    }

    // ==================== 校验方法 ====================

    private void validateWorkOrderSaveData(Long id, MesProWorkOrderSaveReqVO reqVO) {
        // 1. 校验编码唯一
        validateWorkOrderCodeUnique(id, reqVO.getCode());
        // 2. 校验产品存在
        itemService.validateItemExists(reqVO.getProductId());
        // 3. 校验批次配置（如果产品有 clientFlag=true，则 clientId 必填）
        MesMdItemBatchConfigDO batchConfig = itemBatchConfigService.getItemBatchConfigByItemId(reqVO.getProductId());
        if (batchConfig != null && Boolean.TRUE.equals(batchConfig.getClientFlag()) && reqVO.getClientId() == null) {
            throw exception(MD_CLIENT_NOT_EXISTS);
        }
    }

    private void validateWorkOrderCodeUnique(Long id, String code) {
        if (code == null) {
            return;
        }
        MesProWorkOrderDO workOrder = workOrderMapper.selectByCode(code);
        if (workOrder == null) {
            return;
        }
        if (ObjUtil.notEqual(workOrder.getId(), id)) {
            throw exception(PRO_WORK_ORDER_CODE_DUPLICATE);
        }
    }

    @Override
    public void updateProducedQuantity(Long id, BigDecimal incrQuantityProduced) {
        // 校验工单存在
        validateWorkOrderExists(id);
        // 更新数量
        workOrderMapper.updateProducedQuantity(id, incrQuantityProduced);
    }

    @Override
    public void updateBatchCodeIfBlank(Long id, String batchCode) {
        String trimmedBatchCode = StrUtil.trimToNull(batchCode);
        if (trimmedBatchCode == null) {
            throw exception(WM_PRODUCT_PRODUCE_BATCH_REQUIRED);
        }
        validateWorkOrderExists(id);
        workOrderMapper.updateBatchCodeIfBlank(id, trimmedBatchCode);
    }

    @Override
    public Long getWorkOrderCountByVendorId(Long vendorId) {
        return workOrderMapper.selectCountByVendorId(vendorId);
    }

    @Override
    public MesProWorkOrderTemporaryFreezeStatusRespVO getTemporaryFreezeStatus() {
        MesProScheduleCalendarRuleDO rule = getOrCreateScheduleCalendarRule();
        List<MesProWorkOrderDO> workOrders = workOrderMapper.selectList();
        return buildTemporaryFreezeStatus(rule.getTemporaryFreezeEnabled(), workOrders, 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProWorkOrderTemporaryFreezeStatusRespVO updateTemporaryFreeze(Boolean enabled) {
        MesProScheduleCalendarRuleDO rule = getOrCreateScheduleCalendarRule();
        int clearedTaskCount = 0;
        if (Boolean.TRUE.equals(enabled)) {
            clearedTaskCount = enableTemporaryFreeze();
        } else {
            workOrderMapper.updateTemporaryFrozenAll(Boolean.FALSE);
        }
        rule.setTemporaryFreezeEnabled(enabled);
        scheduleCalendarRuleMapper.updateById(rule);
        List<MesProWorkOrderDO> refreshedWorkOrders = workOrderMapper.selectList();
        return buildTemporaryFreezeStatus(enabled, refreshedWorkOrders, clearedTaskCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkOrderTemporaryFrozen(Long id, Boolean temporaryFrozen) {
        MesProWorkOrderDO workOrder = validateWorkOrderExists(id);
        if (ObjUtil.equal(workOrder.getTemporaryFrozen(), temporaryFrozen)) {
            return;
        }
        if (Boolean.TRUE.equals(temporaryFrozen)) {
            reportAllocationOrderChangeService.invalidateWorkOrder(id, SecurityFrameworkUtils.getLoginUserId(),
                    "工单冻结暂停");
        }
        workOrderMapper.updateTemporaryFrozenByIds(List.of(id), temporaryFrozen);
        if (Boolean.TRUE.equals(temporaryFrozen)) {
            clearOpenScheduleForWorkOrders(List.of(id));
        }
    }

    private int enableTemporaryFreeze() {
        List<MesProWorkOrderDO> workOrders = workOrderMapper.selectList();
        Set<Long> whitelistProductIds = getEnabledRouteProductIds();
        List<Long> freezeIds = new ArrayList<>();
        List<Long> unfreezeIds = new ArrayList<>();
        for (MesProWorkOrderDO workOrder : workOrders) {
            boolean shouldFreeze = !whitelistProductIds.contains(workOrder.getProductId());
            if (shouldFreeze) {
                freezeIds.add(workOrder.getId());
            } else if (Boolean.TRUE.equals(workOrder.getTemporaryFrozen())) {
                unfreezeIds.add(workOrder.getId());
            }
        }
        if (CollUtil.isNotEmpty(unfreezeIds)) {
            workOrderMapper.updateTemporaryFrozenByIds(unfreezeIds, Boolean.FALSE);
        }
        if (CollUtil.isNotEmpty(freezeIds)) {
            Long actorUserId = SecurityFrameworkUtils.getLoginUserId();
            for (Long workOrderId : freezeIds) {
                reportAllocationOrderChangeService.invalidateWorkOrder(workOrderId, actorUserId, "工单冻结暂停");
            }
            workOrderMapper.updateTemporaryFrozenByIds(freezeIds, Boolean.TRUE);
            return clearOpenScheduleForWorkOrders(freezeIds);
        }
        return 0;
    }

    private Set<Long> getEnabledRouteProductIds() {
        List<MesProRouteDO> enabledRoutes = routeMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus());
        if (CollUtil.isEmpty(enabledRoutes)) {
            return Collections.emptySet();
        }
        List<MesProRouteProductDO> routeProducts = routeProductMapper.selectListByRouteIds(
                enabledRoutes.stream().map(MesProRouteDO::getId).toList());
        return routeProducts.stream()
                .map(MesProRouteProductDO::getItemId)
                .filter(ObjUtil::isNotNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private int clearOpenScheduleForWorkOrders(Collection<Long> workOrderIds) {
        List<MesProTaskDO> tasks = taskMapper.selectListByWorkOrderIds(workOrderIds);
        List<Long> openTaskIds = tasks.stream()
                .filter(task -> !MesProTaskStatusEnum.isEndStatus(task.getStatus()))
                .map(MesProTaskDO::getId)
                .toList();
        if (CollUtil.isNotEmpty(openTaskIds)) {
            taskDependencyMapper.deleteByTaskIds(openTaskIds);
            taskScheduleExtMapper.deleteByTaskIds(openTaskIds);
            scheduleIssueMapper.deleteByTaskIds(openTaskIds);
            for (Long taskId : openTaskIds) {
                taskMapper.deleteById(taskId);
            }
        }
        scheduleIssueMapper.deleteByWorkOrderIds(workOrderIds);
        for (Long workOrderId : workOrderIds) {
            workOrderMapper.updateQuantityScheduled(workOrderId, BigDecimal.ZERO);
        }
        return openTaskIds.size();
    }

    private MesProWorkOrderTemporaryFreezeStatusRespVO buildTemporaryFreezeStatus(Boolean enabled,
                                                                                  List<MesProWorkOrderDO> workOrders,
                                                                                  int clearedTaskCount) {
        int frozenCount = (int) workOrders.stream().filter(workOrder -> Boolean.TRUE.equals(workOrder.getTemporaryFrozen())).count();
        MesProWorkOrderTemporaryFreezeStatusRespVO response = new MesProWorkOrderTemporaryFreezeStatusRespVO();
        response.setEnabled(Boolean.TRUE.equals(enabled));
        response.setTotalWorkOrderCount(workOrders.size());
        response.setFrozenWorkOrderCount(frozenCount);
        response.setUnfrozenWorkOrderCount(workOrders.size() - frozenCount);
        response.setClearedTaskCount(clearedTaskCount);
        return response;
    }

    private MesProScheduleCalendarRuleDO getOrCreateScheduleCalendarRule() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        MesProScheduleCalendarRuleDO rule = scheduleCalendarRuleMapper.selectByTenantId(tenantId);
        if (rule != null) {
            return rule;
        }
        MesProScheduleCalendarRuleDO create = MesProScheduleCalendarRuleDO.builder()
                .skipStatutoryHolidays(Boolean.FALSE)
                .weekendRestMode("SINGLE")
                .dateShiftModeByDateJson("{}")
                .temporaryFreezeEnabled(Boolean.FALSE)
                .remark("INIT")
                .build();
        create.setTenantId(tenantId);
        scheduleCalendarRuleMapper.insert(create);
        return create;
    }

}
