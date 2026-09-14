# Bug Regression Evidence

## Bug Summary And Expected Behavior

- Bug: 表单模板 `打开 / 编辑 / 填写` 均先校验批记录绑定，普通模板提示“当前模板未绑定批记录表单，无法执行该操作”。
- Expected: 三个按钮分别打开当前模板查看、规则编辑和模拟填写工作区，与批记录表单没有数据依赖。

## Reproduction Command Or Path

- Path: 登录本机 `http://127.0.0.1:8081/mdm/form-center/template`，选择模板并点击三个按钮。
- Deterministic contract: `node tests\e2e\form-template-independent-button-actions-static.spec.js`。

## Root Cause

- “按批记录表单行为对齐”被错误解释为“FormCenter 模板必须绑定 MES 批记录 reportId”。
- 前端因此增加 `BOUND + reportId` 校验和 MES 路由；前后端契约及迁移又为该错误路径增加七个绑定字段。
- 页面原有当前模板查看、编辑和模拟填写工作区没有缺失，错误来自跨域数据绑定设计。

## Regression Tests

- Frontend: `form-template-independent-button-actions-static.spec.js`。
- Backend: `FormCenterTemplateIndependenceContractTest`。
- Database contract: `test_form_template_batch_record_independence.py`。
- Real E2E: Playwright 通过本机 Chrome 依次点击三个按钮。

## RED

- `RED: node tests\e2e\form-template-independent-button-actions-static.spec.js -> FAIL, 打开仍使用批记录设计器且三个按钮依赖绑定。`
- `RED: mvn.cmd -pl yudao-module-bpm "-Dtest=FormCenterTemplateIndependenceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, VO/DO/runtime 仍暴露绑定字段。`
- `RED: python -X utf8 -m pytest script\tests\test_form_template_batch_record_independence.py -> FAIL, 错误迁移和字段仍存在。`

## GREEN

- `GREEN: node tests\e2e\form-template-independent-button-actions-static.spec.js -> PASS。`
- `GREEN: mvn.cmd -pl yudao-module-bpm "-Dtest=FormCenterTemplateIndependenceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 2 tests。`
- `GREEN: python -X utf8 -m pytest script\tests\test_form_template_batch_record_independence.py -> PASS, 2 tests。`
- `GREEN: real Playwright E2E -> PASS, 三个 FormCenter 弹窗可见、无绑定错误、pathname 不变。`

## Verification

- 聚焦前端静态合同、BPM 合同和数据库独立性合同均通过。
- 本机 Playwright 使用真实登录和已安装 Chrome 完成三个按钮页面点击。
- 三个动作均停留在 `/mdm/form-center/template`，未出现批记录绑定错误。

## Risk And Regression Scope

- 风险集中在模板三个按钮、模板池类型和 BPM 模板字段。
- 批记录表单页面自身未修改。
- 本地数据库冗余列未删除，避免未经授权的破坏性变更。

## Blockers And Follow-Up Actions

- 当前缺陷修复无 blocker。
- 冗余列物理清理需要独立迁移审计和用户授权。
