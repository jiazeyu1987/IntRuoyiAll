package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

final class DccRegistrationCertificateCommandFingerprint {

    private DccRegistrationCertificateCommandFingerprint() {
    }

    static String draft(String kind, Long certificateId, Integer rowRevision, Integer snapshotRevision,
                        DccRegistrationCertificateDraftData draft) {
        StringBuilder canonical = new StringBuilder();
        add(canonical, kind);
        add(canonical, certificateId);
        add(canonical, rowRevision);
        add(canonical, snapshotRevision);
        add(canonical, draft.ownerCompanyId());
        add(canonical, draft.productMasterId());
        add(canonical, normalizeText(draft.productName()));
        add(canonical, draft.projectCodeId());
        add(canonical, draft.firstObtainedDate());
        add(canonical, normalizeText(draft.certificateNo()));
        add(canonical, draft.approvalDate());
        add(canonical, draft.effectiveDate());
        add(canonical, draft.expiryDate());
        add(canonical, normalizeText(draft.classification()));
        add(canonical, normalizeText(draft.registrantName()));
        add(canonical, normalizeText(draft.modelSpecification()));
        add(canonical, normalizeText(draft.structureComposition()));
        add(canonical, normalizeText(draft.intendedUse()));
        add(canonical, normalizeText(draft.technicalRequirements()));
        add(canonical, normalizeText(draft.residenceAddress()));
        add(canonical, normalizeText(draft.productionAddress()));
        add(canonical, draft.entrustedProduction());
        add(canonical, draft.selfProduction());
        addList(canonical, draft.entrustedEnterpriseIds());
        add(canonical, normalizeText(draft.remark()));
        return sha256(canonical.toString());
    }

    static String delete(Long certificateId, Integer rowRevision, Integer snapshotRevision) {
        StringBuilder canonical = new StringBuilder();
        add(canonical, "DRAFT_DELETE");
        add(canonical, certificateId);
        add(canonical, rowRevision);
        add(canonical, snapshotRevision);
        return sha256(canonical.toString());
    }

    static String formalize(Long certificateId, Integer rowRevision, Integer snapshotRevision, Long fileId) {
        StringBuilder canonical = new StringBuilder();
        add(canonical, "FORMALIZE");
        add(canonical, certificateId);
        add(canonical, rowRevision);
        add(canonical, snapshotRevision);
        add(canonical, fileId);
        return sha256(canonical.toString());
    }

    static String formalizeApprovedUpload(
            Long certificateId, Integer rowRevision, Integer snapshotRevision, Long fileId, Long validationActorId) {
        StringBuilder canonical = new StringBuilder();
        add(canonical, "FORMALIZE_APPROVED_UPLOAD");
        add(canonical, certificateId);
        add(canonical, rowRevision);
        add(canonical, snapshotRevision);
        add(canonical, fileId);
        add(canonical, validationActorId);
        return sha256(canonical.toString());
    }

    private static void addList(StringBuilder target, List<Long> values) {
        if (values == null) {
            add(target, null);
            return;
        }
        add(target, values.size());
        for (Long value : values) {
            add(target, value);
        }
    }

    private static void add(StringBuilder target, Object value) {
        String text = value == null ? null : value.toString();
        if (text == null) {
            target.append("-1:");
        } else {
            target.append(text.length()).append(':').append(text);
        }
        target.append('|');
    }

    private static String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 算法不可用", exception);
        }
    }
}
