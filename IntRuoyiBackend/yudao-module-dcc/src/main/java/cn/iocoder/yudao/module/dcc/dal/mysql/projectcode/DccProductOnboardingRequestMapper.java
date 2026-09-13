package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProductOnboardingRequestDO;
import cn.iocoder.yudao.module.dcc.enums.DccProductOnboardingStatusConstants;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DccProductOnboardingRequestMapper extends BaseMapperX<DccProductOnboardingRequestDO> {

    default List<DccProductOnboardingRequestDO> selectPendingList() {
        return selectList(new LambdaQueryWrapperX<DccProductOnboardingRequestDO>()
                .eq(DccProductOnboardingRequestDO::getStatus,
                        DccProductOnboardingStatusConstants.PENDING_APPROVAL)
                .orderByDesc(DccProductOnboardingRequestDO::getCreateTime)
                .orderByDesc(DccProductOnboardingRequestDO::getId));
    }

    default DccProductOnboardingRequestDO selectPendingByProjectNameAndProjectCode(String projectName,
                                                                                   String projectCode) {
        return selectOne(new LambdaQueryWrapperX<DccProductOnboardingRequestDO>()
                .eq(DccProductOnboardingRequestDO::getProjectName, projectName)
                .eq(DccProductOnboardingRequestDO::getProjectCode, projectCode)
                .eq(DccProductOnboardingRequestDO::getStatus,
                        DccProductOnboardingStatusConstants.PENDING_APPROVAL));
    }
}
