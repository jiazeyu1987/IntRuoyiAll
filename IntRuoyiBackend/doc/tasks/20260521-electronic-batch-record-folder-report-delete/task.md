# Task: 删除报表设计器电子批记录目录下的所有报表

## 目标

删除 `报表管理 -> 报表设计器` 中 `电子批记录` 文件夹下的全部报表，并同步清理 `MES` 电子批记录生成报表元数据，避免留下仅删 `jimu_report` 或仅删 `mes_pro_batch_record_report` 的脏关联。

## 仓库与前置

- 仓库：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- 分支：`int_main`
- 上一同仓库任务文档：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-cover-product-002-image-v3\task.md`
- 上一任务状态：`Completed on 2026-05-21`
- 影响：上一同仓库任务已完成，不阻塞本次电子批记录目录数据清理。

## 范围

- 只删除 `jimu_report_category.name = '电子批记录'` 目录下的报表数据。
- 同步删除关联的 `mes_pro_batch_record_report` 元数据。
- 不删除 `电子批记录` 目录本身。
- 不修改其他报表目录、其他租户目录名、其他业务模块代码。

## 里程碑

- [x] M1：确认上一同仓库任务已完成，并建立本次任务文档。
- [x] M2：定位 `电子批记录` 目录 ID、目录下报表清单、以及 `MES` 侧关联元数据范围。
- [x] M3：执行定向删除，只清理该目录下报表与对应元数据。
- [x] M4：完成删除后校验、closeout preview 与任务范围提交。

## 预期验证

- 删除前能查到 `电子批记录` 目录下现有报表数量与清单。
- 删除后 `jimu_report` 中该目录下报表数为 `0`。
- 删除后 `mes_pro_batch_record_report` 中关联 `report_id` 残留数为 `0`。
- `电子批记录` 目录记录仍存在。

## 当前状态

Completed on 2026-05-21. 已在本地 `int-ruoyi-mysql` 的 `ruoyi-vue-pro` 数据库中定向删除 `电子批记录` 目录下全部 `106` 份报表，并同步删除 `106` 条 `MES` 电子批记录报表元数据；目录本身保留，删除后无残留关联。

## 最终结果

- 目录 ID：`598eb5f05dac423a831cebb3c97c3fa7`
- 目录租户：`1`
- 删除的 `jimu_report` 报表数：`106`
- 删除的 `mes_pro_batch_record_report` 元数据数：`106`

## 最终验证

- 删除前确认 `jimu_report` 与 `mes_pro_batch_record_report` 各有 `106` 条目标记录，且一一对应。
- 删除后 `jimu_report.type = '598eb5f05dac423a831cebb3c97c3fa7'` 的记录数为 `0`。
- 删除后 `mes_pro_batch_record_report.report_category_id = '598eb5f05dac423a831cebb3c97c3fa7'` 的记录数为 `0`。
- 删除后 `jimu_report_category` 中 `电子批记录` 目录仍存在。
- closeout preview 返回 `status: ready`，无额外清理项或阻塞。
