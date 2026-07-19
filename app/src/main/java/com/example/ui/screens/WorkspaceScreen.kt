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
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.data.WorkspaceFile
import com.example.ui.WorkflowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    viewModel: WorkflowViewModel,
    onNavigateToEditor: () -> Unit
) {
    val files by viewModel.files.collectAsStateWithLifecycle()
    val selectedFilePath by viewModel.selectedFilePath.collectAsStateWithLifecycle()
    val editingFileContent by viewModel.editingFileContent.collectAsStateWithLifecycle()
    val searchQueryFiles by viewModel.searchQueryFiles.collectAsStateWithLifecycle()
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()
    val openFileTabs by viewModel.openFileTabs.collectAsStateWithLifecycle()

    val context = LocalContext.current

    var isCreateFileOpen by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var newFileType by remember { mutableStateOf("JS") } // "JS", "JSON", "CSV", "MD", "TXT", "DIR"

    var isEditCellOpen by remember { mutableStateOf(false) }
    var selectedCellRow by remember { mutableStateOf(0) }
    var selectedCellCol by remember { mutableStateOf(0) }
    var selectedCellValue by remember { mutableStateOf("") }

    val filteredFiles = remember(files, searchQueryFiles) {
        if (searchQueryFiles.isBlank()) files else files.filter { it.path.contains(searchQueryFiles, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مستكشف ومساحة ملفات العمل",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                },
                actions = {
                    IconButton(onClick = { isCreateFileOpen = true }) {
                        Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "إنشاء ملف")
                    }
                    IconButton(onClick = {
                        Toast.makeText(context, "جاري مزامنة مساحة العمل سحابياً...", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Default.CloudSync, contentDescription = "مزامنة سحابية", tint = Color(0xFF00E676))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Left Panel: File Explorer Tree (35% width)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1.3f)
                    .background(Color(0xFF0B0F19))
                    .padding(8.dp)
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQueryFiles,
                    onValueChange = { viewModel.searchQueryFiles.value = it },
                    placeholder = { Text("بحث في المجلدات...", fontSize = 11.sp, color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray
                    ),
                    maxLines = 1,
                    textStyle = TextStyle(fontSize = 11.sp)
                )

                Text("شجرة مساحة العمل (/):", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredFiles) { file ->
                        val isSelected = selectedFilePath == file.path
                        val icon = if (file.isDirectory) Icons.Default.Folder else {
                            when {
                                file.path.endsWith(".json") -> Icons.Default.DataObject
                                file.path.endsWith(".csv") -> Icons.Default.GridOn
                                file.path.endsWith(".js") -> Icons.Default.Code
                                file.path.endsWith(".md") -> Icons.Default.Description
                                else -> Icons.Default.InsertDriveFile
                            }
                        }

                        val iconColor = if (file.isDirectory) Color(0xFFFBC02D) else {
                            when {
                                file.path.endsWith(".json") -> Color(0xFF00E5FF)
                                file.path.endsWith(".csv") -> Color(0xFF00E676)
                                file.path.endsWith(".js") -> Color(0xFFFFEA00)
                                file.path.endsWith(".md") -> Color(0xFFE65100)
                                else -> Color.LightGray
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFF1E293B) else Color.Transparent)
                                .clickable {
                                    if (!file.isDirectory) {
                                        viewModel.openFileInEditor(file.path)
                                    }
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(icon, "", tint = iconColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = file.path.substringAfterLast("/"),
                                    color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            // Operations drop-down/actions inside left tree
                            Row {
                                if (!file.isDirectory) {
                                    IconButton(
                                        onClick = {
                                            // Action: spawn file node in Editor
                                            viewModel.addNode("FILE_READ", "قراءة ${file.path.substringAfterLast("/")}")
                                            Toast.makeText(context, "تم ربط الملف كمشغل قراءة في المحرر!", Toast.LENGTH_SHORT).show()
                                            onNavigateToEditor()
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Link, "", tint = Color(0xFF00E676), modifier = Modifier.size(14.dp))
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.deleteWorkspaceFile(file) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Delete, "", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                // Cloud Sync Indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المزامنة السحابية:", fontSize = 10.sp, color = Color.Gray)
                    Text(cloudSyncStatus, color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFF1E293B)))

            // Right Panel: Text / Spreadsheet / Markdown Editor (65% width)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(2.7f)
                    .padding(8.dp)
            ) {
                if (selectedFilePath == null) {
                    // Empty state editor
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0B0F19), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.EditNote, "", tint = Color.DarkGray, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "لم يتم تحديد ملف نشط للتعديل.\nاختر ملفاً من شجرة الملفات الجانبية للبدء.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // File editor loaded view
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Open tabs headers
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            openFileTabs.forEach { tab ->
                                val isSelected = tab == selectedFilePath
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(if (isSelected) Color(0xFF1E293B) else Color(0xFF0B0F19))
                                        .clickable { viewModel.openFileInEditor(tab) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color(0xFF00E5FF) else Color.Transparent,
                                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                        )
                                ) {
                                    Text(
                                        text = tab.substringAfterLast("/"),
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color.Gray,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // Save action button
                        Button(
                            onClick = { viewModel.saveEditingFile() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Save, "", tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حفظ الملف", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Depending on file type extension, render proper layout
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0B0F19))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        when {
                            selectedFilePath!!.endsWith(".csv") -> {
                                // CSV Spreadsheet Editor Grid!
                                val csvData = editingFileContent
                                val rows = csvData.split("\n").filter { it.isNotBlank() }
                                
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    item {
                                        Text("محرر جداول البيانات CSV الخلايا:", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                                    }
                                    itemsIndexed(rows) { rowIndex, rowText ->
                                        val columns = rowText.split(",")
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            columns.forEachIndexed { colIndex, cellVal ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .background(Color(0xFF1E293B), RoundedCornerShape(4.dp))
                                                        .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
                                                        .clickable {
                                                            selectedCellRow = rowIndex
                                                            selectedCellCol = colIndex
                                                            selectedCellValue = cellVal
                                                            isEditCellOpen = true
                                                        }
                                                        .padding(6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        cellVal,
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            selectedFilePath!!.endsWith(".md") -> {
                                // Markdown View split preview / raw edit
                                var showPreview by remember { mutableStateOf(false) }

                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { showPreview = !showPreview }) {
                                            Text(if (showPreview) "تعديل الكود" else "معاينة المنسق", color = Color(0xFF00E5FF), fontSize = 11.sp)
                                        }
                                    }

                                    if (showPreview) {
                                        // Simple formatted renderer
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            val lines = editingFileContent.split("\n")
                                            items(lines) { line ->
                                                when {
                                                    line.startsWith("# ") -> {
                                                        Text(line.removePrefix("# "), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                                                    }
                                                    line.startsWith("## ") -> {
                                                        Text(line.removePrefix("## "), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                                                    }
                                                    line.startsWith("- ") || line.startsWith("* ") -> {
                                                        Row {
                                                            Text("• ", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                                                            Text(line.removePrefix("- ").removePrefix("* "), color = Color.White, fontSize = 12.sp)
                                                        }
                                                    }
                                                    else -> {
                                                        Text(line, color = Color.LightGray, fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        BasicTextFileEditor(
                                            value = editingFileContent,
                                            onValueChange = { viewModel.editingFileContent.value = it }
                                        )
                                    }
                                }
                            }
                            else -> {
                                // Standard Javascript / Python / JSON Text Editor
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = "تنسيق ملوّن تلقائي للبرمجة:",
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    BasicTextFileEditor(
                                        value = editingFileContent,
                                        onValueChange = { viewModel.editingFileContent.value = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cell editing dialog for CSV spreadsheet
        if (isEditCellOpen) {
            AlertDialog(
                onDismissRequest = { isEditCellOpen = false },
                title = { Text("تعديل قيمة الخلية", color = Color(0xFF00E5FF)) },
                containerColor = Color(0xFF1E293B),
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("الخلية: الصف ${selectedCellRow + 1}، العمود ${selectedCellCol + 1}", color = Color.Gray, fontSize = 11.sp)
                        OutlinedTextField(
                            value = selectedCellValue,
                            onValueChange = { selectedCellValue = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // Rebuild CSV content string
                            val csvData = editingFileContent
                            val rows = csvData.split("\n").filter { it.isNotBlank() }.toMutableList()
                            if (selectedCellRow < rows.size) {
                                val cols = rows[selectedCellRow].split(",").toMutableList()
                                if (selectedCellCol < cols.size) {
                                    cols[selectedCellCol] = selectedCellValue
                                    rows[selectedCellRow] = cols.joinToString(",")
                                    viewModel.editingFileContent.value = rows.joinToString("\n")
                                }
                            }
                            isEditCellOpen = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                    ) {
                        Text("تحديث الخلية", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isEditCellOpen = false }) {
                        Text("إلغاء", color = Color.White)
                    }
                }
            )
        }

        // Create file dialog
        if (isCreateFileOpen) {
            AlertDialog(
                onDismissRequest = { isCreateFileOpen = false },
                title = { Text("إنشاء عنصر جديد في مساحة العمل", color = Color(0xFF00E5FF)) },
                containerColor = Color(0xFF1E293B),
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("اختر نوع وممتد الملف المراد إدراجه:", fontSize = 12.sp, color = Color.LightGray)

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("JS" to "جافا سكريبت", "JSON" to "جيسون JSON", "CSV" to "جدول CSV", "MD" to "مارك داون").forEach { (type, label) ->
                                FilterChip(
                                    selected = newFileType == type,
                                    onClick = { newFileType = type },
                                    label = { Text(label, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF00E5FF),
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

                        OutlinedTextField(
                            value = newFileName,
                            onValueChange = { newFileName = it },
                            placeholder = { Text("أدخل اسم الملف (مثال: reports_builder)", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newFileName.isNotBlank()) {
                                val extension = when (newFileType) {
                                    "JS" -> ".js"
                                    "JSON" -> ".json"
                                    "CSV" -> ".csv"
                                    "MD" -> ".md"
                                    else -> ".txt"
                                }
                                val fullPath = "/$newFileName$extension"
                                val initialContent = when (newFileType) {
                                    "JS" -> "// سكريبت جافاسكريبت جديد\nfunction run() {\n    return \"Hello\";\n}"
                                    "JSON" -> "{\n  \"status\": \"active\"\n}"
                                    "CSV" -> "العنوان,التاريخ,الحالة\nتقرير رقم 1,2026-07-18,مكتمل"
                                    "MD" -> "# تقرير أتمتة جديد\n\nأكتب المحتوى هنا."
                                    else -> ""
                                }
                                viewModel.createWorkspaceFile(fullPath, initialContent, false)
                                newFileName = ""
                                isCreateFileOpen = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        enabled = newFileName.isNotBlank()
                    ) {
                        Text("إدراج وإنشاء الملف", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isCreateFileOpen = false }) {
                        Text("إلغاء", color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
fun BasicTextFileEditor(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxSize(),
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Color(0xFF00E676)
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent
        )
    )
}
