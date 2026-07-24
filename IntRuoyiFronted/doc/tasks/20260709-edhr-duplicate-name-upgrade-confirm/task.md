# eDHR 同名导入升版确认

## 任务目标

- 批记录 Word 导入发现同路线同名批记录时，必须先明确询问用户是否升版。
- 用户选择“否/放弃本次导入”时，立即终止本次导入任务，不调用 `recognizeUploadedRoute`，不写入任何导入数据。
- 用户确认升版时，才以 `upgrade=true` 继续真实导入链路。

## 里程碑

- [x] 明确 BDD 场景和验收标准。
- [x] 先补静态契约测试，覆盖取消同名升版终止导入。
- [x] 最小修改导入确认文案和取消分支。
- [x] 跑相关前端契约测试并记录证据。

## 预期验证

- `node scripts/electronic-batch-record-word-import.test.mjs`
- `node scripts/edhr-duplicate-name-upgrade-confirm.test.mjs`
- `node scripts/edhr-batch-version-phase1-contract.test.mjs`

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，入口层在写入前完成同名升版选择，后端仍保留 `upgrade=false` 拒绝同名写入的保护。
- 是否存在临时补丁或绕过：否。

## 经验门禁

- PowerShell/Windows shell：已读取 `docs/powershell-memory.md`，中文读写使用 UTF-8。
- 前端交付：已读取 `frontend-feature-delivery` 和 `frontend-contract.md`，本任务以页面行为和静态契约验证为主。
- 登录/E2E：已读取 `docs/login-access.md`；本次变更先做静态契约，不执行写入型真实 E2E。

## 当前状态

- 状态：已完成。
- 阻塞：无。

## 最终验证

- `node scripts\edhr-duplicate-name-upgrade-confirm.test.mjs` -> PASS。
- `node scripts\edhr-batch-version-phase1-contract.test.mjs` -> PASS。
- `node scripts\electronic-batch-record-word-import.test.mjs` -> PASS。
- `node tests\e2e\edhr-duplicate-name-upgrade-cancel-real-flow.e2e.js` -> PASS，测试租户真实同名导入选择“否，放弃本次导入”，写接口请求数 `0`。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260709-edhr-duplicate-name-upgrade-confirm\frontend-feature-evidence.md` -> PASS。
