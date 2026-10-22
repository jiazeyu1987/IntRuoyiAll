package cn.iocoder.yudao.module.dcc.service.projectcode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
class DccProjectFolderMaintenanceContractTest {
    @Test void projectFolderHasItsOwnAuthenticatedSaveContract() {
        assertDoesNotThrow(()->{
            var save=Class.forName("cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFolderMaintenanceService$Save");
            Class.forName("cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFolderMaintenanceService")
                    .getMethod("save",Long.class,Long.class,save);
        });
    }
    @Test void logicalFolderDeleteRequiresExactConfirmationAndReason(){
        assertDoesNotThrow(()->Class.forName("cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFolderMaintenanceService")
                .getMethod("delete",Long.class,Long.class,Long.class,boolean.class,String.class));
    }
}
