package cn.iocoder.yudao.module.dcc.signature.service.portal;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureAuthorizationDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureAuthorizationMapper;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyBlocker;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyOverview;
import cn.iocoder.yudao.module.dcc.signature.service.policy.SignatureGovernancePolicyService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationServiceImpl.STATE_DISABLED;
import static cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationServiceImpl.STATE_ENABLED;
import static cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationServiceImpl.STATE_LOCKED;
import static cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationServiceImpl.STATE_UNAUTHORIZED;

public class SignatureGovernancePortalServiceImpl implements SignatureGovernancePortalService {

    private final SignatureGovernancePolicyService policyService;
    private final SignatureGovernancePortalAdapterRegistry portalAdapterRegistry;
    private final DccElectronicSignatureAuthorizationMapper authorizationMapper;

    public SignatureGovernancePortalServiceImpl(SignatureGovernancePolicyService policyService,
                                                SignatureGovernancePortalAdapterRegistry portalAdapterRegistry,
                                                DccElectronicSignatureAuthorizationMapper authorizationMapper) {
        if (policyService == null || portalAdapterRegistry == null || authorizationMapper == null) {
            throw new IllegalArgumentException("Signature governance portal service requires policy, adapters, and authorization mapper");
        }
        this.policyService = policyService;
        this.portalAdapterRegistry = portalAdapterRegistry;
        this.authorizationMapper = authorizationMapper;
    }

    @Override
    public SignatureGovernancePortalOverview getOverview(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Signature governance portal requires login user");
        }
        SignatureGovernancePortalAuthorizationOverview authorization = describeAuthorization(userId);
        List<SignatureGovernancePortalModuleOverview> modules = portalAdapterRegistry.listAdapters().stream()
                .map(adapter -> describeModule(userId, authorization, adapter))
                .toList();
        List<SignatureGovernancePortalBlocker> blockers = new ArrayList<>(authorization.blockers());
        for (SignatureGovernancePortalModuleOverview module : modules) {
            blockers.addAll(module.blockers());
        }
        if (modules.isEmpty()) {
            blockers.add(SignatureGovernancePortalBlocker.of("MODULE_ADAPTER_MISSING",
                    "未注册任何电子签名门户适配器",
                    "统一电子签名页签无法展示正式入口与摘要"));
        }
        List<SignatureGovernancePortalBlocker> distinctBlockers = distinct(blockers);
        long readyModuleTotal = modules.stream().filter(SignatureGovernancePortalModuleOverview::ready).count();
        long pendingTotal = modules.stream().mapToLong(module -> module.metrics().pendingCount()).sum();
        long signatureTotal = modules.stream().mapToLong(module -> module.metrics().signatureCount()).sum();
        boolean ready = distinctBlockers.isEmpty() && !modules.isEmpty();
        return new SignatureGovernancePortalOverview(ready ? "READY" : "BLOCKED", ready, authorization,
                new SignatureGovernancePortalSummary((long) modules.size(), readyModuleTotal,
                        (long) modules.size() - readyModuleTotal, pendingTotal, signatureTotal),
                modules, distinctBlockers);
    }

    private SignatureGovernancePortalModuleOverview describeModule(Long userId,
                                                                   SignatureGovernancePortalAuthorizationOverview authorization,
                                                                   SignatureGovernancePortalAdapter adapter) {
        SignatureGovernancePolicyOverview policy = policyService.describeModule(adapter.getModuleCode());
        SignatureGovernancePortalMetrics metrics = adapter.describeMetrics(userId);
        SignatureGovernancePortalRouteOverview routes = SignatureGovernancePortalRouteOverview.of(
                adapter.getPrimaryRouteLabel(), adapter.getPrimaryRoute(),
                adapter.getSecondaryRouteLabel(), adapter.getSecondaryRoute());
        List<SignatureGovernancePortalBlocker> blockers = new ArrayList<>();
        blockers.addAll(authorization.blockers());
        blockers.addAll(toPortalBlockers(policy.blockers()));
        boolean ready = blockers.isEmpty();
        return new SignatureGovernancePortalModuleOverview(adapter.getModuleCode(), adapter.getModuleName(),
                adapter.getModuleDescription(), ready ? "READY" : "BLOCKED", ready, authorization, policy,
                metrics, routes, distinct(blockers));
    }

    private SignatureGovernancePortalAuthorizationOverview describeAuthorization(Long userId) {
        DccElectronicSignatureAuthorizationDO authorization = authorizationMapper.selectByUserId(userId);
        if (authorization == null) {
            return SignatureGovernancePortalAuthorizationOverview.of(STATE_UNAUTHORIZED, false,
                    List.of(SignatureGovernancePortalBlocker.of("SIGNATURE_AUTH_UNAUTHORIZED",
                            "当前用户未开通电子签名授权",
                            "所有需要电子签名的模块都会被阻断")));
        }
        if (!Boolean.TRUE.equals(authorization.getElectronicSignatureEnabled())
                || STATE_DISABLED.equals(authorization.getAuthorizationState())) {
            return SignatureGovernancePortalAuthorizationOverview.of(STATE_DISABLED, false,
                    List.of(SignatureGovernancePortalBlocker.of("SIGNATURE_AUTH_DISABLED",
                            "当前用户电子签名授权已停用",
                            "所有需要电子签名的模块都会被阻断")));
        }
        if (isActiveLock(authorization)) {
            return SignatureGovernancePortalAuthorizationOverview.of(STATE_LOCKED, false,
                    List.of(SignatureGovernancePortalBlocker.of("SIGNATURE_AUTH_LOCKED",
                            "当前用户电子签名授权已锁定",
                            "授权解锁前，所有需要电子签名的模块都会被阻断")));
        }
        if (STATE_ENABLED.equals(authorization.getAuthorizationState()) || isExpiredLock(authorization)) {
            return SignatureGovernancePortalAuthorizationOverview.of(STATE_ENABLED, true, List.of());
        }
        throw new IllegalStateException("Unsupported electronic signature authorization state: "
                + authorization.getAuthorizationState());
    }

    private static boolean isActiveLock(DccElectronicSignatureAuthorizationDO authorization) {
        LocalDateTime now = LocalDateTime.now();
        if (authorization.getLockedUntil() != null && authorization.getLockedUntil().isAfter(now)) {
            return true;
        }
        return STATE_LOCKED.equals(authorization.getAuthorizationState()) && authorization.getLockedUntil() == null;
    }

    private static boolean isExpiredLock(DccElectronicSignatureAuthorizationDO authorization) {
        return STATE_LOCKED.equals(authorization.getAuthorizationState())
                && authorization.getLockedUntil() != null
                && !authorization.getLockedUntil().isAfter(LocalDateTime.now());
    }

    private static List<SignatureGovernancePortalBlocker> toPortalBlockers(List<SignatureGovernancePolicyBlocker> blockers) {
        return blockers == null ? List.of() : blockers.stream()
                .map(blocker -> SignatureGovernancePortalBlocker.of(blocker.code().name(), blocker.message(),
                        blocker.impact()))
                .toList();
    }

    private static List<SignatureGovernancePortalBlocker> distinct(List<SignatureGovernancePortalBlocker> blockers) {
        Map<String, SignatureGovernancePortalBlocker> distinct = new LinkedHashMap<>();
        if (blockers != null) {
            for (SignatureGovernancePortalBlocker blocker : blockers) {
                if (blocker == null) {
                    continue;
                }
                String key = blocker.code() + "|" + blocker.message() + "|" + blocker.impact();
                distinct.putIfAbsent(key, blocker);
            }
        }
        return List.copyOf(distinct.values());
    }
}
