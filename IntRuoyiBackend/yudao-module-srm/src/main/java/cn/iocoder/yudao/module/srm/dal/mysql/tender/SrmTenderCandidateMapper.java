package cn.iocoder.yudao.module.srm.dal.mysql.tender;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.tender.SrmTenderCandidateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SrmTenderCandidateMapper extends BaseMapperX<SrmTenderCandidateDO> {

    default SrmTenderCandidateDO selectByProjectIdAndSubmissionId(Long projectId, Long submissionId) {
        return selectOne(new LambdaQueryWrapperX<SrmTenderCandidateDO>()
                .eq(SrmTenderCandidateDO::getProjectId, projectId)
                .eq(SrmTenderCandidateDO::getSubmissionId, submissionId)
                .last("LIMIT 1"));
    }

    default List<SrmTenderCandidateDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<SrmTenderCandidateDO>()
                .eq(SrmTenderCandidateDO::getProjectId, projectId)
                .orderByAsc(SrmTenderCandidateDO::getRankNo)
                .orderByAsc(SrmTenderCandidateDO::getId));
    }
}
