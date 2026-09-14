# 表单模板打开填写绑定修复

## Task Goal

修复表单中心/表单模板预览区点击“打开”和“填写”时误提示“当前模板未绑定批记录表单，无法执行该操作”的问题，确保通用 FormCenter 模板使用本页查看与模拟填写流程。

## Milestones

- [x] 建立问题复现与根因定位
- [x] 编写并记录最小回归测试 RED
- [x] 实施最小前端修复
- [x] 运行 GREEN 与相关回归验证
- [x] 完成收尾与经验沉淀

## Expected Verification

- 聚焦静态合同先 RED 后 GREEN。
- `pnpm ts:check` 通过或记录无关阻塞。
- “打开/编辑/填写”均不依赖批记录绑定；批记录专属页面仍保留自己的 `reportId` 前置条件。

## Current Status

completed

## 经验门禁

- 前端静态契约隔离门禁：本任务使用聚焦静态合同覆盖当前按钮行为。
- 表单模板编辑与批记录绑定动作边界门禁：通用 FormCenter 模板动作不得被批记录绑定状态拦截。
- Windows 换行与脚本行为同步门禁：修改 `tests/e2e/*static.spec.js` 后必须运行目标静态合同。
- 脏工作区基线门禁：任务开始时工作区存在并行脏改动，基线提交 `9878db8e` 已保存其中一批既有改动；仍有并行任务继续写入，当前任务只选择性提交本任务文件。
- 经验沉淀门禁：已将“打开/编辑/填写”均不得依赖批记录绑定的通用规则合并到现有 `docs/frontend-development.md`；`docs/experience-index.md` 已有错误文本路由，无需新增重复入口。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，复用页面已有通用模板查看与模拟填写组件，移除错误的批记录绑定耦合。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260727-form-template-open-fill-binding/bug-regression-evidence.md
