SELECT JSON_OBJECT('kind','row_count','table','dcc_legacy_source_name_scope','count',COUNT(*)) FROM dcc_legacy_source_name_scope;
SELECT JSON_OBJECT('kind','row_count','table','dcc_legacy_source_name_evidence','count',COUNT(*)) FROM dcc_legacy_source_name_evidence;
SELECT JSON_OBJECT('kind','row_count','table','dcc_source_name_reservation','count',COUNT(*)) FROM dcc_source_name_reservation;
