package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.ui.screens.AddSubjectDialog
import com.example.ui.screens.HomeNotesScreen
import com.example.ui.screens.NoteWorkspaceScreen
import com.example.ui.screens.ReviewerScreen
import com.example.ui.screens.SubjectsScreen
import kotlinx.coroutines.launch

object EduFlowRoutes {
    const val ROUTE_HOME = "home"
    const val ROUTE_REVIEWER = "reviewer"
    const val ROUTE_SUBJECTS = "subjects"
    const val ROUTE_WORKSPACE = "workspace/{noteId}"

    fun workspace(noteId: Long): String = "workspace/$noteId"
}

private data class TopDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TopDestinations = listOf(
    TopDestination(
        route = EduFlowRoutes.ROUTE_HOME,
        labelRes = R.string.nav_notes,
        selectedIcon = Icons.Rounded.AutoStories,
        unselectedIcon = Icons.Outlined.AutoStories,
        testTag = "nav_tab_notes"
    ),
    TopDestination(
        route = EduFlowRoutes.ROUTE_REVIEWER,
        labelRes = R.string.nav_reviewer,
        selectedIcon = Icons.Rounded.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook,
        testTag = "nav_tab_reviewer"
    ),
    TopDestination(
        route = EduFlowRoutes.ROUTE_SUBJECTS,
        labelRes = R.string.nav_subjects,
        selectedIcon = Icons.Rounded.FolderCopy,
        unselectedIcon = Icons.Outlined.FolderCopy,
        testTag = "nav_tab_subjects"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduFlowApp(
    viewModel: EduFlowViewModel = viewModel()
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
    val filteredNotes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val allDefinitions by viewModel.allDefinitions.collectAsStateWithLifecycle()
    val selectedHomeSubjectId by viewModel.selectedHomeSubjectId.collectAsStateWithLifecycle()
    val selectedReviewerSubjectId by viewModel.selectedReviewerSubjectId.collectAsStateWithLifecycle()
    val aiTaskState by viewModel.aiTaskState.collectAsStateWithLifecycle()
    val recapState by viewModel.recapState.collectAsStateWithLifecycle()

    var showCaptureSheet by remember { mutableStateOf(false) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showTopLevelChrome = currentRoute in TopDestinations.map { it.route }

    val navigateToWorkspace: (Long) -> Unit = { noteId ->
        navController.navigate(EduFlowRoutes.workspace(noteId))
    }

    // Photo Picker (Zero permissions required)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importPhotoAndOpenNote(uri, onCreated = navigateToWorkspace)
        }
    }

    // Camera Capture
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = viewModel.consumePendingCameraUri()
        if (success && uri != null) {
            viewModel.importPhotoAndOpenNote(uri, onCreated = navigateToWorkspace)
        }
    }

    val cameraPermissionDeniedMsg = stringResource(R.string.error_camera_permission)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = viewModel.prepareCameraUri()
            takePictureLauncher.launch(uri)
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(cameraPermissionDeniedMsg)
            }
        }
    }

    val launchCameraScan: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val uri = viewModel.prepareCameraUri()
            takePictureLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val launchPhotoPicker: () -> Unit = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (isWideScreen && showTopLevelChrome) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    FloatingActionButton(
                        onClick = { showCaptureSheet = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("rail_scan_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = "Add or scan note"
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    TopDestinations.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(EduFlowRoutes.ROUTE_HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = stringResource(dest.labelRes)
                                )
                            },
                            label = { Text(stringResource(dest.labelRes)) },
                            modifier = Modifier.testTag(dest.testTag)
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    if (showTopLevelChrome) {
                        TopAppBar(
                            title = {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
                },
                floatingActionButton = {
                    if (!isWideScreen && showTopLevelChrome) {
                        FloatingActionButton(
                            onClick = { showCaptureSheet = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.testTag("home_add_note_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = "Scan or add study note"
                            )
                        }
                    }
                },
                bottomBar = {
                    if (!isWideScreen && showTopLevelChrome) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            TopDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(dest.route) {
                                            popUpTo(EduFlowRoutes.ROUTE_HOME) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag(dest.testTag),
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = EduFlowRoutes.ROUTE_HOME,
                    modifier = Modifier.padding(if (showTopLevelChrome) innerPadding else androidx.compose.foundation.layout.PaddingValues(0.dp))
                ) {
                    composable(EduFlowRoutes.ROUTE_HOME) {
                        HomeNotesScreen(
                            subjects = subjects,
                            notes = filteredNotes,
                            allDefinitions = allDefinitions,
                            selectedSubjectId = selectedHomeSubjectId,
                            onSelectSubjectFilter = viewModel::selectHomeSubjectFilter,
                            onScanWithCamera = launchCameraScan,
                            onPickPhoto = launchPhotoPicker,
                            onCreateBlankNote = {
                                viewModel.createBlankNoteAndOpen(navigateToWorkspace)
                            },
                            onOpenNote = navigateToWorkspace,
                            onAddSubjectClick = { showAddSubjectDialog = true }
                        )
                    }

                    composable(EduFlowRoutes.ROUTE_REVIEWER) {
                        ReviewerScreen(
                            subjects = subjects,
                            notes = allNotes,
                            definitions = allDefinitions,
                            selectedSubjectId = selectedReviewerSubjectId,
                            recapState = recapState,
                            onSelectSubject = viewModel::selectReviewerSubject,
                            onOpenNote = navigateToWorkspace,
                            onGenerateRecap = viewModel::generateSubjectRecap
                        )
                    }

                    composable(EduFlowRoutes.ROUTE_SUBJECTS) {
                        SubjectsScreen(
                            subjects = subjects,
                            notes = allNotes,
                            definitions = allDefinitions,
                            onOpenSubjectReviewer = { subjectId ->
                                viewModel.selectReviewerSubject(subjectId)
                                navController.navigate(EduFlowRoutes.ROUTE_REVIEWER) {
                                    popUpTo(EduFlowRoutes.ROUTE_HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onAddSubjectClick = { showAddSubjectDialog = true },
                            onDeleteSubject = viewModel::deleteSubject
                        )
                    }

                    composable(
                        route = EduFlowRoutes.ROUTE_WORKSPACE,
                        arguments = listOf(
                            navArgument("noteId") { type = NavType.LongType }
                        )
                    ) { backStackEntry ->
                        val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
                        val note by viewModel.observeNote(noteId)
                            .collectAsStateWithLifecycle(initialValue = null)
                        val noteDefinitions by viewModel.observeDefinitionsForNote(noteId)
                            .collectAsStateWithLifecycle(initialValue = emptyList())

                        NoteWorkspaceScreen(
                            note = note,
                            subjects = subjects,
                            definitions = noteDefinitions,
                            aiTaskState = aiTaskState,
                            onBack = { navController.popBackStack() },
                            onSaveNote = viewModel::saveNoteEdits,
                            onExtractWithAi = viewModel::extractFromPhoto,
                            onAutoDetectDefinitions = viewModel::autoDetectDefinitions,
                            onClearAiState = viewModel::clearAiTaskState,
                            onSaveDefinition = viewModel::saveDefinition,
                            onDeleteDefinition = viewModel::deleteDefinition,
                            onDeleteNote = { targetNote ->
                                viewModel.deleteNote(targetNote) {
                                    navController.popBackStack()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCaptureSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCaptureSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = "Add to EduFlow Notes",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                ListItem(
                    headlineContent = { Text("Scan Page with Camera") },
                    supportingContent = { Text("Snap a printed textbook page or neat handwriting") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .clickable {
                            showCaptureSheet = false
                            launchCameraScan()
                        }
                        .testTag("sheet_scan_camera")
                )
                ListItem(
                    headlineContent = { Text("Upload Photo from Gallery") },
                    supportingContent = { Text("Pick a saved note photo and extract editable text") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .clickable {
                            showCaptureSheet = false
                            launchPhotoPicker()
                        }
                        .testTag("sheet_pick_photo")
                )
                ListItem(
                    headlineContent = { Text("Write a Typed Note") },
                    supportingContent = { Text("Type or paste class research and highlight key terms") },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .clickable {
                            showCaptureSheet = false
                            viewModel.createBlankNoteAndOpen(navigateToWorkspace)
                        }
                        .testTag("sheet_blank_note")
                )
            }
        }
    }

    if (showAddSubjectDialog) {
        AddSubjectDialog(
            onDismiss = { showAddSubjectDialog = false },
            onCreateSubject = viewModel::addSubject
        )
    }
}
