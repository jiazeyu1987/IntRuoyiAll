package cn.iocoder.yudao.module.srm.dal.mysql.tender;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.tender.SrmTenderSubmissionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SrmTenderSubmissionMapper extends BaseMapperX<SrmTenderSubmissionDO> {

    default SrmTenderSubmissionDO selectByProjectIdAndSupplierId(Long projectId, Long supplierId) {
        return selectOne(new LambdaQueryWrapperX<SrmTenderSubmissionDO>()
                .eq(SrmTenderSubmissionDO::getProjectId, projectId)
                .eq(SrmTenderSubmissionDO::getSupplierId, supplierId)
                .last("LIMIT 1"));
    }

    default List<SrmTenderSubmissionDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<SrmTenderSubmissionDO>()
                .eq(SrmTenderSubmissionDO::getProjectId, projectId)
                .orderByAsc(SrmTenderSubmissionDO::getBidAmount)
                .orderByAsc(SrmTenderSubmissionDO::getId));
    }
}
