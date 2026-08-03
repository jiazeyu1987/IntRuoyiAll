package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProductOnboardingRequestDO;
import cn.iocoder.yudao.module.dcc.enums.DccProductOnboardingStatusConstants;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DccProductOnboardingRequestMapper extends BaseMapperX<DccProductOnboardingRequestDO> {

    default DccProductOnboardingRequestDO selectPendingByProjectNameAndProjectCode(String projectName,
                                                                                   String projectCode) {
        return selectOne(new LambdaQueryWrapperX<DccProductOnboardingRequestDO>()
                .eq(DccProductOnboardingRequestDO::getProjectName, projectName)
                .eq(DccProductOnboardingRequestDO::getProjectCode, projectCode)
                .eq(DccProductOnboardingRequestDO::getStatus,
                        DccProductOnboardingStatusConstants.PENDING_APPROVAL));
    }
}
