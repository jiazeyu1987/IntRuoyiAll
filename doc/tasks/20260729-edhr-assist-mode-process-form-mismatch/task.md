# 20260729 edhr assist mode process form mismatch

## Task Goal

修复 eDHR 批记录填写页中“填写辅助模式”未按当前粗洗工序加载已配置辅助模式表单的问题，确保辅助模式 UI 使用当前工序绑定的辅助表单配置，不混用其它表单来源。

## Milestones

- [ ] 建立缺陷复现和 BDD 场景。
- [ ] 定位辅助模式表单来源、工序切换上下文和现有测试边界。
- [ ] 先补充失败回归测试，再实现最小修复。
- [ ] 运行目标验证与相邻回归，记录 RED/GREEN/REGRESSION 证据。
- [ ] 完成收尾、经验沉淀、清理、提交与推送。

## Expected Verification

- 目标静态或单元回归能够先 RED 后 GREEN，证明粗洗工序辅助模式读取当前工序配置的辅助表单。
- 相邻 eDHR 辅助模式、FormCenter 表单槽位或批记录填写相关静态合同通过。
- 如本地真实前后端与账号数据齐备，使用 Playwright 走真实批记录填写页核对辅助模式 UI。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：待定位后确认；若正式数据链路缺失则阻塞。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

### 工艺路线三类配置术语契约

- Trigger: 批记录、批记录表单、表单槽位、formBindings、工序开始或批次执行辅助表单相关修改。
- Preflight check: 先区分工序开始、工序设置批记录表单、表单槽位三条独立配置链路，确认当前缺陷属于辅助模式表单链路还是正式批记录表单链路。
- Blocker: 接口或快照缺少正式配置来源时必须阻塞并补齐数据链路，不得使用 `formBindings`、默认 `MAIN`、当前登录人或其它特殊配置推断。
- Verification: 测试分别证明当前工序辅助模式表单来源与其它表单链路不混用。
- Forbidden action: 禁止用前端文案、空值、默认槽位、formBindings 或特殊节点配置掩盖来源缺失。
- Evidence: `AGENTS.md#工艺路线三类配置术语契约`。

### 前端静态契约隔离门禁

- Trigger: 目标前端行为需要 RED/GREEN，但既有宽合同或全量 `pnpm ts:check` 可能先失败于历史问题。
- Preflight check: 先运行最接近的既有契约；若失败点不属于当前任务，新增任务专用最小静态契约覆盖当前行为。
- Blocker: 无法证明失败点与当前任务无关，或专用契约不能稳定先 RED 后 GREEN。
- Verification: 记录无关 blocker、专用契约 RED/GREEN 和全量回归剩余阻塞摘要。
- Forbidden action: 禁止修改无关大契约来绕过历史失败。
- Evidence: `docs/frontend-development.md#前端静态契约隔离门禁`。

### FormCenter 动态表单字段码渲染门禁

- Trigger: eDHR 批次详情、动态表单抽屉、损耗单、过程检验记录、辅助模式表单字段渲染、`fieldCode`、`fieldIdentityMap`。
- Preflight check: 区分 FormCenter 正式字段键和电子表格坐标；打开动态表单时必须加载精确模板版本、实例快照和规则。
- Blocker: 已有实例数据只显示快照 JSON、控件按错误 key 读取为空、或动态表单被误导到传统批记录链路。
- Verification: 静态合同或页面验证证明目标表单控件按当前工序辅助表单配置渲染。
- Forbidden action: 禁止用空布局、传统批记录 execution 或坐标兼容写入冒充通过。
- Evidence: `docs/frontend-development.md#FormCenter 动态表单字段码渲染门禁`。
