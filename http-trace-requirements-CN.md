# Arthas 通用跟踪功能需求文档 (V4.1)

## 更新日志

### V4.1 (2025-01-11)
- **新增功能**：探针配置文件支持jar包版本指定
  - 添加了 `jarVersions` 配置项，支持指定目标jar包的版本范围
  - 支持版本排除功能，可排除特定有问题的版本
  - 新增多版本配置支持，同一探针可为不同版本提供不同配置
  - 添加版本检测和兼容性验证功能
  - 新增版本管理相关的config命令
- **需求编号**：新增 FR-8.8.1 和 FR-8.12
- **验收标准**：更新了探针配置系统的验收标准，增加版本相关验收项

### V4.0 (初始版本)
- 定义了完整的Arthas通用跟踪功能需求
- 包含trace-flow、replay、trace-diff三个核心命令
- 设计了配置驱动的探针系统
- 定义了场景管理和配置系统

## 📋 文档说明

本文档专注于需求定义，详细描述了Arthas通用跟踪功能的业务背景、核心价值、应用场景、需求目标和功能需求。文档保留了重要的背景信息、核心价值需求目标和应用场景，但移除了具体的技术实现细节，专注于回答"做什么"而不是"怎么做"。

**文档特点**：
- ✅ 保留完整的业务背景和用户痛点分析
- ✅ 详细定义核心价值和需求目标
- ✅ 提供21个具体的应用场景
- ✅ 明确的功能和非功能需求
- ✅ 可验证的验收标准
- ❌ 不包含具体的技术实现方案
- ❌ 不包含代码示例和架构设计

## 📑 目录

1. [概述](#1-概述)
2. [需求背景与价值](#2-需求背景与价值)
3. [核心设计原则](#3-核心设计原则)
4. [功能需求](#4-功能需求)
5. [探针系统需求](#5-探针系统需求)
6. [配置管理需求](#6-配置管理需求)
7. [场景管理需求](#7-场景管理需求)
8. [验收标准](#8-验收标准)
9. [风险评估](#9-风险评估)
10. [项目里程碑](#10-项目里程碑)

## 1. 概述

### 1.1 项目背景

#### 1.1.1 业务背景
在现代微服务架构和分布式系统中，应用程序的复杂性日益增加。一个简单的HTTP请求可能会触发：
- 多个数据库查询操作（MySQL、PostgreSQL、Oracle等）
- 缓存读写操作（Redis、Memcached等）
- 外部服务调用（HTTP客户端、RPC调用）
- 消息队列操作（Kafka、RabbitMQ等）
- 文件I/O操作和其他系统资源访问

当系统出现性能问题、异常或业务逻辑错误时，开发者和运维人员需要快速理解请求的完整执行流程，定位问题根源。

#### 1.1.2 现有工具的局限性

**Arthas现有命令的不足：**
- **watch命令**：只能监控单个方法，无法展示完整的调用链路
- **trace命令**：主要关注方法调用树，对HTTP请求的业务流程支持不足
- **monitor命令**：提供统计信息，但缺乏实时的调用详情和上下文关联

**第三方工具的问题：**
- **APM工具**：需要代码侵入或复杂配置，且成本较高
- **日志分析**：需要大量的日志配置和后期分析工作
- **调试工具**：通常只能在开发环境使用，生产环境受限

#### 1.1.3 用户痛点

**开发阶段痛点：**
- 难以理解复杂业务流程的完整执行路径
- 无法快速定位性能瓶颈在哪个环节
- 缺乏直观的调用链路可视化

**测试阶段痛点：**
- 难以验证业务流程是否按预期执行
- 无法有效进行性能测试和分析
- 缺乏有效的回归测试对比手段

**生产环境痛点：**
- 线上问题难以快速定位和分析
- 缺乏非侵入式的实时监控手段
- 无法在不影响业务的情况下进行深度分析

本文档定义了 Arthas 通用跟踪功能的完整需求，旨在为 Java 开发者提供一个强大、易用、可扩展的应用执行流程跟踪工具，解决上述痛点问题。

### 1.2 命令概览

本功能包含三个核心命令：

- **`trace-flow`** (别名: **`tf`**) - 通用跟踪命令，用于实时跟踪应用执行流程
- **`replay`** (别名: **`rp`**) - 请求回放命令，用于重现和调试问题
- **`trace-diff`** (别名: **`td`**) - 跟踪结果对比命令，用于分析执行差异

### 1.3 核心价值

- **快速理解执行逻辑**：通过可视化的调用链路，快速掌握请求的数据处理流程
- **降低代码学习成本**：无需深入阅读代码，即可了解系统的运行机制
- **提升问题定位效率**：从传统的"小时级"排查缩短到"分钟级"定位
- **支持非侵入式分析**：无需修改代码或重启应用，即可获得完整的执行轨迹
- **配置驱动扩展**：通过JSON配置文件定义探针，无需编写Java代码即可扩展监控能力
- **智能版本适配**：自动检测jar包版本并匹配相应的探针配置，确保兼容性和准确性

## 2. 需求目标

### 2.1 业务目标

**解决的核心问题：**
开发者在面对不熟悉的代码时，难以快速理解 Java 应用的数据处理逻辑和执行流程，在出现问题时无法快速定位根因，导致排查效率低下。

**目标用户：**
- **Java 开发者**：需要快速理解系统的执行逻辑和数据流向
- **系统维护者**：需要理解现有代码的处理逻辑
- **问题排查者**：需要在不深入代码的情况下快速定位问题
- **代码审查者**：需要快速掌握系统的运行机制

### 2.2 核心价值

#### 2.2.1 业务价值

**提升开发效率**
- **快速理解系统**：新开发者可以在数小时内理解复杂系统的业务流程
- **减少调试时间**：将传统的"小时级"问题排查缩短到"分钟级"定位
- **降低学习成本**：通过可视化的调用链路，直观理解代码执行逻辑

**提高系统质量**
- **及早发现问题**：在开发和测试阶段就能发现潜在的性能和逻辑问题
- **确保数据一致性**：验证数据在各个环节的处理是否正确
- **支持回归测试**：通过对比分析确保代码修改不会引入新问题

**降低运维成本**
- **快速故障定位**：生产环境问题可以快速定位到具体的组件和参数
- **非侵入式监控**：无需修改代码或重启应用，即可获得完整的执行轨迹
- **减少故障时间**：提高问题解决效率，减少系统停机时间

#### 2.2.2 技术价值

**系统可观测性**
- **完整链路追踪**：提供从HTTP请求到数据库操作的完整执行链路
- **实时监控能力**：支持生产环境的实时监控和分析
- **多维度分析**：支持性能、异常、数据流等多个维度的分析

**配置驱动架构**
- **灵活扩展**：通过配置文件即可支持新的监控场景，无需修改代码
- **版本适配**：自动适配不同版本的jar包和框架
- **个性化定制**：支持用户根据具体需求定制监控策略

**指标驱动设计**
- **直观的指标定义**：用户可以直接定义关心的业务指标
- **自动计算能力**：支持复杂的指标计算和聚合分析
- **数据驱动决策**：为性能优化和架构改进提供数据支撑

#### 2.2.3 用户价值

**开发者价值**
- **提升工作效率**：快速理解和调试复杂系统
- **提高代码质量**：通过实际执行路径验证代码逻辑
- **学习最佳实践**：通过内置场景学习常见问题的解决方法

**测试工程师价值**
- **提高测试效率**：快速验证功能是否按预期执行
- **增强测试覆盖**：发现传统测试方法难以覆盖的问题
- **自动化回归测试**：支持自动化的回归测试和对比分析

**运维工程师价值**
- **快速问题定位**：在生产环境快速定位和解决问题
- **预防性监控**：提前发现潜在的性能和稳定性问题
- **数据驱动运维**：基于实际数据进行系统优化和容量规划

### 2.3 应用场景

#### 2.3.1 开发阶段场景

**场景1：快速熟悉代码逻辑**
- **用户**：新加入团队的开发者
- **需求**：快速理解现有系统的业务流程和数据处理逻辑
- **解决方案**：通过跟踪典型的HTTP请求，可视化展示完整的执行路径
- **价值**：将代码学习时间从数天缩短到数小时

**场景2：理解数据流向**
- **用户**：需要修改业务逻辑的开发者
- **需求**：了解一个请求会访问哪些数据库表、调用哪些外部服务、经过哪些组件
- **解决方案**：通过探针配置捕获所有数据访问操作，展示完整的数据流向图
- **价值**：避免修改代码时遗漏关键依赖，减少bug引入

**场景3：参数传递分析**
- **用户**：调试复杂业务逻辑的开发者
- **需求**：了解数据在各个环节之间是如何传递和转换的
- **解决方案**：捕获方法参数和返回值，展示数据的完整变换过程
- **价值**：快速定位数据处理错误，提升调试效率

#### 2.3.2 测试阶段场景

**场景4：执行逻辑验证**
- **用户**：测试工程师和QA
- **需求**：验证某个功能的实际执行路径是否符合预期设计
- **解决方案**：对比预期的执行流程和实际跟踪结果
- **价值**：确保功能按设计实现，提前发现逻辑错误

**场景5：数据一致性验证**
- **用户**：测试工程师
- **需求**：检查数据在处理过程中是否正确，是否存在数据丢失或错误转换
- **解决方案**：跟踪数据的完整处理链路，验证每个环节的数据正确性
- **价值**：确保数据处理的准确性和完整性

**场景6：性能回归检测**
- **用户**：性能测试工程师
- **需求**：通过对比历史跟踪结果，发现性能退化问题
- **解决方案**：保存基线跟踪结果，与新版本进行对比分析
- **价值**：及时发现性能问题，避免性能退化上线

#### 2.3.3 生产环境场景

**场景7：快速问题诊断**
- **用户**：运维工程师和开发者
- **需求**：遇到线上问题时，快速选择合适的排查方案
- **解决方案**：通过内置场景模板，快速启动相应的跟踪配置
- **价值**：将问题定位时间从小时级缩短到分钟级

**场景8：问题快速定位**
- **用户**：运维工程师
- **需求**：系统出现异常时，快速找到是哪个环节出错
- **解决方案**：实时跟踪异常请求，捕获具体的错误参数和异常信息
- **价值**：快速定位问题根因，减少系统故障时间

**场景9：数据修改源头追踪**
- **用户**：数据分析师和开发者
- **需求**：发现某个数据被意外修改，需要找到所有修改操作的源头
- **解决方案**：通过特定值过滤，追踪包含该值的所有操作路径
- **价值**：快速定位数据异常的根本原因

#### 2.3.4 维护阶段场景

**场景10：代码影响分析**
- **用户**：维护开发者
- **需求**：修改代码前，了解可能影响的调用链路和依赖关系
- **解决方案**：跟踪相关功能的完整执行路径，分析影响范围
- **价值**：降低代码修改风险，减少意外影响

**场景11：系统架构理解**
- **用户**：架构师和高级开发者
- **需求**：通过实际请求跟踪，理解系统的分层架构和组件交互
- **解决方案**：可视化展示系统的实际运行架构
- **价值**：为架构优化和重构提供数据支撑

#### 2.3.5 高级应用场景

**场景12：请求回放调试**
- **用户**：开发者和测试工程师
- **需求**：保存问题请求的回放文件，在开发环境中重复执行进行调试
- **解决方案**：记录完整的请求参数和上下文，支持在不同环境中回放
- **价值**：提高问题复现效率，支持离线调试

**场景13：回归测试**
- **用户**：测试工程师
- **需求**：使用回放文件验证代码修改后的行为是否与预期一致
- **解决方案**：对比回放结果与基线结果，自动检测差异
- **价值**：自动化回归测试，提高测试覆盖率

**场景14：跟踪结果对比**
- **用户**：开发者和运维工程师
- **需求**：保存正常和异常情况的跟踪结果，通过对比分析找出差异
- **解决方案**：提供可视化的对比界面，突出显示差异点
- **价值**：快速识别问题原因，提高分析效率

**场景15：特定值追踪**
- **用户**：数据分析师和开发者
- **需求**：通过特定值过滤，查看包含该值的所有操作路径
- **解决方案**：支持复杂的值匹配和过滤条件
- **价值**：精确追踪数据流向，定位数据异常

**场景16：数据访问分析**
- **用户**：DBA和性能优化工程师
- **需求**：追踪特定数据的访问路径，了解数据的读写模式
- **解决方案**：统计数据访问频率和模式，生成访问报告
- **价值**：优化数据库设计和查询性能

**场景17：自定义监控需求**
- **用户**：运维工程师和开发者
- **需求**：通过配置快速添加对特定方法、第三方库的监控
- **解决方案**：提供灵活的探针配置系统，支持自定义监控点
- **价值**：满足个性化监控需求，提高系统可观测性

#### 2.3.6 专业场景

**场景18：文件操作分析**
- **用户**：系统管理员和开发者
- **需求**：监控文件读写操作，了解数据文件的处理流程
- **解决方案**：跟踪文件I/O操作，记录文件路径、大小、操作类型
- **价值**：优化文件处理逻辑，排查文件相关问题

**场景19：故障复现分析**
- **用户**：开发者和测试工程师
- **需求**：复现问题时，对比正常和异常请求的参数、执行过程和结果差异
- **解决方案**：并行跟踪多个请求，提供详细的对比分析
- **价值**：提高故障复现成功率，加速问题解决

**场景20：数据调试**
- **用户**：开发者
- **需求**：调试复杂的数据处理逻辑，查看每一步的输入输出和中间结果
- **解决方案**：详细记录数据变换过程，支持断点式查看
- **价值**：提高复杂逻辑的调试效率

**场景21：场景模板学习**
- **用户**：初级开发者和新用户
- **需求**：通过内置场景配置学习最佳实践和常见问题的排查方法
- **解决方案**：提供丰富的场景模板和使用指南
- **价值**：降低学习成本，快速掌握工具使用

### 2.4 需求目标

#### 2.4.1 功能目标

**核心功能目标**
- **完整链路跟踪**：实现从HTTP请求到数据库、缓存、外部服务调用的完整链路跟踪
- **实时监控分析**：支持生产环境的实时监控和问题分析
- **配置驱动扩展**：通过配置文件即可支持新的监控场景和第三方组件
- **指标驱动收集**：用户可以直接定义关心的业务指标，系统自动收集和计算

**高级功能目标**
- **智能问题诊断**：基于历史数据和模式识别，提供智能的问题诊断建议
- **性能基线对比**：支持性能基线建立和回归检测
- **场景化模板**：提供丰富的内置场景模板，降低使用门槛
- **可视化分析**：提供直观的可视化界面，支持交互式分析

#### 2.4.2 性能目标

**响应时间目标**
- **启动时间**：trace-flow命令启动时间不超过3秒
- **配置加载**：配置文件解析和加载时间不超过100ms
- **实时响应**：跟踪结果展示延迟不超过500ms

**资源占用目标**
- **内存占用**：增加的内存占用不超过原应用的10%
- **CPU开销**：增加的CPU开销不超过原应用的2%
- **网络开销**：跟踪数据传输不影响业务网络性能

**并发性能目标**
- **高并发支持**：支持1000+ QPS的高并发场景跟踪
- **多线程安全**：确保多线程环境下的数据一致性
- **资源隔离**：跟踪功能不影响业务功能的正常执行

#### 2.4.3 可用性目标

**易用性目标**
- **零配置启动**：提供开箱即用的默认配置
- **一键场景**：常见问题排查场景一键启动
- **智能提示**：提供丰富的错误提示和修复建议
- **学习成本**：新用户在30分钟内掌握基本使用方法

**稳定性目标**
- **故障隔离**：跟踪功能异常不影响业务系统正常运行
- **优雅降级**：在资源不足时自动降级，保证业务优先
- **错误恢复**：支持配置错误的自动检测和恢复
- **兼容性保证**：向后兼容，新版本不破坏现有配置

#### 2.4.4 扩展性目标

**功能扩展目标**
- **插件化架构**：支持第三方插件和探针的无缝集成
- **API开放**：提供完整的API接口，支持二次开发
- **配置扩展**：支持用户自定义配置格式和处理逻辑
- **输出扩展**：支持多种输出格式和目标系统

**生态扩展目标**
- **社区贡献**：建立活跃的社区，支持用户贡献探针配置
- **工具集成**：与主流开发工具和监控系统集成
- **标准兼容**：兼容OpenTracing等行业标准
- **云原生支持**：支持容器化和云原生环境部署

### 2.5 目标用户

- **Java开发者**：需要快速理解和调试复杂的业务逻辑
- **系统架构师**：需要了解系统的实际运行情况和性能瓶颈
- **运维工程师**：需要在生产环境中快速定位和解决问题
- **测试工程师**：需要验证系统功能和性能是否符合预期
- **问题排查者**：需要在不深入代码的情况下快速定位问题
- **代码审查者**：需要快速掌握系统的运行机制

### 2.6 成功标准

**功能完整性：**
- ✅ 能够跟踪完整的HTTP请求生命周期（从接收到响应）
- ✅ 支持主流中间件的调用捕获（MySQL/PostgreSQL、Redis、Kafka、HTTP客户端）
- ✅ 提供灵活的输出格式（控制台树状图、JSON文件、自定义模板）
- ✅ 支持业务跟踪ID关联，便于分布式链路追踪

**执行逻辑理解能力：**
- ✅ 能够清晰展示数据的读取、处理、存储完整流程
- ✅ 自动识别和标记关键执行步骤和数据操作
- ✅ 提供清晰的调用摘要和流程概览
- ✅ 支持快速识别异常路径和性能瓶颈点

**参数和结果记录能力：**
- ✅ 完整记录所有关键操作的输入参数和执行结果
- ✅ 支持复杂对象的结构化显示和数据截断
- ✅ 提供敏感信息脱敏和安全保护机制
- ✅ 支持多级详细程度的数据展示控制

**值过滤和数据追踪能力：**
- ✅ 支持基于特定值的智能过滤和匹配
- ✅ 自动检测和追踪数据修改操作
- ✅ 提供值匹配时的自动堆栈捕获功能
- ✅ 支持复杂的过滤表达式和组合条件

**功能强大性：**
- ✅ 支持深度嵌套的调用链跟踪，无层级限制
- ✅ 支持复杂的过滤和匹配条件（URL模式、HTTP方法、请求头等）
- ✅ 支持实时和批量跟踪模式
- ✅ 支持自定义探针扩展和第三方集成

**配置驱动扩展能力：**
- ✅ 支持通过配置文件定义新探针，无需编写代码
- ✅ 提供灵活的类和方法匹配规则配置
- ✅ 支持配置文件的热更新和动态加载
- ✅ 提供丰富的配置模板和向导工具
- ✅ 支持jar包版本的精确控制和自动检测
- ✅ 支持多版本jar包的差异化配置管理

**内置场景配置能力：**
- ✅ 提供常见问题场景的预设配置模板
- ✅ 支持场景的参数化和快速应用
- ✅ 支持自定义场景的保存和管理
- ✅ 提供场景组合和最佳实践指导

**请求回放能力：**
- ✅ 支持完整的请求回放数据记录和保存
- ✅ 支持多种回放模式（精确、参数化、批量、压力）
- ✅ 提供回放结果对比和差异分析功能
- ✅ 支持回放文件的管理、导入导出功能

**跟踪结果保存和对比能力：**
- ✅ 支持跟踪结果的多格式保存（JSON、XML、文本）
- ✅ 提供跟踪结果的对比分析和差异检测功能
- ✅ 支持历史基线对比和性能回归检测
- ✅ 提供跟踪结果的搜索、分类和管理功能

**易用性要求：**
- ✅ 学习成本低，新用户5分钟内完成首次成功跟踪
- ✅ 配置简单，支持一键启用和智能默认配置
- ✅ 输出直观，非性能专家也能快速识别问题点
- ✅ 错误提示友好，配置错误时有明确的修复建议

### 2.5 预期收益

**定量收益：**
- 代码理解时间减少 80%（从平均1周降至1-2天）
- 问题定位时间减少 70%（从平均2小时降至30分钟）
- 代码理解效率提升 5倍（无需深入阅读代码即可理解流程）
- 系统维护效率提升 3倍

**定性收益：**
- 降低代码理解难度，快速掌握系统运行机制
- 提升团队对复杂系统的理解和维护能力
- 减少因不熟悉代码逻辑导致的开发错误
- 建立可视化的系统文档，便于知识传承

### 2.6 功能概述

新增 `trace-flow` 命令（别名 `tf`），实现对 HTTP 请求的完整跟踪。通过可插拔的探针系统捕获数据库、缓存、消息队列等关键组件的调用信息，并提供灵活的输出和配置管理能力。

**核心功能特性：**
- **配置驱动**：所有探针通过配置文件定义，支持热更新和动态加载
- **版本适配**：自动适配不同版本的jar包，确保兼容性
- **分层配置**：支持系统级、用户级、项目级的配置管理
- **可扩展性**：支持第三方探针贡献和自定义探针开发

## 3. 系统架构设计

### 3.1 整体架构

系统采用分层架构设计，包含以下核心组件：

- **命令层**：提供用户交互接口（trace-flow、replay、trace-diff）
- **配置层**：统一的配置管理系统，支持多层级配置和双向同步
- **探针层**：可插拔的探针系统，支持配置驱动的探针定义
- **跟踪层**：核心跟踪引擎，负责数据收集和处理
- **输出层**：多格式输出支持，包括控制台、文件、自定义模板
- **存储层**：跟踪结果和回放数据的持久化存储

### 3.2 核心设计原则

- **配置驱动**：所有功能通过配置文件定义，避免硬编码，支持版本精确控制
- **可扩展性**：支持插件化扩展，便于添加新的探针和功能
- **非侵入性**：无需修改应用代码即可实现跟踪
- **高性能**：最小化对目标应用的性能影响
- **易用性**：提供丰富的内置场景和模板，降低使用门槛
- **版本兼容**：智能检测jar包版本，确保探针配置的准确性和兼容性
- **热更新**：支持配置文件的实时监控和动态加载，无需重启应用

## 4. 功能性需求

### 4.1 trace-flow 命令核心功能

#### 4.1.1 基本要求

- **FR-0**: `trace-flow` 命令必须提供一个简短的别名 `tf`，两者功能完全等价。

#### 4.1.2 执行流程跟踪

- **FR-1**: 命令必须能够跟踪 Java 应用的完整执行流程，包括但不限于：
    - **HTTP请求跟踪**：跟踪HTTP请求的完整生命周期
        - 请求接收时间：请求到达服务器的时间戳
        - 队列等待时间：请求在 Servlet 容器线程池队列中的等待时间
        - 处理开始时间：实际开始处理请求的时间戳
        - 处理结束时间：请求处理完成的时间戳
        - 响应发送时间：响应数据发送完成的时间戳
    - **方法调用跟踪**：跟踪任意Java方法的调用链路和执行过程
    - **组件交互跟踪**：跟踪各种组件（数据库、缓存、消息队列等）之间的交互

#### 4.1.3 跟踪目标匹配

- **FR-2**: 命令必须支持多种跟踪目标的匹配方式：
    - **URL模式匹配**：基于 URL 模式 (例如，Ant 风格的 `/api/**` 或正则表达式) 来匹配特定的 HTTP 请求
    - **方法匹配**：基于类名和方法名匹配特定的Java方法调用
    - **注解匹配**：基于注解匹配标注了特定注解的方法
    - **包路径匹配**：基于包路径匹配特定包下的所有方法调用
- **FR-3**: 命令必须能够按 HTTP 方法 (例如 `GET`, `POST`) 或方法签名匹配跟踪目标。
- **FR-4**: 如果未提供匹配条件，命令必须跟踪进入系统的下一个符合条件的调用。
- **FR-5**: 命令必须支持一个计数参数，以连续跟踪 N 个满足条件的调用。

#### 4.1.4 值过滤和数据追踪

- **FR-5.1**: 必须支持基于特定值的过滤功能：
    - **参数值过滤**：根据指定的值过滤包含该值的所有操作
    - **多值过滤**：支持同时过滤多个值（如多个用户ID）
    - **模糊匹配**：支持通配符和正则表达式匹配
    - **类型智能匹配**：自动适配不同数据类型（字符串、数字、对象属性）
- **FR-5.2**: 必须提供数据修改追踪功能：
    - **写操作检测**：自动识别可能修改数据的操作（INSERT、UPDATE、DELETE等）
    - **参数深度扫描**：扫描方法参数、SQL参数、HTTP请求体中的目标值
    - **调用链关联**：显示包含目标值的完整调用链路
    - **堆栈自动捕获**：当检测到目标值时自动捕获线程堆栈
- **FR-5.3**: 必须支持灵活的过滤表达式：
    - **简单值匹配**：`--filter-value userId=12345`
    - **多字段匹配**：`--filter-value "userId=12345 OR email=user@example.com"`
    - **深度对象匹配**：`--filter-value "user.id=12345"`
    - **SQL参数匹配**：`--filter-value "sql.params contains 12345"`
    - **组合条件**：支持AND、OR、NOT等逻辑组合

### 4.2 探针系统 **【核心基础架构】**

#### 4.2.0 探针系统核心原则

- **FR-6**: 命令必须能够通过启用不同的 **探针 (Probe)** 来捕获特定组件的调用。所有探针必须通过配置文件定义，系统不应硬编码任何探针实现。
- **FR-7**: 用户必须能通过参数 (例如 `--probes database-probe,cache-probe`) 来选择和组合探针。默认启用的探针必须通过配置文件定义。
- **FR-8**: 系统必须是可扩展的，以便将来可以方便地添加新的探针 (例如 `dubbo-probe`, `mongodb-probe`, `elasticsearch-probe`, `grpc-probe`)。
- **FR-8.1**: 探针系统必须支持插件化架构，允许第三方开发者贡献探针。
- **FR-8.2**: 必须提供探针开发的标准接口和文档，包括生命周期管理、数据收集、错误处理等。
- **FR-8.3**: 必须支持探针的热插拔，无需重启 Arthas 即可加载新探针。

#### 4.2.0.1 探针配置文件核心要求

- **FR-8.0.1**: 探针配置文件必须是系统的核心组成部分，所有探针行为都通过配置文件控制：
    - **零硬编码原则**：系统代码中不得硬编码任何具体的类名、方法名、jar包信息
    - **完全配置驱动**：所有探针的目标类、方法、参数提取、数据处理都通过配置定义
    - **运行时可变**：探针行为可以通过修改配置文件在运行时改变，无需重新编译
    - **声明式定义**：探针配置使用声明式语法，描述"做什么"而不是"怎么做"
- **FR-8.0.2**: 探针配置文件必须支持完整的生命周期管理：
    - **配置验证**：加载前必须验证配置文件的语法和语义正确性
    - **依赖检查**：验证配置中引用的类、方法、jar包是否存在
    - **冲突检测**：检测多个探针配置之间的冲突和重复
    - **性能评估**：评估配置对系统性能的潜在影响
    - **安全检查**：确保配置不会访问敏感信息或执行危险操作

#### 4.2.1 统一探针配置抽象模型 **【最高优先级】**

##### 4.2.1.1 统一配置模型设计原则

- **FR-8.7**: 必须建立统一的探针配置抽象模型，支持所有场景的数据采集：
    - **统一格式**：所有探针使用相同的配置结构和语法规范
    - **场景无关**：配置模型与具体监控场景解耦，通过配置适配不同场景
    - **完全抽象**：将目标匹配、数据捕获、数据处理、输出等抽象为通用模型
    - **表达式驱动**：使用统一的表达式语言描述数据提取和处理逻辑
    - **可扩展性**：新增监控场景无需修改配置格式，只需提供新的配置实例

##### 4.2.1.2 统一配置文件结构

- **FR-8.7.1**: 所有探针配置文件必须遵循统一的顶层结构：
```json
{
  "version": "1.0",                    // 必填：配置版本
  "metadata": {                        // 必填：探针元数据
    "name": "探针名称",
    "description": "探针描述",
    "category": "探针分类",
    "author": "作者",
    "createdAt": "创建时间",
    "tags": ["标签数组"]
  },
  "probe": {                           // 必填：探针核心定义
    "enabled": true,                   // 是否启用
    "priority": 100,                   // 优先级
    "scope": "method|class|package",   // 监控范围
    "lifecycle": "before|after|around", // 生命周期
    "targets": [...],                  // 目标定义（统一格式）
    "capture": {...},                  // 数据捕获（统一格式）
    "processing": {...},               // 数据处理（统一格式）
    "output": {...}                    // 输出配置（统一格式）
  },
  "dependencies": {...},               // 可选：依赖声明
  "performance": {...},                // 可选：性能配置
  "security": {...}                    // 可选：安全配置
}
```

##### 4.2.1.3 统一目标匹配模型

- **FR-8.7.2**: 目标匹配必须使用统一的matcher模型，支持所有监控场景：
```json
{
  "targets": [
    {
      "id": "target-unique-id",
      "name": "目标名称",
      "type": "method|constructor|field|annotation",
      "matcher": {
        "strategy": "exact|wildcard|regex|annotation|inheritance|interface",
        "class": {
          "name": "类名或模式",
          "package": "包名或模式",
          "annotation": "注解名称",
          "implements": "接口名称",
          "extends": "父类名称",
          "modifiers": ["public", "!abstract"]
        },
        "method": {
          "name": "方法名或模式",
          "signature": {
            "parameterTypes": ["参数类型数组"],
            "returnType": "返回类型",
            "exceptions": ["异常类型数组"]
          },
          "annotation": "方法注解",
          "modifiers": ["public", "!static"]
        }
      },
      "versions": {
        "groupId": "Maven groupId",
        "artifactId": "Maven artifactId",
        "versionRange": "[1.0.0, 2.0.0)",
        "excludeVersions": ["排除版本数组"]
      }
    }
  ]
}
```

##### 4.2.1.4 统一数据捕获模型

- **FR-8.7.3**: 数据捕获必须使用统一的capture模型，适配所有数据类型：
```json
{
  "capture": {
    "scope": "local|global|thread|request",
    "timing": {
      "before": {"enabled": true, "timestamp": true},
      "after": {"enabled": true, "executionTime": true},
      "exception": {"enabled": true, "stackTrace": true}
    },
    "data": {
      "parameters": {
        "enabled": true,
        "strategy": "all|indexed|named|filtered",
        "indexes": [0, 1, 2],
        "depth": 3,
        "sizeLimit": 1024
      },
      "returnValue": {
        "enabled": true,
        "depth": 2,
        "sizeLimit": 2048
      },
      "context": {
        "enabled": true,
        "threadLocal": true,
        "requestAttributes": true
      }
    }
  }
}
```
##### 4.2.1.5 统一指标驱动模型 **【核心创新】**

- **FR-8.7.4**: 探针配置必须以指标为驱动，通过定义指标和计算规则实现数据收集：
```json
{
  "metrics": {
    "definitions": [
      {
        "id": "start-time",
        "name": "开始执行时间",
        "type": "timestamp",
        "source": "System.currentTimeMillis()",
        "capturePoint": "before",
        "unit": "milliseconds"
      },
      {
        "id": "end-time",
        "name": "结束执行时间",
        "type": "timestamp",
        "source": "System.currentTimeMillis()",
        "capturePoint": "after",
        "unit": "milliseconds"
      },
      {
        "id": "execution-time",
        "name": "执行耗时",
        "type": "computed",
        "formula": "metrics.endTime - metrics.startTime",
        "unit": "milliseconds",
        "description": "方法执行的总耗时"
      },
      {
        "id": "request-url",
        "name": "请求URL",
        "type": "string",
        "source": "args[0].getRequestURL().toString()",
        "capturePoint": "before"
      },
      {
        "id": "response-status",
        "name": "响应状态码",
        "type": "number",
        "source": "args[1].getStatus()",
        "capturePoint": "after"
      }
    ],
    "aggregations": [
      {
        "id": "avg-execution-time",
        "name": "平均执行时间",
        "type": "average",
        "sourceMetric": "execution-time",
        "window": "1m",
        "groupBy": ["request-url"]
      },
      {
        "id": "error-rate",
        "name": "错误率",
        "type": "percentage",
        "formula": "count(response-status >= 400) / count(*) * 100",
        "window": "1m"
      }
    ]
  }
}
```

##### 4.2.1.6 统一数据处理模型

- **FR-8.7.5**: 数据处理必须支持指标驱动的处理流程：
```json
{
  "processing": {
    "extractors": [
      {
        "id": "extractor-id",
        "name": "提取器名称",
        "type": "parameter|return|field|context|computed",
        "source": "数据源表达式",
        "target": "目标字段名",
        "dataType": "string|number|boolean|object|array",
        "processor": "处理器名称"
      }
    ],
    "transformers": [
      {
        "id": "transformer-id",
        "type": "format|mask|encrypt|compress",
        "source": "源字段",
        "target": "目标字段",
        "config": {"处理器配置": "值"}
      }
    ],
    "filters": [
      {
        "id": "filter-id",
        "condition": "过滤条件表达式",
        "action": "include|exclude|tag|transform|alert",
        "priority": 100
      }
    ]
  }
}
```

##### 4.2.1.7 统一表达式语言规范

- **FR-8.7.6**: 所有探针必须使用统一的表达式语言，支持跨场景的数据提取和指标计算：
    - **基础表达式**：
        - `this` - 当前对象实例
        - `args[0]` - 方法参数（按索引）
        - `args.userId` - 方法参数（按名称）
        - `returnValue` - 方法返回值
        - `exception` - 抛出的异常
        - `target` - 目标对象
    - **属性访问**：
        - `this.userId` - 对象属性访问
        - `args[0].request.headers` - 嵌套属性访问
        - `returnValue.data.size()` - 方法调用
    - **条件表达式**：
        - `args[0] != null ? args[0].toString() : "null"` - 三元运算符
        - `returnValue instanceof List ? returnValue.size() : 0` - 类型检查
    - **内置函数**：
        - `JSON.stringify(obj)` - JSON序列化
        - `String.format("template", args)` - 字符串格式化
        - `MD5.hash(str)` - 哈希计算
        - `StringUtils.truncate(str, 100)` - 字符串截断

##### 4.2.1.5 分离式配置文件结构

- **FR-8.7.6**: 必须提供分离式的探针配置文件结构，每个探针使用独立的配置文件：
    - **HTTP服务探针**：`http-server-probe.json` - HTTP请求接收和处理的监控配置
    - **数据库探针**：`database-probe.json` - JDBC相关类和方法的监控配置
    - **缓存探针**：`cache-probe.json` - Redis、Memcached等缓存客户端的监控配置
    - **消息队列探针**：`messaging-probe.json` - Kafka、RabbitMQ等消息中间件的监控配置
    - **HTTP客户端探针**：`http-client-probe.json` - 各种HTTP客户端库的监控配置
    - **文件操作探针**：`file-operations-probe.json` - 文件I/O操作的监控配置
    - **自定义探针**：用户可以创建自己的探针配置文件，如 `custom-business-probe.json`
- **FR-8.8**: 必须提供灵活的匹配规则配置：
    - **类匹配规则**：支持精确匹配、通配符匹配、正则表达式匹配
    - **方法匹配规则**：支持方法名、参数类型、返回类型、注解匹配
    - **版本匹配规则**：支持jar包版本的精确匹配、范围匹配、排除匹配
    - **条件过滤**：支持基于参数值、返回值的条件过滤
    - **组合规则**：支持AND、OR、NOT等逻辑组合
- **FR-8.8.1**: 必须支持jar包版本的精确控制：
    - **版本范围指定**：支持指定jar包的版本范围（如：[1.0.0, 2.0.0)）
    - **版本排除**：支持排除特定版本的jar包（如：!1.5.0）
    - **版本检测**：运行时自动检测jar包版本并匹配相应的探针配置
    - **版本兼容性警告**：当jar包版本与配置不匹配时提供明确的警告信息
    - **多版本支持**：同一个探针可以为不同版本的jar包提供不同的配置
- **FR-8.9**: 必须支持配置文件的动态加载和热更新：
    - **配置文件监控**：自动监控配置文件变化
    - **热更新机制**：配置变更后自动重新加载探针
    - **配置验证**：加载前验证配置文件的正确性
    - **回滚机制**：配置错误时自动回滚到上一个有效配置

#### 4.2.2 探针配置格式和示例

#### 4.2.2 探针配置文件格式标准 **【强制规范】**

##### 4.2.2.1 配置文件结构规范

- **FR-8.10**: 必须提供标准的探针配置格式，强制使用JSON格式：
    - **文件编码**：必须使用UTF-8编码
    - **JSON格式**：严格遵循JSON规范，支持注释（使用JSON5扩展）
    - **缩进格式**：使用2个空格缩进，确保可读性
    - **字段命名**：使用camelCase命名规范
    - **必填字段验证**：所有必填字段必须存在且非空

- **FR-8.10.1**: 探针配置文件必须包含以下标准结构：
```json
{
  "version": "1.0",                    // 必填：配置文件版本
  "metadata": {                        // 必填：探针元数据
    "name": "探针名称",
    "description": "探针描述",
    "category": "探针分类",
    "author": "作者信息",
    "createdAt": "2025-01-11T10:00:00Z",
    "updatedAt": "2025-01-11T10:00:00Z",
    "tags": ["tag1", "tag2"]
  },
  "probe": {                           // 必填：探针定义
    "enabled": true,                   // 必填：是否启用
    "priority": 100,                   // 可选：优先级（数字越大优先级越高）
    "targets": [...],                  // 必填：目标定义数组
    "capture": {...},                  // 必填：数据捕获配置
    "format": {...},                   // 必填：数据格式化配置
    "filters": [...],                  // 可选：过滤规则数组
    "conditions": {...}                // 可选：启用条件
  },
  "dependencies": {                    // 可选：依赖声明
    "requiredJars": [...],
    "optionalJars": [...],
    "conflictsWith": [...]
  },
  "performance": {                     // 可选：性能配置
    "maxExecutionTime": 5000,
    "maxMemoryUsage": "10MB",
    "samplingRate": 1.0
  }
}
```

##### 4.2.2.2 目标定义详细规范

- **FR-8.10.2**: targets数组中的每个目标必须包含完整的匹配信息：
```json
{
  "name": "目标名称",                   // 必填：目标的唯一标识
  "description": "目标描述",           // 可选：目标的详细描述
  "class": "com.example.UserService", // 必填：目标类名（支持通配符和正则）
  "classMatchType": "exact",          // 必填：匹配类型 exact|wildcard|regex|annotation
  "methods": ["findById", "save"],    // 必填：目标方法列表
  "methodMatchType": "exact",         // 必填：方法匹配类型
  "methodSignature": {                // 可选：精确方法签名
    "parameterTypes": ["java.lang.Long"],
    "returnType": "com.example.User",
    "modifiers": ["public"],
    "annotations": ["@Transactional"]
  },
  "jarVersions": {                    // 必填：jar包版本要求
    "groupId": "com.example",
    "artifactId": "user-service",
    "versionRange": "[1.0.0, 2.0.0)",
    "excludeVersions": ["1.2.0"]
  },
  "conditions": {                     // 可选：启用条件
    "javaVersion": "[8,)",
    "osName": "!Windows",
    "systemProperty": "spring.profiles.active=prod"
  }
}
```

##### 4.2.2.3 数据捕获配置规范

- **FR-8.10.3**: capture配置必须精确定义数据捕获行为：
```json
{
  "parameters": {
    "enabled": true,                  // 是否捕获参数
    "indexes": [0, 1, 2],            // 捕获的参数索引，null表示全部
    "names": ["userId", "request"],   // 捕获的参数名称
    "depth": 3,                      // 对象递归深度
    "sizeLimit": 1024,               // 单个参数大小限制（字节）
    "excludeTypes": ["java.lang.Class"] // 排除的参数类型
  },
  "returnValue": {
    "enabled": true,
    "depth": 2,
    "sizeLimit": 2048,
    "excludeNull": false             // 是否排除null返回值
  },
  "exceptions": {
    "enabled": true,
    "types": ["java.sql.SQLException"], // 只捕获指定类型异常
    "stackTrace": true,
    "stackDepth": 20,
    "causeChain": true               // 是否捕获异常链
  },
  "executionTime": {
    "enabled": true,
    "unit": "milliseconds",          // 时间单位
    "precision": 3                   // 精度（小数位数）
  },
  "threadInfo": {
    "enabled": false,
    "captureThreadName": true,
    "captureThreadId": true,
    "captureThreadState": false
  }
}
```
##### 4.2.2.4 数据格式化配置规范

- **FR-8.10.4**: format配置必须定义数据的格式化和提取规则：
```json
{
  "operationType": "DATABASE",        // 必填：操作类型标识
  "displayName": "数据库查询",        // 可选：显示名称
  "parameterExtractors": [            // 参数提取器数组
    {
      "name": "sql",                  // 提取字段名称
      "expression": "this.toString()", // 提取表达式
      "type": "string",               // 数据类型
      "required": true,               // 是否必需
      "defaultValue": "unknown",      // 默认值
      "formatter": "sql",             // 格式化器类型
      "truncate": 500,                // 截断长度
      "mask": false                   // 是否脱敏
    },
    {
      "name": "parameters",
      "expression": "args",
      "type": "array",
      "processor": "sqlParameterProcessor" // 自定义处理器
    }
  ],
  "resultExtractors": [               // 结果提取器数组
    {
      "name": "affectedRows",
      "expression": "returnValue",
      "type": "integer",
      "condition": "returnValue instanceof Integer"
    },
    {
      "name": "resultSetSize",
      "expression": "returnValue.size()",
      "type": "integer",
      "condition": "returnValue instanceof Collection"
    }
  ],
  "contextExtractors": [              // 上下文提取器数组
    {
      "name": "connectionUrl",
      "expression": "this.getConnection().getMetaData().getURL()",
      "type": "string",
      "cache": true                   // 是否缓存结果
    }
  ],
  "outputTemplate": "SQL: ${sql} | Params: ${parameters} | Rows: ${affectedRows}" // 输出模板
}
```

##### 4.2.2.5 过滤规则配置规范

- **FR-8.10.5**: filters配置必须支持复杂的过滤逻辑：
```json
[
  {
    "name": "读操作过滤器",
    "condition": "sql.toUpperCase().startsWith('SELECT')",
    "action": "include",              // include|exclude|tag|transform
    "tags": ["read", "query"],
    "priority": 100
  },
  {
    "name": "写操作过滤器",
    "condition": "sql.matches('(?i)^(INSERT|UPDATE|DELETE).*')",
    "action": "tag",
    "tags": ["write", "modification"],
    "metadata": {
      "risk": "high",
      "audit": true
    }
  },
  {
    "name": "慢查询过滤器",
    "condition": "executionTime > 1000",
    "action": "include",
    "tags": ["slow", "performance"],
    "alert": {
      "enabled": true,
      "threshold": 5000,
      "message": "检测到慢查询: ${sql}"
    }
  },
  {
    "name": "敏感数据过滤器",
    "condition": "sql.contains('password') || sql.contains('credit_card')",
    "action": "transform",
    "transformer": "sensitiveDataMasker",
    "config": {
      "maskPattern": "***",
      "preserveLength": false
    }
  }
]
```

##### 4.2.2.6 配置验证规则

- **FR-8.10.6**: 配置文件必须通过严格的验证规则：
    - **JSON Schema验证**：使用JSON Schema定义配置文件的结构和约束
    - **语义验证**：验证表达式语法、类名存在性、方法签名正确性
    - **依赖验证**：检查jar包依赖、版本兼容性、冲突检测
    - **性能验证**：评估配置对系统性能的影响，超过阈值时警告
    - **安全验证**：检查配置是否访问敏感信息或执行危险操作

- **FR-8.11**: 配置驱动探针必须支持以下典型场景：
    - **文件操作探针**：监控文件读写、创建、删除等操作
    - **网络操作探针**：监控Socket连接、数据传输等操作
    - **缓存操作探针**：监控自定义缓存框架的操作
    - **业务方法探针**：监控特定业务方法的调用和执行
    - **第三方库探针**：监控第三方库的关键方法调用
- **FR-8.12**: 必须支持jar包版本的自动检测和匹配：
    - **版本检测机制**：运行时自动检测classpath中jar包的版本信息
    - **版本匹配算法**：根据配置的版本范围自动选择合适的探针配置
    - **版本冲突处理**：当存在多个版本的同一jar包时，提供明确的处理策略
    - **版本兼容性报告**：生成jar包版本与探针配置的兼容性报告
    - **动态版本适配**：支持在运行时根据实际jar包版本动态调整探针行为

#### 4.2.3 统一探针配置文件示例

**HTTP服务探针配置文件 (`http-server-probe.json`) - 使用统一格式：**
```json
{
  "version": "1.0",
  "metadata": {
    "name": "HTTP服务探针",
    "description": "监控HTTP请求的接收和处理",
    "category": "http-server",
    "author": "Arthas Team",
    "createdAt": "2025-01-11T10:00:00Z",
    "tags": ["http", "server", "request", "servlet"]
  },
  "probe": {
    "enabled": true,
    "priority": 200,
    "scope": "method",
    "lifecycle": "around",
    "targets": [
      {
        "id": "servlet-request",
        "name": "Servlet请求处理",
        "type": "method",
        "matcher": {
          "strategy": "exact",
          "class": {
            "name": "javax.servlet.http.HttpServlet"
          },
          "method": {
            "name": "service|doGet|doPost|doPut|doDelete"
          }
        },
        "versions": {
          "groupId": "javax.servlet",
          "artifactId": "servlet-api",
          "versionRange": "[2.5.0,)",
          "excludeVersions": []
        }
      },
      {
        "name": "spring-mvc-handler",
        "description": "Spring MVC请求处理",
        "class": "org.springframework.web.servlet.DispatcherServlet",
        "classMatchType": "exact",
        "methods": ["doDispatch"],
        "methodMatchType": "exact",
        "jarVersions": {
          "groupId": "org.springframework",
          "artifactId": "spring-webmvc",
          "versionRange": "[3.0.0,)",
          "excludeVersions": []
        }
      },
      {
        "name": "controller-methods",
        "description": "Controller方法调用",
        "class": "*",
        "classMatchType": "annotation",
        "annotation": "@org.springframework.stereotype.Controller",
        "methods": ["*"],
        "methodMatchType": "annotation",
        "methodAnnotation": "@org.springframework.web.bind.annotation.RequestMapping",
        "jarVersions": {
          "groupId": "org.springframework",
          "artifactId": "spring-web",
          "versionRange": "[3.0.0,)",
          "excludeVersions": []
        }
      },
      {
        "name": "rest-controller-methods",
        "description": "RestController方法调用",
        "class": "*",
        "classMatchType": "annotation",
        "annotation": "@org.springframework.web.bind.annotation.RestController",
        "methods": ["*"],
        "methodMatchType": "annotation",
        "methodAnnotation": "@org.springframework.web.bind.annotation.*Mapping",
        "jarVersions": {
          "groupId": "org.springframework",
          "artifactId": "spring-web",
          "versionRange": "[4.0.0,)",
          "excludeVersions": []
        }
      }
    ],
    "capture": {
      "parameters": {
        "enabled": true,
        "indexes": [0, 1],
        "depth": 2,
        "sizeLimit": 2048,
        "excludeTypes": ["javax.servlet.ServletContext"]
      },
      "returnValue": {
        "enabled": true,
        "depth": 2,
        "sizeLimit": 4096,
        "excludeNull": false
      },
      "exceptions": {
        "enabled": true,
        "types": ["java.lang.Exception"],
        "stackTrace": true,
        "stackDepth": 30,
        "causeChain": true
      },
      "executionTime": {
        "enabled": true,
        "unit": "milliseconds",
        "precision": 3
      },
      "threadInfo": {
        "enabled": true,
        "captureThreadName": true,
        "captureThreadId": true
      }
    },
    "format": {
      "operationType": "HTTP_REQUEST",
      "displayName": "HTTP请求处理",
      "parameterExtractors": [
        {
          "name": "requestUrl",
          "expression": "args[0].getRequestURL().toString()",
          "type": "string",
          "required": true
        },
        {
          "name": "requestMethod",
          "expression": "args[0].getMethod()",
          "type": "string",
          "required": true
        },
        {
          "name": "requestHeaders",
          "expression": "extractHeaders(args[0])",
          "type": "object",
          "processor": "httpHeaderProcessor"
        },
        {
          "name": "requestParameters",
          "expression": "args[0].getParameterMap()",
          "type": "object",
          "truncate": 1000
        },
        {
          "name": "requestBody",
          "expression": "extractRequestBody(args[0])",
          "type": "string",
          "truncate": 2000,
          "processor": "requestBodyProcessor"
        },
        {
          "name": "clientIp",
          "expression": "getClientIpAddress(args[0])",
          "type": "string"
        },
        {
          "name": "userAgent",
          "expression": "args[0].getHeader('User-Agent')",
          "type": "string",
          "truncate": 200
        },
        {
          "name": "sessionId",
          "expression": "args[0].getSession(false) != null ? args[0].getSession().getId() : null",
          "type": "string"
        }
      ],
      "resultExtractors": [
        {
          "name": "responseStatus",
          "expression": "args[1].getStatus()",
          "type": "integer"
        },
        {
          "name": "responseHeaders",
          "expression": "extractResponseHeaders(args[1])",
          "type": "object",
          "processor": "httpHeaderProcessor"
        },
        {
          "name": "responseSize",
          "expression": "getResponseSize(args[1])",
          "type": "long"
        },
        {
          "name": "contentType",
          "expression": "args[1].getContentType()",
          "type": "string"
        }
      ],
      "contextExtractors": [
        {
          "name": "serverName",
          "expression": "args[0].getServerName()",
          "type": "string",
          "cache": true
        },
        {
          "name": "serverPort",
          "expression": "args[0].getServerPort()",
          "type": "integer",
          "cache": true
        },
        {
          "name": "contextPath",
          "expression": "args[0].getContextPath()",
          "type": "string",
          "cache": true
        }
      ],
      "outputTemplate": "${requestMethod} ${requestUrl} | Status: ${responseStatus} | Time: ${executionTime}ms | IP: ${clientIp}"
    },
    "filters": [
      {
        "name": "静态资源过滤器",
        "condition": "requestUrl.matches('.*\\\\.(css|js|png|jpg|jpeg|gif|ico|woff|woff2)$')",
        "action": "exclude",
        "priority": 100
      },
      {
        "name": "健康检查过滤器",
        "condition": "requestUrl.contains('/health') || requestUrl.contains('/actuator')",
        "action": "exclude",
        "priority": 90
      },
      {
        "name": "API请求标记",
        "condition": "requestUrl.startsWith('/api/')",
        "action": "tag",
        "tags": ["api", "rest"],
        "priority": 80
      },
      {
        "name": "错误请求标记",
        "condition": "responseStatus >= 400",
        "action": "tag",
        "tags": ["error", "client-error"],
        "priority": 70,
        "alert": {
          "enabled": true,
          "threshold": 500,
          "message": "检测到HTTP错误: ${requestMethod} ${requestUrl} -> ${responseStatus}"
        }
      },
      {
        "name": "慢请求标记",
        "condition": "executionTime > 1000",
        "action": "tag",
        "tags": ["slow", "performance"],
        "priority": 60,
        "metadata": {
          "risk": "medium",
          "monitor": true
        }
      },
      {
        "name": "敏感参数脱敏",
        "condition": "requestParameters.containsKey('password') || requestParameters.containsKey('token')",
        "action": "transform",
        "transformer": "sensitiveParameterMasker",
        "priority": 50,
        "config": {
          "maskFields": ["password", "token", "secret", "key"],
          "maskPattern": "***"
        }
      }
    ]
  },
  "dependencies": {
    "requiredJars": [
      {
        "groupId": "javax.servlet",
        "artifactId": "servlet-api",
        "versionRange": "[2.5.0,)"
      }
    ],
    "optionalJars": [
      {
        "groupId": "org.springframework",
        "artifactId": "spring-webmvc",
        "versionRange": "[3.0.0,)"
      }
    ]
  },
  "performance": {
    "maxExecutionTime": 10000,
    "maxMemoryUsage": "20MB",
    "samplingRate": 1.0,
    "batchSize": 100
  }
}
```

**数据库探针配置文件 (`database-probe.json`)：**
```json
{
  "version": "1.0",
  "probe": {
    "name": "数据库操作探针",
    "description": "监控JDBC数据库操作",
    "enabled": true,
    "category": "database",
    "targets": [
      {
        "class": "java.sql.PreparedStatement",
        "methods": ["execute", "executeQuery", "executeUpdate"],
        "jarVersions": {
          "groupId": "java.sql",
          "artifactId": "*",
          "versionRange": "[1.0.0,)",
          "excludeVersions": []
        }
      },
      {
        "class": "java.sql.Statement",
        "methods": ["execute", "executeQuery", "executeUpdate"],
        "jarVersions": {
          "groupId": "java.sql",
          "artifactId": "*",
          "versionRange": "[1.0.0,)",
          "excludeVersions": []
        }
      },
      {
        "class": "java.sql.Connection",
        "methods": ["prepareStatement", "createStatement"],
        "jarVersions": {
          "groupId": "java.sql",
          "artifactId": "*",
          "versionRange": "[1.0.0,)",
          "excludeVersions": []
        }
      }
    ],
    "capture": {
      "parameters": true,
      "returnValue": true,
      "exceptions": true,
      "executionTime": true
    },
    "format": {
      "operationType": "DATABASE",
      "parameterExtractors": [
        {
          "name": "sql",
          "expression": "this.toString()"
        },
        {
          "name": "parameters",
          "expression": "this.getParameterMetaData()"
        }
      ],
      "resultExtractors": [
        {
          "name": "affectedRows",
          "expression": "returnValue"
        },
        {
          "name": "resultSetSize",
          "expression": "returnValue.getFetchSize()"
        }
      ]
    },
    "filters": [
      {
        "condition": "sql.contains('SELECT')",
        "tags": ["read"]
      },
      {
        "condition": "sql.contains('INSERT') || sql.contains('UPDATE') || sql.contains('DELETE')",
        "tags": ["write"]
      }
    ]
  }
}
```

**缓存探针配置文件 (`cache-probe.json`)：**
```json
{
  "version": "1.0",
  "probe": {
    "name": "缓存操作探针",
    "description": "监控Redis等缓存操作",
    "enabled": true,
    "category": "cache",
    "targets": [
      {
        "class": "redis.clients.jedis.Jedis",
        "methods": ["get", "set", "del", "exists", "expire"],
        "jarVersions": {
          "groupId": "redis.clients",
          "artifactId": "jedis",
          "versionRange": "[2.0.0, 5.0.0)",
          "excludeVersions": ["3.1.0"]
        }
      },
      {
        "class": "org.springframework.data.redis.core.RedisTemplate",
        "methods": ["opsForValue", "opsForHash", "opsForList"],
        "jarVersions": {
          "groupId": "org.springframework.data",
          "artifactId": "spring-data-redis",
          "versionRange": "[1.0.0,)",
          "excludeVersions": []
        }
      }
    ],

    "capture": {
      "parameters": true,
      "returnValue": true,
      "exceptions": true,
      "executionTime": true
    },
    "format": {
      "operationType": "CACHE",
      "parameterExtractors": [
        {
          "name": "key",
          "expression": "args[0]"
        },
        {
          "name": "value",
          "expression": "args[1]",
          "truncate": 1000
        }
      ],
      "resultExtractors": [
        {
          "name": "result",
          "expression": "returnValue",
          "truncate": 1000
        }
      ]
    }
  }
}
```

**HTTP客户端探针配置文件 (`http-client-probe.json`)：**
```json
{
  "version": "1.0",
  "probe": {
    "name": "HTTP客户端探针",
    "description": "监控HTTP客户端调用",
    "enabled": true,
    "category": "http",
    "targets": [
      {
        "class": "org.apache.http.impl.client.CloseableHttpClient",
        "methods": ["execute"],
        "jarVersions": {
          "groupId": "org.apache.httpcomponents",
          "artifactId": "httpclient",
          "versionRange": "[4.0.0, 6.0.0)",
          "excludeVersions": []
        }
      },
      {
        "class": "okhttp3.OkHttpClient",
        "methods": ["newCall"],
        "jarVersions": {
          "groupId": "com.squareup.okhttp3",
          "artifactId": "okhttp",
          "versionRange": "[3.0.0, 5.0.0)",
          "excludeVersions": []
        }
      },
      {
        "class": "org.springframework.web.client.RestTemplate",
        "methods": ["exchange", "getForObject", "postForObject"],
        "jarVersions": {
          "groupId": "org.springframework",
          "artifactId": "spring-web",
          "versionRange": "[3.0.0,)",
          "excludeVersions": []
        }
      }
    ],

    "capture": {
      "parameters": true,
      "returnValue": true,
      "exceptions": true,
      "executionTime": true
    },
    "format": {
      "operationType": "HTTP_CLIENT",
      "parameterExtractors": [
        {
          "name": "url",
          "expression": "args[0].getURI().toString()"
        },
        {
          "name": "method",
          "expression": "args[0].getMethod()"
        },
        {
          "name": "headers",
          "expression": "args[0].getAllHeaders()"
        }
      ],
      "resultExtractors": [
        {
          "name": "statusCode",
          "expression": "returnValue.getStatusLine().getStatusCode()"
        },
        {
          "name": "responseSize",
          "expression": "returnValue.getEntity().getContentLength()"
        }
      ]
    }
  }
}
```

#### 4.2.4 探针组合配置文件

**探针组合配置文件 (`probe-groups.json`)：**
```json
{
  "version": "1.0",
  "groups": {
    "default": {
      "description": "默认启用的探针组合",
      "probes": [
        "http-server-probe.json",
        "database-probe.json",
        "cache-probe.json",
        "http-client-probe.json"
      ]
    },
    "full": {
      "description": "完整的探针组合",
      "probes": [
        "http-server-probe.json",
        "database-probe.json",
        "cache-probe.json",
        "http-client-probe.json",
        "file-operations-probe.json",
        "messaging-probe.json"
      ]
    },
    "web-only": {
      "description": "Web应用专用探针组合",
      "probes": [
        "http-server-probe.json",
        "http-client-probe.json"
      ],
      "config": {
        "capture-request-body": true,
        "capture-response-body": false
      }
    },
    "performance": {
      "description": "性能分析专用探针组合",
      "probes": [
        "http-server-probe.json",
        "database-probe.json",
        "cache-probe.json"
      ],
      "config": {
        "stack-trace-threshold": 100,
        "capture-queue-time": true
      }
    },
    "minimal": {
      "description": "最小探针组合，仅HTTP服务监控",
      "probes": [
        "http-server-probe.json"
      ]
    }
  }
}
```

#### 4.2.5 探针配置文件目录结构

**系统级探针配置目录结构：**
```
$ARTHAS_HOME/probes/
├── http-server-probe.json       # HTTP服务探针配置
├── database-probe.json          # 数据库探针配置
├── cache-probe.json             # 缓存探针配置
├── http-client-probe.json       # HTTP客户端探针配置
├── file-operations-probe.json   # 文件操作探针配置
├── messaging-probe.json         # 消息队列探针配置
├── probe-groups.json            # 探针组合配置
└── README.md                    # 配置文件说明文档
```

**用户级探针配置目录结构：**
```
~/.arthas/probes/
├── http-server-probe.json       # 用户自定义HTTP服务探针配置
├── database-probe.json          # 用户自定义数据库探针配置
├── custom-business-probe.json   # 用户自定义业务探针配置
├── third-party-probe.json       # 第三方库探针配置
└── probe-groups.json            # 用户自定义探针组合
```

**项目级探针配置目录结构：**
```
./probes/
├── http-server-probe.json       # 项目特定HTTP服务探针配置
├── database-probe.json          # 项目特定数据库探针配置
├── business-probe.json          # 项目业务探针配置
├── integration-probe.json       # 项目集成探针配置
└── probe-groups.json            # 项目探针组合配置
```

#### 4.2.3.1 多版本jar包支持示例

**多版本Redis客户端探针配置示例：**
```json
{
  "probes": {
    "redis-multi-version": {
      "name": "多版本Redis客户端探针",
      "description": "支持不同版本的Redis客户端库",
      "enabled": true,
      "category": "cache",
      "versionConfigs": [
        {
          "name": "jedis-2x",
          "jarVersions": {
            "groupId": "redis.clients",
            "artifactId": "jedis",
            "versionRange": "[2.0.0, 3.0.0)",
            "excludeVersions": ["2.1.0"]
          },
          "targets": [
            {
              "class": "redis.clients.jedis.Jedis",
              "methods": ["get", "set", "del"],
              "parameterExtractors": [
                {
                  "name": "key",
                  "expression": "args[0]"
                }
              ]
            }
          ]
        },
        {
          "name": "jedis-3x",
          "jarVersions": {
            "groupId": "redis.clients",
            "artifactId": "jedis",
            "versionRange": "[3.0.0, 4.0.0)",
            "excludeVersions": []
          },
          "targets": [
            {
              "class": "redis.clients.jedis.Jedis",
              "methods": ["get", "set", "del", "exists"],
              "parameterExtractors": [
                {
                  "name": "key",
                  "expression": "args[0]"
                },
                {
                  "name": "connectionInfo",
                  "expression": "this.getClient().getHost() + ':' + this.getClient().getPort()"
                }
              ]
            }
          ]
        },
        {
          "name": "jedis-4x",
          "jarVersions": {
            "groupId": "redis.clients",
            "artifactId": "jedis",
            "versionRange": "[4.0.0,)",
            "excludeVersions": []
          },
          "targets": [
            {
              "class": "redis.clients.jedis.Jedis",
              "methods": ["get", "set", "del", "exists", "expire"],
              "parameterExtractors": [
                {
                  "name": "key",
                  "expression": "args[0]"
                },
                {
                  "name": "connectionInfo",
                  "expression": "this.getConnectionPoolConfig().toString()"
                }
              ]
            }
          ]
        }
      ],
      "versionDetection": {
        "strategy": "automatic",
        "fallbackToLatest": true,
        "warnOnMismatch": true
      }
    }
  }
}
```

#### 4.2.4 参数和结果详细记录

- **FR-8.4**: 必须提供详细的参数记录功能：
    - **HTTP请求参数**：URL参数、表单参数、JSON请求体、请求头信息
    - **方法调用参数**：Java方法的输入参数值、参数类型、参数名称
    - **SQL参数绑定**：PreparedStatement的参数绑定值和类型
    - **参数值处理**：支持复杂对象的序列化显示、循环引用检测
- **FR-8.5**: 必须提供详细的执行结果记录功能：
    - **HTTP响应结果**：响应状态码、响应头、响应体内容（支持截断）
    - **方法返回值**：Java方法的返回值、返回类型、是否为null
    - **SQL执行结果**：ResultSet内容摘要、影响行数、生成的主键
    - **异常信息**：完整的异常堆栈、异常类型、异常消息
- **FR-8.6**: 必须支持数据截断和脱敏：
    - **大数据截断**：超过指定大小的数据自动截断并显示大小信息
    - **敏感信息脱敏**：自动识别和脱敏密码、令牌、身份证号等敏感信息
    - **自定义脱敏规则**：支持用户定义脱敏字段和脱敏策略
    - **完整数据保留选项**：提供参数控制是否保留完整的原始数据

### 4.3 Trace ID 关联

- **FR-9**: 命令必须为每次独立的请求跟踪生成一个全局唯一的 **Arthas Trace ID** (例如，使用 UUID)。此 ID 由 Arthas 生成，并且必须存在于每个跟踪结果中。
- **FR-10**: 命令必须支持用户提供一个表达式 (例如 OGNL)，以从当前 HTTP 请求 (`HttpServletRequest` 对象) 的任何部分 (如请求头、参数或 Cookie) 提取 **业务 Trace ID**。
- **FR-11**: **Arthas Trace ID** 和任何成功提取的 **业务 Trace ID** 都必须在最终输出中清晰地显示。

### 4.4 输出与格式

- **FR-12**: 执行后，命令必须在控制台中以清晰的树状结构显示跟踪结果。输出的头部应包含：
    - **Arthas Trace ID**
    - **请求基本信息**（URL、方法、状态码）
    - **时间统计摘要**：
        - 队列等待时间（Queue Wait）
        - 实际处理时间（Processing Time）
        - 总响应时间（Total Time）
- **FR-13**: 树状结构中的节点必须严格按照它们的实际执行顺序列出。子节点代表嵌套调用。
- **FR-14**: 每个节点必须清晰地显示其来源探针类型 (如 JDBC, Redis)、持续时间以及关键信息 (如 SQL 语句, Redis 命令)。
- **FR-15**: 必须在输出中明确区分和显示：
    - **队列等待时间**：请求在线程池队列中等待的时间
    - **处理时间**：实际业务逻辑执行的时间
    - **各组件调用时间**：数据库、缓存等各个组件的调用耗时
- **FR-15.1**: 必须提供业务逻辑理解友好的输出格式：
    - **数据库操作摘要**：显示访问了哪些表、执行了什么类型的操作（SELECT/INSERT/UPDATE/DELETE）
    - **缓存操作摘要**：显示缓存的读写操作和命中情况
    - **外部服务调用摘要**：显示调用了哪些外部API和响应状态
    - **业务流程概览**：按时间顺序展示主要的业务处理步骤
- **FR-15.2**: 必须提供详细的参数和结果显示：
    - **参数展示**：以结构化方式显示输入参数，支持嵌套对象的展开
    - **结果展示**：清晰显示执行结果，包括返回值、影响行数、状态信息
    - **数据对比**：支持显示执行前后的数据变化（如UPDATE操作的前后对比）
    - **异常详情**：完整显示异常信息，包括异常类型、消息、关键堆栈
- **FR-15.3**: 必须支持多级详细程度控制：
    - **简要模式**：只显示关键信息和摘要
    - **标准模式**：显示主要参数和结果
    - **详细模式**：显示所有参数、结果和元数据
    - **调试模式**：显示完整的原始数据，包括内部状态信息
- **FR-15.4**: 必须提供值过滤结果的特殊显示：
    - **匹配高亮**：在输出中高亮显示匹配的目标值
    - **过滤摘要**：显示过滤统计信息（总操作数、匹配操作数、过滤率）
    - **匹配路径**：显示目标值在参数结构中的具体位置
    - **堆栈关联**：将堆栈信息与匹配的操作关联显示
    - **时间线视图**：按时间顺序显示所有包含目标值的操作
- **FR-16**: 必须提供一个参数 (例如 `--stack-trace-threshold <ms>`) 来捕获线程的调用堆栈。
    - **机制**: 只有当探针 (如 JDBC) 记录的事件的执行耗时超过指定的毫秒阈值时，才会捕获其堆栈跟踪。
    - **特殊值**: 如果阈值设置为 `0`，将为所有被探测到的事件捕获堆栈跟踪。
- **FR-16.1**: 必须支持基于值过滤的自动堆栈捕获：
    - **值匹配触发**：当检测到指定的过滤值时，自动捕获完整的线程堆栈
    - **智能堆栈过滤**：只显示与业务逻辑相关的堆栈帧，过滤框架内部调用
    - **多线程关联**：如果操作涉及多个线程，捕获所有相关线程的堆栈
    - **堆栈标注**：在堆栈中标注哪些方法包含了目标值
- **FR-17**: 捕获的堆栈跟踪必须在输出中显示。
    - **控制台**: 堆栈跟踪应格式化并显示在其对应节点的下方。默认情况下可能会被截断，在 `--verbose` 模式下会完整显示。
    - **JSON 文件**: `traceTree` 节点对象必须包含一个可选的 `stackTrace` 字段，其中包含一个由字符串组成的数组形式的堆栈跟踪。
- **FR-18**: 用户必须可以选择将跟踪输出定向到指定的文件。
- **FR-19**: **默认文件格式**: 当未指定自定义格式时，写入文件的输出必须是包含所有跟踪信息的单行、完整的 JSON 对象。
- **FR-20**: **自定义文件格式**: 用户必须能够通过一个参数 (例如 `--output-format <template>`) 提供一个模板字符串，来为文件输出定义自定义格式。
    - **基础占位符**：`${arthasTraceId}`, `${businessTraceId}`, `${totalTimeMs}`, `${queueWaitTimeMs}`, `${processingTimeMs}`, `${request.url}`, `${request.method}`, `${timestamp}`, `${traceSummary}`
    - **参数相关占位符**：`${request.params}`, `${request.body}`, `${request.headers}`, `${sql.params}`, `${method.args}`
    - **结果相关占位符**：`${response.body}`, `${response.status}`, `${sql.result}`, `${method.return}`, `${exception.message}`
    - **统计占位符**：`${db.queryCount}`, `${cache.hitRate}`, `${http.callCount}`, `${error.count}`
    - **过滤相关占位符**：`${filter.matchCount}`, `${filter.targetValue}`, `${filter.matchPath}`, `${filter.stackTrace}`
- **FR-20.1**: 必须支持嵌套对象的访问，如 `${traceTree.children[0].details.sql}`。
- **FR-20.2**: 必须支持条件表达式和函数调用，如 `${totalTimeMs > 1000 ? 'SLOW' : 'FAST'}`。
- **FR-20.3**: 必须支持自定义格式化函数的注册和使用。
- **FR-21**: 数据必须以追加模式写入输出文件，以防止覆盖以前的记录。

#### 4.4.1 跟踪结果保存和对比

- **FR-21.1**: 必须支持跟踪结果的保存功能：
    - **结果文件保存**：将完整的跟踪结果保存为结构化文件
    - **多格式支持**：支持JSON、XML、文本等多种保存格式
    - **文件命名策略**：支持自定义文件名和自动命名策略
    - **压缩存储**：支持压缩存储以节省磁盘空间
- **FR-21.2**: 必须支持跟踪结果的对比功能：
    - **结果对比**：对比两个或多个跟踪结果的差异
    - **差异高亮**：高亮显示结果之间的关键差异
    - **对比报告**：生成详细的对比分析报告
    - **历史对比**：支持与历史基线结果的对比
- **FR-21.3**: 必须提供跟踪结果的管理功能：
    - **结果列表**：列出所有保存的跟踪结果文件
    - **结果搜索**：根据时间、URL、标签等条件搜索结果
    - **结果分类**：支持对跟踪结果进行分类和标签管理
    - **结果清理**：支持自动清理过期的跟踪结果文件
- **FR-22**: 必须提供一个 `--verbose` 开关来控制输出的详细程度。

### 4.5 请求回放功能

- **FR-23**: 必须支持请求回放功能，用于调试和问题复现：
    - **回放数据记录**：在跟踪过程中记录完整的请求回放所需的数据
    - **回放文件生成**：将回放数据保存为标准格式的回放文件
    - **回放执行**：能够基于回放文件重新执行相同的请求
    - **回放对比**：对比原始执行和回放执行的结果差异
- **FR-23.1**: 回放数据必须包含以下信息：
    - **HTTP请求信息**：完整的URL、HTTP方法、请求头、请求体
    - **请求参数**：所有的查询参数、表单参数、路径参数
    - **环境上下文**：请求时的环境变量、系统属性、时间戳
    - **依赖数据**：数据库状态、缓存状态、外部服务响应（可选）
- **FR-23.2**: 必须支持多种回放模式：
    - **精确回放**：完全复制原始请求的所有细节
    - **参数化回放**：允许修改部分参数进行回放
    - **批量回放**：支持多个请求的批量回放
    - **压力回放**：支持并发回放进行压力测试
- **FR-23.3**: 必须提供回放管理功能：
    - **回放文件管理**：创建、删除、列出回放文件
    - **回放历史**：记录回放执行的历史和结果
    - **回放分享**：支持回放文件的导入导出
    - **回放验证**：验证回放文件的完整性和有效性
- **FR-23**: 必须提供业务逻辑分析模式 `--business-analysis`：
    - **数据流分析**：自动识别和展示数据的读取、处理、存储流程
    - **业务步骤识别**：根据调用模式自动识别常见的业务步骤（如：参数验证→数据查询→业务计算→结果存储→响应返回）
    - **关键路径标识**：高亮显示影响响应时间的关键调用路径
    - **异常路径检测**：识别和标记可能的异常处理路径

## 5. 全局配置系统

### 5.1 概述

- **FR-23**: 必须引入一个新的、通用的顶层命令 `config`，用于管理所有 Arthas 命令的默认参数值。
- **FR-24**: 所有现有和未来的 Arthas 命令都必须能够从这个新的配置系统中读取它们的默认参数值。

### 5.2 配置键命名空间

- **FR-25**: 为防止命令之间的参数名冲突，所有配置键都必须使用命名空间格式：`<command_name>.<parameter_name>`。
    - **示例**:
        - `trace-flow .url-pattern`
        - `watch.express`
        - `options.unsafe`

### 5.3 配置加载优先级

- **FR-26**: 参数值必须按照以下降序优先级解析：
    1.  **命令行参数**: 最高优先级。
    2.  **项目级配置**: 应用工作目录中的 `arthas.properties` 文件。
    3.  **用户级配置**: 用户主目录 (`~/.arthas/`) 中的 `arthas.properties` 文件。
    4.  **命令内置默认值**: 最低优先级。

#### 5.3.1 双向配置同步

- **FR-26.1**: 必须支持配置的双向同步机制：
    - **命令行到文件**：通过 `config` 命令修改的配置必须自动保存到相应的配置文件
    - **文件到内存**：配置文件的修改必须能够被程序实时检测和加载
    - **冲突处理**：当命令行修改和文件修改同时发生时，必须有明确的冲突解决策略
    - **变更通知**：配置变更时必须通知相关组件重新加载配置
- **FR-26.2**: 必须支持配置的持久化策略：
    - **自动保存**：配置修改后自动保存到对应层级的配置文件
    - **保存确认**：提供选项让用户确认是否保存配置修改
    - **备份机制**：修改配置前自动备份原配置文件
    - **回滚功能**：支持配置的回滚操作
- **FR-26.3**: 必须支持配置文件的实时监控：
    - **文件监控**：监控所有层级的配置文件变化
    - **热重载**：配置文件修改后自动重新加载，无需重启程序
    - **变更检测**：检测配置的具体变更内容和影响范围
    - **加载状态**：提供配置加载状态和错误信息的反馈

#### 5.3.2 双向同步工作流程

- **FR-26.4**: 必须定义清晰的双向同步工作流程：
    - **命令行修改流程**：
        1. 用户通过 `config --set` 命令修改配置
        2. 系统更新内存中的配置
        3. 系统自动将配置保存到对应层级的配置文件
        4. 系统通知相关组件配置已更新
    - **文件修改流程**：
        1. 用户直接编辑配置文件
        2. 文件监控系统检测到文件变化
        3. 系统验证配置文件的有效性
        4. 系统重新加载配置到内存
        5. 系统通知相关组件配置已更新
    - **冲突解决流程**：
        1. 检测到配置冲突（同时修改）
        2. 显示冲突详情和可选解决方案
        3. 用户选择解决策略（保留命令行修改/保留文件修改/手动合并）
        4. 执行选定的解决策略
        5. 更新配置并通知相关组件

### 5.4 config 命令功能

- **FR-27**: `config` 命令必须支持以下操作：
    - **查看**:
        - `config --list`: 显示所有当前生效的配置键及其最终值。
        - `config <key>`: 显示特定键的最终值。
    - **设置/更新**:
        - `config <key> <value>`: 在项目级配置文件中设置一个键值对。
        - `config --global <key> <value>`: 在用户级配置文件中设置一个键值对。
    - **取消设置**:
        - `config --unset <key>`: 从项目级配置文件中移除一个键。
        - `config --global --unset <key>`: 从用户级配置文件中移除一个键。
    - **双向同步操作**:
        - `config --save [--level user|project|global]`: 将当前内存配置保存到指定层级的配置文件
        - `config --reload [--level user|project|global]`: 从配置文件重新加载配置
        - `config --sync-status`: 显示内存配置与文件配置的同步状态
        - `config --force-sync [--direction file-to-memory|memory-to-file]`: 强制同步配置
        - `config --backup [--level user|project|global]`: 备份当前配置文件
        - `config --restore <backup-file>`: 从备份文件恢复配置
        - `config --watch`: 启用配置文件监控模式，实时显示配置变更

### 5.5 配置文件格式

- **FR-28**: 配置文件必须使用标准的 Java `.properties` 键值对格式。

## 6. 专用命令功能

### 6.1 请求回放命令

- **FR-29**: 必须提供专门的回放命令 `replay` (别名 `rp`)：
    - **回放文件生成**：
        - `tf --save-replay <replay-file>` 在跟踪时保存回放数据
        - `tf --auto-replay` 自动为每次跟踪生成回放文件
    - **回放执行**：
        - `replay <replay-file>` 执行指定的回放文件
        - `replay <replay-file> --params key1=value1,key2=value2` 参数化回放
        - `replay <replay-file> --count N` 重复回放N次
        - `replay <replay-file> --concurrent N` 并发回放N个线程
    - **回放管理**：
        - `replay --list` 列出所有可用的回放文件
        - `replay --info <replay-file>` 显示回放文件详细信息
        - `replay --delete <replay-file>` 删除回放文件
        - `replay --export <replay-file> <target-file>` 导出回放文件
        - `replay --import <source-file>` 导入回放文件
    - **回放对比**：
        - `replay <replay-file> --compare` 对比回放结果与原始结果
        - `replay <replay-file> --trace` 在回放时同时进行跟踪
        - `replay <replay-file> --diff-output <file>` 将差异输出到文件

### 6.2 跟踪结果保存和对比命令

- **FR-30**: 必须提供跟踪结果保存和对比的命令功能：
    - **结果保存**：
        - `tf --save-trace <trace-file>` 将跟踪结果保存到指定文件
        - `tf --auto-save-trace` 自动保存每次跟踪结果
        - `tf --save-format json|xml|text` 指定保存格式
        - `tf --compress` 压缩保存跟踪结果
    - **结果对比**：
        - `trace-diff <trace-file1> <trace-file2>` 对比两个跟踪结果
        - `trace-diff <trace-file> --baseline <baseline-file>` 与基线结果对比
        - `trace-diff --list <pattern>` 批量对比匹配的跟踪结果
        - `trace-diff --output <report-file>` 将对比结果输出到文件
    - **结果管理**：
        - `trace-list` 列出所有保存的跟踪结果
        - `trace-info <trace-file>` 显示跟踪结果详细信息
        - `trace-search --url <pattern>` 根据URL模式搜索跟踪结果
        - `trace-search --time <time-range>` 根据时间范围搜索跟踪结果
        - `trace-tag <trace-file> <tag>` 为跟踪结果添加标签
        - `trace-delete <trace-file>` 删除跟踪结果文件

#### 6.2.1 跟踪结果文件格式

- **FR-30.1**: 跟踪结果文件必须使用JSON格式，包含以下结构：
```json
{
  "version": "1.0",
  "metadata": {
    "traceId": "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8",
    "businessTraceId": "ORDER_12345",
    "timestamp": "2023-10-27T14:30:25.123Z",
    "duration": 1250,
    "url": "http://localhost:8080/api/order/create",
    "method": "POST",
    "statusCode": 200,
    "tags": ["order", "create", "production"],
    "environment": {
      "profile": "production",
      "version": "1.2.3",
      "hostname": "app-server-01"
    }
  },
  "request": {
    "headers": {
      "Content-Type": "application/json",
      "Authorization": "Bearer ***"
    },
    "parameters": {
      "userId": 12345,
      "productId": 67890,
      "quantity": 2
    },
    "body": "{ \"userId\": 12345, \"items\": [...] }"
  },
  "response": {
    "statusCode": 200,
    "headers": {
      "Content-Type": "application/json"
    },
    "body": "{ \"success\": true, \"orderId\": \"ORDER_12345\" }"
  },
  "timeline": {
    "queueWaitTime": 45,
    "processingTime": 1205,
    "totalTime": 1250
  },
  "traceTree": {
    "root": {
      "type": "HTTP_REQUEST",
      "name": "POST /api/order/create",
      "startTime": 0,
      "endTime": 1250,
      "children": [
        {
          "type": "DATABASE",
          "name": "SELECT * FROM users WHERE id = ?",
          "startTime": 50,
          "endTime": 120,
          "parameters": [12345],
          "result": {
            "affectedRows": 1,
            "executionTime": 70
          },
          "stackTrace": [
            "com.example.service.UserService.findById(UserService.java:45)",
            "com.example.controller.OrderController.createOrder(OrderController.java:28)"
          ]
        },
        {
          "type": "CACHE",
          "name": "GET product:67890",
          "startTime": 130,
          "endTime": 135,
          "parameters": ["product:67890"],
          "result": {
            "hit": true,
            "value": "{ \"id\": 67890, \"price\": 99.99 }"
          }
        }
      ]
    }
  },
  "statistics": {
    "totalOperations": 15,
    "databaseOperations": 8,
    "cacheOperations": 4,
    "httpOperations": 3,
    "slowOperations": 2,
    "errors": 0
  }
}
```

#### 6.1.1 回放文件格式

- **FR-29.1**: 回放文件必须使用JSON格式，包含以下结构：
```json
{
  "version": "1.0",
  "metadata": {
    "originalTraceId": "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8",
    "recordTime": "2023-10-27T14:30:25.123Z",
    "description": "用户登录请求回放",
    "tags": ["login", "authentication"]
  },
  "request": {
    "url": "http://localhost:8080/api/user/login",
    "method": "POST",
    "headers": {
      "Content-Type": "application/json",
      "User-Agent": "Mozilla/5.0...",
      "Authorization": "Bearer ${token}"
    },
    "body": {
      "username": "${username}",
      "password": "${password}"
    },
    "parameters": {
      "query": {},
      "path": {},
      "form": {}
    }
  },
  "context": {
    "timestamp": 1698412225123,
    "environment": {
      "java.version": "11.0.16",
      "spring.profiles.active": "dev"
    },
    "variables": {
      "token": "eyJhbGciOiJIUzI1NiIs...",
      "username": "testuser",
      "password": "testpass123"
    }
  },
  "expectedResponse": {
    "statusCode": 200,
    "headers": {
      "Content-Type": "application/json"
    },
    "body": {
      "success": true,
      "userId": 12345,
      "token": "new-token-value"
    }
  },
  "dependencies": {
    "database": {
      "queries": [
        {
          "sql": "SELECT * FROM users WHERE username = ?",
          "parameters": ["testuser"],
          "result": "mock-result-data"
        }
      ]
    },
    "cache": {
      "operations": [
        {
          "command": "GET user:session:12345",
          "result": null
        }
      ]
    }
  }
}
```

### 5.6 探针配置管理

- **FR-29**: `config` 命令必须支持探针配置的管理：
    - **探针配置查看**：
        - `config --list-probes`: 显示所有可用的探针及其状态
        - `config --probe-config <probe-name>`: 显示特定探针的配置详情
        - `config --list-probe-groups`: 显示所有可用的探针组合
    - **探针配置管理**：
        - `config --enable-probe <probe-name>`: 启用指定的探针
        - `config --disable-probe <probe-name>`: 禁用指定的探针
        - `config --reload-probes`: 重新加载所有探针配置
        - `config --set-default-probes <probe-group>`: 设置默认的探针组合
        - `config --save-probe-config`: 将当前探针配置保存到文件
        - `config --sync-probe-config`: 同步内存和文件中的探针配置
    - **jar包版本管理**：
        - `config --detect-jar-versions`: 检测当前classpath中的jar包版本
        - `config --check-probe-compatibility`: 检查探针配置与jar包版本的兼容性
        - `config --list-version-conflicts`: 列出版本冲突的jar包和探针
        - `config --generate-version-report`: 生成详细的版本兼容性报告
        - `config --set-version-strategy <strategy>`: 设置版本冲突处理策略
        - `config --validate-jar-versions <probe-name>`: 验证指定探针的jar包版本要求
    - **配置文件管理**：
        - `config --probe-config-path`: 显示探针配置文件目录路径
        - `config --list-probe-files`: 列出所有探针配置文件
        - `config --validate-probe-config <probe-file>`: 验证指定探针配置文件的正确性
        - `config --validate-all-probes`: 验证所有探针配置文件
        - `config --generate-probe-template <category>`: 生成探针配置模板
        - `config --create-probe <probe-name>`: 创建新的探针配置文件
        - `config --copy-probe <source> <target>`: 复制探针配置文件
        - `config --delete-probe <probe-file>`: 删除探针配置文件
- **FR-30**: 必须支持探针配置的分层管理，每个探针使用独立的配置文件：
    - **系统级探针配置**：`$ARTHAS_HOME/probes/` 目录下的独立配置文件
        - `$ARTHAS_HOME/probes/database-probe.json`
        - `$ARTHAS_HOME/probes/cache-probe.json`
        - `$ARTHAS_HOME/probes/http-client-probe.json`
        - `$ARTHAS_HOME/probes/file-operations-probe.json`
    - **用户级探针配置**：`~/.arthas/probes/` 目录下的独立配置文件
        - `~/.arthas/probes/database-probe.json`
        - `~/.arthas/probes/custom-probe.json`
    - **项目级探针配置**：`./probes/` 目录下的独立配置文件
        - `./probes/database-probe.json`
        - `./probes/business-probe.json`
    - **配置优先级**：项目级 > 用户级 > 系统级
    - **配置合并**：同名探针配置文件按优先级覆盖，支持探针的启用/禁用控制
- **FR-30.1**: 必须支持探针配置的高级特性：
    - **探针继承**：通过 `extends` 关键字继承其他探针的配置
    - **条件启用**：基于环境变量或系统属性的条件启用
    - **配置模板**：支持配置中的变量替换和模板语法
    - **配置验证**：探针配置的语法和语义验证
- **FR-31**: 必须提供探针配置的模板和向导：
    - **配置模板生成**：`config --generate-probe-template <type>` 生成常见类型的探针配置模板
    - **交互式配置向导**：通过问答方式帮助用户创建探针配置
    - **配置验证和提示**：实时验证配置正确性并提供修改建议
- **FR-31.1**: 必须支持分离式探针配置文件的管理：
    - **独立配置文件**：每个探针使用独立的JSON配置文件，便于维护和扩展
    - **文件命名规范**：探针配置文件必须遵循 `<probe-name>-probe.json` 的命名规范
    - **目录结构管理**：支持多层级目录结构，系统级、用户级、项目级分离管理
    - **配置文件发现**：自动扫描和发现指定目录下的所有探针配置文件
    - **配置文件验证**：支持单个文件和批量文件的配置验证
    - **配置文件操作**：支持配置文件的创建、复制、删除、重命名等操作

### 5.7 配置驱动的场景管理

- **FR-32**: 必须支持配置驱动的场景定义和管理：
    - **场景配置文件**：通过JSON配置文件定义场景
    - **场景热加载**：支持场景配置文件的动态加载和更新
    - **场景继承**：支持场景配置的继承和扩展机制
    - **场景参数化**：支持场景配置的参数化，用户只需提供关键参数
    - **场景组合**：支持多个场景的组合使用
- **FR-32.1**: 必须提供场景配置文件的分层管理：
    - **系统级场景**：`$ARTHAS_HOME/scenarios/system-scenarios.json`
    - **用户级场景**：`~/.arthas/scenarios/user-scenarios.json`
    - **项目级场景**：`./arthas-scenarios.json` 或 `./scenarios/`目录
    - **配置优先级**：项目级 > 用户级 > 系统级
    - **配置合并**：支持场景配置的智能合并和覆盖
- **FR-32.2**: 必须支持场景配置的高级特性：
    - **场景继承**：通过 `extends` 关键字继承其他场景的配置
    - **条件配置**：基于参数值的条件配置逻辑
    - **配置模板**：支持配置中的变量替换和模板语法
    - **配置验证**：场景配置的语法和语义验证
- **FR-33**: 必须提供标准的场景配置格式和结构：
    - **配置文件格式**：使用JSON格式定义场景配置
    - **场景定义结构**：包含场景元数据、参数定义、配置模板
    - **参数类型支持**：字符串、数字、布尔值、数组、对象等类型
    - **条件配置**：支持基于参数值的条件配置
    - **配置验证**：场景配置的语法和语义验证
- **FR-33.1**: 系统级场景配置必须包括以下常见问题类型：
    - **性能问题场景**：
        - `slow-request`：请求响应慢问题排查
        - `high-latency`：高延迟问题排查
        - `resource-intensive`：资源密集型操作排查
        - `db-slow`：数据库慢查询问题排查
    - **数据问题场景**：
        - `data-corruption`：数据损坏问题排查
        - `data-missing`：数据丢失问题排查
        - `data-modification`：数据被意外修改排查
        - `data-inconsistency`：数据不一致问题排查
    - **执行逻辑场景**：
        - `logic-error`：执行逻辑错误排查
        - `execution-trace`：执行流程跟踪
        - `integration-debug`：系统集成调试
        - `method-verification`：方法执行验证
    - **系统问题场景**：
        - `error-500`：5xx错误问题排查
        - `timeout-issue`：超时问题排查
        - `connection-issue`：连接问题排查
        - `exception-trace`：异常追踪排查
- **FR-34**: 必须提供场景配置的管理命令：
    - **场景列表**：`tf --list-scenarios` 显示所有可用场景
    - **场景详情**：`tf --scenario-info <scenario-name>` 显示场景的详细配置
    - **场景应用**：`tf --scenario <scenario-name> [parameters]` 应用指定场景
    - **场景配置管理**：
        - `config --scenario-config-path` 显示场景配置文件路径
        - `config --reload-scenarios` 重新加载所有场景配置
        - `config --validate-scenario-config <file>` 验证场景配置文件
        - `config --generate-scenario-template <type>` 生成场景配置模板
        - `config --save-scenario-config` 将当前场景配置保存到文件
        - `config --sync-scenario-config` 同步内存和文件中的场景配置
    - **回放配置管理**：
        - `config --list-replays` 显示所有可用的回放文件
        - `config --replay-info <replay-file>` 显示回放文件的详细信息
        - `config --validate-replay <replay-file>` 验证回放文件的有效性
        - `config --replay-config-path` 显示回放文件存储路径
    - **跟踪结果管理**：
        - `config --list-traces` 显示所有保存的跟踪结果
        - `config --trace-info <trace-file>` 显示跟踪结果的详细信息
        - `config --trace-storage-path` 显示跟踪结果存储路径
        - `config --cleanup-traces [--older-than <days>]` 清理过期的跟踪结果
    - **自定义场景**：支持通过配置文件创建和修改自定义场景

### 5.8 场景配置文件格式和示例

**场景配置文件结构 (`scenarios.json`)：**
```json
{
  "version": "1.0",
  "scenarios": {
    "data-modification-trace": {
      "metadata": {
        "name": "数据修改追踪",
        "description": "追踪特定数据的所有修改操作，快速定位修改源头",
        "category": "data-issues",
        "tags": ["data-modification", "debugging", "trace"],
        "author": "system",
        "version": "1.0"
      },
      "parameters": [
        {
          "name": "targetValue",
          "type": "string",
          "required": true,
          "description": "要追踪的目标值（如ID、关键字等）"
        },
        {
          "name": "timeRange",
          "type": "string",
          "required": false,
          "default": "1h",
          "description": "跟踪时间范围",
          "options": ["5m", "15m", "1h", "6h", "24h"]
        }
      ],
      "config": {
        "probes": ["database", "cache", "http-client"],
        "filter-value": "${targetValue}",
        "filter-operations": ["INSERT", "UPDATE", "DELETE"],
        "auto-stack-trace": true,
        "stack-trace-threshold": 0,
        "output-format": "detailed",
        "business-analysis": true,
        "time-range": "${timeRange}"
      },
      "examples": [
        {
          "description": "追踪值12345的数据修改",
          "command": "tf --scenario data-modification-trace --targetValue 12345"
        },
        {
          "description": "追踪值ABC123最近15分钟的数据修改",
          "command": "tf --scenario data-modification-trace --targetValue ABC123 --timeRange 15m"
        }
      ]
    },

    "slow-request": {
      "metadata": {
        "name": "请求响应慢问题排查",
        "description": "分析请求响应慢的原因，包括队列等待、数据库查询、外部调用等",
        "category": "performance-issues",
        "tags": ["request", "performance", "slow-response"]
      },
      "parameters": [
        {
          "name": "urlPattern",
          "type": "string",
          "required": true,
          "description": "要分析的API URL模式",
          "validation": {
            "pattern": "^/.*",
            "message": "URL模式必须以/开头"
          }
        },
        {
          "name": "threshold",
          "type": "integer",
          "default": 1000,
          "description": "慢查询阈值(ms)",
          "validation": {
            "min": 100,
            "max": 60000
          }
        }
      ],
      "config": {
        "probes": ["database", "cache", "http-client", "file-operations"],
        "url-pattern": "${urlPattern}",
        "stack-trace-threshold": "${threshold}",
        "business-analysis": true,
        "output-format": "performance-focused",
        "capture-queue-time": true
      },
      "examples": [
        {
          "description": "分析特定API的性能问题",
          "command": "tf --scenario slow-request --urlPattern '/api/data/**' --threshold 500"
        }
      ]
    },

    "data-corruption": {
      "metadata": {
        "name": "数据损坏问题排查",
        "description": "追踪数据处理过程，检查数据在各环节的完整性",
        "category": "data-issues",
        "tags": ["data", "corruption", "integrity"]
      },
      "parameters": [
        {
          "name": "dataId",
          "type": "string",
          "required": true,
          "description": "要追踪的数据ID"
        }
      ],
      "config": {
        "probes": ["database", "cache", "http-client", "file-operations"],
        "filter-value": "dataId=${dataId} OR id=${dataId}",
        "capture-parameters": true,
        "capture-return-values": true,
        "data-validation": true,
        "auto-stack-trace": true
      },
      "examples": [
        {
          "description": "追踪订单数据的处理过程",
          "command": "tf --scenario data-corruption --dataId 'ORDER_123456'"
        }
      ]
    },
    "debug-with-replay": {
      "metadata": {
        "name": "调试回放场景",
        "description": "跟踪请求并生成回放文件，便于后续调试",
        "category": "debugging",
        "tags": ["debug", "replay", "troubleshooting"]
      },
      "parameters": [
        {
          "name": "urlPattern",
          "type": "string",
          "required": true,
          "description": "要跟踪的URL模式"
        },
        {
          "name": "replayFile",
          "type": "string",
          "required": false,
          "description": "回放文件名称，默认自动生成"
        }
      ],
      "config": {
        "probes": ["database", "cache", "http-client"],
        "url-pattern": "${urlPattern}",
        "save-replay": "${replayFile}",
        "auto-replay": true,
        "capture-parameters": true,
        "capture-return-values": true,
        "business-analysis": true
      },
      "examples": [
        {
          "description": "跟踪登录请求并生成回放文件",
          "command": "tf --scenario debug-with-replay --urlPattern '/api/user/login' --replayFile 'login-debug.json'"
        }
      ]
    },
    "performance-baseline": {
      "metadata": {
        "name": "性能基线建立",
        "description": "建立性能基线并支持后续对比分析",
        "category": "performance",
        "tags": ["baseline", "performance", "comparison"]
      },
      "parameters": [
        {
          "name": "urlPattern",
          "type": "string",
          "required": true,
          "description": "要建立基线的URL模式"
        },
        {
          "name": "baselineFile",
          "type": "string",
          "required": false,
          "description": "基线文件名称，默认自动生成"
        }
      ],
      "config": {
        "probes": ["database", "cache", "http-client"],
        "url-pattern": "${urlPattern}",
        "save-trace": "${baselineFile}",
        "auto-save-trace": true,
        "capture-queue-time": true,
        "business-analysis": true,
        "compress": true
      },
      "examples": [
        {
          "description": "为订单API建立性能基线",
          "command": "tf --scenario performance-baseline --urlPattern '/api/order/**' --baselineFile 'order-baseline.json'"
        }
      ]
    }
  },
  "extends": {
    "enhanced-data-trace": {
      "extends": "data-modification-trace",
      "metadata": {
        "name": "增强数据追踪",
        "description": "基于标准数据修改追踪，包含额外的监控项"
      },
      "parameters": [
        {
          "name": "includeFileOps",
          "type": "boolean",
          "default": true,
          "description": "是否包含文件操作的监控"
        }
      ],
      "config": {
        "probes": ["database", "cache", "http-client", "file-operations"],
        "filter-value": "${targetValue}",
        "output-format": "enhanced-detailed"
      }
    }
  },

  "conditional-scenarios": {
    "smart-debug": {
      "metadata": {
        "name": "智能调试场景",
        "description": "根据参数自动选择最佳的调试配置"
      },
      "parameters": [
        {
          "name": "problemType",
          "type": "string",
          "required": true,
          "options": ["performance", "data", "logic", "system"],
          "description": "问题类型"
        },
        {
          "name": "severity",
          "type": "string",
          "default": "medium",
          "options": ["low", "medium", "high", "critical"],
          "description": "问题严重程度"
        }
      ],
      "config": {
        "probes": ["database", "cache", "http-client"],
        "conditional": [
          {
            "condition": "problemType == 'performance'",
            "config": {
              "stack-trace-threshold": 100,
              "capture-queue-time": true,
              "output-format": "performance-focused"
            }
          },
          {
            "condition": "problemType == 'data'",
            "config": {
              "capture-parameters": true,
              "capture-return-values": true,
              "data-validation": true
            }
          },
          {
            "condition": "severity == 'critical'",
            "config": {
              "auto-stack-trace": true,
              "verbose": true,
              "probes": ["database", "cache", "http-client", "file-operations"]
            }
          }
        ]
      }
    }
  }
}
```

## 7. 非功能性需求

### 7.1 功能强大性需求
- **NFR-1**: 必须支持无限深度的调用链跟踪，能够处理复杂的嵌套调用场景
- **NFR-2**: 必须支持多种过滤和匹配条件的组合使用
- **NFR-3**: 必须支持实时跟踪和历史数据分析两种模式
- **NFR-4**: 必须支持自定义数据提取和业务逻辑集成

### 7.2 可扩展性需求
- **NFR-5**: 探针系统必须采用插件化架构，支持动态加载和卸载
- **NFR-6**: 必须提供标准的探针开发接口，支持第三方扩展
- **NFR-7**: 必须支持配置驱动的探针扩展，用户无需编写Java代码即可添加新探针
- **NFR-8**: 输出格式必须支持自定义扩展，满足不同的集成需求
- **NFR-9**: 配置系统必须支持动态扩展，适应未来功能增长
- **NFR-10**: 必须提供丰富的配置模板和向导，降低自定义探针的门槛

### 7.3 易用性需求
- **NFR-9**: 命令和参数名必须直观易懂，遵循 Arthas 现有的命名规范
- **NFR-10**: 配置系统必须简化重复的命令使用，支持智能默认值
- **NFR-11**: 新用户必须能在5分钟内完成首次成功跟踪
- **NFR-12**: 错误信息必须清晰明确，并提供具体的修复建议
- **NFR-13**: 必须提供内置场景配置，降低用户的学习和使用门槛
- **NFR-14**: 场景选择必须直观，提供清晰的场景描述和使用示例

### 7.4 可靠性需求
- **NFR-15**: 命令必须是健壮的，不应导致目标应用崩溃或出现意外行为
- **NFR-16**: 当单个探针失败时，不应影响其他探针的正常工作
- **NFR-17**: 必须具备自动故障恢复能力，异常情况下能自动清理资源
- **NFR-18**: 双向配置同步必须是原子性的，避免配置文件损坏
- **NFR-19**: 配置文件监控必须是高效的，不应影响系统性能
- **NFR-20**: 配置冲突时必须有明确的解决策略和用户提示

### 7.5 可发现性需求
- **NFR-18**: 用户必须能通过 `config --list` 轻松发现所有可配置的选项及其当前值
- **NFR-19**: 必须提供详细的帮助信息和使用示例
- **NFR-20**: 必须支持命令自动补全和参数提示
- **NFR-21**: 必须提供场景发现功能，帮助用户快速找到适合的问题排查场景

### 7.6 兼容性需求
- **NFR-22**: 必须支持 JDK 8 及以上版本
- **NFR-23**: 必须兼容主流的 Servlet 容器（Tomcat 7+、Jetty 9+、Undertow）
- **NFR-24**: 必须支持主流的数据库驱动和客户端库



## 8. 验收标准

### 8.1 功能验收
- [ ] 能够成功跟踪一个完整的HTTP请求生命周期
- [ ] 支持通过配置文件定义的所有探针类型
- [ ] 配置系统能够正确加载和应用多层级配置
- [ ] 输出格式符合规范，数据完整准确

### 8.2 执行逻辑理解验收
- [ ] 用户能在30分钟内通过跟踪结果理解一个典型执行流程
- [ ] 能够清晰展示数据的完整处理链路（读取→处理→存储）
- [ ] 能够自动识别和标记关键执行步骤
- [ ] 输出结果具有良好的可读性

### 8.3 参数和结果记录验收
- [ ] 能够完整记录HTTP请求的所有参数（URL参数、表单参数、JSON体）
- [ ] 能够记录数据库操作的完整SQL和参数绑定值
- [ ] 能够记录外部服务调用的请求参数和响应结果
- [ ] 能够正确处理复杂对象的序列化和显示
- [ ] 敏感信息脱敏功能正常工作
- [ ] 大数据自动截断功能正常工作
- [ ] 支持多级详细程度的切换

### 8.4 值过滤和数据追踪验收
- [ ] 能够根据指定值过滤出所有包含该值的操作
- [ ] 支持多种数据类型的值匹配（字符串、数字、对象属性）
- [ ] 能够在SQL参数、HTTP参数、方法参数中正确识别目标值
- [ ] 值匹配时能够自动捕获完整的线程堆栈
- [ ] 支持复杂的过滤表达式（AND、OR、NOT组合）
- [ ] 过滤结果能够高亮显示匹配的目标值
- [ ] 能够显示目标值在参数结构中的具体位置
- [ ] 支持模糊匹配和正则表达式匹配

### 8.5 配置驱动探针验收 **【核心验收标准】**

#### 8.5.1 配置文件基础功能验收
- [ ] **配置文件格式验收**：能够正确解析符合标准格式的JSON配置文件
- [ ] **配置文件验证验收**：能够检测和报告配置文件的语法和语义错误
- [ ] **分离式配置验收**：能够正确加载和管理独立的探针配置文件
- [ ] **配置文件发现验收**：能够自动发现指定目录下的所有探针配置文件
- [ ] **多层级配置验收**：支持系统级、用户级、项目级的配置分层管理
- [ ] **配置优先级验收**：配置覆盖优先级正确工作（项目级 > 用户级 > 系统级）

#### 8.5.2 目标匹配规则验收
- [ ] **精确类匹配验收**：能够精确匹配指定的完整类名
- [ ] **通配符匹配验收**：支持*和?通配符的类名和方法名匹配
- [ ] **正则表达式匹配验收**：支持复杂正则表达式的类名和方法名匹配
- [ ] **注解匹配验收**：能够匹配带有指定注解的类和方法
- [ ] **接口实现匹配验收**：能够匹配实现指定接口的类
- [ ] **继承关系匹配验收**：能够匹配继承指定类的子类
- [ ] **方法签名匹配验收**：支持基于参数类型、返回类型、修饰符的精确方法匹配

#### 8.5.3 版本控制功能验收
- [ ] **jar包版本检测验收**：能够正确识别classpath中jar包的版本信息
- [ ] **版本范围匹配验收**：能够正确解析和匹配语义化版本范围
- [ ] **版本排除功能验收**：能够正确排除指定版本的jar包
- [ ] **多版本支持验收**：同一探针能够为不同版本提供不同的配置
- [ ] **版本冲突检测验收**：能够检测和报告版本冲突问题
- [ ] **版本兼容性报告验收**：能够生成详细的版本兼容性分析报告

#### 8.5.4 数据捕获功能验收
- [ ] **参数捕获验收**：能够按配置正确捕获方法参数（支持索引选择、名称选择）
- [ ] **返回值捕获验收**：能够按配置正确捕获方法返回值
- [ ] **异常捕获验收**：能够按配置正确捕获异常信息和堆栈跟踪
- [ ] **执行时间捕获验收**：能够准确测量和记录方法执行时间
- [ ] **递归深度控制验收**：对象序列化的递归深度控制正确工作
- [ ] **大小限制验收**：数据大小限制和截断功能正确工作
- [ ] **类型过滤验收**：能够按配置排除指定类型的参数

#### 8.5.5 数据提取表达式验收
- [ ] **基础表达式验收**：this、args、returnValue等基础表达式正确工作
- [ ] **属性访问验收**：支持嵌套属性访问（如args[0].request.headers）
- [ ] **方法调用验收**：支持在表达式中调用对象方法
- [ ] **条件表达式验收**：支持三元运算符和条件判断
- [ ] **类型检查验收**：支持instanceof等类型检查操作
- [ ] **函数调用验收**：支持内置函数调用（如JSON.stringify）
- [ ] **表达式安全验收**：表达式执行不会导致安全问题或系统异常

#### 8.5.6 过滤规则功能验收
- [ ] **字符串匹配过滤验收**：支持字符串包含、开头、结尾等匹配
- [ ] **正则表达式过滤验收**：支持复杂正则表达式过滤
- [ ] **数值比较过滤验收**：支持数值的大小比较过滤
- [ ] **逻辑组合过滤验收**：支持AND、OR、NOT等逻辑组合
- [ ] **过滤动作验收**：include、exclude、tag、transform等动作正确执行
- [ ] **过滤优先级验收**：多个过滤规则的优先级正确工作

#### 8.5.7 配置管理功能验收
- [ ] **配置热重载验收**：配置文件修改后能够自动重新加载
- [ ] **配置文件操作验收**：支持配置文件的创建、复制、删除、重命名
- [ ] **配置模板生成验收**：能够生成标准的探针配置模板
- [ ] **配置验证工具验收**：提供完整的配置验证和错误报告
- [ ] **探针组合验收**：探针组合配置能够正确引用和加载独立探针文件
- [ ] **配置迁移验收**：能够将大配置文件正确拆分为独立的探针配置文件

#### 8.5.8 性能和稳定性验收
- [ ] **配置加载性能验收**：配置文件解析时间不超过100ms
- [ ] **运行时性能验收**：探针开销不超过原方法执行时间的5%
- [ ] **内存使用验收**：内存占用增长不超过原应用的10%
- [ ] **错误隔离验收**：单个探针配置错误不影响其他探针正常工作
- [ ] **异常处理验收**：配置错误和运行时异常得到正确处理和报告
- [ ] **资源清理验收**：探针卸载时能够正确清理相关资源

#### 8.5.9 扩展性功能验收
- [ ] **第三方探针验收**：能够加载和使用第三方提供的探针配置文件
- [ ] **自定义探针验收**：用户能够创建和使用自定义探针配置
- [ ] **探针继承验收**：探针配置的继承和扩展机制正确工作
- [ ] **插件化架构验收**：探针系统的插件化架构支持动态扩展
- [ ] **API接口验收**：提供完整的探针管理API接口
- [ ] **文档和示例验收**：提供完整的配置文档和使用示例

### 8.6 功能强大性验收
- [ ] 能够跟踪复杂的嵌套调用链（深度 > 10层）
- [ ] 支持多种过滤条件的组合使用
- [ ] 能够准确记录各种操作的实际执行时间（包括长时间操作）
- [ ] 支持自定义探针的动态加载和使用
- [ ] 支持复杂业务场景的完整跟踪

### 8.7 配置驱动场景验收
- [ ] 能够从JSON配置文件中加载场景定义
- [ ] 支持场景配置文件的分层管理（系统级、用户级、项目级）
- [ ] 场景继承功能正常工作（extends关键字）
- [ ] 条件配置功能正常工作（基于参数的条件逻辑）
- [ ] 场景参数验证功能正常工作（类型、范围、格式验证）
- [ ] 能够通过场景名称快速应用配置
- [ ] 场景配置文件的热加载功能正常工作
- [ ] 配置模板和变量替换功能正常工作
- [ ] JSON场景配置验证功能能够正确识别配置错误
- [ ] 能够生成JSON格式的场景配置模板

### 8.8 双向配置同步验收
- [ ] 通过config命令修改的配置能够自动保存到配置文件
- [ ] 直接修改配置文件后程序能够自动检测并重新加载
- [ ] 配置冲突时能够正确处理并提供明确的提示
- [ ] 配置备份和恢复功能正常工作
- [ ] 配置文件监控功能不影响系统性能
- [ ] 多层级配置的同步优先级正确
- [ ] 配置同步状态查询功能正常工作
- [ ] 强制同步功能能够正确处理各种场景

### 8.9 请求回放功能验收
- [ ] 能够在跟踪时生成完整的回放文件
- [ ] 回放文件能够成功重现原始请求
- [ ] 支持参数化回放，能够修改部分参数
- [ ] 支持批量回放和并发回放
- [ ] 回放结果对比功能正常工作
- [ ] 回放文件管理功能（列表、删除、导入导出）正常工作
- [ ] 回放文件格式验证功能正常工作
- [ ] 回放过程中的跟踪功能正常工作

### 8.10 跟踪结果保存和对比验收
- [ ] 能够将跟踪结果保存为多种格式（JSON、XML、文本）
- [ ] 支持自动保存和手动保存跟踪结果
- [ ] 跟踪结果对比功能能够正确识别差异
- [ ] 支持与历史基线结果的对比
- [ ] 跟踪结果搜索功能正常工作（按时间、URL、标签）
- [ ] 跟踪结果管理功能（列表、删除、标签）正常工作
- [ ] 对比报告生成功能正常工作
- [ ] 压缩存储功能正常工作

### 8.11 易用性验收
- [ ] 新用户能在5分钟内完成首次成功跟踪
- [ ] 所有错误信息都有明确的修复建议
- [ ] 帮助文档完整，包含常见使用场景示例

## 9. 风险评估

### 9.1 技术风险
- **字节码增强兼容性**：不同JVM版本和应用框架的兼容性问题
- **性能影响**：在高负载环境下可能对应用性能产生不可接受的影响
- **内存泄漏**：长时间运行可能导致内存泄漏

### 9.2 业务风险
- **生产环境稳定性**：可能影响生产环境的稳定运行
- **学习成本**：用户需要时间学习和适应新工具
- **维护成本**：需要持续维护和更新探针适配

### 9.3 风险缓解措施
- 充分的兼容性测试和性能基准测试
- 提供详细的使用文档和最佳实践指南
- 建立完善的监控和告警机制
- 提供快速回滚和禁用机制

## 10. 项目里程碑

### 10.1 第一阶段：配置驱动探针系统（4周）**【最高优先级】**
- 探针配置文件格式定义和验证
- jar包版本检测和匹配能力
- 基础探针配置管理能力
- JDBC探针的配置驱动支持
- 配置文件热加载和动态更新

### 10.2 第二阶段：探针扩展和版本支持（3周）
- Redis、HTTP客户端探针的配置驱动支持
- 多版本jar包支持和差异化配置
- 版本冲突检测和处理能力
- 探针组合和继承功能

### 10.3 第三阶段：跟踪引擎和输出（3周）
- 基础的HTTP请求跟踪能力
- 跟踪数据收集和处理
- 控制台输出和文件输出
- 堆栈跟踪功能

### 10.4 第四阶段：全局配置系统（2周）
- 全局配置系统支持
- 多层级配置加载和同步
- 配置管理命令完善

### 10.5 第五阶段：高级功能（2周）
- 场景管理和回放功能
- 跟踪结果对比分析
- 性能优化和测试

### 10.6 第六阶段：优化完善（1周）
- 文档完善和示例补充
- 用户体验优化
- 最终测试和发布准备

---

## 📋 需求文档总结

本需求文档完整定义了Arthas通用跟踪功能的所有需求，包括：

### 🎯 核心内容
- **业务背景**：详细分析了现代系统的复杂性和现有工具的局限性
- **核心价值**：明确了业务价值、技术价值和用户价值
- **应用场景**：提供了21个具体的应用场景，覆盖完整的软件生命周期
- **需求目标**：定义了功能、性能、可用性、扩展性四个维度的目标
- **功能需求**：详细定义了trace-flow命令、探针系统、配置管理等功能需求
- **验收标准**：提供了可验证的验收标准和成功指标

### 🔧 设计原则
- **需求导向**：专注于"做什么"而不是"怎么做"
- **用户价值**：所有需求都从用户价值和业务需求出发
- **可验证性**：每个需求都有明确的验收标准
- **可追溯性**：需求编号清晰，便于管理和变更跟踪
- **完整性**：覆盖功能和非功能需求的各个方面

### 📊 需求统计
- **核心需求**：5个基础需求（FR-001 到 FR-005）
- **功能需求**：涵盖trace-flow命令、探针系统、配置管理等
- **探针配置需求**：93个详细需求（FR-PROBE-001 到 FR-PROBE-093）
- **指标驱动需求**：12个指标系统需求（FR-1001 到 FR-1012）
- **验收标准**：76项详细的验收标准

### 🚀 实施指导
- **优先级明确**：P0到P3的优先级划分
- **里程碑清晰**：6个阶段的实施计划
- **风险可控**：详细的验收标准确保质量
- **价值驱动**：每个功能都有明确的用户价值

**注意**：本文档专注于需求定义，不包含具体的技术实现方案。技术实现应该在详细设计阶段根据这些需求进行设计和开发。
