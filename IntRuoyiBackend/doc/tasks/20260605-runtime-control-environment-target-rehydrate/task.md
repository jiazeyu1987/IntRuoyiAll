# 任务：运行控制环境目标回填

## 任务目标

修复 Runtime Control 在 `application-local.yaml` 只覆盖 `yudao.runtime-control.environments.*.host` 时，Spring 绑定把默认 `targets` 清空，导致 `环境(test) 组件(intruoyi-frontend)` 等目标不存在的问题。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；目标配置缺失时仍 fail fast。
- `是否从根因和长期维护角度解决`：是；在配置属性绑定后统一回填默认环境结构，而不是在业务层临时兜底。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 局部 host 覆盖不应丢失默认 target -> Given `application-local.yaml` 只配置 test/prod/backup 的 host / When Runtime Control 属性绑定完成 / Then `intruoyi-frontend`、`intruoyi-backend`、`intruoyi-full`、`website-frontend` 仍然存在。
- BDD: 覆盖后的远端 URL 应与当前 host 一致 -> Given test/prod/backup 的 host 被覆盖 / When 读取 Runtime Control 目标 URL / Then 远端状态 URL 必须使用覆盖后的 host 重新生成。

## 里程碑

- [x] M1：补 RED 测试复现部分环境绑定后 target 丢失。
- [x] M2：实现 RuntimeControlProperties 绑定后默认环境回填。
- [x] M3：跑相关 Java 回归并记录结果。

## 预期验证

- `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test`

## 当前状态

completed

## Current Status

completed

## 完成记录

- 已在 `RuntimeControlProperties.afterPropertiesSet()` 中统一回填默认环境结构。
- 已覆盖 host-only 覆盖下 target 不丢失、远端 URL 使用覆盖 host、正式环境 host-only 覆盖仍保持禁用。
- 验证通过：`mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test`，46 tests passed。
