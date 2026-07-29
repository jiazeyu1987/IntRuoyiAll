# Execution Log

## User Intent

批记录管理员主区域不读取草稿；其他账户提交该批次执行内容后，管理员应能在主区域看到当前已提交内容。

## BDD

BDD: 管理员查看他人已提交批记录内容 -> Given 同一批次的填写人已提交某工序 execution；When 批记录管理员打开批次详情并选择该工序；Then 主区域渲染该已提交 execution 的 cellValuesJson，而不是管理员草稿或空模板预览。

BDD: 草稿和待打开不顶替主区域 -> Given 某工序只有草稿 execution 或待打开任务；When 批记录管理员打开批次详情；Then 主区域不展示草稿内容，不调用空 preview 冒充已提交内容，而显示暂无已提交内容。

## Current Evidence

- 前端主区域当前按 selectedExecution.formViewModel || selectedTaskPreview.formViewModel 取值。
- 后端 task preview 对未创建 execution 的任务返回 cellValuesJson = []。
