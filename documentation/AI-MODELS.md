# AI Models

JAIDoc is designed to work entirely with **local AI models** — no cloud APIs required. The project is developed and
tested against the Llama.cpp Server in routing mode, which dynamically selects the best model for each query based on
complexity.

## AI Software Server

JAIDoc uses **llama.cpp Server** in routing mode to dynamically select the best model for each query based on
complexity. It is built on Linux Debian Forky with CPU, CUDA, and Vulkan support.

| Component | Technology       | Version | Date       |
|-----------|------------------|---------|------------|
| Server    | llama.cpp Server | b10199  | 2026-07-30 |

## Hardware

### Previous Hardware (Retired)

| Component     | Specification                          |
|---------------|----------------------------------------|
| CPU           | Intel Core Ultra 9 275HX               |
| RAM           | 32 GB DDR5-6400                        |
| GPU 1 (local) | NVIDIA RTX 5070 Ti 12GB Mobile         |
| GPU 2 (eGPU)  | NVIDIA RTX 3090 24GB via Thunderbolt 4 |

### Actual Server

| Component             | Specification                        |                       Buy Link                       |                    Notes                     |
|-----------------------|--------------------------------------|:----------------------------------------------------:|:--------------------------------------------:|
| Motherboard           | Machinist X99 MD8 Dual Intel CPU     | https://es.aliexpress.com/item/1005009718980845.html |                                              |
| CPU                   | 2 x E5 2680 V4                       |      https://www.amazon.com/dp/B0D8VS42T8?th=1       |                                              |
| RAM                   | 2 x 16GB DDR4-2400 ECC               |   https://es.aliexpress.com/item/33002249520.html    |                                              |
| GPU 1 - PCI-E 3.0 16X | NVIDIA RTX 3090 24GB                 |                                                      |                                              |
| GPU 2 - PCI-E 3.0 8X  | NVIDIA RTX 3060 12GB                 |                                                      | Motherboard space limitation force to use 8x |
| PSU 1                 | MSI 1,000 Watts                      |      https://www.amazon.com/dp/B0FJZHR289?th=1       |                                              |
| PSU 2                 | MSI 750 Watts                        |      https://www.amazon.com/dp/B0FJZHMSV9?th=1       |                                              |
| VRAM                  | 36 GB (layer split across both GPUs) |                                                      |                                              |

#### Server Benchmark

```
Working in /mnt/DATA-AI/llama/llama.cpp
Using CUDA_SCALE_LAUNCH_QUEUES = 4x
Using GGML_CUDA_ENABLE_UNIFIED_MEMORY = true
Using GGML_CUDA_FA_ALL_QUANTS = true
Executing Benchmark...

ggml_cuda_init: found 2 CUDA devices (Total VRAM: 36029 MiB):
  Device 0: NVIDIA GeForce RTX 3090, compute capability 8.6, VMM: yes, VRAM: 24123 MiB
  Device 1: NVIDIA GeForce RTX 3060, compute capability 8.6, VMM: yes, VRAM: 11906 MiB
ggml_vulkan: Found 2 Vulkan devices:
ggml_vulkan: 0 = NVIDIA GeForce RTX 3060 (NVIDIA) | uma: 0 | fp16: 1 | bf16: 1 | fp4: 0 | warp size: 32 | shared memory: 49152 | int dot: 1 | matrix cores: NV_coopmat2
ggml_vulkan: 1 = NVIDIA GeForce RTX 3090 (NVIDIA) | uma: 0 | fp16: 1 | bf16: 1 | fp4: 0 | warp size: 32 | shared memory: 49152 | int dot: 1 | matrix cores: NV_coopmat2
```

| model                           |      size |  params | backend          | threads | n_batch | n_ubatch | type_k | type_v | fa | ts        |   test |              t/s |
|---------------------------------|----------:|--------:|------------------|--------:|--------:|---------:|-------:|-------:|---:|-----------|-------:|-----------------:|
| qwen35moe 35B.A3B Q5_K - Medium | 24.70 GiB | 34.66 B | CUDA,Vulkan,BLAS |      14 |     512 |      512 |   q8_0 |   q8_0 |  1 | 8.00/2.00 | pp8192 |  3487.07 ± 22.81 |
| qwen35moe 35B.A3B Q5_K - Medium | 24.70 GiB | 34.66 B | CUDA,Vulkan,BLAS |      14 |     512 |      512 |   q8_0 |   q8_0 |  1 | 8.00/2.00 |  tg256 |    109.29 ± 0.58 |
| qwen35moe 35B.A3B Q5_K - Medium | 24.70 GiB | 34.66 B | CUDA,Vulkan,BLAS |      14 |     512 |     1024 |   q8_0 |   q8_0 |  1 | 8.00/2.00 | pp8192 |  3210.06 ± 60.20 |
| qwen35moe 35B.A3B Q5_K - Medium | 24.70 GiB | 34.66 B | CUDA,Vulkan,BLAS |      14 |     512 |     1024 |   q8_0 |   q8_0 |  1 | 8.00/2.00 |  tg256 |    108.05 ± 0.25 |
| qwen35moe 35B.A3B Q5_K - Medium | 24.70 GiB | 34.66 B | CUDA,Vulkan,BLAS |      14 |     512 |     2048 |   q8_0 |   q8_0 |  1 | 8.00/2.00 | pp8192 | 3106.74 ± 147.08 |
| qwen35moe 35B.A3B Q5_K - Medium | 24.70 GiB | 34.66 B | CUDA,Vulkan,BLAS |      14 |     512 |     2048 |   q8_0 |   q8_0 |  1 | 8.00/2.00 |  tg256 |    108.35 ± 0.30 |
| qwen35moe 35B.A3B Q5_K - Medium | 24.70 GiB | 34.66 B | CUDA,Vulkan,BLAS |      14 |    1024 |      512 |   q8_0 |   q8_0 |  1 | 8.00/2.00 | pp8192 |  3117.95 ± 14.72 |
| qwen35moe 35B.A3B Q5_K - Medium | 24.70 GiB | 34.66 B | CUDA,Vulkan,BLAS |      14 |    1024 |      512 |   q8_0 |   q8_0 |  1 | 8.00/2.00 |  tg256 |    108.03 ± 0.43 |
| qwen35moe 35B.A3B Q5_K - Medium | 24.70 GiB | 34.66 B | CUDA,Vulkan,BLAS |      14 |    1024 |     1024 |   q8_0 |   q8_0 |  1 | 8.00/2.00 | pp8192 | 3673.83 ± 108.23 |
| qwen35moe 35B.A3B Q5_K - Medium | 24.70 GiB | 34.66 B | CUDA,Vulkan,BLAS |      14 |    1024 |     1024 |   q8_0 |   q8_0 |  1 | 8.00/2.00 |  tg256 |    107.94 ± 0.31 |
| qwen35moe 35B.A3B Q5_K - Medium | 24.70 GiB | 34.66 B | CUDA,Vulkan,BLAS |      14 |    1024 |     2048 |   q8_0 |   q8_0 |  1 | 8.00/2.00 | pp8192 |  3727.30 ± 32.15 |
| qwen35moe 35B.A3B Q5_K - Medium | 24.70 GiB | 34.66 B | CUDA,Vulkan,BLAS |      14 |    1024 |     2048 |   q8_0 |   q8_0 |  1 | 8.00/2.00 |  tg256 |    107.98 ± 0.21 |
| qwen35moe 35B.A3B Q5_K - Medium | 24.70 GiB | 34.66 B | CUDA,Vulkan,BLAS |      14 |    2048 |      512 |   q8_0 |   q8_0 |  1 | 8.00/2.00 | pp8192 |   3055.55 ± 7.77 |
| qwen35moe 35B.A3B Q5_K - Medium | 24.70 GiB | 34.66 B | CUDA,Vulkan,BLAS |      14 |    2048 |      512 |   q8_0 |   q8_0 |  1 | 8.00/2.00 |  tg256 |    107.91 ± 0.38 |
| qwen35moe 35B.A3B Q5_K - Medium | 24.70 GiB | 34.66 B | CUDA,Vulkan,BLAS |      14 |    2048 |     1024 |   q8_0 |   q8_0 |  1 | 8.00/2.00 | pp8192 | 3566.20 ± 192.19 |
| qwen35moe 35B.A3B Q5_K - Medium | 24.70 GiB | 34.66 B | CUDA,Vulkan,BLAS |      14 |    2048 |     1024 |   q8_0 |   q8_0 |  1 | 8.00/2.00 |  tg256 |    107.96 ± 0.37 |
| qwen35moe 35B.A3B Q5_K - Medium | 24.70 GiB | 34.66 B | CUDA,Vulkan,BLAS |      14 |    2048 |     2048 |   q8_0 |   q8_0 |  1 | 8.00/2.00 | pp8192 | 3887.09 ± 172.76 |
| qwen35moe 35B.A3B Q5_K - Medium | 24.70 GiB | 34.66 B | CUDA,Vulkan,BLAS |      14 |    2048 |     2048 |   q8_0 |   q8_0 |  1 | 8.00/2.00 |  tg256 |    108.04 ± 0.15 |

#### Extra hardware

| Name                               | Buy Link                                             |
|------------------------------------|------------------------------------------------------|
| 12VHPWR Cable Extension            | https://www.amazon.com/dp/B0C4176N2F?th=1            |
| PC Power Button                    | https://www.amazon.com/dp/B0F4DK1KBL                 |
| E-ATX Open Chassis Case Rack       | https://www.amazon.com/dp/B0DDXBRJZ3                 |
| 120Hz OLED Gaming Portable Monitor | https://es.aliexpress.com/item/1005012583492427.html |

## Models

### Model Info

Cache KV use Q8_0 quantization

| Model                       | Quantization | Context Size | Engine | URL                                                                     |
|-----------------------------|:------------:|:------------:|:------:|-------------------------------------------------------------------------|
| Ternary-Bonsai-27B-gguf     |     Q2_0     |  256K (MAX)  | VULKAN | https://huggingface.co/prism-ml/Ternary-Bonsai-27B-gguf                 |
| Qwopus3.6-35B-A3B-Coder-MTP |    Q5_K_M    |  256K (MAX)  |  CUDA  | https://huggingface.co/Jackrong/Qwopus3.6-35B-A3B-Coder-MTP-GGUF        |
| Mellum2-12B-A2.5B           |    Q4_K_M    |  128K (MAX)  |  CUDA  | https://huggingface.co/JetBrains/Mellum2-12B-A2.5B-Thinking-GGUF-Q4_K_M |
| Ornith-1.0-9B               |  UD-Q8_K_XL  |  256K (MAX)  |  CUDA  | https://huggingface.co/unsloth/Ornith-1.0-9B-GGUF                       |
| Ornith-1.0-35B              |  UD-Q5_K_XL  |  256K (MAX)  |  CUDA  | https://huggingface.co/unsloth/Ornith-1.0-35B-GGUF                      |
| gemma-4-12B-it              |  UD-Q8_K_XL  |  128K (MAX)  |  CUDA  | https://huggingface.co/unsloth/gemma-4-12b-it-GGUF                      |
| gemma-4-26B-A4B-it          |  UD-Q5_K_XL  |  256K (MAX)  |  CUDA  | https://huggingface.co/unsloth/gemma-4-26B-A4B-it-GGUF                  |
| gemma-4-31B-it              |  UD-Q5_K_XL  |  256K (MAX)  |  CUDA  | https://huggingface.co/unsloth/gemma-4-31B-it-GGUF                      |
| Phi-4-mini-reasoning        |  UD-Q8_K_XL  |  128K (MAX)  |  CUDA  | https://huggingface.co/unsloth/Phi-4-mini-reasoning-GGUF                |

## AI Agents

The project is developed using multiple AI coding agents, each with different strengths:

| Agent                 | IDE / Platform | Purpose                                                                       |
|-----------------------|----------------|-------------------------------------------------------------------------------|
| Claude Code           | Terminal       | Primary agent — deep research, complex refactors, and architectural decisions |
| IntelliJ AI Assistant | IntelliJ IDEA  | Inline code completion, quick suggestions, and minor fixes within the editor  |
| Junie                 | Terminal       | Alternative agent for comparison — experimental use and secondary opinions    |
