package cn.iocoder.yudao.module.system.service.controlledcontent;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.OBSOLETE_ACTIVE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.PUBLISH;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.REGISTER_ACTIVE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction.WITHDRAW;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.DCC_CONTROLLED_FILE;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.MES_ROUTE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlledContentTransitionProfileContractTest {

    @Test
    void mesRouteProfile_shouldBeStaticManualPublishAndWithdrawToSameDraft() {
        ControlledContentTransitionProfile profile = ControlledContentTransitionProfile.requiredFor(MES_ROUTE);

        assertEquals(MES_ROUTE, profile.contentType());
        assertTrue(profile.supports(REGISTER_ACTIVE));
        assertTrue(profile.supports(WITHDRAW));
        assertTrue(profile.supports(PUBLISH));
        assertFalse(profile.supports(OBSOLETE_ACTIVE));
    }

    @Test
    void dccProfile_shouldBeStaticAutoFinalizationAndWithdrawToNewDraft() {
        ControlledContentTransitionProfile profile = ControlledContentTransitionProfile.requiredFor(DCC_CONTROLLED_FILE);

        assertEquals(DCC_CONTROLLED_FILE, profile.contentType());
        assertTrue(profile.supports(REGISTER_ACTIVE));
        assertTrue(profile.supports(WITHDRAW));
        assertFalse(profile.supports(PUBLISH));
        assertTrue(profile.supports(OBSOLETE_ACTIVE));
    }

    @Test
    void profiles_shouldExposeOnlyStaticSupportedActions() {
        ControlledContentTransitionProfile profile = ControlledContentTransitionProfile.requiredFor(DCC_CONTROLLED_FILE);

        String[] componentNames = Arrays.stream(ControlledContentTransitionProfile.class.getRecordComponents())
                .map(component -> component.getName())
                .toArray(String[]::new);
        assertArrayEquals(new String[]{"contentType", "supportedActions"}, componentNames);
        assertThrows(UnsupportedOperationException.class, () -> profile.supportedActions().add(PUBLISH));
    }
}
