package com.ben.periodt.ui.settings.components

import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

fun rainConfetti(): List<Party> = listOf(
    Party(
        speed = 0f, maxSpeed = 30f, damping = 0.9f, spread = 360,
        colors = listOf(0xf2b179.toInt(), 0xFFD89046.toInt(), 0xf4306d.toInt(), 0xb48def.toInt()),
        position = Position.Relative(0.5, 0.3),
        emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100)
    )
)