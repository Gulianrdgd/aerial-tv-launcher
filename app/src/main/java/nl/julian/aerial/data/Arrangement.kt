package nl.julian.aerial.data

/** How many tiles fit in the dock (and in each grid row). */
const val COLUMNS = 6

/** Apps that land in the dock until the user pins their own. */
private val DEFAULT_DOCK = listOf(
    "com.google.android.youtube.tv",
    "com.netflix.ninja",
    "com.disney.disneyplus",
    "com.amazon.amazonvideo.livingroom",
    "com.spotify.tv.android",
    "org.jellyfin.androidtv",
    "com.plexapp.android",
    "com.android.vending",
)

/** The home screen's two app lists: the dock, and the grid below it. */
data class HomeApps(val dock: List<AppEntry>, val grid: List<AppEntry>)

/** Applies the user's dock, order and hidden set to the scanned apps. */
fun arrange(apps: List<AppEntry>, config: LauncherConfig): HomeApps {
    val visible = apps.filter { it.pkg !in config.hidden }
    val byPkg = visible.associateBy { it.pkg }
    val dock = config.dock?.mapNotNull { byPkg[it] }?.take(COLUMNS) ?: run {
        val preferred = DEFAULT_DOCK.mapNotNull { byPkg[it] }
        (preferred + visible.filter { it !in preferred }).take(COLUMNS)
    }
    val rank = config.order.withIndex().associate { (i, pkg) -> pkg to i }
    // Manually ordered apps first, in their order; the rest stay alphabetical (scan order).
    val grid = visible.filter { it !in dock }.sortedBy { rank[it.pkg] ?: Int.MAX_VALUE }
    return HomeApps(dock, grid)
}

/** [list] with [pkg] moved by [delta] places, clamped to the ends. */
fun moved(list: List<String>, pkg: String, delta: Int): List<String> {
    val from = list.indexOf(pkg)
    if (from < 0) return list
    val to = (from + delta).coerceIn(0, list.lastIndex)
    if (to == from) return list
    return list.toMutableList().apply { add(to, removeAt(from)) }
}

/** Pins [pkg] to the end of the dock; a full dock drops its last app back to the grid. */
fun pinned(dock: List<String>, pkg: String): List<String> =
    if (pkg in dock) dock else (if (dock.size >= COLUMNS) dock.dropLast(1) else dock) + pkg
