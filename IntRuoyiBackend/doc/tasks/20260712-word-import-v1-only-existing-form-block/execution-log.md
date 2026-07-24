# Execution Log

BDD: v1 only first import -> Given 产品/批记录还没有 MAIN 表单, When 用户通过导入按钮导入 Word, Then 系统只生成 V1.0 版本并展示产品名称。
BDD: existing form blocks reimport -> Given 产品/批记录已存在 MAIN 表单, When 用户再次通过导入按钮导入 Word, Then 系统拒绝导入并提示必须先删除已有表单。
GREEN: experience-preflight -> PASS, 沿用当前线程已读取 PowerShell、登录和批记录 Word 导入门禁。
RED: legacy upgrade tests -> FAIL, 旧测试仍期望无表单有执行时可升到 V2.0，与当前 V1.0 首版导入口径冲突。
GREEN: targeted backend v1-only tests -> PASS, 6 tests passed，覆盖新导入固定 V1.0、已有 MAIN 阻断、历史执行引用阻断和版本表 schema。
GREEN: frontend static v1-only contract -> PASS, 批记录表单列表和模板页均固定 upgrade=false，已有 MAIN 表单提示先删除。
GREEN: local restart and real v1-only import -> PASS, 前后端重启后测试租户首次导入返回 V1.0/APPROVED，再次导入被已有 MAIN 表单阻断，列表产品名称完整。
