package cn.iocoder.yudao.module.dcc.service.token;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_EXPIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_VIEWER_TOKEN_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DccViewerTokenErrorCodeContractTest {

    private static final Set<Integer> MERGED_INT_MAIN_DCC_CODES = Set.of(
            1_080_000_094,
            1_080_000_095,
            1_080_000_096,
            1_080_000_097,
            1_080_000_098,
            1_080_000_099,
            1_080_000_100,
            1_080_000_101,
            1_080_000_102,
            1_080_000_103,
            1_080_000_104,
            1_080_000_105,
            1_080_000_106,
            1_080_000_107);

    @Test
    void viewerTokenErrorCodes_doNotOccupyExistingLocalDccCodesAndAreUnique() {
        List<Integer> viewerTokenCodes = List.of(
                code(CONTROLLED_FILE_VIEWER_TOKEN_CONFIG_MISSING),
                code(CONTROLLED_FILE_VIEWER_TOKEN_INVALID),
                code(CONTROLLED_FILE_VIEWER_TOKEN_EXPIRED),
                code(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH));

        assertEquals(viewerTokenCodes.size(), new HashSet<>(viewerTokenCodes).size());
        assertFalse(viewerTokenCodes.stream().anyMatch(MERGED_INT_MAIN_DCC_CODES::contains));
    }

    private Integer code(ErrorCode errorCode) {
        return errorCode.getCode();
    }

}
