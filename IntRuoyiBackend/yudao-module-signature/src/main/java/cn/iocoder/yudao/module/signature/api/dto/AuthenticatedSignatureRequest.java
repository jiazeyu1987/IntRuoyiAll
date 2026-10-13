package cn.iocoder.yudao.module.signature.api.dto;

import com.fasterxml.jackson.databind.JsonNode;

/** Server-only locked facts; credentials and authentication callbacks are deliberately absent. */
public record AuthenticatedSignatureRequest(ElectronicSignatureCommand command, SignatureSubjectSnapshot subject,
        SignatureActionDefinition action, JsonNode stableSubjectKey, String displayName, JsonNode signatureImage) { }
