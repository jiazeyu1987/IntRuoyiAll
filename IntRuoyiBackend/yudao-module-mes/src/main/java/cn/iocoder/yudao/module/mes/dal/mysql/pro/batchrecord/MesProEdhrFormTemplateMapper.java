package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFormTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrFormTemplateDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrFormTemplateMapper extends BaseMapperX<MesProEdhrFormTemplateDO> {

    String STATUS_ACTIVE = "ACTIVE";

    default PageResult<MesProEdhrFormTemplateDO> selectPage(MesProEdhrFormTemplatePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrFormTemplateDO>()
                .likeIfPresent(MesProEdhrFormTemplateDO::getTemplateCode, reqVO.getTemplateCode())
                .likeIfPresent(MesProEdhrFormTemplateDO::getTemplateName, reqVO.getTemplateName())
                .eqIfPresent(MesProEdhrFormTemplateDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesProEdhrFormTemplateDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesProEdhrFormTemplateDO::getId));
    }

    default MesProEdhrFormTemplateDO selectByTemplateCode(String templateCode) {
        return selectOne(MesProEdhrFormTemplateDO::getTemplateCode, templateCode);
    }

    default MesProEdhrFormTemplateDO selectActiveByTemplateCode(String templateCode) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrFormTemplateDO>()
                .eq(MesProEdhrFormTemplateDO::getTemplateCode, templateCode)
                .eq(MesProEdhrFormTemplateDO::getStatus, STATUS_ACTIVE)
                .orderByDesc(MesProEdhrFormTemplateDO::getActiveAt)
                .orderByDesc(MesProEdhrFormTemplateDO::getId));
    }
}
