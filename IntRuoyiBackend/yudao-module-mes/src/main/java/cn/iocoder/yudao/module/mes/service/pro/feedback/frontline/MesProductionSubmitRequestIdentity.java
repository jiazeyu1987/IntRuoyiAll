package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackSubmitReqVO;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureV2Protocol;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Original client business command, captured before server normalization. Credentials are excluded. */
public final class MesProductionSubmitRequestIdentity {
    public static final String VERSION = "PRODUCTION_SUBMIT_REQUEST_V1";
    public static final String RAW_KEY = "productionSubmitRequest";
    private MesProductionSubmitRequestIdentity() { }

    public static String hash(MesProFrontlineFeedbackSubmitReqVO request) {
        if (request == null) throw new IllegalArgumentException("PRODUCTION_REQUEST_REQUIRED");
        ObjectNode tree = (ObjectNode) JsonUtils.parseTree(JsonUtils.toJsonString(request));
        tree.remove("signaturePassword");
        tree.remove("signatureId");
        // This key is server-owned and cannot be supplied in client raw payload.
        if (tree.path("rawPayload").has(RAW_KEY)) throw new IllegalArgumentException("PRODUCTION_REQUEST_PROOF_CLIENT_FORBIDDEN");
        return ElectronicSignatureV2Protocol.hash(tree);
    }

    public static void requireMatches(String expected, com.fasterxml.jackson.databind.JsonNode proof) {
        if (proof == null || !proof.isObject() || proof.size() != 2
                || !VERSION.equals(proof.path("version").asText())
                || !expected.equals(proof.path("hash").asText())) {
            throw new IllegalArgumentException("PRODUCTION_IDEMPOTENCY_PAYLOAD_CONFLICT");
        }
    }
}
