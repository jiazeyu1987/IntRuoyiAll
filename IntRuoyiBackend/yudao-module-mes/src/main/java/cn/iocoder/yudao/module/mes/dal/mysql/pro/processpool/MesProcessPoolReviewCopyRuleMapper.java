package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolReviewCopyRuleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProcessPoolReviewCopyRuleMapper extends BaseMapperX<MesProcessPoolReviewCopyRuleDO> {

    default List<MesProcessPoolReviewCopyRuleDO> selectEnabledListByContext(Long processId,
                                                                            Long deviceId,
                                                                            String templateType) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolReviewCopyRuleDO>()
                .eq(MesProcessPoolReviewCopyRuleDO::getProcessId, processId)
                .eq(MesProcessPoolReviewCopyRuleDO::getDeviceId, deviceId)
                .eq(MesProcessPoolReviewCopyRuleDO::getTemplateType, templateType)
                .eq(MesProcessPoolReviewCopyRuleDO::getEnabled, Boolean.TRUE)
                .orderByAsc(MesProcessPoolReviewCopyRuleDO::getId));
    }
}
