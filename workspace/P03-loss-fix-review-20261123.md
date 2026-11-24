# P03 损耗展示修复与复查

1. 子 agent 实现，主线程复查并补后端测试。修复范围仅为正式报工数量读取、接口输出和生产记录展示。
2. 事件327089原始生产25、损耗1已保存。原页面无物料时把损耗写成0；详情接口此前也未单独提供原始数量。
3. 现在生产记录及合计读取原始报工。一笔事件只计一次，物料核查保留；订单分配量单列，不更改进度算法。
4. 本地相关验证通过；未发布、未重启、未改业务数据，未声称真实页面验收通过。

## 依据和范围

- 原始只读证据：同目录 `P03-loss-readonly-evidence-20261123.json`，调查记录 `P03-loss-readonly-20261123.md`。
- 对照基线：同目录 `p03-fix-baseline`。审查按该基线区分本轮与并行改动；Vue中PQC损耗描述文案变化属于并行改动，保留。
- 保留原始正式明细1件；没有补报、清零、重复签署、数据库写入、配置/权限修改或服务重启。

## 代码链路

仓库根目录：`D:\ProjectPackage\IntRuoyi\IntRuoyiAll`。

| 节点 | 文件及位置 | 本轮结果 |
| --- | --- | --- |
| 正式事件读取 | `IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProcessPoolActiveOrderDetailReadMapper.xml:57` | 原查询保留event_id、当前分配量和事件raw_payload，无查询或分配算法修改 |
| 服务组装 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java:1851` | 从原始payload根节点读取outputQuantity、lossQuantity；缺字段抛业务异常 |
| 领域模型 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetail.java:126` | 新增originalOutputQuantity、originalLossQuantity，保留submittedQuantity的订单分配语义 |
| 接口模型 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderDetailRespVO.java:130` | 输出两项原始数量并说明字段语义 |
| 控制器转换 | `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java:1272` | 原值传递；保留原有权限和材料/损耗明细转换 |
| 前端接口类型 | `IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts:588` | 明确原始数量必填，分配量独立 |
| 页面和弹窗 | `IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue:3102` | 一笔事件一行，展示原始数量，缺字段不填0 |
| 合计和核查 | 同一Vue文件`:3135`、`:3300` | 仅事件主行参与合计；材料身份、清场、设备参数保留，重复事件设备卡过滤 |

## 复查发现及处理

1. 第一版会丢掉生产记录中的材料核查信息：已恢复材料编码、名称、规格、清场和设备参数，数量不再取材料。
2. 第一版重复解析payload且整体缺失时可能空指针：已改为解析一次并明确拒绝缺失正式数量。
3. 同一设备在事件与材料中会重复出现：材料设备按材料身份展示，仅补充材料未覆盖的事件设备。
4. 旧数量测试要求缺值归0：已按用户新规则更新，执行真实Vue函数，保留原有整数格式要求。

## 实际验证

- 后端离线Maven编译及 `MesTeamLeaderActiveOrderDetailServiceImplTest`：101项，失败0、错误0、跳过0。
- 后端用例覆盖25/1、26与分配22/4、缺字段拒绝、物料99/9不覆盖原始25/1、真实控制器字段映射。
- 前端真实函数行为测试通过：25/1/26、两事件合计51/1/52、空/单/多物料不重复、两数量字段的无效值拒绝。
- 材料清场与参数优先、提交信息继承、材料设备身份、根设备去重、根独有设备和无设备行为通过。
- 清场状态、空设备隐藏、关键工序标识、参数仅展示四项相关静态检查通过；Vue脚本和模板编译通过。
- 最终整库 `vue-tsc --noEmit -p tsconfig.relaxed.json` 返回2，32条诊断均为CRM的checkPermi缺失，未涉及本轮组件/API字段。不能声称整库类型检查通过。
- `active-order-production-record-device-no-reference-count-static.spec.cjs`失败；使用修改前Vue基线运行也同样失败，未扩大修复范围。
- 后端日志：`p03-backend-test.log`；整库类型检查日志：`p03-frontend-typecheck.log`。

## 运行边界

未运行真实前端E2E，未取得升级后接口响应及页面截图。当前结论为本地代码修复与相关验证完成，不等于运行环境已经生效。
