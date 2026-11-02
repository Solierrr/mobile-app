package com.project.solaria_mobile.feature.discovery.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.project.solaria_mobile.feature.discovery.domain.CalendarLinkRepository
import com.project.solaria_mobile.feature.discovery.domain.DiscoveryRepository
import com.project.solaria_mobile.feature.discovery.domain.WorkJourneyRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class DiscoveryRepositoryModule {
    @Binds abstract fun bindRepository(repository: RemoteDiscoveryRepository): DiscoveryRepository
    @Binds abstract fun bindJourney(repository: RemoteWorkJourneyRepository): WorkJourneyRepository
    @Binds abstract fun bindCalendarLink(repository: RemoteCalendarLinkRepository): CalendarLinkRepository
}
