# Execution Log: eDHR 详情页辅助模式预览 Switch

## User Intent

用户要求在 eDHR 批次详情页右侧红框位置增加“辅助模式”Switch，切换后中间预览区可以在原表模式和辅助模式之间切换；Switch 只影响中间预览，无辅助配置时显示禁用。

## Initial State

- PRECHECK: 工作区 `E:\IntRuoyi` 已存在大量非本任务修改/未跟踪文件；本任务只触碰 eDHR 详情页辅助预览 Switch 相关文件。
- PRECHECK: 已读取 `frontend-feature-delivery`、`backend-api-delivery`、`docs/frontend-development.md`、`docs/backend-development.md`、`docs/e2e-rules.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`。

## BDD Scenarios

- BDD: 详情页原表/辅助预览切换 -> Given eDHR 批次详情页当前选中右侧某个主生产表卡片且该表单配置了辅助模式, When 用户打开右侧栏顶部“辅助模式”Switch, Then 中间预览区从原表只读预览切换为辅助字段只读列表，不触发保存、提交、打开表单或写请求。
- BDD: 无辅助配置禁用 Switch -> Given 当前选中表单没有 `assistRows`, When 用户查看右侧栏顶部 Switch, Then Switch 保留但禁用，并显示“未配置辅助模式”提示，中间区域仍展示原表模式。
- BDD: 未打开主生产表预览包含辅助快照 -> Given 当前主生产表任务尚未生成执行记录但正式报表配置了辅助行, When 详情页调用 `/task/preview`, Then 响应中的 `formViewModel.executionSnapshotJson` 包含 `assistRows`，供前端只读辅助预览使用。
- BDD: 动态表单来源不混用 -> Given 当前选中的是动态表单卡片, When 用户切换辅助模式或查看表单, Then 动态表单仍按 FormCenter 预览来源处理，不使用批记录报表快照生成辅助行。
