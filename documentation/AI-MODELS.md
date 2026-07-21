# AI Models

JAIDoc is designed to work entirely with **local AI models** — no cloud APIs required. The project is developed and
tested against the Llama.cpp Server in routing mode, which dynamically selects the best model for each query based on
complexity.

## Hardware

### Old Server

| Component     | Specification                          |
|---------------|----------------------------------------|
| CPU           | Intel Core Ultra 9 275HX               |
| RAM           | 32 GB DDR5-6400                        |
| GPU 1 (local) | NVIDIA RTX 5070 Ti 12GB Mobile         |
| GPU 2 (eGPU)  | NVIDIA RTX 3090 24GB via Thunderbolt 4 |

### Actual Server

| Component             | Specification                        |
|-----------------------|--------------------------------------|
| Motherboard           | Machinist X99 MD8 Dual CPU           |
| CPU                   | 2 x E5 2680 V4                       |
| RAM                   | 2 x 16GB DDR4-2400 ECC               |
| GPU 1 - PCI-E 3.0 16X | NVIDIA RTX 3090 24GB                 |
| GPU 2 - PCI-E 3.0 16X | NVIDIA RTX 3060 12GB                 |
| VRAM                  | 36 GB (layer split across both GPUs) |
| PSU 1                 | MSI 1,000 Watts                      |
| PSU 2                 | MSI 750 Watts                        |

## AI Software Server

JAIDoc uses **llama.cpp Server** in routing mode to dynamically select the best model for each query based on
complexity. Is compiled in Linux Debian Forky with CPU, CUDA and VULKAN

| Component | Technology       | Version | Date       |
|-----------|------------------|---------|------------|
| Server    | llama.cpp Server | b10069  | 2026-07-20 |

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
