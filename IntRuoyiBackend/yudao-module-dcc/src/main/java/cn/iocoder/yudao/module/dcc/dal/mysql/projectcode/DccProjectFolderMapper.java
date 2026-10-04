package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFolderDO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
@Mapper
public interface DccProjectFolderMapper extends BaseMapperX<DccProjectFolderDO> {
    default List<DccProjectFolderDO> listByProject(Long projectId) {
        return selectList(new LambdaQueryWrapperX<DccProjectFolderDO>()
                .eq(DccProjectFolderDO::getTenantId, cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())
                .eq(DccProjectFolderDO::getProjectCodeId, projectId)
                .orderByAsc(DccProjectFolderDO::getSortOrder, DccProjectFolderDO::getId));
    }
}
