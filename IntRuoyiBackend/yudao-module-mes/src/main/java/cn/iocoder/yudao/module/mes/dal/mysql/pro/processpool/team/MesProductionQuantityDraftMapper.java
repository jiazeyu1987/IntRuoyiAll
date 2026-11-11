package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProductionQuantityDraftDO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface MesProductionQuantityDraftMapper extends BaseMapperX<MesProductionQuantityDraftDO> {
    default MesProductionQuantityDraftDO lock(Long id) {
        return selectOne(new LambdaQueryWrapperX<MesProductionQuantityDraftDO>()
                .eq(MesProductionQuantityDraftDO::getId, id).last("FOR UPDATE"));
    }
    default MesProductionQuantityDraftDO findRequest(Long operator, String keyHash) {
        return selectOne(MesProductionQuantityDraftDO::getOperatorUserId, operator,
                MesProductionQuantityDraftDO::getSubmitKeyHash, keyHash);
    }
    default List<MesProductionQuantityDraftDO> own(Long operator) {
        return selectList(new LambdaQueryWrapperX<MesProductionQuantityDraftDO>()
                .eq(MesProductionQuantityDraftDO::getOperatorUserId, operator)
                .in(MesProductionQuantityDraftDO::getStatus, List.of("BLOCKED", "READY"))
                .orderByDesc(MesProductionQuantityDraftDO::getId));
    }
    default List<MesProductionQuantityDraftDO> leader(Long leader) {
        return selectList(new LambdaQueryWrapperX<MesProductionQuantityDraftDO>()
                .eq(MesProductionQuantityDraftDO::getLeaderUserId, leader)
                .in(MesProductionQuantityDraftDO::getStatus, List.of("BLOCKED", "READY"))
                .orderByDesc(MesProductionQuantityDraftDO::getId));
    }
}
