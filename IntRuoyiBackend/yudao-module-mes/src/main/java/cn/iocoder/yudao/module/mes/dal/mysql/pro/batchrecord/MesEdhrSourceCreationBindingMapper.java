package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesEdhrSourceCreationBindingDO;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** Explicit tenant, complete result sets. No mutable mapper operations are exposed. */
@Mapper
public interface MesEdhrSourceCreationBindingMapper {
    String COLUMNS = "id,tenant_id,owner_protocol,source_row_id,source_receipt_id,source_version,source_hash,"
            + "request_hash_version,request_hash,creation_event_id,creation_event_hash";

    @Select("SELECT " + COLUMNS + " FROM mes_edhr_source_creation_binding WHERE tenant_id=#{tenantId}"
            + " AND owner_protocol=#{protocol} AND source_row_id=#{rowId} ORDER BY id")
    List<MesEdhrSourceCreationBindingDO> selectBySourceRow(@Param("tenantId") Long tenantId,
            @Param("protocol") String protocol, @Param("rowId") Long rowId);

    @Select("SELECT " + COLUMNS + " FROM mes_edhr_source_creation_binding WHERE tenant_id=#{tenantId}"
            + " AND owner_protocol=#{protocol} AND source_receipt_id=#{receiptId}"
            + " AND source_version=#{version} ORDER BY id")
    List<MesEdhrSourceCreationBindingDO> selectByReceiptVersion(@Param("tenantId") Long tenantId,
            @Param("protocol") String protocol, @Param("receiptId") String receiptId, @Param("version") String version);

    @Select("SELECT " + COLUMNS + " FROM mes_edhr_source_creation_binding WHERE tenant_id=#{tenantId}"
            + " AND creation_event_id=#{eventId} ORDER BY id")
    List<MesEdhrSourceCreationBindingDO> selectByCreationEvent(@Param("tenantId") Long tenantId,
            @Param("eventId") Long eventId);

    @Insert("INSERT INTO mes_edhr_source_creation_binding(tenant_id,owner_protocol,source_row_id,"
            + "source_receipt_id,source_version,source_hash,request_hash_version,request_hash,"
            + "creation_event_id,creation_event_hash) VALUES(#{tenantId},#{ownerProtocol},#{sourceRowId},"
            + "#{sourceReceiptId},#{sourceVersion},#{sourceHash},#{requestHashVersion},#{requestHash},"
            + "#{creationEventId},#{creationEventHash})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MesEdhrSourceCreationBindingDO row);
}
