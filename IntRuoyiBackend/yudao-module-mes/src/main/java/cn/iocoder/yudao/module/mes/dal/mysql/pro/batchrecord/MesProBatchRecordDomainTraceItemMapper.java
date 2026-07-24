package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordDomainTraceItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MesProBatchRecordDomainTraceItemMapper extends BaseMapperX<MesProBatchRecordDomainTraceItemDO> {

    default List<MesProBatchRecordDomainTraceItemDO> selectListBySnapshotId(Long snapshotId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordDomainTraceItemDO>()
                .eq(MesProBatchRecordDomainTraceItemDO::getSnapshotId, snapshotId)
                .orderByAsc(MesProBatchRecordDomainTraceItemDO::getId));
    }

    default List<MesProBatchRecordDomainTraceItemDO> selectListBySnapshotIds(Collection<Long> snapshotIds) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordDomainTraceItemDO>()
                .in(MesProBatchRecordDomainTraceItemDO::getSnapshotId, snapshotIds)
                .orderByAsc(MesProBatchRecordDomainTraceItemDO::getSnapshotId)
                .orderByAsc(MesProBatchRecordDomainTraceItemDO::getId));
    }

}
