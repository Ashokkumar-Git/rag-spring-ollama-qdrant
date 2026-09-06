# Local RAG with Spring AI, Ollama and Qdrant

A simple **Retrieval-Augmented Generation (RAG)** application built with:

* Java 21
* Spring Boot 3.x
* Spring AI 1.1.x
* Ollama
* Qdrant Vector Database
* Maven

The application reads documents, creates embeddings locally using Ollama, stores them in Qdrant, retrieves relevant document chunks when a question is asked, and uses a local Ollama LLM to generate the final answer.

---

## Architecture

```text
                         ┌─────────────────────┐
                         │     User / Client    │
                         └──────────┬──────────┘
                                    │
                                    │ GET /api/rag/ask
                                    ▼
                         ┌─────────────────────┐
                         │   Spring Boot API   │
                         │                     │
                         │     RagService      │
                         └──────────┬──────────┘
                                    │
                         1. Embed question
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │       Ollama        │
                         │                     │
                         │ nomic-embed-text    │
                         └──────────┬──────────┘
                                    │
                                    │ Query vector
                                    ▼
                         ┌─────────────────────┐
                         │       Qdrant        │
                         │   Vector Database   │
                         │                     │
                         │ company_documents   │
                         └──────────┬──────────┘
                                    │
                                    │ Relevant chunks
                                    ▼
                         ┌─────────────────────┐
                         │      RagService     │
                         │                     │
                         │ Question + Context  │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │       Ollama        │
                         │                     │
                         │       qwen3         │
                         └──────────┬──────────┘
                                    │
                                    ▼
                              Final Answer
```

---

# 1. How RAG Works

This project follows the standard RAG flow.

### Document ingestion

```text
company-policy.txt
       │
       ▼
Read document
       │
       ▼
Split into chunks
       │
       ▼
Ollama Embedding Model
       │
       ▼
Generate vectors
       │
       ▼
Qdrant
```

### Question answering

```text
User question
       │
       ▼
Create question embedding
       │
       ▼
Search Qdrant
       │
       ▼
Retrieve relevant chunks
       │
       ▼
Question + Context
       │
       ▼
Ollama LLM
       │
       ▼
Final answer
```

The application does **not** send the complete document to the LLM for every question.

Only the relevant chunks retrieved from Qdrant are sent to the LLM.

---

# 2. Technologies

| Technology  | Version / Usage        |
| ----------- | ---------------------- |
| Java        | 21                     |
| Spring Boot | 3.x                    |
| Spring AI   | 1.1.x                  |
| Maven       | Build tool             |
| Ollama      | Local LLM + embeddings |
| Qdrant      | Vector database        |
| Docker      | Qdrant container       |

---

# 3. Project Structure

```text
spring-ai-local-rag/
│
├── pom.xml
│
├── docker-compose.yml
│
└── src/
    └── main/
        ├── java/
        │   └── com/example/rag/
        │       │
        │       ├── RagApplication.java
        │       │
        │       ├── controller/
        │       │   └── RagController.java
        │       │
        │       └── service/
        │           ├── DocumentIngestionService.java
        │           └── RagService.java
        │
        └── resources/
            │
            ├── application.yml
            │
            └── documents/
                └── company-policy.txt
```

---

# 4. Prerequisites

Install the following:

### Java 21

Verify:

```bash
java -version
```

Expected:

```text
java version "21..."
```

### Maven

Verify:

```bash
mvn -version
```

Make sure Maven is using Java 21.

---

# 5. Install Ollama

Install Ollama on your local machine.

After installation, verify:

```bash
ollama --version
```

Start Ollama if it is not already running.

The default Ollama API is:

```text
http://localhost:11434
```

---

# 6. Download Ollama Models

This project uses two local models.

## LLM

```bash
ollama pull qwen3
```

This model generates the final answer.

## Embedding Model

```bash
ollama pull nomic-embed-text
```

This model converts text into vectors.

Verify:

```bash
ollama list
```

You should see both models.

```text
qwen3
nomic-embed-text
```

---

# 7. Start Qdrant

Qdrant is used as the vector database.

Example `docker-compose.yml`:

```yaml
services:
  qdrant:
    image: qdrant/qdrant:latest
    container_name: local-qdrant
    ports:
      - "6333:6333"
      - "6334:6334"
    volumes:
      - qdrant_storage:/qdrant/storage

volumes:
  qdrant_storage:
```

Start Qdrant:

```bash
docker compose up -d
```

Check the container:

```bash
docker ps
```

Qdrant services:

```text
REST API  : http://localhost:6333
gRPC      : localhost:6334
Dashboard : http://localhost:6333/dashboard
```

---

# 8. Qdrant Collection

The application uses this collection:

```text
company_documents
```

The collection contains vectors generated from the documents.

Conceptually:

```text
company-policy.txt
       │
       ├── Chunk 1
       │      └── Vector + text + metadata
       │
       ├── Chunk 2
       │      └── Vector + text + metadata
       │
       └── Chunk 3
              └── Vector + text + metadata
```

You can inspect the collection using the Qdrant dashboard.

---

# 9. Spring AI Configuration

Example `application.yml`:

```yaml
spring:
  application:
    name: local-rag

  ai:
    ollama:
      base-url: http://localhost:11434

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

---

# 10. Maven Dependencies

The main Spring AI dependencies are:

```xml
<dependencies>

    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-ollama</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-vector-store-qdrant</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

</dependencies>
```

Use the Spring AI BOM to manage Spring AI versions.

---

# 11. Document Ingestion

The document is located at:

```text
src/main/resources/documents/company-policy.txt
```

Example:

```text
Company Leave Policy

Employees are entitled to 24 paid leave days per year.

Casual leave can be requested for short-term personal requirements.

Earned leave should normally be requested at least two days in advance.

Emergency leave can be requested directly from the manager.

The manager is responsible for approving emergency leave.
```

During ingestion:

```text
TXT
 │
 ▼
TextReader
 │
 ▼
TokenTextSplitter
 │
 ▼
Document chunks
 │
 ▼
Ollama Embedding
 │
 ▼
Qdrant
```

---

# 12. Important: Ingestion vs Question

These are two different operations.

### Ingestion

```text
Document
   ↓
Chunk
   ↓
Embedding
   ↓
Qdrant
```

This should happen when a document is added or updated.

### Question

```text
Question
   ↓
Embedding
   ↓
Qdrant similarity search
   ↓
Relevant chunks
   ↓
Ollama LLM
   ↓
Answer
```

When the user asks a question, the application **does not remove the document from Qdrant**.

It simply searches the existing vectors.

---

# 13. Start the Application

Run:

```bash
mvn clean spring-boot:run
```

Or:

```bash
mvn clean package
java -jar target/local-rag-0.0.1-SNAPSHOT.jar
```

The application will start on:

```text
http://localhost:8080
```

---

# 14. Ask a Question

Use the RAG API:

```text
GET /api/rag/ask
```

Example:

```text
http://localhost:8080/api/rag/ask?question=How%20many%20leave%20days%20do%20employees%20get?
```

Example response:

```text
Employees are entitled to 24 paid leave days per year.
```

Another question:

```text
http://localhost:8080/api/rag/ask?question=Who%20approves%20emergency%20leave?
```

Response:

```text
The manager approves emergency leave.
```

---

# 15. Complete RAG Flow

When this API is called:

```text
GET /api/rag/ask?question=Who approves emergency leave?
```

the following happens automatically inside the application:

```text
                    User
                     │
                     ▼
              /api/rag/ask
                     │
                     ▼
                RagService
                     │
                     ▼
             Embed Question
                     │
                     ▼
                  Ollama
             nomic-embed-text
                     │
                     ▼
              Question Vector
                     │
                     ▼
                 Qdrant
                     │
             Similarity Search
                     │
                     ▼
              Top 5 Chunks
                     │
                     ▼
             Build RAG Prompt
                     │
                     ▼
                  Ollama
                   qwen3
                     │
                     ▼
               Final Answer
```

---

# 16. Vector Search

The application uses Spring AI's `VectorStore`:

```java
List<Document> results =
        vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(question)
                .topK(5)
                .build()
        );
```

`topK(5)` means the application requests the five most relevant document chunks.

For example:

```text
Question:
"Who approves emergency leave?"

Qdrant search:

1. Emergency leave can be requested from the manager.
2. The manager is responsible for approving emergency leave.
3. Employees receive 24 paid leave days.
4. Earned leave requires advance notice.
5. Casual leave policy...
```

The relevant chunks are then provided to the LLM.

---

# 17. Prompt Construction

The retrieved documents are combined with the question.

Conceptually:

```text
Context:
Emergency leave can be requested directly from the manager.
The manager is responsible for approving emergency leave.

Question:
Who approves emergency leave?

Answer:
```

Ollama then generates the answer.

---

# 18. Clean Answer Formatting

The LLM prompt should instruct the model to produce readable output.

Recommended rules:

```text
- Use proper spaces between words.
- Use proper punctuation.
- Write complete sentences.
- Return only plain text.
- Do not merge words.
- Do not use unnecessary Markdown.
```

For example, avoid:

```text
Thepolicystatesthatemergencyleave...
```

Prefer:

```text
The policy states that emergency leave can be requested directly from the manager. Therefore, the manager is responsible for approving emergency leave.
```

---

# 19. Accessing Qdrant

You can open the Qdrant dashboard:

```text
http://localhost:6333/dashboard
```

Or use the REST API:

```bash
curl http://localhost:6333/collections
```

Check the specific collection:

```bash
curl http://localhost:6333/collections/company_documents
```

---

# 20. Troubleshooting

## Ollama connection error

If you see an error connecting to Ollama, check:

```bash
ollama list
```

Make sure Ollama is running.

Test:

```bash
curl http://localhost:11434/api/tags
```

---

## Qdrant connection error

Check Docker:

```bash
docker ps
```

Check Qdrant:

```text
http://localhost:6333/dashboard
```

Make sure port `6334` is available for the Spring AI Qdrant connection.

---

## Model not found

Run:

```bash
ollama pull qwen3
```

and:

```bash
ollama pull nomic-embed-text
```

---

## Duplicate documents after restart

If ingestion uses:

```java
@PostConstruct
```

and calls:

```java
vectorStore.add(chunks);
```

every startup, the same document may be indexed repeatedly depending on document IDs/configuration.

For a production application, use a proper ingestion strategy:

```text
Document
   ↓
Calculate document hash/version
   ↓
Already indexed?
   ├── YES → Skip
   │
   └── NO
        ↓
      Chunk
        ↓
      Embed
        ↓
      Store
```

---

# 21. Recommended Production Architecture

For a real application, separate document ingestion from question answering.

### Document API

```text
POST /api/documents/ingest
```

Responsible for:

```text
Upload document
     ↓
Parse
     ↓
Chunk
     ↓
Generate embeddings
     ↓
Store in Qdrant
```

### RAG API

```text
GET /api/rag/ask?question=...
```

Responsible only for:

```text
Question
   ↓
Embedding
   ↓
Vector search
   ↓
LLM
   ↓
Answer
```

This prevents unnecessary document processing.

---

# 22. Future Improvements

This project can be extended with:

* PDF document ingestion
* DOCX document ingestion
* Markdown document ingestion
* Multiple documents
* Document metadata
* Source citations
* Document versioning
* Duplicate detection
* Document update/delete
* RAG Advisor
* Conversation memory
* Streaming responses
* REST API authentication
* React frontend
* PostgreSQL integration
* Hybrid search
* Reranking
* Multiple Ollama models
* Production Docker setup
* Monitoring and logging

---

# 23. Technology Flow Summary

```text
┌─────────────────────────────────────────────────────┐
│                    DOCUMENT                         │
│                                                     │
│ company-policy.txt                                  │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
              ┌─────────────────┐
              │ Spring AI       │
              │ Document Reader │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │ Text Splitter   │
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │ Ollama          │
              │ Embeddings      │
              │ nomic-embed-text│
              └────────┬────────┘
                       │
                       ▼
              ┌─────────────────┐
              │ Qdrant          │
              │ Vector Database │
              └─────────────────┘


QUESTION
   │
   ▼
Spring Boot
   │
   ▼
Ollama Embedding
   │
   ▼
Qdrant Similarity Search
   │
   ▼
Relevant Documents
   │
   ▼
Prompt + Context
   │
   ▼
Ollama qwen3
   │
   ▼
FINAL ANSWER
```

---

# 24. Summary

This project demonstrates a completely local RAG pipeline:

```text
Java 21
   +
Spring Boot 3.x
   +
Spring AI
   +
Ollama
   +
Qdrant
```

No external LLM API is required.

The embeddings and LLM inference are performed locally through Ollama, while Qdrant stores and searches the document vectors.

The main responsibility of each component is:

| Component   | Responsibility                     |
| ----------- | ---------------------------------- |
| Spring Boot | Application/API                    |
| Spring AI   | AI/RAG integration                 |
| Ollama      | Local embeddings + LLM             |
| Qdrant      | Vector storage + similarity search |
| Maven       | Build/dependency management        |
| Docker      | Runs Qdrant locally                |

The key idea is:

> **Documents are indexed once, questions retrieve relevant chunks, and the local LLM generates the answer from those chunks.**
