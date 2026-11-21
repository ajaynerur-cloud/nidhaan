package com.nidhaan.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.nidhaan.app.data.AppDatabase
import com.nidhaan.app.data.ImageStore
import com.nidhaan.app.data.Prefs
import com.nidhaan.app.ui.AllItemsScreen
import com.nidhaan.app.ui.BackupScreen
import com.nidhaan.app.ui.ContainerDetailScreen
import com.nidhaan.app.ui.ContainerEditScreen
import com.nidhaan.app.ui.ItemDetailScreen
import com.nidhaan.app.ui.ItemEditScreen
import com.nidhaan.app.ui.LocationsScreen
import com.nidhaan.app.ui.NidhaanTheme
import com.nidhaan.app.ui.PermissionsScreen
import com.nidhaan.app.ui.PlaceDetailScreen
import com.nidhaan.app.ui.PlaceEditScreen
import com.nidhaan.app.ui.WelcomeScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Graph {
    lateinit var app: Application
    lateinit var db: AppDatabase
    lateinit var prefs: Prefs

    fun init(a: Application) {
        app = a
        db = Room.databaseBuilder(a, AppDatabase::class.java, "nidhaan.db").build()
        prefs = Prefs(a)
    }
}

class NidhaanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
        // remove photo files left behind by cancelled forms
        CoroutineScope(Dispatchers.IO).launch { ImageStore.cleanOrphans() }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NidhaanTheme { AppNav() }
        }
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val start = if (Graph.prefs.onboarded) "places" else "welcome"

    val onTab: (String) -> Unit = { route ->
        nav.navigate(route) {
            popUpTo("places") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val goHomeFromWelcome: () -> Unit = {
        Graph.prefs.onboarded = true
        nav.navigate("places") { popUpTo("welcome") { inclusive = true } }
    }

    NavHost(navController = nav, startDestination = start) {

        composable("welcome") {
            WelcomeScreen(onStartFresh = goHomeFromWelcome, onRestored = goHomeFromWelcome)
        }

        composable("places") {
            LocationsScreen(
                onTab = onTab,
                onOpenPlace = { nav.navigate("place/$it") },
                onAddPlace = { nav.navigate("placeEdit/0") },
                onOpenItem = { nav.navigate("item/$it") },
                onPermissions = { nav.navigate("permissions") }
            )
        }
        composable("allitems") {
            AllItemsScreen(onTab = onTab, onOpenItem = { nav.navigate("item/$it") })
        }
        composable("backup") {
            BackupScreen(onTab = onTab)
        }
        composable("permissions") {
            PermissionsScreen(onBack = { nav.popBackStack() })
        }

        composable("place/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
            val id = it.arguments!!.getLong("id")
            PlaceDetailScreen(
                placeId = id,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate("placeEdit/$id") },
                onOpenContainer = { cid -> nav.navigate("container/$cid") },
                onAddContainer = { nav.navigate("containerEdit/$id/0") }
            )
        }
        composable("placeEdit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
            val id = it.arguments!!.getLong("id")
            PlaceEditScreen(
                placeId = id,
                onBack = { nav.popBackStack() },
                onSaved = { savedId, isNew ->
                    nav.popBackStack()
                    if (isNew) nav.navigate("place/$savedId")
                }
            )
        }

        composable("container/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
            val id = it.arguments!!.getLong("id")
            ContainerDetailScreen(
                containerId = id,
                onBack = { nav.popBackStack() },
                onEdit = { placeId -> nav.navigate("containerEdit/$placeId/$id") },
                onOpenItem = { iid -> nav.navigate("item/$iid") },
                onAddItem = { nav.navigate("itemEdit/$id/0") }
            )
        }
        composable(
            "containerEdit/{placeId}/{id}",
            arguments = listOf(
                navArgument("placeId") { type = NavType.LongType },
                navArgument("id") { type = NavType.LongType }
            )
        ) {
            val placeId = it.arguments!!.getLong("placeId")
            val id = it.arguments!!.getLong("id")
            ContainerEditScreen(
                placeId = placeId,
                containerId = id,
                onBack = { nav.popBackStack() },
                onSaved = { savedId, isNew ->
                    nav.popBackStack()
                    if (isNew) nav.navigate("container/$savedId")
                }
            )
        }

        composable("item/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
            val id = it.arguments!!.getLong("id")
            ItemDetailScreen(
                itemId = id,
                onBack = { nav.popBackStack() },
                onEdit = { cid -> nav.navigate("itemEdit/$cid/$id") },
                onOpenPlace = { pid -> nav.navigate("place/$pid") },
                onOpenContainer = { cid -> nav.navigate("container/$cid") }
            )
        }
        composable(
            "itemEdit/{containerId}/{id}",
            arguments = listOf(
                navArgument("containerId") { type = NavType.LongType },
                navArgument("id") { type = NavType.LongType }
            )
        ) {
            val containerId = it.arguments!!.getLong("containerId")
            val id = it.arguments!!.getLong("id")
            ItemEditScreen(
                containerId = containerId,
                itemId = id,
                onBack = { nav.popBackStack() },
                onDone = { nav.popBackStack() }
            )
        }
    }
}
