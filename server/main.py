from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import httpx

app = FastAPI(title="Lumina Backend")


# 定义请求结构
class ChatRequest(BaseModel):
    message: str
    api_key: str = None  # 如果用户没填，则进入“互助池”逻辑


@app.get("/")
async def root():
    return {"status": "Lumina System Online", "mode": "Guardian Alpha"}


@app.post("/v1/chat")
async def chat_endpoint(request: ChatRequest):
    # 1. 简单的关键词情感初判（后续可接入模型）
    sentiment = "neutral"
    if any(word in request.message for word in ["难过", "孤独", "累"]):
        sentiment = "sad"

    # 2. 模拟 AI 响应逻辑
    # 以后这里会判断是用用户的 Key，还是用互助池里的 Key
    return {
        "reply": f"微光收到了你的信号：{request.message}",
        "sentiment_tag": sentiment,
        "source": "Guardian_Node_01"
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)