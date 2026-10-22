package cn.iocoder.yudao.server.profileworkbench;

import cn.iocoder.yudao.module.system.dal.mysql.profileworkbench.WorkbenchVisibilityVersionMapper;
import cn.iocoder.yudao.module.system.service.profileworkbench.WorkbenchVisibilityWriteTransaction;
import cn.iocoder.yudao.module.system.service.profileworkbench.index.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.*;
import java.util.List;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectionRetentionQueryContractTest {
    @Test void queryAndPreviewUseLockCapableMysqlTransactionsWithoutReadonlyMode() {
        for(String name:List.of("directory","issue","count","page")) {
            var method=java.util.Arrays.stream(IndexQueryTransaction.class.getDeclaredMethods()).filter(m->m.getName().equals(name)).findFirst().orElseThrow();
            var tx=method.getAnnotation(Transactional.class);
            assertFalse(tx.readOnly());assertEquals(Isolation.REPEATABLE_READ,tx.isolation());assertEquals(10,tx.timeout());
            assertEquals(Propagation.REQUIRES_NEW,tx.propagation());
        }
    }
    @Test void retiredActivePointerFailsBeforePersonalVersionAndQueryFactsAreRead() {
        var repository=mock(ProjectionRepository.class);var versions=mock(WorkbenchVisibilityVersionMapper.class);
        var visibility=mock(WorkbenchVisibilityWriteTransaction.class);var registry=mock(WorkbenchSourceRegistry.class);var tokens=mock(IndexQueryTokens.class);
        when(repository.lockGlobal(false)).thenReturn(new Global(0,0,"r"));
        when(repository.lockTenant(2,false)).thenReturn(new Tenant(2,0,0,"retired"));
        when(repository.checkpoint("retired",2)).thenThrow(new IllegalStateException("retired generation"));
        var query=new IndexQueryTransaction(repository,versions,visibility,registry,tokens,new ObjectMapper());
        assertThrows(IllegalStateException.class,()->query.directory(2,8));
        verifyNoInteractions(versions,visibility,registry,tokens);verify(repository,never()).authorizedSources(anyString(),anyLong(),anyLong(),any());
        verify(repository,never()).totals(any());verify(repository,never()).page(any(),anyLong(),anyInt());
    }
}
