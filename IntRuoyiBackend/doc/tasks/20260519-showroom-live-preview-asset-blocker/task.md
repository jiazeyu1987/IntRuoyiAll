# 任务：定位展厅前台 live 预览图资产阻塞

## 目标

定位数字展厅前台在真实入口与三端路由都已可达的情况下，首页图片墙仍全部显示“未发布预览图”的真实阻塞原因，并明确下一步所需前置条件。

## 范围

- 只做真实运行态与数据层诊断。
- 核对 `/showroom/display/home` 的 preview 资产来源链路。
- 核对 `showroom_preview_asset_version` 与 `infra_file` 的运行态数据。
- 不在本任务中伪造图片资产或静默补默认图。

## 前序任务检查

- 前序相关后端任务：`ruoyi-vue-pro/doc/tasks/20260519-showroom-remediation-b5-narration-preview-assets/task.md`
- 当前状态：blocked
- 已记录阻塞：B5 在源码干净编译与 preview/narration 持久化收口上仍有历史阻塞。
- 对本任务影响：虽然 B5 未完成，但当前 runtime 已具备 `previewImageUrl` 读取逻辑，因此可以先对 live 数据阻塞做真实诊断。

## 里程碑

- [x] M1：确认前台 blocker 不是路由不可达，而是 live 预览图为空。
- [x] M2：定位 `previewImageUrl` 后端读取逻辑。
- [x] M3：核对 `showroom_preview_asset_version` 与 `infra_file` 的真实运行态数据。
- [x] M4：记录阻塞结论与下一步前置条件。

## 预期验证

- 能解释 `/showroom/display/home` 为什么返回空 `previewImageUrl`
- 能给出表级/文件级事实，而不是猜测

## 当前状态

Completed. 已确认前台缺失 `previewImageUrl` 的根因是运行库中 `showroom_preview_asset_version` 与 `image/*` 文件均为空；用户已批准使用当前生成的截图作为临时本地验证素材，后续执行任务将正式落地。

## 诊断结论

- `ShowroomApiRuntime.displayHome()` 对 `hallEntries.previewImageUrl` 的来源是 `previewImageUrl(TARGET_HALL, hallId)`。
- `previewImageUrl(...)` 依赖 `showroom_preview_asset_version` 的已发布记录。
- 当前真实运行库：
  - `showroom_preview_asset_version` 行数为 `0`
  - `infra_file` 中 `image/*` 行数为 `0`
  - `showroom_hall` 共有 `8` 个展厅：
    - `心内介植入展厅`
    - `心脏植入展厅`
    - `外周介植入展厅`
    - `神经介植入展厅`
    - `外泌体与超声聚焦展厅`
    - `骨科与泌尿产品展厅`
    - `非介入类产品展厅`
    - `医疗标准件展厅`

## 阻塞说明

- 当前不是“前台路由失败”，而是“运行库里没有任何已发布 preview asset 记录，且文件库里没有任何 image 资产可绑定”。
- 在没有真实图片源或没有用户明确批准复用临时图片的前提下，不能继续发布 live preview 资产，否则会违反无 fallback / 无伪造成功的基线。

## 下一步前置条件

- 已获得用户批准的临时本地验证方案，可以继续进入“预览图资产落地”执行任务。
