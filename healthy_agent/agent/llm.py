from __future__ import annotations
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI
from .config import Settings
from .schemas import AnalysisResult, HealthAnalysisContext, PeriodSummary

class AnalysisChain:
    """LangChain chain; Java facts are explicitly authoritative."""
    def __init__(self, settings: Settings):
        self.chain = None
        if settings.enable_llm:
            model = ChatOpenAI(model=settings.openai_chat_model, temperature=settings.openai_temperature)
            structured = model.with_structured_output(AnalysisResult)
            prompt = ChatPromptTemplate.from_messages([("system", "你是健康饮食分析助手。只能基于 AUTHORITY_FACTS 和 RULE_SUMMARY 作事实判断。不得修改、猜测或覆盖 Java 业务事实；冲突时以 AUTHORITY_FACTS 为准。输出健康建议，不进行疾病诊断。"), ("human", "AUTHORITY_FACTS:\n{facts}\nRULE_SUMMARY:\n{rules}\nUSER_MEMORY:\n{memory}\n请给出结构化健康分析。")])
            self.chain = prompt | structured
    def invoke(self, context: HealthAnalysisContext, rules: str, memory: str):
        if not self.chain: return None
        return self.chain.invoke({"facts": context.model_dump_json(by_alias=True), "rules": rules, "memory": memory})


class PeriodSummaryChain:
    """Small separate chain for compact biweekly/monthly memory summaries."""
    def __init__(self, settings: Settings):
        self.chain = None
        if settings.enable_llm:
            model = ChatOpenAI(model=settings.openai_chat_model, temperature=0).with_structured_output(PeriodSummary)
            prompt = ChatPromptTemplate.from_messages([
                ("system", "你负责把结构化饮食趋势压缩成周期总结。只能根据输入事实，不要编造数值。"),
                ("human", "周期类型={period_type}\n输入事实={facts}\n请输出结构化周期总结。"),
            ])
            self.chain = prompt | model

    def invoke(self, period_type: str, facts: str):
        return self.chain.invoke({"period_type": period_type, "facts": facts}) if self.chain else None
