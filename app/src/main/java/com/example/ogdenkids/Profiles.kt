package com.example.ogdenkids

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

const val MaxProfiles = 2

/** [birthday] 形如 2019-05-01。 */
data class Profile(
    val id: String,
    val zhName: String,
    val enName: String,
    val birthday: String,
    val hobbies: String = "",
    val specialty: String = ""
) {
    val age: Int
        get() {
            val (y, m, d) = birthday.split("-").map { it.toInt() }
            val now = Calendar.getInstance()
            val month = now.get(Calendar.MONTH) + 1
            val day = now.get(Calendar.DAY_OF_MONTH)
            return now.get(Calendar.YEAR) - y - if (month < m || (month == m && day < d)) 1 else 0
        }
}

val LocalProfile = staticCompositionLocalOf<Profile?> { null }

/** 打开当前用户的「我的信息」页；为空时顶栏用户不可点。 */
val LocalEditProfile = staticCompositionLocalOf<(() -> Unit)?> { null }

/** 本机用户列表与当前登录的用户，不设密码。 */
class ProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("ogden-users", Context.MODE_PRIVATE)

    fun profiles(): List<Profile> {
        val array = JSONArray(prefs.getString("profiles", "[]"))
        return List(array.length()) { i ->
            array.getJSONObject(i).let {
                Profile(
                    it.getString("id"), it.getString("zhName"), it.getString("enName"), it.getString("birthday"),
                    it.optString("hobbies"), it.optString("specialty")
                )
            }
        }
    }

    fun currentId(): String? = prefs.getString("current", null)

    fun switchTo(id: String) {
        prefs.edit().putString("current", id).commit()
    }

    fun newId(): String = if (profiles().isEmpty()) FirstProfileId else UUID.randomUUID().toString()

    fun save(profile: Profile) {
        val list = profiles().toMutableList()
        val index = list.indexOfFirst { it.id == profile.id }
        if (index >= 0) list[index] = profile else if (list.size < MaxProfiles) list += profile else return
        val array = JSONArray()
        list.forEach {
            array.put(
                JSONObject().put("id", it.id).put("zhName", it.zhName).put("enName", it.enName).put("birthday", it.birthday)
                    .put("hobbies", it.hobbies).put("specialty", it.specialty)
            )
        }
        prefs.edit().putString("profiles", array.toString()).commit()
    }

    companion object {
        /** 第一个用户沿用多用户之前的进度文件，升级后老进度不丢。 */
        const val FirstProfileId = "default"

        fun progressPrefsName(profileId: String) =
            if (profileId == FirstProfileId) "ogden-progress" else "ogden-progress-$profileId"
    }
}

@Composable
fun ProfileAvatar(profile: Profile, size: Int = 44) {
    Box(
        Modifier
            .size(size.dp)
            .background(Category.Operations.tint, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AppText(profile.zhName.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size * 0.42f).sp)
    }
}

/** 页面顶栏右侧的当前用户，提示本页成绩记在谁名下。 */
@Composable
fun CurrentUserChip() {
    val profile = LocalProfile.current ?: return
    val onEdit = LocalEditProfile.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .then(if (onEdit != null) Modifier.clickable(onClick = onEdit) else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ProfileAvatar(profile, size = 28)
        AppText(profile.zhName, color = InkSoft, fontSize = 13.sp, maxLines = 1)
    }
}

/** 新建 / 查看并修改用户信息。[onBack] 为空表示首次启动必须先建一个用户。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(initial: Profile?, newId: () -> String, onSave: (Profile) -> Unit, onBack: (() -> Unit)?) {
    val context = LocalContext.current
    var zhName by rememberSaveable { mutableStateOf(initial?.zhName.orEmpty()) }
    var enName by rememberSaveable { mutableStateOf(initial?.enName.orEmpty()) }
    var birthday by rememberSaveable { mutableStateOf(initial?.birthday.orEmpty()) }
    var hobbies by rememberSaveable { mutableStateOf(initial?.hobbies.orEmpty()) }
    var specialty by rememberSaveable { mutableStateOf(initial?.specialty.orEmpty()) }
    // 介绍弹窗打开的页（见 ZodiacIntroDialog），null 为关闭；查词回来时弹窗还在
    var zodiacTab by rememberSaveable { mutableStateOf<Int?>(null) }
    val valid = zhName.isNotBlank() && enName.isNotBlank() && birthday.isNotEmpty()

    fun pickBirthday() {
        val (y, m, d) = birthday.takeIf { it.isNotEmpty() }?.split("-")?.map { it.toInt() } ?: listOf(2018, 1, 1)
        DatePickerDialog(context, { _, year, month, day ->
            birthday = "%04d-%02d-%02d".format(year, month + 1, day)
        }, y, m - 1, d).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
    }

    Scaffold(
        containerColor = Paper,
        topBar = {
            TopAppBar(
                title = { AppText(if (initial == null) "新建用户" else "我的信息", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (onBack == null) {
                AppText("欢迎使用 Anna Learning", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                AppText("先填一下小朋友的信息，学习进度会记在这个用户下面。最多可以建 $MaxProfiles 个用户。", color = InkSoft)
            }
            OutlinedTextField(zhName, { zhName = it.take(12) }, label = { Text("中文名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(enName, { enName = it.take(20) }, label = { Text("英文名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = ::pickBirthday, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(8.dp)) {
                Text(if (birthday.isEmpty()) "选择生日" else "生日 $birthday", fontSize = 16.sp)
            }
            if (birthday.isNotEmpty()) {
                val sign = starSignOf(birthday)
                val (stem, zodiac) = ganzhiOf(birthday)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ZodiacCard("星座", "${sign.symbol} ${sign.zh} ${sign.en}", Modifier.weight(1f)) { zodiacTab = 0 }
                    ZodiacCard("生肖", "${zodiac.emoji} 属${zodiac.zh} · ${stem.zh}${zodiac.branch}年", Modifier.weight(1f)) { zodiacTab = 1 }
                }
            }
            OutlinedTextField(
                hobbies, { hobbies = it.take(60) },
                label = { Text("兴趣爱好") }, placeholder = { Text("如：画画、游泳、搭乐高") },
                maxLines = 3, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                specialty, { specialty = it.take(60) },
                label = { Text("特长") }, placeholder = { Text("如：钢琴、跳绳、背古诗") },
                maxLines = 3, modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onSave(Profile(initial?.id ?: newId(), zhName.trim(), enName.trim(), birthday, hobbies.trim(), specialty.trim())) },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text(if (initial == null) "完成" else "保存", fontSize = 18.sp) }
        }
    }
    zodiacTab?.let { tab ->
        if (birthday.isNotEmpty()) ZodiacIntroDialog(birthday, tab, onTab = { zodiacTab = it }, onDismiss = { zodiacTab = null })
    }
}

@Composable
private fun ZodiacCard(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Category.Operations.soft)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        AppText("$label · 点我看介绍", color = InkSoft, fontSize = 12.sp)
        AppText(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
