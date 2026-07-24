package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.assignment.DccProjectCodeAssignmentAuditPageReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMetadataChangeItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface DccControlledFileMetadataChangeItemMapper
        extends BaseMapperX<DccControlledFileMetadataChangeItemDO> {

    default PageResult<DccControlledFileMetadataChangeItemDO> selectPage(
            DccProjectCodeAssignmentAuditPageReqVO reqVO, Collection<Long> sourceFilteredChangeIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DccControlledFileMetadataChangeItemDO>()
                .eqIfPresent(DccControlledFileMetadataChangeItemDO::getProjectCodeId, reqVO.getProjectCodeId())
                .eqIfPresent(DccControlledFileMetadataChangeItemDO::getAssignmentId, reqVO.getAssignmentId())
                .eqIfPresent(DccControlledFileMetadataChangeItemDO::getControlledFileId, reqVO.getControlledFileId())
                .eqIfPresent(DccControlledFileMetadataChangeItemDO::getOperatorUserId, reqVO.getOperatorUserId())
                .eqIfPresent(DccControlledFileMetadataChangeItemDO::getFieldName, reqVO.getFieldName())
                .inIfPresent(DccControlledFileMetadataChangeItemDO::getChangeId, sourceFilteredChangeIds)
                .betweenIfPresent(DccControlledFileMetadataChangeItemDO::getChangedTime, reqVO.getChangedTime())
                .orderByDesc(DccControlledFileMetadataChangeItemDO::getChangedTime)
                .orderByDesc(DccControlledFileMetadataChangeItemDO::getId));
    }

    default List<DccControlledFileMetadataChangeItemDO> selectListByChangeId(Long changeId) {
        return selectList(new LambdaQueryWrapperX<DccControlledFileMetadataChangeItemDO>()
                .eq(DccControlledFileMetadataChangeItemDO::getChangeId, changeId)
                .orderByAsc(DccControlledFileMetadataChangeItemDO::getId));
    }

}
