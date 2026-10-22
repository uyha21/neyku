package id.neyku.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFF0B1110)
private val Panel = Color(0xFF151F1C)
private val Panel2 = Color(0xFF1D2A25)
private val Green = Color(0xFF61E294)
private val Muted = Color(0xFF9AAEA4)
private val Red = Color(0xFFFF7777)
private val White = Color(0xFFF1F7F3)

data class Tx(val id: Long, val title: String, val amount: Long, val income: Boolean, val category: String, val wallet: String, val date: Long)
data class Wallet(val name: String, val opening: Long)
data class Budget(val category: String, val limit: Long)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(11,17,16)
        window.navigationBarColor = android.graphics.Color.rgb(11,17,16)
        setContent { NeyKuApp() }
    }
}

private fun money(v: Long): String = NumberFormat.getCurrencyInstance(Locale("id","ID")).format(v).replace(",00","")
private fun dateText(t: Long): String = SimpleDateFormat("dd MMM yyyy", Locale("id","ID")).format(Date(t))
private fun monthKey(t: Long): String = SimpleDateFormat("yyyyMM", Locale.US).format(Date(t))

class Store(context: Context) {
    private val p = context.getSharedPreferences("neyku_local_v1", Context.MODE_PRIVATE)
    fun pin() = p.getString("pin", "") ?: ""
    fun setPin(v: String) { p.edit().putString("pin", v).apply() }
    fun wallets(): List<Wallet> {
        val a = JSONArray(p.getString("wallets", "[]"))
        return (0 until a.length()).map { val o=a.getJSONObject(it); Wallet(o.getString("name"),o.optLong("opening")) }
    }
    fun saveWallets(list: List<Wallet>) {
        val a=JSONArray(); list.forEach { a.put(JSONObject().put("name",it.name).put("opening",it.opening)) }
        p.edit().putString("wallets",a.toString()).apply()
    }
    fun txs(): List<Tx> {
        val a=JSONArray(p.getString("txs","[]"))
        return (0 until a.length()).map { val o=a.getJSONObject(it); Tx(o.getLong("id"),o.getString("title"),o.getLong("amount"),o.getBoolean("income"),o.getString("category"),o.getString("wallet"),o.getLong("date")) }.sortedByDescending { it.date }
    }
    fun saveTxs(list: List<Tx>) {
        val a=JSONArray(); list.forEach { a.put(JSONObject().put("id",it.id).put("title",it.title).put("amount",it.amount).put("income",it.income).put("category",it.category).put("wallet",it.wallet).put("date",it.date)) }
        p.edit().putString("txs",a.toString()).apply()
    }
    fun budgets(): List<Budget> {
        val a=JSONArray(p.getString("budgets","[]"))
        return (0 until a.length()).map { val o=a.getJSONObject(it); Budget(o.getString("category"),o.getLong("limit")) }
    }
    fun saveBudgets(list: List<Budget>) {
        val a=JSONArray(); list.forEach { a.put(JSONObject().put("category",it.category).put("limit",it.limit)) }
        p.edit().putString("budgets",a.toString()).apply()
    }
}

@Composable
fun NeyKuApp() {
    val context = LocalContext.current
    val store = remember { Store(context) }
    var pin by remember { mutableStateOf(store.pin()) }
    var unlocked by remember { mutableStateOf(pin.isBlank()) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }
    var txs by remember { mutableStateOf(store.txs()) }
    var wallets by remember { mutableStateOf(store.wallets().ifEmpty { listOf(Wallet("Tunai",0), Wallet("Bank",0), Wallet("E-wallet",0)) }) }
    var budgets by remember { mutableStateOf(store.budgets()) }
    var tab by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var showWallet by remember { mutableStateOf(false) }
    var showBudget by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf("") }
    var exportPending by remember { mutableStateOf("") }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if(uri!=null) try {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(exportPending) }
            toast="Laporan CSV berhasil diekspor."
        } catch (_: Exception) { toast="Gagal mengekspor CSV." }
    }

    fun persistTx(list: List<Tx>) { txs=list; store.saveTxs(list) }
    fun persistWallet(list: List<Wallet>) { wallets=list; store.saveWallets(list) }
    fun persistBudget(list: List<Budget>) { budgets=list; store.saveBudgets(list) }

    MaterialTheme(colorScheme = darkColorScheme(
        primary=Green, onPrimary=Bg, background=Bg, surface=Panel, onSurface=White,
        secondary=Green, onBackground=White, error=Red
    )) {
        Surface(Modifier.fillMaxSize(), color=Bg) {
            if (!unlocked) {
                PinScreen(pinInput, { pinInput=it.filter(Char::isDigit).take(6) }, pin.isBlank(), pinError) {
                    if (pin.isBlank() && pinInput.length >= 4) { store.setPin(pinInput); pin=pinInput; unlocked=true; pinInput="" }
                    else if (pinInput == pin) { unlocked=true; pinInput=""; pinError="" }
                    else pinError="PIN salah. Coba lagi."
                }
            } else {
                Scaffold(
                    containerColor=Bg,
                    topBar = { TopBar(onPin={showPin=true}, onExport={
                        val csv = buildString {
                            append("Tanggal,Judul,Jenis,Kategori,Dompet,Nominal\n")
                            txs.sortedBy { it.date }.forEach { t ->
                                append("\"${dateText(t.date)}\",\"${t.title.replace("\"","\"\"")}\",${if(t.income)"Pemasukan" else "Pengeluaran"},\"${t.category}\",\"${t.wallet}\",${t.amount}\n")
                            }
                        }
                        val i=Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE); type="text/csv"; putExtra(Intent.EXTRA_TITLE,"neyku-laporan.csv")
                        }
                        exportPending = csv
                        exportLauncher.launch("neyku-laporan.csv")
                    }) },
                    bottomBar = { NavigationBar(containerColor=Panel, contentColor=Muted) {
                        listOf("Ringkasan","Transaksi","Anggaran","Dompet").forEachIndexed { i, label ->
                            NavigationBarItem(selected=tab==i, onClick={tab=i}, icon={
                                Icon(when(i){0->Icons.Default.Dashboard;1->Icons.Default.ReceiptLong;2->Icons.Default.PieChart;else->Icons.Default.AccountBalanceWallet}, null)
                            }, label={Text(label)}, colors=NavigationBarItemDefaults.colors(selectedIconColor=Green, selectedTextColor=Green, indicatorColor=Panel2, unselectedIconColor=Muted, unselectedTextColor=Muted))
                        }
                    }},
                    floatingActionButton = { if(tab==0 || tab==1) FloatingActionButton(onClick={showAdd=true}, containerColor=Green, contentColor=Bg) { Icon(Icons.Default.Add, "Tambah transaksi") } }
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        when(tab) {
                            0 -> Dashboard(txs, wallets, budgets, onAdd={showAdd=true}, onReports={tab=1})
                            1 -> Transactions(txs, onDelete={t -> persistTx(txs.filterNot { it.id==t.id })})
                            2 -> BudgetsScreen(budgets, txs, onAdd={showBudget=true}, onDelete={b -> persistBudget(budgets.filterNot { it.category==b.category })})
                            else -> WalletsScreen(wallets, txs, onAdd={showWallet=true}, onDelete={w -> if(wallets.size>1) persistWallet(wallets.filterNot { it.name==w.name }) else toast="Minimal satu dompet harus ada."})
                        }
                    }
                }
                if(showAdd) AddTransactionDialog(wallets, onDismiss={showAdd=false}, onSave={title,amount,income,category,wallet ->
                    persistTx(listOf(Tx(System.currentTimeMillis(),title,amount,income,category,wallet,System.currentTimeMillis()))+txs)
                    showAdd=false
                })
                if(showWallet) AddWalletDialog(onDismiss={showWallet=false}, onSave={name,opening ->
                    if(wallets.none { it.name.equals(name,true) }) persistWallet(wallets+Wallet(name,opening)) else toast="Nama dompet sudah ada."
                    showWallet=false
                })
                if(showBudget) AddBudgetDialog(onDismiss={showBudget=false}, onSave={category,limit ->
                    persistBudget(budgets.filterNot { it.category==category }+Budget(category,limit)); showBudget=false
                })
                if(showPin) PinSettingsDialog(hasPin=pin.isNotBlank(), onDismiss={showPin=false}, onSave={newPin ->
                    store.setPin(newPin); pin=newPin; showPin=false; toast="PIN berhasil diperbarui."
                }, onRemove={store.setPin(""); pin=""; showPin=false; toast="PIN dinonaktifkan."})
                if(toast.isNotBlank()) AlertDialog(onDismissRequest={toast=""}, confirmButton={TextButton(onClick={toast=""}) {Text("OK",color=Green)}}, title={Text("neyKu")}, text={Text(toast)})
            }
        }
    }
}

@Composable
fun TopBar(onPin:()->Unit, onExport:()->Unit) {
    Row(Modifier.fillMaxWidth().background(Bg).padding(horizontal=20.dp, vertical=16.dp), verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(Green, RoundedCornerShape(13.dp)), contentAlignment=Alignment.Center) {
            Icon(Icons.Default.Savings, null, tint=Bg, modifier=Modifier.size(25.dp))
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text("neyKu", fontSize=22.sp, fontWeight=FontWeight.Bold, color=White)
            Text("Atur uang, tenang pikiran", fontSize=11.sp, color=Muted)
        }
        IconButton(onClick=onExport) { Icon(Icons.Default.FileDownload, "Ekspor CSV", tint=Green) }
        IconButton(onClick=onPin) { Icon(Icons.Default.Lock, "Pengaturan PIN", tint=Green) }
    }
}

@Composable
fun Dashboard(txs:List<Tx>, wallets:List<Wallet>, budgets:List<Budget>, onAdd:()->Unit, onReports:()->Unit) {
    val now=System.currentTimeMillis()
    val monthTx=txs.filter { monthKey(it.date)==monthKey(now) }
    val income=monthTx.filter { it.income }.sumOf { it.amount }
    val expense=monthTx.filterNot { it.income }.sumOf { it.amount }
    val balance=wallets.sumOf { it.opening } + txs.sumOf { if(it.income) it.amount else -it.amount }
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(18.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(BrushLike(), RoundedCornerShape(24.dp)).padding(22.dp)) {
                Text("TOTAL SALDO", color=Muted, fontSize=12.sp, letterSpacing=1.5.sp)
                Spacer(Modifier.height(7.dp))
                Text(money(balance), fontSize=31.sp, fontWeight=FontWeight.Bold, color=White)
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("↑ Pemasukan bulan ini", color=Muted, fontSize=12.sp); Text(money(income), color=Green, fontWeight=FontWeight.SemiBold, fontSize=16.sp) }
                    Column(Modifier.weight(1f)) { Text("↓ Pengeluaran bulan ini", color=Muted, fontSize=12.sp); Text(money(expense), color=Red, fontWeight=FontWeight.SemiBold, fontSize=16.sp) }
                }
            }
        }
        item {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("Laporan pengeluaran", fontWeight=FontWeight.Bold, fontSize=17.sp, color=White, modifier=Modifier.weight(1f))
                Text("Lihat transaksi", color=Green, fontSize=12.sp, modifier=Modifier.clickable { onReports() })
            }
            Spacer(Modifier.height(8.dp))
            Card(colors=CardDefaults.cardColors(containerColor=Panel), shape=RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Pengeluaran 7 hari terakhir", color=Muted, fontSize=12.sp)
                    Spacer(Modifier.height(10.dp))
                    val days=(6 downTo 0).map { d ->
                        val cal=java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR,-d) }
                        val start=cal.apply { set(java.util.Calendar.HOUR_OF_DAY,0); set(java.util.Calendar.MINUTE,0); set(java.util.Calendar.SECOND,0); set(java.util.Calendar.MILLISECOND,0) }.timeInMillis
                        val end=start+86400000L
                        txs.filter { !it.income && it.date>=start && it.date<end }.sumOf { it.amount }
                    }
                    val max=days.maxOrNull()?.coerceAtLeast(1L) ?: 1L
                    Canvas(Modifier.fillMaxWidth().height(90.dp)) {
                        val slot=size.width/7
                        days.forEachIndexed { i,v ->
                            val h=(v.toFloat()/max)*size.height
                            drawLine(if(i==6) Green else Color(0xFF3C5B4A), Offset(slot*i+slot/2,size.height), Offset(slot*i+slot/2,size.height-h), strokeWidth=slot*.42f, cap=StrokeCap.Round)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
                        listOf("6h","5h","4h","3h","2h","1h","Hari ini").forEach { Text(it,color=Muted,fontSize=9.sp) }
                    }
                }
            }
        }
        item { Text("Anggaran bulan ini", fontWeight=FontWeight.Bold, fontSize=17.sp, color=White) }
        if(budgets.isEmpty()) item { EmptyCard("Belum ada anggaran", "Buka tab Anggaran untuk menetapkan batas pengeluaran.") }
        items(budgets) { b ->
            val spent=monthTx.filter { !it.income && it.category==b.category }.sumOf { it.amount }
            BudgetProgress(b.category, spent, b.limit)
        }
        item { Text("Transaksi terbaru", fontWeight=FontWeight.Bold, fontSize=17.sp, color=White) }
        if(txs.isEmpty()) item { EmptyCard("Belum ada transaksi", "Tekan tombol + untuk mencatat pemasukan atau pengeluaran.") }
        items(txs.take(5)) { TxRow(it) }
        item { Spacer(Modifier.height(64.dp)) }
    }
}

@Composable
fun BrushLike(): Color = Panel2

@Composable
fun Transactions(txs:List<Tx>, onDelete:(Tx)->Unit) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Semua") }
    val filtered=txs.filter { it.title.contains(query,true) || it.category.contains(query,true) || it.wallet.contains(query,true) }
        .filter { filter=="Semua" || (filter=="Pemasukan" && it.income) || (filter=="Pengeluaran" && !it.income) }
    Column(Modifier.fillMaxSize().padding(horizontal=18.dp)) {
        Text("Semua transaksi", fontSize=23.sp, fontWeight=FontWeight.Bold, color=White, modifier=Modifier.padding(top=10.dp,bottom=12.dp))
        OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("Cari transaksi...",color=Muted)},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true,colors=fieldColors())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("Semua","Pemasukan","Pengeluaran").forEach { label ->
            FilterChip(selected=filter==label,onClick={filter=label},label={Text(label)},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Green,selectedLabelColor=Bg))
        } }
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp), contentPadding=PaddingValues(bottom=80.dp)) {
            if(filtered.isEmpty()) item { EmptyCard("Tidak ada transaksi", "Coba ubah pencarian atau tambahkan transaksi.") }
            items(filtered, key={it.id}) { tx -> Row(verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { TxRow(tx) }
                IconButton(onClick={onDelete(tx)}) { Icon(Icons.Default.DeleteOutline,null,tint=Muted) }
            } }
        }
    }
}

@Composable
fun TxRow(tx:Tx) {
    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(13.dp), verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(Panel2,CircleShape), contentAlignment=Alignment.Center) {
            Icon(categoryIcon(tx.category),null,tint=if(tx.income) Green else Muted)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tx.title, color=White, fontWeight=FontWeight.SemiBold, maxLines=1)
            Text("${tx.category} • ${tx.wallet}", color=Muted, fontSize=11.sp)
            Text(dateText(tx.date), color=Muted, fontSize=10.sp)
        }
        Text((if(tx.income) "+" else "−")+money(tx.amount), color=if(tx.income) Green else White, fontWeight=FontWeight.Bold, fontSize=13.sp)
    }
}

@Composable
fun BudgetsScreen(budgets:List<Budget>, txs:List<Tx>, onAdd:()->Unit, onDelete:(Budget)->Unit) {
    val now=System.currentTimeMillis()
    val monthTx=txs.filter { monthKey(it.date)==monthKey(now) && !it.income }
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Anggaran",fontSize=24.sp,fontWeight=FontWeight.Bold,color=White); Text("Pantau batas pengeluaran bulanan",color=Muted,fontSize=12.sp) }
            Button(onClick=onAdd, colors=ButtonDefaults.buttonColors(containerColor=Green,contentColor=Bg)) { Icon(Icons.Default.Add,null); Text("Tambah") }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp), contentPadding=PaddingValues(bottom=24.dp)) {
            if(budgets.isEmpty()) item { EmptyCard("Mulai atur anggaran", "Tetapkan batas per kategori agar pengeluaran lebih terkontrol.") }
            items(budgets) { b ->
                val spent=monthTx.filter { it.category==b.category }.sumOf { it.amount }
                Column(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(18.dp)).padding(16.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text(b.category,fontWeight=FontWeight.Bold,color=White,modifier=Modifier.weight(1f))
                        IconButton(onClick={onDelete(b)}) { Icon(Icons.Default.DeleteOutline,null,tint=Muted) }
                    }
                    BudgetProgress(b.category,spent,b.limit)
                }
            }
        }
    }
}

@Composable
fun BudgetProgress(category:String, spent:Long, limit:Long) {
    val ratio=if(limit<=0) 0f else (spent.toFloat()/limit).coerceIn(0f,1f)
    Column(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(16.dp)).padding(15.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text(category,color=White,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
            Text("${(ratio*100).toInt()}%",color=if(spent>limit) Red else Green,fontWeight=FontWeight.Bold)
        }
        Spacer(Modifier.height(9.dp))
        LinearProgressIndicator(progress={ratio},modifier=Modifier.fillMaxWidth().height(7.dp),color=if(spent>limit) Red else Green,trackColor=Panel2)
        Spacer(Modifier.height(6.dp))
        Row { Text("Terpakai ${money(spent)}",color=Muted,fontSize=11.sp,modifier=Modifier.weight(1f)); Text("Batas ${money(limit)}",color=Muted,fontSize=11.sp) }
        if(spent>limit) Text("Anggaran terlampaui ${money(spent-limit)}",color=Red,fontSize=11.sp)
    }
}

@Composable
fun WalletsScreen(wallets:List<Wallet>, txs:List<Tx>, onAdd:()->Unit, onDelete:(Wallet)->Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Dompet & rekening",fontSize=23.sp,fontWeight=FontWeight.Bold,color=White); Text("Saldo dihitung dari saldo awal dan transaksi",color=Muted,fontSize=11.sp) }
            Button(onClick=onAdd,colors=ButtonDefaults.buttonColors(containerColor=Green,contentColor=Bg)) { Icon(Icons.Default.Add,null); Text("Tambah") }
        }
        Spacer(Modifier.height(14.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            items(wallets) { w ->
                val total=w.opening+txs.filter { it.wallet==w.name }.sumOf { if(it.income) it.amount else -it.amount }
                Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(18.dp)).padding(17.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(45.dp).background(Panel2,CircleShape),contentAlignment=Alignment.Center) { Icon(Icons.Default.AccountBalanceWallet,null,tint=Green) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(w.name,color=White,fontWeight=FontWeight.Bold); Text("Saldo awal ${money(w.opening)}",color=Muted,fontSize=11.sp); Text(money(total),color=Green,fontWeight=FontWeight.Bold,fontSize=18.sp) }
                    IconButton(onClick={onDelete(w)}) { Icon(Icons.Default.DeleteOutline,null,tint=Muted) }
                }
            }
        }
    }
}

@Composable
fun PinScreen(value:String, onChange:(String)->Unit, first:Boolean, error:String, onSubmit:()->Unit) {
    Column(Modifier.fillMaxSize().background(Bg).padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
        Box(Modifier.size(72.dp).background(Green,CircleShape),contentAlignment=Alignment.Center) { Icon(Icons.Default.Savings,null,tint=Bg,modifier=Modifier.size(40.dp)) }
        Spacer(Modifier.height(18.dp))
        Text("neyKu",fontSize=30.sp,fontWeight=FontWeight.Bold,color=White)
        Text(if(first) "Buat PIN 4–6 digit untuk melindungi aplikasi" else "Masukkan PIN untuk melanjutkan",color=Muted)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value,onChange,label={Text("PIN")},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),colors=fieldColors())
        if(error.isNotBlank()) Text(error,color=Red)
        Spacer(Modifier.height(14.dp))
        Button(onClick=onSubmit,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Green,contentColor=Bg)) { Text(if(first) "Simpan PIN" else "Buka neyKu") }
        Text("Data disimpan lokal di perangkat ini.",color=Muted,fontSize=11.sp,modifier=Modifier.padding(top=18.dp))
    }
}

@Composable
fun PinSettingsDialog(hasPin:Boolean,onDismiss:()->Unit,onSave:(String)->Unit,onRemove:()->Unit) {
    var v by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest=onDismiss,containerColor=Panel,title={Text("Keamanan PIN",color=White)},text={
        Column {
            Text(if(hasPin) "Masukkan PIN baru (4–6 digit)." else "Buat PIN baru (4–6 digit).",color=Muted)
            OutlinedTextField(v,{v=it.filter(Char::isDigit).take(6)},label={Text("PIN baru")},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),colors=fieldColors())
            if(hasPin) TextButton(onClick=onRemove) { Text("Nonaktifkan PIN",color=Red) }
        }
    },confirmButton={TextButton(onClick={if(v.length in 4..6) onSave(v)}) {Text("Simpan",color=Green)}},dismissButton={TextButton(onClick=onDismiss){Text("Batal",color=Muted)}})
}

@Composable
fun AddTransactionDialog(wallets:List<Wallet>,onDismiss:()->Unit,onSave:(String,Long,Boolean,String,String)->Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var income by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("Makanan") }
    var wallet by remember { mutableStateOf(wallets.firstOrNull()?.name ?: "Tunai") }
    val categories=listOf("Makanan","Transportasi","Belanja","Tagihan","Kesehatan","Hiburan","Gaji","Lainnya")
    AlertDialog(onDismissRequest=onDismiss,containerColor=Panel,title={Text("Tambah transaksi",color=White)},text={
        Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FilterChip(selected=!income,onClick={income=false},label={Text("Pengeluaran")},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Green,selectedLabelColor=Bg))
                FilterChip(selected=income,onClick={income=true},label={Text("Pemasukan")},colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Green,selectedLabelColor=Bg))
            }
            OutlinedTextField(title,{title=it},label={Text("Nama transaksi")},singleLine=true,colors=fieldColors())
            OutlinedTextField(amount,{amount=it.filter(Char::isDigit)},label={Text("Nominal (Rp)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),colors=fieldColors())
            Text("Kategori",color=Muted,fontSize=12.sp)
            var catExpanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick={catExpanded=true}) { Text(category,color=White); Icon(Icons.Default.ArrowDropDown,null) }
                DropdownMenu(expanded=catExpanded,onDismissRequest={catExpanded=false}) { categories.forEach { DropdownMenuItem(text={Text(it)},onClick={category=it;catExpanded=false}) } }
            }
            Text("Dompet",color=Muted,fontSize=12.sp)
            var walletExpanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick={walletExpanded=true}) { Text(wallet,color=White); Icon(Icons.Default.ArrowDropDown,null) }
                DropdownMenu(expanded=walletExpanded,onDismissRequest={walletExpanded=false}) { wallets.forEach { DropdownMenuItem(text={Text(it.name)},onClick={wallet=it.name;walletExpanded=false}) } }
            }
        }
    },confirmButton={TextButton(onClick={val n=amount.toLongOrNull() ?: 0L; if(title.isNotBlank() && n>0 && wallet.isNotBlank()) onSave(title.trim(),n,income,category,wallet)}) {Text("Simpan",color=Green)}},dismissButton={TextButton(onClick=onDismiss){Text("Batal",color=Muted)}})
}

@Composable
fun AddWalletDialog(onDismiss:()->Unit,onSave:(String,Long)->Unit) {
    var name by remember { mutableStateOf("") }; var opening by remember { mutableStateOf("0") }
    AlertDialog(onDismissRequest=onDismiss,containerColor=Panel,title={Text("Tambah dompet",color=White)},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name,{name=it},label={Text("Nama dompet")},singleLine=true,colors=fieldColors())
            OutlinedTextField(opening,{opening=it.filter(Char::isDigit)},label={Text("Saldo awal (Rp)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),colors=fieldColors())
        }
    },confirmButton={TextButton(onClick={val n=opening.toLongOrNull()?:0L;if(name.isNotBlank())onSave(name.trim(),n)}){Text("Simpan",color=Green)}},dismissButton={TextButton(onClick=onDismiss){Text("Batal",color=Muted)}})
}

@Composable
fun AddBudgetDialog(onDismiss:()->Unit,onSave:(String,Long)->Unit) {
    var category by remember { mutableStateOf("Makanan") }; var limit by remember { mutableStateOf("") }
    val cats=listOf("Makanan","Transportasi","Belanja","Tagihan","Kesehatan","Hiburan","Gaji","Lainnya")
    AlertDialog(onDismissRequest=onDismiss,containerColor=Panel,title={Text("Atur anggaran",color=White)},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            var expanded by remember { mutableStateOf(false) }
            Box { OutlinedButton(onClick={expanded=true}) { Text(category,color=White); Icon(Icons.Default.ArrowDropDown,null) }; DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) { cats.filter { it!="Gaji" }.forEach { DropdownMenuItem(text={Text(it)},onClick={category=it;expanded=false}) } } }
            OutlinedTextField(limit,{limit=it.filter(Char::isDigit)},label={Text("Batas per bulan (Rp)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),colors=fieldColors())
        }
    },confirmButton={TextButton(onClick={val n=limit.toLongOrNull()?:0L;if(n>0)onSave(category,n)} ){Text("Simpan",color=Green)}},dismissButton={TextButton(onClick=onDismiss){Text("Batal",color=Muted)}})
}

@Composable
fun EmptyCard(title:String, subtitle:String) {
    Column(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(16.dp)).padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Icon(Icons.Default.Inbox,null,tint=Muted,modifier=Modifier.size(28.dp))
        Spacer(Modifier.height(8.dp)); Text(title,color=White,fontWeight=FontWeight.SemiBold); Text(subtitle,color=Muted,fontSize=12.sp)
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor=Green, unfocusedBorderColor=Panel2, focusedLabelColor=Green,
    cursorColor=Green, focusedTextColor=White, unfocusedTextColor=White,
    focusedContainerColor=Bg, unfocusedContainerColor=Bg
)

fun categoryIcon(category:String) = when(category) {
    "Makanan" -> Icons.Default.Restaurant
    "Transportasi" -> Icons.Default.DirectionsCar
    "Belanja" -> Icons.Default.ShoppingBag
    "Tagihan" -> Icons.Default.Receipt
    "Kesehatan" -> Icons.Default.MedicalServices
    "Hiburan" -> Icons.Default.Movie
    "Gaji" -> Icons.Default.Payments
    else -> Icons.Default.Category
}
