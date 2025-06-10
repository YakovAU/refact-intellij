# Right-Click Context Menu Implementation for JetBrains IDE Plugin

## Overview

This document describes the implementation of a right-click context menu feature in the Refact.ai JetBrains IDE plugin that allows users to send context from error output dialogs, compiler output, and problems list to the LLM chat.

## Implementation Details

### 1. Action Class

**File**: `src/main/kotlin/com/smallcloud/refactai/listeners/SendToLLMChatAction.kt`

The main action class `SendToLLMChatAction` extends `AnAction` and provides the following functionality:

#### Key Features:
- **Context Detection**: Automatically detects the active tool window (Problems, Build, Messages)
- **Context Extraction**: Extracts relevant context from different UI components
- **Chat Integration**: Sends extracted context to the Refact.ai chat interface
- **Visibility Control**: Shows the action only in relevant contexts

#### Context Extraction Methods:

1. **Problems Tool Window**: 
   - Extracts selected problem descriptions
   - Handles tree structure navigation
   - Fallback to navigatable elements

2. **Build Tool Window**:
   - Extracts selected build output text
   - Handles console view content
   - Supports both Build and Messages tool windows

3. **General Context**:
   - Extracts any selected text as fallback

#### Integration with Chat:
- Uses `RefactAIToolboxPaneFactory.chat?.executeCodeLensCommand()` to send messages
- Creates `ChatMessage` objects with proper structure
- Activates the Refact tool window automatically

### 2. Plugin Registration

**File**: `src/main/resources/META-INF/plugin.xml`

The action is registered in the plugin configuration:

```xml
<action id="SendToLLMChatAction"
        class="com.smallcloud.refactai.listeners.SendToLLMChatAction"
        text="Send to Refact.ai Chat"
        description="Send selected context to Refact.ai Chat">
    <add-to-group group-id="TreePopupMenu" anchor="last"/>
    <add-to-group group-id="ConsoleViewPopupMenu" anchor="last"/>
</action>
```

#### Action Groups:
- **TreePopupMenu**: For Problems tool window (tree-based UI)
- **ConsoleViewPopupMenu**: For Build/Messages tool windows (console-based UI)

### 3. Visibility Logic

The action uses the `update()` method to control when it appears:

```kotlin
val isProblemsWindow = "Problems" == activeToolWindowId
val isBuildWindow = "Build" == activeToolWindowId || "Messages" == activeToolWindowId
val isConsoleContext = place.contains("Console") || place.contains("TreePopup") || place.contains("Popup")

val shouldShow = (isProblemsWindow || isBuildWindow) && isConsoleContext
```

### 4. Error Handling

The implementation includes comprehensive error handling:
- Try-catch blocks around context extraction
- Logging for debugging purposes
- Graceful fallbacks when specific UI components are not available

## Usage

### For Users:
1. **In Problems Tool Window**: Right-click on any problem/error → "Send to Refact.ai Chat"
2. **In Build Tool Window**: Right-click on build output → "Send to Refact.ai Chat"
3. **In Messages Tool Window**: Right-click on compiler messages → "Send to Refact.ai Chat"

### Expected Behavior:
1. Action appears in context menu when relevant
2. Selected context is extracted automatically
3. Refact.ai chat tool window opens
4. Context is sent as a chat message with prefix "Please help me with this context:"

## Technical Considerations

### Action Group IDs
The implementation uses standard IntelliJ action groups:
- `TreePopupMenu` for tree-based components
- `ConsoleViewPopupMenu` for console-based components

### Alternative Action Groups
If the standard groups don't work for specific tool windows, these alternatives could be investigated:
- `ProblemsViewPopupMenu` (if it exists)
- `BuildViewPopupMenu` (if it exists)
- Tool window-specific action groups

### UI Inspector Usage
For precise action group identification, developers can:
1. Enable UI Inspector in IntelliJ IDEA settings
2. Right-click in target tool windows to inspect action groups
3. Update action registration accordingly

## Verification Steps

### Manual Testing:
1. Open a project with compilation errors
2. Navigate to Problems tool window
3. Right-click on a problem entry
4. Verify "Send to Refact.ai Chat" appears
5. Click the action and verify chat opens with context

### Build Verification:
1. Ensure project compiles without errors
2. Run plugin in IDE sandbox
3. Test all supported tool windows
4. Verify context extraction accuracy

## Future Enhancements

### Potential Improvements:
1. **Enhanced Context Extraction**: Extract more detailed problem information (file path, line number, severity)
2. **Customizable Messages**: Allow users to customize the chat message format
3. **Batch Processing**: Support sending multiple selected items at once
4. **Context Filtering**: Filter out sensitive information before sending
5. **Integration with More Tool Windows**: Extend to other relevant tool windows

### Advanced Features:
1. **Smart Context Analysis**: Automatically include related code snippets
2. **Error Categorization**: Categorize errors and provide targeted prompts
3. **Solution Suggestions**: Pre-populate chat with specific solution requests
4. **History Integration**: Remember previous similar issues and solutions

## Troubleshooting

### Common Issues:

1. **Action Not Appearing**:
   - Check if correct tool window is active
   - Verify action group registration
   - Use UI Inspector to identify correct groups

2. **Context Not Extracted**:
   - Ensure text is selected or item is highlighted
   - Check tool window implementation variations
   - Review error logs for extraction failures

3. **Chat Integration Issues**:
   - Verify Refact.ai tool window is available
   - Check chat service availability
   - Review message format compatibility

### Debugging:
- Enable debug logging for `SendToLLMChatAction`
- Use IDE debugger to step through context extraction
- Check IntelliJ Platform logs for action system errors

## Dependencies

### Required IntelliJ Platform APIs:
- `com.intellij.openapi.actionSystem.*`
- `com.intellij.openapi.wm.ToolWindowManager`
- `com.intellij.execution.ui.ConsoleView`
- `com.intellij.ui.treeStructure.Tree`

### Plugin Dependencies:
- Refact.ai plugin core functionality
- Chat pane integration
- Message passing system

## Conclusion

This implementation provides a robust foundation for sending context from IDE tool windows to the LLM chat. The modular design allows for easy extension to additional tool windows and context types, while the comprehensive error handling ensures reliable operation across different IDE configurations.