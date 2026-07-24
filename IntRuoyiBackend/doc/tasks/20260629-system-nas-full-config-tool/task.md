# 任务：系统 NAS 配置工具扩展为完整连接参数台（后端）

- Task ID: `20260629-system-nas-full-config-tool`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

扩展 `yudao-module-infra` 的 NAS 配置契约、保存逻辑、SMB 连接配置与复用链路，使 `/system/nas` 能配置并真正生效当前 SMB NAS 模式下的完整连接参数。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-smart-scheduling-smoke-route-config-cross-tenant\task.md`
- 状态：`blocked`
- 处理说明：已按本轮用户优先级切换显式阻塞，避免与本次 NAS 基础设施参数扩展混改。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接扩展正式 VO/Service/ConnectionConfig/RuntimeControl 复用链路，不做兼容分支掩盖缺参。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 保存 NAS 配置时持久化完整 SMB 参数 -> Given 用户提交带 domain、port、authType 等字段的 NAS 配置 / When 后端保存 / Then 所有字段都按正式 config key 持久化，并在读取接口中完整返回。`
- `BDD: 测试连接时使用完整 SMB 参数 -> Given 用户提交完整 NAS 参数 / When 后端执行 testConnection / Then NasConnectionConfig 应包含新增字段，SMBJ 认证使用对应 domain/port。`
- `BDD: 复用链路重建 NAS 配置对象时不丢字段 -> Given Runtime Control 或 SRM NAS 定位从当前配置派生 share 级连接 / When 系统重建 NasConnectionConfig / Then 新增字段与原配置保持一致。`

## Milestones

1. M1：补 RED 测试锁定新参数契约。`completed`
2. M2：实现最小后端改动并跑 GREEN。`completed`
3. M3：更新证据与收尾。`completed`

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-infra "-Dtest=NasSettingsServiceTest,NasBrowserServiceImplTest,FileControllerTest" test`

## Current Blockers

- 无。

## Final Verification Result

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-infra "-Dtest=NasSettingsServiceTest,NasBrowserServiceImplTest,FileControllerTest" test` -> PASS

## Completion Result

- 后端 NAS 契约已从 4 项扩展为当前 SMB 体系下真实生效的 6 项：`server`、`port`、`share`、`domain`、`username`、`password`。
- `NasBrowserServiceImpl` 已按 `server + port + domain + username + password + share` 建立 SMBJ 连接。
- Runtime Control 发布/备份仓链路与 SRM NAS share 派生链路已同步带上新增参数，不再中途丢失 `port/domain`。
- 本次未引入无效占位参数，也未扩展到当前底层未使用的伪配置项。
