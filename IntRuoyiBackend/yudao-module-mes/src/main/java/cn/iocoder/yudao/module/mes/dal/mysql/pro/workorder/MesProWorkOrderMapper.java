package cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.QuickFilterUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * MES 生产工单 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface MesProWorkOrderMapper extends BaseMapperX<MesProWorkOrderDO> {

    java.util.Map<String, ListFilterQueryUtils.Field> LIST_FILTER_FIELDS = java.util.Map.ofEntries(
            java.util.Map.entry("id", ListFilterQueryUtils.number("id")),
            java.util.Map.entry("code", ListFilterQueryUtils.text("code")),
            java.util.Map.entry("name", ListFilterQueryUtils.text("name")),
            java.util.Map.entry("type", ListFilterQueryUtils.number("type")),
            java.util.Map.entry("orderSourceType", ListFilterQueryUtils.number("order_source_type")),
            java.util.Map.entry("orderSourceCode", ListFilterQueryUtils.text("order_source_code")),
            java.util.Map.entry("demandBillNo", ListFilterQueryUtils.text("demand_bill_no")),
            java.util.Map.entry("productId", ListFilterQueryUtils.number("product_id")),
            java.util.Map.entry("materialSpecification", ListFilterQueryUtils.text("material_specification")),
            java.util.Map.entry("quantity", ListFilterQueryUtils.text("COALESCE(CAST(ROUND(quantity,0) AS CHAR),'')")),
            java.util.Map.entry("quantityProduced", ListFilterQueryUtils.number("quantity_produced")),
            java.util.Map.entry("quantityChanged", ListFilterQueryUtils.number("quantity_changed")),
            java.util.Map.entry("quantityScheduled", ListFilterQueryUtils.number("quantity_scheduled")),
            java.util.Map.entry("clientId", ListFilterQueryUtils.number("client_id")),
            java.util.Map.entry("vendorId", ListFilterQueryUtils.number("vendor_id")),
            java.util.Map.entry("batchCode", ListFilterQueryUtils.text("batch_code")),
            java.util.Map.entry("workshopName", ListFilterQueryUtils.text("workshop_name")),
            java.util.Map.entry("bomVersion", ListFilterQueryUtils.text("bom_version")),
            java.util.Map.entry("pickMode", ListFilterQueryUtils.text("pick_mode")),
            java.util.Map.entry("auxiliaryCode", ListFilterQueryUtils.text("auxiliary_code")),
            java.util.Map.entry("businessStatus", ListFilterQueryUtils.text("business_status")),
            java.util.Map.entry("drawingNumber", ListFilterQueryUtils.text("drawing_number")),
            java.util.Map.entry("refNo", ListFilterQueryUtils.text("ref_no")),
            java.util.Map.entry("scheduleStatus", ListFilterQueryUtils.text("schedule_status")),
            java.util.Map.entry("plannedStartTime", ListFilterQueryUtils.dateTime("planned_start_time")),
            java.util.Map.entry("plannedEndTime", ListFilterQueryUtils.dateTime("planned_end_time")),
            java.util.Map.entry("requestDate", ListFilterQueryUtils.dateTime("request_date")),
            java.util.Map.entry("parentId", ListFilterQueryUtils.number("parent_id")),
            java.util.Map.entry("finishDate", ListFilterQueryUtils.dateTime("finish_date")),
            java.util.Map.entry("releaseDecisionId", ListFilterQueryUtils.number("release_decision_id")),
            java.util.Map.entry("releasedBy", ListFilterQueryUtils.number("released_by")),
            java.util.Map.entry("releasedAt", ListFilterQueryUtils.dateTime("released_at")),
            java.util.Map.entry("cancelDate", ListFilterQueryUtils.dateTime("cancel_date")),
            java.util.Map.entry("status", ListFilterQueryUtils.number("status")),
            java.util.Map.entry("temporaryFrozen", ListFilterQueryUtils.bool("temporary_frozen")),
            java.util.Map.entry("remark", ListFilterQueryUtils.text("remark")),
            java.util.Map.entry("createTime", ListFilterQueryUtils.dateTime("create_time")),
            java.util.Map.entry("creator", ListFilterQueryUtils.text("creator")),
            java.util.Map.entry("productCode", ListFilterQueryUtils.text("(SELECT x.code FROM mes_md_item x WHERE x.id=mes_pro_work_order.product_id AND x.tenant_id=mes_pro_work_order.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("productName", ListFilterQueryUtils.text("(SELECT x.name FROM mes_md_item x WHERE x.id=mes_pro_work_order.product_id AND x.tenant_id=mes_pro_work_order.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("productSpecification", ListFilterQueryUtils.text("material_specification")),
            java.util.Map.entry("clientName", ListFilterQueryUtils.text("(SELECT x.name FROM mes_md_client x WHERE x.id=mes_pro_work_order.client_id AND x.tenant_id=mes_pro_work_order.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("productionMaterialList", ListFilterQueryUtils.text("COALESCE(JSON_UNQUOTE(CONCAT(CHAR(34),REPLACE(SUBSTRING(CAST((SELECT JSON_ARRAYAGG(x.source_bill_no) OVER(ORDER BY x.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM mes_kingdee_production_material_list x WHERE x.work_order_id=mes_pro_work_order.id AND x.tenant_id=mes_pro_work_order.tenant_id AND x.deleted=0 AND x.source_bill_no IS NOT NULL AND x.source_bill_no<>'' AND NOT EXISTS(SELECT 1 FROM mes_kingdee_production_material_list y WHERE y.work_order_id=x.work_order_id AND y.tenant_id=x.tenant_id AND y.deleted=0 AND y.source_bill_no=x.source_bill_no AND y.id<x.id) LIMIT 1) AS CHAR),3,CHAR_LENGTH(CAST((SELECT JSON_ARRAYAGG(x.source_bill_no) OVER(ORDER BY x.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM mes_kingdee_production_material_list x WHERE x.work_order_id=mes_pro_work_order.id AND x.tenant_id=mes_pro_work_order.tenant_id AND x.deleted=0 AND x.source_bill_no IS NOT NULL AND x.source_bill_no<>'' AND NOT EXISTS(SELECT 1 FROM mes_kingdee_production_material_list y WHERE y.work_order_id=x.work_order_id AND y.tenant_id=x.tenant_id AND y.deleted=0 AND y.source_bill_no=x.source_bill_no AND y.id<x.id) LIMIT 1) AS CHAR))-4),CONCAT(CHAR(34),', ',CHAR(34)), '、'),CHAR(34))),'')"))
    );



    default MesProWorkOrderDO selectByIdForUpdate(Long id) {
        if (id == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProWorkOrderDO>()
                .eq(MesProWorkOrderDO::getId, id)
                .last("FOR UPDATE"));
    }

    default MesProWorkOrderDO selectByTenantIdAndCodeForUpdate(Long tenantId, String code) {
        if (tenantId == null || code == null || code.isBlank()) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProWorkOrderDO>()
                .eq(MesProWorkOrderDO::getTenantId, tenantId)
                .eq(MesProWorkOrderDO::getCode, code)
                .last("FOR UPDATE"));
    }

    default List<MesProWorkOrderDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<MesProWorkOrderDO>()
                .orderByAsc(MesProWorkOrderDO::getId));
    }

    default PageResult<MesProWorkOrderDO> selectPage(MesProWorkOrderPageReqVO reqVO) {
        return selectPageByProductIds(reqVO, null);
    }

    default PageResult<MesProWorkOrderDO> selectPageByProductIds(MesProWorkOrderPageReqVO reqVO,
                                                                 Collection<Long> productIds) {
        LambdaQueryWrapperX<MesProWorkOrderDO> queryWrapper = new LambdaQueryWrapperX<MesProWorkOrderDO>()
                .eqIfPresent(MesProWorkOrderDO::getCode, reqVO.getSyncCode())
                .likeIfPresent(MesProWorkOrderDO::getCode, reqVO.getCode())
                .likeIfPresent(MesProWorkOrderDO::getName, reqVO.getName())
                .eqIfPresent(MesProWorkOrderDO::getType, reqVO.getType())
                .likeIfPresent(MesProWorkOrderDO::getOrderSourceCode, reqVO.getOrderSourceCode())
                .likeIfPresent(MesProWorkOrderDO::getDemandBillNo, reqVO.getDemandBillNo())
                .eqIfPresent(MesProWorkOrderDO::getProductId,
                        CollUtil.isEmpty(productIds) ? resolveSingleProductId(reqVO) : null)
                .inIfPresent(MesProWorkOrderDO::getProductId, productIds)
                .eqIfPresent(MesProWorkOrderDO::getClientId, reqVO.getClientId())
                .eqIfPresent(MesProWorkOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(MesProWorkOrderDO::getTemporaryFrozen, reqVO.getTemporaryFrozen())
                .betweenIfPresent(MesProWorkOrderDO::getQuantity, reqVO.getQuantity())
                .betweenIfPresent(MesProWorkOrderDO::getRequestDate, reqVO.getRequestDate());
        QuickFilterUtils.filter(queryWrapper, reqVO.getQuickFilter(), Map.of(
                "code", QuickFilterUtils.QuickFilterField.text(MesProWorkOrderDO::getCode),
                "demandBillNo", QuickFilterUtils.QuickFilterField.text(MesProWorkOrderDO::getDemandBillNo),
                "productSpecification", QuickFilterUtils.QuickFilterField.text(MesProWorkOrderDO::getMaterialSpecification),
                "requestDate", QuickFilterUtils.QuickFilterField.localDateTimeRange(MesProWorkOrderDO::getRequestDate),
                "status", QuickFilterUtils.QuickFilterField.integerSelect(MesProWorkOrderDO::getStatus)
        ));
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, queryWrapper
                .orderByAsc(MesProWorkOrderDO::getTemporaryFrozen)
                .orderByDesc(MesProWorkOrderDO::getId), LIST_FILTER_FIELDS));
    }

    private static Long resolveSingleProductId(MesProWorkOrderPageReqVO reqVO) {
        if (reqVO.getProductId() != null) {
            return reqVO.getProductId();
        }
        if (reqVO.getProductNameFilterId() != null) {
            return reqVO.getProductNameFilterId();
        }
        return reqVO.getProductCodeFilterId();
    }

    default MesProWorkOrderDO selectByCode(String code) {
        return selectOne(MesProWorkOrderDO::getCode, code);
    }

    default List<MesProWorkOrderDO> selectCandidatesByKeyword(String keyword, Collection<Long> productIds) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String searchText = keyword.trim();
        return selectList(new LambdaQueryWrapperX<MesProWorkOrderDO>()
                .and(wrapper -> {
                    wrapper.like(MesProWorkOrderDO::getCode, searchText);
                    if (productIds != null && !productIds.isEmpty()) {
                        wrapper.or().in(MesProWorkOrderDO::getProductId, productIds);
                    }
                })
                .orderByDesc(MesProWorkOrderDO::getId));
    }

    default List<MesProWorkOrderDO> selectListByCodes(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProWorkOrderDO>()
                .in(MesProWorkOrderDO::getCode, codes)
                .orderByDesc(MesProWorkOrderDO::getId));
    }

    default List<MesProWorkOrderDO> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProWorkOrderDO>()
                .in(MesProWorkOrderDO::getId, ids)
                .orderByDesc(MesProWorkOrderDO::getId));
    }

    default List<MesProWorkOrderDO> selectListByIdsForUpdate(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProWorkOrderDO>()
                .in(MesProWorkOrderDO::getId, ids)
                .last("FOR UPDATE"));
    }

    default List<MesProWorkOrderDO> selectListByProductIdsAndStatuses(Collection<Long> productIds,
                                                                      Collection<Integer> statuses) {
        if (productIds == null || productIds.isEmpty() || statuses == null || statuses.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProWorkOrderDO>()
                .in(MesProWorkOrderDO::getProductId, productIds)
                .in(MesProWorkOrderDO::getStatus, statuses)
                .orderByAsc(MesProWorkOrderDO::getId));
    }

    default List<MesProWorkOrderDO> selectListByProductIds(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProWorkOrderDO>()
                .in(MesProWorkOrderDO::getProductId, productIds)
                .orderByAsc(MesProWorkOrderDO::getId));
    }

    default void updateProducedQuantity(Long id, BigDecimal incrQuantityProduced) {
        update(null, new LambdaUpdateWrapper<MesProWorkOrderDO>()
                .eq(MesProWorkOrderDO::getId, id)
                .setSql("quantity_produced = IFNULL(quantity_produced, 0) + " + incrQuantityProduced));
    }

    /** Refresh only the ERP-owned snapshot, including explicit empty source values. */
    default int updateErpSnapshot(MesProWorkOrderDO snapshot) {
        return update(new MesProWorkOrderDO(), new LambdaUpdateWrapper<MesProWorkOrderDO>()
                .eq(MesProWorkOrderDO::getId, snapshot.getId())
                .set(MesProWorkOrderDO::getCode, snapshot.getCode())
                .set(MesProWorkOrderDO::getName, snapshot.getName())
                .set(MesProWorkOrderDO::getType, snapshot.getType())
                .set(MesProWorkOrderDO::getOrderSourceType, snapshot.getOrderSourceType())
                .set(MesProWorkOrderDO::getOrderSourceCode, snapshot.getOrderSourceCode())
                .set(MesProWorkOrderDO::getDemandBillNo, snapshot.getDemandBillNo())
                .set(MesProWorkOrderDO::getProductId, snapshot.getProductId())
                .set(MesProWorkOrderDO::getMaterialSpecification, snapshot.getMaterialSpecification())
                .set(MesProWorkOrderDO::getQuantity, snapshot.getQuantity())
                .set(MesProWorkOrderDO::getBatchCode, snapshot.getBatchCode())
                .set(MesProWorkOrderDO::getWorkshopName, snapshot.getWorkshopName())
                .set(MesProWorkOrderDO::getBomVersion, snapshot.getBomVersion())
                .set(MesProWorkOrderDO::getPickMode, snapshot.getPickMode())
                .set(MesProWorkOrderDO::getAuxiliaryCode, snapshot.getAuxiliaryCode())
                .set(MesProWorkOrderDO::getBusinessStatus, snapshot.getBusinessStatus())
                .set(MesProWorkOrderDO::getDrawingNumber, snapshot.getDrawingNumber())
                .set(MesProWorkOrderDO::getRefNo, snapshot.getRefNo())
                .set(MesProWorkOrderDO::getScheduleStatus, snapshot.getScheduleStatus())
                .set(MesProWorkOrderDO::getPlannedStartTime, snapshot.getPlannedStartTime())
                .set(MesProWorkOrderDO::getPlannedEndTime, snapshot.getPlannedEndTime())
                .set(MesProWorkOrderDO::getRequestDate, snapshot.getRequestDate())
                .set(MesProWorkOrderDO::getRemark, snapshot.getRemark()));
    }

    default void updateBatchCodeIfBlank(Long id, String batchCode) {
        update(null, new LambdaUpdateWrapper<MesProWorkOrderDO>()
                .eq(MesProWorkOrderDO::getId, id)
                .and(wrapper -> wrapper.isNull(MesProWorkOrderDO::getBatchCode)
                        .or()
                        .eq(MesProWorkOrderDO::getBatchCode, ""))
                .set(MesProWorkOrderDO::getBatchCode, batchCode));
    }

    default void updateQuantityScheduled(Long id, BigDecimal quantityScheduled) {
        update(null, new LambdaUpdateWrapper<MesProWorkOrderDO>()
                .eq(MesProWorkOrderDO::getId, id)
                .set(MesProWorkOrderDO::getQuantityScheduled, quantityScheduled));
    }

    default Long selectCountByVendorId(Long vendorId) {
        return selectCount(MesProWorkOrderDO::getVendorId, vendorId);
    }

    default int updateTemporaryFrozenByIds(Collection<Long> ids, Boolean temporaryFrozen) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return update(null, new LambdaUpdateWrapper<MesProWorkOrderDO>()
                .in(MesProWorkOrderDO::getId, ids)
                .set(MesProWorkOrderDO::getTemporaryFrozen, temporaryFrozen));
    }

    default void updateTemporaryFrozenAll(Boolean temporaryFrozen) {
        update(null, new LambdaUpdateWrapper<MesProWorkOrderDO>()
                .set(MesProWorkOrderDO::getTemporaryFrozen, temporaryFrozen));
    }

}
