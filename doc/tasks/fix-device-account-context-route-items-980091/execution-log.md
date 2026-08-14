# Execution Log

## User Intent

- 用户反馈：一线 PQC 切换到按压式扩张压力泵时报错：`设备账号上下文不完整或不一致：routeProjectItems routeId=980091，missingItemIds=[14]`。

## BDD

- BDD: 一线 PQC 设备切换不应误报 routeProjectItems 缺失 -> Given 一线 PQC 选择 routeId=980091 的按压式扩张压力泵相关工序/设备上下文，When 后端构建设备账号上下文，Then 只校验该路线正式需要的项目项并返回完整上下文，不因非当前正式范围的 itemId=14 触发上下文不完整错误。

## TDD Evidence

- RED: 待运行
- GREEN: 待运行

## Milestone Updates

- in_progress: 已创建任务目录和初始任务证据。

## Blockers

- 暂无。
