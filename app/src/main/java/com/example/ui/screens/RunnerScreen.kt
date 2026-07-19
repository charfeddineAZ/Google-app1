package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FlowNode
import com.example.data.LogEntry
import com.example.ui.WorkflowViewModel
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunnerScreen(
    viewModel: WorkflowViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToEditor: () -> Unit
) {
    val currentWf by viewModel.currentWorkflow.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val isExecuting by viewModel.isExecuting.collectAsStateWithLifecycle()
    val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()
    val startNodeId by viewModel.startNodeId.collectAsStateWithLifecycle()
    val endNodeId by viewModel.endNodeId.collectAsStateWithLifecycle()
    val webhookPayload by viewModel.webhookPayload.collectAsStateWithLifecycle()
    val logsFilter by viewModel.logsFilter.collectAsStateWithLifecycle()
    val inspectedLogNode by viewModel.inspectedLogNode.collectAsStateWithLifecycle()
    val parallelQueue by viewModel.parallelQueue.collectAsStateWithLifecycle()
    val nodes by viewModel.nodes.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var activeTab by remember { mutableStateOf(0) } // 0: تشغيل كامل, 1: انتقائي, 2: تتبع (Debug), 3: جدولة
    val tabs = listOf("تشغيل كامل", "تشغيل انتقائي", "تتبع Debug", "مجدول Cron")

    var isQueueOpen by remember { mutableStateOf(false) }
    var cronInput by remember { mutableStateOf("*/15 * * * *") }
    var isSchedulerActive by remember { mutableStateOf(false) }

    val filteredLogs = remember(logs, logsFilter, currentWf) {
        val wfLogs = logs.filter { it.workflowId == (currentWf?.id ?: -1) }
        if (logsFilter == "ALL") wfLogs else wfLogs.filter { it.level == logsFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تشغيل الأتمتة والسجلات",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                },
                actions = {
                    IconButton(onClick = { isQueueOpen = !isQueueOpen }) {
                        Icon(
                            imageVector = Icons.Default.Queue,
                            contentDescription = "قائمة الانتظار",
                            tint = if (isQueueOpen) Color(0xFF00E5FF) else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "الإعدادات")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Active Workflow banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "السيناريو النشط حالياً:",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = currentWf?.name ?: "لم يتم اختيار سيناريو",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Button(
                            onClick = onNavigateToEditor,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("المحرر", color = Color(0xFF00E5FF), fontSize = 12.sp)
                        }
                    }
                }

                // Modes Tab Bar
                ScrollableTabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF00E5FF),
                    edgePadding = 0.dp,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    tabs.forEachIndexed { index, label ->
                        Tab(
                            selected = activeTab == index,
                            onClick = { activeTab = index },
                            text = { Text(label, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                // Active Mode Controls
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        when (activeTab) {
                            0 -> { // Full run
                                Text(
                                    "تشغيل كامل للسيناريو يبدأ من أول عقدة Trigger ويمر بجميع العقد المتصلة تلو الأخرى.",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                OutlinedTextField(
                                    value = webhookPayload,
                                    onValueChange = { viewModel.webhookPayload.value = it },
                                    label = { Text("بيانات Payload التجريبية (JSON)", color = Color.Gray) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF00E5FF),
                                        unfocusedBorderColor = Color.Gray,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.LightGray
                                    ),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    maxLines = 4
                                )
                            }
                            1 -> { // Selective run
                                Text(
                                    "حدد عقدة البداية والنهاية لتشغيل جزء مخصص فقط من سير العمل المطور.",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Start node selection dropdown mock
                                    var startMenuOpen by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedTextField(
                                            value = nodes.find { it.id == startNodeId }?.name ?: (startNodeId ?: "البداية"),
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("عقدة البداية", color = Color.Gray) },
                                            trailingIcon = {
                                                IconButton(onClick = { startMenuOpen = true }) {
                                                    Icon(Icons.Default.ArrowDropDown, "", tint = Color.White)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                                        )
                                        DropdownMenu(expanded = startMenuOpen, onDismissRequest = { startMenuOpen = false }) {
                                            nodes.forEach { n ->
                                                DropdownMenuItem(
                                                    text = { Text(n.name) },
                                                    onClick = {
                                                        viewModel.startNodeId.value = n.id
                                                        startMenuOpen = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // End node selection dropdown mock
                                    var endMenuOpen by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedTextField(
                                            value = nodes.find { it.id == endNodeId }?.name ?: (endNodeId ?: "النهاية"),
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("عقدة النهاية", color = Color.Gray) },
                                            trailingIcon = {
                                                IconButton(onClick = { endMenuOpen = true }) {
                                                    Icon(Icons.Default.ArrowDropDown, "", tint = Color.White)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                                        )
                                        DropdownMenu(expanded = endMenuOpen, onDismissRequest = { endMenuOpen = false }) {
                                            nodes.forEach { n ->
                                                DropdownMenuItem(
                                                    text = { Text(n.name) },
                                                    onClick = {
                                                        viewModel.endNodeId.value = n.id
                                                        endMenuOpen = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> { // Trace / Debug
                                Text(
                                    "تشغيل الأتمتة خطوة بخطوة مع إمكانية استخدام نقاط التوقف لمعاينة البيانات المتنقلة بين العقد بدقة.",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("تفعيل نقاط التوقف التلقائية (Breakpoints)", color = Color.White, fontSize = 14.sp)
                                    Switch(
                                        checked = isPaused,
                                        onCheckedChange = { viewModel.togglePauseExecution() },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                                    )
                                }
                            }
                            3 -> { // Scheduler / Cron
                                Text(
                                    "أدخل تعبير Cron لجدولة سير العمل ليتم تشغيله تلقائياً في الخلفية.",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = cronInput,
                                        onValueChange = { cronInput = it },
                                        label = { Text("تعبير Cron (دقيقة ساعة يوم...)", color = Color.Gray) },
                                        modifier = Modifier.weight(1.5f),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                                        textStyle = TextStyle(fontFamily = FontFamily.Monospace)
                                    )
                                    Row(
                                        modifier = Modifier.weight(1.2f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Text(
                                            if (isSchedulerActive) "مفعّل" else "معطل",
                                            color = if (isSchedulerActive) Color(0xFF00E676) else Color.Gray,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                        Switch(
                                            checked = isSchedulerActive,
                                            onCheckedChange = {
                                                isSchedulerActive = it
                                                Toast.makeText(context, if (it) "تم تفعيل الجدولة!" else "تم تعطيل الجدولة", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E676))
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("جدولة سريعة ومقترحة:", fontSize = 12.sp, color = Color.Gray)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("*/15 * * * *" to "كل 15د", "0 * * * *" to "كل ساعة", "0 8 * * *" to "كل 8ص").forEach { (expr, title) ->
                                        AssistChip(
                                            onClick = { cronInput = expr },
                                            label = { Text(title, fontSize = 10.sp) },
                                            colors = AssistChipDefaults.assistChipColors(
                                                labelColor = Color(0xFF00E5FF),
                                                containerColor = Color(0xFF334155)
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Controls Actions Group
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!isExecuting) {
                                Button(
                                    onClick = { viewModel.executeCurrentWorkflow() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, "", tint = Color.Black)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("▶ تشغيل سير العمل", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.togglePauseExecution() },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isPaused) Color(0xFF00E5FF) else Color(0xFFFBC02D)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, "", tint = Color.Black)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isPaused) "استئناف" else "إيقاف مؤقت", color = Color.Black, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { viewModel.stopExecution() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Stop, "", tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("⏹ إيقاف كلي", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Log Section Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "السجلات الحية (Live Logs)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Filters row
                        val logFilters = listOf("ALL" to "الكل", "INFO" to "موجز", "ERROR" to "أخطاء")
                        logFilters.forEach { (key, title) ->
                            AssistChip(
                                onClick = { viewModel.logsFilter.value = key },
                                label = { Text(title, fontSize = 10.sp) },
                                border = if (logsFilter == key) BorderStroke(1.dp, Color(0xFF00E5FF)) else null,
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = if (logsFilter == key) Color(0xFF00E5FF) else Color.LightGray,
                                    containerColor = if (logsFilter == key) Color(0xFF1E293B) else Color(0xFF1E293B)
                                )
                            )
                        }

                        IconButton(onClick = { viewModel.clearLogs() }) {
                            Icon(Icons.Default.DeleteSweep, "مسح السجلات", tint = Color.Gray)
                        }
                    }
                }

                // Live logs panel
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F19)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    if (filteredLogs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.InsertComment, "", tint = Color.DarkGray, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "لا توجد سجلات تشغيل حالية.\nاضغط على زر التشغيل لرؤية تدفق البيانات.",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredLogs) { log ->
                                LogRowItem(log) {
                                    // Search node in graph to inspect
                                    val matchedNode = nodes.find { it.name == log.nodeName }
                                    if (matchedNode != null) {
                                        viewModel.inspectedLogNode.value = matchedNode
                                    } else {
                                        viewModel.inspectedLogNode.value = FlowNode(
                                            id = "tmp",
                                            name = log.nodeName,
                                            type = "GENERAL",
                                            x = 0f, y = 0f,
                                            properties = mapOf("السجل" to log.message, "نوع الحدث" to log.level, "الزمن المستغرق" to "${log.duration}ms")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Slide out Node Inspect Panel
            AnimatedVisibility(
                visible = inspectedLogNode != null,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it }),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(310.dp)
                    .align(Alignment.CenterEnd)
            ) {
                inspectedLogNode?.let { node ->
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "تفاصيل العقدة المدققة",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF),
                                    fontSize = 16.sp
                                )
                                IconButton(onClick = { viewModel.inspectedLogNode.value = null }) {
                                    Icon(Icons.Default.Close, "إغلاق", tint = Color.White)
                                }
                            }

                            Divider(color = Color.Gray, modifier = Modifier.padding(vertical = 12.dp))

                            Text("اسم العقدة: ${node.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("نوع العقدة: ${node.type}", color = Color.LightGray, fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("مصفوفة المدخلات والمخرجات (JSON):", color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(Color(0xFF0B0F19), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    item {
                                        val propsJson = JSONObject().apply {
                                            node.properties.forEach { (k, v) -> put(k, v) }
                                        }.toString(2)
                                        Text(
                                            text = propsJson,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = Color(0xFF00E676)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val text = JSONObject().apply {
                                            node.properties.forEach { (k, v) -> put(k, v) }
                                        }.toString(2)
                                        clipboard.setText(AnnotatedString(text))
                                        Toast.makeText(context, "تم نسخ مخرجات JSON!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1.2f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                                ) {
                                    Icon(Icons.Default.ContentCopy, "", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("نسخ JSON", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        viewModel.inspectedLogNode.value = null
                                        onNavigateToEditor()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                                ) {
                                    Text("فتح بالمحرر", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Parallel executions queue bottom dialog
            if (isQueueOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { isQueueOpen = false }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .clickable(enabled = false) {},
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "إدارة العمليات المتوازية والمجدولة",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF00E5FF)
                                )
                                IconButton(onClick = { isQueueOpen = false }) {
                                    Icon(Icons.Default.Close, "", tint = Color.White)
                                }
                            }

                            Divider(color = Color.Gray, modifier = Modifier.padding(vertical = 10.dp))

                            if (parallelQueue.isEmpty()) {
                                Text(
                                    "قائمة التشغيل المتوازي فارغة حالياً.",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp)
                                )
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.height(200.dp)
                                ) {
                                    items(parallelQueue) { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(item.workflowName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                Text("رمز العملية: #${item.id}", color = Color.Gray, fontSize = 11.sp)
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                val color = when (item.status) {
                                                    "جاري" -> Color(0xFF00E5FF)
                                                    "مكتمل" -> Color(0xFF00E676)
                                                    "فشل" -> Color(0xFFD32F2F)
                                                    else -> Color.Gray
                                                }
                                                Text(
                                                    text = item.status,
                                                    color = color,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                                if (item.status == "جاري") {
                                                    IconButton(onClick = { viewModel.stopExecution() }) {
                                                        Icon(Icons.Default.Cancel, "إلغاء", tint = Color.Red, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogRowItem(log: LogEntry, onClick: () -> Unit) {
    val levelColor = when (log.level) {
        "SUCCESS" -> Color(0xFF00E676)
        "ERROR" -> Color(0xFFFF1744)
        "WARNING" -> Color(0xFFFFEA00)
        "INFO" -> Color(0xFF00E5FF)
        else -> Color.White
    }

    val df = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timeStr = df.format(Date(log.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.3f))
            .clickable { onClick() }
            .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "[$timeStr]",
            color = Color.Gray,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 1.dp)
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(levelColor.copy(alpha = 0.15f))
                .border(1.dp, levelColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = log.nodeName,
                color = levelColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.message,
                color = Color.LightGray,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
            if (log.duration > 0) {
                Text(
                    text = "المدة: ${log.duration}ms",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
