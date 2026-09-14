# NAS 表格自动同步 BDD Scenarios

## Purpose and Scope

定义“个人工作台 -> 配置 -> NAS表格自动同步”的可观察业务行为、失败行为和边界行为，作为开发与验收依据。

## Evidence Reviewed

- 用户指定入口、权限和 tab 名称。
- 现有个人工作台配置页签权限为 `mes:pro-batch-record-execution:golden-finger`。
- 现有 ERP 同步类型和 infra job/NAS 服务可复用。

## Feature Scenarios

BDD: 配置页签内管理 NAS 表格自动同步 -> Given 用户拥有配置页签权限 When 打开个人工作台配置并进入“NAS表格自动同步” Then 页面显示启用开关、每日开始时间、ERP 表多选、NAS 目录、保存、测试写入、立即执行和执行日志。

BDD: 保存每日自动同步计划 -> Given 用户选择 ERP 表并填写每日开始时间和 NAS 目录 When 点击保存配置 Then 后端保存业务计划、绑定 `erpNasTableAutoSyncJob` 调度任务，并返回计划与 cron。

BDD: 手动执行一次同步 -> Given 已存在启用计划且 NAS 可写 When 用户点击立即执行一次 Then 后端生成 xlsx 文件到 NAS，记录 run 和每个表的 run item，前端刷新执行日志。

## Failure Scenarios

BDD: NAS 配置缺失必须可见失败 -> Given 系统未完成 NAS 连接配置 When 用户测试写入或立即执行 Then 接口返回 NAS 配置缺失错误，前端显示错误且不生成成功 run。

BDD: ERP 表类型不支持导出必须失败 -> Given 请求包含未登记的 ERP 表类型 When 保存启用计划或执行同步 Then 后端返回不支持的 ERP 表类型，不能生成空 sheet 或默认成功。

BDD: 无配置权限用户不能访问 -> Given 用户缺少 `mes:pro-batch-record-execution:golden-finger` When 打开个人工作台 Then 顶层配置页签不可见，直接请求后端接口也被权限拒绝。

## Boundary Scenarios

BDD: 禁用计划允许保留草稿 -> Given 用户关闭启用开关 When 保存空 ERP 表或未完整目录 Then 后端保存 disabled 草稿但不启用 Job。

BDD: 启用计划必须完整 -> Given 用户打开启用开关 When 未选择 ERP 表、未设置时间或 NAS 目录非法 Then 保存失败并显示明确校验错误。

BDD: 文件路径必须限定在 NAS 共享内 -> Given 用户输入 `../` 或绝对路径 When 保存或测试写入 Then 后端拒绝路径穿越，不访问共享根外路径。

## Open Questions

无开放问题。

## Test Blockers

- 没有可用 worktree runtime slot、登录账号、NAS 配置或真实前端入口时，真实 E2E 必须阻塞。
- 若新增 NAS 上传能力未实现，不能用下载 API 或本地文件写入替代。
