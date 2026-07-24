package cn.iocoder.yudao.module.mes.dal.mysql.pro.workrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workrecord.MesProWorkRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * MES 当前绑定状态（快照） Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface MesProWorkRecordMapper extends BaseMapperX<MesProWorkRecordDO> {

    default MesProWorkRecordDO selectByUserId(Long userId) {
        return selectOne(new LambdaQueryWrapperX<MesProWorkRecordDO>()
                .eq(MesProWorkRecordDO::getUserId, userId));
    }

    default List<MesProWorkRecordDO> selectListByWorkstationIdsAndType(Collection<Long> workstationIds, Integer type) {
        if (workstationIds == null || workstationIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProWorkRecordDO>()
                .in(MesProWorkRecordDO::getWorkstationId, workstationIds)
                .eq(MesProWorkRecordDO::getType, type));
    }

}
