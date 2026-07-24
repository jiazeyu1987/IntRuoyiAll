# 任务：运行控制台构建、测试服部署、标记测试通过、上线备份服

## Goal

严格通过本机运行控制台 UI 完成一次发布链路：构建只发代码且勾选 OnlyOffice 的发布包 A，部署到测试服务器，标记测试通过，将同一发布包上线到备份服务器，然后选择测试服立即备份并恢复数据到测试服。

## Scope

- 使用本机管理端运行控制台页面操作，不用 CLI 替代 UI 行为。
- 构建发布包时选择只发代码，并按页面要求勾选 OnlyOffice。
- 测试服目标为 `172.30.30.58`。
- 备份服目标为 `172.30.30.59`。
- 上线备份服前必须完成标记测试通过。
- 上线备份服成功后，通过 UI 选择测试服执行立即备份。
- 测试服立即备份成功后，通过 UI 选择测试服恢复数据。
- 任何步骤失败时，修复后从构建发布包重新开始验证。

## Non-Scope

- 重要：禁止修改正式服务器 `172.30.30.57` 的程序和数据。
- 不登录、不发布、不重启、不验证正式服务器。
- 不用直接 CLI 发布结果替代运行控制台 UI 操作结果。
- 不使用 mock、跳过、fallback 或关闭校验来伪造成功。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；运行控制台操作失败即记录失败并修复。
- `是否从根因和长期维护角度解决`：是；若 UI、后端命令拼接或发布脚本存在缺口，按正式链路修复。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: UI 构建发布包 A -> Given 登录本机运行控制台 / When 选择只发代码、勾选 OnlyOffice 并确认构建 / Then NAS 上生成发布包 A。
- BDD: UI 部署发布包 A 到测试服 -> Given 发布包 A 已生成 / When 在运行控制台选择部署到测试服 / Then `172.30.30.58` 部署成功并可访问。
- BDD: UI 标记测试通过 -> Given 发布包 A 已部署到测试服 / When 填写验证结论并点击标记测试通过 / Then 发布包 A 状态允许上线备份服务器。
- BDD: UI 上线备份服务器 -> Given 发布包 A 已标记测试通过 / When 在运行控制台选择上线备份服务器并确认 / Then `172.30.30.59` 部署成功并可访问。
- BDD: UI 立即备份测试服 -> Given 发布包 A 已上线备份服务器 / When 在运行控制台选择立即备份且目标为测试服 / Then 生成测试服备份点。
- BDD: UI 恢复数据到测试服 -> Given 测试服备份点已生成 / When 在运行控制台选择恢复数据且目标为测试服 / Then 测试服数据恢复成功并健康检查通过。
- BDD: 禁止正式服务器变更 -> Given 任意发布步骤 / When 执行运行控制台动作 / Then 不对 `172.30.30.57` 执行发布、重启、写入或验证修改。

## Milestones

- [x] M1：确认本机运行控制台、登录和环境边界。
- [x] M2：UI 构建发布包 A。
- [x] M3：UI 部署发布包 A 到测试服。
- [x] M4：UI 标记测试通过。
- [x] M5：UI 上线备份服务器。
- [x] M6：UI 选择测试服立即备份。
- [x] M7：UI 选择测试服恢复数据。
- [x] M8：验证前后端修复与目标环境健康。
- [x] M9：收尾清理预览。

## Expected Verification

- Playwright 证明确实通过本机运行控制台完成点击路径。
- NAS 上存在发布包 A。
- 测试服 HTTP 验证通过。
- 标记测试通过状态可追踪。
- 备份服 HTTP 验证通过。
- 测试服备份点可追踪。
- 测试服恢复操作状态为成功。
- 任务日志记录所有 RED/GREEN 证据。

## Current Status

completed

## Current Release Package A

- ReleaseTag: `20260607_ui_code_only_onlyoffice_A_043314`
- Publish scope: `code-only`
- OnlyOffice: included
- NAS path: `Backup/ReleasePackage/20260607_ui_code_only_onlyoffice_A_043314`
- Backup point: `20260607-050200`

## Final Verification

- Full UI artifact: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260607-runtime-console-current-release-refresh\artifacts\runtime-console-full-goal-result.json`
- Full UI stdout: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260607-runtime-console-current-release-refresh\artifacts\runtime-console-full-goal-20260607_ui_code_only_onlyoffice_A_043314.stdout.log`
- Full UI stderr: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260607-runtime-console-current-release-refresh\artifacts\runtime-console-full-goal-20260607_ui_code_only_onlyoffice_A_043314.stderr.log`，长度 0。
- Operation IDs:
  - `build-release`: `f45a3095-a28c-423c-94cf-e2257e2120f5`
  - `publish-test`: `c035f04c-f8db-4555-b9fc-c6c0b3307056`
  - `mark-release-tested`: `404bea05-1954-40c1-b7a1-0fdadd8e9e30`
  - `promote-backup`: `41455001-0c1e-44e0-8ef1-9dc4d7ab6cf6`
  - `backup-now` 测试服: `599de693-0aec-414d-9362-cc3b37a1971f`
  - `restore-data` 测试服: `21336b5c-a5b2-4bd0-8e65-f61536e37f41`
- Independent HTTP health sweep: `172.30.30.58` 与 `172.30.30.59` 的 `48081/actuator/health`、`8081/`、`8080/healthcheck`、`8083/` 均返回 HTTP 200。
- Backend regression: `python -m pytest script\tests\test_restart_int_ruoyi_local_schema.py script\tests\test_runtime_control_scripts.py -q` -> PASS，21 passed。
- Production boundary: 本次 UI 驱动未提交 `promote-prod`，未提交 `targetEnvironment=prod`；未对正式服务器 `172.30.30.57` 提交发布、重启、写入或恢复动作。
- Cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260606-runtime-console-build-test-backup-release --mode preview` -> `status: ready`，`blocked: <none>`。
