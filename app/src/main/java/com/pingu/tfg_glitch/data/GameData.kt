package com.pingu.tfg_glitch.data

import java.util.UUID
import kotlin.math.max // Necesario para cálculos de precios de mercado
import android.util.Log // Solo para depuración en getCropMarketKey si es necesario

// --- Enums y Clases de Datos del Juego ---

enum class DadoSimbolo { GLITCH, CRECIMIENTO, ENERGIA, MONEDA, MISTERIO, PLANTAR }
enum class TipoCultivo { NORMAL, MUTADO }

sealed class CartaSemilla {
    abstract val id: String
    abstract val nombre: String
    abstract val tipo: TipoCultivo
    abstract val costePlantado: Int
    abstract val crecimientoRequerido: Int
    abstract val valorVentaBase: Int
    abstract val pvFinalJuego: Int
}

data class CultivoNormal(
    override val id: String = UUID.randomUUID().toString(),
    override val nombre: String = "",
    override val costePlantado: Int = 0,
    override val crecimientoRequerido: Int = 0,
    override val valorVentaBase: Int = 0,
    override val pvFinalJuego: Int = 0
) : CartaSemilla() {
    override val tipo = TipoCultivo.NORMAL
}

data class CultivoMutado(
    override val id: String = UUID.randomUUID().toString(),
    override val nombre: String = "",
    override val costePlantado: Int = 0,
    override val crecimientoRequerido: Int = 0,
    override val valorVentaBase: Int = 0,
    override val pvFinalJuego: Int = 0,
    val efecto: String = ""
) : CartaSemilla() {
    override val tipo = TipoCultivo.MUTADO
}

data class Granjero(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String = "",
    val habilidadPasiva: String = "",
    val habilidadActivable: String = "",
    val costeActivacion: String = ""
)

data class MejoraGlitch(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String = "",
    val efecto: String = ""
)

data class GlitchEvent(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = ""
)

data class Token(
    val tipo: String = "",
    val cantidad: Int = 0
)

data class CultivoPlantado(
    val id: String = UUID.randomUUID().toString(),
    val carta: CartaSemilla = CultivoNormal(),
    var marcadoresCrecimiento: Int = 0
)

data class CultivoInventario(
    val id: String = "",
    val nombre: String = "",
    var cantidad: Int = 0,
    val valorVentaBase: Int = 0,
    val pvFinalJuego: Int = 0
)

// --- Estructuras para la Mecánica de Misterio ---

data class MysteryOutcome(
    val description: String = "",
    val moneyChange: Int = 0,
    val energyChange: Int = 0,
)

data class MysteryChoice(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val outcome: MysteryOutcome = MysteryOutcome()
)

sealed class MysteryEncounter {
    abstract val id: String
    abstract val type: String
    abstract val title: String
    abstract val description: String
}

data class DecisionEncounter(
    override val id: String = UUID.randomUUID().toString(),
    override val title: String = "",
    override val description: String = "",
    val choices: List<MysteryChoice> = emptyList()
) : MysteryEncounter() {
    override val type: String = "decision"
}

data class RandomEventEncounter(
    override val id: String = UUID.randomUUID().toString(),
    override val title: String = "",
    override val description: String = "",
    val outcomes: List<Pair<MysteryOutcome, Int>> = emptyList()
) : MysteryEncounter() {
    override val type: String = "random"
}

data class MinigameEncounter(
    override val id: String = UUID.randomUUID().toString(),
    override val title: String = "",
    override val description: String = "",
    val minigameType: String = "",
    val successOutcome: MysteryOutcome = MysteryOutcome(),
    val failureOutcome: MysteryOutcome = MysteryOutcome()
) : MysteryEncounter() {
    override val type: String = "minigame"
}


// --- Jugador y Estado de la partida ---

data class Player(
    val id: String = "",
    val gameId: String = "",
    var name: String = "Nuevo Jugador",
    var farmerType: String = "Ingeniero Glitch",
    var money: Int = 10,
    var glitchEnergy: Int = 0,
    val mano: MutableList<CartaSemilla> = mutableListOf(),
    val parcela: MutableList<CultivoPlantado> = mutableListOf(),
    val inventario: MutableList<CultivoInventario> = mutableListOf(),
    var currentDiceRoll: List<DadoSimbolo> = emptyList(),
    var rollPhase: Int = 0,
    var hasUsedStandardReroll: Boolean = false,
    var hasUsedFreeReroll: Boolean = false,
    var hasUsedActiveSkill: Boolean = false,
    var privateSaleBonus: Int = 0, // NUEVO: Para la habilidad del Comerciante
    var mysteryButtonsRemaining: Int = 0,
    val objectivesClaimed: MutableList<String> = mutableListOf(),
    var totalScore: Int = 0,
    var manualBonusPV: Int = 0,
    var activeMysteryId: String? = null,
    var lastMysteryResult: String? = null
)

data class MarketPrices(
    val trigo: Int = 0,
    val maiz: Int = 0,
    val patata: Int = 0,
    val tomateCuadrado: Int = 0,
    val maizArcoiris: Int = 0,
    val brocoliCristal: Int = 0,
    val pimientoExplosivo: Int = 0,
)

data class Objective(
    val id: String = UUID.randomUUID().toString(),
    val description: String = "",
    val rewardPV: Int = 0,
    val type: String = "",
    val targetValue: Int = 0,
    val targetCropId: String? = null
)


// --- Constantes y Datos Iniciales ---

val initialMarketPrices = MarketPrices(
    trigo = 3, maiz = 4, patata = 3, tomateCuadrado = 6,
    maizArcoiris = 7, brocoliCristal = 6, pimientoExplosivo = 8
)

val eventosGlitch = listOf(
    GlitchEvent(name = "Aumento de Demanda", description = "El precio de todos los Cultivos Normales aumenta en +1 solo para esta Fase de Mercado."),
    GlitchEvent(name = "Cosecha Mutante Exitosa", description = "El precio de todos los Cultivos Mutados aumenta en +1 solo para esta Fase de Mercado."),
    GlitchEvent(name = "Interferencia de Señal", description = "¡Estática en la red! Durante esta Fase de Mercado, todos los precios de venta se reducen a la mitad. El mercado volverá a la normalidad en la siguiente ronda."),
    GlitchEvent(name = "Fallo de Suministro", description = "El coste de plantado de todos los cultivos aumenta permanentemente en 1 Moneda. (Este efecto solo puede ocurrir una vez por partida)."),
    GlitchEvent(name = "Impuesto Sorpresa", description = "Todos los jugadores con 10 o más monedas deben pagar un impuesto de 3 monedas a la reserva."),
    GlitchEvent(name = "Fiebre del Oro", description = "El precio del Maíz y la Patata aumenta en +2 durante esta Fase de Mercado."),
    GlitchEvent(name = "Bonus del Sindicato", description = "Todos los jugadores ganan 2 monedas."),
    GlitchEvent(name = "Fuga de Energía", description = "Todos los jugadores pierden 1 Energía Glitch. Si no puedes, pierdes 2 monedas."),
    GlitchEvent(name = "Tesoro Enterrado", description = "¡Suerte inesperada! El jugador inicial de esta ronda gana 4 monedas."),
    GlitchEvent(name = "Inversión Fallida", description = "El jugador con más monedas pierde 3 monedas."),
    GlitchEvent(name = "Sobrecarga de Energía", description = "Todos los jugadores ganan 1 Token de Energía Glitch."),
    GlitchEvent(name = "Crecimiento Acelerado", description = "Todos los jugadores añaden 1 Marcador de Crecimiento a uno de sus cultivos plantados."),
    GlitchEvent(name = "Plaga Leve", description = "Todos los jugadores deben retirar 1 Marcador de Crecimiento de uno de sus cultivos plantados."),
    GlitchEvent(name = "Intercambio Forzoso", description = "Todos los jugadores pasan una carta de su mano al jugador de su izquierda."),
    GlitchEvent(name = "Semillas Misteriosas", description = "Todos los jugadores roban 1 carta del mazo de Semilla Corrupta."),
    GlitchEvent(name = "Mala Cosecha", description = "Una plaga inesperada ataca el cultivo más avanzado. El jugador inicial de la ronda retira 2 Marcadores de Crecimiento de su cultivo plantado que más tenga."),
    GlitchEvent(name = "Fallo de Riego", description = "Todos los jugadores deben elegir 1 de sus cultivos plantados y retirar 1 Marcador de Crecimiento de él."),
    GlitchEvent(name = "Invasión de Bichos Píxel", description = "Cada jugador debe descartar 1 Carta de Semilla Corrupta de su mano. Si no tiene cartas, pierde 2 Monedas."),
    GlitchEvent(name = "Lluvia de Meteoritos Pixelados", description = "¡Peligro! Cada jugador tira un dado físico. Si sacas un 🌀 (Glitch), uno de tus cultivos plantados (elegido al azar) pierde todos sus marcadores de crecimiento."),
    GlitchEvent(name = "Inspiración Súbita", description = "¡Momento de lucidez! En la próxima fase de acciones, cada jugador puede elegir el resultado de uno de sus dados (excepto Misterio)."),
    GlitchEvent(name = "Plantación Gratuita", description = "El primer cultivo que plante cada jugador esta ronda tiene un coste de 0 monedas."),
    GlitchEvent(name = "Silencio de Radio", description = "Las habilidades activables de los Granjeros no se pueden usar esta ronda."),
    GlitchEvent(name = "El Dilema del Granjero", description = "El jugador inicial de la ronda se enfrenta a una decisión difícil: Perder 3 monedas o descartar 2 cartas de su mano."),
    GlitchEvent(name = "Ayuda Inesperada", description = "El jugador inicial de la ronda elige a otro jugador. Ambos ganáis 1 Energía Glitch."),
    GlitchEvent(name = "Sabotaje", description = "El jugador inicial de la ronda elige a un oponente. Ese oponente debe retirar 2 Marcadores de Crecimiento de uno de sus cultivos."),
    GlitchEvent(name = "Datos Corruptos", description = "¡Oh no! Todos los jugadores deben mostrar su mano. El jugador con más cartas debe descartar una al azar.")
)

val allMysteryEncounters = listOf<MysteryEncounter>(
    DecisionEncounter(
        id = "decision_dron",
        title = "Dron de Contrabando",
        description = "Encuentras un dron de reparto estrellado, aún humeante. Dentro, un paquete inestable emite un zumbido. ¿Qué haces?",
        choices = listOf(
            MysteryChoice("choice_abrir", "Intentar abrirlo", MysteryOutcome("¡Riesgo calculado! El paquete explota en una nube de purpurina y monedas. Ganas 4 monedas.", moneyChange = 4)),
            MysteryChoice("choice_ignorar", "Ignorarlo (demasiado arriesgado)", MysteryOutcome("Decides no tentar a la suerte. No pasa nada, pero sientes que has perdido una oportunidad.", moneyChange = 0)),
            MysteryChoice("choice_analizar", "Usar 1 Energía para analizarlo", MysteryOutcome("Tu escáner revela que es seguro. Dentro encuentras un prototipo de batería. Pierdes 1 Energía pero ganas 3.", energyChange = 2))
        )
    ),
    DecisionEncounter(
        id = "decision_mercader",
        title = "Mercader Misterioso",
        description = "Una figura encapuchada aparece entre tus cultivos y te ofrece un trato: 'Te doy 2 Energías Glitch ahora mismo... por solo 5 de tus monedas'.",
        choices = listOf(
            MysteryChoice("choice_aceptar", "Aceptar el trato (pagar 5 monedas)", MysteryOutcome("Haces el intercambio. Te sientes un poco estafado, pero la energía es tuya. Pierdes 5 monedas y ganas 2 Energías.", moneyChange = -5, energyChange = 2)),
            MysteryChoice("choice_rechazar", "Rechazar la oferta", MysteryOutcome("El mercader se encoge de hombros y desaparece entre las sombras. No ganas ni pierdes nada.", moneyChange = 0))
        )
    ),
    DecisionEncounter(
        id = "decision_cable",
        title = "Cable de Datos Expuesto",
        description = "Ves un cable de datos grueso y chispeante saliendo de la tierra. Podrías conectarlo a tu sistema para un impulso, pero parece inestable.",
        choices = listOf(
            MysteryChoice("choice_conectar", "Conectarlo", MysteryOutcome("¡Sobrecarga! El sistema se reinicia bruscamente. Pierdes 2 monedas.", moneyChange = -2)),
            MysteryChoice("choice_cortar", "Cortarlo por seguridad", MysteryOutcome("Recuperas el cobre del cable y lo vendes como chatarra. Ganas 2 monedas.", moneyChange = 2))
        )
    ),
    DecisionEncounter(
        id = "decision_codigo",
        title = "Fragmento de Código",
        description = "Encuentras una vieja tarjeta de memoria con un fragmento de código. Podrías ejecutarlo en tu sistema o venderlo en el mercado negro.",
        choices = listOf(
            MysteryChoice("choice_ejecutar", "Ejecutar el código (cuesta 1 Energía)", MysteryOutcome("El código optimiza tus sistemas de riego. Todos tus cultivos plantados ganan 1 marcador de crecimiento.", energyChange = -1)),
            MysteryChoice("choice_vender", "Vender el fragmento", MysteryOutcome("Un contacto anónimo te paga bien por la tarjeta. Ganas 3 monedas.", moneyChange = 3))
        )
    ),
    RandomEventEncounter(
        id = "random_lluvia_datos",
        title = "Lluvia de Datos",
        description = "Una extraña lluvia de datos cósmicos baña tu granja. Las plantas reaccionan de forma extraña...",
        outcomes = listOf(
            Pair(MysteryOutcome("¡La lluvia ha sido beneficiosa! Tus sistemas se optimizan. Ganas 2 monedas.", moneyChange = 2), 70),
            Pair(MysteryOutcome("La estática ha sobrecargado tus sistemas. Pierdes 1 Energía.", energyChange = -1), 30)
        )
    ),
    RandomEventEncounter(
        id = "random_animal_glitch",
        title = "Animal Glitcheado",
        description = "Una ardilla pixelada corretea por tu granja, dejando un rastro de datos corruptos antes de desaparecer en un muro.",
        outcomes = listOf(
            Pair(MysteryOutcome("La ardilla ha desenterrado algo brillante. ¡Encuentras 3 monedas!", moneyChange = 3), 50),
            Pair(MysteryOutcome("La ardilla mordisqueó un cable de energía. Pierdes 1 Energía.", energyChange = -1), 40),
            Pair(MysteryOutcome("La ardilla simplemente te mira fijamente y se desvanece. Te quedas... perplejo.", moneyChange = 0), 10)
        )
    ),
    RandomEventEncounter(
        id = "random_eco_futuro",
        title = "Eco del Futuro",
        description = "Tu radio capta una extraña transmisión. Parece un informe de mercado... ¡del día de mañana!",
        outcomes = listOf(
            Pair(MysteryOutcome("La información era correcta y te has anticipado al mercado. Ganas 3 monedas.", moneyChange = 3), 60),
            Pair(MysteryOutcome("El eco era falso y te ha distraído de tus tareas. No pasa nada.", moneyChange = 0), 40)
        )
    ),
    RandomEventEncounter(
        id = "random_fantasma",
        title = "Fantasma en la Máquina",
        description = "Las luces de tu granero parpadean y escuchas un susurro digital. ¿Es un error... o algo más?",
        outcomes = listOf(
            Pair(MysteryOutcome("Era un diagnóstico oculto del sistema. Ganas 1 Energía.", energyChange = 1), 50),
            Pair(MysteryOutcome("Era un simple cortocircuito. Pierdes 1 moneda para repararlo.", moneyChange = -1), 50)
        )
    ),
    MinigameEncounter(
        id = "minigame_firewall",
        title = "¡Brecha en el Cortafuegos!",
        description = "¡Alerta! Una conexión no autorizada intenta acceder a tus sistemas. ¡Estabiliza el núcleo de energía para bloquear la intrusión!",
        minigameType = "reaction_time",
        successOutcome = MysteryOutcome("¡Bloqueo exitoso! Has reforzado tu seguridad y optimizado los sistemas. Ganas 1 Energía y 3 monedas.", energyChange = 1, moneyChange = 3),
        failureOutcome = MysteryOutcome("La brecha ha sido parcial. El intruso ha robado algunos datos de mercado. Pierdes 2 monedas.", moneyChange = -2)
    )
)

val allCrops = listOf(
    CultivoNormal(id = "trigo", nombre = "Trigo", costePlantado = 2, crecimientoRequerido = 3, valorVentaBase = 3, pvFinalJuego = 2),
    CultivoNormal(id = "maiz", nombre = "Maíz", costePlantado = 2, crecimientoRequerido = 3, valorVentaBase = 4, pvFinalJuego = 3),
    CultivoNormal(id = "patata", nombre = "Patata", costePlantado = 3, crecimientoRequerido = 4, valorVentaBase = 3, pvFinalJuego = 2),
    CultivoMutado(id = "tomate_cuadrado", nombre = "Tomate Cuadrado", costePlantado = 4, crecimientoRequerido = 5, valorVentaBase = 6, pvFinalJuego = 5, efecto = "Gana 1 energía Glitch al cosechar."),
    CultivoMutado(id = "maiz_arcoiris", nombre = "Maíz Arcoíris", costePlantado = 5, crecimientoRequerido = 6, valorVentaBase = 7, pvFinalJuego = 6, efecto = "Roba 1 carta de mejora Glitch al cosechar."),
    CultivoMutado(id = "brocoli_cristal", nombre = "Brócoli Cristal", costePlantado = 4, crecimientoRequerido = 5, valorVentaBase = 6, pvFinalJuego = 5, efecto = "Puedes venderlo por +2 monedas en el mercado."),
    CultivoMutado(id = "pimiento_explosivo", nombre = "Pimiento Explosivo", costePlantado = 6, crecimientoRequerido = 7, valorVentaBase = 8, pvFinalJuego = 7, efecto = "Inflige 1 daño a un oponente al cosechar."),
)

val allObjectives = listOf(
    Objective(id = "obj_money_15", description = "Acumula 15 Monedas en tu reserva.", rewardPV = 3, type = "money", targetValue = 15),
    Objective(id = "obj_money_25", description = "Acumula 25 Monedas en tu reserva.", rewardPV = 5, type = "money", targetValue = 25),
    Objective(id = "obj_total_harvest_5", description = "Cosecha un total de 5 cultivos (de cualquier tipo).", rewardPV = 2, type = "total_harvest", targetValue = 5),
    Objective(id = "obj_total_harvest_10", description = "Cosecha un total de 10 cultivos (de cualquier tipo).", rewardPV = 4, type = "total_harvest", targetValue = 10),
    Objective(id = "obj_specific_trigo_3", description = "Cosecha 3 Trigos.", rewardPV = 3, type = "specific_harvest", targetValue = 3, targetCropId = "trigo"),
    Objective(id = "obj_specific_maiz_3", description = "Cosecha 3 Maíces.", rewardPV = 3, type = "specific_harvest", targetValue = 3, targetCropId = "maiz"),
    Objective(id = "obj_dice_all_same", description = "Realiza una tirada de dados con los 4 símbolos iguales.", rewardPV = 8, type = "dice_roll_all_same", targetValue = 1)
)

fun getCropMarketKey(cropName: String): String {
    return when (cropName) {
        "Trigo" -> "trigo"
        "Maíz" -> "maiz"
        "Patata" -> "patata"
        "Tomate Cuadrado" -> "tomateCuadrado"
        "Maíz Arcoíris" -> "maizArcoiris"
        "Brócoli Cristal" -> "brocoliCristal"
        "Pimiento Explosivo" -> "pimientoExplosivo"
        else -> {
            Log.w("GameData", "Unknown crop name for market mapping: $cropName")
            ""
        }
    }
}
