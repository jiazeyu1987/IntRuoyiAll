# 工序编码 ER 前缀规则

## 任务目标

- 工序管理中通过“生成”按钮得到的工序编码不再带 `EDHR_PROC_` 长前缀。
- 新生成的工序编码统一以 `ER` 开头。
- 保持工序编码字段仍走现有自动编码接口，不引入前端随机编码或兜底生成。

## 里程碑

- [x] M1：建立任务文档、读取经验门禁并定位工序编码生成链路。
- [x] M2：添加 RED 静态契约，证明当前生成调用没有传入 `ER` 前缀。
- [x] M3：修改工序编码生成调用，统一使用 `ER` 作为规则输入字符。
- [x] M4：运行目标静态测试和相关回归验证。
- [x] M5：提交本任务前端仓改动。

## 预期验证

- `node tests/e2e/mes-pro-process-code-er-prefix-static.spec.js` 先失败后通过。
- 静态契约断言 `ProProcessForm.vue` 调用 `AutoCodeRecordApi.generateAutoCode(MesAutoCodeRuleCode.PRO_PROCESS_CODE, 'ER')`。
- 静态契约断言工序表单源码不包含 `EDHR_PROC_` 固定前缀。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；涉及中文输出、Markdown 读取与验证时必须显式 UTF-8。
- 项目经验索引：已读取 `docs/experience-index.md`，本任务命中 PowerShell 编码门禁。
- 回归修复：已读取 `bug-regression-fix-loop` 与证据契约；本任务已先记录 BDD 与 RED，再做实现。
- 混合脏工作区：当前前端仓有既有脏改；本任务只提交工序编码相关文件和本任务文档，不暂存/回滚无关改动。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。沿用自动编码服务，只为工序编码规则传入明确业务前缀 `ER`。
- 是否存在临时补丁或绕过：否。

## 当前状态

- 状态：COMPLETED
- 已完成：`ProProcessForm.vue` 的生成按钮已改为 `AutoCodeRecordApi.generateAutoCode(MesAutoCodeRuleCode.PRO_PROCESS_CODE, 'ER')`。
- 验证：`node tests/e2e/mes-pro-process-code-er-prefix-static.spec.js` 通过；收尾清理预览无删除项、无阻塞。
