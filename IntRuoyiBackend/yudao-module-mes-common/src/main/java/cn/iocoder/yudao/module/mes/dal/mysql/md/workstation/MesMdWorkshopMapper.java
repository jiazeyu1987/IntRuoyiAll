package cn.iocoder.yudao.module.mes.dal.mysql.md.workstation;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.md.workstation.vo.workshop.MesMdWorkshopPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 车间 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface MesMdWorkshopMapper extends BaseMapperX<MesMdWorkshopDO> {

    java.util.Map<String, ListFilterQueryUtils.Field> LIST_FILTER_FIELDS = java.util.Map.ofEntries(
            java.util.Map.entry("id", ListFilterQueryUtils.number("id")),
            java.util.Map.entry("code", ListFilterQueryUtils.text("code")),
            java.util.Map.entry("name", ListFilterQueryUtils.text("name")),
            java.util.Map.entry("area", ListFilterQueryUtils.number("area")),
            java.util.Map.entry("chargeUserId", ListFilterQueryUtils.number("charge_user_id")),
            java.util.Map.entry("status", ListFilterQueryUtils.number("status")),
            java.util.Map.entry("remark", ListFilterQueryUtils.text("remark")),
            java.util.Map.entry("createTime", ListFilterQueryUtils.dateTime("create_time")),
            java.util.Map.entry("creator", ListFilterQueryUtils.text("creator")),
            java.util.Map.entry("chargeUserName", ListFilterQueryUtils.text("(SELECT x.nickname FROM system_users x WHERE x.id=mes_md_workshop.charge_user_id AND x.tenant_id=mes_md_workshop.tenant_id AND x.deleted=0)"))
    );



    default PageResult<MesMdWorkshopDO> selectPage(MesMdWorkshopPageReqVO reqVO) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, new LambdaQueryWrapperX<MesMdWorkshopDO>()
                .eqIfPresent(MesMdWorkshopDO::getCode, reqVO.getCode())
                .likeIfPresent(MesMdWorkshopDO::getName, reqVO.getName())
                .eqIfPresent(MesMdWorkshopDO::getStatus, reqVO.getStatus())
                .orderByDesc(MesMdWorkshopDO::getId), LIST_FILTER_FIELDS));
    }

    default MesMdWorkshopDO selectByCode(String code) {
        return selectOne(MesMdWorkshopDO::getCode, code);
    }

    default MesMdWorkshopDO selectByName(String name) {
        return selectOne(MesMdWorkshopDO::getName, name);
    }

    default List<MesMdWorkshopDO> selectListByStatus(Integer status) {
        return selectList(MesMdWorkshopDO::getStatus, status);
    }

}
