# 任务：展厅展示层迁移到 Website 仓库

## 目标

将展厅“展示层 / APP 层”相关需求从 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` 的任务拆分中迁出，改为放在 `D:\ProjectPackage\Website` 仓库单独执行；当前仓库仅保留后台管理页、业务接口、审批、指派、讨论、讲解后台等任务。

## 里程碑

- [x] 识别展示层与后台层边界
- [x] 在当前仓库记录迁移规则
- [x] 在 Website 仓库建立 APP 层任务包
- [x] 更新当前派工文档引用

## 迁移规则

- 保留在当前仓库：
  - 后端 `B1-B5`
  - 后台前端 `F1-F3`
- 迁移到 `D:\ProjectPackage\Website`：
  - APP 公司页 / 首页 / 导航
  - APP 产品图片墙 / 音频 / 讲解文字 / 设置
  - APP 大屏 / Pad / 手机展示层
  - APP 集成与 E2E

## 当前状态

已完成。
