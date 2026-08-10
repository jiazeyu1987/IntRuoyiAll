# Task: 批记录测试列表列与行操作调整

## Goal

批记录测试独立页签下的生产组长、一线PQC、一线生产三张列表不再展示“测试项名称”列；每行操作区新增“修改”和“删除”，其中修改只允许编辑该行描述，删除移除当前行。

## Milestones

- [x] M1：锁定现有批记录测试页表格结构、用户列池和行级操作契约。
- [x] M2：补充 RED 静态合同，覆盖移除测试项名称列、修改描述和删除行。
- [x] M3：实现三张列表共享的描述编辑与行删除交互。
- [x] M4：运行目标静态合同、TypeScript 或可用前端验证，并记录结果。
- [x] M5：完成收尾记录，进入 ready_for_closeout 或 completed。

## Expected Verification

- `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs`
- `pnpm ts:check`（若历史或环境阻塞，记录首个阻塞原因和影响）
- `git diff --check -- IntRuoyiFronted\src\views\mes\pro\edhr-batch\BatchRecordTestPage.vue IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs doc\tasks\20260808-batch-record-test-list-actions`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260808-batch-record-test-list-actions/frontend-feature-evidence.md`

## Applicable Gates

- 前端静态契约隔离门禁：目标行为使用当前页面专用静态合同锁定，避免依赖无关全量检查。
- 多角色共享表格列池隔离门禁：三张列表的默认列池和渲染列必须同步移除 `caseName`，不能只隐藏 DOM。
- 表格行定位门禁：行修改/删除必须按当前行对象执行，编辑态保存后仍能按目标文本重新定位。
- Strict No-Fallback：不引入测试项名称列的隐藏 fallback，不用默认描述或吞异常掩盖失败。

## Current Status

completed：实现、目标静态合同、TypeScript、diff check、列名复核、frontend-feature evidence 校验、cleanup preview/apply 和经验沉淀判断均已完成；未执行 Git 提交，符合当前项目默认不提交策略。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，计划同步调整列池、模板列和行级状态。
- `是否存在临时补丁或绕过`：否。
