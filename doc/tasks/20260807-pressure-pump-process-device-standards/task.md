# 压力泵工序设备参数配置

## Task Goal

将 `C:\Users\BJB110\Desktop\文档\批记录压力泵_工序设备参数_完成版.xlsx` 中的工序与设备对应关系、设备参数标准，按正式数据链路配置到本机 `芋道源码` 租户（tenantId=1）admin（userId=1）的生产组长“工序配置”页；源文件中有编号的设备先补齐正式设备台账，再配置班组设备、工序设备映射和参数标准。

## Source Baseline

- 源文件 SHA256：`7AA1EF1A9B8981175B9C8A05375C19B71D66D29127F7DC6E33F669199A9E580E`
- 工作表：`工序设备参数`，17 行、14 列
- 目标工艺路线：`RT000028 / 球囊扩张压力泵`，routeId=922119
- 目标范围：11 台正式设备、11 台生产组长班组设备、13 条工序设备映射、45 条设备参数标准
- 特别口径：光固机只使用用户明确指定的 `A05075`；不使用源单元格中的旧编号 `A05059`
- 无设备编号的“清洁”标准，以及无设备/参数的“硅化Ⅱ、组装Ⅱ、中包装、大包装”，不写入设备参数标准

## Milestones

- [x] M1：冻结 Excel 来源、目标租户/用户/路线和 schema/API/UI 现状
- [x] M2：用 BDD + 严格 TDD 扩展参数标准模型，正式支持原文标准和文本标准
- [x] M3：通过真实生产组长页面补齐正式设备、班组设备、工序设备映射和参数标准
- [x] M4：完成定向回归、真实页面 E2E、只读 API/数据库终态核验
- [x] M5：归档验证证据、经验复盘和任务清理

## Expected Verification

- OfficeCLI 校验源工作簿结构与内容，文件哈希与本任务基线一致
- 数据库迁移合同和 release migration policy gate 通过
- 后端目标 JUnit 覆盖数值标准、文本标准、校验失败和前台只读展示边界
- 前端目标静态合同、相邻合同和 `pnpm ts:check` 通过
- Playwright 从真实本机页面完成设备台账与生产组长工序配置写入，目标接口 HTTP 2xx 且业务码为 0
- 只读 API/数据库核对目标租户 admin：11 台班组设备、13 条映射、45 条参数规则；光固机仅为 A05075
- 不修改 tenantId=1 其他生产组长数据、不修改 tenantId=122 数据、不触碰无关脏改动

## Data Safety

- 用户已明确指定 `芋道源码 / admin` 为本次目标，并选择“先补台账再配置”
- 写入仅限本机 `E:\IntRuoyi` 对应数据库和本机前端真实用户路径
- 目标业务数据是用户要求的正式配置，不在验收后删除
- 迁移和业务写入前后记录精确租户、业务键、行数与终态；不得扩大范围或静默切换租户

## Experience Gate

- 已读取 `docs/experience-index.md`，命中以下正式门禁：
- 生产组长工序配置维护：`process-config/list`、设备映射和设备参数标准必须先按 `mes:pro-process-pool-team-leader:maintain` 判断正式维护权限；不得被工序开始快照误拦，也不得扩大无权限用户范围。
- 前端按钮行为：新增/保存按钮必须调用真实维护弹窗和写接口，静态合同同时锁定可见文案、稳定 `data-*` 锚点和点击处理器。
- 写入型远程下拉：设备候选必须在每次真实 E2E 前核对新鲜度，页面按可见业务唯一编号选择，保存后只读核验正式 ID。
- Schema-backed E2E：真实页面验证前必须应用正式迁移并核对当前运行库字段，不得以页面文案或 API wrapper 存在替代 schema 证据。
- 参数采集权限边界：本任务不修改设备账号/岗位/工作站授权；文本标准仅扩展参数载体，不得成为一线工序授权 fallback。

## Design Decisions

- 数值精确值 `x`：`lower=x, target=x, upper=x`
- 数值范围 `x-y`：`lower=x, target=NULL, upper=y`，不推断中点
- 公差 `x±d`：`lower=x-d, target=x, upper=x+d`
- 文本标准：保留 Excel 原文到 `standardText`，类型为 `TEXT_STANDARD`，数值边界为空
- 所有规则均保留 Excel 原文标准；前台文本标准只读展示，不生成数值采集读数
- 参数编码按 `PP_<两位工序序号>_<设备编号>_<两位参数序号>` 确定性生成

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否
- 是否从根因和长期维护角度解决：是；扩展正式参数标准模型，不用前端文案或默认值伪装文本标准
- 是否存在临时补丁或绕过：否

## Cleanup Keep

- doc/tasks/20260807-pressure-pump-process-device-standards/pressure-pump-config-real.e2e.cjs
- doc/tasks/20260807-pressure-pump-process-device-standards/artifacts/pressure-pump-process-config-desktop.png
- doc/tasks/20260807-pressure-pump-process-device-standards/artifacts/pressure-pump-process-config-mobile.png

## Current Status

completed：最新后端已运行在 `48081`，真实 Playwright UI 已完成压力泵 11 台设备、13 条工序设备映射和 45 条参数标准配置；只读数据库终态核验通过，光固机仅为 `A05075` 且旧编号 `A05059` 不存在。验证报告和可保留 E2E 证据已归档，cleanup preview/apply 已通过；本任务不执行 Git 提交、合并或推送。
