@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.local.AppDatabase
import com.example.data.local.ScanHistoryEntity
import com.example.data.model.PlantScanReport
import com.example.data.repository.PlantDoctorRepository
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlantDoctorViewModel
import com.example.ui.viewmodel.PlantDoctorViewModelFactory
import com.example.ui.viewmodel.ScanUiState
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = PlantDoctorRepository(applicationContext, database.scanHistoryDao())
        val viewModelFactory = PlantDoctorViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                val viewModel: PlantDoctorViewModel = viewModel(factory = viewModelFactory)
                PlantDoctorApp(viewModel)
            }
        }
    }
}

@Composable
fun GeometricLogo() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(GeoPrimary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .border(2.dp, GeoPrimary, RoundedCornerShape(3.dp))
        )
    }
}

@Composable
fun PlantDoctorApp(viewModel: PlantDoctorViewModel) {
    val context = LocalContext.current
    val selectedImage by viewModel.selectedImage.collectAsStateWithLifecycle()
    val scanUiState by viewModel.scanUiState.collectAsStateWithLifecycle()
    val historyList by viewModel.historyState.collectAsStateWithLifecycle()
    val activeDetailReport by viewModel.activeDetailReport.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf("scan") }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    viewModel.selectImage(bitmap)
                } else {
                    Toast.makeText(context, "Could not load selected image.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading image file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.selectImage(bitmap)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GeometricLogo()
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "CROPGUARDIAN",
                            fontWeight = FontWeight.Bold,
                            color = GeoTextDark,
                            fontSize = 20.sp,
                            letterSpacing = (-0.5).sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GeoBackground,
                    titleContentColor = GeoTextDark
                ),
                actions = {
                    if (currentTab == "archive" && historyList.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearAllHistory() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = GeoPrimary
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            GeometricBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GeoBackground)
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = currentTab,
                animationSpec = tween(300),
                label = "tab_crossfade"
            ) { tab ->
                when (tab) {
                    "scan" -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
                        ) {
                            item {
                                WorkspaceCard(
                                    selectedImage = selectedImage,
                                    scanUiState = scanUiState,
                                    onPickGallery = { galleryLauncher.launch("image/*") },
                                    onUseCamera = { cameraLauncher.launch(null) },
                                    onClearWorkspace = { viewModel.clearScanWorkspace() },
                                    onAnalyze = { viewModel.startAnalysis() }
                                )
                            }

                            if (scanUiState is ScanUiState.Success) {
                                item {
                                    Column(modifier = Modifier.padding(bottom = 20.dp)) {
                                        Text(
                                            text = "DIAGNOSIS REPORT",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GeoPrimary,
                                            letterSpacing = 1.2.sp,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        val report = (scanUiState as ScanUiState.Success).report
                                        ReportDetailCard(report = report)
                                    }
                                }
                            }
                        }
                    }

                    "archive" -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ARCHIVED SCANS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GeoPrimary,
                                        letterSpacing = 1.2.sp
                                    )
                                    Text(
                                        text = "${historyList.size} Saved",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GeoTextMuted
                                    )
                                }
                            }

                            if (historyList.isEmpty()) {
                                item {
                                    EmptyHistoryPlaceholder()
                                }
                            } else {
                                items(historyList) { historyItem ->
                                    HistoryItemCard(
                                        item = historyItem,
                                        onViewDetail = { viewModel.showHistoryDetail(historyItem) },
                                        onDelete = { viewModel.deleteHistoryId(historyItem.id) }
                                    )
                                }
                            }
                        }
                    }

                    "profile" -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
                        ) {
                            item {
                                Text(
                                    text = "USER & MODEL PROFILE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GeoPrimary,
                                    letterSpacing = 1.2.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            item {
                                ProfileInfoCard()
                            }
                            item {
                                DiagnosticModelMetaCard()
                            }
                        }
                    }
                }
            }

            if (scanUiState is ScanUiState.Loading) {
                val loadingMessage = (scanUiState as ScanUiState.Loading).message
                LoadingOverlay(message = loadingMessage)
            }

            if (activeDetailReport != null) {
                HistoricDetailDialog(
                    report = activeDetailReport!!,
                    onDismiss = { viewModel.dismissHistoryDetail() }
                )
            }
        }
    }
}

/**
 * High fidelity custom MD3 geometric interactive navigation bar.
 */
@Composable
fun GeometricBottomBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GeoBottomNav,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(80.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Tab 1: SCAN workspace
            BottomBarItem(
                label = "Scan",
                icon = { isActive ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(if (isActive) GeoTextDark else Color.Transparent)
                            .border(1.8.dp, GeoTextDark, RoundedCornerShape(2.dp))
                    )
                },
                isActive = currentTab == "scan",
                onClick = { onTabSelected("scan") }
            )

            // Tab 2: ARCHIVE history
            BottomBarItem(
                label = "Archive",
                icon = { isActive ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .border(1.8.dp, GeoTextDark, RoundedCornerShape(4.dp))
                    ) {
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .align(Alignment.Center)
                                    .background(GeoTextDark, CircleShape)
                            )
                        }
                    }
                },
                isActive = currentTab == "archive",
                onClick = { onTabSelected("archive") }
            )

            // Tab 3: PROFILE metadata info
            BottomBarItem(
                label = "Profile",
                icon = { isActive ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .border(1.8.dp, GeoTextDark, CircleShape)
                    ) {
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                    .background(GeoTextDark)
                            )
                        }
                    }
                },
                isActive = currentTab == "profile",
                onClick = { onTabSelected("profile") }
            )
        }
    }
}

@Composable
fun BottomBarItem(
    label: String,
    icon: @Composable (Boolean) -> Unit,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(
                onClick = onClick,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(if (isActive) GeoAccent else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            icon(isActive)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) GeoTextDark else GeoTextDark.copy(alpha = 0.6f),
            letterSpacing = 0.2.sp
        )
    }
}

/**
 * Image capture and upload workspace layout.
 */
@Composable
fun WorkspaceCard(
    selectedImage: Bitmap?,
    scanUiState: ScanUiState,
    onPickGallery: () -> Unit,
    onUseCamera: () -> Unit,
    onClearWorkspace: () -> Unit,
    onAnalyze: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GeoBorder, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = GeoSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Selected representation placeholder
            if (selectedImage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Image(
                        bitmap = selectedImage.asImageBitmap(),
                        contentDescription = "Selected leaf snippet",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Overlaid Scanning Geometric HUD Viewport
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(16.dp, Color.Black.copy(alpha = 0.06f))
                    ) {
                        // Central exact rounded target box
                        Box(
                            modifier = Modifier
                                .size(175.dp)
                                .align(Alignment.Center)
                                .border(1.8.dp, GeoPrimary.copy(alpha = 0.62f), RoundedCornerShape(14.dp))
                        )

                        // Top right tag
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(24.dp)
                                .background(GeoPrimary, RoundedCornerShape(100.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "LEAF DETECTED",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }

                    // Remove item picker overlay button
                    IconButton(
                        onClick = onClearWorkspace,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear upload data",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                // Empty photo selection viewport state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(GeoSecondary.copy(alpha = 0.45f), Color(0xFFF2F4EC))
                            )
                        )
                        .clickable { onPickGallery() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        // Geometric placeholder icon
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .border(1.8.dp, GeoPrimary, RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterCenterFocus,
                                contentDescription = "Scan Target Frame",
                                tint = GeoPrimary,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Awaiting Leaf Photo",
                            fontWeight = FontWeight.Bold,
                            color = GeoTextDark,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Position the infected foliage clearly inside the frame for deep learning recognition model processing.",
                            fontSize = 12.sp,
                            color = GeoTextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary interactions row
            if (selectedImage == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onUseCamera,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GeoPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Use Camera", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onPickGallery,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.5.dp, GeoPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Get/Pick File", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = onAnalyze,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GeoPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Troubleshoot,
                        contentDescription = "Diagnose",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Run Machine Learning Scan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (scanUiState is ScanUiState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ThreatLightRed),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ThreatLightRed.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Diagnostic Exception",
                            tint = ThreatDarkRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = (scanUiState as ScanUiState.Error).message,
                            color = ThreatDarkRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Highly customized Geometric Balance diagnostic result presentation panel.
 */
@Composable
fun ReportDetailCard(report: PlantScanReport) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GeoBorder, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = GeoSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Status and Detected Disease details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CONDITION DETECTED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GeoPrimary,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = report.plantName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = GeoTextDark,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = if (report.status.equals("Healthy", ignoreCase = true)) "Healthy general status" else report.diseaseName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (report.status.equals("Healthy", ignoreCase = true)) GeoPrimary else ThreatDarkRed
                    )
                }

                // Status Threat Category Badge
                val isHealthy = report.status.equals("Healthy", ignoreCase = true)
                val badgeBgColor = if (isHealthy) GeoSecondary else ThreatLightRed
                val badgeTextColor = if (isHealthy) GeoPrimary else ThreatDarkRed
                val riskLabel = if (isHealthy) "LOW THREAT" else "MODERATE RISK"

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBgColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = riskLabel,
                        color = badgeTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Diagnostic accuracy meter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Model Confidence: ",
                    fontSize = 12.sp,
                    color = GeoTextMuted,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${String.format("%.1f", report.confidence * 100)}%",
                    fontSize = 12.sp,
                    color = GeoPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Underlying Scientific Cause Folder Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GeoSecondary.copy(alpha = 0.25f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = GeoPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Primary Biological Agent",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GeoPrimary
                    )
                    Text(
                        text = report.cause,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = GeoTextDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Identified Symptoms
            GeometricBulletGroup(
                title = "Identified Symptoms",
                items = report.symptoms
            )

            Spacer(modifier = Modifier.height(20.dp))

            // TREATMENT PROTOCOL PANEL (Beautiful Solid container back)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(GeoSecondary)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(14.dp)
                            .background(GeoPrimary, RoundedCornerShape(100.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TREATMENT PROTOCOLS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GeoTextDark,
                        letterSpacing = 0.8.sp
                    )
                }

                report.pesticideRecommendations.forEach { recommendation ->
                    // Chemical strategy
                    TreatmentItem(
                        title = recommendation.chemicalName,
                        subtitle = "${recommendation.pesticideType}: ${recommendation.instructions}",
                        iconStyle = "chemical"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Organic alternative
                    TreatmentItem(
                        title = "Organic Option",
                        subtitle = recommendation.organicAlternative,
                        iconStyle = "organic"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Prevents list
            GeometricBulletGroup(
                title = "Future Prevention Best Practices",
                items = report.preventionTips
            )
        }
    }
}

@Composable
fun GeometricBulletGroup(
    title: String,
    items: List<String>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(GeoPrimary, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GeoPrimary,
                letterSpacing = 0.4.sp
            )
        }

        items.forEach { bullet ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "•",
                    color = GeoPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = bullet,
                    fontSize = 12.sp,
                    color = GeoTextMuted,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun TreatmentItem(
    title: String,
    subtitle: String,
    iconStyle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.65f))
            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconBgColor = if (iconStyle == "chemical") GeoPrimary else Color(0xFF86927C)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            if (iconStyle == "chemical") {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .border(1.8.dp, Color.White, CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .height(2.dp)
                        .background(Color.White, RoundedCornerShape(100.dp))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = GeoTextDark
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = GeoTextMuted,
                lineHeight = 15.sp
            )
        }
    }
}

/**
 * Redesigned clean history card.
 */
@Composable
fun HistoryItemCard(
    item: ScanHistoryEntity,
    onViewDetail: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetail() }
            .border(1.dp, GeoBorder, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = GeoSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GeoSecondary.copy(alpha = 0.5f))
            ) {
                if (item.imageUriPath.isNotEmpty()) {
                    AsyncImage(
                        model = item.imageUriPath,
                        contentDescription = "Archived leafy photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .border(1.5.dp, GeoPrimary, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.plantName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = GeoTextDark,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = if (item.status.equals("Healthy", ignoreCase = true)) "Healthy" else item.diseaseName,
                    fontSize = 13.sp,
                    color = if (item.status.equals("Healthy", ignoreCase = true)) GeoPrimary else ThreatDarkRed,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = getFormattedDate(item.timestamp),
                    fontSize = 10.sp,
                    color = GeoTextMuted
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete from history log",
                    tint = GeoTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyHistoryPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(GeoSecondary.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .border(2.dp, GeoPrimary, RoundedCornerShape(3.dp))
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Archive empty",
            color = GeoTextDark,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Text(
            text = "Diagnose unhealthy plants using the Scan tab. Your results database stores archives here automatically.",
            color = GeoTextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 32.dp).padding(top = 4.dp)
        )
    }
}

/**
 * Beautiful app guide profile screen component.
 */
@Composable
fun ProfileInfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GeoBorder, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = GeoSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "PLANT DOCTOR PORTFOLIO",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = GeoPrimary,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Environmental Guardian",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GeoTextDark,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Active botanical advisor profile, monitoring regional vegetation patterns using Gemini AI deep neural architecture analysis.",
                fontSize = 12.sp,
                color = GeoTextMuted,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = GeoBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProfileMetric(label = "Health Rating", value = "98%")
                ProfileMetric(label = "Botanical Class", value = "Primary")
                ProfileMetric(label = "Region", value = "Southeast")
            }
        }
    }
}

@Composable
fun DiagnosticModelMetaCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GeoBorder, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = GeoSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GeoAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .border(1.5.dp, GeoTextDark, RoundedCornerShape(1.8.dp))
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "System Metadata Specs",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GeoTextDark
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            MetaSpecRow(label = "Primary Model Core", value = "Gemini 3.5 Flash")
            MetaSpecRow(label = "Response Mime", value = "application/json")
            MetaSpecRow(label = "Temperature Parameter", value = "0.2f")
            MetaSpecRow(label = "Security Assertion", value = "HTTPS SSL Key Vault")
        }
    }
}

@Composable
fun ProfileMetric(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = GeoTextMuted, fontWeight = FontWeight.Bold)
        Text(text = value, fontSize = 15.sp, color = GeoPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MetaSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = GeoTextMuted)
        Text(text = value, fontSize = 12.sp, color = GeoTextDark, fontWeight = FontWeight.Bold)
    }
}

/**
 * Premium fullscreen loading dialog overlay.
 */
@Composable
fun LoadingOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
                .border(1.5.dp, GeoPrimary.copy(alpha = 0.35f), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = GeoSurface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Spinning geometric accent loader
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = GeoPrimary,
                        modifier = Modifier.size(52.dp),
                        strokeWidth = 3.dp
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .border(2.dp, GeoPrimary, RoundedCornerShape(3.dp))
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Running Deep Learning Model",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = GeoTextDark,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                AnimatedContent(
                    targetState = message,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "loading_text_animation"
                ) { targetMsg ->
                    Text(
                        text = targetMsg,
                        fontSize = 12.sp,
                        color = GeoTextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}



/**
 * Historic information database restore popup dialog.
 */
@Composable
fun HistoricDetailDialog(
    report: PlantScanReport,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = GeoBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Leaf Scan Info Archive",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GeoTextDark
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Return",
                                tint = GeoPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = GeoBackground
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(modifier = Modifier.padding(bottom = 24.dp)) {
                        ReportDetailCard(report = report)
                    }
                }
            }
        }
    }
}

fun getFormattedDate(milliseconds: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    return formatter.format(Date(milliseconds))
}
