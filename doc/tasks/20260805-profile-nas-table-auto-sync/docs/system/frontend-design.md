# NAS 表格自动同步 Frontend Design

## Purpose and Scope

本设计覆盖“个人工作台 -> 配置”中的额外页签“NAS表格自动同步”。用户能在已有配置页签权限边界内，维护是否启用、每日开始时间、需要同步输出的 ERP 表、NAS 目标目录、文件名规则，并查看最近执行日志、测试 NAS 写入、立即执行一次。前端不管理 NAS 账号密码；NAS 连接仍复用系统 NAS 管理的正式配置。

## Evidence Reviewed

- `IntRuoyiFronted/src/views/Profile/Index.vue`：现有“配置”顶层页签由 `mes:pro-batch-record-execution:golden-finger` 控制，配置内容当前直接渲染两个 eDHR 设置卡片。
- `IntRuoyiFronted/src/views/Profile/components/EdhrRecordbookGlobalSetting.vue` 与 `EdhrReleaseDossierRequirementSetting.vue`：配置卡片使用 `el-card`、显式 loading/saving、接口错误可见，不吞异常。
- `IntRuoyiFronted/src/api/erp/sync/index.ts`：当前 ERP 同步前端仅通过 infra job 触发既有同步任务。
- `IntRuoyiFronted/src/api/system/nas/index.ts`：NAS 管理 API 负责连接配置、浏览、测试和 DCC NAS 审计，不承担业务同步计划。

## Pages and Routes

- 页面入口保持 `/profile`，不新增菜单路由。
- 顶层“配置”页签仍由 `hasGoldenFingerPermission` 控制；新功能作为配置内容中的内部 tab，label 固定为 `NAS表格自动同步`。
- 当用户无 `mes:pro-batch-record-execution:golden-finger` 时，顶层配置页签不可见，内部 NAS 自动同步 tab 不可达；前端不额外暴露隐藏入口。

## Components

- 新增 `ProfileNasTableAutoSyncSetting.vue`，由 `Profile/Index.vue` 的配置面板内部 tabs 引用。
- 新组件包含：启用开关、每日开始时间选择器、ERP 表多选、NAS 相对目录输入、文件名模式输入、保存配置、测试 NAS 写入、立即执行一次、最近执行日志表。
- 新增 `src/api/erp/nasTableSync/index.ts` 封装后端接口；组件不得直接调用 infra job API 或 NAS 配置 API 来拼业务逻辑。

## State and Data Flow

- `onMounted` 同时加载支持的 ERP 表类型、当前计划、最近运行记录。
- 保存配置调用 `PUT /erp/nas-table-sync/plan/save`，后端返回含 `jobId`、`cronExpression`、`enabled` 的正式计划响应，前端以返回值刷新表单。
- 测试写入调用 `POST /erp/nas-table-sync/plan/test-nas-write`，只写入后端生成的可清理测试文件，不由浏览器直接写本地/NAS。
- 立即执行调用 `POST /erp/nas-table-sync/plan/run-once`，完成后刷新最近执行日志。

## Error States

- 加载失败显示 `el-alert`，保存/测试/立即执行失败显示后端错误消息。
- 如果后端报告 NAS 配置缺失、NAS 不可写、未选择 ERP 表、每日开始时间无效、Job 绑定失败或 ERP 表不支持导出，前端必须展示错误，不得写默认成功。
- 运行日志表显示失败原因；失败 run 不应被前端过滤隐藏。

## Accessibility and Responsive Behavior

- 内部 tab 使用 Element Plus `el-tabs`，按钮保留文本标签。
- 表单在窄屏下单列显示，日志表保持横向滚动。
- loading 状态禁用保存、测试和立即执行按钮，避免重复提交。

## Open Questions

- 无开放问题；本轮按用户确认的入口和权限边界实现。

## Design Blockers

- 若后端未提供正式 NAS 上传 API、ERP 表导出器或计划保存接口，前端实现必须阻塞而不是用静态 mock 数据或 infra job 页面代替。
