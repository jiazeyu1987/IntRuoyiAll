# 自研 SRM 已下线

2026-11-26 决定停止自研 SRM，后续采用第三方系统。

自研前端页面、接口客户端、后端模块、供应商门户路由和审批 provider 已删除；登录及统一审批不再进入 SRM。ERP 与 MES 自身的供应商、采购、收货功能继续保留。

发布时执行 `sql/mysql/20261126_srm_retirement.sql`，停用自研 SRM 菜单和角色，并撤销菜单的角色及租户套餐授权。执行后按现有发布流程重启应用、更新前端并重新登录，避免继续使用旧的权限缓存。

数据库中的 SRM 业务表、历史迁移及审计记录保留用于追溯和后续数据迁移。此目录内旧计划和其他文档中 SRM 的历史完成记录均为历史资料，不再是开发或上线要求。第三方产品及接口尚未确定，本次不新增第三方对接代码。

## 本地验证

- 后端完整服务编译通过；认证、租户权限、审批列表和 provider 门禁共 93 项测试通过。
- 前端 Vite test 模式实际构建通过，输出在 `IntRuoyiFronted/.runtime/srm-retirement-build`，未发布。
- 单据编号跳转回归通过；审批治理脚本 9 项测试通过。
- 前端全量类型检查按项目既有 8GB 内存配置执行完成，但未通过：CRM 的 9 个文件缺少 `checkPermi` 引用。这些文件本次未修改。
- 菜单迁移检查已确认 SRM 路由变更关联下线 SQL；全仓检查仍未通过，剩余项是本次未修改的 `mes/pro/task/calendar/index.vue` 缺少 `mes:pro-work-order:query` 迁移决定。
- 两项共享旧测试未通过：`readable-business-data.test.cjs` 的测试加载器读取不存在的 `src/store.ts`；`system-time-format-remaining-modules-static.spec.js` 要求文控签名页具有当前不存在的 `operatedAt` 字段。未将这些问题扩展为 SRM 下线任务。
- 2026-11-26 已按用户授权在当前本机后端数据库 `127.0.0.1:23306/ruoyi-vue-pro` 执行迁移：停用 76 个菜单、2 个 SRM 管理员角色和 4 个角色分类，撤销 137 条角色菜单授权。租户套餐原本不含 SRM 菜单，未改写套餐行。
- 数据库独立连接回读确认 SRM 活动菜单、有效角色菜单授权、包含 SRM 的套餐及启用的 SRM 角色/分类均为 0；40 张 SRM 业务表执行前后逐行摘要一致。相关配置原值已备份至本机 `.runtime/srm-retirement-db-evidence/20261126-110944-before.json`，迁移账本已登记 `APPLIED`，operationId 为 `local-srm-retirement-2026-11-26-11-09-44`。
- 未部署、重启服务或执行真实页面 E2E；现有服务和登录会话的权限缓存不属于数据库回读结果，须在后续发布并重新登录后验证页面。
