package cn.iocoder.yudao.module.dcc.dal.mysql.permission;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclDirectorySnapshotDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DccNasAclDirectorySnapshotMapper extends BaseMapperX<DccNasAclDirectorySnapshotDO> {

    default DccNasAclDirectorySnapshotDO selectBySnapshotIdAndPathHash(Long snapshotId, String pathHash) {
        return selectOne(new LambdaQueryWrapperX<DccNasAclDirectorySnapshotDO>()
                .eq(DccNasAclDirectorySnapshotDO::getSnapshotId, snapshotId)
                .eq(DccNasAclDirectorySnapshotDO::getPathHash, pathHash));
    }

    default List<DccNasAclDirectorySnapshotDO> selectListBySnapshotId(Long snapshotId) {
        return selectList(new LambdaQueryWrapperX<DccNasAclDirectorySnapshotDO>()
                .eq(DccNasAclDirectorySnapshotDO::getSnapshotId, snapshotId)
                .orderByAsc(DccNasAclDirectorySnapshotDO::getId));
    }
}
