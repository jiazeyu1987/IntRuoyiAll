package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrNonconformanceReviewPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrNonconformanceReviewMapper extends BaseMapperX<MesProEdhrNonconformanceReviewDO> {

    String STATUS_PENDING_REVIEW = "pending_review";

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

    default Long selectPendingCountByWorkOrderId(Long workOrderId) {
        return selectCount(new LambdaQueryWrapperX<MesProEdhrNonconformanceReviewDO>()
                .eq(MesProEdhrNonconformanceReviewDO::getWorkOrderId, workOrderId)
                .eq(MesProEdhrNonconformanceReviewDO::getReviewStatus, STATUS_PENDING_REVIEW));
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
