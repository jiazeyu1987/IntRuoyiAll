# 可信时间与审查证据最小闭环范围调整

## Request Summary and Source

- 来源：用户 2026-09-07 明确要求按最小实现尽快闭环，先简单实现；审查老师提出明确异议后再扩展。
- 请求：检查现有可信时间开发文档，删除或延期非必要能力，保留能证明检查清单 2.9 和加强 4.8 的最小实现与证据导出。

## Current Baseline Reviewed

- `doc/tasks/20260907-trusted-time-audit-evidence/task.md`
- `doc/tasks/20260907-trusted-time-audit-evidence/prd.md`
- `doc/tasks/20260907-trusted-time-audit-evidence/development-plan.md`
- `doc/tasks/20260907-trusted-time-audit-evidence/test-plan.md`
- 当前基线包含自研节点证明、非对称验签、可信时间签发 API、逐业务回执、V2 UTC schema、哈希链、事故中心、异步导出、对象归档和离线签名验证，超出赶工闭环的最小需要。

## Classification

- 类型：期限驱动的范围缩减，同时保留合规核心结果。
- 风险：如果把时间同步、正式签名时间或可导出证据一并删减，将无法回答审查问题；这些能力不可延期。

## Impact Analysis

### Product Impact

- 保留一个“可信时间证据”区域和一个“导出时间戳证据”按钮。
- 首版不建设完整事故管理、配置管理和逐签名回执页面。

### Design Impact

- 复用现有 Runtime Control 远程执行、操作记录和告警能力。
- 复用操作系统 chrony、现有服务器时间和现有签名表，不新增统一可信时间平台。
- 定时脚本把每台服务器的时间状态写成只追加 JSON 证据文件；导出只读取这些文件和当前签名抽样结果。

### Data Impact

- 首版不新增可信时间样本、签发回执、事故、配置快照和导出五张业务表。
- 不做全库 UTC 迁移；首版冻结并记录当前 `Asia/Shanghai` 统一时间基准。
- 保留现有 `signedAt` 为正式服务器签署时间；现有用户选择时间只作为业务时间展示，不得覆盖正式签名时间。

### API Impact

- 不新增 `TrustedTimeApi` 和节点证明提交 API。
- 仅在 Runtime Control 增加当前状态、证据包导出和证据包下载最小接口。
- 正式签名接口继续由服务器生成 `signedAt`，客户端不得提交该字段。

### Test Impact

- 保留 chrony 状态解析、签名时间不可覆盖、业务时间独立、证据导出内容和失败暴露测试。
- 延期非对称验签、哈希链连续性、逐业务回执、冻结水位并发和 Object Lock 测试。

### Release and Operations Impact

- 需要在正式服和审查服配置 chrony、只追加证据目录及 systemd timer；属于远程生产级操作，仍需单独授权。
- 时间异常首版复用现有告警并在报告中标红，不自动阻断全部业务签名；若审查要求失败关闭，再进入增强阶段。

## Decision

ACCEPT AND SPLIT：接受最小闭环；原完整方案降为后续增强，不作为首版完成门禁。

### MVP 必做

- 正式服、审查服及相关宿主机使用受控 chrony 时间源。
- 普通用户和应用账号不能修改服务器时间。
- 正式电子签名时间由服务器生成，用户业务时间独立显示。
- 定时保存 chrony 状态、偏差、时间源、服务器时间和数据库时间证据。
- Runtime Control 提供当前状态和一键导出证据包。
- 导出包包含审查摘要、原始 JSON 和 SHA-256 清单；异常或缺失必须明确显示，不得输出默认通过。

### Deferred Until Explicit Review Objection

- 自研节点签名代理和非对称验签。
- `TrustedTimeApi`、逐业务签发回执及五张治理表。
- 全库 UTC 数据迁移和 V2 schema 硬切换。
- 每分钟哈希链、冻结水位、异步导出任务和导出历史中心。
- Object Lock/WORM、manifest 数字签名和独立离线校验工具。
- 自动阻断全部签名、发布、备份和恢复。
- 独立事故/CAPA 管理中心和配置审批页面。

## Required Approvals

- 实施前确认企业允许使用的 NTP 地址；缺失时不得自行选择公网时间源。
- 远程配置、服务重启、定时任务安装、正式服和审查服验证必须获得当轮授权。
- 证据采集周期和偏差阈值首版使用明确配置值，不能硬编码为合规标准。

## Downstream Skill Reruns

- 更新 `roadmap-node-dev-plan` 任务包的范围、里程碑、BDD、验收和 task-state。
- 实施阶段按 backend、frontend、database/ops 对应交付技能执行严格 TDD。

## Blockers and Next Action

- 当前文档更新不阻塞。
- 首版开发前唯一外部业务输入是受控 NTP 地址、偏差阈值和采集周期。
- 下一步按最小范围重写现有任务包，随后从本地 RED 测试开始，不直接操作远程环境。

