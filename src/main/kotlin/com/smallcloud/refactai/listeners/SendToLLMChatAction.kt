package com.smallcloud.refactai.listeners

import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.treeStructure.Tree
import com.smallcloud.refactai.Resources
import com.smallcloud.refactai.panes.RefactAIToolboxPaneFactory
import com.smallcloud.refactai.struct.ChatMessage
import javax.swing.JComponent
import javax.swing.tree.DefaultMutableTreeNode

class SendToLLMChatAction : AnAction(
    "Send to Refact.ai Chat",
    "Send selected context to Refact.ai Chat",
    Resources.Icons.LOGO_RED_16x16
) {
    
    private val logger = Logger.getInstance(SendToLLMChatAction::class.java)

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val toolWindowManager = ToolWindowManager.getInstance(project)
        val activeToolWindowId = toolWindowManager.activeToolWindowId
        val place = e.place

        // Show action only in relevant contexts
        val isProblemsWindow = "Problems" == activeToolWindowId
        val isBuildWindow = "Build" == activeToolWindowId || "Messages" == activeToolWindowId
        val isConsoleContext = place.contains("Console") || place.contains("TreePopup") || place.contains("Popup")

        val shouldShow = (isProblemsWindow || isBuildWindow) && isConsoleContext

        e.presentation.isEnabledAndVisible = shouldShow

        if (shouldShow) {
            val context = extractContext(e, project)
            e.presentation.isEnabled = context.isNotEmpty()
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val context = extractContext(e, project)
        
        if (context.isEmpty()) {
            logger.warn("No context found to send to chat")
            return
        }

        sendContextToChat(project, context)
    }

    private fun extractContext(e: AnActionEvent, project: Project): String {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val activeToolWindowId = toolWindowManager.activeToolWindowId

        return when (activeToolWindowId) {
            "Problems" -> extractProblemsContext(e, project)
            "Build", "Messages" -> extractBuildContext(e, project)
            else -> extractSelectedText(e)
        }
    }

    private fun extractProblemsContext(e: AnActionEvent, project: Project): String {
        try {
            // First try selected text
            val selectedText = e.getData(CommonDataKeys.SELECTED_TEXT)
            if (!selectedText.isNullOrEmpty()) {
                return "Problem: $selectedText"
            }

            // Try to get from Problems tool window
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Problems")
            val selectedContent = toolWindow?.contentManager?.selectedContent
            val component = selectedContent?.component

            // Try to extract from tree selection
            if (component is JComponent) {
                val tree = findTreeComponent(component)
                if (tree != null) {
                    val selectionPath = tree.selectionPath
                    if (selectionPath != null) {
                        val selectedNode = selectionPath.lastPathComponent
                        if (selectedNode is DefaultMutableTreeNode) {
                            val userObject = selectedNode.userObject
                            return "Problem: ${userObject.toString()}"
                        }
                    }
                }
            }

            // Fallback to navigatable data
            val navigatable = e.getData(CommonDataKeys.NAVIGATABLE)
            if (navigatable != null) {
                return "Problem: ${navigatable.toString()}"
            }

        } catch (ex: Exception) {
            logger.warn("Error extracting problems context", ex)
        }

        return ""
    }

    private fun extractBuildContext(e: AnActionEvent, project: Project): String {
        try {
            // First try selected text
            val selectedText = e.getData(CommonDataKeys.SELECTED_TEXT)
            if (!selectedText.isNullOrEmpty()) {
                return "Build Output: $selectedText"
            }

            // Try to get from Build tool window
            val buildWindow = ToolWindowManager.getInstance(project).getToolWindow("Build")
            if (buildWindow != null && buildWindow.isActive) {
                val selectedContent = buildWindow.contentManager.selectedContent
                if (selectedContent != null) {
                    val component = selectedContent.component
                    if (component is ConsoleView) {
                        val consoleText = component.selectedText
                        if (!consoleText.isNullOrEmpty()) {
                            return "Build Output: $consoleText"
                        }
                    }
                }
            }

            // Try Messages tool window
            val messagesWindow = ToolWindowManager.getInstance(project).getToolWindow("Messages")
            if (messagesWindow != null && messagesWindow.isActive) {
                val selectedContent = messagesWindow.contentManager.selectedContent
                if (selectedContent != null) {
                    val component = selectedContent.component
                    if (component is ConsoleView) {
                        val consoleText = component.selectedText
                        if (!consoleText.isNullOrEmpty()) {
                            return "Compiler Messages: $consoleText"
                        }
                    }
                }
            }

        } catch (ex: Exception) {
            logger.warn("Error extracting build context", ex)
        }

        return ""
    }

    private fun extractSelectedText(e: AnActionEvent): String {
        val selectedText = e.getData(CommonDataKeys.SELECTED_TEXT)
        return if (!selectedText.isNullOrEmpty()) {
            "Selected Text: $selectedText"
        } else {
            ""
        }
    }

    private fun findTreeComponent(component: JComponent): Tree? {
        if (component is Tree) {
            return component
        }
        
        for (child in component.components) {
            if (child is JComponent) {
                val result = findTreeComponent(child)
                if (result != null) return result
            }
        }
        
        return null
    }

    private fun sendContextToChat(project: Project, context: String) {
        ApplicationManager.getApplication().invokeLater {
            try {
                // Open the Refact chat tool window
                val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Refact")
                if (toolWindow != null) {
                    toolWindow.activate {
                        RefactAIToolboxPaneFactory.focusChat()
                        
                        // Create a chat message with the context
                        val chatMessage = ChatMessage(
                            role = "user",
                            content = "Please help me with this context:\n\n$context",
                            toolCallId = null,
                            usage = null
                        )
                        
                        // Send the message to chat using the correct method
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