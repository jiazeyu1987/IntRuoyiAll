package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionRecipientDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DccControlledFileDistributionRecipientMapper extends BaseMapperX<DccControlledFileDistributionRecipientDO> {

    default List<DccControlledFileDistributionRecipientDO> selectListByDistributionId(Long distributionId) {
        return selectList(DccControlledFileDistributionRecipientDO::getDistributionId, distributionId);
    }

    default List<DccControlledFileDistributionRecipientDO> selectListByUserId(Long userId) {
        return selectList(DccControlledFileDistributionRecipientDO::getUserId, userId);
    }

    @Select("""
            SELECT COUNT(1)
            FROM dcc_controlled_file_distribution_recipient recipient
            INNER JOIN dcc_controlled_file_distribution distribution
                    ON distribution.id = recipient.distribution_id
            WHERE recipient.tenant_id = #{tenantId}
              AND distribution.tenant_id = #{tenantId}
              AND distribution.controlled_file_id = #{controlledFileId}
              AND recipient.user_id = #{userId}
              AND distribution.distribution_medium = 'PUBLIC_FOLDER'
              AND distribution.status IN ('PENDING', 'SENT', 'READ', 'ACKNOWLEDGED')
              AND recipient.deleted = 0
              AND distribution.deleted = 0
            """)
    long countActiveElectronicRecipientAccess(@Param("tenantId") Long tenantId,
                                              @Param("controlledFileId") Long controlledFileId,
                                              @Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT distribution.controlled_file_id
            FROM dcc_controlled_file_distribution_recipient recipient
            INNER JOIN dcc_controlled_file_distribution distribution
                    ON distribution.id = recipient.distribution_id
            WHERE recipient.tenant_id = #{tenantId}
              AND distribution.tenant_id = #{tenantId}
              AND recipient.user_id = #{userId}
              AND distribution.distribution_medium = 'PUBLIC_FOLDER'
              AND distribution.status IN ('PENDING', 'SENT', 'READ', 'ACKNOWLEDGED')
              AND recipient.deleted = 0
              AND distribution.deleted = 0
            ORDER BY distribution.controlled_file_id
            """)
    List<Long> selectActiveElectronicControlledFileIdsByUserId(@Param("tenantId") Long tenantId,
                                                                @Param("userId") Long userId);
}
