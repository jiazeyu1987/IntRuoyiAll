package cn.iocoder.yudao.module.mes.dal.mysql.dv.repair;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.dv.repair.vo.MesDvRepairPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.repair.MesDvRepairDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * MES 维修工单 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface MesDvRepairMapper extends BaseMapperX<MesDvRepairDO> {

    java.util.Map<String, ListFilterQueryUtils.Field> LIST_FILTER_FIELDS = java.util.Map.ofEntries(
            java.util.Map.entry("id", ListFilterQueryUtils.number("id")),
            java.util.Map.entry("code", ListFilterQueryUtils.text("code")),
            java.util.Map.entry("name", ListFilterQueryUtils.text("name")),
            java.util.Map.entry("machineryId", ListFilterQueryUtils.number("machinery_id")),
            java.util.Map.entry("requireDate", ListFilterQueryUtils.dateTime("require_date")),
            java.util.Map.entry("finishDate", ListFilterQueryUtils.dateTime("finish_date")),
            java.util.Map.entry("confirmDate", ListFilterQueryUtils.dateTime("confirm_date")),
            java.util.Map.entry("result", ListFilterQueryUtils.number("result")),
            java.util.Map.entry("acceptedUserId", ListFilterQueryUtils.number("accepted_user_id")),
            java.util.Map.entry("confirmUserId", ListFilterQueryUtils.number("confirm_user_id")),
            java.util.Map.entry("sourceDocType", ListFilterQueryUtils.number("source_doc_type")),
            java.util.Map.entry("sourceDocId", ListFilterQueryUtils.number("source_doc_id")),
            java.util.Map.entry("sourceDocCode", ListFilterQueryUtils.text("source_doc_code")),
            java.util.Map.entry("status", ListFilterQueryUtils.number("status")),
            java.util.Map.entry("remark", ListFilterQueryUtils.text("remark")),
            java.util.Map.entry("createTime", ListFilterQueryUtils.dateTime("create_time")),
            java.util.Map.entry("creator", ListFilterQueryUtils.text("creator")),
            java.util.Map.entry("machineryCode", ListFilterQueryUtils.text("(SELECT x.code FROM mes_dv_machinery x WHERE x.id=mes_dv_repair.machinery_id AND x.tenant_id=mes_dv_repair.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("machineryName", ListFilterQueryUtils.text("(SELECT x.name FROM mes_dv_machinery x WHERE x.id=mes_dv_repair.machinery_id AND x.tenant_id=mes_dv_repair.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("acceptedUserNickname", ListFilterQueryUtils.text("(SELECT x.nickname FROM system_users x WHERE x.id=mes_dv_repair.accepted_user_id AND x.tenant_id=mes_dv_repair.tenant_id AND x.deleted=0)")),
            java.util.Map.entry("confirmUserNickname", ListFilterQueryUtils.text("(SELECT x.nickname FROM system_users x WHERE x.id=mes_dv_repair.confirm_user_id AND x.tenant_id=mes_dv_repair.tenant_id AND x.deleted=0)"))
    );



    default PageResult<MesDvRepairDO> selectPage(MesDvRepairPageReqVO reqVO) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, new LambdaQueryWrapperX<MesDvRepairDO>()
                .likeIfPresent(MesDvRepairDO::getCode, reqVO.getCode())
                .likeIfPresent(MesDvRepairDO::getName, reqVO.getName())
                .eqIfPresent(MesDvRepairDO::getMachineryId, reqVO.getMachineryId())
                .eqIfPresent(MesDvRepairDO::getResult, reqVO.getResult())
                .eqIfPresent(MesDvRepairDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesDvRepairDO::getRequireDate, reqVO.getRequireDate())
                .orderByDesc(MesDvRepairDO::getId), LIST_FILTER_FIELDS));
    }

    default MesDvRepairDO selectByCode(String code) {
        return selectOne(MesDvRepairDO::getCode, code);
    }

    default Long selectCountByMachineryId(Long machineryId) {
        return selectCount(MesDvRepairDO::getMachineryId, machineryId);
    }

    default List<MesDvRepairDO> selectListByMachineryIdsAndStatuses(Collection<Long> machineryIds,
                                                                    Collection<Integer> statuses) {
        if (machineryIds == null || machineryIds.isEmpty() || statuses == null || statuses.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesDvRepairDO>()
                .in(MesDvRepairDO::getMachineryId, machineryIds)
                .in(MesDvRepairDO::getStatus, statuses)
                .orderByDesc(MesDvRepairDO::getRequireDate)
                .orderByDesc(MesDvRepairDO::getId));
    }

}
