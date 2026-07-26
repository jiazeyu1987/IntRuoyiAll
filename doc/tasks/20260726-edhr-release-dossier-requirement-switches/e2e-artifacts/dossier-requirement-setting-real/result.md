# eDHR 放行资料限制开关真实 E2E

- Status: PASS
- Base URL: http://127.0.0.1:8081
- Backend URL: http://127.0.0.1:48081
- Tenant/User: 芋道源码/admin
- Original: {"incomingInspectionReportRequired":false,"sterilizationReportRequired":false,"finishedProductInspectionReportRequired":false,"finishedProductInspectionRecordRequired":false}
- Changed: {"incomingInspectionReportRequired":true,"sterilizationReportRequired":false,"finishedProductInspectionReportRequired":false,"finishedProductInspectionRecordRequired":false}
- Restore: {"method":"UI","steps":[{"field":"incomingInspectionReportRequired","label":"来料检报告","changed":true,"value":false,"requestPayload":{"incomingInspectionReportRequired":false,"sterilizationReportRequired":false,"finishedProductInspectionReportRequired":false,"finishedProductInspectionRecordRequired":false},"responseHash":"e43c17ab3f5c6bafccfd28cb634968663d14b047ea19bf9c50316271d4b3f268"}],"restored":{"incomingInspectionReportRequired":false,"sterilizationReportRequired":false,"finishedProductInspectionReportRequired":false,"finishedProductInspectionRecordRequired":false},"restoredHash":"e43c17ab3f5c6bafccfd28cb634968663d14b047ea19bf9c50316271d4b3f268"}
- GREEN: real-profile-config-dossier-switch -> PASS，真实页面配置页签展示 4 个资料限制开关，UI 确认切换成功，API 复核变更成功，最后通过 UI 恢复原始状态并复验。
