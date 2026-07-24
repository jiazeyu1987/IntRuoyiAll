package cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.importrecord.MesProFeedbackImportRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackImportRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProFeedbackImportRecordMapper extends BaseMapperX<MesProFeedbackImportRecordDO> {

    default MesProFeedbackImportRecordDO selectBySourceFingerprint(String sourceFileSha256, String sheetName, Integer rowNo) {
        return selectOne(new LambdaQueryWrapperX<MesProFeedbackImportRecordDO>()
                .eq(MesProFeedbackImportRecordDO::getSourceFileSha256, sourceFileSha256)
                .eq(MesProFeedbackImportRecordDO::getSheetName, sheetName)
                .eq(MesProFeedbackImportRecordDO::getRowNo, rowNo));
    }

    default PageResult<MesProFeedbackImportRecordDO> selectPage(MesProFeedbackImportRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProFeedbackImportRecordDO>()
                .eqIfPresent(MesProFeedbackImportRecordDO::getId, reqVO.getId())
                .inIfPresent(MesProFeedbackImportRecordDO::getId, reqVO.getImportRecordIds())
                .eqIfPresent(MesProFeedbackImportRecordDO::getFeedbackId, reqVO.getFeedbackId())
                .eqIfPresent(MesProFeedbackImportRecordDO::getAttributionStatus, reqVO.getAttributionStatus())
                .orderByDesc(MesProFeedbackImportRecordDO::getId));
    }

    default List<MesProFeedbackImportRecordDO> selectListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackImportRecordDO>()
                .in(MesProFeedbackImportRecordDO::getId, ids));
    }

    default List<MesProFeedbackImportRecordDO> selectListByFeedbackIds(Collection<Long> feedbackIds) {
        if (feedbackIds == null || feedbackIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackImportRecordDO>()
                .in(MesProFeedbackImportRecordDO::getFeedbackId, feedbackIds)
                .eq(MesProFeedbackImportRecordDO::getAttributionStatus, MesProFeedbackImportRecordDO.ATTRIBUTION_STATUS_ATTRIBUTED)
                .orderByDesc(MesProFeedbackImportRecordDO::getId));
    }

    default List<MesProFeedbackImportRecordDO> selectAppliedDirectProgressListByScheduleOrderId(Long scheduleOrderId) {
        if (scheduleOrderId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProFeedbackImportRecordDO>()
                .eq(MesProFeedbackImportRecordDO::getScheduleOrderId, scheduleOrderId)
                .eq(MesProFeedbackImportRecordDO::getProgressSourceType,
                        MesProFeedbackImportRecordDO.PROGRESS_SOURCE_TYPE_DIRECT_WORK_REPORT)
                .eq(MesProFeedbackImportRecordDO::getAttributionStatus,
                        MesProFeedbackImportRecordDO.ATTRIBUTION_STATUS_ATTRIBUTED)
                .gt(MesProFeedbackImportRecordDO::getProgressQuantity, java.math.BigDecimal.ZERO)
                .orderByAsc(MesProFeedbackImportRecordDO::getProgressAppliedTime)
                .orderByAsc(MesProFeedbackImportRecordDO::getId));
    }
}
