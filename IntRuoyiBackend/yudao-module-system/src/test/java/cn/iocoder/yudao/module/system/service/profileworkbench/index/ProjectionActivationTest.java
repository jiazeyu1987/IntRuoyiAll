package cn.iocoder.yudao.module.system.service.profileworkbench.index;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;

class ProjectionActivationTest {
    @TempDir Path folder;
    @Test void absentRuntimeApprovalNeverPublishes(){
        var evidence=new FileProjectionActivationEvidence("",new ObjectMapper());
        assertEquals(PROJECTION_ACTIVATION_REQUIRED.code(),assertThrows(ServiceException.class,()->evidence.requireApproved(2,"g",new Watermark(1,2),"hash")).getCode());
    }
    @Test void approvalMustMatchEveryWatermarkAndActualRuntimeGate() throws Exception {
        var mapper=new ObjectMapper().findAndRegisterModules();var path=folder.resolve("approval.json");var watermark=new Watermark(1,2);
        var approval=new FileProjectionActivationEvidence.Approval(2,"g",watermark,"hash","APPROVED",true,true,true,true,true,"operator",Instant.now().plusSeconds(120));
        Files.writeString(path,mapper.writeValueAsString(approval));var evidence=new FileProjectionActivationEvidence(path.toString(),mapper);
        assertDoesNotThrow(()->evidence.requireApproved(2,"g",watermark,"hash"));
        assertThrows(ServiceException.class,()->evidence.requireApproved(2,"g",new Watermark(2,2),"hash"));
        assertThrows(ServiceException.class,()->evidence.requireApproved(2,"other",watermark,"hash"));
        var staticOnly=new FileProjectionActivationEvidence.Approval(2,"g",watermark,"hash","APPROVED",false,true,false,false,false,"operator",Instant.now().plusSeconds(120));
        Files.writeString(path,mapper.writeValueAsString(staticOnly));assertThrows(ServiceException.class,()->evidence.requireApproved(2,"g",watermark,"hash"));
    }
}
