# Implementation Plan: Swarm-Control E2E Verification (Run-6)

> Spec: `docs/superpowers/specs/swarm-e2e-verification-run6.md`
> Date: 2026-04-12
> Status: ready

## Goals

1. 验证 swarm-control 完整链路可用（design → plan → coding）
2. 在 README.md 末尾追加 e2e 标记注释
3. 不修改任何业务代码

## Constraints

- 单行改动，不修改 README.md 已有内容
- 标记格式必须精确为 `<!-- swarm-control real e2e 2026-04-12 run-6 -->`
- 不创建 PR / 不 push
- 工作区必须保持 clean

## Acceptance Criteria

- [ ] README.md 末尾新增一行 HTML 注释
- [ ] `git diff` 只有一行新增
- [ ] 提交后 `git status` 为 clean
- [ ] 不存在其他文件改动

## Implementation Steps

### Step 1: 追加 e2e 标记到 README.md

**Action**: 在 `README.md` 最后一行（第 210 行 `MIT License` 之后）追加一行 HTML 注释。

**Target file**: `README.md`

**Exact change**: 在文件末尾追加以下内容：

```
<!-- swarm-control real e2e 2026-04-12 run-6 -->
```

**Note**: 必须确保在 `MIT License` 后面换行再追加，保持 README.md 原有内容不变。

### Step 2: 验证改动正确性

**Action**: 执行以下验证命令：

1. `git diff README.md` — 确认只有一行新增
2. `git diff --stat` — 确认只涉及 README.md 一个文件
3. `git status` — 确认只有 README.md 被修改，无其他未跟踪文件

**Expected output**:
- `git diff README.md` 显示在末尾新增 `<!-- swarm-control real e2e 2026-04-12 run-6 -->`
- `git diff --stat` 只显示 `README.md | 1 +`

### Step 3: 提交改动

**Action**: 提交 README.md 的改动。

**Commit message**:
```
chore: add swarm-control e2e verification marker (run-6)
```

### Step 4: 提交后验证

**Action**: 执行 `git status` 确认工作区 clean。

## Risk Assessment

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| 追加注释破坏 README 格式 | 极低 | 低 | 注释不会渲染，不影响显示 |
| 误改其他文件 | 极低 | 低 | 只操作 README.md，验证 diff 确认 |
| 分支冲突 | 极低 | 无 | 单行追加，无冲突可能 |

## Execution Order

```
Step 1 (追加标记) → Step 2 (验证改动) → Step 3 (提交) → Step 4 (提交后验证)
```

所有步骤串行执行，无并行可能。总预计改动量：1 行新增。
