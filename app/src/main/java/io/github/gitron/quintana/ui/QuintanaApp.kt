package io.github.gitron.quintana.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gitron.quintana.R
import io.github.gitron.quintana.TimerSet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** How long a deleted set can be brought back. */
private const val UNDO_MILLIS = 5_000L

@Composable
fun QuintanaApp(viewModel: QuintanaViewModel) {
    val library by viewModel.library.collectAsStateWithLifecycle()
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val run by viewModel.run.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val deletedMessage = stringResource(R.string.deleted)
    val undoLabel = stringResource(R.string.undo)

    // The timer notification needs the permission on Android 13+. The run starts either way.
    var waitingToStart by remember { mutableStateOf<TimerSet?>(null) }
    val requestNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        waitingToStart?.let(viewModel::start)
        waitingToStart = null
    }
    val start: (TimerSet) -> Unit = { set ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            waitingToStart = set
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.start(set)
        }
    }

    val delete: (TimerSet) -> Unit = { set ->
        viewModel.delete(set)?.let { deleted ->
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                val result = withTimeoutOrNull(UNDO_MILLIS) {
                    snackbar.showSnackbar(deletedMessage, undoLabel, duration = SnackbarDuration.Indefinite)
                }
                if (result == SnackbarResult.ActionPerformed) viewModel.restore(deleted)
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Eisen),
    ) {
        val current = library
        val active = run
        val shown = screen
        when {
            // Nothing to show until the sets are loaded, which takes a moment at most.
            current == null -> Unit
            active != null -> {
                // A running timer ignores back, so it cannot be ended by accident.
                BackHandler { if (active.finished) viewModel.closeRun() }
                TimerScreen(active, onTogglePause = viewModel::togglePause, onClose = viewModel::closeRun)
            }
            shown is Screen.Edit -> {
                val back = { viewModel.show(shown.from) }
                BackHandler(onBack = back)
                val set = current.set(shown.setId)
                EditScreen(
                    initial = set,
                    otherNames = current.sets.filter { it.id != set?.id }.map { it.name },
                    onBack = back,
                    onSave = viewModel::save,
                )
            }
            shown == Screen.Settings -> {
                val back = { viewModel.show(Screen.Home) }
                BackHandler(onBack = back)
                SettingsScreen(
                    library = current,
                    onBack = back,
                    onDelete = delete,
                    onMove = viewModel::move,
                    onCreate = { viewModel.show(Screen.Edit(setId = null, from = Screen.Settings)) },
                )
            }
            else -> HomeScreen(
                library = current,
                onOpenSettings = { viewModel.show(Screen.Settings) },
                onEdit = { viewModel.show(Screen.Edit(setId = it.id, from = Screen.Home)) },
                onCreate = { viewModel.show(Screen.Edit(setId = null, from = Screen.Home)) },
                onStart = start,
                onSelect = { viewModel.select(it.id) },
            )
        }
        SnackbarHost(
            snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
        ) { data ->
            Snackbar(data, shape = RoundedCornerShape(14.dp), containerColor = Kohle, contentColor = Pergament, actionColor = Or)
        }
    }
}
