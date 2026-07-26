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

- 未执行真实 Playwright E2E：本次未启动本机 `8081/48081` 运行态，也未切换真实全局开关；按全局开关类 E2E 恢复门禁，缺运行态/测试数据时不以 API-only 或 mock 替代。

## Blockers

- 真实 Playwright E2E 未执行，缺当前任务可安全切换并恢复的本机运行态、登录态与测试批次数据确认。
