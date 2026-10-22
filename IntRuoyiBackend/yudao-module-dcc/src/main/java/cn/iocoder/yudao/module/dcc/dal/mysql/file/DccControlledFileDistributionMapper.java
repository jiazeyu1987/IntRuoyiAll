package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DccControlledFileDistributionMapper extends BaseMapperX<DccControlledFileDistributionDO> {

    @Select("SELECT * FROM dcc_controlled_file_distribution WHERE tenant_id=#{tenantId} AND id=#{id} AND deleted=0 FOR UPDATE")
    DccControlledFileDistributionDO selectByIdAndTenantForUpdate(@Param("tenantId") Long tenantId, @Param("id") Long id);

    @Select("SELECT * FROM dcc_controlled_file_distribution WHERE tenant_id=#{tenantId} AND controlled_file_id=#{fileId} AND deleted=0 ORDER BY id FOR UPDATE")
    List<DccControlledFileDistributionDO> selectListByControlledFileIdForUpdate(@Param("tenantId") Long tenantId, @Param("fileId") Long fileId);

    default List<DccControlledFileDistributionDO> selectListByControlledFileId(Long controlledFileId) {
        return selectList(DccControlledFileDistributionDO::getControlledFileId, controlledFileId);
    }
}
