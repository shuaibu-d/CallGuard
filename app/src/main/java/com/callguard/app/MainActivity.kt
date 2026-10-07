package com.callguard.app

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.callguard.app.data.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CallGuardTheme {
                CallGuardApp(this)
            }
        }
    }
}

class MainViewModel(private val db: AppDatabase) : ViewModel() {
    val rules = db.ruleDao().observeAll()
    val history = db.callHistoryDao().observeAll()

    suspend fun add(name: String, pattern: String, type: MatchType) {
        db.ruleDao().insert(RuleEntity(name = name, pattern = pattern, matchType = type))
    }
    suspend fun delete(rule: RuleEntity) = db.ruleDao().delete(rule)
    suspend fun toggle(rule: RuleEntity) = db.ruleDao().update(rule.copy(enabled = !rule.enabled))
    suspend fun clearHistory() = db.callHistoryDao().clear()
}

@Suppress("UNCHECKED_CAST")
class MainViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MainViewModel(db) as T
}

@Composable
fun CallGuardApp(context: Context) {
    val db = remember { AppDatabase.get(context) }
    val vm: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = MainViewModelFactory(db)
    )
    val rules by vm.rules.collectAsStateWithLifecycle(emptyList())
    val history by vm.history.collectAsStateWithLifecycle(emptyList())
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val items = listOf(
                    Triple("home", "Home", Icons.Default.Home),
                    Triple("rules", "Rules", Icons.Default.Tune),
                    Triple("history", "History", Icons.Default.History),
                    Triple("settings", "Settings", Icons.Default.Settings)
                )
                items.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        selected = nav.currentBackStackEntryAsState().value?.destination?.route == route,
                        onClick = { nav.navigate(route) },
                        icon = { Icon(icon, null) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(rules, history, onAdd = { nav.navigate("add") })
            }
            composable("rules") {
                RulesScreen(
                    rules,
                    onAdd = { nav.navigate("add") },
                    onToggle = { scope.launch { vm.toggle(it) } },
                    onDelete = { scope.launch { vm.delete(it) } }
                )
            }
            composable("add") {
                AddRuleScreen(
                    onBack = { nav.popBackStack() },
                    onSave = { name, pattern, type ->
                        scope.launch {
                            vm.add(name, pattern, type)
                            nav.popBackStack()
                        }
                    }
                )
            }
            composable("history") {
                HistoryScreen(history, onClear = { scope.launch { vm.clearHistory() } })
            }
            composable("settings") {
                SettingsScreen(context)
            }
        }
    }
}

@Composable
fun HomeScreen(rules: List<RuleEntity>, history: List<CallHistoryEntity>, onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("CallGuard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Shield, null, Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("Protection ready", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Your blocking rules run on this phone.")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Rules", rules.count { it.enabled }.toString(), Modifier.weight(1f))
            StatCard("Blocked", history.size.toString(), Modifier.weight(1f))
        }
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Add blocking rule")
        }
        Text("Recent blocked calls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        history.take(5).forEach {
            ListItem(
                headlineContent = { Text(maskNumber(it.phoneNumber)) },
                supportingContent = { Text(formatTime(it.timestamp)) },
                leadingContent = { Icon(Icons.Default.Block, null) }
            )
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label)
        }
    }
}

@Composable
fun RulesScreen(
    rules: List<RuleEntity>,
    onAdd: () -> Unit,
    onToggle: (RuleEntity) -> Unit,
    onDelete: (RuleEntity) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Blocking Rules", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            FilledTonalIconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Add") }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rules, key = { it.id }) { rule ->
                Card {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Block, null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(rule.name, fontWeight = FontWeight.Bold)
                            Text("${rule.matchType.name.lowercase()} • ${rule.pattern}")
                        }
                        Switch(checked = rule.enabled, onCheckedChange = { onToggle(rule) })
                        IconButton(onClick = { onDelete(rule) }) {
                            Icon(Icons.Default.Delete, "Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddRuleScreen(
    onBack: () -> Unit,
    onSave: (String, String, MatchType) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(MatchType.PREFIX) }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Add Blocking Rule", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(name, { name = it }, label = { Text("Rule name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(pattern, { pattern = it }, label = { Text("Number or prefix") }, modifier = Modifier.fillMaxWidth())
        Text("Match type", fontWeight = FontWeight.Bold)
        MatchType.values().forEach {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = type == it, onClick = { type = it })
                Text(
                    when (it) {
                        MatchType.PREFIX -> "Starts with"
                        MatchType.EXACT -> "Exact number"
                        MatchType.CONTAINS -> "Contains"
                    }
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = { onSave(name.trim().ifEmpty { "Block ${pattern.trim()}" }, pattern.trim(), type) },
                enabled = pattern.trim().isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) { Text("Save rule") }
        }
    }
}

@Composable
fun HistoryScreen(history: List<CallHistoryEntity>, onClear: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Blocked Calls", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onClear, enabled = history.isNotEmpty()) { Text("Clear") }
        }
        LazyColumn {
            items(history, key = { it.id }) {
                ListItem(
                    headlineContent = { Text(maskNumber(it.phoneNumber)) },
                    supportingContent = { Text(formatTime(it.timestamp)) },
                    leadingContent = { Icon(Icons.Default.Block, null) }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(context: Context) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card {
            Column(Modifier.padding(18.dp)) {
                Text("Call screening", fontWeight = FontWeight.Bold)
                Text("Android must designate CallGuard as the call-screening app for automatic blocking.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    try {
                        val roleManager = context.getSystemService(RoleManager::class.java)
                        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                            context.startActivity(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
                        } else {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    } catch (_: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }) {
                    Text("Enable call screening")
                }
            }
        }
        ListItem(
            leadingContent = { Icon(Icons.Default.CheckCircle, null) },
            headlineContent = { Text("Privacy-first") },
            supportingContent = { Text("Rules and blocked-call history stay on this device.") }
        )
        ListItem(
            leadingContent = { Icon(Icons.Default.Shield, null) },
            headlineContent = { Text("CallGuard 1.0.0") },
            supportingContent = { Text("Offline core • No account required") }
        )
    }
}

@Composable
fun CallGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF4F46E5),
            secondary = androidx.compose.ui.graphics.Color(0xFFF59E0B)
        ),
        content = content
    )
}

fun maskNumber(number: String): String {
    if (number.length <= 6) return "••••"
    return number.take(3) + "••••" + number.takeLast(3)
}

fun formatTime(time: Long): String =
    SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(time))
