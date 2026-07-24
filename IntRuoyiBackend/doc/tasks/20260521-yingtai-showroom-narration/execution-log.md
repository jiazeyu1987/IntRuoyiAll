BDD: showroom narration generation -> Given user-provided company facts and output constraints / When generating a showroom Chinese narration / Then the response must stay within provided facts, remain natural for voiceover, and return body text only

RED: N/A -> FAIL, strict TDD command not applicable because this task produces standalone narration text rather than executable production code

GREEN: manual constraint review -> PASS

Verification evidence:
- Source facts limited to the user-provided company profile for 瑛泰.
- Omitted unsupported listing details and avoided unprovided market, revenue, customer, and future-plan claims.
- Final response prepared as Chinese body text only, without title or bullets.
- Target length controlled to approximately 580 Chinese characters.
