# Student Life OS AI Service

FastAPI service that provides AI-powered responses for the Student Life OS backend.

## Features

- **Multi-LLM Support**: OpenAI, Gemini, Ollama, and Mock modes
- **Streaming Responses**: Real-time response streaming for better UX
- **Safety Filters**: Built-in content safety and phrase filtering
- **Voice-First Design**: Optimized for spoken responses
- **Emotion Integration**: Returns appropriate emotions for mascot animations

## Setup

### 1. Install Dependencies
```bash
cd ai_service
pip install -r requirements.txt
```

### 2. Configure Environment
```bash
cp .env.example .env
# Edit .env with your API keys and preferences
```

### 3. Choose Your LLM Provider

#### OpenAI (Recommended)
```env
LLM_PROVIDER=openai
OPENAI_API_KEY=your_openai_api_key_here
OPENAI_MODEL=gpt-4o-mini
```

#### Google Gemini
```env
LLM_PROVIDER=gemini
GEMINI_API_KEY=your_gemini_api_key_here
GEMINI_MODEL=gemini-1.5-flash
```

#### Local Ollama
```env
LLM_PROVIDER=ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=llama3.1
```

#### Mock Mode (No API needed)
```env
LLM_PROVIDER=mock
```

### 4. Run the Service
```bash
python main.py
```

The service will start on `http://localhost:9001`

## API Endpoints

### Generate Response
```
POST /ai/generate
Content-Type: application/json

{
  "intent": "GREETING",
  "facts": {},
  "userMessage": "Hi there",
  "desiredTone": "friendly",
  "language": "en"
}
```

### Stream Response
```
POST /ai/generate/stream
Content-Type: application/json

{
  "intent": "GREETING",
  "facts": {},
  "userMessage": "Hi there",
  "desiredTone": "friendly",
  "language": "en"
}
```

### Health Check
```
GET /health
```

## Integration with Spring Boot

The Spring Boot backend calls this service via `AiBrainWebClient`:

1. **Non-blocking calls** using WebClient
2. **Automatic retries** with exponential backoff
3. **Graceful fallbacks** when AI service is unavailable
4. **Configurable timeouts** and connection pooling

## Configuration

All settings are configurable via environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `LLM_PROVIDER` | `mock` | LLM provider (openai/gemini/ollama/mock) |
| `OPENAI_API_KEY` | - | OpenAI API key |
| `GEMINI_API_KEY` | - | Google Gemini API key |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama server URL |
| `REQUEST_TIMEOUT_SECONDS` | `20` | Request timeout in seconds |
| `MAX_REPLY_SENTENCES` | `2` | Maximum sentences in response |

## Development

### Project Structure
```
ai_service/
├── main.py                 # FastAPI app and endpoints
├── config.py              # Settings and environment loading
├── models.py              # Pydantic models for requests/responses
├── llm_client.py          # LLM client (non-streaming)
├── streaming_llm_client.py # LLM client (streaming)
├── prompt.py              # Prompt template builder
├── safety.py              # Content safety filters
├── response_builder.py    # Response construction logic
├── requirements.txt       # Python dependencies
└── .env.example          # Environment template
```

### Adding New LLM Providers

1. Add provider-specific logic to `llm_client.py`
2. Add streaming support in `streaming_llm_client.py`
3. Update `config.py` with new environment variables
4. Update the provider selection logic

## Deployment

### Docker
```dockerfile
FROM python:3.10-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt
COPY . .
EXPOSE 9001
CMD ["python", "main.py"]
```

### Docker Compose (with Spring Boot)
```yaml
version: '3.8'
services:
  ai-service:
    build: ./ai_service
    ports:
      - "9001:9001"
    environment:
      - LLM_PROVIDER=openai
      - OPENAI_API_KEY=${OPENAI_API_KEY}
  
  spring-boot:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - ai-service
    environment:
      - AI_BRAIN_BASE_URL=http://ai-service:9001
```

## Monitoring

- Logs include request/response details
- Health check endpoint for load balancers
- Error handling with graceful degradation
- Request timeout and retry logging
