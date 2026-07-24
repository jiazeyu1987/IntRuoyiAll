# 881MO090863 eDHR 全批次真实 E2E 证据

- 状态：PASS
- 批次号：E2E-881MO090863-20260611-012300
- 工单：881MO090863
- 路线：ROUTE-YXN.069.001.1001
- 模拟前缀：E2E模拟填写-881MO090863-20260611-012300
- 生成时间：2026-06-10T17:26:45.240Z

## BDD

- BDD: 创建批次执行 -> Given 芋道源码/admin 存在目标工单和路线 When 真实前端打开或创建批次 Then 批次详情展示 21 道工序、15 张必填批记录和 0 阻塞。
- BDD: 逐张填写签名审批 -> Given 15 张必填批记录 When 逐张打开填写、字段审计、复核签名、追溯校验、提交和审批 Then 全部批准。
- BDD: 关闭归档复盘 -> Given 批次可关闭 When 关闭、归档、下载/打印并打开复盘 Then 可查看填写、签名、审批、关闭和归档记录。

## GREEN

- GREEN: `pnpm e2e:edhr:881-full-flow` -> PASS。
- 批次执行ID：54
- 生产任务上下文：新增 0，复用 15
- 排产页产品路线漂移：当前产品路线 900025，工序数 24；本批次仍使用 900022 / ROUTE-YXN.069.001.1001
- 完成批记录：15 / 15
- 批次批准数：15 / 21
- 阻塞项：0
- 归档状态：SEALED
- 下载文件：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\test-results\20260610-edhr-881MO090863-full-e2e-fill\E2E-881MO090863-20260611-012300-edhr-final.pdf`
- 打印窗口：已打开
- 复盘记录：执行 15，字段审计 14，审批 15，归档 1

## 执行明细

- sort 1 B010 吹球囊成型 -> BRE202606110123162540256, fields=120, fieldAudit=true, approvalSignatureId=663
- sort 2 B020 球囊全检 -> BRE202606110123471650257, fields=0, fieldAudit=false, approvalSignatureId=666
- sort 3 B030 球囊裁剪 -> BRE202606110123540970258, fields=2, fieldAudit=true, approvalSignatureId=670
- sort 4 B040 外管拉伸 -> BRE202606110124031420259, fields=46, fieldAudit=true, approvalSignatureId=674
- sort 5 B050 内管拉伸 -> BRE202606110124157250260, fields=10, fieldAudit=true, approvalSignatureId=678
- sort 6 B140 外管、内管、球囊热处理 -> BRE202606110124333750261, fields=22, fieldAudit=true, approvalSignatureId=682
- sort 7 B060 外管与球囊焊接 -> BRE202606110124447710262, fields=2, fieldAudit=true, approvalSignatureId=686
- sort 8 B200 焊接远端第一步 -> BRE202606110124537540263, fields=2, fieldAudit=true, approvalSignatureId=690
- sort 9 B290 焊接圆角 -> BRE202606110125028160264, fields=23, fieldAudit=true, approvalSignatureId=694
- sort 10 B210 焊接远端锥度 -> BRE202606110125158100265, fields=38, fieldAudit=true, approvalSignatureId=698
- sort 16 B230 RX口检测 -> BRE202606110125300070266, fields=34, fieldAudit=true, approvalSignatureId=702
- sort 17 B240 点胶海波管 -> BRE202606110125439520267, fields=2, fieldAudit=true, approvalSignatureId=706
- sort 18 B250 球囊压握 -> BRE202606110125584400268, fields=92, fieldAudit=true, approvalSignatureId=710
- sort 19 B280 裁剪圆角 -> BRE202606110126161190269, fields=26, fieldAudit=true, approvalSignatureId=714
- sort 21 B320 球囊测漏及全检 -> BRE202606110126278740270, fields=26, fieldAudit=true, approvalSignatureId=718
