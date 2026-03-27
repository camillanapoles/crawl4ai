---
name: conventional-commit
description: 'Prompt and workflow for generating conventional commit messages using a structured XML format. Guides users to create standardized, descriptive commit messages in line with the Conventional Commits specification, including instructions, examples, and validation.'
---

### Instructions

This skill provides a prompt template for generating conventional commit messages following the [Conventional Commits specification](https://www.conventionalcommits.org/en/v1.0.0/).

### Workflow

**Follow these steps:**

1. Run `git status` to review changed files.
2. Run `git diff` or `git diff --cached` to inspect changes.
3. Stage your changes with `git add <file>`.
4. Construct your commit message using the structure below.
5. After generating your commit message, run the commit command in your terminal.

### Commit Message Structure

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

Types: `feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert`

### Examples

```
feat(parser): add ability to parse arrays
fix(ui): correct button alignment
docs: update README with usage instructions
refactor: improve performance of data processing
chore: update dependencies
feat!: send email on registration (BREAKING CHANGE: email service required)
```

### Validation

- **type**: Must be one of the allowed types.
- **scope**: Optional, but recommended for clarity.
- **description**: Required. Use the imperative mood (e.g., "add", not "added").
- **body**: Optional. Use for additional context.
- **footer**: Use for breaking changes or issue references.

### Final Step

```bash
git commit -m "type(scope): description"
```

Replace with your constructed message. Include body and footer if needed.
