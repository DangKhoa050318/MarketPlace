import os
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import google.generativeai as genai
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

app = FastAPI(title="MarketPlace AI Service")

# Configure Gemini API
API_KEY = os.getenv("GEMINI_API_KEY")
if not API_KEY:
    print("WARNING: GEMINI_API_KEY is not set. API calls will fail.")
else:
    genai.configure(api_key=API_KEY)

MODEL_NAME = os.getenv("GEMINI_MODEL_NAME", "gemini-3.1-flash-lite")

class ChatRequest(BaseModel):
    message: str
    product_context: str = ""
    coupon_context: str = ""
    chat_history: list = []  # List of dicts like {"role": "user"/"model", "content": "..."}

class ChatResponse(BaseModel):
    response: str
    model_used: str

@app.post("/api/chat", response_model=ChatResponse)
async def chat_with_gemini(request: ChatRequest):
    if not API_KEY:
        raise HTTPException(status_code=500, detail="GEMINI_API_KEY not configured on AI Service")

    try:
        # Build System Prompt with Context (RAG)
        system_instruction = (
            "Bạn là trợ lý ảo AI thông minh và thân thiện của sàn thương mại điện tử MarketPlace. "
            "Nhiệm vụ của bạn là tư vấn cho khách hàng về sản phẩm và mã giảm giá dựa trên dữ liệu hệ thống cung cấp.\n"
            "Chỉ tư vấn dựa trên danh sách sản phẩm và khuyến mãi được cung cấp bên dưới, KHÔNG ĐƯỢC bịa đặt thông tin.\n"
            "Format câu trả lời bằng Markdown (in đậm tên sản phẩm, dùng bullet points).\n\n"
        )
        
        if request.product_context:
            system_instruction += "=== SẢN PHẨM HIỆN CÓ ===\n"
            system_instruction += request.product_context + "\n\n"
            
        if request.coupon_context:
            system_instruction += "=== MÃ GIẢM GIÁ (COUPON) ĐANG ACTIVE ===\n"
            system_instruction += request.coupon_context + "\n\n"
            
        # Initialize model with system instruction
        model = genai.GenerativeModel(
            model_name=MODEL_NAME,
            system_instruction=system_instruction
        )
        
        # Build history for multi-turn conversation
        formatted_history = []
        for msg in request.chat_history:
            formatted_history.append({
                "role": msg.get("role", "user"),
                "parts": [msg.get("content", "")]
            })
            
        chat = model.start_chat(history=formatted_history)
        
        # Send new message
        response = chat.send_message(request.message)
        
        return ChatResponse(
            response=response.text,
            model_used=MODEL_NAME
        )
        
    except Exception as e:
        print(f"Error calling Gemini: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/health")
async def health_check():
    return {"status": "ok", "model": MODEL_NAME, "api_key_configured": bool(API_KEY)}
