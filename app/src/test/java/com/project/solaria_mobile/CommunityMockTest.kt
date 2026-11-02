package com.project.solaria_mobile

import com.project.solaria_mobile.core.mock.MockApiData
import com.project.solaria_mobile.feature.communication.data.CommunityCreateRequest
import com.project.solaria_mobile.feature.communication.data.CommunityGroupRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityMockTest {
    @Test fun mockEnabledWithRealSessionKeepsActorOwnershipAndGroupParticipation() {
        val data = MockApiData()
        val actor = "real-session-subject"
        val roster = data.membersForActor("qa-project-1", actor)
        assertTrue(roster.any { it.authId == actor })
        val community = data.createCommunity(CommunityCreateRequest("qa-project-1", roster.mapNotNull { it.authId }.toSet()), actor)
        val shown = data.communityForActor(community, actor)
        assertEquals("OWNER", shown.members.first { it.userId == actor }.role)
        assertFalse(shown.members.any { it.userId == "qa-mock-user" })
        val group = data.createCommunityGroup(community.id, CommunityGroupRequest("Geral"), actor)
        assertNotNull(group)
        assertNotNull(data.conversation(requireNotNull(group).conversation.id))
        assertTrue(data.communityGroups(community.id, actor).single().participantIds.contains(actor))
        assertTrue(data.communityGroups("other-project", actor).isEmpty())
    }
}
