# 20260805 Start Backend Runtime

## Task Goal

启动 `E:\IntRuoyi` 的 `int_main` 本地后端运行态，并验证 `http://127.0.0.1:48081/actuator/health` 返回 `UP`。

## Milestones

- [x] 读取本地运行、端口矩阵、任务收尾和编码规则。
- [x] 读取 `docs/experience-index.md` 并识别本次命中的本地后端启动门禁。
- [x] 检查 `48081` 端口占用和已有后端归属。
- [x] 按正式本地运行链路启动或确认后端。
- [x] 验证后端 health 状态并记录证据。

## Expected Verification

- `git status --short --branch` 记录启动前工作区状态。
- `48081` 端口归属检查必须确认是否为 `E:\IntRuoyi\IntRuoyiBackend` 的 `int_main` 后端。
- 后端健康检查必须使用 `http://127.0.0.1:48081/actuator/health`，返回 `status=UP` 才可宣称启动成功。
- 如源码冲突、数据库依赖、Redis、端口占用或启动脚本前置条件缺失，必须 fail fast，不得换端口、复用旧 Jar、切换数据源或降级成功。

## Prior Blocker

- `48081` 当前无监听，health 连接被拒绝。
- 当前工作区已有未合并冲突，且冲突位于后端 Java 源码和测试文件。标准本地启动脚本会先执行 `mvn -pl yudao-server -am -DskipTests package` 再复制独立运行 Jar；在冲突未解决前继续启动会变成失败构建或旧 Jar 降级启动。
- 按严格 no-fallback 规则，本次不使用旧 `target` Jar、不换端口、不跳过 Maven 构建、不修改数据源。
- 继续执行时复核 `git ls-files -u` 已无 unmerged index，相关文件无冲突标记，因此继续标准启动验证。

## Final Verification

- 标准脚本生成独立 runtime Jar：`output/runtime/int_main/backend-runtime-control-20260805-222248.jar`。
- Jar SHA256：`4EA3E8BB6C585C738EB1F99AFE42C33827CB2908E275242819646213488F5A1F`。
- 后端 Java PID：`60192`，监听 `48081`。
- 延迟复验 `http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`。
- 日志确认 `Tomcat started on port 48081`、`Started YudaoServerApplication`、`项目启动成功`。

## Applicable Gates

- `docs/local-runtime.md#固定端口`：`E:\IntRuoyi` 的 `int_main` 后端固定使用 `48081`。
- `docs/local-runtime.md#启动前检查`：启动前必须检查 `48081` 占用；未知进程或非 IntRuoyi 进程占用时 fail fast。
- `docs/local-runtime.md#2026-07-25-本地后端数据库凭据门禁`：本地 MySQL/数据源不可达时不得宣称后端启动成功。
- `docs/local-runtime.md#2026-07-27-本地后端运行-Jar-不可变门禁`：长期运行后端不得直接使用会被 Maven 覆盖的 `target` Jar。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务仅执行正式本地启动链路和健康检查。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed
