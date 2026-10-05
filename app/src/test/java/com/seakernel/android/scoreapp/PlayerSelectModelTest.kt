package com.seakernel.android.scoreapp

import com.seakernel.android.scoreapp.data.Player
import com.seakernel.android.scoreapp.playerselect.CreateModel
import com.seakernel.android.scoreapp.playerselect.PlayerNameChanged
import com.seakernel.android.scoreapp.playerselect.PlayerSearchRequest
import com.seakernel.android.scoreapp.playerselect.PlayersLoaded
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerSelectModelTest {

    private val players = listOf(
        Player(id = 1, name = "alice"),
        Player(id = 2, name = "Bob"),
        Player(id = 3, name = "carol"),
    )

    private fun loaded(): CreateModel =
        CreateModel.update(CreateModel.createDefault(null), PlayersLoaded(players)).modelUnsafe()

    private fun CreateModel.names() = allPlayers.map { it.name }

    @Test
    fun newPlayerSortingLastIsAddedAtTheEnd() {
        val model = CreateModel.update(loaded(), PlayerNameChanged(4, "zed")).modelUnsafe()

        assertEquals(listOf("alice", "Bob", "carol", "zed"), model.names())
        assertEquals(model.allPlayers, model.filteredPlayerList)
        assertEquals(listOf(4L), model.selectedPlayerList)
    }

    @Test
    fun newPlayerIsSortedIgnoringCase() {
        val model = CreateModel.update(loaded(), PlayerNameChanged(4, "Beth")).modelUnsafe()

        assertEquals(listOf("alice", "Beth", "Bob", "carol"), model.names())
    }

    @Test
    fun renamedPlayerMovesToItsNewPlace() {
        val model = CreateModel.update(loaded(), PlayerNameChanged(1, "dave")).modelUnsafe()

        assertEquals(listOf("Bob", "carol", "dave"), model.names())
        assertEquals(model.allPlayers, model.filteredPlayerList)
        assertEquals(emptyList<Long>(), model.selectedPlayerList)
    }

    @Test
    fun searchResultsKeepTheSortOrder() {
        val searched = CreateModel.update(loaded(), PlayerSearchRequest("A")).modelUnsafe()
        val model = CreateModel.update(searched, PlayerNameChanged(4, "Aaron")).modelUnsafe()

        assertEquals(listOf("Aaron", "alice", "carol"), model.filteredPlayerList.map { it.name })

        val searchedAgain = CreateModel.update(model, PlayerSearchRequest("a")).modelUnsafe()
        assertEquals(listOf("Aaron", "alice", "carol"), searchedAgain.filteredPlayerList.map { it.name })
    }
}
