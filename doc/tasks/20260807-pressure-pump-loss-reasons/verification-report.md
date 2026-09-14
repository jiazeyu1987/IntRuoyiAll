# Verification Report

## Result

PASS。已通过本机真实页面为“球囊扩张压力泵”和“按压式球囊扩充压力泵”共 28 道工序配置人员可理解的中文损耗原因；每道工序最终均为 2～5 条，目标路线不再显示 `RLR0807M-*` 占位描述。

## Scope

- 环境：本机 `int_main`，前端 `http://127.0.0.1:8081`，后端 `http://127.0.0.1:48081`。
- 身份：`芋道源码/admin`。
- `球囊扩张压力泵(routeId=922119)`：14 道工序，最终 54 条原因。
- `按压式球囊扩充压力泵(routeId=980091)`：14 道工序，最终 52 条原因。
- 合计：28 道工序、106 条原因；数量分布 `2:7、3:4、4:5、5:12`。

## BDD/TDD Evidence

- RED：`node doc\\tasks\\20260807-pressure-pump-loss-reasons\\pressure-pump-loss-reasons.e2e.mjs red` -> FAIL（预期），53 项未满足最终条件：14 道工序无原因、5 道工序仅 1 条、34 条原因为占位描述。
- PLAN：同脚本 `plan` -> PASS；14 类工序各有 5 条显式候选池，按稳定 `routeProcessId` 种子冻结最终 2～5 条计划；34 条原位修改、72 条新增。
- GREEN APPLY：同脚本 `apply` 最终续跑 -> PASS；恢复前 `completed=54`、`pending=52`、`diverged=0`，本轮 52 个真实页面写请求全部业务码 `0`，请求数与 pending 完全一致。
- GREEN VERIFY：同脚本 `verify` -> PASS；全新会话 `targetProcessCount=28`、`finalReasonCount=106`、`placeholderCount=0`、MES 写请求 `0`。

## Data Integrity

- 34 条既有占位记录已原位改为中文原因，其 ID、`reasonCode`、`routeProcessId` 和 `enabled` 均与初始快照一致。
- 72 条新增原因全部通过“维护损耗”弹框正式 POST 创建；内部编码由系统生成，页面损耗原因列仅显示 `reasonName`。
- 两条路线的逐工序原因数量和名称集合精确等于冻结计划；未删除、停用或移动既有记录，也未修改 schema 或执行 SQL 写入。
- 最终只读会话 page error `0`、console error `0`、目标网络失败 `0`、MES 写请求 `0`。

## Human-Readable Examples

- 粗洗工序：`清洗温度超差`、`表面污渍残留`、`零件混料`、`清洗时间不足`。
- 精洗工序：`清洗后水迹`、`微粒残留`、`纯化水冲洗不足`。
- 组装工序：`装配不到位`、`零件漏装`、`密封圈漏装`、`卡扣未到位`。
- 光固工序：`固化不完全`、`光固胶溢出`、`固化处气泡`、`固化位置偏移`。
- 检测工序：`压力保持不合格`、`泄漏检测不合格`、`推注阻力超差`、`外观检测不合格`。
- 包装工序：`单包装袋破损`、`说明书漏装`、`装箱数量错误`、`封箱不牢`。

## Visual Evidence

- 收尾前已人工检查 `final-pressure-pump.png`：`球囊扩张压力泵` 页面行显示中文原因。
- 收尾前已人工检查 `final-press-pressure-pump.png`：`按压式球囊扩充压力泵` 页面行显示中文原因。
- 截图同时显示与本任务无关的既有提示：`team-device/list` 请求地址不存在。该提示不来自损耗列表或维护接口，本任务未隐藏或扩大范围修复。

## Residual Risk

- 当前共享 `int_main` 仍存在 `team-device/list` 独立运行态错误；不影响本次两条路线的 106 条损耗原因数据结果，但需要另行任务处理设备列表接口契约。
- 其它非目标工艺路线仍可能保留历史 `RLR0807M-*` 原因；本任务严格限定为用户指定的两条压力泵路线。

## Closeout

- 经验沉淀结构核验通过：行内编辑动态定位规则已合并到 `docs/e2e-rules.md`，索引已更新到 `docs/experience-index.md`。
- `task-closeout-cleanup` preview/apply 均通过；7 个任务临时产物已删除，仅保留 3 份核心任务记录，无阻塞和警告。
- 最终状态：`completed`。
