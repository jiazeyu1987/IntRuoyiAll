# G70 native distribution permission delivery

Status: in_progress. Real B/1 controlled-pending sample4045 and valid doc_control actor1/category DISTRIBUTE exist, but exact system permission menu has zero rows. This is missing seed/configuration delivery, not a reason to bypass Controller/FE gates.

Scope: one global permission button 文控下发 (type3, parent6800) and at most one role_menu row for actual tenant1/role910233 enabled unique doc_control. Parent6800 already enabled and granted; retired6808 stays retired and6819 is not granted. No old menu/role/binding/package/user/category/history changes. Root owns actual MySQL/backup/runtime/UI/Git; child prepares one migration and pure offline contracts only.

BDD: Given exact enabled parent6800 and tenant1 role910233 with its original ancestor grant, When no native distribute permission exists, Then one exact button6839 and one scoped binding are inserted atomically. Given a formally UI-created positive-ID button with the exact same permission/payload, Then its ID/old row is preserved and only a missing target binding is inserted. Given an exact completed state, Then replay performs zero row writes.

BDD: Given duplicate/case-fold permission, occupied reserved ID, wrong or disabled payload, foreign/inactive/duplicate doc_control or preexisting wrong binding, When the migration is attempted, Then it signals failure without overwriting or adding partial grants. Missing dependencies abort rather than invent role/menu/package facts. SQL first/repeat/rollback on actual MySQL remains Root-only and is not claimed by offline tests.

Formal payload: name文控下发 / permissiondcc:controlled-file:distribute / type3 / sort99 / parent6800 / path'' / icon'' / component'' / component_name'' / status0 / visible1 / keep_alive0 / always_show0 / deleted0. Creator/updater/time are genuine original audit metadata when reusing a UI-created row, never rewritten.


G70 r2 exact real UI payload: menu605071339 has component/component_name/path/icon all SQL empty-string (ISNULL0/HEXempty), not NULL. The earlier NULL choice was a source-design inference, not business demand; source now requires exact empty strings on both comparison and INSERT, never COALESCE/fallback or oldrow rewrite. Root will execute only r2 after review; old manifest preserved.
