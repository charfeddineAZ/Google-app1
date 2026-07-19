package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AppRepository(private val appDao: AppDao) {

    // --- Workflows ---
    val allWorkflows: Flow<List<Workflow>> = appDao.getAllWorkflows()

    suspend fun getWorkflowById(id: Int): Workflow? = appDao.getWorkflowById(id)

    suspend fun saveWorkflow(workflow: Workflow): Long = appDao.insertWorkflow(workflow)

    suspend fun deleteWorkflow(workflow: Workflow) = appDao.deleteWorkflow(workflow)

    // --- Logs ---
    val allLogs: Flow<List<LogEntry>> = appDao.getAllLogs()

    fun getLogsForWorkflow(workflowId: Int): Flow<List<LogEntry>> = appDao.getLogsForWorkflow(workflowId)

    suspend fun insertLog(log: LogEntry) = appDao.insertLog(log)

    suspend fun clearLogs() = appDao.clearAllLogs()

    // --- Workspace Files ---
    val allFiles: Flow<List<WorkspaceFile>> = appDao.getAllFiles()

    suspend fun getFileByPath(path: String): WorkspaceFile? = appDao.getFileByPath(path)

    suspend fun saveFile(file: WorkspaceFile) = appDao.insertFile(file)

    suspend fun deleteFile(file: WorkspaceFile) = appDao.deleteFile(file)

    suspend fun deleteFilesWithPrefix(prefix: String) = appDao.deleteFilesWithPrefix(prefix)

    // --- Library Items ---
    val allLibraryItems: Flow<List<LibraryItem>> = appDao.getAllLibraryItems()

    fun getLibraryItemsByType(type: String): Flow<List<LibraryItem>> = appDao.getLibraryItemsByType(type)

    suspend fun saveLibraryItem(item: LibraryItem): Long = appDao.insertLibraryItem(item)

    suspend fun deleteLibraryItem(item: LibraryItem) = appDao.deleteLibraryItem(item)

    // --- Secrets ---
    val allSecrets: Flow<List<SecretKey>> = appDao.getAllSecrets()

    suspend fun getSecret(key: String): SecretKey? = appDao.getSecret(key)

    suspend fun saveSecret(key: String, value: String) = appDao.insertSecret(SecretKey(key, value))

    suspend fun deleteSecret(key: String) = appDao.deleteSecret(key)

    // --- Database Seeding ---
    suspend fun seedDatabaseIfEmpty() {
        // Only seed if workflows list is empty
        val currentWorkflows = allWorkflows.firstOrNull() ?: emptyList()
        if (currentWorkflows.isNotEmpty()) return

        // 1. Seed Workflows
        // Workflow 1: AI Data Extraction (استخراج بيانات الويب بالذكاء الاصطناعي)
        val nodes1 = """
            [
              {"id": "node_trigger", "name": "أداة التشغيل (Trigger)", "type": "TRIGGER", "x": 100, "y": 200, "properties": {"event": "تشغيل يدوي"}},
              {"id": "node_browser", "name": "فتح المتصفح", "type": "BROWSER", "x": 350, "y": 180, "properties": {"url": "https://news.ycombinator.com", "action": "جلب محتوى الصفحة"}},
              {"id": "node_ai", "name": "تلخيص الذكاء الاصطناعي", "type": "AI", "x": 620, "y": 180, "properties": {"prompt": "استخرج أفضل 5 مقالات ولخصها في نقاط باللغة العربية"}},
              {"id": "node_file", "name": "حفظ النتيجة في ملف", "type": "FILE_WRITE", "x": 880, "y": 220, "properties": {"path": "/workspace/news_summary.md"}}
            ]
        """.trimIndent()

        val connections1 = """
            [
              {"fromNode": "node_trigger", "fromPort": "output", "toNode": "node_browser", "toPort": "input"},
              {"fromNode": "node_browser", "fromPort": "output", "toNode": "node_ai", "toPort": "input"},
              {"fromNode": "node_ai", "fromPort": "output", "toNode": "node_file", "toPort": "input"}
            ]
        """.trimIndent()

        saveWorkflow(
            Workflow(
                name = "أتمتة استخراج وتلخيص أخبار الويب",
                nodesJson = nodes1,
                connectionsJson = connections1,
                cronExpression = "0 8 * * *", // Every day at 8 AM
                isScheduleActive = true
            )
        )

        // Workflow 2: Automated API Syncer (مزامنة البيانات السحابية)
        val nodes2 = """
            [
              {"id": "node_trigger_cron", "name": "مؤقت الجدولة (Cron)", "type": "TRIGGER_CRON", "x": 100, "y": 150, "properties": {"cron": "*/15 * * * *"}},
              {"id": "node_http", "name": "طلب API خارجي", "type": "HTTP", "x": 350, "y": 150, "properties": {"url": "https://api.coindesk.com/v1/bpi/currentprice.json", "method": "GET"}},
              {"id": "node_code", "name": "تصفية البيانات (JS)", "type": "CODE", "x": 600, "y": 150, "properties": {"script": "const data = JSON.parse(input);\nreturn { price: data.bpi.USD.rate, time: data.time.updated };"}},
              {"id": "node_notify", "name": "إشعار النظام", "type": "NOTIFICATION", "x": 850, "y": 150, "properties": {"title": "تحديث سعر البيتكوين", "message": "السعر الحالي: ${'$'}{price}"}}
            ]
        """.trimIndent()

        val connections2 = """
            [
              {"fromNode": "node_trigger_cron", "fromPort": "output", "toNode": "node_http", "toPort": "input"},
              {"fromNode": "node_http", "fromPort": "output", "toNode": "node_code", "toPort": "input"},
              {"fromNode": "node_code", "fromPort": "output", "toNode": "node_notify", "toPort": "input"}
            ]
        """.trimIndent()

        saveWorkflow(
            Workflow(
                name = "تتبع أسعار العملات الرقمية تلقائياً",
                nodesJson = nodes2,
                connectionsJson = connections2,
                cronExpression = "*/15 * * * *",
                isScheduleActive = false
            )
        )

        // 2. Seed Workspace Files
        saveFile(WorkspaceFile(path = "/README.md", isDirectory = false, content = """
            # 🚀 مرحباً بك في مساحة عمل Workflow Studio!
            
            هنا يمكنك تخزين وإدارة كافة الملفات التي تستخدمها أتمتتك:
            1. **التقارير المستخرجة**: كملفات Markdown أو CSV.
            2. **سكريبتات مخصصة**: بلغة JavaScript أو Python.
            3. **بيانات الإدخال والاختبار**: كملفات JSON أو CSV.
            
            سير العمل الحالي مربوط تلقائياً بهذه الملفات. يمكنك سحبها أو كتابتها مباشرة من عقد 'قراءة ملف' و'كتابة ملف'.
        """.trimIndent()))

        saveFile(WorkspaceFile(path = "/scripts/sample_extractor.js", isDirectory = false, content = """
            // سكريبت مخصص لتصفية واستخراج الروابط الهامة من صفحة الويب
            function extractImportantLinks(htmlContent) {
                const links = [];
                // استخراج الروابط التجريبية
                links.push({ title: "منشور ترحيبي", url: "https://example.com/welcome" });
                links.push({ title: "كتيب الأتمتة", url: "https://example.com/docs" });
                return JSON.stringify(links, null, 2);
            }
        """.trimIndent()))

        saveFile(WorkspaceFile(path = "/data/users_list.csv", isDirectory = false, content = """
            الرقم,الاسم,البريد الإلكتروني,الحالة
            1,أحمد المحمد,ahmed@example.com,نشط
            2,سارة العلي,sara@example.com,نشط
            3,خالد العمر,khaled@example.com,معلق
        """.trimIndent()))

        saveFile(WorkspaceFile(path = "/data/settings.json", isDirectory = false, content = """
            {
              "appName": "Workflow Studio",
              "version": "1.0.0",
              "autoSync": true,
              "preferredLanguage": "ar"
            }
        """.trimIndent()))

        // 3. Seed Library Items
        saveLibraryItem(LibraryItem(
            type = "JS",
            name = "استخراج جميع الروابط (HTML Links Extractor)",
            content = """
                // استخراج كافة الروابط والـ href من مستند HTML كامل
                function extractLinks(html) {
                    const hrefRegex = /href=["'](https?:\/\/[^"']+)["']/g;
                    const matches = [];
                    let match;
                    while ((match = hrefRegex.exec(html)) !== null) {
                        matches.push(match[1]);
                    }
                    return Array.from(new Set(matches));
                }
            """.trimIndent(),
            tags = "أتمتة,ويب,روابط",
            version = 1
        ))

        saveLibraryItem(LibraryItem(
            type = "PYTHON",
            name = "تنسيق مصفوفة JSON وتنقيتها (JSON Cleaner)",
            content = """
                # تنظيف البيانات وترشيح العناصر الفارغة من مصفوفة JSON
                import json
                
                def clean_data(json_str):
                    data = json.loads(json_str)
                    cleaned = [item for item in data if item.get('title') and item.get('url')]
                    return json.dumps(cleaned, ensure_ascii=False, indent=2)
            """.trimIndent(),
            tags = "بيانات,تنظيف",
            version = 1
        ))

        saveLibraryItem(LibraryItem(
            type = "SELECTOR",
            name = "عنوان المقال الرئيسي في الـ Hacker News",
            content = "span.titleline > a",
            tags = "محدد,HackerNews",
            version = 1
        ))

        saveLibraryItem(LibraryItem(
            type = "RACCORD",
            name = "تسجيل دخول تجريبي تلقائي (Auto-Login Flow)",
            content = """
                [
                  {"step": 1, "action": "navigate", "value": "https://example.com/login"},
                  {"step": 2, "action": "input", "target": "input#username", "value": "demo_user"},
                  {"step": 3, "action": "input", "target": "input#password", "value": "demo_pass123"},
                  {"step": 4, "action": "click", "target": "button.submit_btn"},
                  {"step": 5, "action": "wait", "value": "2000"}
                ]
            """.trimIndent(),
            tags = "موصل,أتمتة",
            version = 1
        ))

        // 4. Seed Secrets Vault
        saveSecret("GEMINI_API_KEY", "AI_STUDIO_INJECTED_SECURE_TOKEN")
        saveSecret("GITHUB_PERSONAL_ACCESS_TOKEN", "ghp_placeholderTokenForSyncOperations")
        saveSecret("TELEGRAM_BOT_TOKEN", "7489502948:AAH_PlaceholderForNotifications")
    }
}
