# Execution Log

## User Intent

- 用户澄清：本需求不是永久自动对应，也不是替代手动映射；目标是帮用户初始化一个“球囊扩张压力泵”最新版本批记录表单的辅助模式版本，方便后续测试。
- 初始化规则：根据签字单元格数量创建填写人，最少 1 个；再自动将原表单元格分配给对应填写人；后续仍可手动调整。

## BDD

- BDD: 按签字单元格数量创建填写人 -> Given 最新版本批记录表单存在签字单元格 When 执行初始化 Then 填写人数量等于签字单元格数量且至少为 1。
- BDD: 自动分配原表单元格 -> Given 初始化已创建填写人 When 系统生成辅助映射 Then 每个可填写原表单元格最多归属一个填写人。
- BDD: 初始化后允许手动调整 -> Given 初始化映射已保存 When 用户打开辅助表单映射模式 Then 可继续删除或重新分配映射。

## Commands And Evidence

- 2026-07-28: 已读取 frontend/backend/database/login/local-runtime/e2e/PowerShell/task closeout 规则，确认需先核对 schema 与目标租户范围。
