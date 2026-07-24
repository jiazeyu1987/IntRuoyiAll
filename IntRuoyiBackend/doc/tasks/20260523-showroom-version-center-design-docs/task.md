# 任务：展厅公司/产品版本中心设计文档

## 任务目标

- 基于已确认方案，为 `D:\ProjectPackage\Int\IntRuoyi` 输出一套可直接交给实现的“展厅公司/产品成熟版本管理系统”设计文档。
- 设计文档覆盖后端接口、数据模型、前端工作台、权限/发布/部署要求。
- 明确当前仓库已有能力、缺口、事务边界、失败语义与 rollout 顺序，避免实现阶段再做高影响决策。

## 非目标

- 本任务不修改 `ruoyi-vue-pro` 或 `yudao-ui-admin-vue3` 的生产代码。
- 本任务不先行实现数据库迁移、后端接口、前端路由或页面。
- 本任务不调整现有展厅前台 `Website` 的运行逻辑，只设计其受影响边界。

## 前序任务检查

- 已检查最近同仓真实业务任务：`D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260523-tab-subtab-entry-error-analysis\task.md`
- 该任务状态为 `已完成`
- 不阻塞本次设计文档任务启动

## 里程碑

- [x] M1：建立本任务目录、任务文档与执行日志。
- [x] M2：核对现有 revision / narration / preview asset / release 代码与表结构边界。
- [x] M3：输出前端设计、后端接口设计、数据模型设计、配置安全部署设计四份文档。
- [x] M4：完成设计文档自检，确认关键决策已闭合、无待实现人二次拍板项。

## 预期验证

- 设计文档文件存在且 UTF-8 可读：
  - `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260523-showroom-version-center-design-docs\frontend-design.md`
  - `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260523-showroom-version-center-design-docs\backend-api-design.md`
  - `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260523-showroom-version-center-design-docs\data-model.md`
  - `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260523-showroom-version-center-design-docs\config-security-deployment.md`
- 设计文档覆盖：
  - 路由 / 页面 / 组件边界
  - API 合同 / 错误模型 / 事务边界
  - 表结构 / 关系 / 状态模型 / 迁移说明
  - 权限 / 发布 / 部署 / 观测要求

## 当前状态

- 状态：已完成
- 已完成：
  - 已核对现有展厅后台前端入口：`CompanyWorkbench`、`ProductDetailDialog`、`ProductListTable`、`showroom.ts`
  - 已核对现有后端接口与 runtime：`ShowroomAdminController`、`ShowroomApiRuntime`、`ShowroomReleasePublisherService`
  - 已核对现有核心表：`showroom_company_revision`、`showroom_product_revision`、`showroom_narration_version`、`showroom_preview_asset_version`、`showroom_release*`
  - 已确认当前历史能力现状：
    - 公司内容已有历史查看与复制旧版本为最新版本的局部能力
    - 产品内容已有 revision 历史查看，但缺少统一重发链路
    - 语音与预览图底层存历史 version，但 UI 仅能看最新 live 快照
    - 前台公开层已切到 `showroom release + website-config legacy projection`
  - 已输出四份设计文档，补齐成熟版本中心所需的路由、接口、数据、权限与 rollout 设计
- 待完成：
  - 若用户确认，下一任务进入实现阶段并按严格 TDD 执行
- 阻塞与影响：
  - 本任务为设计文档任务，无实现阻塞

## 实现交接约束

- 下一阶段实现必须按 `BDD + Strict TDD + Subagent-Driven Development` 执行。
- 推荐按以下子任务拆分：
  - `schema/backfill`
  - `backend APIs/services`
  - `frontend workbench`
- 每个子任务都必须在对应 `execution-log.md` 记录：
  - `BDD: <scenario name> -> Given/When/Then`
  - `RED: <command> -> FAIL, <expected reason>`
  - `GREEN: <command> -> PASS`
  - `REGRESSION: <command> -> PASS`
- pre-migration 公司历史缺 authoritative snapshot、公司/产品缺 preview asset linkage、或全局 current release source 不健康时，必须失败并记录阻断，不得 fallback。

## Cleanup Keep

- doc/tasks/20260523-showroom-version-center-design-docs/frontend-design.md
- doc/tasks/20260523-showroom-version-center-design-docs/backend-api-design.md
- doc/tasks/20260523-showroom-version-center-design-docs/data-model.md
- doc/tasks/20260523-showroom-version-center-design-docs/config-security-deployment.md

