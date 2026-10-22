package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAttachmentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DccControlledFileAttachmentMapper extends BaseMapperX<DccControlledFileAttachmentDO> {

    @Select("""
            SELECT * FROM dcc_controlled_file_attachment
            WHERE tenant_id=#{tenantId} AND controlled_file_id=#{fileId}
              AND attachment_purpose='TRAINING_RECORD' AND application_process_instance_id=#{round}
              AND deleted=0 FOR UPDATE
            """)
    DccControlledFileAttachmentDO selectTrainingRecordForUpdate(@Param("tenantId") Long tenantId,
            @Param("fileId") Long fileId, @Param("round") String round);

    default List<DccControlledFileAttachmentDO> selectListByControlledFileId(Long controlledFileId) {
        return selectList(new LambdaQueryWrapperX<DccControlledFileAttachmentDO>()
                .eq(DccControlledFileAttachmentDO::getControlledFileId, controlledFileId)
                .orderByAsc(DccControlledFileAttachmentDO::getSortNo)
                .orderByAsc(DccControlledFileAttachmentDO::getId));
    }

}
