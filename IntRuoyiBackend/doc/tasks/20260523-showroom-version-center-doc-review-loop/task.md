# 任务：展厅版本中心设计文档评审闭环

## 任务目标

- 以 reviewer 主导、子 agent 协同修订的方式，对 `20260523-showroom-version-center-design-docs` 设计文档执行放行评审闭环。
- 评审与修订必须达到以下标准：
  - 逻辑自洽，没有实现级矛盾或隐藏 bug
  - 与当前主系统逻辑一致，不与现有 release / revision / narration / preview asset 主链路冲突
  - 接口清晰，返回结构、权限边界、事务边界、阻断规则足够明确
  - 文档明确下一阶段按 BDD + Strict TDD + Subagent-Driven Development 执行

## 非目标

- 本任务不实现生产代码。
- 本任务不新增临时兼容方案、fallback、默认成功逻辑或静默降级规则。
- 本任务不重做业务需求，仅基于当前已确认范围修正文档。

## 前序任务检查

- 已检查 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-showroom-version-center-design-docs\task.md`
- 该任务状态为 `已完成`
- 不阻塞本次评审闭环任务启动

## 里程碑

- [x] M1：建立本次评审任务目录、任务文档与执行日志。
- [x] M2：主 reviewer 完成本地首轮审查并形成阻塞问题清单。
- [x] M3：子 agent 完成独立系统对齐审查与文档修订。
- [x] M4：主 reviewer 复审并给出最终放行结论。

## 预期验证

- 文档评审输出必须覆盖：
  - 逻辑层
  - 系统契约层
  - 接口清晰度
  - BDD/TDD/Subagent 实施方式
- 本任务 `execution-log.md` 必须包含：
  - BDD 场景
  - RED：首轮 review 发现阻塞问题
  - GREEN：复审通过或明确阻塞
- 若文档被修订，以下文件必须同步更新并保持 UTF-8 可读：
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-showroom-version-center-design-docs\frontend-design.md`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-showroom-version-center-design-docs\backend-api-design.md`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-showroom-version-center-design-docs\data-model.md`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-showroom-version-center-design-docs\config-security-deployment.md`

## 当前状态

- 状态：已完成
- 已完成：
  - 已确认需要以 reviewer 主导方式复审版本中心设计文档，而不是直接进入实现
  - 已确认本轮允许使用子 agent 做独立审查与文档修订
  - 已完成主 reviewer 首轮审查，识别出路由定案、当前内容/当前线上语义、接口 shape、公司历史快照、全局 release blocker、实施方式约束六类阻塞问题
  - 已接收独立 reviewer findings，并据此修订版本中心设计文档
  - 已完成复审，确认文档满足逻辑自洽、主系统对齐、接口清晰、BDD/TDD/Subagent 实施约束四项要求
- 待完成：
  - 如用户确认，下一任务进入实现阶段
- 阻塞与影响：
  - 无阻塞，已放行文档进入实现准备阶段
