package cn.iocoder.yudao.module.system.service.profileworkbench;
import cn.iocoder.yudao.module.system.dal.mysql.profileworkbench.WorkbenchVisibilityVersionMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import jakarta.annotation.Resource;
@Service @DS("master")
public class WorkbenchVisibilityContextInitializer {
 @Resource private WorkbenchVisibilityVersionMapper versions;
 @Transactional(propagation=Propagation.REQUIRES_NEW,rollbackFor=Exception.class,timeout=10)
 public long initialize(long tenant,long user) {
  if(tenant<0 || user<=0)throw cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.IDENTITY_CONTEXT_MISSING.exception();
  versions.initialize(tenant,user);
  Long value=versions.shared(tenant,user);
  if(value==null || value<1)throw cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.VISIBILITY_CONTEXT_MISSING.exception();
  return value;
 }
}
