package cn.iocoder.yudao.module.bpm.workbench;
import org.apache.ibatis.annotations.Mapper;
import java.util.*;
@Mapper
@com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine="true")
public interface EdhrVoidWorkbenchMapper {
    @org.apache.ibatis.annotations.Options(timeout=10)
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM ACT_RE_PROCDEF WHERE CAST(ID_ AS BINARY)=CAST(#{definition} AS BINARY) AND CAST(TENANT_ID_ AS BINARY)=CAST(#{tenant} AS BINARY) AND CAST(KEY_ AS BINARY)=CAST('mes-edhr-batch-execution-void-v1' AS BINARY)")
    long countVoidDefinition(@org.apache.ibatis.annotations.Param("tenant") String tenant,@org.apache.ibatis.annotations.Param("definition") String definition);
    long countBrokenDomain(Map<String,Object> parameters);
    Long count(Map<String,Object> parameters);
    List<EdhrVoidWorkbenchRow> chunk(Map<String,Object> parameters);
    List<EdhrVoidWorkbenchRow> rows(Map<String,Object> parameters);
}
