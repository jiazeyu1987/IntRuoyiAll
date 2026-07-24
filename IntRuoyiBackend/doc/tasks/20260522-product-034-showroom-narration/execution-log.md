BDD: showroom product narration generation -> Given user-provided product facts and output constraints / When generating a showroom Chinese narration / Then the response must stay within provided facts, remain natural for voiceover, and return Chinese body text only

RED: N/A -> FAIL, strict TDD command not applicable because this task produces standalone narration text rather than executable production code

GREEN: manual constraint review -> PASS

Verification evidence:
- Source facts limited to the user-provided profile for product_034.
- Included only product name, product type, lifecycle, intended use, structural selling points, and registration details that were explicitly provided.
- Avoided adding unsupported parameters, clinical conclusions, performance claims beyond the provided wording, or extra registration claims.
- Final response prepared as Chinese body text only, without title or bullets.
