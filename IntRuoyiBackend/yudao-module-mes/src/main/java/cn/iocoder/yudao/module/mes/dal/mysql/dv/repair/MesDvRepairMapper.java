package cn.iocoder.yudao.module.mes.dal.mysql.dv.repair;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
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

    default PageResult<MesDvRepairDO> selectPage(MesDvRepairPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesDvRepairDO>()
                .likeIfPresent(MesDvRepairDO::getCode, reqVO.getCode())
                .likeIfPresent(MesDvRepairDO::getName, reqVO.getName())
                .eqIfPresent(MesDvRepairDO::getMachineryId, reqVO.getMachineryId())
                .eqIfPresent(MesDvRepairDO::getResult, reqVO.getResult())
                .eqIfPresent(MesDvRepairDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesDvRepairDO::getRequireDate, reqVO.getRequireDate())
                .orderByDesc(MesDvRepairDO::getId));
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
