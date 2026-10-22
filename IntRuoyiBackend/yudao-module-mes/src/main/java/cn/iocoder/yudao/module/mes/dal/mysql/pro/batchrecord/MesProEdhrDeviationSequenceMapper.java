package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MesProEdhrDeviationSequenceMapper {

    @Insert("""
            INSERT INTO mes_pro_edhr_deviation_sequence (tenant_id, `year_month`, `last_value`)
            VALUES (#{tenantId}, #{yearMonth}, LAST_INSERT_ID(1))
            ON DUPLICATE KEY UPDATE `last_value` = LAST_INSERT_ID(`last_value` + 1)
            """)
    int allocateNext(@Param("tenantId") Long tenantId, @Param("yearMonth") String yearMonth);

    @Select("SELECT LAST_INSERT_ID()")
    Long selectLastInsertedId();
}
