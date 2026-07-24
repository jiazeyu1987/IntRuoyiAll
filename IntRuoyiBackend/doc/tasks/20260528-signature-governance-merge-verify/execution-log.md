# Execution Log：20260528-signature-governance-merge-verify

BDD: merge branch into int_main -> Given 电子签名治理支线已通过 reviewer 放行, When 将后端和前端支线融合进 `int_main`, Then 主 worktree 必须包含支线最新代码且不覆盖无关用户改动。

BDD: restart before verification -> Given 主 worktree 已合并最新代码, When 执行主租户验证前, Then 必须先重启前后端，确保运行时加载的是合并后的最新代码。

BDD: verify with main tenant -> Given 前后端已重启, When 使用 `芋道源码 / admin / admin123` 进入电子签名治理路径, Then 验证结果必须来自真实前端路径和真实后端接口，不使用 mock 或静默降级。

BDD: fix via test tenant on failure -> Given 主租户验证失败, When 需要修改代码或数据准备, Then 必须回测试租户修复和验证，再回到 `芋道源码 / admin / admin123` 复验成功后才能放行。

GREEN: task document created -> PASS.

GREEN: git merge --no-ff --no-commit codex/20260528-signature-governance-docs (backend int_main) -> PASS, automatic merge completed without conflicts and commit is intentionally held for runtime verification.

GREEN: git merge --no-ff --no-commit codex/20260528-signature-governance-docs (frontend int_main) -> PASS, automatic merge completed without conflicts and commit is intentionally held for runtime verification.

GREEN: `mvn -pl yudao-module-dcc -Dtest=SignatureGovernanceRetentionServiceTest,SignatureGovernanceRetentionObjectStoreVerificationServiceTest,SignatureGovernanceControllerTest,ConfigurableSignatureGovernancePolicySourceProviderTest,SignatureGovernancePolicyIntAuthAdapterTest,SignatureGovernancePolicyServiceTest test` -> PASS, 47 tests run, 0 failures, 0 errors.

GREEN: `python -m pytest script\tests\test_signature_governance_menu_sql.py` -> PASS, 2 tests.

BLOCKER: `script\deploy\restart-int-ruoyi-local.ps1 -Component full -WorktreeName int_main` -> FAIL, unrelated frontend worktree `showroom-hall-description-export` has no paired backend worktree, so the helper refused to build a port map.

RED: `script\deploy\restart-int-ruoyi-local.ps1 -Component full -WorktreeName int_main` -> FAIL, expected reason: restart helper must fail fast when the mixed worktree map is inconsistent instead of silently using a partial runtime map.

BLOCKER: direct backend start on `48081` -> FAIL fast, `DCC electronic signature evidence configuration is missing`; impact: runtime cannot prove DCC signature evidence without explicit HMAC config.

GREEN: backend restart on `48081` with `DCC_SIGNATURE_EVIDENCE_HMAC_SECRET=CODEX-DCC-E2E-HMAC-SECRET-20260526`, `DCC_SIGNATURE_EVIDENCE_KEY_VERSION=dcc-hmac-v1`, unified policy source env, and MinIO verifier env -> PASS, `GET /actuator/health` returned `{"status":"UP"}`.

BLOCKER: frontend Vite dev server on `8081` -> FAIL, `EMFILE: too many open files` while opening `src/types/auto-imports.d.ts` and later `index.html`.

GREEN: frontend production build for verification -> PASS, `pnpm build:local` with `VITE_BASE_URL=http://127.0.0.1:48081`; built `dist` contains `127.0.0.1:48081` and no old `127.0.0.1:48098` API target.

GREEN: frontend preview on `8081` -> PASS, login page returned HTTP 200.

GREEN: test tenant policy E2E after frontend fix -> PASS, `测试租户 / aoteman / admin123` reached `/signature-governance` and policy E2E passed.

GREEN: main tenant final verification -> PASS, `芋道源码 / admin / admin123` reached `http://127.0.0.1:8081/signature-governance`; policy API returned `READY`, `ready=true`, module statuses `DCC, EDHR, INTAUTH, SHOWROOM`, and no failed `admin-api` responses.

GREEN: `git worktree remove D:\ProjectPackage\Int\IntRuoyi\worktrees\20260528-signature-governance-docs\ruoyi-vue-pro` -> PASS, backend支线 worktree 已删除。

GREEN: `git worktree remove D:\ProjectPackage\Int\IntRuoyi\worktrees\20260528-signature-governance-docs\yudao-ui-admin-vue3` -> PASS, frontend支线 worktree 已删除。

GREEN: `Test-Path D:\ProjectPackage\Int\IntRuoyi\worktrees\20260528-signature-governance-docs\ruoyi-vue-pro` and frontend path -> PASS, both returned `False`; `git worktree list` no longer includes `20260528-signature-governance-docs`.

GREEN: `task-closeout-cleanup --mode preview` -> PASS, cleanup plan keeps `task.md` and `execution-log.md`, deletes only task-local `verification-report.md`, and reports no blockers.

GREEN: `task-closeout-cleanup --mode apply` -> PASS, deleted only `doc/tasks/20260528-signature-governance-merge-verify/verification-report.md`; task core records remain.
