package cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
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
                .likeIfPresent(MesProWorkOrderDO::getCode, reqVO.getCode())
                .likeIfPresent(MesProWorkOrderDO::getName, reqVO.getName())
                .eqIfPresent(MesProWorkOrderDO::getType, reqVO.getType())
                .likeIfPresent(MesProWorkOrderDO::getOrderSourceCode, reqVO.getOrderSourceCode())
                .eqIfPresent(MesProWorkOrderDO::getProductId,
                        CollUtil.isEmpty(productIds) ? resolveSingleProductId(reqVO) : null)
                .inIfPresent(MesProWorkOrderDO::getProductId, productIds)
                .eqIfPresent(MesProWorkOrderDO::getClientId, reqVO.getClientId())
                .eqIfPresent(MesProWorkOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(MesProWorkOrderDO::getTemporaryFrozen, reqVO.getTemporaryFrozen())
                .betweenIfPresent(MesProWorkOrderDO::getRequestDate, reqVO.getRequestDate());
        QuickFilterUtils.filter(queryWrapper, reqVO.getQuickFilter(), Map.of(
                "code", QuickFilterUtils.QuickFilterField.text(MesProWorkOrderDO::getCode),
                "requestDate", QuickFilterUtils.QuickFilterField.localDateTimeRange(MesProWorkOrderDO::getRequestDate),
                "status", QuickFilterUtils.QuickFilterField.integerSelect(MesProWorkOrderDO::getStatus)
        ));
        return selectPage(reqVO, queryWrapper
                .orderByAsc(MesProWorkOrderDO::getTemporaryFrozen)
                .orderByDesc(MesProWorkOrderDO::getId));
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

    default List<MesProWorkOrderDO> selectCandidatesByKeyword(String keyword, Collection<Long> productIds,
                                                              int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        int safeLimit = Math.max(1, Math.min(limit, 20));
        String searchText = keyword.trim();
        return selectList(new LambdaQueryWrapperX<MesProWorkOrderDO>()
                .and(wrapper -> {
                    wrapper.like(MesProWorkOrderDO::getCode, searchText);
                    if (productIds != null && !productIds.isEmpty()) {
                        wrapper.or().in(MesProWorkOrderDO::getProductId, productIds);
                    }
                })
                .orderByDesc(MesProWorkOrderDO::getId)
                .last("LIMIT " + safeLimit));
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

    default void updateTemporaryFrozenByIds(Collection<Long> ids, Boolean temporaryFrozen) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        update(null, new LambdaUpdateWrapper<MesProWorkOrderDO>()
                .in(MesProWorkOrderDO::getId, ids)
                .set(MesProWorkOrderDO::getTemporaryFrozen, temporaryFrozen));
    }

    default void updateTemporaryFrozenAll(Boolean temporaryFrozen) {
        update(null, new LambdaUpdateWrapper<MesProWorkOrderDO>()
                .set(MesProWorkOrderDO::getTemporaryFrozen, temporaryFrozen));
    }

}
