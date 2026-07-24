package cn.iocoder.yudao.module.dcc.dal.mysql.permission;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclSnapshotDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DccNasAclSnapshotMapper extends BaseMapperX<DccNasAclSnapshotDO> {

    default DccNasAclSnapshotDO selectBySnapshotKey(String snapshotKey) {
        return selectOne(new LambdaQueryWrapperX<DccNasAclSnapshotDO>()
                .eq(DccNasAclSnapshotDO::getSnapshotKey, snapshotKey));
    }
}
