---
name: swarm-e2e-verification-run6
description: 最小真实链路验证 — 在 README.md 末尾追加 swarm-control e2e 标记（run-6）
type: spec
created: 2026-04-12
status: ready-for-implementation
---

# Spec: Swarm-Control 端到端链路验证（Run 6）

## Goals

1. 验证 swarm-control 完整链路（ticket 创建 → design_discussion → implementation_plan → coding → PR）可用。
2. 在 `README.md` 末尾追加一行 HTML 注释作为链路验证标记，确认 coder 能正确读写仓库文件并提交。
3. 不修改任何业务代码，将影响范围限制为 README.md 末尾一行。

## Constraints

- **单行改动**：仅在 README.md 最后一行之后追加一行 HTML 注释，不修改已有内容。
- **无业务影响**：HTML 注释在浏览器和 Markdown 渲染中不可见，不影响文档展示。
- **标记格式**：必须精确为 `<!-- swarm-control real e2e 2026-04-12 run-6 -->`，包含日期和 run 编号以便追溯。
- **不创建 PR / 不 push**：coding 阶段只提交到当前分支，push 和 PR 由控制面负责。
- **工作区干净**：完成后 `git status` 必须为 clean。

## Acceptance Criteria

- [ ] README.md 末尾新增一行且仅一行内容：`<!-- swarm-control real e2e 2026-04-12 run-6 -->`
- [ ] README.md 其余内容无任何变化（`git diff` 只有一行新增）。
- [ ] 提交信息清晰说明本次改动目的（e2e 验证标记）。
- [ ] `git status` 在提交后为 clean。
- [ ] 不存在其他文件改动（无额外文件引入、无配置变更）。

## Implementation Context

### 改动描述

| 文件 | 操作 | 具体内容 |
|------|------|----------|
| `README.md` | 追加一行 | 在文件末尾添加 `<!-- swarm-control real e2e 2026-04-12 run-6 -->` |

### 验证方式

```bash
# 确认末尾追加
tail -1 README.md
# 预期输出: <!-- swarm-control real e2e 2026-04-12 run-6 -->

# 确认只有一行 diff
git diff HEAD~1 --stat
# 预期输出: README.md | 1 +

# 确认工作区干净
git status
# 预期输出: nothing to commit, working tree clean
```

### 风险评估

- **风险等级**：极低
- **回滚方式**：`git revert HEAD` 即可完全恢复。
- **前置依赖**：无。

## Notes

本次改动是 swarm-control 平台自身链路验证的一部分，不属于 Alert Hub V5 业务功能。标记中的 `run-6` 用于区分不同轮次的 e2e 测试，便于在仓库历史中追溯平台能力验证记录。
