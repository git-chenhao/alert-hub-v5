# DeduplicationService 单元测试实现计划

## 元信息

- **关联设计规格**: `docs/superpowers/specs/deduplication-service-test.md`
- **被测类**: `src/main/java/com/alerthub/service/DeduplicationService.java` (68 行)
- **测试文件**: `src/test/java/com/alerthub/service/DeduplicationServiceTest.java`
- **测试框架**: JUnit 5 + Mockito (与现有 `AlertServiceTest` 风格一致)

## 源码分析

### 被测类结构 (DeduplicationService)

```
DeduplicationService
├── 构造函数: DeduplicationService(CacheManager cacheManager)
│   └── this.deduplicationCache = cacheManager.getCache("deduplication")
├── @Value("${alerthub.deduplication.enabled:true}") boolean enabled
├── isDuplicate(String fingerprint) → boolean
│   ├── enabled=false → return false (短路)
│   ├── deduplicationCache=null → log.warn + return false (降级)
│   ├── cache.get(fingerprint) != null && true → return true (重复)
│   └── cache.get(fingerprint) == null → cache.put(fp, TRUE) + return false (首次)
├── clearFingerprint(String fingerprint) → void
│   └── deduplicationCache != null → cache.evict(fingerprint)
└── clearAll() → void
    └── deduplicationCache != null → cache.clear()
```

### 关键技术点

1. **构造函数注入** — `cacheManager.getCache("deduplication")` 在构造时立即调用，需要 mock 该返回值
2. **`@Value` 字段注入** — `enabled` 默认 `true`，测试中需通过 `ReflectionTestUtils.setField()` 覆盖为 `false`
3. **null 安全** — `deduplicationCache` 可能为 null（`CacheManager` 返回 null），所有方法都有 null 检查
4. **缓存 mock** — 需模拟 `Cache.get(key, type)` 返回值和 `Cache.put()`、`Cache.evict()`、`Cache.clear()` 行为

## 实现步骤

### Step 1: 创建测试类骨架

创建 `src/test/java/com/alerthub/service/DeduplicationServiceTest.java`。

**类结构：**
```java
@ExtendWith(MockitoExtension.class)
class DeduplicationServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache deduplicationCache;

    private DeduplicationService service;

    @BeforeEach
    void setUp() {
        when(cacheManager.getCache("deduplication")).thenReturn(deduplicationCache);
        service = new DeduplicationService(cacheManager);
    }
}
```

**注意**: 不使用 `@InjectMocks`，因为构造函数中有 `cacheManager.getCache()` 逻辑需要精确控制。手动在 `@BeforeEach` 中构造实例。

### Step 2: 实现 isDuplicate 测试 (4 个场景)

| 方法名 | 场景 | 关键 mock | 断言 |
|--------|------|-----------|------|
| `isDuplicate_firstTime_returnsFalse` | 1.1 首次检查 | `cache.get("fp1", Boolean.class) → null` | `assertFalse` + `verify(cache).put("fp1", TRUE)` |
| `isDuplicate_alreadyExists_returnsTrue` | 1.2 重复检查 | `cache.get("fp1", Boolean.class) → TRUE` | `assertTrue` |
| `isDuplicate_disabled_returnsFalse` | 1.3 功能关闭 | `ReflectionTestUtils.setField(service, "enabled", false)` | `assertFalse` + `verifyNoInteractions(cache)` |
| `isDuplicate_cacheNull_returnsFalse` | 1.4 缓存未初始化 | `cacheManager.getCache("deduplication") → null`，重新构造 service | `assertFalse` |

### Step 3: 实现 clearFingerprint 测试 (2 个场景)

| 方法名 | 场景 | 关键 mock | 断言 |
|--------|------|-----------|------|
| `clearFingerprint_existing_removesFromCache` | 2.1 清除已存在指纹 | 默认 setUp 即可 | `verify(cache).evict("fp1")` |
| `clearFingerprint_cacheNull_noException` | 2.2 缓存未初始化 | `cacheManager.getCache → null`，重新构造 service | 不抛异常 |

### Step 4: 实现 clearAll 测试 (2 个场景)

| 方法名 | 场景 | 关键 mock | 断言 |
|--------|------|-----------|------|
| `clearAll_clearsCache` | 3.1 清除所有缓存 | 默认 setUp 即可 | `verify(cache).clear()` |
| `clearAll_cacheNull_noException` | 3.2 缓存未初始化 | `cacheManager.getCache → null`，重新构造 service | 不抛异常 |

### Step 5: 验证

```bash
mvn test -pl . -Dtest=DeduplicationServiceTest
```

确认 8 个测试全部通过，无 Spring 上下文启动。

## 代码风格约定

遵循 `AlertServiceTest` 已有模式：
- `// Given / When / Then` 注释结构
- 中文 Javadoc 注释
- `@ExtendWith(MockitoExtension.class)` 注解
- 静态导入 `Assertions.*` 和 `Mockito.*`

## 完整 import 列表

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
```

## 风险与注意事项

1. **`@Value` 默认值** — `enabled` 字段默认为 `true`（因为 `@Value` 在无 Spring 上下文时不注入，boolean 默认值就是 `false`），所以场景 1.3 中必须用 `ReflectionTestUtils.setField` 显式设置。实际上默认 boolean 值是 `false`，所以需要在 `setUp` 中显式设置为 `true` 来模拟生产行为，或者在需要 `enabled=true` 的测试中设置。
   - **修正**: Java boolean 字段默认值为 `false`。无 Spring 上下文时 `@Value` 不生效。因此：
     - 场景 1.1/1.2/1.4: 需在 setUp 中 `ReflectionTestUtils.setField(service, "enabled", true)` 确保走缓存逻辑
     - 场景 1.3: `ReflectionTestUtils.setField(service, "enabled", false)` 测试关闭路径
2. **场景 1.4 构造顺序** — `when(cacheManager.getCache("deduplication")).thenReturn(null)` 必须在 `new DeduplicationService(cacheManager)` 之前设置，因为构造函数立即调用 `getCache`。
3. **`@BeforeEach` 中的 mock 重置** — MockitoExtension 会自动重置 mock，无需手动 `reset()`。

## 预估规模

- 测试文件约 100-120 行
- 8 个测试方法 + 1 个 `setUp` 方法
- 无需修改任何生产代码或配置文件
