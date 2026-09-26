// ============================================================
// File: OnboardingActivity.kt
// Purpose: First-run-only explainer (3-slide swipeable pager) shown
//          once before the user reaches MainActivity. Full-bleed
//          photo backgrounds (solar/rooftop imagery) with a gradient
//          scrim on the first two slides, the Joule brand gradient on
//          the third — colorful, immersive treatment rather than
//          plain-white-with-icon, per 2026-09-26 direction ("more
//          colorful, great UI/UX, not full enterprise"). Marks itself
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
import androidx.compose.foundation.Image
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartmicrogrid.R
import com.smartmicrogrid.MainActivity
import com.smartmicrogrid.ui.components.JouleMark
import com.smartmicrogrid.ui.theme.SmartMicrogridTheme
import com.smartmicrogrid.ui.theme.StripeInk
import kotlinx.coroutines.launch

// One onboarding slide's background photo (null = brand-gradient background instead) and copy.
private data class OnboardingSlide(val imageRes: Int?, val eyebrow: String, val title: String, val body: String)

private val SLIDES = listOf(
    OnboardingSlide(
        imageRes = R.drawable.onboarding_solar,
        eyebrow = "Discover",
        title = "Find microgrid hubs near you",
        body = "Browse solar energy hubs on the map, each with live battery slot availability.",
    ),
    OnboardingSlide(
        imageRes = R.drawable.onboarding_battery,
        eyebrow = "Reserve",
        title = "Reserve an energy slot",
        body = "Book a charging or discharging slot in a few taps, within a simple 7-day window.",
    ),
    OnboardingSlide(
        imageRes = null,
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
            OnboardingScreen(onFinished = ::finishOnboarding)
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

// Renders the full-bleed background for the active slide, the brand row + Skip, the slide pager,
// the pill indicator, and Next/Get started — all overlaid on top of that background rather than
// confined to a plain surface, so the photo/gradient reaches every edge of the screen. Still uses
// SmartMicrogridTheme for the shared type scale, but every color on this screen is set explicitly
// (white text, ink button content) rather than theme-relative — this screen is intentionally
// always dark-on-photo regardless of the device's light/dark setting.
@Composable
private fun OnboardingScreen(onFinished: () -> Unit) {
  SmartMicrogridTheme {
    val pagerState = rememberPagerState(pageCount = { SLIDES.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == SLIDES.lastIndex

    Box(modifier = Modifier.fillMaxSize()) {
        SlideBackground(slide = SLIDES[pagerState.currentPage])

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                JouleMark(modifier = Modifier.width(26.dp), monochrome = Color.White)
                Text(
                    text = "Joule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f),
                )
                TextButton(onClick = onFinished, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                    Text("Skip")
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
                    .padding(bottom = 20.dp),
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
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = StripeInk),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp)
                    .height(52.dp),
            ) {
                Text(if (isLastPage) "Get started" else "Next", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
  }
}

// The active slide's full-screen backdrop: a photo with a bottom-heavy dark scrim (so white text
// stays legible without hiding the image), or the Joule brand gradient when there's no photo.
@Composable
private fun SlideBackground(slide: OnboardingSlide) {
    if (slide.imageRes != null) {
        Image(
            painter = painterResource(slide.imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to StripeInk.copy(alpha = 0.35f),
                        0.55f to StripeInk.copy(alpha = 0.75f),
                        1f to StripeInk.copy(alpha = 0.96f),
                    ),
                ),
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(Color(0xFF533AFD), Color(0xFF2E2B8C), Color(0xFF11EFE3)))),
        )
    }
}

// Renders a single slide's eyebrow, title and body, anchored toward the bottom of the pager area
// (over the scrim) rather than centered — a Stories-style layout that keeps the photo's upper
// two-thirds uncluttered. Scrollable so long body copy on a short/landscape screen clips into a
// scroll instead of overflowing off-screen.
@Composable
private fun OnboardingSlideContent(slide: OnboardingSlide) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = slide.eyebrow.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF9AF2EC),
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Text(
            text = slide.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Start,
        )
        Text(
            text = slide.body,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = 10.dp, bottom = 24.dp),
        )
    }
}

// Renders pager progress: the current page is an elongated pill, the rest are small dots.
@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width by animateDpAsState(targetValue = if (isSelected) 22.dp else 6.dp, label = "indicatorWidth")
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .height(6.dp)
                    .width(width)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.35f)),
            )
        }
    }
}
