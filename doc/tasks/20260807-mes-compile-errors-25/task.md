# MES 25 个编译错误修复

## 任务目标

- 在保留共享工作区现有改动的前提下，复现并逐个修复当前 MES 模块的 25 个编译错误。
- 识别每个错误的正式类型、接口或实现契约来源，不通过 fallback、默认值或吞异常掩盖错误。
- 完成 MES reactor 编译与相关定向回归验证。

## 里程碑

- [ ] M1：建立任务证据并复现、归档全部编译错误。
- [ ] M2：按根因分组修复 25 个编译错误，并补充或更新对应回归测试。
- [ ] M3：通过定向测试、MES reactor 编译及相关回归。
- [ ] M4：完成证据校验、经验沉淀和任务清理。

## 预期验证

- RED：`mvn -pl yudao-module-mes -am -DskipTests compile` 稳定复现当前编译错误。
- GREEN：同一 Maven reactor 编译命令通过。
- REGRESSION：运行受影响测试类以及 `mvn -pl yudao-module-mes -am test`；若共享环境资源导致无法完成，记录精确阻塞证据，不以较小范围冒充通过。
- 运行 bug regression evidence validator 与任务清理 preview/apply。

## 经验门禁

- 已读取 `docs/experience-index.md`，命中以下门禁：
  - MES companion contract：实现引用新增 VO、Mapper、Service 或 `@Override` 时，必须核对对应正式合同文件并执行 `mvn -pl yudao-module-mes -am "-DskipTests" compile`；禁止删除调用或注释实现来绕过编译。
  - Maven Reactor 兄弟模块：MES 依赖兄弟模块时必须保留 `-am`，不得用本地陈旧依赖得出结论。
  - Windows Maven 并发/目标目录：同仓已有 Maven 写入同一 `target` 时不得叠加命令或停止其它任务进程；须等待并发进程退出后再运行本任务 RED/GREEN。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；以正式 Java 类型和服务契约为准修复编译断裂。
- 是否存在临时补丁或绕过：否。

## Current Status

blocked：用户授权处理 PID 49972 时该进程已自行退出；随后标准 MES reactor `compile`、全量 `testCompile` 以及从空 MES `target` 编译 2540 个主源码文件均为 `BUILD SUCCESS`，当前错误数为 0。缺少可复现的 25 个错误清单，不能按 strict TDD/no-fallback 要求虚构错误并启动 6 个修复子线程；需要实际失败命令和完整错误输出，或能复现错误的源码状态。
