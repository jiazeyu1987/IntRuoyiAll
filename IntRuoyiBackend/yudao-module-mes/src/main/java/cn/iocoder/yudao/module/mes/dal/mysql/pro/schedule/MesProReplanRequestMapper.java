package cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProReplanRequestDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.*;

@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface MesProReplanRequestMapper {
    @Insert("INSERT INTO mes_pro_replan_request (tenant_id, operator_id, request_key, payload_hash) VALUES (#{tenantId}, #{operatorId}, #{key}, #{hash}) ON DUPLICATE KEY UPDATE request_key = request_key")
    void claim(@Param("tenantId") Long tenantId, @Param("operatorId") Long operatorId,
               @Param("key") String key, @Param("hash") String hash);

    @Select("SELECT payload_hash, result_json FROM mes_pro_replan_request WHERE tenant_id=#{tenantId} AND operator_id=#{operatorId} AND request_key=#{key} FOR UPDATE")
    MesProReplanRequestDO lock(@Param("tenantId") Long tenantId, @Param("operatorId") Long operatorId, @Param("key") String key);

    @Update("UPDATE mes_pro_replan_request SET result_json=#{result} WHERE tenant_id=#{tenantId} AND operator_id=#{operatorId} AND request_key=#{key}")
    int complete(@Param("tenantId") Long tenantId, @Param("operatorId") Long operatorId,
                 @Param("key") String key, @Param("result") String result);
}
