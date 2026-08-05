# AC-M11 生产报工数量与损耗边界校验

## Task Goal

修复岗位需求矩阵 AC-M11 中生产员工事实报工的数量/损耗边界缺口：后端正式报工必须 fail-fast 拒绝负数产出、负数损耗、损耗数量大于产出数量，禁止通过合格数量截断或默认成功掩盖非法报工。

## Milestones

- [x] 建立任务目录、BDD/TDD 记录和适用门禁
- [x] 复核现有生产报工提交与拆分逻辑
- [x] 先补充失败回归测试，记录 RED
- [x] 实现最小后端校验修复，记录 GREEN
- [x] 更新 AC-M11 矩阵分析与验证报告

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitServiceTest,MesProFrontlineFeedbackPayloadSplitterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `git diff --check -- <task-owned paths>`

## Current Status

ready_for_closeout

已完成 AC-M11 数量/损耗边界代码级修复与目标 JUnit 验证；仍未提交/推送，因为共享工作区存在并行任务脏改动和本分支已有 ahead 状态，后续提交需要单独处理基线与选择性暂存。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务目标是移除非法数量被截断/默认成功掩盖的风险。
- `是否从根因和长期维护角度解决`：是。计划在后端正式生产报工链路加入领域边界校验，并用回归测试固定行为。
- `是否存在临时补丁或绕过`：否。本切片不新增临时兼容分支，不扩大到 AC-M11 其它未验收项。

## Applicable Gates

- Backend API Delivery：后端校验行为必须有 BDD、RED、GREEN 和失败路径验证。
- Bug Regression Fix Loop：先复现非法数量可进入拆分/提交链路的缺口，再用最小修复关闭。
- PowerShell Maven `-D` 参数门禁：目标 Maven 命令中的 `-Dtest` 与 `-Dsurefire.failIfNoSpecifiedTests=false` 必须整体加双引号。
- Maven Reactor 兄弟模块门禁：MES 模块测试使用 `-pl yudao-module-mes -am`，避免兄弟模块旧产物掩盖编译问题。
- No-Fallback：缺少正式数量关系、非法输入或测试前置时必须 fail-fast，不允许 mock、默认成功或静默降级。
