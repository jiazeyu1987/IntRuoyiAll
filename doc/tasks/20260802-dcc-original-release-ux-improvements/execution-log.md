# 20260802 DCC 原版发布前端体验优化 Execution Log

## User Intent

- 用户要求修复 DCC 原版发布链路中前端操作和业务流程体验问题。
- 范围限定为当前 DCC 原版发布体验，不顺手修其它场景，不通过 API/SQL 改业务状态，不使用 admin 账号执行业务路径。

## Rules Read

- `AGENTS.md`
- `docs/frontend-development.md`
- `docs/e2e-rules.md`
- `docs/login-access.md`
- `docs/task-closeout-rules.md`
- `docs/powershell-encoding.md`
- `frontend-feature-delivery` skill and `references/frontend-contract.md`
- `behavior-driven-development` skill

## BDD

- BDD: 上传预览错误可解释 -> Given 上传人位于 DCC 原版上传页, When 上传预览接口返回文件存储不可用、文件格式错误、权限不足或编号重复, Then 页面在上传区域展示对应业务原因和处理建议, And 不把所有失败都显示为通用“系统异常”。
- BDD: 文件编号提前唯一性校验 -> Given 上传人填写文件编号, When 文件编号变更并失焦或触发校验, Then 页面查询当前版本/编号占用状态并显示“将创建 master 主档”或“编号已存在”提示, And 提交前阻止重复编号。
- BDD: 上传前权限前置提示 -> Given 上传人进入上传页, When 分类、项目代码或文件类型列表为空或无上传权限, Then 页面在对应控件旁显示明确缺口, And 不等到提交后才暴露权限问题。
- BDD: 审批详情处理态清晰 -> Given 审批人从 DCC 审批中心待办进入文件详情, When 当前账号拥有待处理审批任务, Then 页面展示“待我审批/签名处理态”标识、当前节点、签名按钮和处理说明, And 只读 viewer 展示为“只读预览”。
- BDD: 签名弹窗业务提示增强 -> Given 审批人点击签名/审批动作, When 签名弹窗打开, Then 弹窗展示当前节点、签名人、签名动作、提交后流转说明和电子签名审计提示。
- BDD: 审批进度固定可视化 -> Given 用户查看文件详情, When 上传审批存在或尚未完成, Then 页面固定展示文控审核、会签审核、会签批准、文控批准四级轨迹，包含处理人、时间和签名状态。
- BDD: 发布后直达受控浏览 -> Given V1.0 原版文件已发布生效, When 用户在文件详情查看 ACTIVE 文件, Then 页面提供“查看受控浏览当前有效版”按钮并跳转到按文件编号定位的受控浏览页。
- BDD: 当前有效版标识突出 -> Given 用户查看详情或受控浏览列表, When 当前文件是 master 当前生效版本且状态 ACTIVE, Then 页面突出显示“当前有效版 / ACTIVE / V1.0”标识，避免与草稿、历史或升版记录混淆。
- BDD: 审批待办展示 DCC 关键信息 -> Given 审批人查看 DCC 待办列表, When 待办属于受控文件原版发布, Then 待办行展示文件编号、版本、文件类型、当前审批节点和申请人。

## Verification Evidence

- RED: `node tests/e2e/dcc-original-release-ux-improvements-static.spec.js` -> FAIL, missing `resolveUploadPreviewErrorMessage`; 上传预览仍只透传通用错误，无法区分文件存储、格式、权限和编号重复。
- GREEN: `node tests/e2e/dcc-original-release-ux-improvements-static.spec.js` -> PASS, 覆盖 9 项 DCC 原版发布 UX 要求。
- GREEN: `node tests/e2e/dcc-upload-current-version-static.spec.js` -> PASS, 文件编号/当前版本预检查相邻合同通过。
- GREEN: `node tests/e2e/dcc-approval-center-handling-entry-static.spec.js` -> PASS, 审批中心 DCC 处理态入口相邻合同通过。
- GREEN: `node tests/e2e/dcc-browser-version-summary-static.spec.js` -> PASS, 受控浏览版本摘要相邻合同通过。
- GREEN: `node tests/e2e/dcc-detail-handling-summary-static.spec.js` -> PASS, 详情处理摘要相邻合同通过。
- GREEN: `pnpm ts:check` -> PASS, 前端 relaxed TypeScript 检查通过。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260802-dcc-original-release-ux-improvements/frontend-feature-evidence.md` -> PASS, 前端证据结构校验通过。
- Verification: 未运行真实审批/签名写入型 Playwright E2E；本任务只做前端 UX 修复，未使用 API/SQL 修改 DCC 文件状态、审批状态或签名证据。

- Experience consolidation: checked existing `docs/local-runtime.md`, `docs/frontend-development.md`, and `docs/task-closeout-rules.md`; DCC upload-preview MinIO, upload category permission, and evidence cleanup gates already cover reusable lessons, so no new long-term experience document was created.

## Blockers

- Closeout blocker: 当前工作区已有大量非本任务脏改动且 `int_main` ahead `origin/int_main`；cleanup apply 已无删除项通过，未执行基线提交、实现提交或 push，避免混入其它任务改动。
