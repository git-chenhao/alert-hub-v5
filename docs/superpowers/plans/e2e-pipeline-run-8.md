# Implementation Plan: E2E Pipeline Run-8

**Spec**: `docs/superpowers/specs/e2e-pipeline-run-8.md`
**任务类型**: coding（README HTML 注释追加）
**风险等级**: 极低

## 实现步骤

### Step 1: 追加 HTML 注释到 README.md

- **文件**: `README.md`
- **操作**: 在文件末尾追加一个空行和 HTML 注释行
- **预期变更前**:
  ```
  # Alert Hub V5
  ```
- **预期变更后**:
  ```
  # Alert Hub V5

  <!-- swarm-control real e2e 2026-04-12 run-8 -->
  ```
- **工具**: 使用 Edit 工具，将 `# Alert Hub V5` 替换为包含注释的完整内容

### Step 2: 验证变更正确性

- 验证 README.md 最后一行为目标 HTML 注释：`tail -1 README.md`
- 验证原有内容未被修改：`head -1 README.md` 应输出 `# Alert Hub V5`
- 验证无其他业务文件被修改：`git diff --name-only` 应仅列出 `README.md`

### Step 3: 提交改动

- 暂存：`git add README.md`
- 提交，commit message: `docs: 在 README.md 末尾追加 E2E pipeline run-8 注释`
- 验证提交成功：`git log -1 --oneline`

## 执行顺序

```
Step 1 (追加注释) → Step 2 (验证) → Step 3 (提交)
```

严格顺序执行，不可并行。

## 关键风险与缓解

| 风险 | 可能性 | 影响 | 缓解措施 |
|------|--------|------|----------|
| 意外修改 README.md 已有内容 | 极低 | 中 | 使用精确的 old_string 匹配，Step 2 验证原有内容 |
| 注释格式不正确 | 极低 | 低 | 严格使用 spec 定义的字符串，逐字符核对 |
| 其他文件被意外修改 | 极低 | 中 | Step 2 检查 `git diff --name-only` |

## 验证方式

1. **内容验证**: README.md 最后一行精确匹配 `<!-- swarm-control real e2e 2026-04-12 run-8 -->`
2. **完整性验证**: README.md 第一行仍为 `# Alert Hub V5`
3. **范围验证**: `git diff --name-only` 仅包含 `README.md`
4. **提交验证**: `git log -1` 确认提交存在且 message 正确
