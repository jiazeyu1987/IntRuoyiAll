package cn.iocoder.yudao.module.srm;
import cn.iocoder.yudao.module.srm.dal.dataobject.supplier.SrmErpSupplierDO;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class BusinessTenantBoundaryTest {
 @Test void scopesReadsAndWritesForBothTenants() {
  var interceptor = new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(new TenantProperties()));
  try {
   for (Class<?> entity : List.of(SrmErpSupplierDO.class)) {
    var table = TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), entity).getTableName();
    for (long tenant : new long[]{1,165}) {
     TenantContextHolder.setTenantId(tenant);
     for (String sql : List.of("SELECT * FROM "+table+" WHERE id=42", "UPDATE "+table+" SET deleted=1 WHERE id=42", "DELETE FROM "+table+" WHERE id=42")) {
      String scoped = interceptor.parserSingle(sql,null);
      assertTrue(scoped.contains("tenant_id = "+tenant),scoped);
     }
     String insert = interceptor.parserSingle("INSERT INTO "+table+" (id) VALUES (42)",null);
     assertTrue(insert.contains("tenant_id"),insert);
     assertTrue(insert.contains("42, "+tenant),insert);
    }
   }
  } finally { TenantContextHolder.clear(); }
 }
}
