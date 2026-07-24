# 任务：恢复本地展厅产品映射

## 目标

将当前本地运行库 `127.0.0.1:23306/ruoyi-vue-pro` 的 `showroom_hall_product` 从临时联调用的单产品映射状态，恢复到 `20260520_113715` 发布快照中的历史映射状态。

## 范围

- 只恢复本地运行库中的 `showroom_hall_product`
- 使用已确认的历史恢复源：
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\tmp\publish-int-ruoyi-to-test\20260520_113715\ruoyi-vue-pro-current.sql`
  - `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260521-showroom-mapping-recovery-compare\restore-showroom-hall-product-from-20260520_113715.sql`
- 回写本次恢复验证证据

## 非范围

- 不恢复测试服务器 `172.30.30.58`
- 不恢复 preview asset、narration、审批流、评论或其他 showroom 表
- 不修改 Java / 前端业务代码

## 上一任务检查

- 同仓库上一未完成任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-test-server-full-publish-overwrite-data\task.md`
- 状态：`Blocked on 2026-05-21`
- 影响：该任务已因当前更高优先级的数据恢复问题显式阻塞，且尚未执行测试服全量覆盖发布；本任务仅恢复本地运行库。
 
- 上一相关后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-app-config-live-data-local-verification\task.md`
- 状态：`Completed`
- 影响：已确认当前单产品映射是本地联调临时回填，不应视为正式展厅编排。

- 上一恢复排查任务：`D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260521-showroom-mapping-recovery-compare\task.md`
- 状态：`Completed`
- 影响：已确认 `20260520_113715` 是当前最可信的映射恢复源。

## 里程碑

- [x] M1：创建任务文档并确认上一任务状态。
- [x] M2：执行恢复前 RED 校验，确认当前仍是 `8` 条单产品映射。
- [x] M3：执行恢复 SQL，将本地映射恢复为历史 `166` 条。
- [x] M4：执行恢复后 GREEN 校验，确认每个 hall 的映射数量恢复。
- [x] M5：回写证据并执行 cleanup 预览。

## 预期验证

- `showroom_hall_product` 当前 `8` 条 -> RED
- `showroom_hall_product` 恢复后 `166` 条 -> GREEN
- `hall 1..8` 恢复到历史映射分布：
  - `26 / 28 / 27 / 17 / 10 / 20 / 11 / 27`

## 当前状态

Completed.

## 完成结果

- 本地运行库 `127.0.0.1:23306/ruoyi-vue-pro` 的 `showroom_hall_product` 已从 `8` 条单产品映射恢复为 `166` 条历史映射。
- 恢复后 hall 分布已回到历史状态：
  - `hall 1 = 26`
  - `hall 2 = 28`
  - `hall 3 = 27`
  - `hall 4 = 17`
  - `hall 5 = 10`
  - `hall 6 = 20`
  - `hall 7 = 11`
  - `hall 8 = 27`
- 说明：本次只恢复了 `showroom_hall_product`，没有恢复产品 preview / narration 的已发布资源。

## 残余阻塞

- 匿名 `GET /showroom/display/app-config` 当前返回：
  - `SHOWROOM_TARGET_NOT_FOUND: live product ZH narration not found`
- 影响：
  - 如果你当前看的“展厅”是依赖 `app-config` 的 Website consumer，这一条链路仍然没有完全恢复。
  - 原因不是本次映射恢复失败，而是历史映射里的产品本来就缺已发布中文讲解等 live 资源。
- 已核对 `20260520_113715` 历史快照：
  - `product 1..166` 中 `PUBLISHED` product preview 数量 `0`
  - `PUBLISHED` ZH narration 数量 `0`
  - `PUBLISHED` EN narration 数量 `0`
  - 因此没有可直接回放的完整 consumer 资源源头。

## 最终验证

- 本地恢复前 `showroom_hall_product = 8` -> PASS
- 修正恢复 SQL 后再次执行 -> PASS
- 本地恢复后 `showroom_hall_product = 166` -> PASS
- hall 1..8 数量分布恢复 -> PASS
- `GET /showroom/display/app-config` -> FAIL，暴露真实缺失前置条件 `live product ZH narration not found`
- `task_closeout.py --mode preview` -> PASS

## Cleanup Keep

- `ruoyi-vue-pro/doc/tasks/20260521-showroom-hall-product-restore/task.md`
- `ruoyi-vue-pro/doc/tasks/20260521-showroom-hall-product-restore/execution-log.md`
