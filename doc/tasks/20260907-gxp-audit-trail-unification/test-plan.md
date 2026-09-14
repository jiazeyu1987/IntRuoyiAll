# 统一 GxP 审计追踪 BDD 与严格 TDD 计划

## BDD Scenarios

### BDD-AT-01 完整审计事件

Given 授权用户修改一个已登记的 GxP 对象，When 用户填写原因并成功保存，Then 系统保存同一事务内的操作人、服务器时间、动作、原因、前值、后值、差异、对象版本和来源请求。

### BDD-AT-02 审计失败回滚

Given 业务数据可以更新但审计事件无法写入，When 用户提交 GxP 变更，Then 整个事务失败且业务数据、版本和关联表均保持原值。

### BDD-AT-03 电子签名强绑定

Given 某批准动作要求电子签名，When 请求缺少签名、签名人不匹配或内容哈希不匹配，Then 业务动作和审计事件均不提交；When 签名有效，Then 审计事件能定位签名人、时间、含义和内容哈希。

### BDD-AT-04 不可变与篡改检测

Given 已提交的审计链和每日清单，When 任一事件被修改、删除、插入或换序，Then 完整性检查失败并指出首个异常链位置，系统不得显示通过。

### BDD-AT-05 关键动作覆盖

Given 一项已登记的 GxP 对象，When 分别执行新增、修改、删除/作废、提交、批准、发布和权限变更，Then 每个成功动作均生成符合其策略的标准事件。

### BDD-AT-06 周期审查

Given 到期审查规则和冻结事件范围，When 质量审查员创建审查批次，Then 系统保存不可变快照和 hash；存在未关闭整改或缺少质量签名时批次不能关闭。

### BDD-AT-07 保存与恢复

Given 事件处于批准保存期或 legal hold，When 普通用户、管理员或清理任务尝试删除，Then 操作被拒绝；When 从独立备份恢复，Then 事件、证据、签名和 hash 全部可验证。

### BDD-AT-08 系统变更追踪

Given 发布包含代码、接口、Migration 或配置变化，When 发布执行，Then 系统保存 releaseTag、commit、文件 hash、Migration、配置快照 hash、操作者、时间和结果。

### BDD-AT-09 可信时间

Given 服务器连接批准的时间源，When 生成审计或签名事件，Then 正式时间由服务器产生并关联最新时间证据；偏差超阈值或证据过期时受控动作按批准策略失败关闭。

### BDD-AT-10 持续覆盖门禁

Given 开发者新增写入口，When CI 扫描发现其未登记、缺少原因/签名策略或缺少测试，Then 构建失败并列出精确入口。

### BDD-AT-11 自包含法规归档恢复

Given 主数据库审计数据不可用且正式 WORM 归档包存在，When 在独立恢复环境只使用归档包恢复指定范围，Then 事件、状态信封、证据关联、签名、领域证据、可信时间和 hash 全部可查阅并可重算，且不依赖生产数据库残留记录。

### BDD-AT-12 连续周期自动审查

Given 已批准的审查计划和固定时钟，When 跨越三个周期且其中一个周期发生停机，Then 每个应审周期恰好形成一个持久化批次，停机周期被补扫，失败和逾期生成告警及不可覆盖运行证据。

### BDD-AT-13 全写边界覆盖发现

Given Controller、领域服务、Job、消费者、Migration 和运维脚本分别新增写入口，When CI 生成实际入口清单并与策略登记表比较，Then 未登记、重复 operationId、缺少测试或不适用批准过期的入口均使构建失败。

### BDD-AT-14 特权篡改与封存窗口

Given 应用账号无修改权限且特权审计已外送，When 普通管理员或特权账号尝试修改/删除事件、执行 DDL、关闭审计或让未封存水位超期，Then 操作被拒绝或独立证据发现异常，治理和发布状态不能为通过。

### BDD-AT-15 CREATE/UPDATE/DELETE 状态信封

Given 三类受控业务动作，When 生成标准事件，Then CREATE 保存 ABSENT 到 PRESENT、UPDATE 保存 PRESENT 到 PRESENT、DELETE/VOID 保存 PRESENT 到 ABSENT/VOIDED；使用 null、UNAVAILABLE 或缺失必需 data 时事件拒绝提交。

### BDD-AT-16 可重算系统变更清单

Given OpenAPI、Migration 顺序、关键配置或镜像 digest 发生变化，When 执行发布预检和发布，Then 生成确定性前后 hash 与结构化 diff并绑定批准、原因和操作者；任一基线或批准缺失时发布失败。

### BDD-AT-17 审计能力强制启用

Given 任一 GxP 模块、操作登记或受控写入口在当前环境启用，When `gxp.audit.enabled` 缺失/关闭、策略 hash 无效或审计内核自检失败，Then 应用启动失败且不开放受控写入口；When 全部前置有效，Then启动自检保存可查询的策略版本和覆盖报告身份。

### BDD-AT-18 跨领域规范展示与独立导出

Given DCC、eDHR、权限、配置和系统变更存在不同专业证据，When 审查员在统一页面查询详情并导出固定范围，Then 所有事件以相同标准字段、状态信封、时间和签名结构呈现，专业证据保持可追溯，导出包可独立重算且关键差异不被冗余内容掩盖。

## TDD Sequence

1. RED：先写 schema 合同测试，证明事件表无 `deleted/updater/update_time`，不存在更新删除 Mapper，且唯一键、hash、策略版本字段齐全。
2. GREEN：实现最小表结构、DO、Mapper 和 append 服务。
3. RED：写真实事务测试，注入审计插入失败并断言业务写入回滚。
4. GREEN：在同一 Spring 事务代理边界中完成业务写入和审计 append。
5. RED：写规范 JSON、hash 链、删除/修改/插入/换序测试。
6. GREEN：实现版本化规范化和完整性校验。
7. RED/GREEN：逐业务动作接入，每次只交付一个可验证切片。
8. RED/GREEN：实现查询、导出、周期审查、时间证据和 WORM 回执。
9. REGRESSION：复跑受影响领域测试、审计内核测试和发布策略门禁。
10. REGRESSION：在隔离环境执行只依赖法规归档包的恢复、连续周期调度、全入口扫描、特权篡改、系统变更清单重算、强制启用和跨领域规范导出。

## Required Test Layers

- 单元测试：字段规则、脱敏、规范化、hash、策略匹配、错误模型。
- 数据库测试：唯一约束、权限、并发序列、不可更新删除、真实事务回滚。
- 服务集成测试：每个 GxP 动作的 before/after、原因、签名和事件数量。
- 静态合同测试：写入口登记、注解、策略和测试映射完整。
- 恢复测试：数据库、对象证据、清单、签名和时间证据联合恢复。
- 归档独立性测试：清空演练数据库后，仅以 WORM 归档包恢复并完成查询、导出和 hash 重算。
- 调度运行测试：固定时钟覆盖连续周期、停机补扫、失败重试、重复触发和逾期告警。
- 覆盖发现测试：对每类写边界植入未登记样例，证明 CI 会失败且报告精确来源。
- 特权安全测试：使用隔离数据库权限模拟 DML、DDL、授权和关闭审计，验证独立外送证据。
- Playwright E2E：仅在用户当轮明确授权后，通过真实页面执行原因输入、签名、业务动作、审计查询和导出；API/DB 只做只读核验。

## Requirement Traceability

| 检查项 | 主要设计控制 | 主要场景 |
| --- | --- | --- |
| 2.1 | 统一内核、强制登记、启用自检和启动失败 | BDD-AT-01、10、17 |
| 2.2 | 强类型事件、状态信封、原因 | BDD-AT-01、15 |
| 2.3 | 持久化计划、自动周期审查和补扫 | BDD-AT-06、12 |
| 2.4 | append-only、hash、特权审计、密封水位、WORM | BDD-AT-04、07、14 |
| 2.5 | 版本化 GxP 覆盖登记和全写边界 CI | BDD-AT-05、10、13 |
| 2.6 | signatureRecordId 和内容 hash | BDD-AT-03 |
| 2.7 | 保存策略、自包含法规归档、独立恢复 | BDD-AT-07、11 |
| 2.8 | 系统级事件和可重算变更清单 | BDD-AT-08、16 |
| 2.9 | 受控服务器时间和时间证据 | BDD-AT-09 |
| 2.10 | 标准事件 schema、统一 UI 和独立导出 | BDD-AT-01、04、18 |

## Evidence Log Template

- `BDD: <scenario> -> Given/When/Then`
- `RED: <command> -> FAIL, <expected reason>`
- `GREEN: <command> -> PASS`
- `REGRESSION: <command> -> PASS`
