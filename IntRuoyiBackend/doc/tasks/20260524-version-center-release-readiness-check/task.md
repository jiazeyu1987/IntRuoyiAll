# 任务：版本中心合并后发布就绪性核查

## 任务目标

- 核查 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` 当前 `int_main` 在版本中心合并后是否具备直接发布前置条件。
- 只做核查与结论输出，不执行真实发布、不改线上环境。

## 非目标

- 不执行测试服/正式服发布。
- 不修改代码或脚本，除非发现必须记录的阻塞。
- 不用 fallback 掩盖发布阻塞。

## 前序任务检查

- 已检查主线融合任务：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260523-showroom-version-center-impl\ruoyi-vue-pro\doc\tasks\20260524-showroom-version-center-int-main-merge\task.md`
- 状态：已完成并已快进合入 `int_main`

## 里程碑

- [x] M1：建立核查任务记录。
- [x] M2：核对主仓工作树、分支头和最近提交。
- [x] M3：核对发布相关验证、脚本前置条件与已知阻塞。
- [x] M4：给出后端发布就绪结论。

## 预期验证

- `git status --short`
- 最近发布相关验证结果复核
- 发布脚本/发布前置条件核查

## 当前状态

- 状态：已完成

## 结论

- 后端侧发布前置条件已满足。
- 已确认：
  - 当前后端主仓 `int_main` 头提交为 `62a0b0aff6`
  - 工作树在本任务记录提交前可保持干净
  - 测试服/正式服状态脚本可正常执行并返回健康状态
  - 发布脚本自测 `script/tests/test_publish_int_ruoyi_to_test_tooling.py` `18 PASS`
  - `mvn -f pom.xml -pl yudao-server -am -DskipTests package` -> PASS
  - `mvn -pl yudao-module-showroom -DskipTests compile` -> PASS
- 后端本身没有发现新的发布阻塞。

## 说明

- 最终“是否能直接发布”仍受前端仓发布核查结果约束；本仓结论仅表示后端侧 ready。
