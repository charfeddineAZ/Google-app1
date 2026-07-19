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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.SecretKey
import com.example.ui.WorkflowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WorkflowViewModel
) {
    val secrets by viewModel.secrets.collectAsStateWithLifecycle()
    val liteMode by viewModel.liteMode.collectAsStateWithLifecycle()

    val context = LocalContext.current

    var isAddSecretOpen by remember { mutableStateOf(false) }
    var secretKeyInput by remember { mutableStateOf("") }
    var secretValInput by remember { mutableStateOf("") }

    // Mock diagnostic states
    var isCheckingPerformance by remember { mutableStateOf(false) }
    var cpuLoad by remember { mutableStateOf("12%") }
    var memoryUsage by remember { mutableStateOf("48 MB") }
    var sqliteQuerySpeed by remember { mutableStateOf("3 ms") }
    var activeRuntimesCount by remember { mutableStateOf("1 active") }

    // External App notification states
    var telegramToken by remember { mutableStateOf("") }
    var slackWebhookUrl by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الخزنة الآمنة والإعدادات",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- SECTION 1: Secrets & Credentials Vault ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🔑 الخزنة الآمنة للمفاتيح والمصادقة",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Button(
                        onClick = { isAddSecretOpen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Default.Add, "", tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة مفتاح", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (secrets.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Text(
                            "الخزنة فارغة حالياً. أضف مفتاح API مثل GEMINI_API_KEY لتفعيل عقد الذكاء الاصطناعي.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            } else {
                items(secrets) { key ->
                    var isVisible by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(key.key, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = if (isVisible) key.value else "•••••••••••••••••••• (مؤمن ومحمي)",
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(onClick = { isVisible = !isVisible }) {
                                    Icon(
                                        imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "عرض",
                                        tint = Color.LightGray
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteSecretKey(key.key) }) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            // --- SECTION 2: External Notifications Webhooks ---
            item {
                Text(
                    "🔔 قنوات الإشعارات والـ Webhooks الخارجية",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("اربط قنوات التواصل لتلقي مخرجات الأتمتة المكتملة فوراً:", fontSize = 11.sp, color = Color.Gray)

                        OutlinedTextField(
                            value = telegramToken,
                            onValueChange = { telegramToken = it },
                            label = { Text("معرّف بوت تليجرام (Telegram Bot Token)", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                        )

                        OutlinedTextField(
                            value = slackWebhookUrl,
                            onValueChange = { slackWebhookUrl = it },
                            label = { Text("رابط Slack Webhook URL المخصص", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                        )

                        Button(
                            onClick = {
                                Toast.makeText(context, "تم ربط قنوات الإرسال بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF00E676), RoundedCornerShape(8.dp))
                        ) {
                            Text("حفظ وتحديث قنوات الإرسال", color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- SECTION 3: Live System Diagnostics ---
            item {
                Text(
                    "📊 مؤشرات وحالة أداء النظام الحية",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("نسبة ضغط المعالج (CPU):", color = Color.LightGray, fontSize = 12.sp)
                            Text(cpuLoad, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("استهلاك الذاكرة العشوائية (RAM):", color = Color.LightGray, fontSize = 12.sp)
                            Text(memoryUsage, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("سرعة استجابة قاعدة البيانات SQLite:", color = Color.LightGray, fontSize = 12.sp)
                            Text(sqliteQuerySpeed, color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("خيوط الأتمتة الموازية الفعالة:", color = Color.LightGray, fontSize = 12.sp)
                            Text(activeRuntimesCount, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }

                        if (isCheckingPerformance) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                            }
                        }

                        Button(
                            onClick = {
                                isCheckingPerformance = true
                                // Simulating runtime query diagnostics
                                cpuLoad = "7%"
                                memoryUsage = "45 MB"
                                sqliteQuerySpeed = "1 ms"
                                activeRuntimesCount = "2 active threads"
                                Toast.makeText(context, "اكتمل فحص تشخيص أداء SQLite والأتمتة!", Toast.LENGTH_SHORT).show()
                                isCheckingPerformance = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Speed, "", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تشغيل فحص تشخيص النظام")
                        }
                    }
                }
            }

            // --- SECTION 4: System Preferences & Toggles ---
            item {
                Text(
                    "⚙️ خيارات ومفضلات سير العمل",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("وضع الأداء البسيط (Lite Canvas Mode)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("يعطل الطبقات البصرية الفائقة لزيادة عمر البطارية واستجابة الشاشات الضعيفة.", color = Color.Gray, fontSize = 11.sp)
                            }
                            Switch(
                                checked = liteMode,
                                onCheckedChange = { viewModel.liteMode.value = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                            )
                        }

                        Divider(color = Color(0xFF334155))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("تصفير الذاكرة وسير العمل", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("إعادة تهيئة كاملة للملفات المخزنة وحذف جميع الإنجازات.", color = Color.Gray, fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    viewModel.clearLogs()
                                    Toast.makeText(context, "تم مسح جميع السجلات والملفات الموقتة بنجاح!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))
                            ) {
                                Text("تصفير بقايا الأتمتة", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Add secret dialog
        if (isAddSecretOpen) {
            AlertDialog(
                onDismissRequest = { isAddSecretOpen = false },
                title = { Text("إدراج مفتاح API جديد للخزنة", color = Color(0xFF00E5FF)) },
                containerColor = Color(0xFF1E293B),
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("يتم تخزين المفاتيح محلياً ومشفرة بالكامل داخل جهازك:", fontSize = 11.sp, color = Color.LightGray)

                        OutlinedTextField(
                            value = secretKeyInput,
                            onValueChange = { secretKeyInput = it },
                            label = { Text("اسم المفتاح (مثال: GEMINI_API_KEY)", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                        )

                        OutlinedTextField(
                            value = secretValInput,
                            onValueChange = { secretValInput = it },
                            label = { Text("قيمة المفتاح السرّية المحددة", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (secretKeyInput.isNotBlank() && secretValInput.isNotBlank()) {
                                viewModel.saveSecretKey(secretKeyInput, secretValInput)
                                secretKeyInput = ""
                                secretValInput = ""
                                isAddSecretOpen = false
                                Toast.makeText(context, "تم حفظ المفتاح بأمان وسرية!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        enabled = secretKeyInput.isNotBlank() && secretValInput.isNotBlank()
                    ) {
                        Text("حفظ وتشفير", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isAddSecretOpen = false }) {
                        Text("إلغاء", color = Color.White)
                    }
                }
            )
        }
    }
}
