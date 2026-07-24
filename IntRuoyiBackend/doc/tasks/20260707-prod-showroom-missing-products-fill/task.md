# 正式服展厅产品缺失补全

## 任务目标
- 核对正式服当前展厅产品列表、旧编号映射、展柜引用、产品语音和当前 website 发布包。
- 若确认正式服仍缺产品，按全量产品 zip 导入/导出策略补齐正式服数据。
- 补全后手动发布展厅，并验证 admin/website 产品数量、语音资产和旧编号映射一致。

## 里程碑
- [ ] M1 核对正式服当前缺失范围与上一轮修复结果差异。
- [ ] M2 确认可用于正式服补全的本机全量产品包或重新生成全量包。
- [ ] M3 执行正式服最小范围导入/补齐与手动发布。
- [ ] M4 验证正式服 admin 数据、website 发布包、产品语音与旧编号映射。

## 经验门禁
- PowerShell/中文/远端命令：必须显式 UTF-8，避免用 PowerShell 管道传中文 SQL 到远端子进程。
- 正式服写入：用户已明确授权“在正式服务器补全”；动作前必须先只读核对目标主机、当前数据、可用备份/恢复约束和写入范围。
- 发布/恢复相关：不得清空或覆盖整库；只允许展厅产品数据补齐、导入、发布和只读验证。
- NAS/挂载保护：不得删除、清空或修改 `/mnt/nas`、共享盘挂载或 `fstab`。

## 设计约束检查
- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；先核真实缺口，再用全量产品包补齐。
- `是否存在临时补丁或绕过`：否。

## Current Status
in progress

## 完成记录
- 完成时间：2026-07-07T13:04:28Z
- 正式服导入前备份：`/opt/intruoyi/runtime/backups/prod-full-before-showroom-fill-20260707T124446Z.sql.gz`
- 本机导出包：`evidence/showroom-products-local-admin-20260707T122451Z.zip`，检查结果为 163 行产品、全部 `INT-*`、无 `product_*` / `e2e*`、无冻结字段。
- 正式服导入结果：`totalRows=163`、`successCount=16`、`skippedCount=147`、`failureCount=0`。
- 补入产品：`INT-20`、`INT-21`、`INT-22`、`INT-23`、`INT-24`、`INT-25`、`INT-64`、`INT-69`、`INT-70`、`INT-71`、`INT-72`、`INT-73`、`INT-74`、`INT-75`、`INT-83`、`INT-99`。
- 正式服最终数据：管理端产品总数 163，活跃 `INT-*` 产品 163，活跃 `product_*` / `e2e*` 产品 0。
- 发布结果：Website current release `20260707T130144Z-be276b74dfa8-081780e2a98e`，根页面 200，无 `更新失败` / `SHOWROOM_RELEASE_INSTALL_FAILED`。
- 备注：补齐后 16 个补入产品的 `legacy_product_code` 为空，未在本次强行猜测写入旧编号，避免旧编号错配。

## 当前状态
- completed
- 追加修复：正式服芋道源码租户已补写 15 个可靠 `legacy_product_code` 映射；`INT-83/product_082` 因历史名称匹配歧义未强行映射。备份：`/opt/intruoyi/runtime/backups/prod-showroom-product-legacy-before-20260707T130917Z.sql.gz`。
