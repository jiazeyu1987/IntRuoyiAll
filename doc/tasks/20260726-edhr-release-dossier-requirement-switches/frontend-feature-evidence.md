# Frontend Feature Evidence

## Scope

- 个人中心“配置”页签继续仅对金手指权限可见。
- 在记录本全局开关旁新增 eDHR 放行资料限制卡片，包含来料检报告、灭菌报告、成品检报告、成品检记录限制 4 个 switch。
- 每次 switch 保存前弹确认框；取消或接口失败回滚完整旧配置；接口失败展示后端错误。
- 放行预检/追溯展示新增资料限制检查项和 `DOSSIER` 分类映射。

## Acceptance

- 金手指用户能在个人中心配置页签看到 4 个资料限制开关；普通用户看不到配置页签。
- switch 保存前必须确认；取消或接口失败必须回滚到原完整配置。
- 接口失败显示后端错误，不静默降级或默认成功。
- 放行预检/追溯能展示新增资料限制检查项、分类和特殊节点附件来源。

## BDD

- `BDD: 金手指配置可见性 -> Given 金手指用户 / When 打开个人中心配置页签 / Then 可看到 4 个资料限制开关；普通用户不可见配置页签。`
- `BDD: 打开后阻止无资料放行 -> Given 某资料限制打开 / When 对应特殊节点未完成或无已保存附件 / Then 放行预检生成 BLOCKER 且提交放行失败。`

## RED

- `RED: node tests/e2e/edhr-release-dossier-requirement-setting-static.spec.js -> FAIL`，缺少 `releaseDossierRequirementSetting.ts` API wrapper、Profile 配置卡片和展示映射。

## Verification

- `GREEN: node tests\e2e\edhr-release-dossier-requirement-setting-static.spec.js -> PASS`。
- `GREEN: pnpm ts:check -> PASS`。
- `GREEN: node tests\e2e\edhr-release-check-result-chinese-static.spec.js -> PASS`。
- `GREEN: node tests\e2e\edhr-release-dialog-copy-cleanup-static.spec.js -> PASS`。

## E2E

- `GREEN: node scripts/preflight/login-preflight.mjs --base-url http://127.0.0.1:8081 --target-path /user/profile --target-text 个人工作台 -> PASS`，本机默认身份标签 `芋道源码/admin` 能真实登录个人中心。
- `GREEN: node --check tests\e2e\edhr-release-dossier-requirement-setting-real.e2e.js -> PASS`，任务专用真实 E2E 脚本已落地并通过语法检查。
- `GREEN: node tests\e2e\edhr-release-dossier-requirement-setting-real.e2e.js -> PASS`，使用隔离 worktree slot 5（8086/48086）真实打开个人中心配置页，确认 4 个 switch 和配置 hash 可见。
- `GREEN: UI toggle + API verify -> PASS`，通过页面确认框打开“来料检报告”，API 复核 `incomingInspectionReportRequired=true` 且配置 hash 变化。
- `GREEN: UI restore + DB verify -> PASS`，`finally` 通过页面恢复原始全 false 配置，并用本机 Docker MySQL 只读复验 `infra_config` 值已恢复。
- `GREEN: node tests\e2e\edhr-release-dossier-requirement-setting-real.e2e.js -> PASS`，用户授权替换 `48081` 旧运行态后，使用 `8081/48081` 当前用户路径完成真实页面配置、UI 切换、API 复核和 UI 恢复。

## Closeout Notes

- 真实 E2E 证据文件：`doc/tasks/20260726-edhr-release-dossier-requirement-switches/e2e-artifacts/dossier-requirement-setting-real/result.md`。
- 旧 `48081` 仍属于并行运行态，本任务未停止；本次 E2E 使用登记端口 8086/48086 完成隔离验证。
- 用户已授权停止旧 `48081` 运行态；当前 `48081` 已由任务 Jar PID `57744` 承载，`8081/48081` 真实 E2E 已 PASS。
