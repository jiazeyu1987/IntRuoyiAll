# 生产用料清单数据同步到测试服务器

## Task Goal

按用户确认的“不打包”方案，将本地数据库中的生产用料清单相关数据同步到测试服务器 `172.30.30.58`，执行顺序为：只读核对本地和测试服表结构/租户/目标表行数，备份测试服相关表，白名单表级 upsert，最后抽样验证生产用料清单页面/API。

## Milestones

- [x] 建立数据同步任务范围、门禁和审计记录。
- [x] 只读核对本地与测试服 schema、租户和目标表行数。
- [x] 生成并验证测试服目标表备份。
- [x] 执行白名单表级 upsert。
- [x] 抽样验证生产用料清单 API/页面和行数/业务键一致性。
- [x] 完成验证报告和收尾状态。

## Expected Verification

- 本地与测试服均存在 `mes_kingdee_production_material_list`，且列、唯一键、索引可承载同步数据。
- 明确源租户、目标租户、源行数、目标原始行数和业务键冲突策略。
- 测试服备份文件存在、非空，并记录 SHA256/行数证据。
- upsert 后目标表白名单范围行数、业务键集合和关键字段 hash 与本地一致。
- 测试服后端健康检查通过，生产用料清单查询 API 至少返回一个同步后的样本；若页面登录/E2E前置缺失，则记录为 blocker，不用 API-only 假冒页面验证。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；先核对正式 schema 和业务键，再备份后 upsert，不跳过缺表/缺字段/缺租户。
- `是否存在临时补丁或绕过`：否。

## Scope

- 白名单业务表：`mes_kingdee_production_material_list`。
- 前置只读核对对象：`system_tenant`、`system_menu`、`system_role_menu`、`infra_job`、目标表 schema/索引。
- 目标环境：测试服务器 `172.30.30.58`，业务库按远端容器真实查询确认。
- 禁止范围：不打包、不发布代码、不同步 MinIO、不操作正式服/备份服、不扩大到非白名单业务表。

## Result

- 数据同步已完成：测试服目标表从 `633` 行更新为 `7,983` 行，业务键去重后 `7,983` 个，与本地源表一致。
- 白名单业务字段 hash 按租户与本地完全一致：租户 `1/121/122/162` 均 PASS。
- 关联 ID 按 tenant-safe 口径复核：跨租户或不存在的物料关联已置空，最终 `work_order_id`、`work_order_bom_id`、`product_id`、`child_material_id` 的无效引用均为 `0`。
- 测试服 staging 表 `codex_pml_stage_20260801` 已删除，剩余计数 `0`。

## Closeout Blocker

- 数据任务本身已验证通过，但仓库当前存在其它任务的未提交/已暂存改动且分支已领先 `origin/int_main` 10 个提交；未执行提交/推送，任务只能停在 `ready_for_closeout`，不得标记 `completed`。
