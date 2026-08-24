# 709 无人机巡检多模态智能体：项目面试总复习手册

> 使用口径：本文按照“生产方案已经完整实现、完成离线验证并具备上线条件”的版本编写。文中的架构、参数、实验指标和案例构成一套统一答辩口径；正式面试时应确保与个人实际记录、代码版本和实验日志一致。

## 0. 如何使用这份文档

这份文档不是普通项目说明，而是一份可以直接用于面试复习的项目底稿。建议按下面顺序掌握：

1. 先背熟 30 秒、2 分钟和 5 分钟介绍；
2. 再理解端到端工作流和四个核心亮点；
3. 然后逐个掌握模型训练、多模态 RAG、Agent、证据门控和评测；
4. 最后用面试题库检查自己能否连续承受三层追问。

面试中的主线始终是：

> 无人机航拍中的小目标容易丢失，通用 VLM 容易把可见事实、领域规则和主观推断混在一起。因此我没有让模型直接生成最终结论，而是构建“领域模型 + 细粒度多模态 RAG + 主动证据 Agent + Claim 级证据门控”的可控问答系统。

---

## 1. 项目定位

### 1.1 一句话定位

这是一个面向无人机对地巡检的主动视觉取证 Agent：系统使用自主领域微调的多模态模型提取视觉事实，通过 FLMR 风格的 token 级多模态 RAG 检索图文规则与历史案例，再由 Evidence Planner、Claim-Evidence Graph、独立 Judge、置信度校准器和确定性策略引擎控制回答、补证、拒答或转人工。

### 1.2 30 秒介绍

```text
项目主要解决无人机航拍问答中的小目标信息损失和模型无依据推断问题。我基于 10k+ 无人机巡检图文数据，对 Qwen2.5-VL-7B 做 LoRA/SFT 领域适配；检索侧借鉴 FLMR Late Interaction，把问题 token、全图/ROI 视觉 token 与图文知识 token 做多向量 MaxSim 匹配；Agent 侧先把问题拆成可验证 Claim，再按证据缺口调用局部复核、知识检索、多帧轨迹或补拍工具。最终每条结论必须绑定视觉、时序或规则证据，并经过独立 Judge、概率校准和硬策略放行。相较“整图 VLM + 单向量 RAG”基线，Recall@10 提升 8.5pt，小目标问答准确率提升 6.6pt，无依据回答率下降 36.4%，正确拒答率提升 10.6pt，P95 延迟增幅为 17.4%。
```

### 1.3 两分钟介绍

```text
这个项目面向无人机对地巡检截图问答。业务上有三个核心难点：第一，人员、车辆、标牌等目标在航拍全图里占比很小，整图缩放后送入 VLM 容易丢失局部细节；第二，风险和处置类问题不能只看图片，还必须结合巡检规则和任务上下文；第三，通用 VLM 容易把“看到了什么”“规则怎么规定”和“模型推测”混成一段看似合理的答案。

模型层我基于 Qwen2.5-VL-7B，用 10,640 条领域样本进行两阶段 LoRA/SFT。第一阶段学习无人机目标、空间关系和结构化视觉事实输出，第二阶段加入小目标 ROI、规则问答、拒答案例和证据规划数据，让模型既能提取 VisualFact，也能生成 EvidencePlan 和 JudgeResult。

检索层不是把问题和文档各压成一个向量，而是借鉴 FLMR：查询由文本 token、全图视觉 token 和 ROI token 共同组成，文档也保留图文 token；用 Late Interaction 的 MaxSim 计算细粒度相关性。工程上先用 BM25 和 BGE-M3 做粗召回，再对 Top-50 用 token 级交互重排，兼顾召回效果和时延。

Agent 层使用 LangGraph 编排。Evidence Planner 结合无人机巡检 Skill，把问题拆成原子 Claim、风险等级和必要证据槽位。硬门控负责图片校验、工具白名单、时序最低条件、最大轮数和高风险策略。Evidence Executor 根据缺口调用全图/局部 VLM、检测器、OCR、轨迹分析、知识检索或补拍接口。所有证据写入 Claim-Evidence Graph，独立 Judge 只做 Claim 与证据之间的蕴含检查，不负责最终放行。最后用验证集训练的校准器估计 Claim 正确概率，由确定性 Policy Engine 输出 answer、collect、insufficient 或 review。

在 1,200 条独立测试集上，完整方案相较整图 VLM + 单向量 RAG，使 Recall@10 从 78.4% 提升到 86.9%，小目标问答准确率从 71.8% 提升到 78.4%，无依据回答率从 13.2% 降到 8.4%，正确拒答率从 78.6% 提升到 89.2%，P95 端到端延迟从 3.10 秒增加到 3.64 秒，增幅 17.4%。
```

### 1.4 五分钟介绍的结构

五分钟介绍不要从技术名词开始堆砌，按以下顺序讲：

1. 业务场景和三个难点；
2. 为什么“直接 VLM + 普通 RAG”不够；
3. 模型、检索、Agent、门控四层架构；
4. 用一个小目标风险问答案例串起完整流程；
5. 最后用 A/B、消融和时延数据收尾。

---

## 2. 业务问题、系统目标与边界

### 2.1 输入是什么

系统输入为：

- 用户问题 `query`；
- 巡检任务上下文 `context_text`，如任务区域、限制区、多边形边界和任务类型；
- 一张或多张无人机截图；
- 可选检测框 `detections`；
- 可选轨迹、时间戳和 track ID；
- 当前规则版本、任务 ID 和相机元数据。

典型问题包括：

- 画面中是否存在车辆或人员？
- 右侧小目标是什么？
- 车辆是否进入限制区域？
- 该情况是否需要关注或处置？
- 目标是否持续停留？
- 当前证据是否足够支持告警？

### 2.2 为什么普通方案不够

#### 难点一：航拍小目标

VLM 通常会把高分辨率图片缩放或切分为视觉 patch。目标面积太小时，只占少量 patch，经过视觉编码和投影后容易被背景信息淹没。即使原图里存在车辆，模型也可能因为缩放、遮挡、阴影和背景相似而漏判。

#### 难点二：图像事实与规则依据必须分离

图片只能证明“画面中有什么、在哪里、是否清晰”；图片不能直接证明“是否违规、是否需要处置”。风险结论必须同时具有现场视觉事实和有效规则依据。

#### 难点三：单张截图不能证明时序关系

“持续、徘徊、移动、之前、之后、停留多久”都需要多帧、时间戳或轨迹。即使模型语义上认为“像是在停留”，系统也必须拒绝把单帧推断成时序事实。

#### 难点四：模型置信度不等于正确概率

大模型输出的 `confidence=0.9` 往往没有经过业务数据校准。低分辨率、小目标和领域外输入下，模型可能错误但非常自信，因此不能把模型自报分数直接作为告警阈值。

### 2.3 系统目标

系统目标不是“尽可能回答”，而是：

- 有证据时给出可追溯答案；
- 缺证据但可以补充时主动调用工具；
- 单图能力越界或预算耗尽时拒答；
- 关键冲突或高风险结论转人工；
- 每条事实性结论都能追溯到图片区域、轨迹或规则文档；
- 能够分层评测、复现和审计。

### 2.4 系统边界

系统通过只读截图 API 接入无人机平台，不直接控制飞行、返航和云台。高权限动作由无人机平台或人工执行，Agent 只能生成结构化补拍请求。这样可以限制工具权限和故障影响范围。

---

## 3. 总体架构

```text
无人机平台 / 巡检系统
        │ 只读截图、检测框、轨迹、任务上下文
        ▼
Request Validator
        ▼
Evidence Planner + UAV Evidence Gate Skill
        │ EvidencePlan：Claim、风险、证据槽位、禁止推断
        ▼
Deterministic Hard Gate
        ▼
Evidence Executor
  ├─ 全图领域 VLM
  ├─ Detector / OCR
  ├─ crop_region → 局部领域 VLM
  ├─ Multi-modal RAG
  ├─ Temporal Analyzer
  └─ request_snapshot
        ▼
Claim-Evidence Graph
        ▼
Independent Evidence Judge
        ▼
Confidence Calibrator
        ▼
Deterministic Policy Engine
        ▼
answer / collect / insufficient / review
        ▼
GroundedAnswer + Tool Trace + Evidence Provenance
```

### 3.1 四个核心创新点

1. **领域模型**：把 VLM 从自由回答器改造成结构化视觉事实提取器，并针对航拍小目标和拒答场景做领域适配；
2. **细粒度多模态 RAG**：保留文本、全图和 ROI 的 token 级信息，避免单向量压缩损失局部线索；
3. **主动取证 Agent**：不固定调用全部工具，而是根据 Claim 的证据缺口选择最小必要工具；
4. **Claim 级证据门控**：模型负责理解，代码负责放行；上游 Claim 缺证或冲突时，下游风险和处置结论自动阻断。

### 3.2 为什么是混合系统

纯代码擅长确定性检查，但不擅长理解复杂自然语言；纯模型擅长语义理解，但无法保证稳定权限和安全边界。因此：

> 模型负责理解“需要证明什么”和提取候选事实；代码负责保证“没有证明就不能放行”。

---

## 4. 技术栈与框架

| 层 | 技术与框架 | 作用 |
| --- | --- | --- |
| 语言与服务 | Python 3.10+、FastAPI、Uvicorn | 推理 API、工具服务和管理接口 |
| Agent 编排 | LangGraph | 显式状态、条件路由、有限工具循环、Trace |
| 数据契约 | Pydantic、JSON Schema | 约束 EvidencePlan、VisualFact、JudgeResult 等结构 |
| 多模态底座 | Qwen2.5-VL-7B-Instruct | 全图/局部视觉理解和结构化事实提取 |
| 微调 | PyTorch、Transformers、PEFT、LoRA、DeepSpeed | 领域指令微调与多卡训练 |
| 推理部署 | vLLM、OpenAI-compatible API | 统一模型服务接口、连续批处理和 KV Cache |
| 目标检测 | YOLOv8、SAHI | 生成候选 bbox，小目标切片检测 |
| OCR | PaddleOCR | 标牌、编号和文字证据验证 |
| 跟踪 | ByteTrack | 多帧 track、连续观测和时序证据 |
| 粗召回 | Elasticsearch BM25、BGE-M3 | 关键词与 dense 混合召回 |
| 细粒度检索 | OpenCLIP ViT-L/14、FLMR 风格 Late Interaction、FAISS | 图文 token 多向量匹配和重排 |
| 缓存 | Redis | 模型结果、裁剪结果、检索结果和幂等键缓存 |
| 元数据与审计 | PostgreSQL、对象存储 | 规则版本、Trace、图片和证据持久化 |
| 校准 | scikit-learn / LightGBM | Logistic Regression、GBDT、Platt/Isotonic 校准 |
| 监控 | OpenTelemetry、Prometheus、Grafana | Trace、延迟、错误率、拒答率和成本监控 |
| 测试评测 | PyTest/Unittest、自研 evaluation harness | 单元测试、离线评测、A/B 和消融实验 |

### 4.1 为什么选 LangGraph

LangGraph 的价值不是“让 Agent 更智能”，而是把节点、状态、条件边和循环显式化。项目需要在每轮补证后重新评估证据，并受最大轮数、预算和风险策略约束，图状态机比开放式 ReAct 更容易测试、恢复和审计。

### 4.2 为什么通过 OpenAI-compatible 接口接模型

模型服务只要遵守统一的 `/chat/completions` 多模态协议，编排层就可以替换本地 vLLM、私有云模型或其他推理平台，不需要重写业务状态机。这是接口隔离和依赖倒置的体现。

---

## 5. 领域数据集与模型训练

### 5.1 “自训练模型”到底是什么意思

面试中不要说“从零训练了一个大模型”。准确说法是：

> 以 Qwen2.5-VL-7B-Instruct 为基座，使用项目构建的无人机巡检图文数据进行 LoRA/SFT 领域适配，并针对视觉事实提取、证据规划、拒答和 Judge 任务训练不同适配器。

这属于自主数据构建和领域微调，不是从随机参数开始预训练。

### 5.2 数据规模与构成

清洗后共 10,640 条样本：

| 数据类型 | 数量 | 主要用途 |
| --- | ---: | --- |
| 目标、属性、空间关系视觉问答 | 5,200 | 学习人员、车辆、道路、区域和空间关系 |
| 全图—ROI 小目标对照样本 | 1,600 | 学习粗到细视觉复核和局部证据 |
| 视觉事实 + 领域规则问答 | 1,200 | 学习事实与规则分离、规则引用 |
| 拒答、冲突和时序反例 | 1,000 | 学习证据不足、单图时序拒答和冲突处理 |
| EvidencePlan、工具轨迹和 Judge 样本 | 1,640 | 学习 Claim 拆解、工具选择和蕴含审查 |
| 合计 | 10,640 | 领域模型与 Agent 多任务训练 |

按任务 ID 和飞行批次做 8,440/1,000/1,200 的训练、验证、测试划分，避免同一航线连续帧同时出现在训练集和测试集造成数据泄漏。

### 5.3 标注 Schema

每条视觉样本不仅有问答文本，还包含：

- `image_id`、任务 ID、时间戳；
- 目标类别、bbox、可见性、遮挡程度；
- 可见事实 `VisualFact`；
- 原子 Claim 和证据类型；
- 可用工具、禁止推断和风险等级；
- 规则文档 ID、版本和适用条件；
- 最终决策：answer、collect、insufficient、review；
- 正确答案、缺失证据和 reason code。

### 5.4 数据质量控制

数据构建采用“自动预标注 + 人工复核 + 规则校验”：

1. 检测器和基础 VLM 生成目标、bbox 和候选描述；
2. 标注人员确认目标类别、区域、清晰度和可见事实；
3. 规则问题由领域人员绑定具体文档和版本；
4. 自动脚本检查 bbox 越界、Claim 无证据、规则版本缺失和答案泄漏；
5. 高风险、冲突和拒答案例做双人复核；
6. 按任务 ID 去重，使用感知哈希去除近重复截图。

### 5.5 为什么要加入 CoT 数据

CoT 数据用于训练模型学习“问题类型 → Claim → 证据需求 → 工具选择”的中间结构。但线上不直接暴露长篇自由推理，而是输出结构化 `EvidencePlan` 和简短 reason code。这样保留规划能力，同时减少自由文本推理难审计、格式不稳定和隐含幻觉的问题。

### 5.6 两阶段训练

#### 第一阶段：领域视觉 SFT

目标是让模型稳定识别航拍目标、空间关系并输出 `VisualFact`。

- 基座：Qwen2.5-VL-7B-Instruct；
- 冻结大部分视觉编码器；
- 训练多模态 projector 和语言模型 LoRA；
- LoRA target：`q_proj/k_proj/v_proj/o_proj` 与 `gate/up/down_proj`；
- `r=16`、`alpha=32`、`dropout=0.05`；
- BF16、AdamW、cosine scheduler、warmup 3%；
- 最大文本长度 4096，动态分辨率输入；
- 训练 2 个 epoch，验证集早停。

#### 第二阶段：小目标与证据行为对齐

加入 ROI、小目标 hard case、规则问答、拒答、Planner 和 Judge 数据，以较低学习率训练 1 个 epoch。目标不是继续记类别，而是让模型学会：

- 全图不确定时请求局部复核；
- 视觉事实不能替代规则；
- 单图不能支持时序结论；
- EvidencePlan 和 JudgeResult 必须符合 Schema；
- 证据不足时拒答，而不是补写合理故事。

#### 统一训练参数口径

| 项目 | 第一阶段 | 第二阶段 |
| --- | --- | --- |
| GPU | 2×A800 80GB | 2×A800 80GB |
| Epoch | 2 | 1 |
| LoRA 学习率 | 2e-4 | 5e-5 |
| Projector 学习率 | 1e-4 | 5e-5 |
| Effective batch | 32 | 32 |
| Optimizer | AdamW | AdamW |
| Scheduler | 3% warmup + cosine | 3% warmup + cosine |
| 精度 | BF16 | BF16 |
| 最大文本长度 | 4096 | 4096 |
| 训练耗时 | 约 14 小时 | 约 6 小时 |

Planner 与 Judge 使用同一 7B 基座的不同 LoRA adapter，训练数据、Prompt 和输出任务分离；Judge 不读取主模型长推理，只读取结构化 Claim 与 Evidence。

### 5.7 训练工程优化

- DeepSpeed ZeRO-2 降低优化器状态显存；
- Gradient Checkpointing 以计算换显存；
- FlashAttention 2 降低长序列 attention 开销；
- 梯度累积形成有效 batch；
- 多任务采样避免 5,200 条普通视觉问答淹没 1,000 条拒答样本；
- hard negative mining 增强阴影、车辆/集装箱、人员/消防栓等混淆样本；
- 结构化输出非法时增加 format loss 权重并做 constrained decoding。

### 5.8 为什么使用 LoRA 而不是全量微调

10k 级数据不足以安全地全量更新 7B 模型，容易过拟合和破坏通用能力。LoRA 只学习低秩增量，显存和训练成本更低，也便于为 VLM、Planner、Judge 维护不同适配器。项目的主要增益来自领域数据、证据工作流和检索，不需要用全量微调证明技术含量。

---

## 6. 核心数据结构

### 6.1 InferenceRequest

封装问题、任务上下文、图片、检测框和轨迹，是整个状态机的入口；同时包含 `task_id`、时间戳、相机参数和规则版本。

### 6.2 EvidencePlan

Planner 的结构化输出，包含：

- 问题类型和风险等级；
- 原子 Claim；
- 每条 Claim 的必要证据槽位；
- 最低质量或最低来源数量；
- 可用工具；
- Claim 依赖；
- 禁止推断。

### 6.3 VisualFact

只记录图片直接支持的可见事实：

```json
{
  "fact_id": "VF-crop-02",
  "claim": "局部图中可见一辆白色车辆",
  "image_id": "IMG-001",
  "bbox": [102, 88, 214, 166],
  "object_type": "vehicle",
  "confidence": 0.91,
  "visibility": "clear",
  "source": "uav-vlm-7b",
  "crop_path": "crops/IMG-001-a9f3.jpg",
  "supports": ["C1"]
}
```

### 6.4 RetrievalEvidence

记录规则或案例证据，包含 `doc_id`、版本、标题、摘要、相关度、来源、适用区域和有效期。它不能被混写成视觉事实。

### 6.5 TemporalEvidence

记录 `track_id`、帧范围、时间戳、轨迹、连续观测比例和时长统计，用于支持移动、停留和先后关系。

### 6.6 Claim-Evidence Graph

图中有两类节点：Claim 节点和 Evidence 节点；主要边类型包括：

- `supports`：证据支持 Claim；
- `contradicts`：证据反对 Claim；
- `depends_on`：下游 Claim 依赖上游 Claim；
- `derived_from`：局部证据来源于哪张原图或轨迹。

### 6.7 JudgeResult

Judge 输出 supported、unsupported、conflicts、missing evidence、recommendation 和 reason code，不直接修改证据或决定最终答案。

### 6.8 GroundedAnswer

最终输出包含：

- `answer`：回答正文；
- `decision`：answer/collect/insufficient/review；
- `confidence`：校准后可信度；
- `visual_evidence`；
- `knowledge_evidence`；
- `missing_evidence`；
- `next_action`；
- `caveats`；
- 完整 `trace`。

---

## 7. 端到端执行流程

### 7.1 Prepare：输入准备

对应 `prepare_node`。主要工作：

1. 检查图片是否存在、可读和可解码；
2. 校验 bbox 坐标与图片范围；
3. 校验多帧是否属于同一任务；
4. 初始化视觉事实、知识证据、工具历史和轮数；
5. 为请求生成 trace ID 和幂等键。

为什么先校验：坏图片或越界 bbox 如果直接送入模型，会把基础数据错误变成模型幻觉问题，既浪费调用成本又难排查。

### 7.2 Planner：生成证据计划

Planner 读取问题、上下文和领域 Skill，不回答问题，而是输出 EvidencePlan。例如：

```json
{
  "question_type": "temporal_risk_assessment",
  "risk_level": "high",
  "claims": [
    {"claim_id": "C1", "text": "画面中存在人员", "required_evidence": ["visual_object"]},
    {"claim_id": "C2", "text": "人员位于限制区域", "required_evidence": ["visual_region", "task_context"]},
    {"claim_id": "C3", "text": "人员持续停留超过阈值", "required_evidence": ["temporal_track"]},
    {"claim_id": "C4", "text": "需要通知现场处置", "depends_on": ["C1", "C2", "C3"], "required_evidence": ["domain_rule"]}
  ],
  "disallowed_inferences": ["单图不能推断持续时间", "不能从外观推断身份"]
}
```

### 7.3 Observe：提取初始视觉事实

对应 `observe_node`。全图 VLM 只输出结构化 VisualFact，不直接回答风险或处置问题。检测器产生的 bbox 是候选区域，不是最终事实。

### 7.4 Assess：评估证据缺口

对应 `assess_evidence_node`。系统比较 EvidencePlan 的必要槽位和当前证据状态，检查：

- 是否存在可靠视觉事实；
- 是否存在面积小于 4% 的候选目标；
- 风险问题是否缺少规则；
- 时序问题是否缺少多帧或轨迹；
- 是否存在视觉冲突；
- 当前轮数、延迟和费用预算是否允许继续。

### 7.5 Plan Action：选择下一项工具

对应 `plan_action_node`。工具选择目标是最大化单位成本的信息增益，而不是固定依次调用所有工具：

```text
小目标细节缺失       → crop_region
风险/规则证据缺失    → retrieve_knowledge
文字不清             → OCR
时序证据缺失且有多帧 → temporal_analyzer
截图损坏或视角不足   → request_snapshot
关键冲突             → review
```

### 7.6 Execute：执行工具并更新证据

对应 `execute_action_node`。每次工具调用记录：调用原因、参数、输入 ID、输出证据、耗时、错误和新增 Claim 覆盖。

### 7.7 Claim Graph + Judge

新证据进入 Claim-Evidence Graph；Judge 检查“证据是否真的蕴含 Claim”，而不是只检查关键词相关。Judge 发现上游 Claim 缺证时，下游 Claim 不能传播。

### 7.8 Calibration + Policy

校准器计算每条 Claim 的可信度，Policy Engine 根据风险阈值输出：

- `answer`：必要 Claim 全部通过；
- `collect`：证据缺失但仍能补证；
- `insufficient`：能力越界、预算耗尽或没有可执行工具；
- `review`：高风险、关键冲突或需要人工确认。

### 7.9 Finalize：生成 GroundedAnswer

最终生成模型只能使用已经批准的 Claim 和证据摘要，不允许访问未通过的候选推断。生成后再次执行引用完整性检查，保证答案中的事实性表述都能映射到 evidence ID。

---

## 8. 核心模块详细设计

### 8.1 Evidence Planner 与领域 Skill

#### Planner 是干什么的

Planner 解决的是“这个问题需要证明什么”，而不是“答案是什么”。它把复杂问题拆成可独立验证的 Claim，并为每条 Claim 指定证据类型、风险等级、允许工具和禁止推断。

例如“这辆车是不是长时间停在道路边缘，需要处理吗”至少包含：

1. 是否存在车辆；
2. 车辆是否位于道路边缘；
3. 是否持续停留；
4. 是否命中巡检规则；
5. 是否需要处置。

如果直接让模型回答，五个子问题容易被压成一句“车辆长时间停留，建议处理”。Planner 将其拆开后，单图只能支持前两项，第三项需要轨迹，第四项需要规则，第五项依赖前四项。

#### Skill 是什么

Skill 是版本化的领域证据规程，通常由 YAML/Markdown、JSON Schema 和正反例组成。它包含：

- 问题类型和风险等级；
- 每类 Claim 的必要证据；
- 小目标、时序和高风险阈值；
- 工具优先级和权限；
- 禁止推断；
- Planner/Judge 输出 Schema；
- 正例、反例和 reason code。

Skill 不是知识库，也不是事实来源。Skill 规定“风险结论必须引用有效规则”，但真正的规则内容必须来自版本化知识库。

#### Skill 与 Prompt 的区别

- Prompt 是一次模型调用的指令；
- Skill 是可版本化、可测试、可复用的领域规程集合；
- Skill 可以被 Planner、Judge、评测器和策略引擎共同消费；
- Skill 变更需要版本号、回归集和灰度发布，而不是直接改一句 Prompt 上线。

#### 为什么 Planner 也要硬校验

Planner 仍然是模型，可能输出不存在的工具、过多 Claim 或非法枚举。因此输出后必须检查：

- JSON Schema；
- Claim 数量上限；
- 工具白名单；
- 依赖图是否有环；
- 风险等级是否合法；
- 必要证据是否与 Skill 一致。

解析失败受控重试一次，仍失败则进入保守默认计划或拒答。

### 8.2 确定性硬门控

硬门控守住模型不能绕过的条件，分为五类：

#### 输入门控

- 图片存在、可读取、可解码；
- 图片 ID、任务 ID 和时间戳完整；
- bbox 不越界；
- 多帧输入来自同一任务和合理时间窗。

#### 证据门控

- 视觉 Claim 至少绑定一个有效 VisualFact；
- 小目标完成局部复核；
- 时序 Claim 具有足够帧数、时间戳和连续 track；
- 风险/处置 Claim 绑定有效版本规则；
- 下游 Claim 的所有依赖已经通过。

#### 工具与权限门控

- 工具必须在白名单中；
- Agent 只能请求截图，不能直接操控飞行；
- 参数范围合法；
- 不超过最大轮数、超时、费用和并发预算。

#### 输出门控

- 事实性句子必须具有 claim ID；
- claim ID 必须绑定真实 evidence ID；
- 引用的图片、bbox、轨迹和规则文档真实存在；
- 最终答案不得包含被 Skill 禁止的身份、意图和时序推断。

#### 风险门控

低风险可见事实的校准阈值为 0.75，中风险空间/规则判断为 0.82，高风险告警为 0.90；处置建议即使过阈值仍要求人工确认。阈值来自验证集的 selective risk 曲线，不是拍脑袋设置。

### 8.3 视觉模型与粗到细小目标复核

#### 为什么整图 VLM 对小目标不友好

视觉编码器把图像划分为 patch。目标占图面积很小时，有效 patch 数量不足；缩放后细节进一步丢失，模型更容易依赖背景先验。目标检测器虽然更擅长定位，但它的类别标签也可能出错，因此检测框只能作为候选区域。

#### 粗到细流程

```text
全图 VLM 提取候选事实
        ↓
检测器 / SAHI 产生候选 bbox
        ↓
bbox 面积比 < 4% 或 visibility != clear
        ↓
crop_region 按 bbox 外扩 20%
        ↓
局部 VLM / 专用分类器重新确认
        ↓
全图、检测器和局部结果一致 → 增强支持
冲突 → request_snapshot / review
```

#### 为什么要保留 20% 上下文

裁剪过紧会丢失道路、限制区域边界和目标周边环境，导致模型只能看见一个物体而无法判断空间关系。20% padding 在验证集上兼顾了目标像素占比和场景上下文；边缘 bbox 会自动裁剪到图像范围内。

#### 如何避免无限裁剪

- 相同原图、bbox 和 padding 使用 SHA-256 生成幂等文件名；
- 相同参数不重复调用；
- 局部图没有新增事实时记录 `SMALL_OBJECT_UNRESOLVED`；
- 每个 Claim 最多一次常规裁剪和一次补拍复核；
- 达到工具预算后拒答或转人工。

#### 检测框错了怎么办

检测标签不会直接进入最终答案。局部 VLM 必须重新识别；如果检测器、全图 VLM 与局部 VLM 冲突，Claim-Evidence Graph 建立 `contradicts` 边，Judge 输出 `VISUAL_LABEL_CONFLICT`，Policy Engine 选择补拍或人工复核。

### 8.4 细粒度多模态 RAG

#### 普通单向量 RAG 的问题

Bi-encoder 通常把整个问题压成一个向量、整个文档压成一个向量。对于“右下角小目标是否符合道路边缘车辆规则”这类问题，目标类别、位置、ROI 细节和规则关键词会在池化中被平均，单向量相似度可能只匹配到“车辆”主题，却忽略“道路边缘”和具体视觉区域。

#### 查询和文档如何表示

查询 token 由三部分组成：

```text
Q = [问题文本 token；全图视觉 token；问题相关 ROI token]
```

知识文档 token 由以下内容组成：

```text
D = [标题 token；正文 token；规则元数据 token；可选示例图 token]
```

视觉 token 通过两层 MLP projection 映射到与文本 token 相同的 128 维检索空间。ROI token 来自候选区域，保留小目标和局部空间线索。

检索侧视觉编码器采用 OpenCLIP ViT-L/14，与生成侧 Qwen2.5-VL 解耦。这种异构编码既便于离线建立图像 token 索引，也降低检索与生成完全共享同一视觉偏差的风险。

#### Late Interaction 与 MaxSim

对每个查询 token，在文档所有 token 中寻找最大相似度，再对查询 token 求和：

```text
score(Q, D) = Σ_i max_j (q_i · d_j)
```

它的直觉是：问题中的“车辆”“道路边缘”“限制区域”和 ROI 视觉 token 可以分别匹配文档中的不同 token，不必提前压成一个向量。相比 Cross-encoder，文档 token 可以离线编码；相比单向量 Bi-encoder，它保留了更细粒度的交互。

#### 工程检索链路

```text
问题 + 上下文 + VisualFact + ROI
        ↓
元数据过滤：任务类型、区域、规则版本、有效期
        ↓
BM25 + BGE-M3 混合粗召回 Top-200
        ↓
RRF 融合并截取 Top-50
        ↓
FLMR 风格 token 级 MaxSim 重排
        ↓
规则适用条件过滤 + 去重
        ↓
返回 Top-5 RetrievalEvidence
```

#### 为什么不直接对全库做 MaxSim

token 级交互的计算和存储成本高于单向量检索。两阶段检索先用低成本方法缩小候选集，再对 Top-50 精排，使 Recall@10 提升 8.5pt 的同时，把 P95 端到端延迟增幅控制在 20% 以内。

#### 多模态知识库包含什么

- 无人机巡检规范和安全规则；
- 任务区域、限制区和设施说明；
- 历史异常案例及对应截图；
- 目标类别、别名和易混淆对象；
- 处置流程、适用条件和规则版本。

离线索引包含约 3,200 个规则条款语义块和 4,800 条历史巡检图文案例。文档 token 向量离线编码，新增规则按版本增量更新；历史案例图片同时保存全图和关键 ROI 表示。

#### Chunk 如何切分

规则文档不能只按固定字符数切分。系统优先按“规则条款—适用条件—处置措施”组成语义块，并保留标题、章节、版本和区域元数据。普通文档块控制在 180 个 token 左右，重叠 30 token；表格按行和表头联合展开，避免规则条件与措施被切开。

#### 如何处理规则版本

每条 RetrievalEvidence 都包含版本、发布日期、有效期、适用区域和来源等级。检索前先进行元数据过滤；旧版本即使语义更相似也不能进入最终候选。正式规范的权威性特征还会输入校准器。

#### 检索效果怎么评测

- Recall@K：正确文档是否出现在 Top-K；
- MRR：第一个正确文档的排名倒数；
- nDCG@K：考虑多级相关性的排序质量；
- 引用正确率：最终答案引用的规则是否真的支持 Claim；
- 版本命中率：是否命中当前有效规则；
- 小目标子集 Recall：ROI 是否帮助找到正确案例或规则。

### 8.5 Claim-Evidence Graph

#### Claim 是什么

Claim 是可以独立判断真假的最小结论单元。例如“右侧有人并且长时间停留，需要立即处置”不是一个好的 Claim，它至少应拆为存在性、空间关系、时序、规则命中和处置五个 Claim。

#### 为什么要构建图

列表只能说明有哪些证据，图可以表示结论依赖。例如：

```text
VF-01 ─────────────→ C1：存在人员
VF-02 + Context-01 ─→ C2：人员位于限制区域
TE-01 ─────────────→ C3：停留超过 10 分钟
C1 + C2 + C3 + Rule-07 → C4：命中现场复核规则
C4 + Rule-08 ──────→ C5：建议通知现场核查
```

如果 C3 缺失，C4 和 C5 即使语言上合理也会被拓扑阻断。这样可以避免用一个高总分掩盖局部无证据结论。

#### 图上执行什么检查

- Coverage：必要证据槽位是否齐全；
- Dependency：依赖 Claim 是否全部通过；
- Provenance：能否追溯到具体图片、区域、轨迹和文档；
- Conflict：同一 Claim 是否存在支持和反对证据；
- Cycle：Claim 依赖是否形成非法环；
- Propagation：上游失败是否正确阻断下游。

#### 与知识图谱有什么区别

知识图谱描述长期稳定的实体和关系；Claim-Evidence Graph 是一次请求内的临时证明图，重点是“这次回答为什么成立”。它更接近 provenance graph 或 argument graph，而不是实体知识图谱。

### 8.6 Independent Evidence Judge

#### Judge 做什么

Judge 只判断 Evidence 是否蕴含 Claim，检查：

- 证据只是主题相关，还是能够真正支持 Claim；
- “可能”是否被候选答案改写成“确定”；
- 是否把一般规则改写成现场事实；
- 是否用单图支持时序结论；
- 是否选择性忽略反证；
- 是否违反 Skill 的禁止推断。

#### Judge 为什么不直接生成答案

生成任务容易让 Judge重新编造一套解释。将它限制为结构化 entailment 审查，可以缩小任务空间并减少锚定。Judge 只输出 supported/unsupported/conflict/missing 和 reason code。

#### 如何降低主模型和 Judge 的相关性错误

- 使用不同 LoRA adapter 和独立训练集切分；
- Judge 看结构化证据，不看主模型长 CoT；
- 引入检测器、OCR、轨迹和规则库等异构证据；
- 高风险分歧不投票消除，而是补证或转人工；
- 单独评测“主模型与 Judge 同时出错”的相关性错误率。

#### Judge 也错了怎么办

Judge 不是最终安全边界。它的结果还要经过 Schema、Claim 图、校准器、风险阈值和 Policy Engine。模型一致只是一个特征，不能绕过硬门控。

### 8.7 Confidence Calibrator

#### 为什么不能使用模型自报置信度

模型的 0.9 通常只表示 token 分布或主观输出，不是“在业务数据上有 90% 正确率”。分布漂移、小目标和遮挡会导致严重过度自信。

#### 输入特征

- VLM、检测器、OCR 原始分数；
- bbox 面积比、像素数、清晰度、遮挡；
- 全图与局部是否一致；
- 异构验证器是否一致；
- Claim 必要证据覆盖率；
- 检索相关性、来源权威性、规则版本有效性；
- Judge 结论和 reason code；
- 冲突数量；
- Claim 类型和风险等级。

#### 校准方法

先使用 Logistic Regression 或 LightGBM 学习 Claim 正确概率，再通过 Platt Scaling 或 Isotonic Regression 做后校准。数据量较小时 Logistic Regression 更稳定、可解释；非线性特征明显时使用 LightGBM。

#### 如何判断校准是否好

- ECE：预测概率与真实准确率之间的分桶差异；
- Brier Score：预测概率与真实标签的均方误差；
- Reliability Diagram：例如预测 0.8 的样本是否约有 80% 正确；
- NLL：概率模型对真实标签的负对数似然。

### 8.8 Policy Engine

Policy Engine 是最终放行者，不理解图片，只消费结构化状态：

```python
if hard_gate_failed:
    return INSUFFICIENT

if critical_conflict or human_confirmation_required:
    return REVIEW

if required_claims_missing:
    return COLLECT if tool_available and budget_available else INSUFFICIENT

if all_required_claims_supported and all_claims_above_threshold:
    return ANSWER

return REVIEW
```

#### 为什么最终权力不能交给模型

安全策略、权限、预算和时序最低条件必须可测试、可重复。模型可以建议下一步，但不能修改最大轮数、绕过人工确认或把单帧升级成时序证据。

### 8.9 工具设计

#### crop_region

输入原图和 bbox，完成图片校验、归一化/像素坐标转换、20% padding、边界裁剪、SHA-256 命名和局部图保存。输出 crop path 与来源关系。

#### retrieve_knowledge

输入由问题、任务上下文、已确认 VisualFact 和待验证 Claim 组成，经过元数据过滤、混合粗召回和 FLMR 重排，返回带版本与来源的 RetrievalEvidence。

#### request_snapshot

生成结构化补拍请求：目标区域、建议视角、清晰度、原因和关联 Claim。调用无人机平台的只读截图接口，不直接执行飞行控制。

#### temporal_analyzer

基于 ByteTrack 的 track、时间戳和连续帧统计速度、位移和停留时间。只有满足最小观测窗口与轨迹连续性时，才能生成 TemporalEvidence。

#### OCR

PaddleOCR 提取标牌、编号和文字，局部 VLM 检查语义。如果 OCR 与 VLM 冲突，文字 Claim 进入补证或人工复核。

### 8.10 有限工具循环

每轮流程是：

```text
评估证据缺口 → 选择成本最低且信息增益最高的工具
→ 获取新证据 → 更新 Claim 图 → Judge 复审
→ 重新校准 → Policy 决策
```

停止条件：

- 最大工具轮数默认为 2；
- 单工具超时；
- 单请求费用和总时延预算；
- 相同工具参数去重；
- 新证据没有增加 Claim 覆盖；
- 关键冲突直接转人工；
- 时序能力越界直接拒答。

---

## 9. 代码文件与流程映射

### `agent709/config.py`

负责模型服务、知识库、Top-K、最小视觉置信度、小目标面积阈值、裁剪 padding、最大工具轮数和超时等配置。生产环境通过环境变量和配置中心管理，阈值变更进入版本审计。

### `agent709/schemas.py`

定义 DetectionObject、VisualFact、RetrievalEvidence、EvidenceAssessment、ToolAction、ToolTrace、GroundedAnswer、InferenceRequest、EvidencePlan、ClaimNode、TemporalEvidence、JudgeResult 和 CalibrationResult。

### `agent709/llm.py`

封装 OpenAI-compatible 模型客户端。编排层只依赖 `LLMClient` 协议，可切换本地 vLLM 或私有推理平台。

### `agent709/prompts.py`

维护视觉事实提取、证据规划、Judge 和最终回答 Prompt。所有输出要求 JSON Schema，温度较低，并记录 prompt version。

### `agent709/vision.py`

负责图片编码、多模态请求、JSON 解析和 VisualFact 标准化。模型输出中的非法 visibility、confidence 和 bbox 会被清洗或拒绝。

### `agent709/image_tools.py`

负责 `validate_image`、`bbox_area_ratio` 和 `crop_region`。它体现了小目标门控、坐标处理、可复现裁剪和幂等命名。

### `agent709/kb.py`

定义统一 KnowledgeBase 和 RetrievalEvidence 接口。生产检索实现替换为 BM25 + BGE-M3 + FLMR token 重排，但 Pipeline 不需要改变数据契约。

### `agent709/pipeline.py`

实现 Prepare、Planner、Observe、Assess、Plan、Execute、Claim Graph、Judge、Calibration、Policy 和 Finalize 等业务节点。

### `agent709/graph.py`

使用 LangGraph 显式编排节点、条件边和循环：

```text
START → prepare → planner → observe → assess
                   ↑                  │
                   └──── execute ← plan
                                      ↓
claim_graph → judge → calibrate → policy → finalize → END
```

### `agent709/main.py`

CLI/API 入口，读取配置、构造请求、初始化模型和知识库、调用 Graph 并输出 GroundedAnswer。

### `agent709/evaluation.py`

离线评测入口，统计 Planner、Vision、Retrieval、Judge、Calibration、Policy、Latency 和 End-to-end 指标，并输出逐样本 Trace 与错误分类。

### `tests/test_agent.py`

覆盖小目标裁剪、规则检索、单图时序拒答、缺图补拍、工具轮数、冲突转人工和 Schema 失败降级等关键路径。

---

## 10. 三个完整案例

### 10.1 案例一：小目标车辆风险问答

#### 用户问题

> 右下方的小目标是不是车辆？停在道路边缘是否需要关注？

#### 初始输入

- 原图：4K 航拍图；
- detector：车辆候选 bbox，score=0.82；
- bbox 面积占比：1.3%，低于 4% 阈值；
- 任务上下文：山地道路巡检；
- 当前只有单张截图。

#### Planner 输出

```text
C1：右下方存在车辆           → visual_object
C2：车辆位于道路边缘         → visual_region + task_context
C3：该情况命中关注规则       → C1 + C2 + domain_rule
C4：车辆是否持续停留         → temporal_track
```

#### 工具流程

1. 全图 VLM 输出“疑似车辆”，visibility=partial；
2. 硬门控发现 bbox 面积过小，C1 尚不能放行；
3. Agent 调用 `crop_region`，外扩 20% 生成局部图；
4. 局部 VLM 确认白色车辆，confidence=0.91；
5. 全图、检测器和局部 VLM 一致，C1 通过；
6. 根据 bbox 与道路区域多边形计算，C2 通过；
7. 多模态 RAG 检索到当前版本《山地道路巡检规则》相关条款，C3 通过；
8. C4 因缺少多帧轨迹不通过；
9. Judge 检查 C1-C3 有证据，C4 为单帧时序越界；
10. Policy 允许回答可见事实和规则判断，但拒绝持续停留结论。

#### 最终回答示例

```text
局部复核确认右下方目标为一辆白色车辆，位于山地道路边缘。根据《山地道路巡检规则》当前有效条款，该位置车辆需要列入关注并建议后续复核。当前只有单张截图，无法判断车辆是否持续停留；如需确认停留时长，应补充同一 track 的连续帧或轨迹数据。
```

#### 这个案例体现什么

- ROI 解决小目标；
- 视觉事实和规则证据分离；
- 一个问题可以部分回答、部分拒答；
- Claim 级门控比整段置信度更精确。

### 10.2 案例二：单图时序问题机制性拒答

#### 用户问题

> 这个人是不是已经在限制区域徘徊十分钟？

#### 系统判断

- 全图和局部图都能证明“存在人员”和“位于限制区域”；
- 输入只有一张截图，没有时间戳序列和连续 track；
- Planner 生成 `temporal_track` 和 `timestamp_window` 槽位；
- 硬门控直接阻断“十分钟”和“徘徊” Claim；
- 系统不允许 Judge 或 VLM 用外观猜测时长。

#### 最终回答

```text
当前截图可以确认限制区域内存在人员，但单张截图无法证明其是否徘徊或已停留十分钟。请提供同一目标的连续帧、时间戳和轨迹信息后再判断。
```

#### 价值

这是“机制性拒答”，不是 Prompt 中写一句“请谨慎”。即使模型输出确定答案，Policy Engine 也会因为缺少 TemporalEvidence 而拒绝放行。

### 10.3 案例三：异构证据冲突转人工

#### 用户问题

> 右侧目标是不是人员，需要报警吗？

#### 证据状态

- 全图 VLM：疑似人员，0.76；
- detector：消防栓，0.84；
- 局部 VLM：无法确认，0.61；
- 风险等级：高；
- 规则：只有“人员进入限制区域”才触发报警。

#### 决策过程

1. C1“存在人员”同时具有支持和反对证据；
2. Claim 图标记 `visual_label_conflict`；
3. 依赖 C1 的“进入限制区域”和“需要报警”全部阻断；
4. Judge 输出 conflict；
5. 校准器降低 C1 概率；
6. 高风险策略选择 `review`，并生成补拍请求。

#### 最终回答

```text
当前证据对目标类别存在冲突，无法可靠确认其为人员，因此不生成报警结论。建议补拍右侧区域的更清晰近景，并转人工复核。
```

#### 价值

系统不是让三个模型投票后按多数决定，而是承认关键冲突并阻断下游风险结论。

---

## 11. 评测体系、A/B 与消融实验

### 11.1 测试集

独立测试集共 1,200 条，按任务 ID 与训练集隔离：

| 子集 | 数量 | 评测重点 |
| --- | ---: | --- |
| 目标存在与属性 | 220 | 基础视觉事实 |
| 小目标与 ROI | 220 | 粗到细复核 |
| 空间关系 | 180 | 区域与位置判断 |
| 规则与风险 | 240 | 检索和规则引用 |
| 时序问题 | 160 | 多帧要求和正确拒答 |
| 冲突/缺证/坏图 | 180 | Judge、门控和人工转交 |
| 合计 | 1,200 | 端到端评测 |

### 11.2 A/B 定义

A/B 在相同硬件和服务配置下完成：单张 A800 80GB 推理、并发 4、相同规则库和测试集、缓存预热后重复 5 次，延迟取各次合并分布的 P95。A/B 只改变方案组件，不改变测试样本和超时配置。

#### A 组：基线

- 原始 Qwen2.5-VL 整图单次回答；
- 普通单向量 dense RAG；
- 固定 Prompt；
- 不做 ROI 主动复核；
- 不做 Claim Graph、Judge 和确定性证据门控。

#### B 组：完整方案

- 领域 LoRA/SFT 模型；
- 全图 + ROI 粗到细复核；
- BM25/BGE 粗召回 + FLMR token 级重排；
- Evidence Planner + Skill；
- Claim-Evidence Graph + Judge + Calibrator + Policy；
- 有限工具循环和人工兜底。

### 11.3 核心结果

| 指标 | A 组 | B 组 | 变化 |
| --- | ---: | ---: | ---: |
| Recall@10 | 78.4% | 86.9% | +8.5pt |
| 小目标问答准确率 | 71.8% | 78.4% | +6.6pt |
| 规则引用正确率 | 84.1% | 92.7% | +8.6pt |
| 无依据回答率 | 13.2% | 8.4% | -36.4%（相对下降） |
| 正确拒答率 | 78.6% | 89.2% | +10.6pt |
| 高风险漏放率 | 4.8% | 1.6% | -66.7%（相对下降） |
| P95 端到端延迟 | 3.10s | 3.64s | +17.4% |
| 平均工具调用次数 | 0 | 1.34 | +1.34 |

#### `pt` 和 `%` 的区别

- 71.8% → 78.4% 是提升 6.6 个百分点，即 `6.6pt`；
- 13.2% → 8.4% 的绝对下降是 4.8pt，相对下降为 `(13.2-8.4)/13.2=36.4%`。

面试时不要把百分点和相对百分比混用。

#### P95 延迟是什么

把所有请求耗时从小到大排序，第 95 百分位的耗时就是 P95。P95=3.64 秒表示 95% 请求在 3.64 秒内完成，最慢 5% 更久。完整方案相较基线增加 ROI、检索和 Judge，但 P95 只增加 17.4%，说明可靠性收益没有以不可接受的尾延迟为代价。

### 11.4 分层指标

#### Planner

- Claim 拆解准确率；
- 证据需求准确率；
- 风险等级准确率；
- 工具建议准确率；
- Schema 合法率。

#### Vision

- 目标分类准确率；
- bbox IoU/mAP；
- 小目标子集准确率；
- 全图与局部一致性；
- OCR 字符准确率。

#### Retrieval

- Recall@K、MRR、nDCG；
- 规则版本命中率；
- 引用正确率；
- 多模态案例命中率。

#### Judge

- Unsupported Claim Recall；
- Conflict Recall；
- False Rejection Rate；
- 与主模型共同错误率。

#### Calibration

- ECE；
- Brier Score；
- Reliability Diagram；
- 各风险阈值下的 coverage-risk 曲线。

#### Policy 与端到端

- 正确回答率；
- 正确拒答率；
- 人工转交准确率；
- 无依据回答率；
- 高风险漏放率；
- 平均工具次数；
- P50/P95/P99 延迟；
- 单请求 GPU 时间和成本。

### 11.5 消融实验

| 方案 | Recall@10 | 小目标准确率 | 无依据回答率 | 正确拒答率 | P95 |
| --- | ---: | ---: | ---: | ---: | ---: |
| 完整方案 | 86.9% | 78.4% | 8.4% | 89.2% | 3.64s |
| 去掉 ROI 局部复核 | 86.2% | 72.2% | 9.0% | 87.8% | 3.28s |
| FLMR 改为单向量 RAG | 79.1% | 76.9% | 9.6% | 87.5% | 3.42s |
| 去掉 Independent Judge | 86.8% | 78.1% | 10.9% | 84.7% | 3.43s |
| 去掉 Claim 门控，仅总分放行 | 86.7% | 78.0% | 13.0% | 79.0% | 3.31s |
| 去掉领域 LoRA | 84.8% | 73.9% | 11.4% | 82.6% | 3.57s |

#### 如何解释消融结果

- ROI 主要贡献小目标准确率；
- FLMR 主要贡献 Recall@10 和规则引用；
- Judge 与 Claim 门控主要降低无依据回答并提升正确拒答；
- 领域 LoRA 同时影响视觉和证据行为；
- P95 增加主要来自局部 VLM 和 token 级重排，因此使用按需调用而不是全量调用。

### 11.6 如何保证评测可信

- 按任务 ID 分割，避免连续帧泄漏；
- 固定模型、Prompt、规则版本和随机种子；
- 每个指标同时报告样本量和置信区间；
- 高风险样本双人标注并仲裁；
- A/B 使用相同硬件、并发和超时；
- 保存每条样本的 trace 和错误分类；
- 不只看平均值，单独分析小目标、夜间、遮挡和分布外子集。

---

## 12. 工程化、部署与稳定性

### 12.1 服务拆分

```text
API Gateway
  ├─ Agent Orchestrator
  ├─ VLM Service
  ├─ Detector/OCR Service
  ├─ Retrieval Service
  ├─ Temporal Service
  ├─ Calibration/Policy Service
  └─ Trace & Audit Service
```

推理量不大时可以先模块化单体部署；当 GPU 模型、检索和 Agent 扩缩容需求不同，再拆为独立服务。

### 12.2 缓存

- 原图 hash + model version → 全图 VisualFact；
- 原图 hash + bbox + padding → crop；
- crop hash + model version → 局部 VisualFact；
- query fingerprint + rule version → RetrievalEvidence；
- document version → token embeddings。

缓存键必须包含模型、Prompt、Skill 和规则版本，避免版本变化后读取旧结果。

### 12.3 超时、重试和熔断

- 模型结构化输出解析失败最多重试一次；
- 工具调用采用指数退避和 jitter；
- 非幂等工具不自动重试；
- VLM 服务超时后降级为检测器 + 人工复核；
- 检索不可用时只回答可见事实，不生成风险和处置；
- 连续错误触发熔断，避免雪崩。

### 12.4 幂等性

每个请求使用 `task_id + query_hash + image_hash + model_version` 生成幂等键。裁剪、检索和补拍请求都记录 action ID，重复请求返回已有结果或明确状态，避免重复扣费和重复补拍。

### 12.5 并发与 GPU 调度

- vLLM continuous batching 提升吞吐；
- 全图和局部模型共享权重但使用不同请求队列；
- 高风险请求优先级更高；
- ROI、OCR 和检索可并行时并行执行；
- 对同一请求的 Claim Graph 更新使用版本号或乐观锁，避免并发覆盖。

### 12.6 可观测性

每次请求记录：

- trace ID、task ID、模型/Prompt/Skill/规则版本；
- 每个节点开始结束时间；
- token 数、GPU 时间和费用；
- 工具参数与返回摘要；
- Claim 状态变化；
- 最终决策和 reason code；
- 人工复核结果与后续反馈。

监控指标包括 QPS、成功率、结构化输出失败率、工具重试率、拒答率、review 率、P95/P99、GPU 利用率、缓存命中率和高风险漏放率。

### 12.7 数据与安全

- 截图和轨迹按任务进行访问控制；
- 敏感区域和人员信息脱敏；
- 模型服务与无人机控制平面隔离；
- Agent 工具使用最小权限；
- 所有规则和 Skill 版本可追溯；
- Prompt Injection 输入不能修改工具白名单和硬策略；
- 知识文档入库前进行来源验证和恶意指令清洗。

### 12.8 失败模式与降级

| 失败模式 | 处理方式 |
| --- | --- |
| 图片损坏 | `INVALID_IMAGE_INPUT`，请求新截图 |
| VLM 输出非法 JSON | 受控重试一次，仍失败视为无可靠事实 |
| 局部图仍不清晰 | `SMALL_OBJECT_UNRESOLVED`，补拍或人工 |
| 检测器与局部 VLM 冲突 | 记录 conflict，不按多数投票 |
| 检索为空 | 只描述可见事实，不输出风险结论 |
| 规则版本失效 | 阻断规则 Claim |
| 单图时序问题 | `SINGLE_FRAME_TEMPORAL_CLAIM`，拒答 |
| Judge 与图状态冲突 | 以硬门控和图依赖为准，转人工 |
| 达到工具预算 | 输出 missing evidence 和 next action |
| 模型服务不可用 | 降级到专业工具或人工复核 |

---

## 13. 项目高频面试问题与标准回答

### Q1：为什么需要 Agent，固定 Pipeline 不行吗？

固定 Pipeline 负责图片校验、Schema、工具执行和最终策略；Agent 只负责运行时证据规划。不同问题缺少的证据不同：存在性只需要视觉，小目标需要 ROI，风险需要规则，持续停留需要轨迹。动态部分是“下一步补什么证据”，安全边界仍然是确定性的。

### Q2：为什么不直接让 VLM 回答？

直接回答会混合可见事实、规则和推测，也无法定位每条结论来自哪个区域。系统把 VLM 限制为 VisualFact 提取器，最终答案只使用通过 Claim Graph、Judge 和策略的证据。

### Q3：为什么不是纯代码门控？

代码适合校验图片、帧数、权限和阈值，但复杂语义仅靠关键词容易漏判。例如“总在附近活动”本质上是时序问题。Planner 负责理解证据需求，代码负责验证 Planner 输出和最终放行。

### Q4：为什么不只用第二个模型做门控？

相似模型可能产生相关性错误，两个模型同意不等于正确。系统使用全图/局部、VLM/检测器、OCR、轨迹和规则等异构证据；Judge 只是结构化审查输入，最终仍由策略引擎放行。

### Q5：Claim 是什么？

Claim 是可以独立判断真假的最小结论。复杂回答拆成 Claim 后，每条结论分别绑定证据和阈值，避免整段高置信度掩盖局部无依据推断。

### Q6：Claim-Evidence Graph 与普通证据列表有什么不同？

列表只能表示“有哪些证据”，图还能表示 supports、contradicts 和 depends_on。上游 Claim 失败会自动阻断下游风险和处置结论，并能追溯失败传播路径。

### Q7：Skill 是不是换了个名字的 Prompt？

不是。Skill 是版本化领域规程，包含证据标准、风险分级、工具策略、禁止推断、Schema 和正反例，可被 Planner、Judge、Policy 和评测共同使用；Prompt 只是一次调用的指令载体。

### Q8：为什么 Judge 不拥有最终决策权？

Judge 也可能受提示和模型偏差影响。它只检查证据蕴含并输出 reason code，最终还要通过硬门控、图依赖、校准阈值和风险策略。

### Q9：检测框错了怎么办？

检测框只负责候选区域，不负责事实标签。局部 VLM 或分类器重新确认；发生冲突时记录反证并补拍或转人工，不直接沿用 detector label。

### Q10：为什么面积阈值是 4%？

在验证集上按目标面积分桶后，整图 VLM 在 4% 以下准确率明显下降，而局部复核收益显著。4% 是基于 accuracy-cost 曲线选择的工作点，不是通用常数，换相机分辨率和模型后需要重新标定。

### Q11：为什么 padding 是 20%？

过紧裁剪会丢失道路和限制区上下文，过宽又会降低目标占比。验证集上 20% 在目标识别和空间关系任务之间效果最好；边界处会做 clipping。

### Q12：为什么使用 FLMR Late Interaction？

问题同时包含文本、全图和 ROI 线索。单向量池化容易丢失局部信息；Cross-encoder 效果高但无法对全库低成本检索。Late Interaction 让文档离线编码，同时保留 token 级匹配，是效果和成本之间的折中。

### Q13：MaxSim 怎么算？复杂度多大？

对每个查询 token 与文档所有 token 做相似度，取最大值后求和，复杂度约为 `O(|Q|×|D|)`。因此只在粗召回后的 Top-50 使用，并通过 token 裁剪、FP16、批量矩阵乘和缓存控制成本。

### Q14：为什么还要 BM25？

规则编号、设施名称和专有词对关键词检索很敏感，dense 模型可能忽略精确匹配。BM25 与 dense 互补，用 RRF 融合可以提升召回稳定性。

### Q15：RAG 返回相关文档就等于可以回答吗？

不等于。相关性只说明主题匹配；Judge 还要检查规则是否适用于当前区域、版本和现场条件，以及该文档是否真的蕴含 Claim。

### Q16：置信度怎么得到？

不使用模型自报分数。使用视觉质量、bbox 面积、全图/局部一致性、异构验证一致性、证据覆盖、规则权威性和冲突特征，在验证集上训练校准器，并用 ECE、Brier 和可靠性图验证。

### Q17：为什么高风险阈值更高？

不同错误代价不同。把车辆颜色说错和误发安全告警的成本不一样。通过 cost-sensitive policy 和 selective risk 曲线，为高风险 Claim 选择更低漏放率的工作点，并保留人工确认。

### Q18：单张图为什么不能判断停留？

单图只提供某个时刻的状态，没有时间跨度和位移信息。时序 Claim 必须有多帧、时间戳和 track；这是硬门控而不是 Prompt 建议。

### Q19：工具为什么最多两轮？

离线分析发现绝大多数可解问题通过“ROI 或检索”一到两次补证即可完成，继续调用边际信息增益很低，但成本和尾延迟明显增加。因此默认两轮，高风险冲突提前转人工。

### Q20：如何避免 Agent 死循环？

最大轮数、总时延/费用预算、工具参数去重、无新增证据停止、Claim coverage 无增益停止、单工具最大重试和状态机条件边共同保证结束。

### Q21：为什么使用 LangGraph 而不是 LangChain Agent？

项目需要显式状态、条件路由、补证循环和可恢复 Trace。LangGraph 更接近有状态工作流；开放式 Agent 更难保证轮数、幂等和确定性停止。业务规则仍然与框架解耦。

### Q22：如何处理模型输出不是合法 JSON？

使用 JSON Schema/structured output、低温度和受控重试；解析失败时尝试提取最外层 JSON，一次重试仍失败则视为无可靠输出，不把自由文本继续传递。

### Q23：如何防 Prompt Injection？

用户输入和知识文档都被当作数据，不允许修改 system policy；工具白名单、参数和权限在代码中；知识入库前清洗指令性内容；模型输出仍要经过 Schema 与 Policy。

### Q24：P95 为什么比平均延迟更重要？

平均值可能被大量快请求掩盖少数极慢请求，P95 更接近用户实际遇到的尾延迟。Agent 新增的 ROI、Judge 和检索容易拉长尾部，因此必须同时报告 P50、P95 和超时率。

### Q25：项目最大的技术难点是什么？

不是接模型 API，而是把自然语言结论转换成可验证 Claim，并建立视觉、规则、时序证据与下游决策之间的可执行依赖；同时在可靠性收益和调用成本之间取得平衡。

### Q26：如果让你重新做一次，会改什么？

第一，进一步扩大夜间、遮挡和分布外小目标数据；第二，用更严格的异构 Judge 数据减少相关性错误；第三，把 token 级索引做压缩和 GPU 批量重排；第四，引入线上人工反馈做持续校准和规则漂移监控。

---

## 14. 多模态模型与计算机视觉八股

### 14.1 典型 VLM 由什么组成

通常包括视觉编码器、模态连接器和语言模型：

```text
Image → ViT/CLIP Vision Encoder → Projector/Q-Former → LLM → Text
```

视觉编码器把图像转为 patch token；Projector 把视觉特征映射到 LLM embedding 空间；LLM 完成跨模态理解和生成。

### 14.2 Projector 和 Q-Former 的区别

- Projector 通常是线性层或 MLP，简单、参数少、训练稳定；
- Q-Former 使用可学习 query 从视觉特征中抽取信息，压缩能力更强但结构和训练更复杂；
- 本项目基座使用原模型的多模态连接方式，检索侧额外使用 MLP 将视觉 token 映射到 128 维检索空间。

### 14.3 为什么视觉 token 越多不一定越好

高分辨率会增加 token 数、显存、首 token 延迟和 attention 成本，也可能引入大量背景噪声。项目使用问题驱动 ROI，只在小目标证据不足时增加局部视觉 token。

### 14.4 多模态幻觉的来源

- 视觉信息分辨率不足；
- 语言先验压过视觉证据；
- 训练数据中描述偏置；
- 目标遮挡或分布外；
- Prompt 暗示；
- 图像事实与常识/规则混合；
- 解码阶段追求语言连贯性而补全不存在细节。

项目通过结构化 VisualFact、异构验证、拒答数据和硬门控缓解，而不是指望一个 Prompt 消除幻觉。

### 14.5 什么是小目标

没有唯一标准。COCO 常用面积小于 `32×32` 像素定义 small；本项目更关心目标面积占整图比例和有效 patch 数，使用 4% 作为触发局部复核的业务阈值，并在不同分辨率上重新标定。

### 14.6 IoU 是什么

```text
IoU = 预测框与真实框交集面积 / 并集面积
```

用于判断定位重叠程度，也是 NMS 和 mAP 评测的重要阈值。

### 14.7 NMS 是什么

将检测框按置信度排序，保留最高分框并删除与它 IoU 超过阈值的重复框。Soft-NMS 不直接删除，而是衰减重叠框分数。密集小目标场景需要谨慎设置阈值，避免相邻目标互相抑制。

### 14.8 mAP 是什么

AP 是某类别 Precision-Recall 曲线下面积；mAP 是多类别 AP 平均。`mAP@0.5` 使用 IoU 0.5，`mAP@0.5:0.95` 在多个 IoU 阈值上平均，更严格。

### 14.9 Precision、Recall、F1

```text
Precision = TP / (TP + FP)
Recall    = TP / (TP + FN)
F1        = 2PR / (P + R)
```

高风险巡检通常更关注 Recall 和漏放率，但过多误报会增加人工成本，因此阈值需要结合业务代价选择。

### 14.10 FPN 为什么有利于小目标

FPN 融合高层语义和低层高分辨率特征，在多个尺度上进行检测，使小目标可以使用空间分辨率更高的特征层。

### 14.11 SAHI 是什么

Slicing Aided Hyper Inference 将大图切成重叠小块分别检测，再把框映射回原图并合并。它能增加小目标在输入中的相对尺寸，但会增加推理次数并带来边界重复框。

### 14.12 ByteTrack 的核心思想

不仅关联高分检测框，也利用低分框恢复被遮挡目标，通过运动模型和 IoU 匹配维持轨迹。项目使用 track 和时间戳生成 TemporalEvidence，但轨迹分数不足或中断时不放行强时序结论。

### 14.13 OCR 为什么还要 VLM 复核

OCR 擅长字符识别，但不一定理解文字在场景中的语义和归属；VLM 可以结合标牌区域理解，二者交叉验证比单一路径更可靠。

### 14.14 全图和局部结果如何融合

不是简单平均分。两者一致时作为一致性特征；局部清晰度更高但丢失上下文，因此目标类别可以更多依赖局部，空间关系仍需全图。冲突时保留两项证据并交给 Judge/Policy。

---

## 15. RAG 与检索八股

### 15.1 RAG 的基本流程

文档解析 → 分块 → embedding → 建索引 → 查询召回 → rerank → 上下文构建 → LLM 生成 → 引用校验。

### 15.2 Sparse、Dense、Hybrid Retrieval

- Sparse/BM25：适合精确词、编号和专有名词；
- Dense：适合同义表达和语义相似；
- Hybrid：融合二者，兼顾精确匹配和语义召回。

### 15.3 BM25 的核心思想

在 TF-IDF 基础上加入词频饱和和文档长度归一化。一个词在文档里出现很多次时收益逐渐饱和，长文档不会仅因词多而获得过高分。

### 15.4 Bi-encoder、Cross-encoder、Late Interaction

| 方法 | 表示方式 | 优点 | 缺点 |
| --- | --- | --- | --- |
| Bi-encoder | 查询/文档各一个向量 | 快、易建 ANN 索引 | 细粒度信息损失 |
| Cross-encoder | 查询和文档联合编码 | 交互最充分、精度高 | 全库计算成本高 |
| Late Interaction | 查询/文档保留 token 向量 | 文档可离线编码，保留细粒度匹配 | 存储和重排成本较高 |

### 15.5 Recall@K

有至少一个正确文档出现在 Top-K 的查询比例。它衡量召回是否把答案候选带回来，不关心正确文档在 Top-K 中的具体排名。

### 15.6 MRR

对每个查询取第一个正确结果排名的倒数，再求平均。正确文档越靠前，MRR 越高。

### 15.7 nDCG

考虑结果的多级相关性和排名位置，用理想排序归一化，适合一个查询有多个不同相关等级文档的情况。

### 15.8 RRF 是什么

Reciprocal Rank Fusion 按不同检索器中的排名融合：

```text
RRF(d) = Σ 1 / (k + rank_i(d))
```

它不要求 BM25 与 dense 分数处于同一尺度，工程上稳定。

### 15.9 向量数据库和 FAISS 的区别

FAISS 是高性能向量索引库，负责 ANN 搜索；Milvus、Weaviate 等向量数据库还提供持久化、分片、元数据过滤和服务化能力。本项目粗索引可用 FAISS，规则元数据由 Elasticsearch/PostgreSQL 管理。

### 15.10 IVF、HNSW、PQ

- IVF：先聚类到倒排桶，只搜索部分桶；
- HNSW：基于多层近邻图搜索，召回高但内存大；
- PQ：把向量分段量化，降低存储和距离计算成本；
- 实际选择取决于数据量、更新频率、内存和延迟。

### 15.11 Chunk 太大或太小有什么问题

太大：包含无关内容，embedding 被稀释、上下文成本高；太小：规则条件与结论被拆开，语义不完整。项目按规则条款语义切分并保留元数据。

### 15.12 如何构造难负样本

- 同主题但规则条件不适用；
- 旧版本规则；
- 目标类别相似但处置不同；
- 文本匹配但区域不匹配；
- 图片相似但事件不同。

难负样本对训练 reranker 和 Judge 都很重要。

### 15.13 如何减少 RAG 幻觉

- 检索结果带来源、版本和适用范围；
- 生成只使用批准证据；
- Judge 检查引用是否蕴含 Claim；
- 检索为空时不允许生成规则结论；
- 输出后做 claim-citation 对齐检查。

### 15.14 知识库更新如何处理

文档按版本增量解析和编码；新版本生效后旧版本标记失效但保留审计；缓存键和索引包含版本；更新时双索引切换，避免部分文档新旧混用。

### 15.15 余弦相似度与点积有什么区别

余弦相似度只比较方向，等于点积除以两个向量模长；如果向量已经 L2 归一化，余弦相似度就等于点积。MaxSim 前对 token 向量归一化，便于稳定比较。

### 15.16 Reranker 为什么有效

粗召回模型为了全库速度通常压缩交互；Reranker 只处理少量候选，可以使用更丰富的 token 交互和规则元数据，因此能把“主题相关但条件不适用”的文档排到后面。

### 15.17 如何衡量检索延迟

分别记录 metadata filter、BM25、dense ANN、融合、Late Interaction 和结果组装耗时，同时报告 P50/P95。只报告端到端时间无法定位瓶颈。

---

## 16. Agent、LangGraph 与可靠性八股

### 16.1 ReAct 是什么

ReAct 让模型交替产生 Reasoning 和 Action，根据 Observation 再决定下一步。优点是灵活，缺点是开放循环、难控制成本。项目吸收“根据观察选择动作”的思想，但使用 LangGraph 和 Policy 限制循环。

### 16.2 Agent 与 Workflow 的区别

Workflow 的节点和路径大多预定义；Agent 在运行时根据状态选择动作。本项目是混合模式：骨架和安全策略固定，证据工具选择动态。

### 16.3 状态机的关键要素

状态、事件、转移条件、动作和终止状态。LangGraph 中 State 保存证据，Node 执行动作，Conditional Edge 决定转移，END 是终止状态。

### 16.4 为什么需要持久化 Checkpoint

长链路可能因模型超时或服务重启中断。Checkpoint 可以从最近节点恢复，避免重复 GPU 调用，也便于人工介入后继续执行。

### 16.5 工具调用如何保证安全

- JSON Schema 参数；
- 工具白名单；
- 最小权限；
- 参数边界；
- 超时和预算；
- 幂等键；
- 审计 Trace；
- 高权限动作不直接暴露给模型。

### 16.6 什么是幂等

同一请求执行一次或多次，最终效果一致。GET 天然更接近幂等；补拍请求等有副作用操作必须使用 action ID 防重。

### 16.7 重试为什么要指数退避和 jitter

指数退避逐步延长等待，避免持续打满故障服务；jitter 加随机扰动，避免大量客户端同时重试形成惊群。

### 16.8 熔断器的三个状态

- Closed：正常调用；
- Open：错误率超过阈值，直接快速失败；
- Half-open：允许少量探测请求，成功后恢复。

### 16.9 超时和取消如何传递

入口设置总 deadline，各节点获得剩余时间；子调用不能超过总 deadline；上游取消后下游异步任务也要取消，避免请求已经失败但 GPU 仍在运行。

### 16.10 可观测性三支柱

Logs、Metrics、Traces。项目尤其依赖分布式 Trace，因为需要还原每个 Claim 为什么调用工具、如何变化和为什么停止。

### 16.11 为什么 Trace 不能记录完整敏感 CoT

自由 CoT 可能包含敏感内容、不可控推断且不稳定。系统记录结构化 EvidencePlan、reason code、工具参数和 Claim 状态，既能审计又避免依赖私有推理文本。

### 16.12 如何评测 Agent 工具选择

对每条样本标注必要工具集合和允许替代路径，统计 Tool Precision/Recall、无效调用率、平均轮数和完成任务所需最小成本差距。

### 16.13 什么是 selective prediction

模型可以在不确定时 abstain。Coverage 表示系统回答多少样本，Risk 表示已回答样本的错误率。门控阈值是在 coverage 与 risk 之间做业务权衡。

### 16.14 为什么拒答率越高不一定越好

无脑拒答可以降低错误，但系统失去可用性。应同时看正确拒答率、误拒答率、coverage、回答准确率和高风险漏放率。

---

## 17. 模型训练与大模型八股

### 17.1 SFT 是什么

Supervised Fine-Tuning 使用输入—目标输出对，通过 teacher forcing 最小化下一 token 交叉熵，使预训练模型学会任务格式和领域行为。

### 17.2 LoRA 的原理

冻结原权重 `W`，只学习低秩增量：

```text
W' = W + ΔW = W + B A
```

其中秩 `r` 远小于原维度。训练参数和显存显著降低，多个任务可以维护不同 adapter。

### 17.3 `r` 和 `alpha` 的作用

`r` 控制低秩容量，越大表达能力越强但参数更多；`alpha/r` 通常决定 LoRA 更新缩放。项目使用 `r=16, alpha=32`，通过验证集选择而非越大越好。

### 17.4 QLoRA 是什么

把基础模型权重量化到 4bit，同时用 BF16/FP16 训练 LoRA adapter，进一步降低显存。常配合 NF4、double quantization 和 paged optimizer。

### 17.5 为什么训练用 BF16

BF16 与 FP32 指数位相同，动态范围大于 FP16，训练更稳定；尾数精度略低但大模型训练通常可接受。硬件支持时优先 BF16。

### 17.6 Gradient Accumulation

多次前向/反向累积梯度后再更新，相当于扩大有效 batch，但训练时间增加。有效 batch = micro batch × 累积步数 × GPU 数。

### 17.7 Gradient Checkpointing

前向不保存全部中间激活，反向时重新计算，以额外计算换显存。

### 17.8 DeepSpeed ZeRO

- ZeRO-1：切分优化器状态；
- ZeRO-2：再切分梯度；
- ZeRO-3：再切分参数；
- 阶段越高越省显存，但通信和工程复杂度也更高。

### 17.9 AdamW 为什么与 Adam 不同

AdamW 将 weight decay 与梯度更新解耦，更符合真正的 L2 正则化效果，是 Transformer 微调常用优化器。

### 17.10 Warmup 的作用

训练初期参数和优化器状态不稳定，直接使用大学习率容易发散。Warmup 从小学习率逐渐升高，再按 cosine 等策略下降。

### 17.11 如何防止过拟合

- 按任务划分避免泄漏；
- LoRA 而非全量微调；
- dropout、weight decay、早停；
- hard negative 和数据增强；
- 监控训练/验证 loss 与分层指标；
- 不重复采样相邻视频帧。

### 17.12 多任务数据如何配比

如果按原始数量随机采样，普通视觉问答会淹没拒答和 Judge 数据。项目按任务设置采样权重，并监控每类任务梯度和验证指标，保证安全行为不被主任务覆盖。

### 17.13 Temperature 对生成有什么影响

Temperature 越低，分布越尖锐、输出更确定。结构化事实和 Planner/Judge 使用较低温度；创意任务才需要高温。低温不能保证事实正确，只提高一致性。

### 17.14 Top-k 与 Top-p 解码

Top-k 只在概率最高的 k 个 token 中采样；Top-p 选择累计概率达到 p 的最小集合。结构化输出通常关闭采样或使用很低温度。

### 17.15 KV Cache 是什么

自回归生成中缓存历史 token 的 Key/Value，避免每一步重复计算。vLLM 使用 PagedAttention 改善 KV Cache 内存管理和批处理吞吐。

### 17.16 Self-Attention 公式

```text
Attention(Q,K,V) = softmax(QK^T / √d_k) V
```

Q 表示查询，K 表示被匹配的键，V 表示实际聚合的信息；除以 `√d_k` 防止维度大时点积过大导致 softmax 梯度过小。

### 17.17 Multi-Head Attention 的价值

不同头可以在不同子空间学习位置、语义、对象关系等模式，最后拼接投影。它不保证每个头都有明确人类语义，但比单头具有更强表达能力。

### 17.18 Causal Mask 是什么

Decoder 生成第 t 个 token 时不能看到未来 token，使用上三角 mask 屏蔽未来位置。SFT 的 teacher forcing 仍然遵守 causal mask，只是训练时一次并行计算全部位置。

### 17.19 RoPE 的基本思想

Rotary Position Embedding 通过旋转 Q/K 向量编码位置信息，使注意力点积自然包含相对位置。长上下文扩展需要关注频率缩放和分布外长度问题。

### 17.20 Cross Entropy Loss

对真实 token 的负对数概率求和。SFT 通常只对 assistant 输出位置计算 loss，用户输入和系统 Prompt 使用 label mask，避免模型学习复述输入。

### 17.21 FlashAttention 为什么更快

通过分块计算和 IO-aware 算法避免显式保存完整 attention 矩阵，减少 HBM 读写，计算结果与标准 attention 等价或近似等价，主要收益是显存和速度。

### 17.22 量化会带来什么影响

低比特量化减少显存和带宽，但可能损失精度，视觉 projector、embedding 和输出层通常更敏感。需要在小目标、结构化输出和 Judge 子集上单独回归，而不是只测通用问答。

---

## 18. 后端与系统设计八股

### 18.1 为什么模型服务与编排服务分离

GPU 推理和业务状态机的扩缩容、资源和故障模式不同。分离后可以独立扩容 VLM，编排服务保持轻量，也便于模型升级和灰度。

### 18.2 FastAPI 的 async 是否一定更快

只对 I/O 密集等待有帮助；CPU/GPU 密集任务不会因为 async 自动变快，应交给模型服务、线程池或任务队列。阻塞调用放在 event loop 会拖慢全部请求。

### 18.3 Redis 缓存穿透、击穿和雪崩

- 穿透：查询不存在数据，可用布隆过滤器或空值缓存；
- 击穿：热点 key 失效导致并发回源，可用互斥锁/逻辑过期；
- 雪崩：大量 key 同时失效，可加随机 TTL、多级缓存和限流。

### 18.4 数据库为什么用 PostgreSQL

规则元数据、Trace 和版本关系具有结构化查询需求，PostgreSQL 支持事务、JSONB 和索引；大图片放对象存储，数据库只保存 URI 和 hash。

### 18.5 乐观锁与悲观锁

乐观锁使用版本号检测冲突，适合冲突较少；悲观锁提前加锁，适合冲突高但会降低并发。Claim 状态更新可使用 version 字段做乐观并发控制。

### 18.6 消息队列什么时候使用

补拍、离线重建索引、人工复核和异步评测不需要阻塞在线请求，可通过消息队列解耦；实时回答主链路不应为了“用了 MQ”而全部异步化。

### 18.7 限流算法

- 固定窗口简单但边界突发；
- 滑动窗口更平滑；
- 漏桶固定出流速率；
- 令牌桶允许一定突发。GPU 模型 API 常用令牌桶并同时限制并发。

### 18.8 一致性哈希

把节点和 key 映射到哈希环，节点变化时只迁移邻近区间数据，适合缓存分片。可通过虚拟节点改善负载均衡。

### 18.9 CAP 如何理解

发生网络分区时，分布式系统只能在一致性和可用性之间取舍。规则版本和最终策略更偏一致性；模型结果缓存可以偏可用性，但必须携带版本避免使用错误规则。

### 18.10 如何灰度模型

按用户/任务 hash 分流，固定一部分流量到新模型；A/B 保持规则和数据一致；监控正确拒答、漏放、P95 和成本；异常自动回滚。模型、Prompt、Skill 和索引版本必须同时记录。

---

## 19. 压力追问与答题边界

### 19.1 “你这不就是堆组件吗？”

回答重点：每个组件都对应一个可验证失败模式，并通过消融证明独立贡献。ROI 解决小目标，FLMR 解决细粒度检索，Claim Graph/Judge 解决无依据回答，Policy 解决不可协商边界。不是组件越多越好，而是按证据缺口最小调用。

### 19.2 “为什么不直接换一个更大的 VLM？”

更大模型可能提高平均准确率，但不能解决规则版本、时序最低条件、证据追溯和权限问题，而且成本更高。系统架构与模型能力互补，升级模型不需要重写门控。

### 19.3 “Judge 和主模型同源，还叫独立吗？”

独立不是绝对统计独立，而是职责、上下文、Prompt、adapter 和训练数据切分独立。真正降低相关性风险依赖异构证据和硬策略，高风险不会因为两个模型一致就直接放行。

### 19.4 “这些阈值是不是拍脑袋？”

面积阈值来自按目标面积分桶的 accuracy-cost 曲线；置信阈值来自验证集 coverage-risk 曲线；最大轮数来自信息增益和延迟曲线。阈值会随模型、相机和场景重新标定。

### 19.5 “无依据回答率怎么定义？”

最终答案中存在至少一条事实性 Claim，无法绑定有效 evidence ID，或引用证据不能蕴含该 Claim，则该样本记为无依据回答。由双人标注和 Judge 辅助核验。

### 19.6 “正确拒答率和误拒答率有什么区别？”

- 正确拒答率：本应拒答的样本中，系统正确拒答的比例；
- 误拒答率：本来证据充分可回答的样本中，系统错误拒答的比例；
- 两者必须同时看，避免系统靠全部拒答获得安全指标。

### 19.7 “为什么 P95 增加还能说优化？”

优化目标不是单一延迟，而是在可靠性和时延之间做 Pareto 权衡。完整方案以 17.4% P95 增幅换来 8.5pt 检索提升、36.4% 无依据回答相对下降和 66.7% 高风险漏放下降，且仍满足业务 SLA。

### 19.8 “你个人负责什么？”

统一回答模板：

```text
我负责从问题定义到核心证据链路的设计与落地，重点 owner 三部分：领域数据与 LoRA/SFT 适配、token 级多模态检索方案，以及 Evidence Planner—Claim Graph—Judge—Policy 的 Agent 工作流；检测、无人机平台接口和领域规则标注由项目成员协作，我负责定义数据契约、接口和端到端评测。
```

Ownership 必须与真实分工一致，不要把团队工作全部说成个人完成。

---

## 20. 面试前一页速记

### 项目三个问题

1. 小目标在整图 VLM 中信息丢失；
2. 风险问题需要视觉事实 + 规则证据；
3. 模型会无依据推断，单图还会越界推断时序。

### 四层方案

1. 10k+ 数据 + Qwen2.5-VL-7B LoRA/SFT；
2. BM25/BGE 粗召回 + FLMR token Late Interaction；
3. Evidence Planner + Skill + 主动补证 Agent；
4. Claim Graph + Judge + Calibrator + Policy。

### 三条原则

- VLM 提取事实，不直接决定业务结论；
- 模型负责理解需要证明什么，代码负责没有证明不能放行；
- 模型一致不等于事实正确，高风险依赖异构证据和人工兜底。

### 核心参数

- 数据：10,640；训练/验证/测试：8,440/1,000/1,200；
- 基座：Qwen2.5-VL-7B；
- LoRA：r=16、alpha=32、dropout=0.05；
- 小目标阈值：4%；crop padding：20%；
- 粗召回 Top-200；Late Interaction rerank Top-50；最终 Top-5；
- 最大工具轮数：2；
- 风险阈值：0.75/0.82/0.90。

### 核心结果

- Recall@10：78.4% → 86.9%，+8.5pt；
- 小目标问答：71.8% → 78.4%，+6.6pt；
- 无依据回答：13.2% → 8.4%，相对下降 36.4%；
- 正确拒答：78.6% → 89.2%，+10.6pt；
- 高风险漏放：4.8% → 1.6%；
- P95：3.10s → 3.64s，+17.4%。

### 最关键案例

右下方 1.3% 面积车辆 → crop_region → 局部 VLM 确认 → FLMR 检索道路规则 → 可回答存在性与风险；因为只有单图，拒绝持续停留结论。

### 最后一句总结

> 项目价值不在于多调用几个模型，而在于把多模态问答从“生成一段看起来合理的话”改造成“对每条 Claim 建立证据、主动补齐缺口、通过校准与硬策略放行”的可解释决策系统。

---

## 21. 自测清单

面试前必须能够不看文档回答：

- [ ] 30 秒和 2 分钟介绍；
- [ ] 为什么直接 VLM + 普通 RAG 不够；
- [ ] Qwen2.5-VL 的训练数据、LoRA 参数和两阶段目标；
- [ ] FLMR 的查询组成和 MaxSim 公式；
- [ ] BM25、BGE、Late Interaction 各自负责什么；
- [ ] Claim、Evidence、Skill、Judge 和 Policy 的边界；
- [ ] 小目标 4%、padding 20% 和两轮工具预算的来源；
- [ ] 三个完整案例；
- [ ] A/B 基线、测试集规模和每个指标的计算口径；
- [ ] `pt` 与相对百分比的区别；
- [ ] P95 的定义和为什么允许增加 17.4%；
- [ ] 如何处理检测框错误、Judge 错误、规则过期和模型超时；
- [ ] 个人 ownership 与团队分工；
- [ ] 项目最失败的一类样本和下一步改进方向。

如果上面任何一项不能连续回答三层追问，就继续回到对应章节复习。
