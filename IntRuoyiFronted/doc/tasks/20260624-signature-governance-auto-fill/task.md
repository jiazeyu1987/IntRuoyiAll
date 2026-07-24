# 任务：电子签名治理自动回填

## 任务目标

将电子签名治理页中需要真实 ID、Hash、对象 Key 的字段改为“从真实记录选择并自动回填”，减少人工手输，避免用户误填伪造或不一致的审计证据。

## 里程碑

- [x] M1：补充 RED 契约，要求页面提供真实候选加载和自动回填入口。
- [x] M2：确认后端是否已有候选接口；缺失时新增只读候选 API。
- [x] M3：前端接入候选接口，长期留存、周期复核、CSV质量包自动回填基础字段。
- [x] M4：运行前端契约、类型检查、后端相关测试。
- [x] M5：真实登录验证页面能加载候选并回填；无候选时显式提示；技术字段已禁止手填。
- [x] M5.1：将禁用输入框重构为来源卡、只读摘要和操作区，避免用户误以为需要手工填写。
- [ ] M6：记录证据、closeout 并分别提交前后端相关改动。

## 预期验证

- `node scripts\signature-governance-page-contract.test.mjs`
- `node tests\e2e\signature-governance-e2e-static.spec.js`
- `npm run ts:check`
- 后端候选 API 单元/契约测试
- Playwright 登录 `http://localhost:8081`，使用 `测试租户/aoteman/111111`，访问 `/signature-governance/retention`、`/periodic-review`、`/csv-package`，确认可从真实候选自动回填或显式提示无候选。

## 剩余阻塞

- 测试租户 `tenant_id=122` 的 `dcc_controlled_file_signature` 现有 242 条均为 `deleted=1`，正常分页接口不会返回；因此当前只能验证“无候选显式提示”，不能验证“选择候选后完整自动回填成功”。

## 当前状态

前端实现已完成，静态验证已通过；自动生成字段已从禁用输入框改为来源卡、只读摘要和操作区；测试租户仍缺少可用于完整回填成功验证的未删除 DCC 签名记录，本轮真实浏览器自动生成路径登录跳转超时，未计为通过。

## 前一任务检查

- 上一电子签名前端任务 `20260624-signature-authorization-state-chinese` 已完成并提交。
- 当前前端仓库存在其它任务脏改动；本任务提交时只纳入电子签名治理自动回填相关文件。

## 经验门禁

- `docs/login-access.md`：真实 E2E 默认本机 `http://localhost:8081`，使用测试租户 `测试租户/aoteman/111111`；登录失败不得静默切换租户或账号。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：治理页应保持紧凑操作台风格，避免嵌套卡片；状态、表格和操作应清晰可扫读。
- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`：已命中的经验门禁要求真实 E2E 前先记录 `experience-preflight`，并在日志中保留真实来源与阻塞说明。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。无真实候选时页面明确提示，不自动造数据。
- `是否从根因和长期维护角度解决`：是，从真实记录候选回填审计字段，减少手工误填。
- `是否存在临时补丁或绕过`：否。

## 真实验证

- 浏览器访问 `http://localhost:8081/signature-governance/retention`、`/signature-governance/csv-package`
- 断言 `责任人`、`DCC样本`、`eDHR样本`、`DCC对象Key`、`eDHR对象Key`、`恢复对象Key`、`Release ID`、`文档ID`、`追溯证据`、`培训ID`、`变更ID`、`签名证据` 均不再要求用户手填，以来源卡或只读摘要呈现
- 点击 `加载真实样本`、`加载eDHR归档样本`、`加载CSV来源样本` 后，页面触发真实接口调用并显式暴露来源缺失或空数据

## BDD 场景

- `BDD: 长期留存选择真实文件签名候选 -> Given 系统存在真实文件签名记录 / When 用户在长期留存页选择候选 / Then DCC回执来源ID、对象Key、版本ID、SHA256、证据Hash 自动回填，不要求用户手工编写。`
- `BDD: 周期复核选择真实投影样本 -> Given 系统存在真实签名记录 / When 用户在周期复核页选择样本 / Then 来源表、来源ID、来源Hash、动作、含义 自动回填。`
- `BDD: CSV质量包选择发布候选 -> Given 系统能提供真实签名治理候选 / When 用户在CSV质量包页选择候选 / Then Release ID、材料证据、追溯证据、QA签名证据 自动回填；缺候选时显式提示。`

## Cleanup Keep

- `doc/tasks/20260624-signature-governance-auto-fill/task.md`
- `doc/tasks/20260624-signature-governance-auto-fill/execution-log.md`
- `doc/tasks/20260624-signature-governance-auto-fill/frontend-feature-evidence.md`
