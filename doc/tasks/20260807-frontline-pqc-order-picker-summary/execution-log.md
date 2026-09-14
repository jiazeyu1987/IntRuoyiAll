# Execution Log

## User Intent

- “选择订单”弹框内每张订单卡片显示编码、产品、数量。
- 三项信息按三行排布，并缩小字体，确保长订单编码和产品名称完整显示。

## BDD Scenarios

BDD: 订单候选展示三行正式摘要 -> Given 活跃订单包含正式订单编码、产品名称和正数生产数量，When 一线 PQC 打开“选择订单”，Then 每张卡片按编码、产品、数量三行显示同一订单的数据，数量去除无意义小数尾零。

BDD: 长订单信息完整可见 -> Given 订单编码或产品名称较长，When 订单选择器在 1440x900、1920x1080 或 PQC 全屏状态显示，Then 三行文字允许换行且不使用省略号，所有值保持在当前卡片内，卡片之间不重叠。

BDD: 原有选择行为保持 -> Given 全部生产组长活跃订单已加载，When 用户搜索订单号并点击三行摘要卡片，Then 仍选择该正式订单、保持整卡选中态并关闭选择器。

## Command Intent

- 先新增最小静态合同并取得 RED。
- 再实现三行模板、同源候选数据和专用样式，运行同一合同取得 GREEN。
- 最后运行相邻回归、TypeScript 和真实 Playwright 只读验收。

## TDD Evidence

- RED: `node tests/e2e/mes-frontline-pqc-order-picker-summary-static.spec.cjs` -> FAIL，现有订单卡片只有 `option.label`，缺少订单对象、编码/产品/数量三行 DOM 与完整文字专用样式。
- GREEN: `node tests/e2e/mes-frontline-pqc-order-picker-summary-static.spec.cjs` -> PASS，卡片保留正式活跃订单对象并按编码、产品、数量三行展示，数量复用正式格式化方法，长文字无省略号或隐藏溢出。
- RED: 首轮真实截图复核 -> FAIL，选中卡片的深色背景已生效，但三行摘要自身的显式深色文字覆盖了按钮继承色，导致选中订单对比度不足；新增选中态文字颜色静态合同后按预期失败。
- GREEN: 将选中态覆盖收敛到 PQC 订单选择器作用域并显式设置三行标签和值为白色，聚焦合同重新 PASS。
- GREEN: `pnpm ts:check` -> PASS，无 TypeScript 错误。
- REGRESSION: 订单选择器布局、顶部订单产品摘要、全部活跃订单搜索、订单切换和 PQC 全屏静态合同 -> PASS。
- E2E PRECHECK: 官方 `scripts/preflight/login-preflight.mjs` -> PASS，身份 `芋道源码/admin`，目标路径 `/mes/pro/feedback/edhr-batch-pqc-fill`。
- E2E HARNESS: 首轮脚本在通知自动消失期间点击已脱离 DOM 的关闭图标而超时；改为等待正式通知生命周期结束后重跑，不改变产品页面行为。
- E2E: `node pqc-order-picker-summary-real.e2e.cjs` -> PASS，真实接口返回 11 条活跃订单；全部卡片的编码、产品、数量与接口逐条一致，`1440x900`、`1920x1080` 和 PQC 全屏均通过卡片边界、无重叠、无裁切、换行、15px 最大字号和选中态白色文字断言；PQC 写请求数为 0，pageerror 数为 0。
- VISUAL: 三张最终截图确认普通页面和全屏状态下三行摘要完整可见，选中卡片为深色背景白色文字。
- DIFF: `git diff --check` -> PASS，仅有 Git 行尾转换警告，无空白错误。
- EXPERIENCE: 将“父级 active 文字继承色可能被内部 label/value 的普通态显式 `color` 覆盖，必须检查子元素计算色”合并到现有 `docs/frontend-development.md#前端截图样式块静态契约门禁`，并更新经验索引。

## Milestone Status

- M1：completed。
- M2：completed。
- M3：completed。
- M4：completed。

## Closeout

- Frontend evidence validator PASS，关键结论已复制到 `verification-report.md`。
- `task-closeout-cleanup` preview 确认只删除本任务临时 Playwright 脚本和技能 evidence，保留三份核心文档、三张截图及结果 JSON；apply 状态为 `applied`，无 blocked 或 warnings。
- 未执行 Git 提交、合并或推送。

## Blockers

- None。
