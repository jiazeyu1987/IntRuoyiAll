# Execution Log

BDD: 恢复数据默认数据责任人为 admin -> Given 责任矩阵没有显式保存 `prod + restore-data + data-owner` / When 用户提交恢复数据 / Then 后端应使用默认 `admin` 通过责任人门禁，并继续校验恢复候选。

BDD: 显式数据责任人优先生效 -> Given 运维人员保存了 `prod + restore-data + data-owner` 的显式责任人 / When 查询责任矩阵或提交恢复数据 / Then 显式责任人应覆盖默认 `admin`。

BDD: 演练和快照仍不阻断恢复候选 -> Given 恢复候选缺少演练报告和现场快照 / When 用户打开恢复数据候选 / Then 候选不应因为这两项被阻断。
