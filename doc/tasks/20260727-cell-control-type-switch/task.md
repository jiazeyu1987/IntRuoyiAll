# 单元格控件类型切换

## Task Goal

在现有批记录表单“单元格规则”弹窗内实现可审计的单元格控件类型切换能力，支持文本、日期、日期时间、电子签名、下拉框、数字输入及数字上下限配置；不改造积木报表 iframe 原生右键菜单或右侧属性栏。

## Milestones

- [x] M1：完成规则/技能/前后端门禁读取，确认最小实现落点为现有 `BatchRecordCellRulesConfirmDialog` 与 `/cell-rules` 契约。
- [x] M2：按项目规则提交既有脏工作区基线，保护并行任务改动。
- [x] M3：补充 RED 静态契约，证明当前规则弹窗缺少下拉选项编辑和签名联动保存契约。
- [x] M4：实现前端规则弹窗的控件类型切换、下拉选项编辑、数字上下限校验和签名类型提示。
- [x] M5：实现/加固后端 cell-rules 对下拉选项与签名规则的校验和持久化语义。
- [x] M6：运行定向 RED/GREEN/REGRESSION 验证并更新证据。

## Expected Verification

- `node IntRuoyiFronted/tests/e2e/edhr-cell-control-type-switch-static.spec.js`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellRuleSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `git diff --check`

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺少签名 marker、下拉选项不足、数字上下限非法必须 fail-fast。
- `是否从根因和长期维护角度解决`：是；复用现有规则弹窗和 `/cell-rules` 数据模型，不注入 iframe、不手工改 Jimu JSON。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- Trigger: Jimu fillForm 组件类型、componentFlag、多行文本/日期/电子签名/签名日期格与 eDHR 单元格规则不一致。
- Preflight check: 同时核对 `fillForm.componentFlag`、`edhrCellRule.valueType/componentFlag`、`edhrSignature` 与前端规则编辑器，不只改显示文案。
- Blocker: 签名规则没有 enabled `edhrSignature`、下拉框没有至少两个有效选项、数字最小值大于最大值、或后端保存后没有同步 `fillForm`。
- Verification: 静态契约覆盖前端控件切换和后端保存校验；定向 Maven 测试覆盖下拉选项、签名 marker 和数字约束。
- Forbidden action: 禁止改 iframe 原生 UI、禁止直接手工改 Jimu JSON、禁止用默认文本控件或空 options 掩盖配置缺失。
- Evidence: `docs/backend-development.md#jimu-fillform-组件类型语义优先边界`，本任务 `execution-log.md`。

## Baseline

- Dirty-worktree baseline commit: `b7dc3380 chore: baseline existing worktree changes`.
- Remaining concurrent dirty files after baseline are outside this task except `ExecutionPage.vue`;本任务尽量避免修改该文件，若必须改同文件将先做 hunk 级冲突检查。
