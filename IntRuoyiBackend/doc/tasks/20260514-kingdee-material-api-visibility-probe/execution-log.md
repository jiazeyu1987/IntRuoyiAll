# Execution Log: Probe Kingdee material API visibility

BDD: Targeted Kingdee material probe explains PTCA mismatch -> Given PTCA material codes are visible in the Kingdee UI, When the local integration account queries `BD_MATERIAL` directly for those codes, Then we can determine whether the current API account/query can see them and why local sync differs from the UI.

RED: prior thread state -> FAIL, there was no direct `BD_MATERIAL` probe result yet, so the PTCA mismatch explanation was only an inference from local ERP/MES rows.

GREEN: targeted real `BD_MATERIAL` exact-code probe with the configured local Kingdee API account -> PASS, `A006.049.1002`, `A006.049.1003`, `AW.129.01`, `AW.129.02`, `YXN.037.001.1004`, and `YXN.037.011.1004` were all returned by the Kingdee API, often under multiple `FUseOrgId.FName` rows.

GREEN: real paged probe using the current sync query shape -> PASS, scanning the first `5000` rows produced by `FilterString = (FNumber <> '') and (FDocumentStatus = 'C')` and `OrderString = FNumber ASC` returned `MATCHES=0` for the 15 PTCA codes that existed in local ERP but were absent from MES.

CONCLUSION: the mismatch is not caused by querying the wrong Kingdee table and not caused by the configured API account lacking access. The current sync misses those PTCA codes because it only consumes the first `5000` sorted `BD_MATERIAL` rows, and multi-organization duplicate rows exhaust that range before the target PTCA rows are reached.
