package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class DccWorkflowDatePolicyTest {
    private final LocalDate today=LocalDate.of(2026,10,11);
    private DccWorkflowDatePolicy policy() {return new DccWorkflowDatePolicy(){
        @Override public LocalDateTime now(){return today.atTime(12,0);}
    };}
    @Test void signatureModeRequiresNoPretendCalendarDate() {
        var dates=policy();assertDoesNotThrow(()->require(dates,"AFTER_STANDARDIZATION_SIGNATURE",null));
        assertThrows(IllegalArgumentException.class,()->require(dates,"AFTER_STANDARDIZATION_SIGNATURE",today));
    }
    @Test void presetDateRetainsExistingTodayAndPastDateGate() {
        var dates=policy();assertDoesNotThrow(()->require(dates,"PRESET_DATE",today));
        assertDoesNotThrow(()->require(dates,"PRESET_DATE",today.plusDays(1)));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->require(dates,"PRESET_DATE",null));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->require(dates,"PRESET_DATE",today.minusDays(1)));
    }
    @Test void absentOrUnknownModesNeverBecomePresetFallback() {
        for(String mode:new String[]{null,""," PRESET_DATE","PRESET_DATE ","true","LEGACY"})
            assertThrows(IllegalArgumentException.class,()->require(policy(),mode,today));
    }
    @Test void aDateLegalAtSubmissionRemainsUsableAfterTheBusinessClockAdvances() {
        var day=new java.util.concurrent.atomic.AtomicReference<>(today);
        var dates=new DccWorkflowDatePolicy(){@Override public LocalDateTime now(){return day.get().atTime(12,0);}};
        dates.requireEffectiveConfiguration("PRESET_DATE",today);
        day.set(today.plusDays(2));
        assertDoesNotThrow(()->ReflectionTestUtils.invokeMethod(dates,"requireSubmittedEffectiveConfiguration","PRESET_DATE",today));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->dates.requireEffectiveConfiguration("PRESET_DATE",today));
    }
    @Test void submittedShapeStillRequiresADateAndAnExplicitUnambiguousMode() {
        var dates=policy();
        assertDoesNotThrow(()->ReflectionTestUtils.invokeMethod(dates,"requireSubmittedEffectiveConfiguration","AFTER_STANDARDIZATION_SIGNATURE",null));
        assertThrows(RuntimeException.class,()->ReflectionTestUtils.invokeMethod(dates,"requireSubmittedEffectiveConfiguration","PRESET_DATE",null));
        assertThrows(RuntimeException.class,()->ReflectionTestUtils.invokeMethod(dates,"requireSubmittedEffectiveConfiguration","AFTER_STANDARDIZATION_SIGNATURE",today));
        for(String mode:new String[]{null,""," PRESET_DATE","UNKNOWN"})
            assertThrows(RuntimeException.class,()->ReflectionTestUtils.invokeMethod(dates,"requireSubmittedEffectiveConfiguration",mode,today));
    }
    private void require(DccWorkflowDatePolicy dates,String mode,LocalDate date){
        dates.requireEffectiveConfiguration(mode,date);
    }
}
