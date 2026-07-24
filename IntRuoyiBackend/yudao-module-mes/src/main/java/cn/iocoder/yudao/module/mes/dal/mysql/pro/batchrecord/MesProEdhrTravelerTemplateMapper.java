package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrTravelerTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrTravelerTemplateDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrTravelerTemplateMapper extends BaseMapperX<MesProEdhrTravelerTemplateDO> {

    String STATUS_ACTIVE = "ACTIVE";

    default PageResult<MesProEdhrTravelerTemplateDO> selectPage(MesProEdhrTravelerTemplatePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrTravelerTemplateDO>()
                .likeIfPresent(MesProEdhrTravelerTemplateDO::getTemplateCode, reqVO.getTemplateCode())
                .likeIfPresent(MesProEdhrTravelerTemplateDO::getTemplateName, reqVO.getTemplateName())
                .eqIfPresent(MesProEdhrTravelerTemplateDO::getStatus, reqVO.getStatus())
                .likeIfPresent(MesProEdhrTravelerTemplateDO::getApplicableProductCode, reqVO.getApplicableProductCode())
                .eqIfPresent(MesProEdhrTravelerTemplateDO::getApplicableRouteId, reqVO.getApplicableRouteId())
                .eqIfPresent(MesProEdhrTravelerTemplateDO::getApplicableProcessId, reqVO.getApplicableProcessId())
                .betweenIfPresent(MesProEdhrTravelerTemplateDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesProEdhrTravelerTemplateDO::getId));
    }

    default MesProEdhrTravelerTemplateDO selectByTemplateCode(String templateCode) {
        return selectOne(MesProEdhrTravelerTemplateDO::getTemplateCode, templateCode);
    }

    default List<MesProEdhrTravelerTemplateDO> selectActiveTemplatesByProductAndRoute(String applicableProductCode,
                                                                                      Long applicableRouteId) {
        LambdaQueryWrapper<MesProEdhrTravelerTemplateDO> query = new LambdaQueryWrapper<>();
        query.eq(MesProEdhrTravelerTemplateDO::getStatus, STATUS_ACTIVE);
        if (StrUtil.isBlank(applicableProductCode)) {
            query.isNull(MesProEdhrTravelerTemplateDO::getApplicableProductCode);
        } else {
            query.eq(MesProEdhrTravelerTemplateDO::getApplicableProductCode, applicableProductCode);
        }
        if (applicableRouteId == null) {
            query.isNull(MesProEdhrTravelerTemplateDO::getApplicableRouteId);
        } else {
            query.eq(MesProEdhrTravelerTemplateDO::getApplicableRouteId, applicableRouteId);
        }
        query.orderByDesc(MesProEdhrTravelerTemplateDO::getActiveAt).orderByDesc(MesProEdhrTravelerTemplateDO::getId);
        return selectList(query);
    }
}
