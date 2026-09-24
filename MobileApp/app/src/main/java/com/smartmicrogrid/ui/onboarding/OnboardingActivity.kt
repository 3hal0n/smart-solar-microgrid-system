// ============================================================
// File: OnboardingActivity.kt
// Purpose: First-run-only explainer (3-slide swipeable pager) shown
//          once before the user reaches MainActivity. Marks itself
//          as seen via OnboardingPreferences so SplashActivity skips
//          straight past it on every later launch. Pure UI, and kept
//          general/app-level — no prosumer- or operator-specific
//          screens, since those belong to Dinil's ui/prosumer and
//          Migara's ui/operator packages, not this shared flow.
// Author: Shalon
// ============================================================
package com.smartmicrogrid.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartmicrogrid.MainActivity
import com.smartmicrogrid.ui.theme.SmartMicrogridTheme
import kotlinx.coroutines.launch

// One onboarding slide's copy.
private data class OnboardingSlide(val title: String, val body: String)

private val SLIDES = listOf(
    OnboardingSlide(
        title = "Find microgrid hubs near you",
        body = "Browse solar energy hubs on the map, each with live battery slot availability.",
    ),
    OnboardingSlide(
        title = "Reserve an energy slot",
        body = "Book a charging or discharging slot in a few taps, within a simple 7-day window.",
    ),
    OnboardingSlide(
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

// Renders the slide pager, dot indicator, and Skip/Next/Get-started controls.
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
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onFinished) {
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
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Text(if (isLastPage) "Get started" else "Next")
            }
        }
    }
}

// Renders a single slide's title and body copy, centered. Scrollable so long body copy on a
// short/landscape screen clips into a scroll instead of overflowing off-screen.
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
        Text(
            text = slide.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = slide.body,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

// Renders a row of dots marking pager progress, with the current page's dot highlighted.
@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isSelected) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        },
                    ),
            )
        }
    }
}
