# DeduplicationService 单元测试设计规格

## 概述

为 `com.alerthub.service.DeduplicationService` 编写单元测试。该服务负责基于 Caffeine 缓存的告警指纹去重，是告警链路中防止重复告警的关键组件。目前项目中已有 `AlertServiceTest` 和 `AlertWebhookControllerTest`，但 `DeduplicationService` 尚无独立测试覆盖。

## 目标

- 验证 `DeduplicationService` 所有公开方法的行为正确性
- 覆盖正常路径和边界条件
- 与项目现有测试风格保持一致（JUnit 5 + Mockito）

## 被测类分析

```java
// 核心方法
boolean isDuplicate(String fingerprint)   // 检查指纹是否重复，首次返回 false 并标记，再次返回 true
void   clearFingerprint(String fingerprint) // 清除单个指纹缓存
void   clearAll()                           // 清除所有缓存
```

**关键依赖：**
- `CacheManager`（Spring Cache 抽象，底层为 Caffeine）
- `@Value("${alerthub.deduplication.enabled:true}")` 配置开关

## 测试场景设计

### 1. `isDuplicate` 方法

| # | 场景 | 输入 | 预期返回 | 说明 |
|---|------|------|----------|------|
| 1.1 | 首次检查（未命中） | 新指纹 | `false` | 指纹首次出现，不重复 |
| 1.2 | 重复检查（命中） | 已存在指纹 | `true` | 第二次调用同一指纹，返回重复 |
| 1.3 | 功能关闭 | 任意指纹 | `false` | `enabled=false` 时直接返回 false |
| 1.4 | 缓存未初始化 | 任意指纹 | `false` | `CacheManager` 返回 null 缓存时安全降级 |

### 2. `clearFingerprint` 方法

| # | 场景 | 输入 | 预期行为 |
|---|------|------|----------|
| 2.1 | 清除已存在的指纹 | 已缓存指纹 | 缓存被清除，后续 `isDuplicate` 返回 `false` |
| 2.2 | 缓存未初始化 | 任意指纹 | 不抛异常，静默跳过 |

### 3. `clearAll` 方法

| # | 场景 | 输入 | 预期行为 |
|---|------|------|----------|
| 3.1 | 清除所有缓存 | - | 缓存被清空 |
| 3.2 | 缓存未初始化 | - | 不抛异常，静默跳过 |

## 测试实现方案

### 测试类结构

```
com.alerthub.service.DeduplicationServiceTest
├── @ExtendWith(MockitoExtension.class)
├── @Mock CacheManager
├── @Mock Cache deduplicationCache
├── @InjectMocks DeduplicationService（注意：需手动构造，因为有构造函数逻辑）
└── 测试方法（如上表所述）
```

### 技术要点

1. **构造函数注入**：`DeduplicationService` 在构造函数中调用 `cacheManager.getCache("deduplication")`，需要 mock `CacheManager` 返回 mock `Cache`
2. **`@Value` 字段**：`enabled` 字段通过反射或 `@TestPropertySource` 注入测试值
3. **缓存行为模拟**：使用 `when(cache.get(...)).thenReturn(...)` 模拟缓存读写行为

### 示例测试代码骨架

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

    // 场景 1.1: 首次检查返回 false
    @Test
    void isDuplicate_firstTime_returnsFalse() {
        when(deduplicationCache.get("fp1", Boolean.class)).thenReturn(null);

        assertFalse(service.isDuplicate("fp1"));
        verify(deduplicationCache).put("fp1", Boolean.TRUE);
    }

    // 场景 1.2: 重复检查返回 true
    @Test
    void isDuplicate_alreadyExists_returnsTrue() {
        when(deduplicationCache.get("fp1", Boolean.class)).thenReturn(Boolean.TRUE);

        assertTrue(service.isDuplicate("fp1"));
    }

    // 场景 1.3: 功能关闭
    @Test
    void isDuplicate_disabled_returnsFalse() {
        ReflectionTestUtils.setField(service, "enabled", false);

        assertFalse(service.isDuplicate("fp1"));
        verifyNoInteractions(deduplicationCache);
    }

    // 场景 1.4: 缓存未初始化
    @Test
    void isDuplicate_cacheNull_returnsFalse() {
        DeduplicationService svc = new DeduplicationService(cacheManager);
        // cacheManager.getCache 返回 null 时
        when(cacheManager.getCache("deduplication")).thenReturn(null);
        DeduplicationService svcWithNullCache = new DeduplicationService(cacheManager);

        assertFalse(svcWithNullCache.isDuplicate("fp1"));
    }
}
```

## 约束

- 使用 JUnit 5 + Mockito，与项目现有测试风格一致
- 不启动 Spring 上下文（纯单元测试）
- 测试文件路径：`src/test/java/com/alerthub/service/DeduplicationServiceTest.java`

## 验收标准

- [ ] 所有 8 个测试场景通过
- [ ] 覆盖 `isDuplicate`、`clearFingerprint`、`clearAll` 三个公开方法
- [ ] 覆盖 `enabled` 配置开关和缓存 null 的降级逻辑
- [ ] 不依赖 Spring 上下文，纯 Mockito 单元测试
- [ ] `mvn test -pl . -Dtest=DeduplicationServiceTest` 通过
