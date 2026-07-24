package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitIssuePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrInitIssueDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrInitIssueMapper extends BaseMapperX<MesProEdhrInitIssueDO> {

    String ISSUE_LEVEL_BLOCKER = "BLOCKER";
    String ISSUE_STATUS_OPEN = "OPEN";
    String ISSUE_STATUS_SUPERSEDED = "SUPERSEDED";

    default PageResult<MesProEdhrInitIssueDO> selectPage(MesProEdhrInitIssuePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrInitIssueDO>()
                .eq(MesProEdhrInitIssueDO::getInitBatchId, reqVO.getInitBatchId())
                .eqIfPresent(MesProEdhrInitIssueDO::getIssueLevel, reqVO.getIssueLevel())
                .eqIfPresent(MesProEdhrInitIssueDO::getIssueStatus, reqVO.getIssueStatus())
                .eqIfPresent(MesProEdhrInitIssueDO::getPackageType, reqVO.getPackageType())
                .likeIfPresent(MesProEdhrInitIssueDO::getSourceFileName, reqVO.getSourceFileName())
                .likeIfPresent(MesProEdhrInitIssueDO::getResponsibleName, reqVO.getResponsibleName())
                .orderByDesc(MesProEdhrInitIssueDO::getIssueLevel)
                .orderByAsc(MesProEdhrInitIssueDO::getSourceFileName)
                .orderByAsc(MesProEdhrInitIssueDO::getSourceRowNo)
                .orderByDesc(MesProEdhrInitIssueDO::getId));
    }

    default List<MesProEdhrInitIssueDO> selectOpenListByBatchId(Long initBatchId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrInitIssueDO>()
                .eq(MesProEdhrInitIssueDO::getInitBatchId, initBatchId)
                .eq(MesProEdhrInitIssueDO::getIssueStatus, ISSUE_STATUS_OPEN)
                .orderByDesc(MesProEdhrInitIssueDO::getIssueLevel)
                .orderByAsc(MesProEdhrInitIssueDO::getSourceFileName)
                .orderByAsc(MesProEdhrInitIssueDO::getSourceRowNo)
                .orderByDesc(MesProEdhrInitIssueDO::getId));
    }

    default int countOpenBlockers(Long initBatchId) {
        return Math.toIntExact(selectCount(new LambdaQueryWrapperX<MesProEdhrInitIssueDO>()
                .eq(MesProEdhrInitIssueDO::getInitBatchId, initBatchId)
                .eq(MesProEdhrInitIssueDO::getIssueLevel, ISSUE_LEVEL_BLOCKER)
                .eq(MesProEdhrInitIssueDO::getIssueStatus, ISSUE_STATUS_OPEN)));
    }

    default int closeOpenByBatchId(Long initBatchId) {
        return update(new MesProEdhrInitIssueDO()
                        .setIssueStatus(ISSUE_STATUS_SUPERSEDED),
                new LambdaUpdateWrapper<MesProEdhrInitIssueDO>()
                        .eq(MesProEdhrInitIssueDO::getInitBatchId, initBatchId)
                        .eq(MesProEdhrInitIssueDO::getIssueStatus, ISSUE_STATUS_OPEN));
    }
}
