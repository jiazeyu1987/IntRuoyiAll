# 执行日志：统一电子签名页签后端聚合协议

BDD: 统一电子签名页签展示 DCC/eDHR 摘要 -> Given 当前账号具备统一电子签名页签权限 / When 打开统一电子签名页签 / Then 后端返回 DCC 与 eDHR 的授权状态、签名记录数、待处理数和正式入口路径。

BDD: 未授权签名必须显式暴露 -> Given 当前账号未开通电子签名授权 / When 请求统一电子签名页签摘要 / Then DCC/eDHR 模块状态显示未授权阻塞，不能把状态降级为正常。

BDD: 正式签名仍回模块执行 -> Given 当前账号在统一页签看到 DCC/eDHR 待处理项 / When 点击模块入口 / Then 前端跳转到 DCC 或 eDHR 正式签名页，而不是 BPM 通用审批按钮。

BDD: 新模块统一接入 -> Given 后续模块需要电子签名 / When 接入统一页签 / Then 只需实现统一聚合/展示协议，不再复制独立签名中心。

GREEN: worktree-setup -> PASS, backend `D:\ProjectPackage\Int\IntRuoyiWorktrees\sign2\ruoyi-vue-pro` and frontend `D:\ProjectPackage\Int\IntRuoyiWorktrees\sign2\yudao-ui-admin-vue3` both created on branch `codex/sign2`.

GREEN: experience-gate-read -> PASS, read `docs/worktree-memory.md`, `docs/login-access.md`, and `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`; high-risk E2E/startup actions still require `experience-preflight` before execution.

RED: `mvn -pl yudao-module-dcc "-Dtest=SignatureGovernancePortalServiceTest,SignatureGovernanceControllerTest" test` -> FAIL, expected missing unified portal service/adapter contract: `cn.iocoder.yudao.module.dcc.signature.service.portal` package does not exist.

GREEN: `mvn -pl yudao-module-dcc "-Dtest=SignatureGovernancePortalServiceTest,SignatureGovernanceControllerTest" test` -> PASS, 15 tests passed; unified portal service aggregates adapters and controller exposes `GET /signature-governance/portal/overview`.

GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS, eDHR portal adapter compiles with DCC portal protocol in reactor.

GREEN: frontend-contract -> PASS, frontend `node scripts/signature-governance-page-contract.test.mjs` and `node tests/e2e/signature-governance-e2e-static.spec.js` both passed against `GET /signature-governance/portal/overview`.

BLOCKER: task-closeout-cleanup preview -> blocked, linked worktree cannot be fast-forward merged into `int_main` and main backend worktree is dirty; no cleanup or merge was applied.

BDD: 权威电子签名策略源必须正式配置 -> Given DCC/eDHR/Showroom/IntAuth 需要进入统一电子签名策略治理 / When 后端启动并读取 `signature.governance.policy.modules` / Then 每个模块必须存在 `source-code`、`policy-version`、`authority-confirmed=true`、`owner` 和 `approval-ref`，不得用缺省值或 fallback 放行。

RED: `mvn -pl yudao-server -Dtest=SignatureGovernancePolicySourceConfigTest test` -> FAIL, `application-local.yaml` 与 `application-dev.yaml` 缺少 `signature.governance.policy.modules` 权威策略源配置，真实 E2E 只能返回 `POLICY_SOURCE_MISSING`。

GREEN: `mvn -pl yudao-server -Dtest=SignatureGovernancePolicySourceConfigTest test` -> PASS, 1 test passed；本地与 dev 配置均声明 DCC、EDHR、SHOWROOM、INTAUTH 的权威策略源、版本、确认人和批准引用。

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS, Spring Boot repackage 生成可执行 `yudao-server.jar`，用于 sign2 后端 `http://127.0.0.1:48086`。

GREEN: backend-health -> PASS, `http://127.0.0.1:48086/actuator/health` 返回 `{"status":"UP"}`，运行态归属为 `D:\ProjectPackage\Int\IntRuoyiWorktrees\sign2\ruoyi-vue-pro`。

GREEN: real-data-policy-e2e -> PASS, `SIGNATURE_GOVERNANCE_E2E_* node tests/e2e/signature-governance-policy.e2e.js` 在测试租户真实登录后通过，`test-results/signature-governance/policy.json` 记录 `status=PASS`。

GREEN: bug-regression-evidence -> PASS, `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence ...\bug-regression-evidence.md` 校验证据结构完整。

BLOCKER: task-closeout-cleanup preview -> blocked, `delete=<none>`；因主后端 worktree `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` 仍为脏状态且 `codex/sign2` 不能快进合并到 `int_main`，本次不执行 cleanup apply、合并或删除 worktree。

GREEN: experience-preflight -> PASS, 用户明确要求先提交前后端代码、融合进 `int_main`、融合后真实 E2E 验证成功再删除 `sign2` worktree；融合前将临时保护主工作区非本任务脏改动，不覆盖、不回滚、不混入本任务提交。

GREEN: merged-result-e2e -> PASS, `int_main` 干净融合后重新启动 `http://127.0.0.1:48081` 与 `http://127.0.0.1:8081`，`node scripts/preflight/login-preflight.mjs ...` 与 `SIGNATURE_GOVERNANCE_E2E_* node tests/e2e/signature-governance-policy.e2e.js` 再次通过，`policy.json` 记录 `status=PASS`。
