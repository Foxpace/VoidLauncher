package com.tomasrepcik.voidlauncher.launcher.action

enum class TextAssistant(val packageName: String, val website: String) {
    ChatGpt("com.openai.chatgpt", "https://chatgpt.com/"),
    Claude("com.anthropic.claude", "https://claude.ai/new"),
    Gemini("com.google.android.apps.bard", "https://gemini.google.com/app"),
}
