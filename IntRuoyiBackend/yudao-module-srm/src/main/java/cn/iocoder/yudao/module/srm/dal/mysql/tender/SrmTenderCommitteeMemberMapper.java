package cn.iocoder.yudao.module.srm.dal.mysql.tender;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.tender.SrmTenderCommitteeMemberDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SrmTenderCommitteeMemberMapper extends BaseMapperX<SrmTenderCommitteeMemberDO> {

    default List<SrmTenderCommitteeMemberDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<SrmTenderCommitteeMemberDO>()
                .eq(SrmTenderCommitteeMemberDO::getProjectId, projectId)
                .orderByAsc(SrmTenderCommitteeMemberDO::getId));
    }
}
