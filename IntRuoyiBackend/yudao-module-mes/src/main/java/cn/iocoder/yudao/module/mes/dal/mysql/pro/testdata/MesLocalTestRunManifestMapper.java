package cn.iocoder.yudao.module.mes.dal.mysql.pro.testdata;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.testdata.MesLocalTestRunManifestDO;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface MesLocalTestRunManifestMapper extends BaseMapperX<MesLocalTestRunManifestDO> {
  default MesLocalTestRunManifestDO selectByWorkOrderId(Long id) {
    return selectOne(
        new LambdaQueryWrapperX<MesLocalTestRunManifestDO>()
            .eq(MesLocalTestRunManifestDO::getTenantId, TenantContextHolder.getRequiredTenantId())
            .eq(MesLocalTestRunManifestDO::getWorkOrderId, id));
  }

  default List<MesLocalTestRunManifestDO> selectByRunId(String runId) {
    return selectList(
        new LambdaQueryWrapperX<MesLocalTestRunManifestDO>()
            .eq(MesLocalTestRunManifestDO::getTenantId, TenantContextHolder.getRequiredTenantId())
            .eq(MesLocalTestRunManifestDO::getRunId, runId)
            .orderByAsc(MesLocalTestRunManifestDO::getSlot));
  }

  @Select("SELECT id FROM system_tenant WHERE id=#{tenantId} AND deleted=0 FOR UPDATE")
  Long lockTenant(@Param("tenantId") Long tenantId);
}
