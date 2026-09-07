package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileCheckoutDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DccControlledFileCheckoutMapper extends BaseMapperX<DccControlledFileCheckoutDO> {

    @Select("""
            SELECT *
            FROM dcc_controlled_file_checkout
            WHERE tenant_id = #{tenantId}
              AND master_id = #{masterId}
              AND status = 'ACTIVE'
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 1
            FOR UPDATE
            """)
    DccControlledFileCheckoutDO selectActiveByMasterId(@Param("tenantId") Long tenantId,
                                                       @Param("masterId") Long masterId);

    @Select("""
            SELECT *
            FROM dcc_controlled_file_checkout
            WHERE tenant_id = #{tenantId}
              AND base_iteration_id = #{baseIterationId}
              AND deleted = 0
            ORDER BY id DESC
            LIMIT 1
            """)
    DccControlledFileCheckoutDO selectLatestByBaseIterationId(@Param("tenantId") Long tenantId,
                                                              @Param("baseIterationId") Long baseIterationId);

    @Update("""
            UPDATE dcc_controlled_file_checkout
               SET status = 'CHECKED_IN',
                   checkin_upload_ticket = #{uploadTicket},
                   checkin_iteration_id = #{iterationId},
                   checkin_source_file_id = #{sourceFileId},
                   checkin_source_sha256 = #{sourceSha256},
                   checked_in_time = CURRENT_TIMESTAMP,
                   updater = #{actorId},
                   update_time = CURRENT_TIMESTAMP
             WHERE tenant_id = #{tenantId}
               AND id = #{checkoutId}
               AND actor_id = #{actorId}
               AND status = 'ACTIVE'
               AND deleted = 0
            """)
    int markCheckedIn(@Param("tenantId") Long tenantId,
                      @Param("checkoutId") Long checkoutId,
                      @Param("actorId") Long actorId,
                      @Param("uploadTicket") String uploadTicket,
                      @Param("iterationId") Long iterationId,
                      @Param("sourceFileId") Long sourceFileId,
                      @Param("sourceSha256") String sourceSha256);

    @Update("""
            UPDATE dcc_controlled_file_checkout
               SET status = 'CANCELLED',
                   cancel_reason = #{reason},
                   cancelled_time = CURRENT_TIMESTAMP,
                   updater = #{actorId},
                   update_time = CURRENT_TIMESTAMP
             WHERE tenant_id = #{tenantId}
               AND id = #{checkoutId}
               AND actor_id = #{actorId}
               AND status = 'ACTIVE'
               AND deleted = 0
            """)
    int markCancelled(@Param("tenantId") Long tenantId,
                      @Param("checkoutId") Long checkoutId,
                      @Param("actorId") Long actorId,
                      @Param("reason") String reason);
}
