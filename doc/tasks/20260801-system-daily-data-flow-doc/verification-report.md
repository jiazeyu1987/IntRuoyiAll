# Verification Report

## Summary

已将“系统未来一天的工作 / 数据流转”写入 `C:\Users\BJB110\Desktop\文档\职责\系统.md`，并完成 UTF-8 读取与关键内容验证。

## Verification Commands

- `python -X utf8 -c "...系统.md..."` -> PASS，输出 `UTF8_READ_OK target_chars= 3165`，标题为 `# 系统一天的数据流转`。
- `rg -n "ERP订单/物料数据|系统生产订单池|一线报工数据|PQC检验任务|过程检验记录|完整性检查|放行待办" C:\Users\BJB110\Desktop\文档\职责\系统.md` -> PASS，关键数据流转节点均已覆盖。
- `Get-Item -LiteralPath C:\Users\BJB110\Desktop\文档\职责\系统.md` -> PASS，确认文件已生成，大小 8115 字节。

## Scope Verification

- 文档覆盖 ERP 订单和物料数据进入系统。
- 文档覆盖生产订单池、一线报工、班组长确认与订单分配。
- 文档覆盖 QA 检验规程、PQC 检验任务、PQC 组长确认和过程检验记录。
- 文档覆盖批记录完整性检查、放行待办和订单结束。
- 文档明确计划排产员、仓库、物料员不登录本系统操作，本系统通过 ERP 数据和现场角色提交形成闭环。
