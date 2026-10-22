package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewCounterDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * eDHR 不合格评审编号计数器 Mapper。
 */
@Mapper
public interface MesProEdhrNonconformanceReviewCounterMapper
        extends BaseMapperX<MesProEdhrNonconformanceReviewCounterDO> {

    /**
     * 原子地为租户分配下一个流水号。
     * 首次分配为 0，后续每次递增 1；依赖 InnoDB 主键冲突更新锁保证并发安全。
     */
    @Insert("INSERT INTO mes_pro_edhr_nonconformance_review_counter "
            + "(tenant_id, current_serial, create_time, update_time) "
            + "VALUES (#{tenantId}, 0, NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE current_serial = current_serial + 1, update_time = NOW()")
    int insertOrIncrement(@Param("tenantId") Long tenantId);

    @Select("SELECT tenant_id, current_serial, create_time, update_time "
            + "FROM mes_pro_edhr_nonconformance_review_counter "
            + "WHERE tenant_id = #{tenantId} FOR UPDATE")
    MesProEdhrNonconformanceReviewCounterDO selectByTenantIdForUpdate(@Param("tenantId") Long tenantId);
}
