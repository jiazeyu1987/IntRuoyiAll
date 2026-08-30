# 表单模板 Word 导入自动识别与发布流程

## Request Summary And Source

- 请求来源：用户在 2026-08-28 明确确认新的导入发布流程。
- 请求内容：Word 导入时自动做规则识别；识别结果先生成草稿/待审批版本；审批流自动通过则自动发布，需要手动审核则审核后发布；最终使用发布出来的版本测试，不再手工点击“规则识别”。

## Current Baseline Reviewed

- 表单模板 Word 解析层已经具备代码规则识别能力，可识别数量、日期、选择项、签名等单元格类型，并生成 `cellRules` 与签名标记。
- 导入运行态已经存在版本草稿、升版审批和审批通过发布的服务边界。
- 填写配置里的手工规则识别仍是独立补识别入口；旧草稿缺少表格 rows 时会 fail fast 抛出 `Template schema rows are missing`。
- 当前用户验收口径不再要求手工点击该入口，而是以 Word 导入生成的版本为准。

## Classification

- 需求澄清：表单模板应和批记录表单一样在导入解析阶段完成代码规则识别，不依赖 AI。
- 交互修正：导入弹窗必须让用户知道导入后版本处于“已发布 / 待审批 / 草稿”的哪一种真实状态。
- 测试流程修正：真实 E2E 使用发布版本，不再把旧草稿的手工识别错误当作导入流程结果。

## Impact

- Product: 用户从导入 Word 开始即可得到带单元格规则的版本；发布后直接用于映射和填写验证。
- Frontend: 导入弹窗提示与成功 toast 按后端状态展示。
- Backend: 保持代码规则识别和审批发布链路；新增合同验证防止退回到 AI 或手工识别。
- Data: 不直接 SQL 修改模板规则；识别结果随版本正式保存。
- Tests: 增加导入自动识别状态提示静态合同和后端导入流程合同。

## Decision

- 接受该流程。
- 立即范围：锁定导入时自动识别、状态提示和后端合同验证。
- 禁止 fallback：导入失败不能自动切手工规则识别，AI/Codex CLI 也不是本流程依赖。

## Required Approvals

- 用户已回复“确认”和“开始处理”，授权在当前任务范围内修改代码并验证。
- Git 合并/推送另行按用户明确指令和当前工作区状态执行；不得混入无关脏改动。

## Downstream Skill Reruns

- `behavior-driven-development`：记录 BDD。
- `frontend-feature-delivery`：导入弹窗状态反馈。
- `backend-api-delivery`：导入识别与审批发布合同。
- `quality-assurance-test-suite`/Playwright：真实用户路径验证。

## Blockers And Next Action

- 当前结果：真实页面导入验证已通过，指定 Word 文件导入后自动识别并发布为 `33 / V21.0`，规则数 `145`、签名标记数 `42`。
- 下一步：如需提交/推送，只能选择本任务相关文件，不能混入当前工作区其它 DCC/MES/登录安全等并行改动。
