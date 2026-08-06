# AI Service for MarketPlace

This is a FastAPI microservice that handles interactions with the Google Gemini API to power the storefront's Chatbot widget.

## Prerequisites
- Python 3.9+
- A Google Gemini API Key

## Setup & Run

1. Create a virtual environment (optional but recommended):
   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

3. Configure environment variables:
   Copy `.env.example` to `.env` and insert your Gemini API Key.
   ```bash
   cp .env.example .env
   ```

4. Start the server:
   ```bash
   uvicorn main:app --reload --port 8000
   ```

The API will be available at `http://localhost:8000`. You can test it via Swagger UI at `http://localhost:8000/docs`.
