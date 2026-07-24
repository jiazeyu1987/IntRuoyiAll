BDD: showroom product narration generation -> Given user-provided product facts and output constraints / When generating a showroom Chinese narration / Then the response must stay within provided facts, remain natural for voiceover, and return Chinese body text only

RED: N/A -> FAIL, strict TDD command not applicable because this task produces standalone narration text rather than executable production code

GREEN: manual constraint review -> PASS

Verification evidence:
- Source facts limited to the user-provided profile for product_098.
- Included only product name, product type, lifecycle, operating principle, risk reduction wording, and transdermal delivery description that were explicitly provided.
- Omitted registration fields because the provided values were `/` and the instructions require skipping missing fields.
- Avoided adding unsupported parameters, indications, clinical conclusions, or extra regulatory claims.
- Final response prepared as Chinese body text only, without title or bullets.
