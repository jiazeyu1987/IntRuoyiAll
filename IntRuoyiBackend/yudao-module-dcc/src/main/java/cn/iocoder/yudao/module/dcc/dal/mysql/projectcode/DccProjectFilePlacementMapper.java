package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFilePlacementDO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
@Mapper
public interface DccProjectFilePlacementMapper extends BaseMapperX<DccProjectFilePlacementDO> {
    default DccProjectFilePlacementDO findFile(Long fileId) {
        return selectOne(new LambdaQueryWrapperX<DccProjectFilePlacementDO>()
                .eq(DccProjectFilePlacementDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(DccProjectFilePlacementDO::getControlledFileId, fileId));
    }
    default List<DccProjectFilePlacementDO> listFolder(Long projectId, Long folderId) {
        return selectList(new LambdaQueryWrapperX<DccProjectFilePlacementDO>()
                .eq(DccProjectFilePlacementDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(DccProjectFilePlacementDO::getProjectCodeId, projectId)
                .eq(DccProjectFilePlacementDO::getProjectFolderId, folderId)
                .orderByAsc(DccProjectFilePlacementDO::getControlledFileId));
    }
}
