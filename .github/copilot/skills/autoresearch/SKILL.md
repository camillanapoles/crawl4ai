---
name: autoresearch
description: 'Autonomous iterative experimentation loop for any programming task. Guides the user through defining goals, measurable metrics, and scope constraints, then runs an autonomous loop of code changes, testing, measuring, and keeping/discarding results. Inspired by Karpathy''s autoresearch. USE FOR: autonomous improvement, iterative optimization, experiment loop, auto research, performance tuning, automated experimentation, hill climbing, try things automatically, optimize code, run experiments, autonomous coding loop. DO NOT USE FOR: one-shot tasks, simple bug fixes, code review, or tasks without a measurable metric.'
license: MIT
compatibility: Requires git. The project must be a git repository. Requires terminal access to run commands.
metadata:
  author: luiscantero
  inspired-by: https://github.com/karpathy/autoresearch
---

# Autoresearch: Autonomous Iterative Experimentation

An autonomous experimentation loop for any programming task. You define the goal and how to measure it; the agent iterates autonomously -- modifying code, running experiments, measuring results, and keeping or discarding changes -- until interrupted.

This skill is inspired by [Karpathy's autoresearch](https://github.com/karpathy/autoresearch), generalized from ML training to **any programming task with a measurable outcome**.

---

## Agent Behavior Rules

1. **DO** guide the user through the Setup phase interactively before starting the loop.
2. **DO** establish a baseline measurement before making any changes.
3. **DO** commit every experiment attempt before running it (so it can be reverted cleanly).
4. **DO** keep a results log (TSV) tracking every experiment.
5. **DO** revert changes that do not improve the metric (git reset to last known good).
6. **DO** run autonomously once the loop starts -- never pause to ask "should I continue?".
7. **DO NOT** modify files the user marked as out-of-scope.
8. **DO NOT** skip the measurement step -- every experiment must be measured.
9. **DO NOT** keep changes that regress the metric unless the user explicitly allowed trade-offs.
10. **DO NOT** install new dependencies or make environment changes unless the user approved it.

---

## Phase 1: Setup (Interactive)

Before any experimentation begins, work with the user to establish these parameters.

### 1.1 Define the Goal
Ask the user: **What are you trying to improve or optimize?**

### 1.2 Define the Metric
Ask for:
- `METRIC_COMMAND`: the command to run
- `METRIC_EXTRACTION`: how to extract the numeric metric from output
- `METRIC_DIRECTION`: `lower_is_better` or `higher_is_better`

### 1.3 Define the Scope
- `IN_SCOPE_FILES`: files/dirs the agent may edit
- `OUT_OF_SCOPE_FILES`: files/dirs that must not be modified

### 1.4 Confirm Setup
Summarize all parameters back to the user in a clear table and ask for confirmation. Do not proceed until confirmed.

---

## Phase 2: Branch & Baseline

1. Create branch: `git checkout -b autoresearch/<tag>`
2. Initialize `results.tsv` with header: `experiment\tcommit\tmetric\tstatus\tdescription`
3. Run the baseline and record as experiment `0`

---

## Phase 3: Experiment Loop

Run continuously until budget reached or user interrupts:

```
LOOP:
  1. THINK   - Analyze previous results. Generate experiment hypothesis.
  2. EDIT    - Modify in-scope files to implement the idea.
  3. COMMIT  - git add + git commit: "experiment: <description>"
  4. RUN     - Execute metric command. Redirect to run.log.
  5. MEASURE - Extract metric from run.log.
  6. DECIDE  - IMPROVED: keep commit. WORSE: git reset --hard HEAD~1.
  7. LOG     - Append row to results.tsv.
  8. CONTINUE
```

---

## Phase 4: Reporting

When loop ends:
1. Print full results.tsv as formatted table
2. Summarize: experiments run, kept/discarded, baseline vs final metric
3. Show git log of kept experiments
4. Recommend next steps

---

## Quick Reference

### Results TSV Format
```
experiment	commit	metric	status	description
0	a1b2c3d	0.997900	baseline	unmodified code
1	b2c3d4e	0.993200	keep	increase learning rate to 0.04
```

### Key Principles
1. **Measure everything**: No experiment without a measurement.
2. **Revert failures**: The branch only advances on improvements.
3. **Stay autonomous**: Never stop to ask. Think harder if stuck.
4. **Keep it simple**: Complexity is a cost. Weigh it against gains.
5. **Log everything**: The TSV is the research journal.
