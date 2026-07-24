# 任务：电子签名治理支线合并与主租户验证

## 目标

将 `codex/20260528-signature-governance-docs` 支线融合进 `int_main`，重启主 worktree 前后端，确认加载最新代码后使用 `芋道源码 / admin / admin123` 进行真实路径验证。若验证失败，回到测试租户修复后再次使用 `芋道源码 / admin / admin123` 复验；验证成功后删除当前支线 worktree。

## 范围

- 后端主 worktree：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- 前端主 worktree：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- 后端支线 worktree：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260528-signature-governance-docs\ruoyi-vue-pro`
- 前端支线 worktree：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260528-signature-governance-docs\yudao-ui-admin-vue3`
- 目标分支：`int_main`
- 支线分支：`codex/20260528-signature-governance-docs`

## Milestones

- [x] M1：创建合并验证任务文档。
- [x] M2：后端与前端支线合并进 `int_main`。
- [x] M3：重启主 worktree 前后端，确认加载最新代码。
- [x] M4：用 `芋道源码 / admin / admin123` 执行真实路径验证。
- [x] M5：如失败，回测试租户修复并再次回主租户验证。
- [x] M6：验证成功后删除支线 worktree。
- [x] M7：记录证据并提交本任务改动。

## BDD

BDD: merge branch into int_main -> Given 电子签名治理支线已通过 reviewer 放行, When 将后端和前端支线融合进 `int_main`, Then 主 worktree 必须包含支线最新代码且不覆盖无关用户改动。

BDD: restart before verification -> Given 主 worktree 已合并最新代码, When 执行主租户验证前, Then 必须先重启前后端，确保运行时加载的是合并后的最新代码。

BDD: verify with main tenant -> Given 前后端已重启, When 使用 `芋道源码 / admin / admin123` 进入电子签名治理路径, Then 验证结果必须来自真实前端路径和真实后端接口，不使用 mock 或静默降级。

BDD: fix via test tenant on failure -> Given 主租户验证失败, When 需要修改代码或数据准备, Then 必须回测试租户修复和验证，再回到 `芋道源码 / admin / admin123` 复验成功后才能放行。

## Expected Verification

- `git status` 确认合并范围。
- 后端/前端本地重启记录。
- Playwright 真实路径验证记录。
- 如发生修复，记录测试租户修复证据和主租户复验证据。
- 支线 worktree 删除结果。

## Current Status

completed

- 状态：completed
- 当前阶段：主租户验证已通过，支线 worktree 已删除，准备精确暂存并提交本任务改动。
- 已知情况：后端主 worktree 存在既有未跟踪 `runtime/`，本任务不得覆盖或提交；前端主 worktree 干净。

## Milestone Evidence

- M2：2026-05-28 在后端与前端主 worktree 均执行 `git merge --no-ff --no-commit codex/20260528-signature-governance-docs`，无冲突，保持未提交状态等待重启验证。
- M3：封装脚本 `script/deploy/restart-int-ruoyi-local.ps1 -Component full -WorktreeName int_main` 被无关孤立前端 worktree `showroom-hall-description-export` 阻塞；改用同端口同参数直接启动主线后端 `48081` 与前端 `8081`。
- M3：后端首次启动失败快停，原因是缺少 `dcc.signature.evidence` HMAC 配置；补入 `DCC_SIGNATURE_EVIDENCE_HMAC_SECRET=CODEX-DCC-E2E-HMAC-SECRET-20260526` 与 `DCC_SIGNATURE_EVIDENCE_KEY_VERSION=dcc-hmac-v1` 后重启成功，`GET http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`。
- M5：前端 Vite dev server 在 Windows 大仓验证中触发 `EMFILE: too many open files`；前端增加静态契约并关闭 AutoImport dev/build d.ts 生成后回归通过，但 dev server 仍受 Vite 文件句柄限制影响，最终使用 `pnpm build:local` 生成真实前端产物并以 Vite preview 在 `8081` 验证。
- M5：测试租户复验 `SIGNATURE_GOVERNANCE_E2E_BASE_URL=http://127.0.0.1:8081 SIGNATURE_GOVERNANCE_E2E_TENANT=测试租户 SIGNATURE_GOVERNANCE_E2E_USERNAME=aoteman SIGNATURE_GOVERNANCE_E2E_PASSWORD=admin123 node tests\e2e\signature-governance-policy.e2e.js`，结果 PASS。
- M4：主租户最终验证使用 `芋道源码 / admin / admin123` 登录 `http://127.0.0.1:8081/signature-governance`，页面显示 `电子签名治理` 与 `READY`，读取型策略接口返回 `status=READY`、`ready=true`，模块为 `DCC, EDHR, INTAUTH, SHOWROOM`，无 4xx/5xx `admin-api` 响应。
- M6：后端与前端支线 worktree 均已执行 `git worktree remove`；`Test-Path D:\ProjectPackage\Int\IntRuoyi\worktrees\20260528-signature-governance-docs\ruoyi-vue-pro` 与前端对应路径均返回 `False`，`git worktree list` 不再包含该支线。
- M7：`task-closeout-cleanup` 预览通过，后续仅清理本任务临时 `verification-report.md`，保留 `task.md` 与 `execution-log.md`。
