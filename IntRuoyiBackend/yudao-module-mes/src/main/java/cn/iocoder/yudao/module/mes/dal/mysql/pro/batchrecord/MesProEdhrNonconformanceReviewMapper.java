package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Collection;

@Mapper
public interface MesProEdhrNonconformanceReviewMapper extends BaseMapperX<MesProEdhrNonconformanceReviewDO> {

    String STATUS_PENDING_REVIEW = "pending_review";

    @Select("SELECT * FROM mes_pro_edhr_nonconformance_review WHERE id = #{id} "
            + "AND deleted = b'0' FOR UPDATE")
    MesProEdhrNonconformanceReviewDO selectByIdForUpdate(@Param("id") Long id);

    default PageResult<MesProEdhrNonconformanceReviewDO> selectPage(
            MesProEdhrNonconformanceReviewPageReqVO reqVO) {
        return selectPage(reqVO, buildPageQuery(reqVO));
    }

    default PageResult<MesProEdhrNonconformanceReviewDO> selectPendingPage(
            MesProEdhrNonconformanceReviewPageReqVO reqVO) {
        return selectPage(reqVO, buildPageQuery(reqVO)
                .eq(MesProEdhrNonconformanceReviewDO::getReviewStatus, STATUS_PENDING_REVIEW));
    }

    default MesProEdhrNonconformanceReviewDO selectPendingByBatchExecutionId(Long batchExecutionId) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrNonconformanceReviewDO>()
                .eq(MesProEdhrNonconformanceReviewDO::getBatchExecutionId, batchExecutionId)
                .eq(MesProEdhrNonconformanceReviewDO::getReviewStatus, STATUS_PENDING_REVIEW)
                .orderByDesc(MesProEdhrNonconformanceReviewDO::getId));
    }

    default MesProEdhrNonconformanceReviewDO selectPendingBySource(String sourceType, Long sourceId) {
        if (sourceType == null || sourceType.isBlank() || sourceId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProEdhrNonconformanceReviewDO>()
                .eq(MesProEdhrNonconformanceReviewDO::getSourceType, sourceType)
                .eq(MesProEdhrNonconformanceReviewDO::getSourceId, sourceId)
                .eq(MesProEdhrNonconformanceReviewDO::getReviewStatus, STATUS_PENDING_REVIEW)
                .orderByDesc(MesProEdhrNonconformanceReviewDO::getId)
                .last("LIMIT 1"));
    }

    default MesProEdhrNonconformanceReviewDO selectLatestBySource(String sourceType, Long sourceId) {
        if (sourceType == null || sourceType.isBlank() || sourceId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProEdhrNonconformanceReviewDO>()
                .eq(MesProEdhrNonconformanceReviewDO::getSourceType, sourceType)
                .eq(MesProEdhrNonconformanceReviewDO::getSourceId, sourceId)
                .orderByDesc(MesProEdhrNonconformanceReviewDO::getId)
                .last("LIMIT 1"));
    }

    default List<MesProEdhrNonconformanceReviewDO> selectLatestBySourceIds(
            String sourceType, Collection<Long> sourceIds) {
        if (sourceType == null || sourceType.isBlank() || sourceIds == null || sourceIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProEdhrNonconformanceReviewDO>()
                .eq(MesProEdhrNonconformanceReviewDO::getSourceType, sourceType)
                .in(MesProEdhrNonconformanceReviewDO::getSourceId, sourceIds)
                .orderByDesc(MesProEdhrNonconformanceReviewDO::getId));
    }

    default Long selectPendingCountByWorkOrderId(Long workOrderId) {
        return selectCount(new LambdaQueryWrapperX<MesProEdhrNonconformanceReviewDO>()
                .eq(MesProEdhrNonconformanceReviewDO::getWorkOrderId, workOrderId)
                .eq(MesProEdhrNonconformanceReviewDO::getReviewStatus, STATUS_PENDING_REVIEW));
    }

    default Long selectBlockingCountByWorkOrderId(Long workOrderId) {
        return selectCount(new LambdaQueryWrapperX<MesProEdhrNonconformanceReviewDO>()
                .eq(MesProEdhrNonconformanceReviewDO::getWorkOrderId, workOrderId)
                .and(query -> query
                        .eq(MesProEdhrNonconformanceReviewDO::getReviewStatus, STATUS_PENDING_REVIEW)
                        .or()
                        .eq(MesProEdhrNonconformanceReviewDO::getDisposition, "void")));
    }

    default List<MesProEdhrNonconformanceReviewDO> selectListByBatchExecutionId(Long batchExecutionId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrNonconformanceReviewDO>()
                .eq(MesProEdhrNonconformanceReviewDO::getBatchExecutionId, batchExecutionId)
                .orderByDesc(MesProEdhrNonconformanceReviewDO::getId));
    }

    private LambdaQueryWrapperX<MesProEdhrNonconformanceReviewDO> buildPageQuery(
            MesProEdhrNonconformanceReviewPageReqVO reqVO) {
        return new LambdaQueryWrapperX<MesProEdhrNonconformanceReviewDO>()
                .likeIfPresent(MesProEdhrNonconformanceReviewDO::getReviewCode, reqVO.getReviewCode())
                .eqIfPresent(MesProEdhrNonconformanceReviewDO::getSourceType, reqVO.getSourceType())
                .eqIfPresent(MesProEdhrNonconformanceReviewDO::getBatchExecutionId, reqVO.getBatchExecutionId())
                .likeIfPresent(MesProEdhrNonconformanceReviewDO::getBatchExecutionCode,
                        reqVO.getBatchExecutionCode())
                .likeIfPresent(MesProEdhrNonconformanceReviewDO::getWorkOrderCode, reqVO.getWorkOrderCode())
                .likeIfPresent(MesProEdhrNonconformanceReviewDO::getBatchCode, reqVO.getBatchCode())
                .eqIfPresent(MesProEdhrNonconformanceReviewDO::getReviewStatus, reqVO.getReviewStatus())
                .eqIfPresent(MesProEdhrNonconformanceReviewDO::getDisposition, reqVO.getDisposition())
                .orderByDesc(MesProEdhrNonconformanceReviewDO::getId);
    }
}
