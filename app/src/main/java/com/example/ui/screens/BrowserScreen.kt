package com.example.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.WorkflowViewModel

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: WorkflowViewModel,
    onNavigateToEditor: () -> Unit
) {
    val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    val isSelectorModeActive by viewModel.isSelectorModeActive.collectAsStateWithLifecycle()
    val isRecordingRaccord by viewModel.isRecordingRaccord.collectAsStateWithLifecycle()
    val recordedSteps by viewModel.recordedSteps.collectAsStateWithLifecycle()
    val capturedSelectors by viewModel.capturedSelectors.collectAsStateWithLifecycle()

    val context = LocalContext.current

    var urlInput by remember { mutableStateOf(currentUrl) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Tab session management
    var tabsList by remember { mutableStateOf(listOf("Hacker News" to "https://news.ycombinator.com", "Google" to "https://google.com")) }
    var activeTabIndex by remember { mutableStateOf(0) }

    var isRecorderSaveOpen by remember { mutableStateOf(false) }
    var raccordNameInput by remember { mutableStateOf("") }

    var isSelectorSaveOpen by remember { mutableStateOf(false) }
    var selectorNameInput by remember { mutableStateOf("") }
    var capturedSelectorPath by remember { mutableStateOf("") }

    // HTML Table Extractor State
    var isExtractorOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "المتصفح والأتمتة الحية",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    },
                    actions = {
                        // Quick Workflow Link Action
                        IconButton(onClick = {
                            viewModel.addNode("BROWSER", "زيارة: ${currentUrl.take(15)}")
                            Toast.makeText(context, "تم إدراج عقدة المتصفح في المحرر بنجاح!", Toast.LENGTH_SHORT).show()
                            onNavigateToEditor()
                        }) {
                            Icon(imageVector = Icons.Default.Link, contentDescription = "استخدام في Workflow", tint = Color(0xFF00E676))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0F172A)
                    )
                )

                // Navigation Controls & URL Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { webViewInstance?.goBack() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, "", tint = Color.White)
                    }

                    IconButton(
                        onClick = { webViewInstance?.reload() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Refresh, "", tint = Color.White)
                    }

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedBorderColor = Color(0xFF00E5FF)
                        ),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        trailingIcon = {
                            IconButton(onClick = {
                                val destination = if (!urlInput.startsWith("http")) "https://$urlInput" else urlInput
                                urlInput = destination
                                viewModel.currentUrl.value = destination
                                webViewInstance?.loadUrl(destination)
                            }) {
                                Icon(Icons.Default.ArrowForward, "", tint = Color(0xFF00E5FF))
                            }
                        }
                    )
                }

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B0F19))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(tabsList) { idx, tabItem ->
                        val (title, url) = tabItem
                        val isSelected = activeTabIndex == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    activeTabIndex = idx
                                    urlInput = url
                                    viewModel.currentUrl.value = url
                                    webViewInstance?.loadUrl(url)
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(title, fontSize = 10.sp, color = if (isSelected) Color(0xFF00E5FF) else Color.Gray)
                                if (tabsList.size > 1) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Close,
                                        "",
                                        tint = Color.Gray,
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clickable {
                                                tabsList = tabsList.filterIndexed { i, _ -> i != idx }
                                                activeTabIndex = 0
                                                val nextUrl = tabsList.first().second
                                                urlInput = nextUrl
                                                viewModel.currentUrl.value = nextUrl
                                                webViewInstance?.loadUrl(nextUrl)
                                            }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // New tab
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B))
                                .clickable {
                                    tabsList = tabsList + ("مبوبة جديدة" to "https://example.com")
                                    activeTabIndex = tabsList.size - 1
                                    urlInput = "https://example.com"
                                    viewModel.currentUrl.value = "https://example.com"
                                    webViewInstance?.loadUrl("https://example.com")
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, "", tint = Color(0xFF00E5FF), modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Automation top status heads
            AnimatedVisibility(
                visible = isSelectorModeActive || isRecordingRaccord,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelectorModeActive) Color(0xFF2E7D32) else Color(0xFFC62828))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSelectorModeActive) "🎯 وضع التقاط المحددات نشط (اضغط على أي عنصر)" else "🔴 جاري تسجيل موصل المتصفح Raccord...",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isRecordingRaccord) {
                        Button(
                            onClick = { isRecorderSaveOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("⏹ حفظ وتسجيل", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.isSelectorModeActive.value = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Cancel, "", tint = Color.White)
                        }
                    }
                }
            }

            LaunchedEffect(isSelectorModeActive, isRecordingRaccord, webViewInstance) {
                webViewInstance?.evaluateJavascript(
                    "if (window.setAutomationState) { window.setAutomationState($isSelectorModeActive, $isRecordingRaccord); }",
                    null
                )
            }

            // Web Content Area: holding WebView and overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Real Android WebView integration with automation hooks!
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    url?.let {
                                        urlInput = it
                                        viewModel.currentUrl.value = it
                                        // Update tab item URL as well
                                        tabsList = tabsList.mapIndexed { idx, (t, u) ->
                                            if (idx == activeTabIndex) t to it else t to u
                                        }
                                    }
                                    
                                    val script = """
                                        window.isSelectorMode = $isSelectorModeActive;
                                        window.isRecording = $isRecordingRaccord;

                                        window.setAutomationState = function(isSelector, isRec) {
                                            window.isSelectorMode = isSelector;
                                            window.isRecording = isRec;
                                        };

                                        if (!window.hasAutomationListeners) {
                                            window.hasAutomationListeners = true;

                                            function getCssSelector(el) {
                                                if (!(el instanceof Element)) return '';
                                                if (el.id) {
                                                    return '#' + el.id;
                                                }
                                                var path = [];
                                                while (el && el.nodeType === Node.ELEMENT_NODE) {
                                                    var selector = el.nodeName.toLowerCase();
                                                    if (el.id) {
                                                        selector += '#' + el.id;
                                                        path.unshift(selector);
                                                        break;
                                                    } else {
                                                        var sib = el, nth = 1;
                                                        while (sib = sib.previousElementSibling) {
                                                            if (sib.nodeName.toLowerCase() == el.nodeName.toLowerCase())
                                                                nth++;
                                                        }
                                                        if (nth > 1) {
                                                            selector += ":nth-of-type(" + nth + ")";
                                                        }
                                                    }
                                                    path.unshift(selector);
                                                    el = el.parentNode;
                                                }
                                                return path.join(' > ');
                                            }

                                            document.addEventListener('click', function(e) {
                                                var selector = getCssSelector(e.target);
                                                var text = e.target.innerText || e.target.value || '';
                                                
                                                if (window.isSelectorMode) {
                                                    e.preventDefault();
                                                    e.stopPropagation();
                                                    if (window.AndroidAutomation) {
                                                        window.AndroidAutomation.onSelectorCaptured(selector, text.trim().substring(0, 40));
                                                    }
                                                    return false;
                                                }
                                                
                                                if (window.isRecording) {
                                                    if (window.AndroidAutomation) {
                                                        window.AndroidAutomation.onClicked(selector, text.trim().substring(0, 40));
                                                    }
                                                }
                                            }, true);
                                        }
                                    """.trimIndent()
                                    view?.evaluateJavascript(script, null)
                                }
                            }
                            settings.javaScriptEnabled = true
                            addJavascriptInterface(
                                WebAppInterface(
                                    webView = this,
                                    onSelectorCaptured = { selector, text ->
                                        capturedSelectorPath = selector
                                        selectorNameInput = text.ifBlank { "محدد $selector" }
                                        isSelectorSaveOpen = true
                                    },
                                    onClicked = { selector, text ->
                                        viewModel.addRecordingStep("click", selector, text)
                                        Toast.makeText(context, "تم تسجيل نقرة على: $selector", Toast.LENGTH_SHORT).show()
                                    },
                                    onInput = { selector, value ->
                                        viewModel.addRecordingStep("input", selector, value)
                                    }
                                ),
                                "AndroidAutomation"
                            )
                            loadUrl(currentUrl)
                            webViewInstance = this
                        }
                    },
                    update = { view ->
                        webViewInstance = view
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Grab overlay for selector mode simulation in prototype
                if (isSelectorModeActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                    ) {
                        // Create visual mock selectors for the HackerNews layout
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val mockSelectors = listOf(
                                "span.titleline > a" to "عنوان المقالات الرئيسي",
                                "span.score" to "نقاط التقييم",
                                "td.subtext a:last-child" to "تعليقات المقال",
                                "input#search" to "مربع بحث الموقع"
                            )

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)),
                                border = BorderStroke(1.dp, Color(0xFF00E5FF))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("🎯 حدد عنصر من الصفحة لحفظ محدد الـ DOM الخاص به:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    mockSelectors.forEach { (path, label) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable {
                                                    capturedSelectorPath = path
                                                    selectorNameInput = label
                                                    isSelectorSaveOpen = true
                                                }
                                                .padding(6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(label, color = Color(0xFF00E5FF), fontSize = 11.sp)
                                            Text(path, color = Color.LightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Automation Toolbar
            BottomAppBar(
                containerColor = Color(0xFF1E293B),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.height(54.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Selector Mode Button
                    IconButton(onClick = { viewModel.isSelectorModeActive.value = !isSelectorModeActive }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FilterCenterFocus,
                                contentDescription = "محدد",
                                tint = if (isSelectorModeActive) Color(0xFF00E5FF) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text("التقاط محدد", fontSize = 9.sp, color = Color.White)
                        }
                    }

                    // 2. Action Recorder Button
                    IconButton(onClick = {
                        if (isRecordingRaccord) {
                            isRecorderSaveOpen = true
                        } else {
                            viewModel.startRecording()
                            Toast.makeText(context, "بدأ تسجيل الموصل تلقائياً! انتقل أو تفاعل بالصفحة.", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (isRecordingRaccord) Icons.Default.StopCircle else Icons.Default.FiberManualRecord,
                                contentDescription = "تسجيل",
                                tint = if (isRecordingRaccord) Color.Red else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(if (isRecordingRaccord) "إيقاف" else "تسجيل موصل", fontSize = 9.sp, color = Color.White)
                        }
                    }

                    // 3. Extractor table button
                    IconButton(onClick = { isExtractorOpen = true }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.DownloadForOffline, "", tint = Color.White, modifier = Modifier.size(20.dp))
                            Text("استخراج CSV", fontSize = 9.sp, color = Color.White)
                        }
                    }

                    // 4. Custom Scripts list dropdown simulated
                    IconButton(onClick = {
                        Toast.makeText(context, "تم حقن مقتطف JavaScript لاستخراج العناوين!", Toast.LENGTH_SHORT).show()
                        // Append step to recorder if active
                        if (isRecordingRaccord) {
                            viewModel.addRecordingStep("script_evaluate", "document", "extractTitles()")
                        }
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PlayForWork, "", tint = Color.White, modifier = Modifier.size(20.dp))
                            Text("مقتطفات JS", fontSize = 9.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Recorder Save Dialog
        if (isRecorderSaveOpen) {
            AlertDialog(
                onDismissRequest = { isRecorderSaveOpen = false },
                title = { Text("حفظ الموصل المسجل Raccord", color = Color(0xFF00E5FF)) },
                containerColor = Color(0xFF1E293B),
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("سيتم تجميع الخطوات الـ ${recordedSteps.size} المسجلة كأداة موصل أوتوماتيكية جديدة:", fontSize = 11.sp, color = Color.LightGray)

                        LazyColumn(modifier = Modifier.height(110.dp)) {
                            items(recordedSteps) { step ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("خطوة ${step.step}: ${step.action}", color = Color.White, fontSize = 11.sp)
                                    Text(step.value.take(20), color = Color.Gray, fontSize = 10.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = raccordNameInput,
                            onValueChange = { raccordNameInput = it },
                            placeholder = { Text("اسم الموصل (مثال: جلب أسعار السلع التلقائي)", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (raccordNameInput.isNotBlank()) {
                                viewModel.stopAndSaveRecording(raccordNameInput)
                                raccordNameInput = ""
                                isRecorderSaveOpen = false
                                Toast.makeText(context, "تم حفظ الموصل في مكتبة Raccords بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        enabled = raccordNameInput.isNotBlank()
                    ) {
                        Text("تأكيد وحفظ", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isRecorderSaveOpen = false }) {
                        Text("إلغاء", color = Color.White)
                    }
                }
            )
        }

        // Selector Save Dialog
        if (isSelectorSaveOpen) {
            AlertDialog(
                onDismissRequest = { isSelectorSaveOpen = false },
                title = { Text("حفظ المحدد الملتقط", color = Color(0xFF00E5FF)) },
                containerColor = Color(0xFF1E293B),
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("مسار الـ DOM الملتقط:", color = Color.Gray, fontSize = 11.sp)
                        Text(
                            capturedSelectorPath,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00E676),
                            fontSize = 11.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        )

                        OutlinedTextField(
                            value = selectorNameInput,
                            onValueChange = { selectorNameInput = it },
                            placeholder = { Text("أدخل اسماً للمحدد (مثال: زر السلة)", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (selectorNameInput.isNotBlank()) {
                                viewModel.saveCapturedSelector(selectorNameInput, capturedSelectorPath)
                                selectorNameInput = ""
                                isSelectorSaveOpen = false
                                viewModel.isSelectorModeActive.value = false
                                Toast.makeText(context, "تم حفظ المحدد بنجاح في مكتبة Selectors!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        enabled = selectorNameInput.isNotBlank()
                    ) {
                        Text("حفظ المحدد", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isSelectorSaveOpen = false }) {
                        Text("إلغاء", color = Color.White)
                    }
                }
            )
        }

        // Table Extractor Dialog
        if (isExtractorOpen) {
            AlertDialog(
                onDismissRequest = { isExtractorOpen = false },
                title = { Text("مستخرج جداول الـ HTML", color = Color(0xFF00E5FF)) },
                containerColor = Color(0xFF1E293B),
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "تم العثور على جدول واحد (1 Table) في مستند الصفحة الحالي. هل ترغب في تصديره كملف CSV أو JSON إلى مساحة العمل مباشرة؟",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // Extract simulated CSV table data and save to workspace
                            val path = "/data/hacker_news_extracted.csv"
                            val simulatedCsv = "الرقم,العنوان,الرابط\n1,أخبار الأتمتة المبتكرة,https://news.ycombinator.com\n2,مستقبل الذكاء الاصطناعي,https://news.ycombinator.com"
                            viewModel.createWorkspaceFile(path, simulatedCsv, false)
                            isExtractorOpen = false
                            Toast.makeText(context, "تم حفظ الجدول المستخرج في مساحة العمل بنجاح!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                    ) {
                        Text("حفظ كملف CSV", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isExtractorOpen = false }) {
                        Text("إلغاء", color = Color.White)
                    }
                }
            )
        }
    }
}

class WebAppInterface(
    private val webView: android.webkit.WebView,
    private val onSelectorCaptured: (String, String) -> Unit,
    private val onClicked: (String, String) -> Unit,
    private val onInput: (String, String) -> Unit
) {
    @android.webkit.JavascriptInterface
    fun onSelectorCaptured(selector: String, text: String) {
        webView.post {
            onSelectorCaptured(selector, text)
        }
    }

    @android.webkit.JavascriptInterface
    fun onClicked(selector: String, text: String) {
        webView.post {
            onClicked(selector, text)
        }
    }

    @android.webkit.JavascriptInterface
    fun onInput(selector: String, value: String) {
        webView.post {
            onInput(selector, value)
        }
    }
}
