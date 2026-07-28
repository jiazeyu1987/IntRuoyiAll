# Execution Log

## User Intent

- 用户要求初始化“损耗单”的最新版本和“过程检验记录”的最新版本。
- 初始化目标是方便测试第一版辅助模式；后续仍由用户手动调整。

## BDD

- BDD: 损耗单最新版本初始化 -> Given 损耗单最新版本存在 When 执行初始化 Then 每张损耗单表单拥有完整辅助行和对应填写人分配。
- BDD: 过程检验记录最新版本初始化 -> Given 过程检验记录最新版本存在 When 执行初始化 Then 每张过程检验记录表单拥有完整辅助行和对应填写人分配。
- BDD: 后续手动调整保留 -> Given 初始化已写入 When 用户打开辅助表单映射模式 Then 可以基于初始化结果继续手动调整。

## Commands And Evidence

- 2026-07-28: 已读取 database / PowerShell encoding / task closeout 规则，确认需要先核对真实 schema 与目标版本范围。
- RED: `python -X utf8 doc\tasks\20260728-extra-slot-initial-assist-mapping\initialize_extra_slot_assist_mapping.py --verify` -> FAIL, `LOSS_REPORT` 与 `PROCESS_INSPECTION` 最新目标均缺少完整 `edhrAssistRows` 和 scoped fill assignments。
