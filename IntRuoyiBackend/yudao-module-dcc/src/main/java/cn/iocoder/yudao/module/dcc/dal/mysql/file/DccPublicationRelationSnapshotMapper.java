package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationRelationSnapshotDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DccPublicationRelationSnapshotMapper extends BaseMapperX<DccPublicationRelationSnapshotDO> {

    default List<DccPublicationRelationSnapshotDO> selectListByBatchId(Long batchId) {
        return selectList(DccPublicationRelationSnapshotDO::getBatchId, batchId);
    }
}
