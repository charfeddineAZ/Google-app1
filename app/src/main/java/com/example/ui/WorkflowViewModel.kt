package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class WorkflowViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = AppRepository(database.appDao())
    private val automationEngine = AutomationEngine(application, repository)

    // --- Database-Backed Reactive Flows ---
    val workflows: StateFlow<List<Workflow>> = repository.allWorkflows
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<LogEntry>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val files: StateFlow<List<WorkspaceFile>> = repository.allFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val libraryItems: StateFlow<List<LibraryItem>> = repository.allLibraryItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val secrets: StateFlow<List<SecretKey>> = repository.allSecrets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Editor States ---
    val currentWorkflow = MutableStateFlow<Workflow?>(null)
    val nodes = MutableStateFlow<List<FlowNode>>(emptyList())
    val connections = MutableStateFlow<List<FlowConnection>>(emptyList())
    val selectedNodeId = MutableStateFlow<String?>(null)
    val zoom = MutableStateFlow(1.0f)
    val panOffset = MutableStateFlow(Pair(0f, 0f))
    val minimapVisible = MutableStateFlow(false)
    val liteMode = MutableStateFlow(false)
    val snapToGrid = MutableStateFlow(true)
    val gridSize = MutableStateFlow(40f)

    // --- Runner States ---
    val isExecuting = MutableStateFlow(false)
    val isPaused = MutableStateFlow(false)
    val startNodeId = MutableStateFlow<String?>(null)
    val endNodeId = MutableStateFlow<String?>(null)
    val webhookPayload = MutableStateFlow("{\n  \"message\": \"مرحباً بك في الأتمتة\",\n  \"userId\": 1024\n}")
    val logsFilter = MutableStateFlow("ALL") // "ALL", "SUCCESS", "INFO", "WARNING", "ERROR"
    val inspectedLogNode = MutableStateFlow<FlowNode?>(null)
    val parallelQueue = MutableStateFlow<List<QueueItem>>(emptyList())

    // --- Workspace States ---
    val selectedFilePath = MutableStateFlow<String?>(null)
    val editingFileContent = MutableStateFlow("")
    val searchQueryFiles = MutableStateFlow("")
    val cloudSyncStatus = MutableStateFlow("مكتمل") // "IDLE", "SYNCING", "COMPLETED", "ERROR"
    val openFileTabs = MutableStateFlow<List<String>>(listOf("/README.md"))

    // --- Library States ---
    val selectedLibraryType = MutableStateFlow("ALL") // "ALL", "JS", "PYTHON", "RACCORD", "SELECTOR"
    val isCommunityHubActive = MutableStateFlow(false)

    // --- Browser States ---
    val currentUrl = MutableStateFlow("https://news.ycombinator.com")
    val isSelectorModeActive = MutableStateFlow(false)
    val isRecordingRaccord = MutableStateFlow(false)
    val recordedSteps = MutableStateFlow<List<BrowserStep>>(emptyList())
    val capturedSelectors = MutableStateFlow<List<String>>(emptyList())

    // --- Customization States ---
    val appLanguage = MutableStateFlow("AR") // "AR", "EN"
    val appTheme = MutableStateFlow("DARK") // "DARK", "LIGHT", "AUTO"
    val nodeTimeoutSec = MutableStateFlow(30)
    val parallelRunsLimit = MutableStateFlow(3)

    // --- AI Copilot States ---
    val copilotPrompt = MutableStateFlow("")
    val isCopilotGenerating = MutableStateFlow(false)
    val copilotResponse = MutableStateFlow("")

    init {
        viewModelScope.launch {
            // Seed database with beautiful defaults if empty
            repository.seedDatabaseIfEmpty()
            
            // Set first workflow as active if available
            val list = repository.allWorkflows.firstOrNull() ?: emptyList()
            if (list.isNotEmpty()) {
                selectWorkflow(list.first())
            }
        }
    }

    // --- Workflow Management Operations ---
    fun selectWorkflow(workflow: Workflow) {
        currentWorkflow.value = workflow
        nodes.value = JsonUtils.parseNodes(workflow.nodesJson)
        connections.value = JsonUtils.parseConnections(workflow.connectionsJson)
        selectedNodeId.value = null
        
        // Populate start and end node options in Runner screen
        val triggerNode = nodes.value.find { n -> n.type.startsWith("TRIGGER") }
        startNodeId.value = triggerNode?.id ?: nodes.value.firstOrNull()?.id
        endNodeId.value = nodes.value.lastOrNull()?.id
    }

    fun createNewWorkflow(name: String) {
        viewModelScope.launch {
            val defaultNodes = """
                [
                  {"id": "node_trigger_1", "name": "أداة التشغيل (Trigger)", "type": "TRIGGER", "x": 150, "y": 200, "properties": {"event": "تشغيل يدوي"}}
                ]
            """.trimIndent()
            val newWf = Workflow(
                name = name,
                nodesJson = defaultNodes,
                connectionsJson = "[]"
            )
            val id = repository.saveWorkflow(newWf)
            val created = repository.getWorkflowById(id.toInt())
            if (created != null) {
                selectWorkflow(created)
            }
        }
    }

    fun addNode(type: String, name: String, customProperties: Map<String, String>? = null) {
        val currentList = nodes.value.toMutableList()
        val newId = "node_${UUID.randomUUID().toString().take(6)}"
        
        // Position at screen center (offset dynamically based on pan)
        val posX = 400f - panOffset.value.first / zoom.value
        val posY = 300f - panOffset.value.second / zoom.value

        // Default properties based on node type
        val props = customProperties ?: when (type) {
            // Traditional types
            "BROWSER" -> mapOf("url" to "https://news.ycombinator.com", "action" to "جلب محتوى الصفحة")
            "CODE" -> mapOf("script" to "// اكتب كود JavaScript هنا\nreturn input;")
            "HTTP" -> mapOf("url" to "https://api.coindesk.com/v1/bpi/currentprice.json", "method" to "GET")
            "AI" -> mapOf("prompt" to "لخص البيانات السابقة باللغة العربية بأسلوب احترافي")
            "FILE_WRITE" -> mapOf("path" to "/workspace/output_${newId.takeLast(3)}.txt")
            "FILE_READ" -> mapOf("path" to "/README.md")
            "NOTIFICATION" -> mapOf("title" to "إشعار Workflow Studio", "message" to "تم إنجاز المهمة بنجاح!")
            "WAIT" -> mapOf("duration" to "2000")
            
            // 1. Triggers (🚀 عقد التشغيل)
            "TRIGGER_MANUAL" -> mapOf("event" to "تشغيل يدوي")
            "TRIGGER_WEBHOOK" -> mapOf("path" to "/webhook/trigger_${newId.takeLast(3)}", "method" to "POST")
            "TRIGGER_CRON" -> mapOf("cron" to "*/5 * * * *", "schedule" to "كل 5 دقائق")
            "TRIGGER_BROWSER_EVENT" -> mapOf("page" to "https://google.com", "selector" to "button#submit")
            "TRIGGER_FILE_WATCHER" -> mapOf("directory" to "/workspace", "filter" to "*.csv")
            "TRIGGER_NOTIFICATION" -> mapOf("app" to "All Apps")
            "TRIGGER_NFC_QR" -> mapOf("type" to "QR_CODE")
            "TRIGGER_VOICE" -> mapOf("phrase" to "ابتدأ العمل")
            "TRIGGER_SHAKE" -> mapOf("sensitivity" to "MEDIUM")
            "TRIGGER_SPECIFIC_TIME" -> mapOf("datetime" to "2026-07-20 12:00:00")

            // 2. Browser Nodes (🌐 عقد المتصفح)
            "BROWSER_OPEN_URL" -> mapOf("url" to "https://google.com", "headless" to "true")
            "BROWSER_CLICK" -> mapOf("selector" to "button.login")
            "BROWSER_TYPE" -> mapOf("selector" to "input#username", "text" to "admin")
            "BROWSER_SCROLL" -> mapOf("direction" to "DOWN", "amount" to "500")
            "BROWSER_WAIT_SELECTOR" -> mapOf("selector" to "div.content", "timeout" to "10000")
            "BROWSER_EXEC_JS" -> mapOf("script" to "return document.title;")
            "BROWSER_EXTRACT_CONTENT" -> mapOf("selector" to "body", "format" to "TEXT")
            "BROWSER_EXTRACT_FILES" -> mapOf("selector" to "a.download", "target_dir" to "/workspace/browser_downloads")
            "BROWSER_SCREENSHOT" -> mapOf("filename" to "screenshot_1.png")
            "BROWSER_COOKIES" -> mapOf("action" to "GET_ALL")
            "BROWSER_USER_AGENT" -> mapOf("user_agent" to "Mozilla/5.0 (Linux; Android 10)")
            "BROWSER_NAVIGATE" -> mapOf("url" to "https://news.ycombinator.com")
            "BROWSER_REFRESH" -> emptyMap()
            "BROWSER_BACK" -> emptyMap()
            "BROWSER_CLOSE" -> emptyMap()

            // 3. Code Nodes (📜 عقد الكود)
            "CODE_JS" -> mapOf("script" to "// اكتب كود JavaScript هنا\nreturn input;")
            "CODE_PYTHON" -> mapOf("script" to "# اكتب كود Python هنا\nreturn input")
            "CODE_LIBRARY" -> mapOf("script_id" to "script_1", "language" to "JS")

            // 4. Data Nodes (📊 عقد البيانات)
            "DATA_HTTP" -> mapOf("url" to "https://api.example.com/data", "method" to "GET")
            "DATA_PARSE_JSON" -> mapOf("input" to "payload")
            "DATA_STRINGIFY" -> mapOf("input" to "object")
            "DATA_EXTRACT" -> mapOf("path" to "$.store.book[0].title")
            "DATA_SET_VAR" -> mapOf("name" to "my_variable", "value" to "100")
            "DATA_GET_VAR" -> mapOf("name" to "my_variable")

            // 5. Flow Control (🔀 عقد التحكم في التدفق)
            "FLOW_IF" -> mapOf("condition" to "input.value > 10")
            "FLOW_SWITCH" -> mapOf("property" to "status", "cases" to "success,error,pending")
            "FLOW_LOOP" -> mapOf("items" to "input.list", "index" to "i")
            "FLOW_MERGE" -> mapOf("mode" to "WAIT_ALL")
            "FLOW_JOIN" -> mapOf("timeout" to "5000")
            "FLOW_STOP" -> mapOf("status" to "SUCCESS", "message" to "توقف كامل")
            "FLOW_DELAY" -> mapOf("duration" to "3000")
            "FLOW_THROW_ERROR" -> mapOf("message" to "خطأ مخصص من التدفق")
            "FLOW_ERROR_HANDLER" -> mapOf("catch" to "ALL")

            // 6. Wait & Timing (⏱ عقد الانتظار والتوقيت)
            "WAIT_TIME" -> mapOf("duration" to "10")
            "WAIT_UNTIL" -> mapOf("condition" to "global_status == 'ready'")
            "WAIT_TIMER" -> mapOf("delay" to "60")

            // 7. Files (📁 عقد الملفات)
            "FILE_CREATE_FOLDER" -> mapOf("path" to "/workspace/new_folder")
            "FILE_LIST" -> mapOf("directory" to "/workspace")
            "FILE_DELETE" -> mapOf("path" to "/workspace/temp.txt")
            "FILE_ZIP" -> mapOf("action" to "ZIP", "source" to "/workspace/data", "destination" to "/workspace/archive.zip")
            "FILE_MOVE_COPY" -> mapOf("action" to "COPY", "source" to "a.txt", "destination" to "b.txt")

            // 8. Navigation & Screen Control (🧭 عقد التنقل والتحكم في الشاشات)
            "NAV_GO_TO_SCREEN" -> mapOf("screen" to "Runner", "mode" to "READ_ONLY")
            "NAV_CLOSE_SCREEN" -> mapOf("screen" to "Browser")
            "NAV_SAVE_SCREEN" -> mapOf("key" to "shared_data", "value" to "payload")
            "NAV_READ_SCREEN" -> mapOf("key" to "shared_data")
            "NAV_SHOW_NOTIF" -> mapOf("title" to "تنبيه الأتمتة", "message" to "محتوى مخصص")
            "NAV_VIBRATE" -> mapOf("pattern" to "SHORT")
            "NAV_PLAY_SOUND" -> mapOf("sound" to "beep.wav")
            "NAV_COPY_CLIPBOARD" -> mapOf("text" to "payload")
            "NAV_TAKE_PHOTO" -> mapOf("filename" to "photo.jpg")
            "NAV_GET_GPS" -> mapOf("accuracy" to "HIGH")
            "NAV_READ_NFC_QR" -> mapOf("type" to "QR_CODE")
            "NAV_OVERLAY" -> mapOf("text" to "تنبيه تراكبي", "duration" to "5")

            // 9. Raccord (🧩 عقد Raccord)
            "RACCORD_RUN" -> mapOf("raccord_id" to "raccord_1", "username" to "admin")
            "RACCORD_CONDITIONAL" -> mapOf("raccord_id" to "raccord_1")

            // 10. Selector (🎯 عقد Selector)
            "SELECTOR_EXTRACT" -> mapOf("selector_id" to "selector_1")
            "SELECTOR_WAIT" -> mapOf("selector_id" to "selector_1")
            "SELECTOR_CLICK" -> mapOf("selector_id" to "selector_1")
            "SELECTOR_TYPE" -> mapOf("selector_id" to "selector_1", "text" to "input_value")

            // 11. Advanced (🛠️ عقد متقدمة)
            "ADV_AI" -> mapOf("prompt" to "لخص البيانات المدخلة", "model" to "gemini-3.5-flash")
            "ADV_IMAGE" -> mapOf("action" to "RESIZE", "width" to "800", "height" to "600")
            "ADV_CSV_JSON" -> mapOf("action" to "JSON_TO_CSV")
            "ADV_ENCRYPT" -> mapOf("action" to "ENCRYPT", "algorithm" to "AES", "key" to "secret_key")
            "ADV_EMAIL" -> mapOf("to" to "user@example.com", "subject" to "تم التنفيذ", "smtp_server" to "smtp.example.com")
            "ADV_SOAP_GRAPHQL" -> mapOf("type" to "GRAPHQL", "query" to "{ query { data } }")
            "ADV_ADB" -> mapOf("command" to "shell input tap 500 500")

            // 12. Documentation (📝 عقد التوثيق والملاحظات)
            "DOC_STICKY" -> mapOf("text" to "اكتب ملاحظتك هنا لتنظيم التدفق", "color" to "YELLOW")
            "DOC_RICH_NOTE" -> mapOf("content" to "اكتب نصاً غنياً هنا")
            "DOC_COMMENT" -> mapOf("target_node_id" to "", "text" to "تعليق سريع")

            else -> emptyMap()
        }

        currentList.add(FlowNode(newId, name, type, posX, posY, props))
        nodes.value = currentList
        saveCurrentWorkflow()
    }

    fun centerAllNodes() {
        val currentNodes = nodes.value
        if (currentNodes.isEmpty()) {
            panOffset.value = Pair(0f, 0f)
            zoom.value = 1.0f
            return
        }
        val minX = currentNodes.minOf { it.x }
        val maxX = currentNodes.maxOf { it.x }
        val minY = currentNodes.minOf { it.y }
        val maxY = currentNodes.maxOf { it.y }

        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f

        zoom.value = 1.0f
        panOffset.value = Pair(200f - centerX, 300f - centerY)
    }

    fun updateNodePosition(nodeId: String, newX: Float, newY: Float) {
        val x = if (snapToGrid.value) {
            val size = gridSize.value
            (Math.round(newX / size) * size).toFloat()
        } else {
            newX
        }
        val y = if (snapToGrid.value) {
            val size = gridSize.value
            (Math.round(newY / size) * size).toFloat()
        } else {
            newY
        }
        val currentList = nodes.value.map { node ->
            if (node.id == nodeId) node.copy(x = x, y = y) else node
        }
        nodes.value = currentList
        saveCurrentWorkflow()
    }

    fun updateNodeProperties(nodeId: String, properties: Map<String, String>) {
        val currentList = nodes.value.map { node ->
            if (node.id == nodeId) node.copy(properties = properties) else node
        }
        nodes.value = currentList
        saveCurrentWorkflow()
    }

    fun deleteNode(nodeId: String) {
        // Delete Node
        nodes.value = nodes.value.filter { it.id != nodeId }
        // Delete any associated connections
        connections.value = connections.value.filter { it.fromNode != nodeId && it.toNode != nodeId }
        if (selectedNodeId.value == nodeId) {
            selectedNodeId.value = null
        }
        saveCurrentWorkflow()
    }

    fun copyNode(node: FlowNode) {
        val currentList = nodes.value.toMutableList()
        val newId = "node_${UUID.randomUUID().toString().take(6)}"
        val duplicated = node.copy(
            id = newId,
            x = node.x + 50f,
            y = node.y + 50f
        )
        currentList.add(duplicated)
        nodes.value = currentList
        saveCurrentWorkflow()
    }

    fun connectNodes(fromNodeId: String, toNodeId: String) {
        val currentConns = connections.value.toMutableList()
        // Prevent duplicate connections from same output
        currentConns.removeAll { it.fromNode == fromNodeId }
        currentConns.add(FlowConnection(fromNodeId, "output", toNodeId, "input"))
        connections.value = currentConns
        saveCurrentWorkflow()
    }

    fun deleteConnection(conn: FlowConnection) {
        connections.value = connections.value.filter { it != conn }
        saveCurrentWorkflow()
    }

    fun saveCurrentWorkflow() {
        val current = currentWorkflow.value ?: return
        val nodesStr = JsonUtils.serializeNodes(nodes.value)
        val connsStr = JsonUtils.serializeConnections(connections.value)
        
        val updated = current.copy(
            nodesJson = nodesStr,
            connectionsJson = connsStr
        )
        currentWorkflow.value = updated
        
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveWorkflow(updated)
        }
    }

    fun deleteCurrentWorkflow() {
        val current = currentWorkflow.value ?: return
        viewModelScope.launch {
            repository.deleteWorkflow(current)
            val all = repository.allWorkflows.firstOrNull() ?: emptyList()
            if (all.isNotEmpty()) {
                selectWorkflow(all.first())
            } else {
                currentWorkflow.value = null
                nodes.value = emptyList()
                connections.value = emptyList()
            }
        }
    }

    // --- Execution and Logs Operations ---
    fun executeCurrentWorkflow() {
        val current = currentWorkflow.value ?: return
        if (isExecuting.value) return

        viewModelScope.launch {
            isExecuting.value = true
            isPaused.value = false
            
            // Add scenario running to queue
            val queueId = UUID.randomUUID().toString().take(6)
            parallelQueue.value = parallelQueue.value + QueueItem(queueId, current.name, "جاري")

            // Real simulation runner
            val success = automationEngine.executeWorkflow(
                workflow = current,
                startNodeId = startNodeId.value,
                endNodeId = endNodeId.value,
                webhookPayload = webhookPayload.value,
                onLog = { level, nodeName, msg, duration ->
                    // Persistent log entry
                    repository.insertLog(
                        LogEntry(
                            workflowId = current.id,
                            level = level,
                            nodeName = nodeName,
                            message = msg,
                            duration = duration
                        )
                    )
                }
            )

            // Update queue status
            parallelQueue.value = parallelQueue.value.map { item ->
                if (item.id == queueId) item.copy(status = if (success) "مكتمل" else "فشل") else item
            }
            
            isExecuting.value = false
        }
    }

    fun stopExecution() {
        isExecuting.value = false
        isPaused.value = false
        viewModelScope.launch {
            repository.insertLog(
                LogEntry(
                    workflowId = currentWorkflow.value?.id ?: 0,
                    level = "ERROR",
                    nodeName = "تحكم المشغّل",
                    message = "🛑 تم إيقاف التنفيذ يدوياً بواسطة المستخدم."
                )
            )
        }
    }

    fun togglePauseExecution() {
        isPaused.value = !isPaused.value
        viewModelScope.launch {
            repository.insertLog(
                LogEntry(
                    workflowId = currentWorkflow.value?.id ?: 0,
                    level = "WARNING",
                    nodeName = "تتبع العقد",
                    message = if (isPaused.value) "⏸ تم تعليق التنفيذ مؤقتاً في وضع التتبع." else "▶ تم استئناف التنفيذ بنجاح."
                )
            )
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    // --- Workspace File Operations ---
    fun createWorkspaceFile(path: String, content: String, isDirectory: Boolean) {
        viewModelScope.launch {
            repository.saveFile(WorkspaceFile(path = path, isDirectory = isDirectory, content = content))
            openFileInEditor(path)
        }
    }

    fun openFileInEditor(path: String) {
        selectedFilePath.value = path
        val list = openFileTabs.value.toMutableList()
        if (!list.contains(path)) {
            list.add(path)
            openFileTabs.value = list
        }
        viewModelScope.launch {
            val file = repository.getFileByPath(path)
            editingFileContent.value = file?.content ?: ""
        }
    }

    fun saveEditingFile() {
        val path = selectedFilePath.value ?: return
        viewModelScope.launch {
            repository.saveFile(WorkspaceFile(path = path, isDirectory = false, content = editingFileContent.value))
            repository.insertLog(
                LogEntry(
                    workflowId = currentWorkflow.value?.id ?: 0,
                    level = "SUCCESS",
                    nodeName = "مساحة العمل",
                    message = "💾 تم حفظ التعديلات في الملف: $path"
                )
            )
        }
    }

    fun deleteWorkspaceFile(file: WorkspaceFile) {
        viewModelScope.launch {
            repository.deleteFile(file)
            val openTabs = openFileTabs.value.filter { it != file.path }
            openFileTabs.value = openTabs
            if (selectedFilePath.value == file.path) {
                selectedFilePath.value = openTabs.firstOrNull()
                if (selectedFilePath.value != null) {
                    val nextFile = repository.getFileByPath(selectedFilePath.value!!)
                    editingFileContent.value = nextFile?.content ?: ""
                } else {
                    editingFileContent.value = ""
                }
            }
        }
    }

    // --- Library Operations ---
    fun addLibraryItem(name: String, type: String, content: String, tags: String = "") {
        viewModelScope.launch {
            repository.saveLibraryItem(LibraryItem(name = name, type = type, content = content, tags = tags))
        }
    }

    fun deleteLibraryItem(item: LibraryItem) {
        viewModelScope.launch {
            repository.deleteLibraryItem(item)
        }
    }

    // --- Secret/Vault Operations ---
    fun saveSecretKey(key: String, value: String) {
        viewModelScope.launch {
            repository.saveSecret(key, value)
        }
    }

    fun deleteSecretKey(key: String) {
        viewModelScope.launch {
            repository.deleteSecret(key)
        }
    }

    // --- Browser Recorded Integrations ---
    fun startRecording() {
        isRecordingRaccord.value = true
        recordedSteps.value = listOf(
            BrowserStep(1, "navigate", value = currentUrl.value)
        )
    }

    fun addRecordingStep(action: String, target: String, value: String) {
        if (!isRecordingRaccord.value) return
        val current = recordedSteps.value.toMutableList()
        val nextStep = current.size + 1
        current.add(BrowserStep(nextStep, action, target, value))
        recordedSteps.value = current
    }

    fun stopAndSaveRecording(name: String) {
        isRecordingRaccord.value = false
        val stepsList = recordedSteps.value
        if (stepsList.isEmpty()) return

        // Convert stepsList to String/JSON representation
        val jsonArr = JSONArray()
        stepsList.forEach { s ->
            val obj = JSONObject()
            obj.put("step", s.step)
            obj.put("action", s.action)
            obj.put("target", s.target)
            obj.put("value", s.value)
            jsonArr.put(obj)
        }

        viewModelScope.launch {
            repository.saveLibraryItem(
                LibraryItem(
                    name = name,
                    type = "RACCORD",
                    content = jsonArr.toString(),
                    tags = "موصل,مسجل,ويب"
                )
            )
        }
        recordedSteps.value = emptyList()
    }

    fun saveCapturedSelector(name: String, path: String) {
        viewModelScope.launch {
            repository.saveLibraryItem(
                LibraryItem(
                    name = name,
                    type = "SELECTOR",
                    content = path,
                    tags = "محدد,مستخرج"
                )
            )
        }
    }

    // --- AI Copilot workflow builder ---
    fun generateAiWorkflow() {
        val prompt = copilotPrompt.value
        if (prompt.isBlank() || isCopilotGenerating.value) return

        isCopilotGenerating.value = true
        copilotResponse.value = ""

        viewModelScope.launch(Dispatchers.IO) {
            val apiKeyObj = repository.getSecret("GEMINI_API_KEY")
            val apiKey = apiKeyObj?.value ?: ""

            if (apiKey.isNotEmpty() && apiKey != "AI_STUDIO_INJECTED_SECURE_TOKEN") {
                try {
                    val systemPrompt = """
                        You are an expert workflow designer in JSON. The user wants to build a workflow based on their query.
                        You must return a raw JSON object containing two fields: "nodes" (as a JSON array of FlowNodes) and "connections" (as a JSON array of FlowConnections).
                        
                        FlowNode structure:
                        {
                          "id": "node_unique_id",
                          "name": "Node name in Arabic",
                          "type": "TRIGGER" or "BROWSER" or "CODE" or "AI" or "HTTP" or "FILE_READ" or "FILE_WRITE" or "NOTIFICATION" or "WAIT",
                          "x": Float coordinate (e.g. 100 to 900),
                          "y": Float coordinate (e.g. 100 to 600),
                          "properties": { "property_key": "value" }
                        }
                        
                        FlowConnection structure:
                        {
                          "fromNode": "id_of_source_node",
                          "fromPort": "output",
                          "toNode": "id_of_destination_node",
                          "toPort": "input"
                        }
                        
                        Make sure your entire output is just the raw JSON, no markdown code block fences (e.g. ```json), and no extra conversational text. Valid Arabic node names.
                    """.trimIndent()

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val reqObj = JSONObject()
                    
                    val contentsArr = JSONArray()
                    val contentObj = JSONObject()
                    val partsArr = JSONArray()
                    val partObj = JSONObject()
                    partObj.put("text", "طلب المستخدم بالعربية: $prompt")
                    partsArr.put(partObj)
                    contentObj.put("parts", partsArr)
                    contentsArr.put(contentObj)
                    reqObj.put("contents", contentsArr)

                    // Add system prompt inside generationConfig or as content
                    val systemInstructionObj = JSONObject()
                    val sysParts = JSONArray()
                    sysParts.put(JSONObject().put("text", systemPrompt))
                    systemInstructionObj.put("parts", sysParts)
                    reqObj.put("systemInstruction", systemInstructionObj)

                    val request = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                        .post(RequestBody.create(mediaType, reqObj.toString()))
                        .build()

                    val response = OkHttpClient().newCall(request).execute()
                    val body = response.body?.string() ?: ""
                    
                    val resObj = JSONObject(body)
                    val textResult = resObj.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                        .replace("```json", "")
                        .replace("```", "")
                        .trim()

                    // Parse textResult as JSON containing "nodes" and "connections"
                    val generatedJson = JSONObject(textResult)
                    val generatedNodes = generatedJson.getJSONArray("nodes").toString()
                    val generatedConns = generatedJson.getJSONArray("connections").toString()

                    withContext(Dispatchers.Main) {
                        // Create a new workflow loaded with the parsed nodes
                        val newWf = Workflow(
                            name = "أتمتة ذكية: ${prompt.take(30)}...",
                            nodesJson = generatedNodes,
                            connectionsJson = generatedConns
                        )
                        val id = repository.saveWorkflow(newWf)
                        val created = repository.getWorkflowById(id.toInt())
                        if (created != null) {
                            selectWorkflow(created)
                        }
                        copilotResponse.value = "✅ نجح المساعد الذكي ببناء سير العمل وتثبيته في لوحة التحكم!"
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        buildLocalAiWorkflowFallback(prompt)
                    }
                }
            } else {
                // Key is missing or default placeholder, build beautiful rules-based local workflow matching prompt keywords
                delay(1200)
                withContext(Dispatchers.Main) {
                    buildLocalAiWorkflowFallback(prompt)
                }
            }
            isCopilotGenerating.value = false
        }
    }

    private fun buildLocalAiWorkflowFallback(prompt: String) {
        // Built-in intelligent builder depending on Arabic keywords
        val name: String
        val nodesStr: String
        val connsStr: String

        if (prompt.contains("فيسبوك") || prompt.contains("تواصل") || prompt.contains("تغريد") || prompt.contains("مواقع")) {
            name = "تزامن شبكات التواصل الاجتماعي"
            nodesStr = """
                [
                  {"id": "ai_trig", "name": "بداية مجدولة (Trigger)", "type": "TRIGGER_CRON", "x": 100, "y": 200, "properties": {"cron": "0 12 * * *"}},
                  {"id": "ai_http", "name": "جلب المنشورات الهامة", "type": "HTTP", "x": 350, "y": 180, "properties": {"url": "https://api.coindesk.com/v1/bpi/currentprice.json", "method": "GET"}},
                  {"id": "ai_gpt", "name": "إعادة صياغة ذكية بالذكاء الاصطناعي", "type": "AI", "x": 600, "y": 180, "properties": {"prompt": "صغ هذه التحديثات في صيغة منشور ترويجي جذاب للعربية"}},
                  {"id": "ai_notify", "name": "إرسال الإشعار والمنشور جاهز", "type": "NOTIFICATION", "x": 850, "y": 200, "properties": {"title": "تمت الصياغة", "message": "التحديث جاهز للنشر تلقائياً!"}}
                ]
            """.trimIndent()
            connsStr = """
                [
                  {"fromNode": "ai_trig", "fromPort": "output", "toNode": "ai_http", "toPort": "input"},
                  {"fromNode": "ai_http", "fromPort": "output", "toNode": "ai_gpt", "toPort": "input"},
                  {"fromNode": "ai_gpt", "fromPort": "output", "toNode": "ai_notify", "toPort": "input"}
                ]
            """.trimIndent()
        } else if (prompt.contains("تحميل") || prompt.contains("ملف") || prompt.contains("حفظ") || prompt.contains("جدول")) {
            name = "أتمتة ملفات التقارير وتصديرها"
            nodesStr = """
                [
                  {"id": "ai_trig", "name": "التشغيل اليدوي للتقرير", "type": "TRIGGER", "x": 100, "y": 200, "properties": {"event": "تشغيل يدوي"}},
                  {"id": "ai_read", "name": "قراءة ملف قائمة المستخدمين", "type": "FILE_READ", "x": 350, "y": 180, "properties": {"path": "/data/users_list.csv"}},
                  {"id": "ai_ai", "name": "فرز ومعالجة القائمة", "type": "AI", "x": 600, "y": 180, "properties": {"prompt": "حدد المستخدمين النشطين ورتبهم في جدول منظم"}},
                  {"id": "ai_write", "name": "تصدير التقرير النهائي", "type": "FILE_WRITE", "x": 850, "y": 200, "properties": {"path": "/workspace/active_users_report.md"}}
                ]
            """.trimIndent()
            connsStr = """
                [
                  {"fromNode": "ai_trig", "fromPort": "output", "toNode": "ai_read", "toPort": "input"},
                  {"fromNode": "ai_read", "fromPort": "output", "toNode": "ai_ai", "toPort": "input"},
                  {"fromNode": "ai_ai", "fromPort": "output", "toNode": "ai_write", "toPort": "input"}
                ]
            """.trimIndent()
        } else {
            // General multi-node beautiful flow
            name = "سير عمل مخصص: " + prompt.take(20) + "..."
            nodesStr = """
                [
                  {"id": "ai_trig", "name": "إشارة البداية المخصصة", "type": "TRIGGER", "x": 120, "y": 200, "properties": {"event": "بداية"}},
                  {"id": "ai_browser", "name": "فتح المتصفح المساعد", "type": "BROWSER", "x": 360, "y": 180, "properties": {"url": "https://news.ycombinator.com", "action": "جلب الأخبار"}},
                  {"id": "ai_ai", "name": "تحليل واستخراج ذكي", "type": "AI", "x": 600, "y": 180, "properties": {"prompt": "استخرج أفضل الأخبار وقم بترجمتها للعربية"}},
                  {"id": "ai_notify", "name": "إشعار بالتحديثات", "type": "NOTIFICATION", "x": 840, "y": 220, "properties": {"title": "تحديثات ذكية", "message": "أخبار اليوم جاهزة ومترجمة!"}}
                ]
            """.trimIndent()
            connsStr = """
                [
                  {"fromNode": "ai_trig", "fromPort": "output", "toNode": "ai_browser", "toPort": "input"},
                  {"fromNode": "ai_browser", "fromPort": "output", "toNode": "ai_ai", "toPort": "input"},
                  {"fromNode": "ai_ai", "fromPort": "output", "toNode": "ai_notify", "toPort": "input"}
                ]
            """.trimIndent()
        }

        viewModelScope.launch {
            val newWf = Workflow(
                name = name,
                nodesJson = nodesStr,
                connectionsJson = connsStr
            )
            val id = repository.saveWorkflow(newWf)
            val created = repository.getWorkflowById(id.toInt())
            if (created != null) {
                selectWorkflow(created)
            }
            copilotResponse.value = "✨ تم إنشاء سير العمل '${name}' بنجاح محلياً!"
        }
    }
}
