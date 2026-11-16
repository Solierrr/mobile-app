package com.project.solaria_mobile.feature.project

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.designsystem.components.RemoteImage
import com.project.solaria_mobile.feature.project.domain.ProjectDetailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first

@HiltViewModel
class ProjectImageViewModel @Inject constructor(private val repository: ProjectDetailRepository) : ViewModel() {
    suspend fun cover(id: String): ByteArray? {
        val photos = repository.observePhotos(id).first().result as? ApiResult.Success ?: return null
        val photo = photos.data.firstOrNull { it.cover } ?: photos.data.firstOrNull() ?: return null
        return (repository.photoContent(id, photo.id) as? ApiResult.Success)?.data
    }
}

@Composable
fun ProjectCardImage(projectId: String, modifier: Modifier, contentScale: ContentScale = ContentScale.Crop, viewModel: ProjectImageViewModel = hiltViewModel()) {
    RemoteImage(projectId, viewModel::cover, null, modifier, contentScale = contentScale)
}
