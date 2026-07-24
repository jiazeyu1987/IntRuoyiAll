BDD: image import produces corrected draft templates -> Given an operator uploads a supported production-record image from the local MES page, When the backend runs OCR or layout analysis and then invokes Codex CLI for structure correction, Then the system should return corrected template candidates with `sheet_layout_json`, `meta_json`, confidence, and issue summaries.

BDD: low-confidence or invalid correction fails fast -> Given the OCR draft is incomplete, or Codex CLI returns invalid JSON, blank output, or confidence below the configured threshold, When correction finishes, Then the import session should be marked failed and the system should not allow template commit or report generation.

BDD: corrected candidates can be committed as reusable templates -> Given an image import session has corrected candidates, When the operator commits selected candidates, Then the system should persist them into the existing batch-record template model and preserve both corrected layout JSON and correction metadata for traceability.

BDD: phase 1 entry remains on a locally owned page -> Given the current Jimu report list is rendered inside an iframe, When the image-import feature is introduced, Then the first release should expose the action from the local MES batch-record page instead of patching Jimu's internal toolbar.

Design-only note: this task stops at system design. No RED or GREEN test evidence was created because no production code changes or executable behavior changes were requested in this task.
