# Enterprise Content Mesh Framework (POC)

This repository serves as an architectural proof-of-concept demonstrating a high-throughput, non-blocking content federation layer. Built to solve vendor lock-in and migration fragmentation in enterprise environments, the framework acts as a unified abstraction layer over multiple content schemas—aggregating and customizing content dynamically at the orchestration boundary.

### Core Architecture Highlights
* **Reactive Core:** Engineered natively on **Project Reactor** and Spring WebFlux to guarantee non-blocking I/O execution loops under high concurrent stress.
* **Multi-Protocol Federation:** Abstracted driver pipeline resolving content seamlessly across modern Headless CMS (AEM GraphQL) and legacy enterprise systems (JCR/Jackrabbit, CMIS) concurrently.
* **Stateless Pipeline Customization:** Sequential, thread-safe in-memory filtering engine for on-the-fly localization, currency serialization, and dynamic targeting before layout delivery.
* **Enterprise Spring Batch Support:** Scalable Master-Slave partitioned batch jobs (`cachePreWarmJob`) for proactive distributed cache pre-warming, featuring asynchronous chunk processing and SPI-driven source providers.

---

## 1. Architecture Overview

CMS Mesh is a high-performance content delivery framework designed to unify, target, and transform unstructured content across multiple Content Management Systems (CMS) including JCR/Jackrabbit repositories and CMIS-compliant systems. It integrates dynamic customization filters, an in-memory bi-directional cache, and distributed batch processing capabilities.

### Component & Class Structure

The following diagram maps out the core classes, interfaces, and registry structures of the CMS Mesh platform.

```mermaid
graph TD
    %% Styling definitions
    classDef api fill:#e1f5fe,stroke:#0288d1,stroke-width:2px;
    classDef engine fill:#e8f5e9,stroke:#388e3c,stroke-width:2px;
    classDef cache fill:#fff9c4,stroke:#fbc02d,stroke-width:2px;
    classDef spi fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px;
    classDef provider fill:#ffe0b2,stroke:#f57c00,stroke-width:2px;
    classDef batch fill:#e0f7fa,stroke:#00acc1,stroke-width:2px;

    subgraph API_Layer [API & Ingress Layer]
        CmsController["CmsController<br/>(REST & SSE Endpoints: /page/structure, /page, /page/stream, /cache/prewarm)"]
    end
    class CmsController api;

    subgraph Core_Engine [Core Orchestration Engine]
        LayoutAssembler["LayoutAssembler<br/>(Coordinates resolution, pipeline, and cache)"]
    end
    class LayoutAssembler engine;

    subgraph Cache_Layer [Bi-Directional Index Caching]
        LayoutCache["LayoutCache<br/>(Coordinates cache entries with invalidation tags)"]
        BiDirectionalCache["BiDirectionalIndexCache<br/>(Manages keys, tags, & Caffeine Core store)"]
        
        LayoutCache --> BiDirectionalCache
    end
    class LayoutCache,BiDirectionalCache cache;

    subgraph Batch_Infrastructure [Spring Batch Processing Layer]
        JobLauncher["JobLauncher<br/>(Triggers cache pre-warm jobs)"]
        Partitioner["CmsPagePartitioner<br/>(Master-slave sharding via SPI)"]
        SourceProvider["CmsBatchSourceProvider<br/>(SPI for dynamic target paths)"]
        AuditListener["CmsBatchAuditListener<br/>(Chunk duration & error auditing)"]

        JobLauncher --> Partitioner
        Partitioner --> SourceProvider
    end
    class JobLauncher,Partitioner,SourceProvider,AuditListener batch;

    subgraph Targeting_Pipeline [Customization Filters - SPI]
        CustomizationFilter["CustomizationFilter<br/>(Interface)"]
        SegmentFilter["SegmentCustomizationFilter<br/>(Applies segment adjustments)"]
        ChannelFilter["ChannelCustomizationFilter<br/>(Applies channel specific states)"]

        CustomizationFilter --> SegmentFilter
        CustomizationFilter --> ChannelFilter
    end
    class CustomizationFilter,SegmentFilter,ChannelFilter spi;

    subgraph Content_Providers [Provider Drivers - SPI]
        ProviderDriver["ProviderDriver<br/>(Interface)"]
        JcrProvider["JcrProviderDriver<br/>(Resolves nodes via JCR)"]
        CmisProvider["CmisProviderDriver<br/>(Resolves nodes via OpenCMIS)"]
        AemProvider["AemProviderDriver<br/>(Resolves headless components via AEM GraphQL)"]

        ProviderDriver --> JcrProvider
        ProviderDriver --> CmisProvider
        ProviderDriver --> AemProvider
    end
    class ProviderDriver,JcrProvider,CmisProvider,AemProvider provider;

    %% Dependencies
    CmsController -->|Unifies incoming requests| LayoutAssembler
    CmsController -->|Triggers batch prewarm job| JobLauncher
    LayoutAssembler -->|Read / Write records| LayoutCache
    LayoutAssembler -->|Sequentially runs components through| CustomizationFilter
    LayoutAssembler -->|Delegates layout & component resolution| ProviderDriver
```

### Lifecycle & Data Flow Sequence

This diagram details the lifecycle sequence of a page layout resolution when requested through the `LayoutAssembler`.

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client Thread / Controller
    participant LA as LayoutAssembler
    participant LC as LayoutCache (Bi-Directional)
    participant CAF as Caffeine Core Store
    participant PR as Content Providers

    Client->>LA: assemblePageLayout(path, ctx, providerId)
    LA->>LC: get(cacheKey)
    LC->>CAF: getIfPresent(cacheKey)
    alt Cache Hit
        CAF-->>LC: Cached Map Structure
        LC-->>LA: Cached Map Structure
        LA-->>Client: Return Page Layout Map
    else Cache Miss
        CAF-->>LC: null
        LC-->>LA: null
        LA->>PR: resolvePageLayout(path)
        PR-->>LA: PageLayout Model
        LA->>PR: resolveComponent(id)
        PR-->>LA: ContentComponent Model
        Note right of LA: Apply Customization Filter Transformations
        LA->>LC: put(cacheKey, layout, result)
        LC->>CAF: put(cacheKey, result)
        Note right of LC: Link cacheKey with slots tags in registries
        LA-->>Client: Return Assembled Map Structure
    end

    Note over CAF: Eviction Event Triggered (TTL / Size Limit)
    CAF->>LC: removalListener(cacheKey, value)
    LC->>LC: removeKeyFromRegistry(cacheKey)
    Note right of LC: Atomic index references cleaned up
```

### Cache State Invalidation Lifecycle

The `BiDirectionalIndexCache` maintains tag-to-key and key-to-tag relationships. This diagram demonstrates how the cache shifts states programmatically or asynchronously.

```stateDiagram-v2
    [*] --> UncachedState : Cold Ingress Inbound Requests
    UncachedState --> MemoryAllocatedState : put() Invocation Triggered

    state MemoryAllocatedState {
        [*] --> MapEntryRegistered : Generate Content Key Matrix
        MapEntryRegistered --> TagIndexAttached : Link Path Elements to tagToKeyRegistry
        TagIndexAttached --> InverseIndexLocked : Link Identity to keyToTagRegistry
    }

    MemoryAllocatedState --> SynchronousWipeState : Administrative Manual Invalidation Called
    SynchronousWipeState --> EvictionDispatchedState : Purge Tag Indices & Invoke cache.invalidate()

    MemoryAllocatedState --> AsynchronousEvictionState : Natural TTL Lifespan Limit Expired

    AsynchronousEvictionState --> EvictionDispatchedState : Caffeine Background Thread Invokes Listener
    EvictionDispatchedState --> CleanedState : Clear Map References Atomically via compute()

    CleanedState --> [*] : Memory Reclaimed Completely
```

---

## 2. Dynamic Configurations & Properties

### JCR, CMIS, and Spring Batch Properties Configuration

File configurations are driven by standard Spring Boot configuration loading.

#### `application.yml` Schema Example
```yaml
cms:
  mesh:
    default-provider: AEM_GRAPHQL # Default provider protocol used when none specified in request
    cache:
      enabled: true
      max-size: 50000
      expire-after-write-minutes: 15
    aem:
      graphql-endpoint: "https://publish-pXX-eYY.adobeaemcloud.com/graphql/execute.json/common/componentByPath"
      access-token: "YOUR_BEARER_TOKEN"
    jcr:
      user: admin
      password: admin
      transient-repo-dir-prefix: jacket-repo-
      repository-xml-name: /repository.xml
      sling-namespace-prefix: sling
      sling-namespace-uri: http://sling.apache.org/jcr/sling/1.0
      sling-template-property: sling:resourceType
      cq-template-property: cq:template
    batch:
      enabled: true
      chunk-size: 10
      grid-size: 4
      thread-pool-size: 8
      skip-limit: 3
      retry-interval-ms: 1000
      default-protocol: LOCAL_JCR
      target-paths:
        - "/content/home"
        - "/content/products"
        - "/content/about"
        - "/content/pricing"
```

`MeshProperties.java`, `JcrProperties.java`, `CmisProperties.java`, and `CmsBatchProperties.java` are loaded dynamically at runtime via Spring `@ConfigurationProperties`.

---

## 3. Usage & Step-by-Step Guide

To instantiate and orchestrate the CMS Mesh pipeline:

### Step 1: Bootstrap properties
```java
// Configure the Cache and Default Provider Protocols
MeshProperties.CacheConfig cacheConfig = new MeshProperties.CacheConfig(true, 50000, 15);
MeshProperties properties = new MeshProperties(ProviderDriverProtocol.LOCAL_JCR, cacheConfig);
```

### Step 2: Assemble Components with Customization Pipelines
```java
// Create provider lists & customization filters
List<ProviderDriver> providerDrivers = List.of(jcrProvider, cmisProvider);
List<CustomizationFilter> customizationFilters = List.of(segmentCustomizationFilter, channelCustomizationFilter);

ProviderDriversRegistry registry = new ProviderDriversRegistry(properties.defaultProviderDriver(), providerDrivers);
CustomizationFiltersRegistry filtersRegistry = new CustomizationFiltersRegistry(customizationFilters);

// Instantiate the Pipeline Assembler
LayoutAssembler assembler = new LayoutAssembler(registry, filtersRegistry, cache);
```

### Step 3: Issue Page Assembly Requests
Using `Project Reactor`'s asynchronous publishers:
```java
String pagePath = "/content/home";
UserContext userCtx = new UserContext(Map.of(ContextKey.TIER, "VIP"));

assembler.assemblePageLayout(pagePath, userCtx, ProviderDriverProtocol.LOCAL_JCR)
    .subscribe(resultMap -> {
        // resultMap holds the PageLayout object containing resolved slot groups
        resultMap.slots().forEach((slot, group) -> {
            System.out.println("Slot: " + slot + " contains components: " + group.components().size());
        });
    });
```

---

## 4. Extending the Framework

CMS Mesh is modular. Extending the capability of the framework requires implementing one of two SPI interfaces:

### A. Implementing a Custom Provider Driver
To add support for a new CMS backend (e.g., AEM Content Services, Strapi CMS, or local files):
1. **Implement `ProviderDriver`** from package `net.bopsys.cms.mesh.spi`.
2. **Define a custom enum value** in `ProviderDriverProtocol.java` (e.g., `STRAPI_REST`).

```java
package net.bopsys.cms.mesh.provider;

import net.bopsys.cms.mesh.model.ContentComponent;
import net.bopsys.cms.mesh.model.ProviderDriverProtocol;
import net.bopsys.cms.mesh.model.PageLayoutDefinition;
import net.bopsys.cms.mesh.model.SlotGroupDefinition;
import net.bopsys.cms.mesh.spi.ProviderDriver;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class StrapiProviderDriver implements ProviderDriver {

    EOverride
    public ProviderDriverProtocol getSupportedProtocol() {
        return ProviderDriverProtocol.STRAPI_REST; // Map to enum specification
    }

    @Override
    public Mono<PageLayoutDefinition> resolvePageLayout(String pagePath) {
        // Build or query page metadata containing slotted arrays of component references
        return Mono.just(PageLayoutDefinition.builder()
            .pagePath(pagePath)
            .templateName("strapi-grid")
            .slots(java.util.Map.of("content", new SlotGroupDefinition("content", java.util.List.of("/comp/101"))))
            .build());
    }

    @Override
    public Mono<ContentComponent> resolveComponent(String componentPath) {
        // Retrieve and parse unstructured component from the strapi repository
        return Mono.just(ContentComponent.builder()
            .componentId(componentPath)
            .type("strapi/banner")
            .state(java.util.Map.of("headline", "Dynamic Banner Title"))
            .build());
    }
}
```

### B. Implementing a Custom Customization Filter
Customization filters intercept the components during execution before they are loaded into the slot layout mappings.
1. **Implement `CustomizationFilter`** from package `net.bopsys.cms.mesh.spi`.
2. Add the Spring Bean to the initialized lists injected into the `LayoutAssembler`.

```java
package net.bopsys.cms.mesh.pipeline;

import net.bopsys.cms.mesh.model.ContentComponent;
import net.bopsys.cms.mesh.model.UserContext;
import net.bopsys.cms.mesh.spi.CustomizationFilter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class GeolocationCustomizationFilter implements CustomizationFilter {

    EOverride
    public Mono<ContentComponent> applyCustomization(ContentComponent incoming, UserContext userContext) {
        // Inspect Context parameters (e.g. COUNTRY) and conditionally modify components map inside state
        return Mono.just(incoming); 
    }
}
```

---

## 5. Enterprise Spring Batch Cache Pre-Warming

To eliminate cold-cache penalties and ensure high-throughput resilience, the framework includes an enterprise-grade **Spring Batch 5** distributed pre-warming subsystem.

### Architecture & Capabilities
* **Master-Slave Partitioning (`CmsPagePartitioner`):** Dynamically shards target content paths into discrete execution contexts at runtime.
* **Asynchronous Chunk Processing (`AsyncItemProcessor` / `AsyncItemWriter`):** Offloads heavy layout assembly and cache hydration to background worker thread pools.
* **SPI-Driven Source Provisioning (`CmsBatchSourceProvider`):** Externalizes path resolution and protocols, allowing custom repository traversals.
* **Fault Tolerance & Auditing (`CmsBatchAuditListener`, `CmsBatchProperties`):** Implements step-level skip limits, retries, and chunk duration metrics.
* **REST & Newman Integration:** Triggerable on-demand via `POST /api/v1/cache/prewarm`.

### Spring Batch Architecture Flow

```mermaid
sequenceDiagram
    autonumber
    actor Client as Admin / REST Client / Scheduler
    participant Controller as CmsController
    participant JobLauncher as Spring Batch JobLauncher
    participant Partitioner as CmsPagePartitioner
    participant WorkerStep as Worker Step (Partition-N)
    participant AsyncProc as AsyncItemProcessor
    participant Assembler as LayoutAssembler
    participant Writer as AsyncItemWriter
    participant Cache as LayoutCache

    Client->>Controller: POST /api/v1/cache/prewarm
    Controller->>JobLauncher: run(cachePreWarmJob, jobParameters)
    JobLauncher->>Partitioner: partition(gridSize)
    Partitioner->>Partitioner: Query CmsBatchSourceProvider
    Partitioner-->>JobLauncher: Map<String, ExecutionContext> (Path Slices)
    
    loop Parallel Worker Threads
        JobLauncher->>WorkerStep: Execute Partition Steps Concurrently
        WorkerStep->>AsyncProc: Read & Offload Chunk Items
        AsyncProc->>Assembler: assemblePageLayout(path, ctx, protocol)
        Assembler-->>AsyncProc: Resolved PageLayout Model
        AsyncProc-->>WorkerStep: Future<PageLayout>
        WorkerStep->>Writer: Write Batch Chunk
        Writer->>Cache: put(cacheKey, layout)
    end

    JobLauncher-->>Controller: JobExecution Summary
    Controller-->>Client: JSON Response (jobId, status, exitCode)
```

---

## 6. Testing and Validation

### Unit & Integration Testing via Gradle
To execute all local unit, parameterized, reactive pipeline, partitioner unit tests (`CmsPagePartitionerTest`), and batch integration tests (`CachePreWarmBatchIntegrationTest`), use:
```bash
./gradlew test
```

### Functional/Integration Routing Tests via Postman/Newman
This framework includes Newman integration test suites covering API endpoints, SSE streaming behaviors, and the batch pre-warm triggers.

#### Running Newman Tests
1. Ensure target server is running locally on port :3000.
    ```bash
    ./gradlew bootRun
    ```

2. Install the Newman tool globally:
    ```bash
    npm install -g newman
    ```
3. Platform test suites can be executed via Newmans CLI:
    ```bash
    newman run postman/cms-mesh-api.json
    ```
