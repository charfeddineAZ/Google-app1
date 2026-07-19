package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FlowConnection
import com.example.data.FlowNode
import com.example.ui.WorkflowViewModel
import org.json.JSONObject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowEditorScreen(
    viewModel: WorkflowViewModel,
    onNavigateToRunner: () -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    val workflows by viewModel.workflows.collectAsStateWithLifecycle()
    val currentWf by viewModel.currentWorkflow.collectAsStateWithLifecycle()
    val nodes by viewModel.nodes.collectAsStateWithLifecycle()
    val connections by viewModel.connections.collectAsStateWithLifecycle()
    val selectedNodeId by viewModel.selectedNodeId.collectAsStateWithLifecycle()
    val zoom by viewModel.zoom.collectAsStateWithLifecycle()
    val panOffset by viewModel.panOffset.collectAsStateWithLifecycle()
    val minimapVisible by viewModel.minimapVisible.collectAsStateWithLifecycle()
    val liteMode by viewModel.liteMode.collectAsStateWithLifecycle()
    val snapToGrid by viewModel.snapToGrid.collectAsStateWithLifecycle()
    val gridSize by viewModel.gridSize.collectAsStateWithLifecycle()

    val copilotPrompt by viewModel.copilotPrompt.collectAsStateWithLifecycle()
    val isCopilotGenerating by viewModel.isCopilotGenerating.collectAsStateWithLifecycle()
    val copilotResponse by viewModel.copilotResponse.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val libraryItems by viewModel.libraryItems.collectAsStateWithLifecycle()
    var isEditingNodeScriptOpen by remember { mutableStateOf(false) }
    var editingNodeForScript by remember { mutableStateOf<FlowNode?>(null) }

    var isAddNodeSheetOpen by remember { mutableStateOf(false) }
    var isEditNodeOpen by remember { mutableStateOf(false) }
    var isCopilotOpen by remember { mutableStateOf(false) }
    var isDataMapperOpen by remember { mutableStateOf(false) }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var isGridSettingsOpen by remember { mutableStateOf(false) }
    var selectedPortNodeId by remember { mutableStateOf<String?>(null) } // To coordinate tap-to-connect

    // Workflows switcher state
    var isWfSwitcherOpen by remember { mutableStateOf(false) }
    var newWfNameInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.clickable { isWfSwitcherOpen = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentWf?.name ?: "محرر الأتمتة",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            fontSize = 17.sp
                        )
                        Icon(Icons.Default.ArrowDropDown, "", tint = Color(0xFF00E5FF))
                    }
                },
                actions = {
                    IconButton(onClick = { isCopilotOpen = true }) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "الذكاء الاصطناعي Copilot",
                            tint = Color(0xFF00E5FF)
                        )
                    }
                    IconButton(onClick = { viewModel.saveCurrentWorkflow() }) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "حفظ سير العمل", tint = Color.White)
                    }
                    IconButton(onClick = onNavigateToRunner) {
                        Icon(imageVector = Icons.Default.PlayCircleFilled, contentDescription = "تشغيل", tint = Color(0xFF00E676))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        bottomBar = {
            // Dynamic Contextual Toolbar
            BottomAppBar(
                containerColor = Color(0xFF1E293B),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.height(60.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedNodeId == null) {
                        // General Tools
                        IconButton(onClick = { isAddNodeSheetOpen = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddBox, "", tint = Color(0xFF00E5FF))
                                Text("إضافة عقدة", fontSize = 9.sp, color = Color.White)
                            }
                        }
                        IconButton(onClick = { viewModel.zoom.value = (zoom + 0.1f).coerceAtMost(2.0f) }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ZoomIn, "", tint = Color.White)
                                Text("تكبير", fontSize = 9.sp, color = Color.LightGray)
                            }
                        }
                        IconButton(onClick = { viewModel.zoom.value = (zoom - 0.1f).coerceAtLeast(0.5f) }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ZoomOut, "", tint = Color.White)
                                Text("تصغير", fontSize = 9.sp, color = Color.LightGray)
                            }
                        }
                        IconButton(onClick = { viewModel.centerAllNodes() }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CenterFocusStrong, "", tint = Color(0xFF00E5FF))
                                Text("توسيط الكل", fontSize = 9.sp, color = Color.White)
                            }
                        }
                        IconButton(onClick = { viewModel.minimapVisible.value = !minimapVisible }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Map, "", tint = if (minimapVisible) Color(0xFF00E5FF) else Color.White)
                                Text("الخريطة", fontSize = 9.sp, color = Color.LightGray)
                            }
                        }
                        IconButton(onClick = { isGridSettingsOpen = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (snapToGrid) Icons.Default.GridOn else Icons.Default.GridOff,
                                    contentDescription = "محاذاة الشبكة",
                                    tint = if (snapToGrid) Color(0xFF00E5FF) else Color.White
                                )
                                Text("المحاذاة", fontSize = 9.sp, color = if (snapToGrid) Color(0xFF00E5FF) else Color.LightGray)
                            }
                        }
                    } else {
                        // Node Selected Tools
                        val selectedNode = nodes.find { it.id == selectedNodeId }
                        IconButton(onClick = { isEditNodeOpen = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SettingsSuggest, "", tint = Color(0xFF00E5FF))
                                Text("خصائص", fontSize = 9.sp, color = Color.White)
                            }
                        }
                        IconButton(onClick = { isDataMapperOpen = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.MergeType, "", tint = Color(0xFFFBC02D))
                                Text("ربط نواتج", fontSize = 9.sp, color = Color.White)
                            }
                        }
                        IconButton(onClick = { selectedNode?.let { viewModel.addNode(it.type, "${it.name} (نسخة)") } }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ContentCopy, "", tint = Color.White)
                                Text("مضاعفة", fontSize = 9.sp, color = Color.LightGray)
                            }
                        }
                        IconButton(onClick = { selectedNodeId?.let { viewModel.deleteNode(it) } }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Delete, "", tint = Color(0xFFFF1744))
                                Text("حذف", fontSize = 9.sp, color = Color.LightGray)
                            }
                        }
                        IconButton(onClick = { viewModel.selectedNodeId.value = null }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Close, "", tint = Color.Gray)
                                Text("إلغاء", fontSize = 9.sp, color = Color.LightGray)
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0B0F19)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    // Pan and zoom canvas support
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        viewModel.panOffset.value = Pair(
                            panOffset.first + pan.x,
                            panOffset.second + pan.y
                        )
                        viewModel.zoom.value = (zoom * gestureZoom).coerceIn(0.5f, 2.0f)
                    }
                }
        ) {
            // Infinite Grid Background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridSpacing = gridSize * zoom
                val offsetX = panOffset.first % gridSpacing
                val offsetY = panOffset.second % gridSpacing

                // Draw vertical lines
                var x = offsetX
                while (x < size.width) {
                    drawLine(
                        color = if (snapToGrid) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color(0xFF1E293B).copy(alpha = 0.4f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.3f
                    )
                    x += gridSpacing
                }

                // Draw horizontal lines
                var y = offsetY
                while (y < size.height) {
                    drawLine(
                        color = if (snapToGrid) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color(0xFF1E293B).copy(alpha = 0.4f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.3f
                    )
                    y += gridSpacing
                }
            }

            // Connection Lines Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                connections.forEach { conn ->
                    val fromNodeObj = nodes.find { it.id == conn.fromNode }
                    val toNodeObj = nodes.find { it.id == conn.toNode }

                    if (fromNodeObj != null && toNodeObj != null) {
                        // Nodes dimensions are approx 160 x 70 dp. Scale with zoom.
                        val cardW = 160f * zoom
                        val cardH = 70f * zoom

                        // Output port on right middle of source card
                        val startX = (fromNodeObj.x * zoom) + panOffset.first + cardW
                        val startY = (fromNodeObj.y * zoom) + panOffset.second + (cardH / 2)

                        // Input port on left middle of target card
                        val endX = (toNodeObj.x * zoom) + panOffset.first
                        val endY = (toNodeObj.y * zoom) + panOffset.second + (cardH / 2)

                        // Draw cubic bezier curve for standard polished node routing
                        val path = Path().apply {
                            moveTo(startX, startY)
                            cubicTo(
                                startX + (50f * zoom), startY,
                                endX - (50f * zoom), endY,
                                endX, endY
                            )
                        }

                        drawPath(
                            path = path,
                            color = Color(0xFF00E5FF),
                            style = Stroke(width = 2.5f * zoom)
                        )

                        // Draw connection point dot
                        drawCircle(
                            color = Color(0xFF00E676),
                            radius = 4f * zoom,
                            center = Offset(endX, endY)
                        )
                    }
                }
            }

            // Render Nodes on Canvas
            nodes.forEach { node ->
                val cardW = 160.dp
                val cardH = 70.dp

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                ((node.x * zoom) + panOffset.first).roundToInt(),
                                ((node.y * zoom) + panOffset.second).roundToInt()
                            )
                        }
                        .size(cardW * zoom, cardH * zoom)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedNodeId == node.id) Color(0xFF1E293B) else Color(0xFF0F172A))
                        .border(
                            width = if (selectedNodeId == node.id) 2.dp else 1.dp,
                            color = if (selectedNodeId == node.id) Color(0xFF00E5FF) else Color(0xFF334155),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            viewModel.selectedNodeId.value = node.id
                        }
                        .pointerInput(node.id) {
                            var accumulatedX = node.x
                            var accumulatedY = node.y
                            detectDragGestures(
                                onDragStart = {
                                    accumulatedX = node.x
                                    accumulatedY = node.y
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    accumulatedX += dragAmount.x / zoom
                                    accumulatedY += dragAmount.y / zoom
                                    viewModel.updateNodePosition(
                                        nodeId = node.id,
                                        newX = accumulatedX,
                                        newY = accumulatedY
                                    )
                                }
                            )
                        }
                ) {
                    // Node Content
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Category Icon
                        val isTrigger = node.type.startsWith("TRIGGER")
                        val isBrowser = node.type.startsWith("BROWSER")
                        val isCode = node.type.startsWith("CODE")
                        val isData = node.type.startsWith("DATA") || node.type == "HTTP"
                        val isFlow = node.type.startsWith("FLOW")
                        val isWait = node.type.startsWith("WAIT")
                        val isFile = node.type.startsWith("FILE")
                        val isNav = node.type.startsWith("NAV")
                        val isRaccord = node.type.startsWith("RACCORD")
                        val isSelector = node.type.startsWith("SELECTOR")
                        val isAdvanced = node.type.startsWith("ADV") || node.type == "AI"
                        val isDoc = node.type.startsWith("DOC")

                        val nodeColor = when {
                            isTrigger -> Color(0xFFEF5350)
                            isBrowser -> Color(0xFF42A5F5)
                            isCode -> Color(0xFFFFCA28)
                            isData -> Color(0xFF26C6DA)
                            isFlow -> Color(0xFFAB47BC)
                            isWait -> Color(0xFFFFA726)
                            isFile -> Color(0xFFFF7043)
                            isNav -> Color(0xFF26A69A)
                            isRaccord -> Color(0xFF5C6BC0)
                            isSelector -> Color(0xFFEC407A)
                            isAdvanced -> Color(0xFF9CCC65)
                            isDoc -> Color(0xFF78909C)
                            else -> Color.Gray
                        }

                        val nodeIcon = when {
                            isTrigger -> Icons.Default.FlashOn
                            isBrowser -> Icons.Default.Language
                            isCode -> Icons.Default.Code
                            isData -> Icons.Default.Dns
                            isFlow -> Icons.Default.MergeType
                            isWait -> Icons.Default.HourglassEmpty
                            isFile -> Icons.Default.FolderOpen
                            isNav -> Icons.Default.SettingsSuggest
                            isRaccord -> Icons.Default.Extension
                            isSelector -> Icons.Default.FilterAlt
                            isAdvanced -> Icons.Default.AutoAwesome
                            isDoc -> Icons.Default.Info
                            else -> Icons.Default.Settings
                        }

                        Box(
                            modifier = Modifier
                                .size(30.dp * zoom)
                                .clip(RoundedCornerShape(6.dp))
                                .background(nodeColor.copy(alpha = 0.2f))
                                .border(1.dp, nodeColor, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = nodeIcon,
                                contentDescription = "",
                                tint = nodeColor,
                                modifier = Modifier.size(16.dp * zoom)
                            )
                        }

                        // Labels
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = node.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp * zoom,
                                maxLines = 1
                            )
                            Text(
                                text = node.type,
                                color = Color.Gray,
                                fontSize = 8.sp * zoom,
                                maxLines = 1
                            )
                        }
                    }

                    // --- Input Port Dot (Left-middle) ---
                    Box(
                        modifier = Modifier
                            .size(12.dp * zoom)
                            .align(Alignment.CenterStart)
                            .offset(x = (-6).dp * zoom)
                            .clip(CircleShape)
                            .background(if (selectedPortNodeId != null && selectedPortNodeId != node.id) Color(0xFF00E676) else Color(0xFF334155))
                            .border(1.dp, Color.White, CircleShape)
                            .clickable {
                                val fromId = selectedPortNodeId
                                if (fromId != null && fromId != node.id) {
                                    viewModel.connectNodes(fromId, node.id)
                                    selectedPortNodeId = null
                                    Toast
                                        .makeText(context, "تم توصيل العقدتين!", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                    )

                    // --- Output Port Dot (Right-middle) ---
                    Box(
                        modifier = Modifier
                            .size(12.dp * zoom)
                            .align(Alignment.CenterEnd)
                            .offset(x = 6.dp * zoom)
                            .clip(CircleShape)
                            .background(if (selectedPortNodeId == node.id) Color(0xFF00E5FF) else Color(0xFF00E676))
                            .border(1.dp, Color.White, CircleShape)
                            .clickable {
                                selectedPortNodeId = if (selectedPortNodeId == node.id) null else node.id
                            }
                    )
                }
            }

            // If there is a selected node, draw its floating quick-actions bar directly above it!
            nodes.find { it.id == selectedNodeId }?.let { selectedNode ->
                val cardW = 160.dp
                val cardH = 70.dp
                val barW = 120.dp
                val barH = 36.dp
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                ((selectedNode.x * zoom) + panOffset.first + ((cardW.toPx() * zoom - barW.toPx()) / 2)).roundToInt(),
                                ((selectedNode.y * zoom) + panOffset.second - (barH.toPx() + 8.dp.toPx())).roundToInt()
                            )
                        }
                        .size(barW, barH)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.95f))
                        .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1. Delete (حذف)
                        IconButton(
                            onClick = {
                                viewModel.deleteNode(selectedNode.id)
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف العقدة", tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                        }

                        // Divider
                        Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color(0xFF334155)))

                        // 2. Copy (نسخ)
                        IconButton(
                            onClick = {
                                viewModel.copyNode(selectedNode)
                                Toast.makeText(context, "تم نسخ العقدة وتكرارها!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ العقدة", tint = Color(0xFFFFCA28), modifier = Modifier.size(16.dp))
                        }

                        // Divider
                        Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color(0xFF334155)))

                        // 3. Edit (تعديل)
                        IconButton(
                            onClick = {
                                editingNodeForScript = selectedNode
                                isEditingNodeScriptOpen = true
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل السكريبت والخصائص", tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Canvas HUD Indicators
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مستوى التكبير: ${(zoom * 100).roundToInt()}%",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (selectedPortNodeId != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E5FF))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("وضع التوصيل نشط", color = Color(0xFF00E5FF), fontSize = 10.sp)
                }
            }

            // Minimap HUD
            if (minimapVisible) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(100.dp, 80.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text("الخريطة", fontSize = 8.sp, color = Color.Gray, modifier = Modifier.padding(4.dp))
                        // Draw mini node representations
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            nodes.forEach { n ->
                                val miniX = (n.x / 10f).coerceIn(0f, size.width)
                                val miniY = (n.y / 10f).coerceIn(0f, size.height)
                                drawCircle(
                                    color = Color(0xFF00E5FF),
                                    radius = 2f,
                                    center = Offset(miniX, miniY)
                                )
                            }
                        }
                    }
                }
            }

            // Workflows Switcher Dialog
            if (isWfSwitcherOpen) {
                AlertDialog(
                    onDismissRequest = { isWfSwitcherOpen = false },
                    title = { Text("قائمة السيناريوهات المتاحة", color = Color(0xFF00E5FF)) },
                    containerColor = Color(0xFF1E293B),
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newWfNameInput,
                                onValueChange = { newWfNameInput = it },
                                label = { Text("سيناريو جديد (أدخل الاسم)", color = Color.Gray) },
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = {
                                        if (newWfNameInput.isNotBlank()) {
                                            viewModel.createNewWorkflow(newWfNameInput)
                                            newWfNameInput = ""
                                            isWfSwitcherOpen = false
                                        }
                                    }) {
                                        Icon(Icons.Default.Add, "", tint = Color(0xFF00E676))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Divider(color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))

                            LazyColumn(modifier = Modifier.height(180.dp)) {
                                items(workflows) { wf ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (currentWf?.id == wf.id) Color(0xFF0F172A) else Color.Transparent)
                                            .clickable {
                                                viewModel.selectWorkflow(wf)
                                                isWfSwitcherOpen = false
                                            }
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(wf.name, color = Color.White, fontSize = 13.sp)
                                        if (currentWf?.id == wf.id) {
                                            Icon(Icons.Default.Check, "", tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { isWfSwitcherOpen = false }) {
                            Text("إغلاق", color = Color.White)
                        }
                    }
                )
            }

            // AI Copilot Dialog
            if (isCopilotOpen) {
                AlertDialog(
                    onDismissRequest = { isCopilotOpen = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, "", tint = Color(0xFF00E5FF))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("المساعد الذكي لبناء الأتمتة", color = Color(0xFF00E5FF))
                        }
                    },
                    containerColor = Color(0xFF1E293B),
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "اكتب بالعربية ما ترغب في أتمتته، وسيقوم المساعد الذكي برسم العقد وربطها ببعضها بشكل متكامل فورا!",
                                fontSize = 12.sp,
                                color = Color.LightGray
                            )

                            OutlinedTextField(
                                value = copilotPrompt,
                                onValueChange = { viewModel.copilotPrompt.value = it },
                                label = { Text("مثال: اجلب سعر البيتكوين كل ساعة واحفظ النتيجة في ملف", color = Color.Gray) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                            )

                            if (isCopilotGenerating) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF00E5FF))
                                }
                            }

                            if (copilotResponse.isNotEmpty()) {
                                Text(
                                    text = copilotResponse,
                                    fontSize = 12.sp,
                                    color = Color(0xFF00E676),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                        .padding(8.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.generateAiWorkflow() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            enabled = !isCopilotGenerating && copilotPrompt.isNotBlank()
                        ) {
                            Text("بناء السيناريو 🚀", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { isCopilotOpen = false }) {
                            Text("إلغاء", color = Color.White)
                        }
                    }
                )
            }

            // n8n-style Data Mapper dialog
            if (isDataMapperOpen) {
                AlertDialog(
                    onDismissRequest = { isDataMapperOpen = false },
                    title = { Text("لوحة تخطيط البيانات والمخرجات", color = Color(0xFFFBC02D)) },
                    containerColor = Color(0xFF1E293B),
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "اربط نواتج العقد السابقة كمدخلات للعقد التالية تلقائياً لتوليد تعابير برمجية ديناميكية:",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    item {
                                        Text("عمود المخرجات السابقة (Outputs):", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    items(nodes.filter { it.id != selectedNodeId }) { prevNode ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val expression = "\$node[\"${prevNode.name}\"].output"
                                                    val selectedNode = nodes.find { it.id == selectedNodeId }
                                                    if (selectedNode != null) {
                                                        val updatedProps = selectedNode.properties.toMutableMap()
                                                        // Insert expression in first available prop
                                                        val key = updatedProps.keys.firstOrNull() ?: "expression"
                                                        updatedProps[key] = expression
                                                        viewModel.updateNodeProperties(selectedNode.id, updatedProps)
                                                        Toast.makeText(context, "تم تطبيق الربط!", Toast.LENGTH_SHORT).show()
                                                        isDataMapperOpen = false
                                                    }
                                                }
                                                .padding(6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(prevNode.name, color = Color.White, fontSize = 12.sp)
                                            Text("\$node[\"${prevNode.id}\"]", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { isDataMapperOpen = false }) {
                            Text("إغلاق", color = Color.White)
                        }
                    }
                )
            }

            // Add Node Bottom Sheet panel (🧩 كتالوج العقد الكامل)
            if (isAddNodeSheetOpen) {
                var searchQuery by remember { mutableStateOf("") }
                var selectedCategoryId by remember { mutableStateOf("ALL") }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { isAddNodeSheetOpen = false }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                            .align(Alignment.BottomCenter)
                            .clickable(enabled = false) {},
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AddBox, "", tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "🧩 كتالوج العقد الكامل (12 فئة)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF00E5FF)
                                    )
                                }
                                IconButton(onClick = { isAddNodeSheetOpen = false }) {
                                    Icon(Icons.Default.Close, "", tint = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Search bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("ابحث عن أي عقدة بالاسم أو الوصف...", color = Color.Gray, fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, "", tint = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A),
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color.Gray
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Category selector (Horizontal Row)
                            val catalogCategories = listOf(
                                Triple("ALL", "🔍 الكل", Icons.Default.Search),
                                Triple("TRIGGERS", "🚀 التشغيل", Icons.Default.FlashOn),
                                Triple("BROWSER", "🌐 المتصفح", Icons.Default.Language),
                                Triple("CODE", "📜 الكود", Icons.Default.Code),
                                Triple("DATA", "📊 البيانات", Icons.Default.Dns),
                                Triple("FLOW", "🔀 التحكم", Icons.Default.MergeType),
                                Triple("WAIT", "⏱️ التوقيت", Icons.Default.HourglassEmpty),
                                Triple("FILES", "📁 الملفات", Icons.Default.FolderOpen),
                                Triple("NAV", "🧭 الهاتف", Icons.Default.SettingsSuggest),
                                Triple("RACCORD", "🧩 الموصلات", Icons.Default.Extension),
                                Triple("SELECTOR", "🎯 المحددات", Icons.Default.FilterAlt),
                                Triple("ADVANCED", "🛠️ متقدم", Icons.Default.AutoAwesome),
                                Triple("DOC", "📝 التوثيق", Icons.Default.Info)
                            )

                            ScrollableTabRow(
                                selectedTabIndex = catalogCategories.indexOfFirst { it.first == selectedCategoryId }.coerceAtLeast(0),
                                containerColor = Color.Transparent,
                                contentColor = Color(0xFF00E5FF),
                                edgePadding = 0.dp,
                                divider = {},
                                indicator = {}
                            ) {
                                catalogCategories.forEach { (catId, catName, catIcon) ->
                                    Tab(
                                        selected = selectedCategoryId == catId,
                                        onClick = { selectedCategoryId = catId },
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    catIcon, "",
                                                    tint = if (selectedCategoryId == catId) Color(0xFF00E5FF) else Color.Gray,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(catName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        selectedContentColor = Color(0xFF00E5FF),
                                        unselectedContentColor = Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Data source
                            val fullCatalog = listOf(
                                Pair("TRIGGERS", listOf(
                                    Triple("TRIGGER_MANUAL", "تشغيل يدوي (Manual)", "يبدأ سير العمل يدويًا بضغطة زر"),
                                    Triple("TRIGGER_WEBHOOK", "تكامل Webhook", "تنشئ عنوان URL لاستقبال طلبات الويب"),
                                    Triple("TRIGGER_CRON", "جدولة زمنية (Cron)", "تشغيل دوري تلقائي بجدول زمني"),
                                    Triple("TRIGGER_BROWSER_EVENT", "حدث تصفح الويب", "حدث تلقائي ينشط عند تحميل صفحة أو نقر عنصر"),
                                    Triple("TRIGGER_FILE_WATCHER", "مراقب الملفات", "يبدأ عند إضافة/تعديل ملف بالمسار"),
                                    Triple("TRIGGER_NOTIFICATION", "إشعار نظام", "يبدأ عند استقبال إشعار من نظام التشغيل"),
                                    Triple("TRIGGER_NFC_QR", "مسح NFC / QR", "يبدأ عند مسح رمز الاستجابة السريعة"),
                                    Triple("TRIGGER_VOICE", "أمر صوتي", "يبدأ بالتعرف على كلمة منطوقة مسبقًا"),
                                    Triple("TRIGGER_SHAKE", "اهتزاز الجهاز", "يبدأ عند هز الهاتف بنمط محدد"),
                                    Triple("TRIGGER_SPECIFIC_TIME", "وقت محدد", "يبدأ في تاريخ ووقت معين لمرة واحدة")
                                )),
                                Pair("BROWSER", listOf(
                                    Triple("BROWSER_OPEN_URL", "فتح رابط ويب", "فتح عنوان URL مخصص مع خيارات"),
                                    Triple("BROWSER_CLICK", "نقر على عنصر", "النقر على زر أو رابط بمحدد CSS/XPath"),
                                    Triple("BROWSER_TYPE", "إدخال نص", "إدخال قيمة في حقل إدخال بالمتصفح"),
                                    Triple("BROWSER_SCROLL", "تمرير الصفحة", "التمرير عموديًا/أفقيًا لمستوى معين"),
                                    Triple("BROWSER_WAIT_SELECTOR", "انتظار عنصر", "تعليق التدفق لحين ظهور عنصر مخصص"),
                                    Triple("BROWSER_EXEC_JS", "تشغيل JavaScript", "تنفيذ كود JS في سياق صفحة الويب"),
                                    Triple("BROWSER_EXTRACT_CONTENT", "استخراج نص/HTML", "سحب وتصفية البيانات المحددة"),
                                    Triple("BROWSER_EXTRACT_FILES", "استخراج ملفات", "تحميل الصور أو ملفات PDF تلقائيًا"),
                                    Triple("BROWSER_SCREENSHOT", "لقطة شاشة", "حفظ الصفحة الحالية كصورة PNG"),
                                    Triple("BROWSER_COOKIES", "إدارة الكوكيز", "قراءة أو حفظ الكوكيز والجلسة"),
                                    Triple("BROWSER_USER_AGENT", "تعيين User-Agent", "تغيير هوية المتصفح ديناميكيًا")
                                )),
                                Pair("CODE", listOf(
                                    Triple("CODE_JS", "سكريبت JavaScript", "معالجة المدخلات بلغة JS متكاملة"),
                                    Triple("CODE_PYTHON", "سكريبت Python", "تحليل البيانات بلغة بايثون مدمجة"),
                                    Triple("CODE_LIBRARY", "جلب من المكتبة", "استيراد كود جاهز ومحفوظ مسبقًا")
                                )),
                                Pair("DATA", listOf(
                                    Triple("DATA_HTTP", "طلب ويب HTTP Request", "استدعاء واجهات REST API بدقة"),
                                    Triple("DATA_PARSE_JSON", "تحليل JSON", "تحويل النص إلى كائن برمجي منظم"),
                                    Triple("DATA_STRINGIFY", "تنسيق إلى JSON", "تحويل الكائن إلى نص منظم"),
                                    Triple("DATA_EXTRACT", "استخراج قيمة JSONPath", "استخلاص قيم محددة من هيكل معقد"),
                                    Triple("DATA_SET_VAR", "تعيين متغير عام", "حفظ قيمة في الذاكرة المشتركة"),
                                    Triple("DATA_GET_VAR", "قراءة متغير عام", "استرجاع المتغيرات في مراحل التدفق")
                                )),
                                Pair("FLOW", listOf(
                                    Triple("FLOW_IF", "شرط منطقي (IF/Else)", "توجيه البيانات حسب الشروط"),
                                    Triple("FLOW_SWITCH", "تفرع متعدد (Switch)", "توجيه التدفق لعدة مسارات بدقة"),
                                    Triple("FLOW_LOOP", "حلقة تكرار (Loop)", "تكرار العقد لكل عنصر في مصفوفة"),
                                    Triple("FLOW_MERGE", "دمج المسارات (Merge)", "دمج مخرجات مسارين في تدفق واحد"),
                                    Triple("FLOW_JOIN", "انتظار الكل (Join)", "توقيف السير لحين اكتمال كل الشُعب"),
                                    Triple("FLOW_STOP", "إيقاف السير (Stop)", "إنهاء تنفيذ التدفق وإرجاع قيمة"),
                                    Triple("FLOW_DELAY", "تأخير زمني (Delay)", "انتظار ثواني محددة قبل المواصلة"),
                                    Triple("FLOW_THROW_ERROR", "خطأ متعمد", "إطلاق استثناء لاختبار معالجة الأخطاء"),
                                    Triple("FLOW_ERROR_HANDLER", "معالج الأخطاء", "تشغيل مسار بديل عند حدوث أي خلل")
                                )),
                                Pair("WAIT", listOf(
                                    Triple("WAIT_TIME", "انتظار مؤقت", "توقف مؤقت بالثواني مع تعبيرات"),
                                    Triple("WAIT_UNTIL", "انتظار حتى تحقق شرط", "البقاء معلقًا لحين مطابقة القيم"),
                                    Triple("WAIT_TIMER", "مؤقت خلفي", "إطلاق حدث أوتوماتيكي بالخلفية")
                                )),
                                Pair("FILES", listOf(
                                    Triple("FILE_READ", "قراءة ملف من Workspace", "جلب محتوى نصي أو JSON محلي"),
                                    Triple("FILE_WRITE", "حفظ/كتابة ملف", "تصدير المخرجات لملفات نصية أو CSV"),
                                    Triple("FILE_CREATE_FOLDER", "إنشاء مجلد", "تأسيس بنية تنظيمية للمخرجات"),
                                    Triple("FILE_LIST", "سرد ملفات ومجلدات", "عرض واستعراض مكونات Workspace"),
                                    Triple("FILE_DELETE", "حذف ملف/مجلد", "التنظيف الدوري للمساحات المؤقتة"),
                                    Triple("FILE_ZIP", "ضغط/فك ضغط Zip", "أرشفة المخرجات أو فك حزم البيانات"),
                                    Triple("FILE_MOVE_COPY", "نقل/نسخ ملف", "تنظيم ملفات التدفق الداخلي")
                                )),
                                Pair("NAV", listOf(
                                    Triple("NAV_GO_TO_SCREEN", "الانتقال لشاشة مخصصة", "توجيه واجهة المستخدم لشاشة معينة"),
                                    Triple("NAV_CLOSE_SCREEN", "إغلاق الشاشة الحالية", "إنهاء النافذة الجارية تلقائيًا"),
                                    Triple("NAV_SAVE_SCREEN", "تخزين متغير بالواجهة", "مشاركة النتائج مع شاشات التطبيق"),
                                    Triple("NAV_READ_SCREEN", "قراءة مدخلات الواجهة", "استلام القيم المدخلة من المستخدم"),
                                    Triple("NAV_SHOW_NOTIF", "إظهار إشعار Android", "تنبيه عبر شريط الإشعارات الرئيسي"),
                                    Triple("NAV_VIBRATE", "اهتزاز الهاتف", "إعطاء استجابة حركية بنمط معين"),
                                    Triple("NAV_PLAY_SOUND", "تشغيل صوت", "تنبيه صوتي مخصص للمستخدم"),
                                    Triple("NAV_COPY_CLIPBOARD", "نسخ للحافظة", "حفظ النصوص بحافظة الهاتف"),
                                    Triple("NAV_TAKE_PHOTO", "التقاط صورة كاميرا", "تفعيل الكاميرا وتخزين اللقطة"),
                                    Triple("NAV_GET_GPS", "تحديد إحداثيات GPS", "استخراج خطوط الطول والعرض حاليًا"),
                                    Triple("NAV_READ_NFC_QR", "مسح NFC/QR", "استخراج محتوى الشفرات بالهاتف"),
                                    Triple("NAV_OVERLAY", "رسم تراكبي (Overlay)", "عرض نافذة عائمة فوق التطبيقات"),
                                    Triple("BROWSER_NAVIGATE", "توجيه المتصفح لـ URL", "تغيير رابط الصفحة الحالية للمتصفح"),
                                    Triple("BROWSER_REFRESH", "تحديث الصفحة الحالية", "إعادة تحميل وتنشيط الويب للتبويب الجاري"),
                                    Triple("BROWSER_BACK", "الرجوع للخلف التاريخي", "العودة خطوة للوراء في متصفح الويب"),
                                    Triple("BROWSER_SCROLL", "تمرير الشاشة Scroll", "تمرير محتوى الصفحة لأسفل أو لأعلى"),
                                    Triple("BROWSER_SCREENSHOT", "لقطة الشاشة للمتصفح", "تصوير شاشة الويب النشطة وحفظها كـ PNG"),
                                    Triple("BROWSER_CLOSE", "إغلاق تبويب المتصفح", "إقفال تبويبة المتصفح وتفريغ مواردها")
                                )),
                                Pair("RACCORD", listOf(
                                    Triple("RACCORD_RUN", "تشغيل موصل Raccord", "استدعاء سيناريو متصفح متكامل"),
                                    Triple("RACCORD_CONDITIONAL", "Raccord شرطي", "تشغيل بمسار نجاح/فشل انتقائي")
                                )),
                                Pair("SELECTOR", listOf(
                                    Triple("SELECTOR_EXTRACT", "استخراج بمحدد", "استخلاص العنصر بموجب المكتبة"),
                                    Triple("SELECTOR_WAIT", "انتظار محدد بالمكتبة", "تعليق التدفق لحين جاهزية العنصر"),
                                    Triple("SELECTOR_CLICK", "نقر بمحدد المكتبة", "الضغط التلقائي على العنصر المسجل"),
                                    Triple("SELECTOR_TYPE", "إدخال بمحدد المكتبة", "تعبئة الخانات بالعنصر المخزن")
                                )),
                                Pair("ADVANCED", listOf(
                                    Triple("ADV_AI", "معالجة الذكاء الاصطناعي (LLM)", "استدعاء طرازات Gemini الذكية"),
                                    Triple("ADV_IMAGE", "معالجة الصور والقص", "تعديل القياسات والأحجام تلقائيًا"),
                                    Triple("ADV_CSV_JSON", "تحويل CSV ↔ JSON", "إعادة هيكلة الجداول والمصفوفات"),
                                    Triple("ADV_ENCRYPT", "تشفير AES/RSA", "تأمين وحماية البيانات الحساسة"),
                                    Triple("ADV_EMAIL", "إرسال بريد إلكتروني", "توصيل المخرجات بالبريد SMTP"),
                                    Triple("ADV_SOAP_GRAPHQL", "طلبات SOAP / GraphQL", "الاتصال بقواعد ويب وهياكل متطورة"),
                                    Triple("ADV_ADB", "تنفيذ أمر ADB مخصص", "التفاعل المطور مع الهاتف للتجارب")
                                )),
                                Pair("DOC", listOf(
                                    Triple("DOC_STICKY", "بطاقة ملاحظة ملونة (Sticky)", "ملاحظة توثيقية عائمة على القماش"),
                                    Triple("DOC_RICH_NOTE", "عقدة محرر نصوص غني", "كتابة مستندات تفصيلية مرافقة للعمل"),
                                    Triple("DOC_COMMENT", "فقاعة تعليق توضيحية", "تعليق مصغر ملاصق لعقد محددة")
                                ))
                            )

                            // Construct dynamic catalog with library items integrated
                            val dynamicCatalog = fullCatalog.map { (catId, nodeList) ->
                                val extraNodes = mutableListOf<Triple<String, String, String>>()
                                if (catId == "SELECTOR") {
                                    libraryItems.filter { it.type == "SELECTOR" }.forEach { item ->
                                        extraNodes.add(Triple("SELECTOR_CLICK_LIB_${item.id}", "نقر بمحدد: ${item.name}", "اضغط تلقائيًا على محدد المكتبة: ${item.content}"))
                                        extraNodes.add(Triple("SELECTOR_TYPE_LIB_${item.id}", "إدخال بمحدد: ${item.name}", "أدخل قيمة في محدد المكتبة: ${item.content}"))
                                        extraNodes.add(Triple("SELECTOR_EXTRACT_LIB_${item.id}", "استخراج بمحدد: ${item.name}", "اسحب البيانات بمحدد المكتبة: ${item.content}"))
                                        extraNodes.add(Triple("SELECTOR_WAIT_LIB_${item.id}", "انتظار محدد: ${item.name}", "انتظر ظهور محدد المكتبة: ${item.content}"))
                                    }
                                } else if (catId == "CODE") {
                                    libraryItems.filter { it.type == "JS" || it.type == "PYTHON" }.forEach { item ->
                                        val nodeType = if (item.type == "JS") "CODE_JS_LIB_${item.id}" else "CODE_PYTHON_LIB_${item.id}"
                                        val langName = if (item.type == "JS") "JS" else "Python"
                                        extraNodes.add(Triple(nodeType, "سكريبت $langName: ${item.name}", "تشغيل كود محفوظ بالمكتبة: ${item.content.take(30)}..."))
                                    }
                                } else if (catId == "RACCORD") {
                                    libraryItems.filter { it.type == "RACCORD" }.forEach { item ->
                                        extraNodes.add(Triple("RACCORD_RUN_LIB_${item.id}", "تشغيل موصل: ${item.name}", "تشغيل سيناريو المتصفح المسجل بالمكتبة"))
                                    }
                                }
                                Pair(catId, nodeList + extraNodes)
                            }

                            // Filter nodes based on search and category
                            val filteredItems = dynamicCatalog
                                .filter { selectedCategoryId == "ALL" || it.first == selectedCategoryId }
                                .flatMap { (catId, nodeList) ->
                                    nodeList.map { Pair(catId, it) }
                                }
                                .filter { (_, nodeData) ->
                                    val (type, name, desc) = nodeData
                                    searchQuery.isBlank() || 
                                    name.contains(searchQuery, ignoreCase = true) || 
                                    desc.contains(searchQuery, ignoreCase = true) ||
                                    type.contains(searchQuery, ignoreCase = true)
                                }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Nodes list
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (filteredItems.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("لم يتم العثور على أي عقدة مطابقة", color = Color.Gray, fontSize = 13.sp)
                                        }
                                    }
                                } else {
                                    items(filteredItems) { (catId, nodeData) ->
                                        val (type, name, desc) = nodeData
                                        val catIcon = catalogCategories.find { it.first == catId }?.third ?: Icons.Default.Settings

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (type.contains("_LIB_")) {
                                                        val libId = type.substringAfterLast("_").toIntOrNull()
                                                        val libItem = libraryItems.find { it.id == libId }
                                                        if (libItem != null) {
                                                            val baseType = when {
                                                                type.startsWith("SELECTOR_CLICK") -> "SELECTOR_CLICK"
                                                                type.startsWith("SELECTOR_TYPE") -> "SELECTOR_TYPE"
                                                                type.startsWith("SELECTOR_EXTRACT") -> "SELECTOR_EXTRACT"
                                                                type.startsWith("SELECTOR_WAIT") -> "SELECTOR_WAIT"
                                                                type.startsWith("CODE_JS") -> "CODE_JS"
                                                                type.startsWith("CODE_PYTHON") -> "CODE_PYTHON"
                                                                type.startsWith("RACCORD_RUN") -> "RACCORD_RUN"
                                                                else -> "CODE"
                                                            }
                                                            val customProps = when (baseType) {
                                                                "SELECTOR_CLICK", "SELECTOR_WAIT", "SELECTOR_EXTRACT" -> mapOf("selector_id" to libItem.content, "library_item_id" to libItem.id.toString())
                                                                "SELECTOR_TYPE" -> mapOf("selector_id" to libItem.content, "text" to "", "library_item_id" to libItem.id.toString())
                                                                "CODE_JS", "CODE_PYTHON" -> mapOf("script" to libItem.content, "library_item_id" to libItem.id.toString())
                                                                "RACCORD_RUN" -> mapOf("raccord_id" to libItem.name, "steps" to libItem.content, "library_item_id" to libItem.id.toString())
                                                                else -> emptyMap()
                                                            }
                                                            viewModel.addNode(baseType, name, customProps)
                                                        } else {
                                                            viewModel.addNode(type, name)
                                                        }
                                                    } else {
                                                        viewModel.addNode(type, name)
                                                    }
                                                    isAddNodeSheetOpen = false
                                                },
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                            border = BorderStroke(1.dp, Color(0xFF334155))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                // Category indicator icon
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF00E5FF).copy(alpha = 0.1f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(catIcon, "", tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                    Text(desc, color = Color.Gray, fontSize = 11.sp, maxLines = 2)
                                                }

                                                Icon(Icons.Default.AddCircle, "", tint = Color(0xFF00E676), modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Edit Node properties slide drawer panel
            if (isEditNodeOpen) {
                val selectedNode = nodes.find { it.id == selectedNodeId }
                if (selectedNode != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { isEditNodeOpen = false }
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center)
                                .padding(24.dp)
                                .clickable(enabled = false) {},
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "إعداد خصائص العقدة",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF00E5FF)
                                    )
                                    IconButton(onClick = { isEditNodeOpen = false }) {
                                        Icon(Icons.Default.Close, "", tint = Color.White)
                                    }
                                }

                                Divider(color = Color.Gray, modifier = Modifier.padding(vertical = 10.dp))

                                Text("اسم العقدة", color = Color.White, fontSize = 12.sp)
                                var tempName by remember { mutableStateOf(selectedNode.name) }
                                OutlinedTextField(
                                    value = tempName,
                                    onValueChange = { tempName = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                                )

                                // Render property inputs depending on type
                                val tempProperties = remember { mutableStateMapOf<String, String>().apply { putAll(selectedNode.properties) } }

                                selectedNode.properties.forEach { (key, value) ->
                                    Text(key, color = Color.White, fontSize = 12.sp)
                                    OutlinedTextField(
                                        value = tempProperties[key] ?: value,
                                        onValueChange = { tempProperties[key] = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.updateNodeProperties(selectedNode.id, tempProperties.toMap())
                                            // update name as well
                                            val currentList = viewModel.nodes.value.map { node ->
                                                if (node.id == selectedNode.id) node.copy(name = tempName) else node
                                            }
                                            viewModel.nodes.value = currentList
                                            viewModel.saveCurrentWorkflow()
                                            isEditNodeOpen = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("حفظ التغييرات", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { isEditNodeOpen = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("إلغاء")
                                    }
                                }
                            }
                        }
                    }
                }

                // Grid Settings Dialog (🎛️ إعدادات محاذاة الشبكة)
                if (isGridSettingsOpen) {
                    AlertDialog(
                        onDismissRequest = { isGridSettingsOpen = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.GridOn, "", tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("محاذاة العناصر للشبكة", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            }
                        },
                        text = {
                            Column {
                                Text("تساعدك المحاذاة التلقائية على تنظيم وتنسيق عقد سير العمل بشكل مرتب ومتناسق.", color = Color.Gray, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("تفعيل مغناطيس المحاذاة", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Switch(
                                        checked = snapToGrid,
                                        onCheckedChange = { viewModel.snapToGrid.value = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF00E5FF),
                                            checkedTrackColor = Color(0xFF00E5FF).copy(alpha = 0.5f)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text("حجم خلية الشبكة: ${gridSize.toInt()} بكسل", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Slider(
                                    value = gridSize,
                                    onValueChange = { viewModel.gridSize.value = it },
                                    valueRange = 20f..80f,
                                    steps = 5,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF00E5FF),
                                        activeTrackColor = Color(0xFF00E5FF)
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text("مقاسات سريعة:", color = Color.Gray, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(20f, 40f, 60f, 80f).forEach { size ->
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { viewModel.gridSize.value = size },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (gridSize == size) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF0F172A)
                                            ),
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = if (gridSize == size) Color(0xFF00E5FF) else Color(0xFF334155)
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${size.toInt()}px",
                                                    color = if (gridSize == size) Color(0xFF00E5FF) else Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { isGridSettingsOpen = false }) {
                                Text("تم", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                            }
                        },
                        containerColor = Color(0xFF1E293B)
                    )
                }

                // Direct Script & Properties Editor Dialog (📝 تعديل السكريبت والخصائص مباشرة)
                if (isEditingNodeScriptOpen && editingNodeForScript != null) {
                    val node = editingNodeForScript!!
                    
                    // Main property keys
                    val mainPropKey = when {
                        node.type.startsWith("CODE") || node.type.contains("JS") || node.type.contains("PYTHON") -> "script"
                        node.type.startsWith("SELECTOR") -> "selector_id"
                        node.type.contains("AI") -> "prompt"
                        node.type.contains("URL") || node.type == "HTTP" || node.type == "DATA_HTTP" -> "url"
                        else -> node.properties.keys.firstOrNull() ?: "value"
                    }
                    val mainPropLabel = when (mainPropKey) {
                        "script" -> "الكود البرمجي (Script)"
                        "selector_id" -> "مسار المحدد (DOM Selector)"
                        "prompt" -> "المطالبة الذكية (AI Prompt)"
                        "url" -> "عنوان الرابط (URL)"
                        else -> "القيمة المحددة ($mainPropKey)"
                    }
                    
                    var tempValue by remember(node.id) { mutableStateOf(node.properties[mainPropKey] ?: "") }
                    var tempName by remember(node.id) { mutableStateOf(node.name) }
                    
                    val extraProps = remember(node.id) { 
                        val map = mutableStateMapOf<String, String>()
                        node.properties.filterKeys { it != mainPropKey }.forEach { (k, v) ->
                            map[k] = v
                        }
                        map
                    }

                    AlertDialog(
                        onDismissRequest = { isEditingNodeScriptOpen = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EditNote, "", tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تعديل عقدة: ${node.name}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("تعديل مباشر لاسم العقدة والخصائص الرئيسية:", color = Color.Gray, fontSize = 12.sp)
                                
                                OutlinedTextField(
                                    value = tempName,
                                    onValueChange = { tempName = it },
                                    label = { Text("اسم العقدة", color = Color.Gray) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                                )
                                
                                Text(mainPropLabel, color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                
                                OutlinedTextField(
                                    value = tempValue,
                                    onValueChange = { tempValue = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(if (mainPropKey == "script" || mainPropKey == "prompt") 200.dp else 120.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontFamily = if (mainPropKey == "script") FontFamily.Monospace else FontFamily.Default,
                                        fontSize = 12.sp
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A),
                                        focusedBorderColor = Color(0xFF00E5FF)
                                    ),
                                    placeholder = { Text("اكتب المحتوى هنا...", color = Color.DarkGray) }
                                )

                                if (mainPropKey == "selector_id") {
                                    val savedSelectors = libraryItems.filter { it.type == "SELECTOR" }
                                    if (savedSelectors.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("🎯 اختر من المحددات المحفوظة في المكتبة:", color = Color.LightGray, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        androidx.compose.foundation.lazy.LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(savedSelectors) { item ->
                                                SuggestionChip(
                                                    onClick = { tempValue = item.content },
                                                    label = { Text(item.name, fontSize = 10.sp) },
                                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                                        labelColor = Color(0xFF00E5FF),
                                                        containerColor = Color(0xFF0F172A)
                                                    ),
                                                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // Extra properties if any
                                if (extraProps.isNotEmpty()) {
                                    Text("خصائص إضافية:", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    extraProps.keys.forEach { key ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(key, color = Color.Gray, fontSize = 11.sp, modifier = Modifier.weight(0.3f))
                                            OutlinedTextField(
                                                value = extraProps[key] ?: "",
                                                onValueChange = { extraProps[key] = it },
                                                modifier = Modifier.weight(0.7f).height(48.dp),
                                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val finalProps = node.properties.toMutableMap()
                                    finalProps[mainPropKey] = tempValue
                                    extraProps.forEach { (k, v) -> finalProps[k] = v }
                                    
                                    // Update name and properties in workflow
                                    val updatedList = viewModel.nodes.value.map { n ->
                                        if (n.id == node.id) n.copy(name = tempName, properties = finalProps) else n
                                    }
                                    viewModel.nodes.value = updatedList
                                    viewModel.saveCurrentWorkflow()
                                    
                                    isEditingNodeScriptOpen = false
                                    Toast.makeText(context, "تم حفظ التعديلات مباشرة!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                            ) {
                                Text("حفظ", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { isEditingNodeScriptOpen = false }) {
                                Text("إلغاء", color = Color.White)
                            }
                        },
                        containerColor = Color(0xFF1E293B)
                    )
                }
            }
        }
    }
}
