# 产品需求：可信时间最小闭环

## Purpose and Scope

首版只回答三个审查问题：服务器时间是否同步、正式签名时间能否被用户修改、系统能否立即导出证据。

## Requirements

- R1：Runtime Control 巡检读取正式服、审查服的 chrony 状态、时间源、偏差、Leap 状态、服务器 UTC 时间和数据库时间。
- R2：`signedAt` 继续由服务器生成；`signatureDisplayAt` 必须等于 `signedAt`。
- R3：`selectedSignedAt` 作为独立业务发生时间，保留原因和时区，不能替代签名时间。
- R4：时间检查写入现有 `inspection-runs.json`；测试阶段未配置偏差阈值时仍采集并展示 Last/RMS，不因偏差数值阻断，但 chrony、同步、NTP、Leap、Stratum、服务器/数据库 UTC 或远程命令异常仍必须使巡检不能 PASS。
- R5：运行控制台提供一个“导出时间戳证据”按钮，导出指定已保存巡检。
- R6：ZIP 固定包含 `审查摘要.html`、`原始证据.json`、`SHA256SUMS.txt`。

## BDD Scenarios

- Given 用户填写业务发生时间 / When 签名 / Then 正式签名展示仍为服务器 `signedAt`。
- Given chrony 正常 / When 巡检 / Then 保存实际时间源和偏差并判定 PASS。
- Given chrony 缺失、无选中源或远端不可达 / When 巡检 / Then 明确 BLOCKED，整体不能 PASS。
- Given 巡检已保存 / When 导出 / Then ZIP 三个文件都来自同一巡检 ID，且不重新采集。
- Given 巡检异常 / When 导出 / Then 摘要保留异常且不得显示通过。
- Given 测试阶段未配置偏差阈值 / When 巡检 / Then 保存实际偏差且不执行数值超限判定，其它时间可信性门禁保持生效。

## Acceptance Criteria

- AC-01：正式签名展示时间不能被用户业务时间覆盖。
- AC-02：正式服、审查服时间状态进入现有巡检结果。
- AC-03：时间命令失败或证据缺失时巡检不能 PASS。
- AC-04：页面显示时间检查结果并可导出指定巡检。
- AC-05：ZIP 固定三文件且 SHA-256 正确。
- AC-06：导出不修改巡检、签名或审计记录。
- AC-07：未配置偏差阈值时仍采集 Last/RMS 并允许其它证据正常的时间项 PASS；配置正数阈值后恢复 Last/RMS 超限阻断。

## Open Questions

- 企业受控 NTP 地址、正式环境偏差阈值和巡检周期在正式上线前必须提供。
