package com.example.clock.ui.alarms

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clock.data.audio.ProceduralSoundGenerator
import com.example.clock.data.audio.SoundItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundPickerSheet(
    selectedSoundUri: String,
    onSoundSelected: (uri: String, name: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun playPreview(uriStr: String) {
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
            val uri = ProceduralSoundGenerator.getSoundFileUri(context, uriStr)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                prepare()
                start()
            }
            previewPlayer = mp
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.stop()
            previewPlayer?.release()
            previewPlayer = null
        }
    }

    // System ringtones
    val systemRingtones = remember {
        val list = mutableListOf<SoundItem>()
        val rm = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
        val cursor = rm.cursor
        while (cursor != null && cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = rm.getRingtoneUri(cursor.position).toString()
            list.add(SoundItem(uri, title, isProcedural = false))
        }
        list
    }

    // Audio file picker launcher
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val name = uri.lastPathSegment ?: "Custom Audio"
            onSoundSelected(uri.toString(), name)
            playPreview(uri.toString())
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sound",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                TextButton(onClick = onDismiss) {
                    Text("Done", color = Color(0xFF0A84FF), fontSize = 17.sp)
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = "ORIGINAL SYNTHESIZED TONES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(ProceduralSoundGenerator.PROCEDURAL_ALARM_TONES) { sound ->
                    val isSelected = sound.id == selectedSoundUri
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSoundSelected(sound.id, sound.name)
                                playPreview(sound.id)
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp)
                            .testTag("sound_${sound.name.lowercase().replace(" ", "_")}"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF0A84FF) else Color(0xFF8E8E93),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = sound.name,
                                fontSize = 17.sp,
                                color = if (isSelected) Color(0xFF0A84FF) else Color.White
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color(0xFF0A84FF)
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 0.5.dp)
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "CUSTOM FILE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { safLauncher.launch(arrayOf("audio/*")) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color(0xFF0A84FF),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Pick audio file from device...",
                            fontSize = 17.sp,
                            color = Color(0xFF0A84FF)
                        )
                    }
                    HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 0.5.dp)
                }

                if (systemRingtones.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "SYSTEM RINGTONES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8E8E93),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(systemRingtones) { sound ->
                        val isSelected = sound.id == selectedSoundUri
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSoundSelected(sound.id, sound.name)
                                    playPreview(sound.id)
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sound.name,
                                fontSize = 17.sp,
                                color = if (isSelected) Color(0xFF0A84FF) else Color.White
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF0A84FF)
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
