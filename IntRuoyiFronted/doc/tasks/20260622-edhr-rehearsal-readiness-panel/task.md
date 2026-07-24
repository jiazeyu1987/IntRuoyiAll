# 任务：eDHR 演练预检前端面板

## 任务目标

- 在 eDHR 批次执行操作台提供正式的“演练预检”入口。
- 接入后端只读 `rehearsal-readiness` API，让用户在真实演练前看到菜单、签名、BPM、路线、权限范围和模板规则缺口。
- 不新增测试专用控件，不 mock 成功，不自动修复租户数据。

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260622-smart-scheduling-smoke-xlsx-dependency-fix\task.md`
- 状态：`completed`
- 处理：上一任务已完成，不阻塞本次 eDHR 前端机制补齐。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 前端页面遵循 IntPP 生产工单列表风格，使用紧凑操作台、白色表面、蓝/中性色、表格化状态展示。
  - 前端请求失败必须暴露真实错误，不得静默吞掉或显示假成功。
  - 本切片只接入只读预检，不写真实租户数据，不自动修复 BPM、模板或权限配置。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，把后端只读预检能力显式放到演练入口前。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 用户可从批次执行页启动演练预检 -> Given 用户有 eDHR 批次执行查询权限 / When 打开批次执行页 / Then 页面提供“演练预检”入口且不依赖隐藏脚本。`
- `BDD: 预检参数缺失必须前端阻塞 -> Given 路线 ID 或三类责任人 ID 缺失 / When 用户点击开始预检 / Then 前端显示明确错误且不调用后端。`
- `BDD: 后端预检结果必须可见 -> Given 后端返回 PASS 或 BLOCKED / When 预检完成 / Then 页面展示 overallStatus、blocker/pass 数量、每项 code、角色、对象、消息和建议。`
- `BDD: 预检失败必须暴露真实错误 -> Given 后端接口失败 / When 预检请求失败 / Then 页面显示真实错误信息，不清空为假通过。`

## 里程碑

1. M1：创建任务包与 RED 静态合同。`DONE`
2. M2：接入 API 类型和批次执行页预检对话框。`DONE`
3. M3：运行静态合同、类型检查和证据校验。`DONE`
4. M4：收尾清理预览并提交。`IN_PROGRESS`

## 预期验证

- `node tests/e2e/edhr-rehearsal-readiness-panel-static.spec.js`
- `node --max-old-space-size=8192 node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260622-edhr-rehearsal-readiness-panel\frontend-feature-evidence.md`

## 当前状态

`COMPLETED`

已在 eDHR 批次执行页新增只读“演练预检”对话框，用户可输入路线与三类责任人 ID，调用真实 readiness API 并查看 PASS/BLOCKER 明细。

## Cleanup Keep

- `doc/tasks/20260622-edhr-rehearsal-readiness-panel/frontend-feature-evidence.md`
