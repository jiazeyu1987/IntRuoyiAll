package cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.QuickFilterUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderCompletionFilterEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.hutool.core.collection.CollUtil;

/**
 * MES 排产工单 Mapper
 */
@Mapper
public interface MesProScheduleOrderMapper extends BaseMapperX<MesProScheduleOrderDO> {

    default PageResult<MesProScheduleOrderDO> selectPage(MesProScheduleOrderPageReqVO reqVO) {
        return selectPageByProductIds(reqVO, null);
    }

    default PageResult<MesProScheduleOrderDO> selectPageByProductIds(MesProScheduleOrderPageReqVO reqVO,
                                                                     Collection<Long> productIds) {
        LambdaQueryWrapperX<MesProScheduleOrderDO> queryWrapper = new LambdaQueryWrapperX<MesProScheduleOrderDO>()
                .likeIfPresent(MesProScheduleOrderDO::getCode, reqVO.getCode())
                .likeIfPresent(MesProScheduleOrderDO::getErpWorkOrderCode, reqVO.getErpWorkOrderCode())
                .eqIfPresent(MesProScheduleOrderDO::getWorkOrderId, reqVO.getWorkOrderId())
                .eqIfPresent(MesProScheduleOrderDO::getProductId,
                        CollUtil.isEmpty(productIds) ? reqVO.getProductId() : null)
                .inIfPresent(MesProScheduleOrderDO::getProductId, productIds)
                .eqIfPresent(MesProScheduleOrderDO::getDiffStatus, reqVO.getDiffStatus())
                .betweenIfPresent(MesProScheduleOrderDO::getPromiseDate, reqVO.getPromiseDate());
        if (MesProScheduleOrderCompletionFilterEnum.INCOMPLETE.getValue().equals(reqVO.getCompletionFilter())) {
            queryWrapper.in(MesProScheduleOrderDO::getStatus,
                    MesProScheduleOrderStatusEnum.PREPARE.getStatus(),
                    MesProScheduleOrderStatusEnum.SCHEDULED.getStatus(),
                    MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus());
        } else if (MesProScheduleOrderCompletionFilterEnum.COMPLETED.getValue().equals(reqVO.getCompletionFilter())) {
            queryWrapper.eq(MesProScheduleOrderDO::getStatus, MesProScheduleOrderStatusEnum.FINISHED.getStatus());
        } else if (reqVO.getCompletionFilter() == null) {
            queryWrapper.eqIfPresent(MesProScheduleOrderDO::getStatus, reqVO.getStatus());
        }
        QuickFilterUtils.filter(queryWrapper, reqVO.getQuickFilter(), Map.of(
                "code", QuickFilterUtils.QuickFilterField.text(MesProScheduleOrderDO::getCode),
                "erpWorkOrderCode", QuickFilterUtils.QuickFilterField.text(MesProScheduleOrderDO::getErpWorkOrderCode),
                "promiseDate", QuickFilterUtils.QuickFilterField.localDateRange(MesProScheduleOrderDO::getPromiseDate),
                "status", QuickFilterUtils.QuickFilterField.integerSelect(MesProScheduleOrderDO::getStatus)
        ));
        return selectPage(reqVO, queryWrapper
                .orderByAsc(MesProScheduleOrderDO::getPromiseDate)
                .orderByAsc(MesProScheduleOrderDO::getPriorityNo)
                .orderByAsc(MesProScheduleOrderDO::getId));
    }

    default MesProScheduleOrderDO selectEffectiveByWorkOrderId(Long workOrderId) {
        return selectOne(new LambdaQueryWrapperX<MesProScheduleOrderDO>()
                .eq(MesProScheduleOrderDO::getWorkOrderId, workOrderId)
                .ne(MesProScheduleOrderDO::getStatus, MesProScheduleOrderStatusEnum.FINISHED.getStatus())
                .ne(MesProScheduleOrderDO::getStatus, MesProScheduleOrderStatusEnum.CANCELED.getStatus()));
    }

    default List<MesProScheduleOrderDO> selectListByWorkOrderIds(Collection<Long> workOrderIds) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProScheduleOrderDO>()
                .in(MesProScheduleOrderDO::getWorkOrderId, workOrderIds)
                .orderByDesc(MesProScheduleOrderDO::getId));
    }

    @Select("SELECT MAX(code) FROM mes_pro_schedule_order "
            + "WHERE deleted = b'0' AND code LIKE CONCAT(#{prefix}, '%')")
    String selectMaxCodeByPrefix(String prefix);

    @Select("SELECT MAX(route_version) FROM mes_pro_schedule_order "
            + "WHERE deleted = b'0' AND route_version LIKE CONCAT(#{prefix}, '%')")
    String selectMaxRouteVersionByPrefix(String prefix);

    default List<MesProScheduleOrderDO> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProScheduleOrderDO>()
                .in(MesProScheduleOrderDO::getId, ids)
                .orderByDesc(MesProScheduleOrderDO::getId));
    }

    default List<MesProScheduleOrderDO> selectEffectiveListByWorkOrderIds(Collection<Long> workOrderIds) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProScheduleOrderDO>()
                .in(MesProScheduleOrderDO::getWorkOrderId, workOrderIds)
                .ne(MesProScheduleOrderDO::getStatus, MesProScheduleOrderStatusEnum.FINISHED.getStatus())
                .ne(MesProScheduleOrderDO::getStatus, MesProScheduleOrderStatusEnum.CANCELED.getStatus())
                .orderByDesc(MesProScheduleOrderDO::getId));
    }

    default List<MesProScheduleOrderDO> selectListForNightlyReplan() {
        return selectList(new LambdaQueryWrapperX<MesProScheduleOrderDO>()
                .in(MesProScheduleOrderDO::getStatus,
                        MesProScheduleOrderStatusEnum.PREPARE.getStatus(),
                        MesProScheduleOrderStatusEnum.SCHEDULED.getStatus(),
                        MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .eq(MesProScheduleOrderDO::getFrozen, Boolean.FALSE)
                .orderByAsc(MesProScheduleOrderDO::getPromiseDate)
                .orderByAsc(MesProScheduleOrderDO::getPriorityNo)
                .orderByAsc(MesProScheduleOrderDO::getId));
    }

    default List<MesProScheduleOrderDO> selectListForProcessWip() {
        return selectList(new LambdaQueryWrapperX<MesProScheduleOrderDO>()
                .in(MesProScheduleOrderDO::getStatus,
                        MesProScheduleOrderStatusEnum.PREPARE.getStatus(),
                        MesProScheduleOrderStatusEnum.SCHEDULED.getStatus(),
                        MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .eq(MesProScheduleOrderDO::getFrozen, Boolean.FALSE)
                .orderByAsc(MesProScheduleOrderDO::getPromiseDate)
                .orderByAsc(MesProScheduleOrderDO::getPriorityNo)
                .orderByAsc(MesProScheduleOrderDO::getId));
    }

    default List<MesProScheduleOrderDO> selectListWithoutRoute() {
        return selectList(new LambdaQueryWrapperX<MesProScheduleOrderDO>()
                .eq(MesProScheduleOrderDO::getAutoSchedulable, Boolean.FALSE)
                .isNull(MesProScheduleOrderDO::getRouteId)
                .orderByAsc(MesProScheduleOrderDO::getPromiseDate)
                .orderByAsc(MesProScheduleOrderDO::getId));
    }

    default List<MesProScheduleOrderDO> selectAutoSchedulableByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProScheduleOrderDO>()
                .in(MesProScheduleOrderDO::getId, ids)
                .in(MesProScheduleOrderDO::getStatus,
                        MesProScheduleOrderStatusEnum.PREPARE.getStatus(),
                        MesProScheduleOrderStatusEnum.SCHEDULED.getStatus(),
                        MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .eq(MesProScheduleOrderDO::getAutoSchedulable, Boolean.TRUE)
                .eq(MesProScheduleOrderDO::getFrozen, Boolean.FALSE)
                .orderByAsc(MesProScheduleOrderDO::getPromiseDate)
                .orderByAsc(MesProScheduleOrderDO::getPriorityNo)
                .orderByAsc(MesProScheduleOrderDO::getId));
    }

    default int updateProgress(Long id, BigDecimal completedQuantity, BigDecimal uncompletedQuantity,
            BigDecimal progressPercent) {
        MesProScheduleOrderDO updateObj = new MesProScheduleOrderDO();
        updateObj.setId(id);
        updateObj.setCompletedQuantity(completedQuantity);
        updateObj.setUncompletedQuantity(uncompletedQuantity);
        updateObj.setProgressPercent(progressPercent);
        return updateById(updateObj);
    }

    default int updateProgressSummary(Long id, BigDecimal totalQuantity, BigDecimal completedQuantity,
            BigDecimal uncompletedQuantity, BigDecimal progressPercent, Integer status) {
        MesProScheduleOrderDO updateObj = new MesProScheduleOrderDO();
        updateObj.setId(id);
        updateObj.setTotalQuantity(totalQuantity);
        updateObj.setCompletedQuantity(completedQuantity);
        updateObj.setUncompletedQuantity(uncompletedQuantity);
        updateObj.setProgressPercent(progressPercent);
        updateObj.setStatus(status);
        return updateById(updateObj);
    }

    default int clearManualFinishAndUpdateProgress(Long id, Integer status, BigDecimal totalQuantity,
            BigDecimal completedQuantity, BigDecimal uncompletedQuantity, BigDecimal progressPercent) {
        return update(null, new LambdaUpdateWrapper<MesProScheduleOrderDO>()
                .eq(MesProScheduleOrderDO::getId, id)
                .set(MesProScheduleOrderDO::getManualFinished, Boolean.FALSE)
                .set(MesProScheduleOrderDO::getManualFinishedTime, null)
                .set(MesProScheduleOrderDO::getManualFinishedBy, null)
                .set(MesProScheduleOrderDO::getManualFinishedReason, null)
                .set(MesProScheduleOrderDO::getStatus, status)
                .set(MesProScheduleOrderDO::getTotalQuantity, totalQuantity)
                .set(MesProScheduleOrderDO::getCompletedQuantity, completedQuantity)
                .set(MesProScheduleOrderDO::getUncompletedQuantity, uncompletedQuantity)
                .set(MesProScheduleOrderDO::getProgressPercent, progressPercent));
    }

    default int clearFrozen(Long id) {
        return update(null, new LambdaUpdateWrapper<MesProScheduleOrderDO>()
                .eq(MesProScheduleOrderDO::getId, id)
                .set(MesProScheduleOrderDO::getFrozen, Boolean.FALSE)
                .set(MesProScheduleOrderDO::getFrozenTime, null)
                .set(MesProScheduleOrderDO::getFrozenBy, null)
                .set(MesProScheduleOrderDO::getFreezeReason, null));
    }

}
