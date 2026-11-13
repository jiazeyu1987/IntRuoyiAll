package cn.iocoder.yudao.module.mes.dal.mysql.dv.checkrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.dv.checkrecord.vo.MesDvCheckRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.checkrecord.MesDvCheckRecordDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 设备点检记录 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface MesDvCheckRecordMapper extends BaseMapperX<MesDvCheckRecordDO> {

    java.util.Map<String, ListFilterQueryUtils.Field> LIST_FILTER_FIELDS = java.util.Map.ofEntries(
            java.util.Map.entry("id", ListFilterQueryUtils.number("id")),
            java.util.Map.entry("planId", ListFilterQueryUtils.number("plan_id")),
            java.util.Map.entry("machineryId", ListFilterQueryUtils.number("machinery_id")),
            java.util.Map.entry("checkTime", ListFilterQueryUtils.dateTime("check_time")),
            java.util.Map.entry("userId", ListFilterQueryUtils.number("user_id")),
            java.util.Map.entry("status", ListFilterQueryUtils.number("status")),
            java.util.Map.entry("remark", ListFilterQueryUtils.text("remark")),
            java.util.Map.entry("createTime", ListFilterQueryUtils.dateTime("create_time")),
            java.util.Map.entry("creator", ListFilterQueryUtils.text("creator")),
            java.util.Map.entry("machineryCode", ListFilterQueryUtils.text("(SELECT x.code FROM mes_dv_machinery x WHERE x.id=mes_dv_check_record.machinery_id AND x.tenant_id=mes_dv_check_record.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("machineryName", ListFilterQueryUtils.text("(SELECT x.name FROM mes_dv_machinery x WHERE x.id=mes_dv_check_record.machinery_id AND x.tenant_id=mes_dv_check_record.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("machineryBrand", ListFilterQueryUtils.text("(SELECT x.brand FROM mes_dv_machinery x WHERE x.id=mes_dv_check_record.machinery_id AND x.tenant_id=mes_dv_check_record.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("machinerySpecification", ListFilterQueryUtils.text("(SELECT x.specification FROM mes_dv_machinery x WHERE x.id=mes_dv_check_record.machinery_id AND x.tenant_id=mes_dv_check_record.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("planCode", ListFilterQueryUtils.text("(SELECT x.code FROM mes_dv_check_plan x WHERE x.id=mes_dv_check_record.plan_id AND x.tenant_id=mes_dv_check_record.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("planName", ListFilterQueryUtils.text("(SELECT x.name FROM mes_dv_check_plan x WHERE x.id=mes_dv_check_record.plan_id AND x.tenant_id=mes_dv_check_record.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("nickname", ListFilterQueryUtils.text("(SELECT x.nickname FROM system_users x WHERE x.id=mes_dv_check_record.user_id AND x.tenant_id=mes_dv_check_record.tenant_id AND x.deleted=0)"))
    );



    default PageResult<MesDvCheckRecordDO> selectPage(MesDvCheckRecordPageReqVO reqVO) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, new LambdaQueryWrapperX<MesDvCheckRecordDO>()
                .eqIfPresent(MesDvCheckRecordDO::getPlanId, reqVO.getPlanId())
                .eqIfPresent(MesDvCheckRecordDO::getMachineryId, reqVO.getMachineryId())
                .eqIfPresent(MesDvCheckRecordDO::getUserId, reqVO.getUserId())
                .eqIfPresent(MesDvCheckRecordDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesDvCheckRecordDO::getCheckTime, reqVO.getCheckTime())
                .orderByDesc(MesDvCheckRecordDO::getId), LIST_FILTER_FIELDS));
    }

    default Long selectCountByMachineryId(Long machineryId) {
        return selectCount(MesDvCheckRecordDO::getMachineryId, machineryId);
    }

}
