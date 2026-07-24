# 任务：产品主数据后端

## 任务目标

新增中立产品主数据能力，提供产品主数据表、全量 Excel 导入预览与确认、基础 CRUD/API、DCC 引用快照、展厅产品绑定能力，并支持管理员将既有展厅产品内容按规则预览后映射到产品主数据。DCC 和展厅不得直接互相依赖产品清单。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260605-backend-runtime-base-local-config/task.md`
- 状态：`completed`
- 处理：上一任务已完成并记录验证；本任务在独立 worktree 与分支 `codex/20260607-product-master-data` 中实施。

## BDD 场景

- BDD: Excel 全量导入产品主数据 -> Given 管理员上传包含产品编码和中文名称的 Excel / When 预览并确认导入 / Then 系统新增或更新 Excel 中的产品，停用 Excel 缺失的既有产品，不物理删除。
- BDD: 产品主数据校验失败直接阻塞 -> Given Excel 存在空产品编码、重复编码、重复 DCC 产品编号或非法 14 位 DCC 产品编号 / When 执行导入预览 / Then 后端返回明确失败原因，不写入产品数据。
- BDD: DCC 提交必须引用启用产品主数据 -> Given 申请人提交 DCC 受控文件 / When 产品主数据缺失、停用或无合法 DCC 产品编号 / Then 提交失败并暴露前置条件，不保存默认产品快照。
- BDD: DCC 产品快照保持历史可追溯 -> Given DCC 记录已保存产品主数据引用和名称快照 / When 产品主数据名称后续变更 / Then 历史 DCC 记录继续显示提交时快照，新 DCC 使用最新主数据。
- BDD: 展厅产品绑定产品主数据 -> Given 展厅产品已绑定产品主数据 / When 展厅列表、展柜选择或 Excel 展示资料导入读取产品 / Then 产品编码和基础名称来自产品主数据，展厅资料继续保存在展厅 revision。
- BDD: 芋道源码管理员从展厅映射产品主数据 -> Given `芋道源码/admin` 账户已有展厅产品内容 / When 管理员预览并确认展厅映射 / Then 系统按产品编码新增、更新或绑定产品主数据，重复编码或缺少编码/中文名直接失败，展厅产品写入 `product_master_id`。

## Milestones

- [x] M1：建立任务文档、执行日志、后端证据文档。
- [x] M2：补充后端 RED 测试，覆盖 schema、导入校验、DCC 引用、展厅绑定。
- [x] M3：新增 `yudao-module-mdm`、主数据 schema、API、导入预览/确认。
- [x] M4：改造 DCC 提交和元数据更新，写入 `product_master_id` 与快照。
- [x] M5：改造展厅产品绑定主数据和 Excel 导入读取。
- [x] M6：新增展厅内容到产品主数据的预览/确认映射能力。
- [x] M7：运行后端验证、记录证据、cleanup 预览并提交。

## Expected Verification

- `mvn -pl yudao-module-mdm -am test`
- `mvn -pl yudao-module-dcc -am -Dtest=*Product* test`
- `mvn -pl yudao-module-showroom -am -Dtest=*Product* test`
- `mvn -pl yudao-module-showroom -Dtest=ShowroomMdmProductMappingContractTest test`
- `mvn -pl yudao-module-showroom -Dtest=ShowroomMdmProductMappingServiceTest test`
- `python -m pytest script\tests\test_product_master_sql_contract.py`
- `git diff --check`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\worktrees\20260607-product-master-data\ruoyi-vue-pro --task-id 20260607-product-master-data --mode preview`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少产品主数据、非法编码、停用产品、Excel 错误或 schema 缺失均直接失败。
- `是否从根因和长期维护角度解决`：是。新增中立主数据模块，DCC 和展厅通过稳定产品 ID 与快照解耦。
- `是否存在临时补丁或绕过`：否。不使用展厅表作为 DCC 的隐式数据源，不用名称猜测代替稳定编码。

## 当前状态

completed: 产品主数据后端已变基到最新 `int_main`，后端脚本测试、MDM、DCC、Showroom 产品相关回归与 diff 检查均通过；可 fast-forward 合并并删除独立 worktree。
