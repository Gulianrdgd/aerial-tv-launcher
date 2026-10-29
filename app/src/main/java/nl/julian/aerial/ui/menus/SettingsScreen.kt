package nl.julian.aerial.ui.menus

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import nl.julian.aerial.theme.FluidEasing
import nl.julian.aerial.theme.Symbols
import nl.julian.aerial.theme.LocalSurfaces
import nl.julian.aerial.ui.Text
import nl.julian.aerial.ui.tr

/** One row in a settings category. */
sealed interface SettingItem {
    val icon: Int
    val title: String

    /** On or off, with a switch. [description] says what it does. */
    data class Toggle(
        @DrawableRes override val icon: Int,
        override val title: String,
        val checked: Boolean,
        val description: String? = null,
        val onToggle: () -> Unit,
    ) : SettingItem

    /** One of a few options; OK opens the full list with the current one marked. */
    data class Choice(
        @DrawableRes override val icon: Int,
        override val title: String,
        val options: List<String>,
        val selected: Int,
        val description: String? = null,
        val onPick: (Int) -> Unit,
    ) : SettingItem

    /** Opens something: a dialog, a sign-in, the TV's own settings. */
    data class Action(
        @DrawableRes override val icon: Int,
        override val title: String,
        val value: String? = null,
        val onClick: () -> Unit,
    ) : SettingItem
}

/** A category in the left pane, and its settings. */
data class SettingCategory(@DrawableRes val icon: Int, val name: String, val items: List<SettingItem>)

/**
 * Settings as two panes, like the TV's own settings: categories on the
 * left, the focused category's settings on the right, and the path at the
 * top ("Settings › Looks › Panels"). A choice opens the list of all its
 * options in the right pane. Back always goes one level up.
 */
@Composable
fun SettingsScreen(categories: List<SettingCategory>, onDismiss: () -> Unit) {
    var selected by remember { mutableIntStateOf(0) }
    /** The choice whose options are open in the right pane, by index in the selected category. */
    var picking by remember { mutableStateOf<Int?>(null) }
    var inRight by remember { mutableStateOf(false) }
    val left = remember(categories.size) { List(categories.size) { FocusRequester() } }
    val category = categories[selected.coerceIn(categories.indices)]
    val rows = remember(selected, category.items.size) { List(category.items.size) { FocusRequester() } }
    val pickerFirst = remember { FocusRequester() }
    var returnTo by remember { mutableStateOf<Int?>(null) }
    /**
     * True while the right pane swaps its rows for a choice's options (or back):
     * the focused row disappears for a moment and focus may pass through the
     * categories, which mustn't switch the category.
     */
    var swapping by remember { mutableStateOf(false) }

    fun toLeft() = runCatching { left[selected].requestFocus() }
    fun closePicker() {
        swapping = true
        returnTo = picking
        picking = null
    }

    Overlay(onDismiss = {
        when {
            picking != null -> closePicker()
            inRight -> toLeft()
            else -> onDismiss()
        }
    }) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp, vertical = 28.dp)
                .background(LocalSurfaces.current.lowest.copy(alpha = 0.97f), RoundedCornerShape(32.dp))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(32.dp))
                .padding(horizontal = 24.dp, vertical = 22.dp),
        ) {
            Breadcrumb(listOfNotNull("Settings", category.name, picking?.let { category.items.getOrNull(it)?.title }))
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxSize()) {
                // Left: categories. Focusing one shows its settings; OK or Right moves into them.
                Column(
                    Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .focusGroup(),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    categories.forEachIndexed { i, c ->
                        MenuRow(
                            c.icon, c.name,
                            onClick = { runCatching { rows.firstOrNull()?.requestFocus() } },
                            focusRequester = left[i],
                            modifier = Modifier
                                .onFocusChanged { if (it.isFocused && !swapping && selected != i) { selected = i; picking = null } }
                                .onPreviewKeyEvent { e ->
                                    if (e.key == Key.DirectionRight && e.type == KeyEventType.KeyDown) {
                                        runCatching { rows.firstOrNull()?.requestFocus() }
                                        true
                                    } else false
                                },
                            trailing = if (i == selected) { { SelectedMark() } } else null,
                        )
                    }
                }
                Box(Modifier.padding(horizontal = 20.dp).width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.10f)))
                // Right: the category's settings, or one choice's options. Left or Back returns to the categories.
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .onFocusChanged { inRight = it.hasFocus }
                        .onPreviewKeyEvent { e ->
                            if (e.key == Key.DirectionLeft && e.type == KeyEventType.KeyDown) {
                                if (picking != null) closePicker() else toLeft()
                                true
                            } else false
                        }
                        .focusGroup(),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    val open = picking?.let { category.items.getOrNull(it) as? SettingItem.Choice }
                    if (open != null) {
                        open.description?.let { Hint(it) }
                        open.options.forEachIndexed { o, label ->
                            MenuRow(
                                if (o == open.selected) Symbols.RadioOn else Symbols.RadioOff, label,
                                focusRequester = if (o == open.selected) pickerFirst else null,
                                onClick = {
                                    open.onPick(o)
                                    closePicker()
                                },
                            )
                        }
                        LaunchedEffect(picking) {
                            withFrameNanos { }
                            runCatching { pickerFirst.requestFocus() }
                            swapping = false
                        }
                    } else {
                        category.items.forEachIndexed { i, item -> ItemRow(item, rows[i]) { swapping = true; picking = i } }
                        LaunchedEffect(returnTo) {
                            val back = returnTo ?: return@LaunchedEffect
                            withFrameNanos { }
                            runCatching { rows[back].requestFocus() }
                            returnTo = null
                            swapping = false
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        toLeft()
    }
}

@Composable
private fun ItemRow(item: SettingItem, requester: FocusRequester, onOpenChoice: () -> Unit) {
    when (item) {
        is SettingItem.Toggle -> MenuRow(
            item.icon, item.title, value = item.description, focusRequester = requester,
            onClick = item.onToggle,
            trailing = { Switch(item.checked) },
        )
        is SettingItem.Choice -> MenuRow(
            item.icon, item.title, value = item.description, focusRequester = requester,
            onClick = onOpenChoice,
            trailing = { ValueChevron(item.options.getOrElse(item.selected) { "" }) },
        )
        is SettingItem.Action -> MenuRow(
            item.icon, item.title, value = item.value, focusRequester = requester,
            onClick = item.onClick,
            trailing = { Icon(painterResource(Symbols.Chevron), null, Modifier.size(22.dp)) },
        )
    }
}

/** "Settings › Looks › Panels": where you are, the last part brightest. */
@Composable
private fun Breadcrumb(parts: List<String>) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        parts.forEachIndexed { i, part ->
            if (i > 0) {
                Icon(
                    painterResource(Symbols.Chevron), null, tint = cs.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp).size(24.dp),
                )
            }
            Text(
                tr(part),
                style = if (i == parts.lastIndex) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                color = if (i == parts.lastIndex) cs.onSurface else cs.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
    )
}

/** The current value of a choice, then a chevron: there's a list behind it. */
@Composable
private fun ValueChevron(value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(value, style = MaterialTheme.typography.labelLarge, maxLines = 1, color = LocalContentColor.current.copy(alpha = 0.8f))
        Icon(painterResource(Symbols.Chevron), null, Modifier.padding(start = 4.dp).size(22.dp))
    }
}

/** On/off: a pill track with a knob that slides across. */
@Composable
private fun Switch(checked: Boolean) {
    val cs = MaterialTheme.colorScheme
    val knob by animateDpAsState(if (checked) 18.dp else 0.dp, tween(300, easing = FluidEasing), label = "knob")
    Box(
        Modifier
            .size(44.dp, 26.dp)
            .background(if (checked) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.35f), CircleShape)
            .padding(3.dp),
    ) {
        Box(Modifier.offset(x = knob).size(20.dp).background(if (checked) cs.onPrimary else Color.White, CircleShape))
    }
}

/** A small dot marking the category whose settings are on the right. */
@Composable
private fun SelectedMark() {
    Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
}
