package cn.iocoder.yudao.module.system.service.profileworkbench.index;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @DS("master") @ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class ProjectionControlInitializer {
    private final ProjectionRepository repository;
    public ProjectionControlInitializer(ProjectionRepository repository){this.repository=repository;}
    @Transactional(isolation=Isolation.REPEATABLE_READ,rollbackFor=Exception.class,timeout=10)
    public void initialize(long tenant,String revision){repository.initializeControls(tenant,revision);}
}
