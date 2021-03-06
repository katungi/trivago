package com.trivago.ui

import android.view.View
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.agoda.kakao.recycler.KRecyclerItem
import com.agoda.kakao.recycler.KRecyclerView
import com.agoda.kakao.screen.Screen
import com.agoda.kakao.screen.Screen.Companion.idle
import com.agoda.kakao.screen.Screen.Companion.onScreen
import com.agoda.kakao.text.KTextView
import com.trivago.R
import com.trivago.core.settings.Settings
import com.trivago.core.utils.TrivagoSharedPreferenceLiveData
import com.trivago.data.character
import com.trivago.data.repository.CharacterSearchRepository
import com.trivago.data.starWarsCharacter
import com.trivago.data.starWarsCharacterTwo
import com.trivago.ui.adapter.CharactersRecyclerViewAdapter
import com.trivago.ui.viewmodel.CharacterSearchViewModel
import com.trivago.ui.viewmodel.ThemeViewModel
import com.trivago.ui.views.CharacterSearchActivity
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import org.koin.test.inject
import org.koin.test.mock.declare

@RunWith(AndroidJUnit4::class)
class CharacterSearchActivityTest : KoinTest {

    private val context =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val themeViewModel: ThemeViewModel by inject()

    private val characterSearchRepository = mockk<CharacterSearchRepository>()

    @Before
    fun setup() {
        declare { mockk<ThemeViewModel>() }
        declare {
            CharacterSearchViewModel(characterSearchRepository)
        }
    }

    @Test
    fun testBlankPlaceholder_isDisplayed_whenNoCharacters_haveBeenSearched() {
        every { themeViewModel.getAppTheme() } returns TrivagoSharedPreferenceLiveData(
            sharedPreferences,
            Settings.PREFERENCE_THEME_KEY,
            "Light"
        )
        every { characterSearchRepository.getCharacters() } returns flowOf(emptyList())

        ActivityScenario.launch(CharacterSearchActivity::class.java)

        onScreen<CharacterSearchScreen> {
            noCharacters.isDisplayed()
        }
        idle(3000)
    }

    @Test
    fun testBlankPlaceholder_isHidden_and_listIsPopulated() {
        every { themeViewModel.getAppTheme() } returns TrivagoSharedPreferenceLiveData(
            sharedPreferences,
            Settings.PREFERENCE_THEME_KEY,
            "Light"
        )

        every { characterSearchRepository.getCharacters() } returns flowOf(listOf(character))

        ActivityScenario.launch(CharacterSearchActivity::class.java)

        onScreen<CharacterSearchScreen> {
            noCharacters.isNotDisplayed()

            charactersList {
                firstChild<CharacterItem> {
                    isDisplayed()
                    name {
                        hasText("Test")
                    }

                    description {
                        hasText("2020")
                    }
                }
            }
        }
        idle(3000)
    }

    @Test
    fun testThat_listIsNotPopulatedTwice() {
        every { themeViewModel.getAppTheme() } returns TrivagoSharedPreferenceLiveData(
            sharedPreferences,
            Settings.PREFERENCE_THEME_KEY,
            "Light"
        )

        every { characterSearchRepository.getCharacters() } returns flowOf(listOf(character))

        val scenario = ActivityScenario.launch(CharacterSearchActivity::class.java)

        scenario.onActivity {
            val recyclerView = it.findViewById<RecyclerView>(R.id.recyclerViewCharacters)
            assertThat(recyclerView.adapter?.itemCount, `is`(1))

            val adapter = recyclerView.adapter as CharactersRecyclerViewAdapter
            adapter.submitList(mutableListOf(starWarsCharacter))

            assertThat(recyclerView.adapter?.itemCount, `is`(1))
        }

        onScreen<CharacterSearchScreen> {
            charactersList {
                firstChild<CharacterItem> {
                    isDisplayed()
                    name {
                        hasText("Test")
                    }

                    description {
                        hasText("2020")
                    }
                }
            }
        }

        scenario.onActivity {
            val recyclerView = it.findViewById<RecyclerView>(R.id.recyclerViewCharacters)
            val adapter = recyclerView.adapter as CharactersRecyclerViewAdapter
            adapter.submitList(mutableListOf(starWarsCharacterTwo))
            assertThat(recyclerView.adapter?.itemCount, `is`(1))
        }

        onScreen<CharacterSearchScreen> {
            charactersList {
                firstChild<CharacterItem> {
                    isDisplayed()
                    name {
                        hasText("Character Two")
                    }

                    description {
                        hasText("2020")
                    }
                }
            }
        }
        idle(3000)
    }
}

class CharacterSearchScreen : Screen<CharacterSearchScreen>() {
    val noCharacters = KTextView { withId(R.id.noCharacters) }
    val charactersList = KRecyclerView(
        { withId(R.id.recyclerViewCharacters) },
        itemTypeBuilder = {
            itemType(::CharacterItem)
        }
    )
}

class CharacterItem(parent: Matcher<View>) : KRecyclerItem<CharacterItem>(parent) {
    val name: KTextView = KTextView(parent) { withId(R.id.textViewCharacterName) }
    val description: KTextView = KTextView(parent) { withId(R.id.textViewCharacterDescription) }
}