package com.tomasrepcik.voidlauncher.launcher.navigation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.KoinIsolatedContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.koinApplication
import org.koin.dsl.module

class EntryScopedViewModelTest {
    private val resources = EntryScopedViewModelResources()
    private val composeRule = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(resources)
        .around(composeRule)

    @Test
    fun givenTwoNavigationEntries_whenViewModelsAreResolved_thenEachEntryOwnsItsInstance() {
        // GIVEN
        val firstEntry = resources.firstEntry
        val secondEntry = resources.secondEntry
        lateinit var firstResolution: EntryViewModel
        lateinit var repeatedResolution: EntryViewModel
        lateinit var secondResolution: EntryViewModel
        val testKoin = resources.testKoin

        // WHEN
        composeRule.setContent {
            KoinIsolatedContext(context = testKoin) {
                CompositionLocalProvider(LocalViewModelStoreOwner provides firstEntry) {
                    firstResolution = koinViewModel()
                    repeatedResolution = koinViewModel()
                }
                CompositionLocalProvider(LocalViewModelStoreOwner provides secondEntry) {
                    secondResolution = koinViewModel()
                }
            }
        }
        composeRule.waitForIdle()

        // THEN
        assertSame(firstResolution, repeatedResolution)
        assertNotSame(firstResolution, secondResolution)
    }
}

private class EntryScopedViewModelResources : ExternalResource() {
    val firstEntry = TestEntryOwner()
    val secondEntry = TestEntryOwner()
    val testKoin = koinApplication {
        modules(module { viewModel { EntryViewModel() } })
    }

    override fun after() {
        firstEntry.viewModelStore.clear()
        secondEntry.viewModelStore.clear()
        testKoin.close()
    }
}

private class EntryViewModel : ViewModel()

private class TestEntryOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}
