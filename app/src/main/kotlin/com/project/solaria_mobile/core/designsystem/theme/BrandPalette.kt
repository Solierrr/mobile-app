package com.project.solaria_mobile.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Laranja da marca em escala tonal (50 a 900). O 500 é a cor de ação (`action-base` do web) e o 300 é o topo do degradê
 * dos botões (`action-highlight`); as demais são derivadas deles. Os mesmos valores estão em `res/values/colors.xml`,
 * para a tela de abertura e os componentes do sistema usarem a mesma cor.
 *
 * Contraste sobre branco: o 500 dá cerca de 3:1, bom para preenchimentos e ícones, mas pouco para texto pequeno; para texto
 * sobre fundo claro use o 700 ([Brand.strong]).
 */
object Brand {
    val tone50 = Color(0xFFFFF3EA)
    val tone100 = Color(0xFFFFE4D1)
    val tone200 = Color(0xFFFFC9A3)
    val tone300 = Color(0xFFFF9B52)
    val tone400 = Color(0xFFFF8A3D)
    val tone500 = Color(0xFFFF650A)
    val tone600 = Color(0xFFE85505)
    val tone700 = Color(0xFFC2410C)
    val tone800 = Color(0xFF9A3412)
    val tone900 = Color(0xFF7C2D12)

    /**
     * Laranja liso para preenchimentos fora do degradê: chips, dias, progresso, chaves, selos. É um tom entre o meio do
     * degradê dos botões (`#FF8030`) e o topo, para ele não parecer mais escuro que o botão.
     */
    val base = Color(0xFFFF8236)

    /** Fundo do degradê dos botões (`action-base` do web); só o degradê usa. */
    val gradientBase = tone500

    /** Topo do degradê dos botões. */
    val highlight = tone300

    /** Texto e ícones pequenos de cor de marca sobre fundo claro. */
    val strong = tone700

    /** Fundo suave de destaque. */
    val container = tone50
}
