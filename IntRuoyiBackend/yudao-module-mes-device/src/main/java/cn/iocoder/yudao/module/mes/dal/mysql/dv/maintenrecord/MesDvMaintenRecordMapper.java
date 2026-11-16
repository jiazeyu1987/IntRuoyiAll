package cn.iocoder.yudao.module.mes.dal.mysql.dv.maintenrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.dv.maintenrecord.vo.MesDvMaintenRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.maintenrecord.MesDvMaintenRecordDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 设备保养记录 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface MesDvMaintenRecordMapper extends BaseMapperX<MesDvMaintenRecordDO> {

    java.util.Map<String, ListFilterQueryUtils.Field> LIST_FILTER_FIELDS = java.util.Map.ofEntries(
            java.util.Map.entry("id", ListFilterQueryUtils.number("id")),
            java.util.Map.entry("planId", ListFilterQueryUtils.number("plan_id")),
            java.util.Map.entry("machineryId", ListFilterQueryUtils.number("machinery_id")),
            java.util.Map.entry("maintenTime", ListFilterQueryUtils.dateTime("mainten_time")),
            java.util.Map.entry("userId", ListFilterQueryUtils.number("user_id")),
            java.util.Map.entry("status", ListFilterQueryUtils.number("status")),
            java.util.Map.entry("remark", ListFilterQueryUtils.text("remark")),
            java.util.Map.entry("createTime", ListFilterQueryUtils.dateTime("create_time")),
            java.util.Map.entry("creator", ListFilterQueryUtils.text("creator")),
            java.util.Map.entry("machineryCode", ListFilterQueryUtils.text("(SELECT x.code FROM mes_dv_machinery x WHERE x.id=mes_dv_mainten_record.machinery_id AND x.tenant_id=mes_dv_mainten_record.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("machineryName", ListFilterQueryUtils.text("(SELECT x.name FROM mes_dv_machinery x WHERE x.id=mes_dv_mainten_record.machinery_id AND x.tenant_id=mes_dv_mainten_record.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("machineryBrand", ListFilterQueryUtils.text("(SELECT x.brand FROM mes_dv_machinery x WHERE x.id=mes_dv_mainten_record.machinery_id AND x.tenant_id=mes_dv_mainten_record.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("machinerySpecification", ListFilterQueryUtils.text("(SELECT x.specification FROM mes_dv_machinery x WHERE x.id=mes_dv_mainten_record.machinery_id AND x.tenant_id=mes_dv_mainten_record.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("planCode", ListFilterQueryUtils.text("(SELECT x.code FROM mes_dv_check_plan x WHERE x.id=mes_dv_mainten_record.plan_id AND x.tenant_id=mes_dv_mainten_record.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("planName", ListFilterQueryUtils.text("(SELECT x.name FROM mes_dv_check_plan x WHERE x.id=mes_dv_mainten_record.plan_id AND x.tenant_id=mes_dv_mainten_record.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("nickname", ListFilterQueryUtils.text("(SELECT x.nickname FROM system_users x WHERE x.id=mes_dv_mainten_record.user_id AND x.tenant_id=mes_dv_mainten_record.tenant_id AND x.deleted=0)"))
    );



    default PageResult<MesDvMaintenRecordDO> selectPage(MesDvMaintenRecordPageReqVO reqVO) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, new LambdaQueryWrapperX<MesDvMaintenRecordDO>()
                .eqIfPresent(MesDvMaintenRecordDO::getPlanId, reqVO.getPlanId())
                .eqIfPresent(MesDvMaintenRecordDO::getMachineryId, reqVO.getMachineryId())
                .eqIfPresent(MesDvMaintenRecordDO::getUserId, reqVO.getUserId())
                .betweenIfPresent(MesDvMaintenRecordDO::getMaintenTime, reqVO.getMaintenTime())
                .orderByDesc(MesDvMaintenRecordDO::getId), LIST_FILTER_FIELDS));
    }

    default Long selectCountByMachineryId(Long machineryId) {
        return selectCount(MesDvMaintenRecordDO::getMachineryId, machineryId);
    }

}
