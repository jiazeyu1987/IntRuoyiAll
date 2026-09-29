package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationCreateRequestDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MesProEdhrDeviationCreateRequestMapper {

    @Insert("""
            INSERT INTO mes_pro_edhr_deviation_create_request
                (tenant_id, idempotency_key, payload_hash, deviation_id)
            VALUES (#{tenantId}, #{idempotencyKey}, #{payloadHash}, NULL)
            ON DUPLICATE KEY UPDATE idempotency_key = VALUES(idempotency_key)
            """)
    int reserveOrLock(@Param("tenantId") Long tenantId,
                      @Param("idempotencyKey") String idempotencyKey,
                      @Param("payloadHash") String payloadHash);

    @Select("""
            SELECT tenant_id, idempotency_key, payload_hash, deviation_id
            FROM mes_pro_edhr_deviation_create_request
            WHERE tenant_id = #{tenantId}
              AND idempotency_key = #{idempotencyKey}
            FOR UPDATE
            """)
    MesProEdhrDeviationCreateRequestDO selectForUpdate(@Param("tenantId") Long tenantId,
                                                        @Param("idempotencyKey") String idempotencyKey);

    @Update("""
            UPDATE mes_pro_edhr_deviation_create_request
            SET deviation_id = #{deviationId}
            WHERE tenant_id = #{tenantId}
              AND idempotency_key = #{idempotencyKey}
              AND payload_hash = #{payloadHash}
              AND deviation_id IS NULL
            """)
    int linkDeviation(@Param("tenantId") Long tenantId,
                      @Param("idempotencyKey") String idempotencyKey,
                      @Param("payloadHash") String payloadHash,
                      @Param("deviationId") Long deviationId);
}
