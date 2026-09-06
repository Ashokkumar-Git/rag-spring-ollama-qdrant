# Local RAG with Spring AI, Ollama Docker & Qdrant

A local Retrieval-Augmented Generation (RAG) application built using:

* Java 21
* Spring Boot 3.x
* Spring AI 1.1.x
* Ollama running in Docker
* Qdrant running in Docker
* Maven

The application retrieves relevant information from local documents using vector similarity search and sends the retrieved context to a local Ollama LLM to generate an answer.

---

## 1. Architecture

```text
                    ┌──────────────────────────┐
                    │       User / Client      │
                    └────────────┬─────────────┘
                                 │
                                 │ GET /api/rag/ask
                                 ▼
                    ┌──────────────────────────┐
                    │ Spring Boot Application  │
                    │                          │
                    │ Java 21                  │
                    │ Spring AI                │
                    └───────┬─────────┬────────┘
                            │         │
                 Similarity │         │ LLM
                   Search   │         │ Request
                            ▼         ▼
                    ┌──────────┐   ┌──────────────┐
                    │ Qdrant   │   │ Ollama       │
                    │          │   │ Docker       │
                    │ Vectors  │   │              │
                    │ + Text   │   │ qwen3        │
                    │ + Meta   │   │              │
                    └──────────┘   │ nomic-embed  │
                                   └──────────────┘
```

### Local services

```text
Existing Local Ollama
    localhost:11434
        │
        │ completely independent
        │
Docker
 ├── Ollama
 │    localhost:11435
 │
 └── Qdrant
      localhost:6333
      localhost:6334
```

The existing Ollama installation on the computer is **not affected**.

The RAG application uses the Docker Ollama instance on port `11435`.

---

# 2. How RAG Works

The application follows this flow:

```text
User Question
      │
      ▼
Create Query Embedding
      │
      ▼
Search Qdrant
      │
      ▼
Retrieve Top Relevant Documents
      │
      ▼
Build Prompt with Context
      │
      ▼
Send Prompt to Ollama
      │
      ▼
Generate Answer
      │
      ▼
Return Answer
```

For example:

```text
Question:
How many paid leave days are available?

        ↓

Embedding

        ↓

Qdrant similarity search

        ↓

Relevant company policy chunks

        ↓

Ollama qwen3

        ↓

Answer:
Employees are eligible for 24 paid leave days per year.
```

---

# 3. Technologies

| Technology      | Purpose                         |
| --------------- | ------------------------------- |
| Java 21         | Application development         |
| Spring Boot 3.x | Backend framework               |
| Spring AI       | AI/RAG integration              |
| Ollama          | Local LLM and embeddings        |
| Qdrant          | Vector database                 |
| Docker          | Container runtime               |
| Maven           | Build and dependency management |

---

# 4. Prerequisites

Install:

* Java 21
* Maven
* Docker Desktop

You do **not** need to install another Ollama instance on your computer.

The RAG project runs its own Ollama instance inside Docker.

---

# 5. Existing Local Ollama

If Ollama is already installed locally, keep it running on:

```text
http://localhost:11434
```

You can check your existing local Ollama models with:

```bash
ollama list
```

This installation is independent from the Docker Ollama used by this project.

---

# 6. Docker Ollama

The project uses a separate Ollama Docker container.

The Docker Ollama is exposed on:

```text
http://localhost:11435
```

The container itself continues to use Ollama's internal port:

```text
11434
```

The mapping is:

```text
localhost:11435 → Docker Ollama:11434
```

This allows both Ollama instances to run at the same time.

---

# 7. Docker Compose

Create:

```text
docker-compose.yml
```

```yaml
services:

  ollama:
    image: ollama/ollama:latest
    container_name: rag-ollama
    ports:
      - "11435:11434"
    volumes:
      - ollama_rag_data:/root/.ollama
    restart: unless-stopped

  qdrant:
    image: qdrant/qdrant:latest
    container_name: rag-qdrant
    ports:
      - "6333:6333"
      - "6334:6334"
    volumes:
      - qdrant_data:/qdrant/storage
    restart: unless-stopped

volumes:
  ollama_rag_data:
  qdrant_data:
```

Start the containers:

```bash
docker compose up -d
```

Check:

```bash
docker ps
```

You should see:

```text
rag-ollama
rag-qdrant
```

---

# 8. Check Docker Ollama

Check the Docker Ollama version:

```bash
docker exec -it rag-ollama ollama --version
```

Check installed models:

```bash
docker exec -it rag-ollama ollama list
```

This is different from:

```bash
ollama list
```

The first command checks the **Docker Ollama**.

The second command checks your **existing local Ollama**.

---

# 9. Install Models in Docker Ollama

Pull the chat model:

```bash
docker exec -it rag-ollama ollama pull qwen3
```

Pull the embedding model:

```bash
docker exec -it rag-ollama ollama pull nomic-embed-text
```

Check:

```bash
docker exec -it rag-ollama ollama list
```

Expected:

```text
NAME
qwen3
nomic-embed-text
```

The models are stored in:

```text
ollama_rag_data
```

Docker volume.

Therefore, restarting the container does not require downloading the models again.

---

# 10. Docker Ollama and Local Ollama Are Separate

You can have both running:

```text
Local Ollama
    ↓
localhost:11434

Docker Ollama
    ↓
localhost:11435
```

For example:

```bash
ollama list
```

might show your existing models.

While:

```bash
docker exec -it rag-ollama ollama list
```

shows only the models used by this RAG project.

They do not share their model storage.

---

# 11. Optional Ollama Web UI

The Ollama Docker image itself does not provide a full model-management web UI.

If a browser-based UI is required, Open WebUI can be added as another Docker container.

Example architecture:

```text
Browser
   │
   ▼
Open WebUI
   │
   ▼
Docker Ollama
localhost:11435
```

This is optional and is not required for the Spring AI RAG application.

---

# 12. Qdrant

Qdrant is used as the vector database.

Ports:

```text
REST API:
http://localhost:6333

gRPC:
localhost:6334

Dashboard:
http://localhost:6333/dashboard
```

Check collections:

```bash
curl http://localhost:6333/collections
```

Check the RAG collection:

```bash
curl http://localhost:6333/collections/company_documents
```

---

# 13. Important: Persistent Qdrant Storage

Qdrant uses a Docker volume:

```yaml
volumes:
  - qdrant_data:/qdrant/storage
```

Therefore, vector data survives:

```bash
docker compose stop
docker compose start
```

and:

```bash
docker compose down
docker compose up -d
```

Do **not** use:

```bash
docker compose down -v
```

unless you intentionally want to delete the Qdrant data.

`down -v` removes the Docker volumes containing the stored vectors.

---

# 14. Project Structure

```text
local-rag/
│
├── docker-compose.yml
├── pom.xml
├── README.md
│
└── src/
    └── main/
        ├── java/
        │   └── com/example/rag/
        │       ├── RagApplication.java
        │       │
        │       ├── controller/
        │       │   └── RagController.java
        │       │
        │       └── service/
        │           ├── RagService.java
        │           └── DocumentIngestionService.java
        │
        └── resources/
            ├── application.yml
            │
            └── documents/
                └── company-policy.txt
```

---

# 15. Maven Configuration

Example `pom.xml` dependencies:

```xml
<dependencies>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-ollama</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-vector-store-qdrant</artifactId>
    </dependency>

</dependencies>
```

Use a Spring AI 1.1.x release compatible with your Spring Boot 3.x version.

---

# 16. Application Configuration

Because Spring Boot is running directly on the computer, it connects to the Docker containers through their exposed host ports.

`application.yml`:

```yaml
spring:

  ai:

    ollama:
      base-url: http://localhost:11435

      chat:
        model: qwen3

      embedding:
        model: nomic-embed-text

    vectorstore:
      qdrant:
        host: localhost
        port: 6334
        collection-name: company_documents
        initialize-schema: true
        use-tls: false
```

Important:

```yaml
base-url: http://localhost:11435
```

Do not use:

```yaml
base-url: http://localhost:11434
```

for this RAG project because `11434` belongs to your existing local Ollama installation.

---

# 17. Document Ingestion

The document is located at:

```text
src/main/resources/documents/company-policy.txt
```

Example:

```text
Company Leave Policy

Employees are eligible for 24 paid leave days per year.

Employees can request casual leave and earned leave.

Leave should normally be requested at least two days in advance.

Emergency leave can be requested through the employee's manager.
```

The ingestion process is:

```text
company-policy.txt
       ↓
TextReader
       ↓
Text Splitter
       ↓
Document Chunks
       ↓
Embedding Model
       ↓
Qdrant
```

---

# 18. Important: Do Not Re-index Every Application Restart

Do not blindly use:

```java
@PostConstruct
public void loadDocuments() {
    vectorStore.add(chunks);
}
```

for a production-style implementation.

Why?

Every time Spring Boot starts:

```text
Application starts
       ↓
Read document
       ↓
Create chunks
       ↓
Create embeddings
       ↓
Add vectors to Qdrant
```

This can create duplicate vectors.

---

# 19. Recommended Document Ingestion

Use a separate ingestion operation:

```text
POST /api/documents/ingest
```

The process should be:

```text
Document
   ↓
Calculate SHA-256 hash
   ↓
Check existing document metadata
   ↓
Is hash unchanged?
   │
   ├── YES → Skip indexing
   │
   └── NO
        ↓
      Delete old chunks
        ↓
      Create new chunks
        ↓
      Generate embeddings
        ↓
      Store in Qdrant
        ↓
      Store new hash
```

This means restarting Spring Boot does not automatically re-index the document.

---

# 20. RAG Question API

The application exposes:

```text
GET /api/rag/ask
```

Example:

```text
http://localhost:8080/api/rag/ask?question=How many paid leave days are available?
```

The client only needs to call this API.

Internally Spring AI performs:

```text
Question
   ↓
Embedding
   ↓
Qdrant similarity search
   ↓
Retrieve relevant documents
   ↓
Build prompt
   ↓
Ollama qwen3
   ↓
Answer
```

The client does not need to separately call:

* Ollama embedding API
* Qdrant API
* Ollama chat API

Spring AI handles those operations.

---

# 21. RAG Service

The service retrieves the most relevant documents:

```java
List<Document> documents = vectorStore.similaritySearch(
        SearchRequest.builder()
                .query(question)
                .topK(5)
                .build()
);
```

Then the retrieved context is passed to Ollama.

---

# 22. Response Formatting

The LLM prompt should explicitly request readable output.

Example:

```java
String prompt = """
    You are a helpful assistant answering questions using the provided context.

    CONTEXT:
    %s

    QUESTION:
    %s

    RULES:
    1. Answer only using the provided context.
    2. If the answer is not available in the context, say:
       "I don't know based on the provided documents."
    3. Use normal spaces between every word.
    4. Use proper punctuation.
    5. Write complete, readable sentences.
    6. Never join words together.
    7. Return plain text only.
    8. Do not add information that is not present in the context.

    ANSWER:
    """.formatted(context, question);
```

Expected:

```text
I don't know based on the provided documents.
```

Instead of:

```text
Idon'tknowbasedontheprovideddocuments.
```

---

# 23. Debugging Retrieved Documents

If the response contains merged words, first check whether the retrieved document itself is correct.

```java
documents.forEach(document ->
        System.out.println("RETRIEVED: " + document.getText())
);
```

If the console shows:

```text
Thepolicystatesthat...
```

the problem is probably in document extraction or ingestion.

If the retrieved text is correct but Ollama returns:

```text
Thepolicystatesthat...
```

then the problem is likely in the LLM response formatting.

---

# 24. Starting the Application

Start Docker services:

```bash
docker compose up -d
```

Check:

```bash
docker ps
```

Verify Docker Ollama:

```bash
docker exec -it rag-ollama ollama list
```

Verify Qdrant:

```text
http://localhost:6333/dashboard
```

Then start Spring Boot:

```bash
mvn clean spring-boot:run
```

---

# 25. Complete Local RAG Environment

After everything starts:

```text
Windows
│
├── Existing Ollama
│      └── localhost:11434
│
├── Java 21
│      │
│      └── Spring Boot + Spring AI
│              │
│              ├──────────────► localhost:11435
│              │                   Docker Ollama
│              │
│              └──────────────► localhost:6334
│                                  Docker Qdrant
│
└── Docker
       │
       ├── rag-ollama
       │     ├── qwen3
       │     └── nomic-embed-text
       │
       └── rag-qdrant
             └── company_documents
```

---

# 26. Complete RAG Flow

```text
                 USER
                   │
                   ▼
          /api/rag/ask
                   │
                   ▼
          Spring Boot 3.x
                   │
                   ▼
             Spring AI
                   │
          ┌────────┴────────┐
          │                 │
          ▼                 ▼
   Embedding Model       Qdrant
   nomic-embed-text      Vector Search
          │                 │
          └────────┬────────┘
                   │
                   ▼
             Relevant Chunks
                   │
                   ▼
              Prompt
                   │
                   ▼
             Docker Ollama
                 qwen3
                   │
                   ▼
                Answer
```

---

# 27. Useful Docker Commands

Start:

```bash
docker compose up -d
```

Stop:

```bash
docker compose stop
```

Start again:

```bash
docker compose start
```

Stop and recreate containers:

```bash
docker compose down
docker compose up -d
```

View logs:

```bash
docker logs rag-ollama
```

```bash
docker logs rag-qdrant
```

Follow Ollama logs:

```bash
docker logs -f rag-ollama
```

List containers:

```bash
docker ps
```

List Docker volumes:

```bash
docker volume ls
```

---

# 28. Useful Ollama Commands

Docker Ollama model list:

```bash
docker exec -it rag-ollama ollama list
```

Pull model:

```bash
docker exec -it rag-ollama ollama pull qwen3
```

Pull embedding model:

```bash
docker exec -it rag-ollama ollama pull nomic-embed-text
```

Remove Docker Ollama model:

```bash
docker exec -it rag-ollama ollama rm MODEL_NAME
```

---

# 29. Qdrant Data Persistence

Qdrant uses:

```text
qdrant_data
```

Docker volume.

Ollama uses:

```text
ollama_rag_data
```

Docker volume.

Therefore:

```text
Container restart
       ↓
Data remains
```

The Docker containers can be recreated without downloading Ollama models or losing Qdrant vectors, provided the volumes are preserved.

---

# 30. Troubleshooting

### Ollama connection refused

Check:

```bash
docker ps
```

Then:

```bash
docker logs rag-ollama
```

Check the Docker Ollama endpoint:

```text
http://localhost:11435
```

Make sure `application.yml` contains:

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11435
```

---

### Model not found

Check:

```bash
docker exec -it rag-ollama ollama list
```

Pull the model:

```bash
docker exec -it rag-ollama ollama pull qwen3
```

For embeddings:

```bash
docker exec -it rag-ollama ollama pull nomic-embed-text
```

---

### Qdrant connection problem

Check:

```bash
docker ps
```

Check:

```text
http://localhost:6333/dashboard
```

Verify:

```yaml
spring:
  ai:
    vectorstore:
      qdrant:
        host: localhost
        port: 6334
```

---

### Duplicate documents

If documents are duplicated after restarting Spring Boot, check that document ingestion is not being executed blindly from `@PostConstruct`.

Use document hash/version checking and a separate ingestion endpoint.

---

# 31. Production-style Improvements

The sample can be extended with:

* PDF ingestion
* DOCX ingestion
* Markdown ingestion
* Multiple documents
* Document metadata
* Document hash/version tracking
* Incremental indexing
* Delete/update document APIs
* Source citations
* Conversation memory
* RAG Advisor
* Hybrid search
* Re-ranking
* Authentication
* REST API documentation
* Dockerized Spring Boot application
* Open WebUI
* Monitoring
* Logging
* GPU support

---

# 32. Recommended Architecture

For this project, the recommended development architecture is:

```text
Spring Boot
Java 21
Spring AI
     │
     ├───────────────┐
     │               │
     ▼               ▼
Docker Ollama      Docker Qdrant
:11435             :6334
     │               │
     ├── qwen3       └── vectors
     │
     └── nomic-embed-text
```

Your existing local Ollama remains completely independent:

```text
Existing Ollama
localhost:11434
```

The RAG project uses:

```text
Docker Ollama
localhost:11435
```

---

# 33. Summary

This project provides a completely local RAG implementation using:

```text
Java 21
   +
Spring Boot 3.x
   +
Spring AI
   +
Docker Ollama
   +
Qdrant
```

No cloud LLM is required.

The important design decisions are:

1. Existing local Ollama remains on `11434`.
2. Docker Ollama uses host port `11435`.
3. Docker Ollama models are stored in `ollama_rag_data`.
4. Qdrant vectors are stored in `qdrant_data`.
5. Spring AI connects to Docker Ollama automatically.
6. Spring AI performs embedding and vector retrieval automatically.
7. Questions do not modify or remove documents from Qdrant.
8. Documents should not be blindly indexed during every application startup.
9. Document hash/version checking should be used to prevent duplicate indexing.
10. A separate document ingestion API is recommended for a production-style RAG implementation.

The final request flow is:

```text
User
 ↓
Spring Boot API
 ↓
Spring AI
 ↓
Qdrant similarity search
 ↓
Relevant document chunks
 ↓
Docker Ollama
 ↓
Formatted answer
```
