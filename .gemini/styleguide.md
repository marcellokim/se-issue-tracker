# Gemini Code Assist Review Guide

## Language and tone
- Write review comments in Korean.
- Keep comments concise, specific, and actionable.
- Avoid duplicate low-value comments when another review bot has already covered the same issue.

## Repository workflow rules
- Normal pull requests target `dev`; `main` is the stable branch.
- Dependabot updates should be opened against `dev` through `.github/dependabot.yml`.
- Do not recommend force-pushes, branch-protection bypasses, direct pushes to `main`/`dev`, or final coursework submission automation.
- Changes under `.github/workflows/`, branch-protection scripts, hooks, and GitHub automation guardrails require maintainer-level review.

## Project-specific review focus
- For Java code, prioritize correctness, Java 21 compatibility, MVC separation, UI/domain separation, persistence behavior, and JUnit coverage.
- Treat JavaFX and Swing as the intended two UI toolkit assumption unless the team changes that decision.
- For issue workflow logic, check the confirmed state flow: `NEW -> ASSIGNED -> FIXED -> RESOLVED -> CLOSED`, Tester rejection `FIXED -> ASSIGNED`, PL reopen `CLOSED/RESOLVED -> REOPENED`, and PL delete through the separate deleted-issue management flow.
- For design documents, distinguish confirmed design context from implementation that is actually present in the repository.
- Do not generate final coursework prose on behalf of the team; flag risks and suggest review directions.

## Noise control
- Prefer comments for correctness, security, failing tests, broken automation, or clear maintainability risks.
- Avoid style-only comments unless they block readability or violate an explicit repository convention.
