// ============================================================
// File: OnboardingActivity.kt
// Purpose: First-run-only explainer (3-slide swipeable pager) shown
//          once before the user reaches MainActivity. Marks itself
//          as seen via OnboardingPreferences so SplashActivity skips
//          straight past it on every later launch. Pure UI, and kept
//          general/app-level — no prosumer- or operator-specific
//          screens, since those belong to their owners' packages,
//          not this shared flow.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartmicrogrid.MainActivity
import com.smartmicrogrid.ui.components.IconTile
import com.smartmicrogrid.ui.components.JouleIcons
import com.smartmicrogrid.ui.components.JouleMark
import com.smartmicrogrid.ui.theme.SmartMicrogridTheme
import kotlinx.coroutines.launch

// One onboarding slide's icon and copy.
private data class OnboardingSlide(val icon: ImageVector, val eyebrow: String, val title: String, val body: String)

private val SLIDES = listOf(
    OnboardingSlide(
        icon = JouleIcons.MapPin,
        eyebrow = "Discover",
        title = "Find microgrid hubs near you",
        body = "Browse solar energy hubs on the map, each with live battery slot availability.",
    ),
    OnboardingSlide(
        icon = JouleIcons.Battery,
        eyebrow = "Reserve",
        title = "Reserve an energy slot",
        body = "Book a charging or discharging slot in a few taps, within a simple 7-day window.",
    ),
    OnboardingSlide(
        icon = JouleIcons.Pulse,
        eyebrow = "Track",
        title = "Track every transfer",
        body = "Get a QR code for each booking and follow your energy transfer history from your dashboard.",
    ),
)

class OnboardingActivity : ComponentActivity() {

    // Shows the swipeable slide flow; both Skip and "Get started" mark onboarding as shown.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartMicrogridTheme {
                OnboardingScreen(onFinished = ::finishOnboarding)
            }
        }
    }

    // Persists that onboarding has been shown, then routes into the app and clears this
    // activity (and Splash) from the back stack.
    private fun finishOnboarding() {
        OnboardingPreferences.markOnboardingShown(this)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

// Renders the brand row + Skip, the slide pager, the pill indicator, and Next/Get started.
@Composable
private fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { SLIDES.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == SLIDES.lastIndex

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // safeDrawingPadding() keeps Skip/Next clear of the status bar and gesture nav bar under
        // enableEdgeToEdge() — this screen has no Scaffold (which would apply that automatically).
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                JouleMark(modifier = Modifier.width(28.dp))
                Text(
                    text = "Joule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f),
                )
                TextButton(onClick = onFinished) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                OnboardingSlideContent(SLIDES[page])
            }

            PageIndicator(
                pageCount = SLIDES.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            )

            Button(
                onClick = {
                    if (isLastPage) {
                        onFinished()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
                    .height(52.dp),
            ) {
                Text(if (isLastPage) "Get started" else "Next", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// Renders a single slide: an icon tile, eyebrow label, title and body. Scrollable so long body
// copy on a short/landscape screen clips into a scroll instead of overflowing off-screen.
@Composable
private fun OnboardingSlideContent(slide: OnboardingSlide) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconTile(icon = slide.icon, size = 72.dp)
        Text(
            text = slide.eyebrow.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 28.dp),
        )
        Text(
            text = slide.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = slide.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

// Renders pager progress: the current page is an elongated pill, the rest are small dots.
@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width by animateDpAsState(targetValue = if (isSelected) 22.dp else 6.dp, label = "indicatorWidth")
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .height(6.dp)
                    .width(width)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            )
        }
    }
}
