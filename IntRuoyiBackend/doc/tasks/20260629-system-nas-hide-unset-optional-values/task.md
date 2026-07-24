# 任务：NAS 未设置可选参数不作为当前已配置值返回（后端）

- Task ID: `20260629-system-nas-hide-unset-optional-values`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

调整 `/infra/file/nas-config` 的读取与保存语义：未显式设置的 NAS 可选参数不再伪装成“当前已配置值”返回给前端；同时保留运行时默认端口行为，避免影响现有 NAS 连接链路。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-system-nas-full-config-tool\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成 NAS `port/domain` 正式参数接入；本次只补“展示语义”和“清空可选参数”行为，不改 SMB 真实连接能力边界。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `docs/powershell-memory.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - PowerShell 中文读写与任务文档更新统一显式 UTF-8。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。区分“读取给界面展示的当前已配置值”和“运行时使用的默认端口语义”，避免把默认值伪装成已配置值。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 未设置端口时读取接口不返回伪配置值 -> Given infra.nas.port 当前没有真实配置 / When 前端读取 NAS 配置 / Then 响应里的 port 为空，而不是默认返回 445。`
- `BDD: 运行时读取 NAS 配置时仍使用系统默认端口 -> Given infra.nas.port 当前没有真实配置 / When 后端构建实际 NAS 连接参数 / Then NasConnectionConfig 仍使用默认 SMB 端口，不影响现有连接链路。`
- `BDD: 清空可选参数时持久层不保留空壳配置 -> Given 用户把 NAS 端口或域清空保存 / When 后端处理保存请求 / Then 对应可选参数配置被删除，不再作为“当前已配置值”返回。`

## Milestones

1. M1：补 RED 测试锁定“未设置不返回伪配置值”与“运行时默认端口仍可用”。`completed`
2. M2：实现最小后端修复与清空保存语义。`completed`
3. M3：跑 GREEN 并更新证据。`completed`

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-infra "-Dtest=NasSettingsServiceTest,FileControllerTest,NasBrowserServiceImplTest" test`

## Current Blockers

- 无。

## Final Verification Result

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-infra "-Dtest=NasSettingsServiceTest,FileControllerTest,NasBrowserServiceImplTest" test` -> PASS

## Completion Result

- `GET /infra/file/nas-config` 不再把未显式配置的 `port` 伪装成当前值 `445` 返回。
- 运行时实际建立 NAS 连接时，`NasConnectionConfig` 仍会对空端口使用默认 SMB 端口，不影响现有连接链路。
- 清空 `port/domain` 后保存，会删除对应配置项，后续页面读取时不再显示这些未设置字段。
