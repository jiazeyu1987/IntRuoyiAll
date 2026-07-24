# eDHR 待处理节点标签移到右侧栏

## 任务目标

将 eDHR 批次详情页左侧待处理工序卡片中的槽位状态标签和缺失配置提示，移动到右侧当前工序摘要栏中，匹配截图中“蓝框内容放到红框位置”的布局要求。

## 里程碑

- [x] 创建任务目录并记录任务目标
- [x] 按 BDD + 严格 TDD 补充静态契约并观察 RED
- [x] 修改批次详情页布局与样式
- [x] 运行静态契约、类型/语法验证并记录证据
- [x] 完成任务文档并提交本任务改动

## 预期验证

- 左侧待处理工序卡片只保留排序、工序名称和点击选择能力，不再展示槽位状态标签或缺失配置提示。
- 右侧当前工序摘要栏在选中待处理工序时展示槽位状态标签和缺失配置提示。
- 不改变后端 API、权限判定、待办动作、数据源或错误展示。
- 不引入 fallback、降级、吞异常或临时绕过。

## 当前状态

已完成。新增静态契约通过，`vue-tsc` 在项目大内存参数下通过；原有 `edhr-batch-pending-form-entry-static.spec.js` 暴露当前工作区已有审批动作契约差异，本次未修改审批/权限逻辑。

## Current Status

completed. Layout change, static contract, frontend evidence validation, and relaxed type check are complete.

## 验证结果

- RED：`node tests/e2e/edhr-batch-pending-slot-tags-right-rail-static.spec.js` -> FAIL，左侧待处理工序卡片仍展示槽位状态标签或缺失配置提示。
- GREEN：`node --check tests/e2e/edhr-batch-pending-slot-tags-right-rail-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/edhr-batch-pending-slot-tags-right-rail-static.spec.js` -> PASS。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd exec vue-tsc --noEmit -p tsconfig.relaxed.json --pretty false` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260708-edhr-pending-slot-tags-right-rail/frontend-feature-evidence.md` -> PASS，证据文件随后按收尾清理规则删除。
- GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-edhr-pending-slot-tags-right-rail --mode apply` -> PASS，仅删除本任务临时证据文件。
- BLOCKER：`node tests/e2e/edhr-batch-pending-form-entry-static.spec.js` -> FAIL，失败于“审核人和批准人的待办必须显示对应动作并进入审批页”既有契约断言；本次 diff 未触碰审批动作逻辑。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`，本任务中文读写优先使用 `apply_patch` 和 UTF-8 显式读取验证。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，本次仅调整既有 eDHR 详情页信息摆放，不引入无关视觉重设计。
- 前端交付契约：已读取 `frontend-feature-delivery` 技能和 `references/frontend-contract.md`，本任务按 BDD + 静态契约 TDD 执行。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，直接调整待处理节点信息的信息架构和静态契约。
- 是否存在临时补丁或绕过：否。
