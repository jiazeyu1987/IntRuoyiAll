package cn.iocoder.yudao.module.bpm.dal.mysql.formcenter;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.QueryWrapperX;
import cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionSnapshotDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FormActionSnapshotMapper extends BaseMapperX<FormActionSnapshotDO> {

    default List<FormActionSnapshotDO> selectByInstanceId(Long tenantId, Long instanceId) {
        return selectList(new QueryWrapperX<FormActionSnapshotDO>()
                .eq("tenant_id", tenantId)
                .eq("instance_id", instanceId)
                .orderByAsc("snapshot_version"));
    }

    default Long selectCountByInstanceId(Long tenantId, Long instanceId) {
        return selectCount(new QueryWrapperX<FormActionSnapshotDO>()
                .eq("tenant_id", tenantId)
                .eq("instance_id", instanceId));
    }

}
