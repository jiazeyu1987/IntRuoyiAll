package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * Authoritative JSON_TABLE searches cannot be parsed by the installed JSqlParser 5.2.
 * Only these dedicated methods bypass SQL rewriting: the outer tenant is mandatory and bound,
 * and every source/identity/visibility predicate is explicitly tenant correlated and soft-delete scoped.
 * Use direct COUNT and LIMIT (no IPage/derived count) so no pagination parser touches JSON_TABLE.
 * system_users department/self permissions are resolved by the original identity mapper before
 * this query and carried as bound IDs in TraceSearchScope; that table is not queried here.
 */
@Mapper
public interface MesProEdhrBatchSearchMapper {
    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    @Select("<script>SELECT mes_pro_edhr_batch_execution.* FROM mes_pro_edhr_batch_execution "
            + "WHERE mes_pro_edhr_batch_execution.tenant_id=#{tenantId} AND mes_pro_edhr_batch_execution.deleted=0 "
            + "<if test='ew != null and ew.sqlSegment != null and ew.sqlSegment != &quot;&quot;'> AND ${ew.sqlSegment}</if> "
            + "ORDER BY mes_pro_edhr_batch_execution.id DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<MesProEdhrBatchExecutionDO> selectSearchPage(@Param("tenantId") Long tenantId,
            @Param("ew") LambdaQueryWrapperX<MesProEdhrBatchExecutionDO> query,
            @Param("offset") long offset, @Param("limit") int limit);

    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    @Select("<script>SELECT COUNT(*) FROM mes_pro_edhr_batch_execution "
            + "WHERE mes_pro_edhr_batch_execution.tenant_id=#{tenantId} AND mes_pro_edhr_batch_execution.deleted=0 "
            + "<if test='ew != null and ew.sqlSegment != null and ew.sqlSegment != &quot;&quot;'> AND ${ew.sqlSegment}</if></script>")
    long selectSearchCount(@Param("tenantId") Long tenantId,
            @Param("ew") LambdaQueryWrapperX<MesProEdhrBatchExecutionDO> query);

    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    @Select("<script>SELECT MAX(mes_pro_edhr_batch_execution.id) FROM mes_pro_edhr_batch_execution "
            + "WHERE mes_pro_edhr_batch_execution.tenant_id=#{tenantId} AND mes_pro_edhr_batch_execution.deleted=0 "
            + "<if test='ew != null and ew.sqlSegment != null and ew.sqlSegment != &quot;&quot;'> AND ${ew.sqlSegment}</if></script>")
    Long selectIntegrityUpperId(@Param("tenantId") Long tenantId,
            @Param("ew") LambdaQueryWrapperX<MesProEdhrBatchExecutionDO> query);

    @InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    @Select("<script>SELECT mes_pro_edhr_batch_execution.* FROM mes_pro_edhr_batch_execution "
            + "WHERE mes_pro_edhr_batch_execution.tenant_id=#{tenantId} AND mes_pro_edhr_batch_execution.deleted=0 "
            + "AND mes_pro_edhr_batch_execution.id &gt; #{afterId} AND mes_pro_edhr_batch_execution.id &lt;= #{upperId} "
            + "<if test='ew != null and ew.sqlSegment != null and ew.sqlSegment != &quot;&quot;'> AND ${ew.sqlSegment}</if> "
            + "ORDER BY mes_pro_edhr_batch_execution.id ASC LIMIT #{limit}</script>")
    List<MesProEdhrBatchExecutionDO> selectIntegrityChunk(@Param("tenantId") Long tenantId,
            @Param("ew") LambdaQueryWrapperX<MesProEdhrBatchExecutionDO> query,
            @Param("afterId") long afterId, @Param("upperId") long upperId, @Param("limit") int limit);
}
