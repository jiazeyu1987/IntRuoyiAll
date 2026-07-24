package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateImpactPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDhrTemplateImpactDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrDhrTemplateImpactMapper extends BaseMapperX<MesProEdhrDhrTemplateImpactDO> {

    default PageResult<MesProEdhrDhrTemplateImpactDO> selectPage(MesProEdhrDhrTemplateImpactPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrDhrTemplateImpactDO>()
                .eqIfPresent(MesProEdhrDhrTemplateImpactDO::getTemplateId, reqVO.getTemplateId())
                .eqIfPresent(MesProEdhrDhrTemplateImpactDO::getActionType, reqVO.getActionType())
                .orderByDesc(MesProEdhrDhrTemplateImpactDO::getId));
    }
}
