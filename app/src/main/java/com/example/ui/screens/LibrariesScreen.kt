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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LibraryItem
import com.example.data.BrowserStep
import org.json.JSONArray
import org.json.JSONObject
import com.example.ui.WorkflowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrariesScreen(
    viewModel: WorkflowViewModel,
    onNavigateToEditor: () -> Unit
) {
    val items by viewModel.libraryItems.collectAsStateWithLifecycle()
    val selectedLibraryType by viewModel.selectedLibraryType.collectAsStateWithLifecycle()
    val isCommunityHubActive by viewModel.isCommunityHubActive.collectAsStateWithLifecycle()

    val context = LocalContext.current

    var isTestRunOpen by remember { mutableStateOf(false) }
    var testingItemName by remember { mutableStateOf("") }
    var mockDataInput by remember { mutableStateOf("{\n  \"username\": \"test_user\",\n  \"count\": 42\n}") }
    var mockResultText by remember { mutableStateOf("") }

    var isEditItemOpen by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<LibraryItem?>(null) }
    var editingItemContent by remember { mutableStateOf("") }

    var isAddItemOpen by remember { mutableStateOf(false) }
    var addName by remember { mutableStateOf("") }
    var addType by remember { mutableStateOf("JS") }
    var addContent by remember { mutableStateOf("") }
    var addTags by remember { mutableStateOf("") }

    var raccordStepsState by remember { mutableStateOf(listOf<BrowserStep>()) }

    LaunchedEffect(isEditItemOpen, editingItem, isAddItemOpen) {
        if (isEditItemOpen && editingItem?.type == "RACCORD") {
            try {
                val list = mutableListOf<BrowserStep>()
                val arr = JSONArray(editingItemContent)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        BrowserStep(
                            step = obj.optInt("step", i + 1),
                            action = obj.optString("action", "navigate"),
                            target = obj.optString("target", ""),
                            value = obj.optString("value", "")
                        )
                    )
                }
                raccordStepsState = list
            } catch (e: Exception) {
                raccordStepsState = emptyList()
            }
        } else if (isAddItemOpen) {
            raccordStepsState = emptyList()
        }
    }

    val filteredItems = remember(items, selectedLibraryType) {
        if (selectedLibraryType == "ALL") items else items.filter { it.type == selectedLibraryType }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مكتبات الأكواد والمحددات",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                },
                actions = {
                    IconButton(onClick = { isAddItemOpen = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة عنصر")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Main tabs switcher: My Library vs Community Hub
            TabRow(
                selectedTabIndex = if (isCommunityHubActive) 1 else 0,
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFF00E5FF),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .padding(bottom = 12.dp)
            ) {
                Tab(
                    selected = !isCommunityHubActive,
                    onClick = { viewModel.isCommunityHubActive.value = false },
                    text = { Text("مكتبتي المحلّية", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = isCommunityHubActive,
                    onClick = { viewModel.isCommunityHubActive.value = true },
                    text = { Text("🏪 المتجر ومكتبة المجتمع", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }

            // Categories horizontal filter chips
            val categories = listOf("ALL" to "الكل", "JS" to "JavaScript", "PYTHON" to "Python", "RACCORD" to "موصلات", "SELECTOR" to "محددات DOM")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedLibraryType == key,
                        onClick = { viewModel.selectedLibraryType.value = key },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00E5FF),
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.LightGray
                        )
                    )
                }
            }

            if (!isCommunityHubActive) {
                // Local Library Items List
                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LibraryBooks, "", tint = Color.DarkGray, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "المكتبة فارغة للفرع المحدد حالياً.\nانقر على زر + أو التقط محددات من المتصفح.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredItems) { item ->
                            LibraryItemCard(
                                item = item,
                                onTestClick = {
                                    testingItemName = item.name
                                    mockDataInput = "{\n  \"payload\": \"بيانات تجريبية للتشغيل\",\n  \"id\": 1\n}"
                                    // Simulated execution runner for library elements
                                    mockResultText = when (item.type) {
                                        "JS" -> "✅ نجح تصفية كود الجافا سكريبت:\n[\n  \"https://example.com/welcome\",\n  \"https://example.com/docs\"\n]"
                                        "PYTHON" -> "🐍 مخرجات البايثون:\n{\n  \"cleaned\": true,\n  \"items_count\": 2\n}"
                                        "SELECTOR" -> "🎯 تم تحديد الرابط بنجاح:\nXPath: //span[@class='titleline']/a"
                                        "RACCORD" -> "🌐 محاكاة الخطوات الخمسة للموصل:\n1. الانتقال للموقع ✅\n2. كتابة الحساب ✅\n3. كتابة كلمة المرور ✅\n4. الضغط على تسجيل الدخول ✅"
                                        else -> "تم التنفيذ بنجاح."
                                    }
                                    isTestRunOpen = true
                                },
                                onEditClick = {
                                    editingItem = item
                                    editingItemContent = item.content
                                    isEditItemOpen = true
                                },
                                onDeleteClick = {
                                    viewModel.deleteLibraryItem(item)
                                },
                                onLinkClick = {
                                    // Spawns associated code in Editor
                                    viewModel.addNode(if (item.type == "RACCORD") "BROWSER" else "CODE", "مكتبة: ${item.name.take(15)}")
                                    Toast.makeText(context, "تم إدراج العقدة في المحرر!", Toast.LENGTH_SHORT).show()
                                    onNavigateToEditor()
                                }
                            )
                        }
                    }
                }
            } else {
                // Community Store view (Mocked Store items)
                val storeItems = listOf(
                    LibraryItem(1, "JS", "مستخرج بيانات تويتر الذكي (Twitter Scraper)", "function scrapeTweets(user) { ... }", "مواقع,تويتر", 2),
                    LibraryItem(2, "PYTHON", "تحليل المشاعر التلقائي بالنصوص", "def sentiment_analyze(text): ...", "ذكاء,تحليل", 1),
                    LibraryItem(3, "RACCORD", "تخطي الكابتشا والتحقق الثنائي تلقائياً", "[ {navigate}, {bypass} ]", "موصل,متقدم", 3),
                    LibraryItem(4, "SELECTOR", "زر الشراء المباشر في متجر أمازون", "button#add-to-cart-button", "محدد,أمازون", 1)
                ).filter { selectedLibraryType == "ALL" || it.type == selectedLibraryType }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(storeItems) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(item.type, color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, "", tint = Color(0xFFFFD600), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("4.9 (48 تقييم)", color = Color.Gray, fontSize = 10.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("الوسوم: ${item.tags}", color = Color.Gray, fontSize = 11.sp)

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("المطور: محمد العلي", color = Color.LightGray, fontSize = 11.sp)
                                    Button(
                                        onClick = {
                                            viewModel.addLibraryItem(item.name, item.type, item.content, item.tags)
                                            Toast.makeText(context, "تم تحميل العنصر وإضافته لمكتبتك بنجاح!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.CloudDownload, "", tint = Color.Black, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تنزيل مجاني", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Test run dialog
        if (isTestRunOpen) {
            AlertDialog(
                onDismissRequest = { isTestRunOpen = false },
                title = { Text("بيئة تشغيل اختبار العقدة", color = Color(0xFF00E5FF)) },
                containerColor = Color(0xFF1E293B),
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("الاسم: $testingItemName", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text("مصفوفة الإدخال الوهمية لبيئة الاختبار:", color = Color.Gray, fontSize = 11.sp)

                        OutlinedTextField(
                            value = mockDataInput,
                            onValueChange = { mockDataInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp),
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                        )

                        Text("مخرجات التنفيذ (Console Output):", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            LazyColumn {
                                item {
                                    Text(
                                        text = mockResultText,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color(0xFF00E676)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { isTestRunOpen = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))) {
                        Text("إغلاق الاختبار", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Edit Item code dialog
        if (isEditItemOpen && editingItem != null) {
            AlertDialog(
                onDismissRequest = { isEditItemOpen = false },
                title = { Text("تعديل كود ومسار العنصر", color = Color(0xFF00E5FF)) },
                containerColor = Color(0xFF1E293B),
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("اسم العنصر: ${editingItem!!.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        
                        if (editingItem!!.type == "RACCORD") {
                            Text("خطوات التصفح المسجلة (Raccord Steps)", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("تعديل الخطوات بصرياً:", color = Color.Gray, fontSize = 11.sp)
                                TextButton(
                                    onClick = {
                                        val nextStep = raccordStepsState.size + 1
                                        raccordStepsState = raccordStepsState + BrowserStep(nextStep, "navigate", value = "https://")
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00E676))
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إضافة خطوة", fontSize = 11.sp)
                                }
                            }

                            if (raccordStepsState.isEmpty()) {
                                Text("لا يوجد خطوات حالياً. اضغط 'إضافة خطوة' للبدء.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                            } else {
                                Box(modifier = Modifier.heightIn(max = 240.dp)) {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(raccordStepsState) { s ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                                border = BorderStroke(1.dp, Color(0xFF334155))
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("خطوة #${s.step}", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        IconButton(
                                                            onClick = {
                                                                raccordStepsState = raccordStepsState.filter { it.step != s.step }.mapIndexed { idx, step ->
                                                                    step.copy(step = idx + 1)
                                                                }
                                                            },
                                                            modifier = Modifier.size(20.dp)
                                                        ) {
                                                            Icon(Icons.Default.Delete, "", tint = Color(0xFFEF5350), modifier = Modifier.size(14.dp))
                                                        }
                                                    }

                                                    // Action selection
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        listOf("navigate" to "انتقال", "click" to "نقر", "input" to "إدخال", "wait" to "انتظار").forEach { (act, label) ->
                                                            FilterChip(
                                                                selected = s.action == act,
                                                                onClick = {
                                                                    raccordStepsState = raccordStepsState.map { 
                                                                        if (it.step == s.step) it.copy(action = act) else it
                                                                    }
                                                                },
                                                                label = { Text(label, fontSize = 10.sp) },
                                                                colors = FilterChipDefaults.filterChipColors(
                                                                    selectedContainerColor = Color(0xFF00E5FF),
                                                                    selectedLabelColor = Color.Black
                                                                )
                                                            )
                                                        }
                                                    }

                                                    if (s.action != "navigate") {
                                                        OutlinedTextField(
                                                            value = s.target,
                                                            onValueChange = { newVal ->
                                                                raccordStepsState = raccordStepsState.map {
                                                                    if (it.step == s.step) it.copy(target = newVal) else it
                                                                }
                                                            },
                                                            label = { Text("المحدد (CSS / XPath Selector)", color = Color.Gray, fontSize = 9.sp) },
                                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                                            textStyle = TextStyle(fontSize = 11.sp),
                                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                                                        )
                                                    }

                                                    val valLabel = if (s.action == "navigate") "الرابط (URL)" else "القيمة المدخلة / مدة الانتظار"
                                                    OutlinedTextField(
                                                        value = s.value,
                                                        onValueChange = { newVal ->
                                                            raccordStepsState = raccordStepsState.map {
                                                                if (it.step == s.step) it.copy(value = newVal) else it
                                                            }
                                                        },
                                                        label = { Text(valLabel, color = Color.Gray, fontSize = 9.sp) },
                                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                                        textStyle = TextStyle(fontSize = 11.sp),
                                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = editingItemContent,
                                onValueChange = { editingItemContent = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color(0xFF00E676)),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val contentToSave = if (editingItem!!.type == "RACCORD") {
                                val jsonArr = JSONArray()
                                raccordStepsState.forEach { s ->
                                    val obj = JSONObject()
                                    obj.put("step", s.step)
                                    obj.put("action", s.action)
                                    obj.put("target", s.target)
                                    obj.put("value", s.value)
                                    jsonArr.put(obj)
                                }
                                jsonArr.toString()
                            } else {
                                editingItemContent
                            }
                            viewModel.addLibraryItem(editingItem!!.name, editingItem!!.type, contentToSave, editingItem!!.tags)
                            isEditItemOpen = false
                            Toast.makeText(context, "تم التحديث بنجاح!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                    ) {
                        Text("حفظ التغييرات", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isEditItemOpen = false }) {
                        Text("إلغاء", color = Color.White)
                    }
                }
            )
        }

        // Add item dialog
        if (isAddItemOpen) {
            AlertDialog(
                onDismissRequest = { isAddItemOpen = false },
                title = { Text("إضافة عنصر جديد للمكتبة", color = Color(0xFF00E5FF)) },
                containerColor = Color(0xFF1E293B),
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("JS" to "JS", "PYTHON" to "Python", "RACCORD" to "موصل", "SELECTOR" to "محدد").forEach { (type, label) ->
                                FilterChip(
                                    selected = addType == type,
                                    onClick = { addType = type },
                                    label = { Text(label, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF00E5FF),
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

                        OutlinedTextField(
                            value = addName,
                            onValueChange = { addName = it },
                            label = { Text("اسم العنصر (مثال: فلترة النصوص)", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                        )

                        if (addType == "RACCORD") {
                            Text("خطوات التصفح المسجلة (Raccord Steps)", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("إضافة خطوات تصفح بصرياً:", color = Color.Gray, fontSize = 11.sp)
                                TextButton(
                                    onClick = {
                                        val nextStep = raccordStepsState.size + 1
                                        raccordStepsState = raccordStepsState + BrowserStep(nextStep, "navigate", value = "https://")
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00E676))
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إضافة خطوة", fontSize = 11.sp)
                                }
                            }

                            if (raccordStepsState.isEmpty()) {
                                Text("لا يوجد خطوات حالياً. اضغط 'إضافة خطوة' للبدء.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                            } else {
                                Box(modifier = Modifier.heightIn(max = 200.dp)) {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(raccordStepsState) { s ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                                border = BorderStroke(1.dp, Color(0xFF334155))
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("خطوة #${s.step}", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        IconButton(
                                                            onClick = {
                                                                raccordStepsState = raccordStepsState.filter { it.step != s.step }.mapIndexed { idx, step ->
                                                                    step.copy(step = idx + 1)
                                                                }
                                                            },
                                                            modifier = Modifier.size(20.dp)
                                                        ) {
                                                            Icon(Icons.Default.Delete, "", tint = Color(0xFFEF5350), modifier = Modifier.size(14.dp))
                                                        }
                                                    }

                                                    // Action selection
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        listOf("navigate" to "انتقال", "click" to "نقر", "input" to "إدخال", "wait" to "انتظار").forEach { (act, label) ->
                                                            FilterChip(
                                                                selected = s.action == act,
                                                                onClick = {
                                                                    raccordStepsState = raccordStepsState.map { 
                                                                        if (it.step == s.step) it.copy(action = act) else it
                                                                    }
                                                                },
                                                                label = { Text(label, fontSize = 10.sp) },
                                                                colors = FilterChipDefaults.filterChipColors(
                                                                    selectedContainerColor = Color(0xFF00E5FF),
                                                                    selectedLabelColor = Color.Black
                                                                )
                                                            )
                                                        }
                                                    }

                                                    if (s.action != "navigate") {
                                                        OutlinedTextField(
                                                            value = s.target,
                                                            onValueChange = { newVal ->
                                                                raccordStepsState = raccordStepsState.map {
                                                                    if (it.step == s.step) it.copy(target = newVal) else it
                                                                }
                                                            },
                                                            label = { Text("المحدد (CSS / XPath Selector)", color = Color.Gray, fontSize = 9.sp) },
                                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                                            textStyle = TextStyle(fontSize = 11.sp),
                                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                                                        )
                                                    }

                                                    val valLabel = if (s.action == "navigate") "الرابط (URL)" else "القيمة المدخلة / مدة الانتظار"
                                                    OutlinedTextField(
                                                        value = s.value,
                                                        onValueChange = { newVal ->
                                                            raccordStepsState = raccordStepsState.map {
                                                                if (it.step == s.step) it.copy(value = newVal) else it
                                                            }
                                                        },
                                                        label = { Text(valLabel, color = Color.Gray, fontSize = 9.sp) },
                                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                                        textStyle = TextStyle(fontSize = 11.sp),
                                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = addContent,
                                onValueChange = { addContent = it },
                                label = { Text("الكود البرمجي أو محدد الـ DOM", color = Color.Gray) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                            )
                        }

                        OutlinedTextField(
                            value = addTags,
                            onValueChange = { addTags = it },
                            label = { Text("الوسوم (مفصولة بفاصلة)", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val contentToSave = if (addType == "RACCORD") {
                                val jsonArr = JSONArray()
                                raccordStepsState.forEach { s ->
                                    val obj = JSONObject()
                                    obj.put("step", s.step)
                                    obj.put("action", s.action)
                                    obj.put("target", s.target)
                                    obj.put("value", s.value)
                                    jsonArr.put(obj)
                                }
                                jsonArr.toString()
                            } else {
                                addContent
                            }

                            if (addName.isNotBlank() && (addType == "RACCORD" || contentToSave.isNotBlank())) {
                                viewModel.addLibraryItem(addName, addType, contentToSave, addTags)
                                addName = ""
                                addContent = ""
                                addTags = ""
                                raccordStepsState = emptyList()
                                isAddItemOpen = false
                                Toast.makeText(context, "تم الإدراج بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        enabled = addName.isNotBlank() && (addType == "RACCORD" || addContent.isNotBlank())
                    ) {
                        Text("حفظ العنصر", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isAddItemOpen = false }) {
                        Text("إلغاء", color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
fun LibraryItemCard(
    item: LibraryItem,
    onTestClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onLinkClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val color = when (item.type) {
                        "JS" -> Color(0xFFFBC02D)
                        "PYTHON" -> Color(0xFFFFB300)
                        "RACCORD" -> Color(0xFF00E5FF)
                        else -> Color(0xFF00E676)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(color.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(item.type, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    Text("الإصدار v${item.version}", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onLinkClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.AddLink, "إدراج بالمحرر", tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, "حذف", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(item.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(
                text = if (item.content.length > 80) item.content.take(80) + "..." else item.content,
                color = Color.LightGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(6.dp)
            )

            if (item.tags.isNotEmpty()) {
                Text("الوسوم: ${item.tags}", color = Color.Gray, fontSize = 10.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onEditClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .height(26.dp)
                        .padding(end = 6.dp)
                ) {
                    Icon(Icons.Default.Edit, "", modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعديل الكود", fontSize = 10.sp)
                }

                Button(
                    onClick = onTestClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, "", tint = Color.Black, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تشغيل اختبار", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
