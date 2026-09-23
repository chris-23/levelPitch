package io.github.cnissler.levelpitch.ui.tutorial

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.cnissler.levelpitch.LevelPitchApp
import io.github.cnissler.levelpitch.R
import io.github.cnissler.levelpitch.ui.level.PixelArt
import io.github.cnissler.levelpitch.ui.pixel.SpriteKind
import io.github.cnissler.levelpitch.ui.pixel.bubbleOffset
import io.github.cnissler.levelpitch.ui.pixel.bullseyeLevel
import io.github.cnissler.levelpitch.ui.pixel.chockArt
import io.github.cnissler.levelpitch.ui.pixel.phonePlacementArt
import io.github.cnissler.levelpitch.ui.pixel.vehicleSprite
import io.github.cnissler.levelpitch.ui.pixel.wedgeBadge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.asin

/** One tutorial page: an illustration (animated while [Page.art] gets active = true), a title and a short text. */
private class Page(val title: Int, val body: Int, val art: @Composable (active: Boolean) -> Unit)

private val pages = listOf(
    Page(R.string.tut_welcome_title, R.string.tut_welcome_body) { AppIcon() },
    Page(R.string.tut_sensor_title, R.string.tut_sensor_body) { active -> LiveSpiritLevel(active) },
    Page(R.string.tut_setup_title, R.string.tut_setup_body) { PixelArt(vehicleSprite(SpriteKind.MOTORHOME).image, 2.8.dp) },
    Page(R.string.tut_spot_title, R.string.tut_spot_body) { PixelArt(phonePlacementArt(), 5.dp) },
    Page(R.string.tut_calibrate_title, R.string.tut_calibrate_body) { PixelArt(bullseyeLevel(0, 0), 6.dp) },
    Page(R.string.tut_level_title, R.string.tut_level_body) { active -> ClimbingWedge(active) },
    Page(R.string.tut_caravan_title, R.string.tut_caravan_body) { PixelArt(vehicleSprite(SpriteKind.CARAVAN_SINGLE).image, 2.9.dp) },
    Page(R.string.tut_safety_title, R.string.tut_safety_body) { PixelArt(chockArt(), 6.dp) },
)

/**
 * Step-by-step introduction, swipeable or with Next. Shown on first start and from the menu;
 * [onDone] runs on "Let's go" and on "Skip".
 */
@Composable
fun TutorialScreen(onDone: () -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex
    BackHandler(enabled = pager.currentPage > 0) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            HorizontalPager(pager, Modifier.weight(1f)) { i ->
                TutorialPage(pages[i], active = pager.currentPage == i)
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    if (!last) TextButton(onClick = onDone) { Text(stringResource(R.string.tut_skip)) }
                }
                PageDots(pages.size, pager.currentPage)
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    Button(onClick = {
                        if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    }) { Text(stringResource(if (last) R.string.tut_done else R.string.tut_next)) }
                }
            }
        }
    }
}

@Composable
private fun TutorialPage(page: Page, active: Boolean) {
    // Centred vertically on tall screens, scrollable on small ones or with large fonts.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) { page.art(active) }
            Text(
                stringResource(page.title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(page.body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    val description = stringResource(R.string.tut_page, current + 1, count)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.semantics { contentDescription = description }) {
        repeat(count) { i ->
            Box(
                Modifier
                    .size(if (i == current) 10.dp else 7.dp)
                    .clip(CircleShape)
                    .background(if (i == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

/** The launcher icon, large: background and foreground layers cropped to the visible 72 of 108 dp. */
@Composable
private fun AppIcon() {
    Box(Modifier.size(200.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.requiredSize(300.dp))
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(300.dp))
    }
}

/** A round spirit level driven by the accelerometer while its page is shown. */
@Composable
private fun LiveSpiritLevel(active: Boolean) {
    val source = (LocalContext.current.applicationContext as LevelPitchApp).accelerometer
    var offset by remember { mutableStateOf(0 to 0) }
    LaunchedEffect(active) {
        if (!active || !source.isAvailable) return@LaunchedEffect
        var right = Double.NaN
        var top = 0.0
        source.readings().collect { v ->
            val n = v.norm
            val r = Math.toDegrees(asin((v.x / n).coerceIn(-1.0, 1.0)))
            val t = Math.toDegrees(asin((v.y / n).coerceIn(-1.0, 1.0)))
            // Smooth the ~100 Hz readings so the bubble floats instead of jittering.
            if (right.isNaN()) {
                right = r
                top = t
            } else {
                right += 0.12 * (r - right)
                top += 0.12 * (t - top)
            }
            offset = bubbleOffset(right, top)
        }
    }
    val image = remember(offset) { bullseyeLevel(offset.first, offset.second) }
    PixelArt(image, 6.dp)
}

/** The level loop in a loop: the recommended step glows, then the tyre stands on it. */
@Composable
private fun ClimbingWedge(active: Boolean) {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(active) {
        while (active) {
            delay(900)
            frame = (frame + 1) % 4
        }
    }
    val steps = listOf(30.0, 60.0, 90.0)
    val image = remember(frame) {
        if (frame < 2) wedgeBadge(steps, current = 0, target = 2) else wedgeBadge(steps, current = 2, target = null)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PixelArt(image, 6.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(if (frame < 2) R.string.tut_level_anim_plan else R.string.tut_level_anim_done),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
