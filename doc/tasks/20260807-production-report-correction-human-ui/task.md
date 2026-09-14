# 生产报工修改弹窗业务化改造

## Task Goal

将生产组长“报工管理”的“修改报工内容”弹窗从内部 JSON/ID 协议表单改造为可直接操作的业务表单；服务端使用当前登录人和电子签名密码生成修订签名与审计身份，前端自动生成修改后 payload 和字段差异。

## Milestones

- [x] M1 根据用户截图复现并定位不可用界面与审计身份风险。
- [x] M2 记录业务化交互 BDD 和聚焦 RED 测试。
- [x] M3 完成后端当前登录人签名契约和前端业务表单。
- [x] M4 完成定向测试、类型检查和真实页面验证。
- [x] M5 完成证据归档和任务收尾。
- [ ] M6 增加面向业务人员的修改日志查询与时间线展示。
- [ ] M7 完成日志接口、页面状态、响应式真实路径验证和再次收尾。

## Expected Verification

- 弹窗不再显示或要求输入 payload JSON、字段变更 JSON、签名快照 JSON、用户 ID、签名 ID。
- 弹窗展示报工上下文、可修改业务字段、修改前后对比、修改原因和当前用户电子签名密码。
- 前端只根据用户实际修改生成字段级差异；没有变化时禁止提交。
- 服务端忽略客户端身份声明的可能性，直接使用当前登录用户校验密码、生成新签名和签名快照。
- 影响数量片段的字段继续携带正式数量片段身份，并保留 FIFO 锁定校验。
- 聚焦静态合同先 RED 后 GREEN，后端单元测试、前端 `pnpm ts:check` 和真实页面截图验证通过。
- 报工列表提供“修改记录”入口；日志按时间倒序展示修改人、修改时间、修改原因、电子签名状态和逐字段前后值。
- 日志接口和页面均不暴露事件号、修订号、用户号、签名号、字段代码、原始 payload 或签名快照 JSON。
- 日志查询具备加载、空记录、失败重试和移动端可读状态。

## Current Status

in_progress - 用户补充要求修改日志也必须面向人展示，正在增加正式日志查询接口和可读时间线。

## Applicable Experience Gate

- 已读取 `docs/experience-index.md`；适用 `docs/frontend-development.md` 的按钮行为一致性、静态合同隔离和类型检查门禁。
- 已读取 `docs/backend-development.md`；服务端接收业务字段，登录身份、签名、字段差异和审计快照由服务端生成。
- 已读取 `docs/e2e-rules.md` 与 `docs/login-access.md`；真实页面仅使用本机 `8081/48081` 和 `芋道源码/admin` 做只读验收，不在 admin 基线写入数据。
- 历史报工继续绑定提交事件中的路线/工序快照；路线发布新版本不重写历史事件，原损耗原因快照可继续显示，新增配置不反向污染历史记录。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，业务字段、审计身份和签名生成分别由正式层负责。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- `doc/tasks/20260807-production-report-correction-human-ui/correction-dialog-desktop.png`
- `doc/tasks/20260807-production-report-correction-human-ui/correction-dialog-mobile.png`
- `doc/tasks/20260807-production-report-correction-human-ui/correction-dialog-mobile-signature.png`
