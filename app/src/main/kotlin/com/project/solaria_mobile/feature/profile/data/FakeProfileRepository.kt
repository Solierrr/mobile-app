package com.project.solaria_mobile.feature.profile.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class FakeProfileRepository @Inject constructor() : ProfileRepository {
    override val ownProfile = MutableStateFlow(UserProfile("RC", "Ronaldo Chagas", "@ronaldoa02349993", "São Paulo", "013110–200", "55+ (11) 91022–3438"))
    override val otherProfile = MutableStateFlow(UserProfile("RN", "Rodrigo Nolabas", "@rodrigaoobatechagaas", "São Paulo", "Engenheiro Elétrico", "55+ (11) 91022–3438"))
    override val connections = MutableStateFlow(
        listOf(
            Connection("EV", "Everson", "@eozoiogarai"),
            Connection("BB", "Bruce Banner", "@hulksmash"),
            Connection("SU", "Sasuke Uchiha", "@konohabush"),
            Connection("AN", "Ana Silva", "@anasilva"),
            Connection("ML", "Mariana Lima", "@marianalima"),
            Connection("FS", "Felipe Santos", "@felipesantos"),
        )
    )
    private val mutableConnected = MutableStateFlow(false)
    override val connected: StateFlow<Boolean> = mutableConnected
    override fun toggleConnection() { mutableConnected.value = !mutableConnected.value }
}
