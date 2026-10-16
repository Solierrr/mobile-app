package com.project.solaria_mobile.core.designsystem.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

private const val HEXAGON_VERTICES = 6
private const val PENTAGON_VERTICES = 5
private const val CLOVER_LEAVES = 4
private const val COOKIE_LOBES = 9
private const val BURST_POINTS = 12
private const val SUN_RAYS = 8

object MorphShapes {
    val hexagon: RoundedPolygon = RoundedPolygon(HEXAGON_VERTICES, rounding = CornerRounding(0.18f)).normalized()
    val pentagon: RoundedPolygon = RoundedPolygon(PENTAGON_VERTICES, rounding = CornerRounding(0.2f)).normalized()
    val clover: RoundedPolygon = RoundedPolygon.star(
        CLOVER_LEAVES,
        innerRadius = 0.45f,
        rounding = CornerRounding(0.4f),
        innerRounding = CornerRounding(0.3f),
    ).normalized()
    val cookie: RoundedPolygon = RoundedPolygon.star(
        COOKIE_LOBES,
        innerRadius = 0.8f,
        rounding = CornerRounding(0.5f),
    ).normalized()
    val burst: RoundedPolygon = RoundedPolygon.star(
        BURST_POINTS,
        innerRadius = 0.78f,
        rounding = CornerRounding(0.2f),
    ).normalized()
    val sun: RoundedPolygon = RoundedPolygon.star(
        SUN_RAYS,
        innerRadius = 0.8f,
        rounding = CornerRounding(0.2f),
    ).normalized()
    val pill: RoundedPolygon = RoundedPolygon.rectangle(
        width = 1f,
        height = 0.62f,
        rounding = CornerRounding(0.31f),
    ).normalized()
}

class PolygonShape(private val polygon: RoundedPolygon) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = polygon.toPath().asComposePath()
        path.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(path)
    }
}

object ExpressiveShapes {
    val cookie: Shape = PolygonShape(MorphShapes.cookie)
    val clover: Shape = PolygonShape(MorphShapes.clover)
}
