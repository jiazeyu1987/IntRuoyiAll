package cn.iocoder.yudao.module.mes.dal.mysql.pro.frontline;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.frontline.MesFrontlineEmployeeTemplateBindingDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesFrontlineEmployeeTemplateBindingMapper
        extends BaseMapperX<MesFrontlineEmployeeTemplateBindingDO> {

    default MesFrontlineEmployeeTemplateBindingDO selectEnabledByContext(Long actualEmployeeId, Long routeProcessId,
                                                                        Long processId) {
        return selectOne(new LambdaQueryWrapperX<MesFrontlineEmployeeTemplateBindingDO>()
                .eq(MesFrontlineEmployeeTemplateBindingDO::getActualEmployeeId, actualEmployeeId)
                .eq(MesFrontlineEmployeeTemplateBindingDO::getRouteProcessId, routeProcessId)
                .eq(MesFrontlineEmployeeTemplateBindingDO::getProcessId, processId)
                .eq(MesFrontlineEmployeeTemplateBindingDO::getStatus, CommonStatusEnum.ENABLE.getStatus()));
    }
}
