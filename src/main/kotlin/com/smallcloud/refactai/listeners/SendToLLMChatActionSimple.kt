package com.smallcloud.refactai.listeners

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.wm.ToolWindowManager
import com.smallcloud.refactai.Resources
import com.smallcloud.refactai.panes.RefactAIToolboxPaneFactory
import com.smallcloud.refactai.struct.ChatMessage

/**
 * Simplified version of SendToLLMChatAction for testing and troubleshooting.
 * This version focuses on basic functionality with minimal dependencies.
 */
class SendToLLMChatActionSimple : AnAction(
    "Send to Refact.ai Chat (Simple)",
    "Send selected text to Refact.ai Chat",
    Resources.Icons.LOGO_RED_16x16
) {
    
    private val logger = Logger.getInstance(SendToLLMChatActionSimple::class.java)

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        // Simple visibility logic - show when there's selected text
        val selectedText = e.getData(CommonDataKeys.SELECTED_TEXT)
        val hasSelection = !selectedText.isNullOrEmpty()
        
        e.presentation.isEnabledAndVisible = hasSelection
        e.presentation.isEnabled = hasSelection
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val selectedText = e.getData(CommonDataKeys.SELECTED_TEXT)
        
        if (selectedText.isNullOrEmpty()) {
            logger.warn("No text selected")
            return
        }

        sendToChat(project, selectedText)
    }

    private fun sendToChat(project: com.intellij.openapi.project.Project, text: String) {
        ApplicationManager.getApplication().invokeLater {
            try {
                // Open the Refact chat tool window
                val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Refact")
                if (toolWindow != null) {
                    toolWindow.activate {
                        RefactAIToolboxPaneFactory.focusChat()
                        
                        // Create a chat message with the selected text
                        val chatMessage = ChatMessage(
                            role = "user",
                            content = "Please help me with this context:\n\n$text",
                            toolCallId = null,
                            usage = null
                        )
                        
                        // Send the message to chat
                        RefactAIToolboxPaneFactory.chat?.executeCodeLensCommand(arrayOf(chatMessage), true, false)
                    }
                } else {
                    logger.warn("Refact tool window not found")
                }
            } catch (ex: Exception) {
                logger.error("Error sending context to chat", ex)
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}