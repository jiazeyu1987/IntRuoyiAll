package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.QuickFilterUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface MesProEdhrBatchExecutionMapper extends BaseMapperX<MesProEdhrBatchExecutionDO> {

    int BATCH_STATUS_ARCHIVED = 40;
    int BATCH_STATUS_REJECTED = 50;
    int BATCH_STATUS_VOIDED = 60;

    @Update("UPDATE mes_pro_edhr_batch_execution SET active_context_key = NULL WHERE id = #{id}")
    void clearActiveContextKey(@Param("id") Long id);

    default MesProEdhrBatchExecutionDO selectByActiveContextKey(String activeContextKey) {
        if (activeContextKey == null || activeContextKey.isBlank()) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProEdhrBatchExecutionDO>()
                .eq(MesProEdhrBatchExecutionDO::getActiveContextKey, activeContextKey)
                .notIn(MesProEdhrBatchExecutionDO::getStatus, BATCH_STATUS_VOIDED));
    }

    default MesProEdhrBatchExecutionDO selectByContext(Long workOrderId, String batchCode, Long routeId) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrBatchExecutionDO>()
                .eq(MesProEdhrBatchExecutionDO::getWorkOrderId, workOrderId)
                .eq(MesProEdhrBatchExecutionDO::getBatchCode, batchCode)
                .eq(MesProEdhrBatchExecutionDO::getRouteId, routeId)
                .notIn(MesProEdhrBatchExecutionDO::getStatus, BATCH_STATUS_VOIDED)
                .orderByDesc(MesProEdhrBatchExecutionDO::getId));
    }

    default List<MesProEdhrBatchExecutionDO> selectListByWorkOrderIdAndBatchCode(Long workOrderId,
                                                                                 String batchCode) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrBatchExecutionDO>()
                .eq(MesProEdhrBatchExecutionDO::getWorkOrderId, workOrderId)
                .eq(MesProEdhrBatchExecutionDO::getBatchCode, batchCode)
                .notIn(MesProEdhrBatchExecutionDO::getStatus, BATCH_STATUS_VOIDED)
                .orderByDesc(MesProEdhrBatchExecutionDO::getId));
    }

    default PageResult<MesProEdhrBatchExecutionDO> selectPage(EdhrBatchExecutionPageReqVO reqVO) {
        return selectPage(reqVO, buildPageQuery(reqVO));
    }

    default List<MesProEdhrBatchExecutionDO> selectList(EdhrBatchExecutionPageReqVO reqVO) {
        return selectList(buildPageQuery(reqVO));
    }

    private LambdaQueryWrapperX<MesProEdhrBatchExecutionDO> buildPageQuery(EdhrBatchExecutionPageReqVO reqVO) {
        LambdaQueryWrapperX<MesProEdhrBatchExecutionDO> queryWrapper = new LambdaQueryWrapperX<MesProEdhrBatchExecutionDO>()
                .inIfPresent(MesProEdhrBatchExecutionDO::getId, reqVO.getBatchExecutionIds())
                .likeIfPresent(MesProEdhrBatchExecutionDO::getBatchExecutionCode, reqVO.getBatchExecutionCode())
                .eqIfPresent(MesProEdhrBatchExecutionDO::getWorkOrderId, reqVO.getWorkOrderId())
                .likeIfPresent(MesProEdhrBatchExecutionDO::getWorkOrderCode, reqVO.getWorkOrderCode())
                .likeIfPresent(MesProEdhrBatchExecutionDO::getBatchCode, reqVO.getBatchCode())
                .likeIfPresent(MesProEdhrBatchExecutionDO::getProductCode, reqVO.getProductCode())
                .eqIfPresent(MesProEdhrBatchExecutionDO::getRouteId, reqVO.getRouteId())
                .likeIfPresent(MesProEdhrBatchExecutionDO::getRouteCode, reqVO.getRouteCode())
                .eqIfPresent(MesProEdhrBatchExecutionDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesProEdhrBatchExecutionDO::getCreateTime, reqVO.getCreateTime());
        if (reqVO.getStatuses() != null && !reqVO.getStatuses().isEmpty()) {
            queryWrapper.in(MesProEdhrBatchExecutionDO::getStatus, reqVO.getStatuses());
        }
        if (reqVO.getExcludeStatuses() != null && !reqVO.getExcludeStatuses().isEmpty()) {
            queryWrapper.notIn(MesProEdhrBatchExecutionDO::getStatus, reqVO.getExcludeStatuses());
        }
        if (Boolean.TRUE.equals(reqVO.getExcludeReleased())) {
            queryWrapper.notExists("SELECT 1 FROM mes_pro_edhr_release_transaction rt "
                    + "WHERE rt.tenant_id = mes_pro_edhr_batch_execution.tenant_id "
                    + "AND rt.batch_execution_id = mes_pro_edhr_batch_execution.id "
                    + "AND rt.deleted = 0 "
                    + "AND rt.release_status = 'RELEASED'");
        }
        if (Boolean.TRUE.equals(reqVO.getCompletedTraceOnly())) {
            queryWrapper.and(wrapper -> wrapper
                    .in(MesProEdhrBatchExecutionDO::getStatus, BATCH_STATUS_ARCHIVED, BATCH_STATUS_REJECTED)
                    .or()
                    .exists(releasedTransactionExistsSql()));
        }
        queryWrapper.notIn(MesProEdhrBatchExecutionDO::getStatus, BATCH_STATUS_VOIDED);
        QuickFilterUtils.filter(queryWrapper, reqVO.getQuickFilter(), Map.of(
                "batchExecutionCode", QuickFilterUtils.QuickFilterField.text(MesProEdhrBatchExecutionDO::getBatchExecutionCode),
                "workOrderCode", QuickFilterUtils.QuickFilterField.text(MesProEdhrBatchExecutionDO::getWorkOrderCode),
                "batchCode", QuickFilterUtils.QuickFilterField.text(MesProEdhrBatchExecutionDO::getBatchCode),
                "product", QuickFilterUtils.QuickFilterField.text(MesProEdhrBatchExecutionDO::getProductCode),
                "status", QuickFilterUtils.QuickFilterField.integerSelect(MesProEdhrBatchExecutionDO::getStatus),
                "createTime", QuickFilterUtils.QuickFilterField.localDateTimeRange(MesProEdhrBatchExecutionDO::getCreateTime)
        ));
        return queryWrapper.orderByDesc(MesProEdhrBatchExecutionDO::getId);
    }

    private String releasedTransactionExistsSql() {
        return "SELECT 1 FROM mes_pro_edhr_release_transaction rt "
                + "WHERE rt.tenant_id = mes_pro_edhr_batch_execution.tenant_id "
                + "AND rt.batch_execution_id = mes_pro_edhr_batch_execution.id "
                + "AND rt.deleted = 0 "
                + "AND rt.release_status = 'RELEASED'";
    }

    default List<MesProEdhrBatchExecutionDO> selectUnarchivedListByBatchCode(String batchCode) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrBatchExecutionDO>()
                .eq(MesProEdhrBatchExecutionDO::getBatchCode, batchCode)
                .notIn(MesProEdhrBatchExecutionDO::getStatus, 40, 50, BATCH_STATUS_VOIDED)
                .orderByDesc(MesProEdhrBatchExecutionDO::getId));
    }
}
