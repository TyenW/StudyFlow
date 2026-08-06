# 🎓 StudyFlow — Academic Management & Obsidian PKM Assistant

[![OS Compatibility](https://img.shields.io/badge/OS-Linux%20%7C%20macOS%20%7C%20Windows-0078D4.svg?style=for-the-badge&logo=linux&logoColor=white)](https://github.com/)
[![Java Version](https://img.shields.io/badge/Java-11%2B-orange.svg?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Architecture](https://img.shields.io/badge/Dependencies-Zero%20External%20JARs-blue.svg?style=for-the-badge&logo=java&logoColor=white)](https://github.com/)
[![TUI & OAuth](https://img.shields.io/badge/Interface-ANSI%20TUI%20%2B%20OAuth2%20ServerSocket-success.svg?style=for-the-badge)](https://github.com/)
[![Integrations](https://img.shields.io/badge/Integrations-Canvas%20%7C%20Google%20Tasks%20%7C%20Obsidian-purple.svg?style=for-the-badge)](https://github.com/)
[![Roadmap AI](https://img.shields.io/badge/AI%20Module-Ollama%20RAG%20(Roadmap)-yellow.svg?style=for-the-badge&logo=ollama&logoColor=white)](https://github.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)](LICENSE)

> **StudyFlow** é uma solução completa de linha de comando (CLI/TUI) de altíssima performance desenvolvida em **Java 11+** para gestão acadêmica universitária e organização de conhecimento via **Obsidian (Segundo Cérebro / Personal Knowledge Management - PKM)**.
>
> Projetado sob a filosofia de **Zero Dependências Externas (100% Java Standard Library)**, o sistema integra em tempo real as APIs nativas do **Canvas LMS**, **Google Tasks (OAuth2)** e geradores de **Grafos Interativos de Conhecimento em HTML/Canvas**.
>
> ⚠️ *Nota sobre o módulo de IA (`OllamaClient`): O cliente HTTP e os prompts para integração com LLMs locais (Ollama / RAG) estão estruturados na arquitetura, porém o módulo consta atualmente como **Implementação Futura (Roadmap)** por limitações temporárias de VRAM do ambiente de desenvolvimento.*

---

## 📌 Sumário
- [Visão Geral & Problema Resolvido](#-visão-geral--problema-resolvido)
- [Arquitetura Geral & Fluxo de Dados](#-arquitetura-geral--fluxo-de-dados)
- [Funcionalidades Detalhadas por Módulo](#-funcionalidades-detalhadas-por-módulo)
  - [1. Gestão Acadêmica & Ciclo de Vida Universitário](#1-gestão-acadêmica--ciclo-de-vida-universitário)
  - [2. Obsidian & Segundo Cérebro (PKM Engine)](#2-obsidian--segundo-cérebro-pkm-engine)
  - [3. Motor de IA Acadêmica Local (Ollama & RAG - Roadmap Futuro)](#3-motor-de-ia-acadêmica-local-ollama--rag---roadmap-futuro)
  - [4. Integração REST API com Canvas LMS](#4-integração-rest-api-com-canvas-lms)
  - [5. Sincronização Bi-direcional Google Tasks (OAuth2 PKCE)](#5-sincronização-bi-direcional-google-tasks-oauth2-pkce)
  - [6. Visualizador de Grafo de Conhecimento Interativo](#6-visualizador-de-grafo-de-conhecimento-interativo)
  - [7. Ferramentas Integradas de Workflow & Segurança](#7-ferramentas-integradas-de-workflow--segurança)
- [Decisões de Engenharia & Filosofia do Projeto](#-decisões-de-engenharia--filosofia-do-projeto)
- [Compatibilidade Multiplataforma (Cross-Platform)](#-compatibilidade-multiplataforma-cross-platform)
- [Estrutura do Repositório](#-estrutura-do-repositório)
- [Guia de Instalação e Execução](#-guia-de-instalação-e-execução)
- [Guia de Configuração de Integrações](#-guia-de-configuração-de-integrações)
- [Demonstração da Interface (TUI)](#-demonstração-da-interface-tui)
- [Licença](#-licença)

---

## 🧠 Visão Geral & Problema Resolvido

No ambiente universitário atual, estudantes enfrentam uma **alta fragmentação de dados e ferramentas**:
- **Prazos e Entregas**: Dispersos entre portais EAD proprietários (Canvas LMS) e agendas pessoais.
- **Anotações de Aula**: Armazenadas em repositórios Markdown (Obsidian) sem indexação cruzada com matérias ativas.
- **Métricas de Desempenho**: Falta de visão preditiva sobre notas necessárias para aprovação e controle rigoroso de frequência/faltas.

O **StudyFlow** resolve esse problema atuando como um **Hub Centralizador CLI Multiplataforma**, unificando planejamento acadêmico, sincronização de dados de nuvem e inteligência de conhecimento local em uma única aplicação rápida, segura e sem overhead de memória.

---

## 🏗️ Arquitetura Geral & Fluxo de Dados

<table align="center" width="100%">
  <thead>
    <tr>
      <th colspan="2" align="center">🎓 StudyFlow — Arquitetura de Módulos &amp; Fluxo de Dados</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td width="50%" valign="top">
        <h4>🖥️ Core (Java Standard Library)</h4>
        <ul>
          <li><code>InteractiveMenu</code> &mdash; Engine TUI com navegação ANSI por setas</li>
          <li><code>Main Manager</code> &mdash; Gestão de Matérias, Prazos, Faltas e Notas</li>
          <li><code>ObsidianManager</code> &mdash; Motor de PKM, Metadados e Backlinks</li>
          <li><code>GraphGenerator</code> &mdash; Gerador de Grafo HTML Autônomo</li>
          <li><code>JsonParser &amp; FrontmatterParser</code> &mdash; Parsers Nativos</li>
        </ul>
      </td>
      <td width="50%" valign="top">
        <h4>🌐 Cloud Integrations</h4>
        <ul>
          <li><code>Canvas LMS REST API</code> &mdash; Sincronização de Tarefas e Módulos</li>
          <li><code>Google Tasks API</code> &mdash; OAuth2 PKCE via ServerSocket <code>:8080</code></li>
        </ul>
        <hr />
        <h4>🤖 Roadmap / Futura Implementação</h4>
        <ul>
          <li><code>Ollama Local AI &amp; RAG</code> &mdash; <i>(Inativo por limitação de VRAM)</i></li>
        </ul>
      </td>
    </tr>
    <tr>
      <td colspan="2" align="center">
        <h4>💾 Armazenamento Local &amp; Segundo Cérebro</h4>
        <p>
          <kbd>planejamento.json</kbd> &nbsp;&nbsp;&nbsp;⇄&nbsp;&nbsp;&nbsp; 
          <kbd>Obsidian Vault (.md)</kbd> &nbsp;&nbsp;&nbsp;➔&nbsp;&nbsp;&nbsp; 
          <kbd>.trash (Quarentena de Anexos Órfãos)</kbd>
        </p>
      </td>
    </tr>
  </tbody>
</table>

---

## 🔥 Funcionalidades Detalhadas por Módulo

### 1. Gestão Acadêmica & Ciclo de Vida Universitário
- **Motor de Planejamento Curricular**: Carregamento e validação relacional de arquivos `planejamento.json`, organizando matérias por períodos/semestres.
- **Ciclo de Vida Automatizado**:
  - `A Cursar`: Disciplinas planejadas.
  - `Cursando`: Transição ativa que gera automaticamente a estrutura física de diretórios no sistema de arquivos local.
  - `Concluída`: Arquivamento dinâmico de anotações e trabalhos finalizados.
- **Calculadora Preditiva de Desempenho**:
  - Cálculo de média ponderada conforme fórmula da instituição.
  - Estimativa exata da nota mínima necessária na prova final (P2 / Exame) para atingir a média de aprovação.
- **Monitoramento de Assiduidade (Faltas)**:
  - Controle de ausências registradas em relação ao limite máximo tolerado por carga horária (ex: 25% de faltas permitidas).
  - Alertas visuais ANSI graduados (Verde ➔ Amarelo ➔ Vermelho Crítico).
- **Dashboard "O que tenho para hoje?"**:
  - Consolidação central de tarefas pendentes do Canvas, Google Tasks e matérias locais, ordenadas por data de entrega e urgência.

---

### 2. Obsidian & Segundo Cérebro (PKM Engine)
- **Auditoria Inteligente de Frontmatter**: Varredura em profundidade de notas Markdown para identificar e listar arquivos sem metadados essenciais (`tags`, `created_date`, `aliases`).
- **Indexador & Gerenciador Global de Metadados**:
  - Renomeação e refatoração em lote de `tags` e `aliases` em todo o cofre Obsidian com atualização atômica de referências.
- **Diagnóstico da Redes de Anotações**:
  - Detecção de **links quebrados** (referências `[[Nota Inexistente]]` sem destino).
  - Identificação de **notas órfãs** (páginas sem conexões de entrada ou saída no grafo).
- **Higienização Segura de Anexos Órfãos**:
  - Varredura de mídias armazenadas em pastas de anexos (`.png`, `.jpg`, `.pdf`, `.svg`).
  - Comparação relacional com todo o corpo dos arquivos `.md`. Arquivos não referenciados são isolados na pasta `.trash`.
- **Gerador Sintático de Wikilinks**:
  - Análise lexicográfica dos títulos e apelidos das notas do cofre. Converte automaticamente termos correspondentes no texto em links bilaterais `[[Wikilinks]]`.
- **Sincronizador de Backlinks (Índice Reverso)**:
  - Injeção e atualização dinâmica de um bloco estruturado `## Linked Mentions` no final de cada anotação Markdown.

---

### 3. Motor de IA Acadêmica Local (Ollama & RAG - Roadmap Futuro)
> [!NOTE]
> **Status**: *Módulo em Roadmap / Futura Implementação*
>
> A infraestrutura cliente Java (`OllamaClient.java`) e a modelagem de prompts para resumos e RAG foram projetadas na arquitetura do sistema. O módulo encontra-se inativo no momento devido à limitação de VRAM de hardware para execução fluida de LLMs locais (ex: Llama3/Mistral).

- **Estrutura Planejada**:
  - Comunicação REST com Ollama local (`http://localhost:11434`).
  - Geração de resumos, flashcards e questionários acadêmicos.
  - **Vault Chat (RAG)**: Injeção de contexto de anotações do cofre em prompts de IA.

---

### 4. Integração REST API com Canvas LMS
- **Consumo Direto de Endpoints REST**:
  - Endpoints `/api/v1/courses`, `/api/v1/users/self/favorites/courses` e `/api/v1/planner/items`.
- **Sincronização de Tarefas e Prazos**:
  - Importação de atividades avaliativas, tarefas de envio e questionários com conversão de datas para o fuso horário local.
- **Acompanhamento de Entregas & Submissões**:
  - Leitura do status das submissões e notas atribuídas pelos docentes.
- **Download Inteligente de Módulos e Arquivos**:
  - Varredura da árvore de arquivos do curso no Canvas com download automatizado de PDFs, slides e listas para as pastas locais das matérias.

---

### 5. Sincronização Bi-direcional Google Tasks (OAuth2 PKCE)
- **Autenticação OAuth2 sem Servidor Externo**:
  - O **StudyFlow** inicializa um `java.net.ServerSocket` temporário na porta `8080` local.
  - Abre o navegador padrão via `java.awt.Desktop`, captura o `authorization_code` no callback do redirect `http://localhost:8080` e encerra a porta com segurança.
- **Gerenciamento de Tokens**: Troca e armazenamento seguro de `access_token` e `refresh_token`.
- **Sincronização em Tempo Real**: Envio de tarefas locais para a API do Google Tasks e pull de atualizações efetuadas no celular/web.

---

### 6. Visualizador de Grafo de Conhecimento Interativo
- **Geração de HTML Autônomo (`GraphGenerator.java`)**:
  - Parser nativo que lê o cofre Obsidian, constrói a matriz de adjacência de conexões entre notas e compila um arquivo `.html` visual.
- **Interface Interativa Web**:
  - Renderização gráfica com física de partículas (força de repulsão/atração) permitindo zoom, pan, destaque de nós e navegação visual no navegador sem necessidade de plugins externos.

---

### 7. Ferramentas Integradas de Workflow & Segurança
- **Assistente Git Incorporado**:
  - Execução gerenciada de comandos de controle de versão (`git status`, wizard de commit formatado e `git push`) diretamente pelo menu da aplicação.
- **Exportador Sanitizado de Projeto (ZIP Cleaner)**:
  - Criação de pacote `.zip` para envio acadêmico ou portfólio.
  - **Sanitização Automática**: Omite arquivos contendo credenciais sensíveis (`credentials.json`, `token.json`), histórico do `.git`, diretórios temporários e bytecode `.class`.

---

## 💡 Decisões de Engenharia & Filosofia do Projeto

### 1. Filosofia "Zero Third-Party Dependencies"
Para garantir um executável extremamente leve e imune a vulnerabilidades na cadeia de suprimentos (*supply chain attacks*), o **StudyFlow** **não utiliza nenhuma biblioteca externa (JARs)**.
- **JSON Parser Customizado (`JsonParser.java`)**: Parser recursivo leve para objetos, listas, strings e números em JSON.
- **Frontmatter Parser Customizado (`FrontmatterParser.java`)**: Extrator de metadados YAML delimitados por `---` em arquivos Markdown.
- **HTTP Client Standard (`java.net.http.HttpClient`)**: Requisições assíncronas e síncronas nativas do Java 11+.

### 2. Baixo Consumo de Recursos
- **Tempo de Inicialização**: < 50ms.
- **Uso de Memória RAM**: ~25 MB a ~40 MB durante a execução.

---

## 💻 Compatibilidade Multiplataforma (Cross-Platform)

O **StudyFlow** é totalmente compatível com **Linux**, **macOS** e **Windows**:

| Recurso | Windows | Linux / macOS |
| :--- | :--- | :--- |
| **Renderização ANSI (Cores & Estilos)** | Suportado nativamente no Windows Terminal e PowerShell (`chcp 65001`) | Suportado nativamente nos terminais POSIX (Bash/Zsh) |
| **Leitura de Teclado Interativa** | Suportado via fallback de buffer estendido | Suportado via controle de entrada do terminal |
| **Callback OAuth2 (`ServerSocket`)** | Escuta em `http://localhost:8080` com liberação de porta | Escuta em `http://localhost:8080` sem necessidade de root |
| **Abertura de Navegador** | `java.awt.Desktop` / Comando do Sistema | `java.awt.Desktop` / `xdg-open` / `open` |

---

## 📂 Estrutura do Repositório

```
StudyFlow/
├── Cursar.java                   # Entrypoint principal (Menu Raiz e Shutdown Hooks)
├── Makefile                      # Script de automação cross-platform (Windows/Linux/macOS)
├── config/                       # Módulos de regras de negócio e infraestrutura
│   ├── Main.java                 # Gestor Acadêmico (Notas, Faltas, Tarefas, Git Wizard)
│   ├── ObsidianManager.java      # Motor do Segundo Cérebro (Auditoria, Links, Cleaner)
│   ├── CanvasManager.java        # Cliente REST API para integração Canvas LMS
│   ├── GoogleTasksManager.java   # Cliente OAuth2 PKCE & Google Tasks API
│   ├── OllamaClient.java         # Cliente HTTP para LLMs (Módulo em Roadmap / Futuro)
│   ├── GraphGenerator.java       # Gerador do Grafo Interativo HTML/Canvas
│   ├── InteractiveMenu.java      # Engine de Terminal TUI com navegação ANSI
│   ├── FileManager.java          # Gerenciamento de operações de I/O em arquivos
│   ├── FrontmatterParser.java    # Parser de metadados YAML em notas Markdown
│   ├── JsonParser.java           # Parser JSON nativo recursivo
│   ├── FolderManager.java       # Gerenciador da estrutura de diretórios das matérias
│   ├── Installer.java            # Setup interativo e validação do ambiente
│   ├── Subject.java              # Entidade de domínio representando uma Disciplina
│   └── SubjectJson.java          # DTO de serialização/deserialização de matérias
└── readme.md                     # Documentação oficial do projeto
```

---

## 🚀 Guia de Instalação e Execução

### Pré-requisitos
- **Java Development Kit (JDK)**: Versão 11 ou superior ([OpenJDK 17+](https://adoptium.net/) ou JDK 21 recomendados).
- **Git**: (Opcional) Para utilizar o assistente de controle de versão integrado.

### 1. No Windows (PowerShell / Prompt de Comando)

Você pode utilizar os scripts auxiliares incluídos no repositório:

```powershell
# Executar a aplicação (compila e roda automaticamente)
.\build.ps1

# Apenas compilar os fontes
.\build.ps1 compile

# Limpar os arquivos .class gerados
.\build.ps1 clean
```

*(Ou usando `.\build.bat` no Prompt de Comando CMD).*

### 2. No Linux / macOS (ou Windows com Make)

```bash
# Compilar todos os arquivos Java
make compile

# Executar a aplicação
make run

# Limpar arquivos compilados (.class)
make clean
```

### 3. Comandos Nativos Diretos (Qualquer Terminal)

```bash
# Compilação
javac -encoding UTF-8 Cursar.java config/*.java

# Execução
java "-Dfile.encoding=UTF-8" Cursar
```

---

## ⚙️ Guia de Configuração de Integrações

### Configuração do Canvas LMS
1. No Canvas LMS, acesse **Account ➔ Settings**.
2. Clique em **+ New Access Token**.
3. No **StudyFlow**, selecione a opção `📥 Importar tarefas do Canvas` e insira a URL da sua instituição e o token gerado.

### Configuração do Google Tasks API
1. Acesse o [Google Cloud Console](https://console.cloud.google.com/).
2. Crie um projeto e ative a **Google Tasks API**.
3. Crie credenciais do tipo **OAuth 2.0 Client ID (Desktop App)**.
4. Ao selecionar `⚙️ Configurar Google Tasks` no **StudyFlow**, informe o `Client ID` e `Client Secret`. O navegador abrirá automaticamente para autorização.

### Configuração da IA Local (Ollama - Roadmap Futuro)
> [!NOTE]
> A integração com Ollama está mapeada em `OllamaClient.java` como um recurso para versões futuras (exigirá instalação local do Ollama e GPU com VRAM dedicada).

---

## 🖥️ Demonstração da Interface (TUI)

```text
==================================================
  🧠 SEGUNDO CÉREBRO & GESTÃO STUDYFLOW
==================================================
  > 🎓 Ir para o Cursar Normal (Faculdade)
    🧠 Gerenciar Obsidian (Segundo Cérebro)
    📦 Gerar ZIP (Preparar Projeto para Envio)
    ❌ Sair
==================================================
[Use as setas ↑ ↓ e pressione ENTER para confirmar]
```

---

## 📄 Licença

Este projeto é distribuído sob a licença **MIT**. Consulte o arquivo `LICENSE` para obter mais detalhes. Sinta-se à vontade para utilizar o código em seus próprios estudos e portfólio.
