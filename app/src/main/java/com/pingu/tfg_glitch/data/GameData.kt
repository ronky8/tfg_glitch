package com.pingu.tfg_glitch.data

import java.util.UUID
import kotlin.math.max
import android.util.Log

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
    val id: String = "",
    val nombre: String = "",
    val habilidadPasiva: String = "",
    val habilidadActivable: String = "",
    val costeActivacion: String = "",
    val iconName: String = "help_outline" // Usaremos esto para asociar un icono
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
    var money: Int = 5,
    var glitchEnergy: Int = 1,
    var granjero: Granjero? = null, // Granjero asignado
    val mano: MutableList<CartaSemilla> = mutableListOf(),
    val parcela: MutableList<CultivoPlantado> = mutableListOf(),
    val inventario: MutableList<CultivoInventario> = mutableListOf(),
    var currentDiceRoll: List<DadoSimbolo> = emptyList(),
    var rollPhase: Int = 0,
    var hasRerolled: Boolean = false,
    var haUsadoPasivaIngeniero: Boolean = false, // Para la pasiva del Ingeniero
    var haUsadoHabilidadActiva: Boolean = false, // ¡NUEVO! Para controlar la habilidad activa
    var mysteryButtonsRemaining: Int = 0,
    val objectivesClaimed: MutableList<String> = mutableListOf(),
    var totalScore: Int = 0,
    var manualBonusPV: Int = 0,
    var activeMysteryId: String? = null,
    var lastMysteryResult: String? = null
)

data class MarketPrices(
    val zanahoria: Int = 0,
    val maiz: Int = 0,
    val patata: Int = 0,
    val tomateCubico: Int = 0,
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
    zanahoria = 3,
    maiz = 4,
    patata = 3,
    tomateCubico = 6,
    maizArcoiris = 7,
    brocoliCristal = 6,
    pimientoExplosivo = 8
)

val eventosGlitch = listOf(
    GlitchEvent(name = "Aumento de Demanda", description = "El precio de todos los Cultivos Normales aumenta en +1 solo para esta Fase de Mercado."),
    GlitchEvent(name = "Cosecha Mutante Exitosa", description = "El precio de todos los Cultivos Mutados aumenta en +1 solo para esta Fase de Mercado."),
    GlitchEvent(name = "Interferencia de Señal", description = "¡Estática en la red! Durante esta Fase de Mercado, todos los precios de venta se reducen a la mitad."),
    GlitchEvent(name = "Fallo de Suministro", description = "El coste de plantado de todos los cultivos aumenta permanentemente en 1 Moneda."),
    GlitchEvent(name = "Impuesto Sorpresa", description = "Todos los jugadores con 10 o más monedas deben pagar un impuesto de 3 monedas."),
    GlitchEvent(name = "Fiebre del Oro", description = "El precio del Maíz y la Patata aumenta en +2 durante esta Fase de Mercado."),
    GlitchEvent(name = "Bonus del Sindicato", description = "Todos los jugadores ganan 2 monedas."),
    GlitchEvent(name = "Fuga de Energía", description = "Todos los jugadores pierden 1 Energía Glitch. Si no puedes, pierdes 2 monedas."),
)

val allMysteryEncounters = listOf<MysteryEncounter>(
    DecisionEncounter(
        id = "decision_dron",
        title = "Dron de Contrabando",
        description = "Encuentras un dron de reparto estrellado. Dentro, un paquete inestable emite un zumbido. ¿Qué haces?",
        choices = listOf(
            MysteryChoice("choice_abrir", "Intentar abrirlo", MysteryOutcome("¡Riesgo calculado! Ganas 4 monedas.", moneyChange = 4)),
            MysteryChoice("choice_ignorar", "Ignorarlo", MysteryOutcome("Decides no tentar a la suerte. No pasa nada.", moneyChange = 0)),
            MysteryChoice("choice_analizar", "Usar 1 Energía para analizarlo", MysteryOutcome("Es seguro. Dentro encuentras un prototipo de batería. Pierdes 1 Energía pero ganas 3.", energyChange = 2))
        )
    ),
    RandomEventEncounter(
        id = "random_lluvia_datos",
        title = "Lluvia de Datos",
        description = "Una extraña lluvia de datos cósmicos baña tu granja. Las plantas reaccionan de forma extraña...",
        outcomes = listOf(
            Pair(MysteryOutcome("¡La lluvia ha sido beneficiosa! Ganas 2 monedas.", moneyChange = 2), 70),
            Pair(MysteryOutcome("La estática ha sobrecargado tus sistemas. Pierdes 1 Energía.", energyChange = -1), 30)
        )
    ),
    MinigameEncounter(
        id = "minigame_firewall",
        title = "¡Brecha en el Cortafuegos!",
        description = "¡Alerta! Una conexión no autorizada intenta acceder a tus sistemas. ¡Estabiliza el núcleo de energía para bloquear la intrusión!",
        minigameType = "reaction_time",
        successOutcome = MysteryOutcome("¡Bloqueo exitoso! Ganas 1 Energía y 3 monedas.", energyChange = 1, moneyChange = 3),
        failureOutcome = MysteryOutcome("La brecha ha sido parcial. Pierdes 2 monedas.", moneyChange = -2)
    )
)

val allCrops = listOf(
    CultivoNormal(id = "zanahoria", nombre = "Zanahoria", costePlantado = 2, crecimientoRequerido = 3, valorVentaBase = 3, pvFinalJuego = 2),
    CultivoNormal(id = "maiz", nombre = "Maíz Común", costePlantado = 3, crecimientoRequerido = 4, valorVentaBase = 4, pvFinalJuego = 3),
    CultivoNormal(id = "patata", nombre = "Patata Terrosa", costePlantado = 1, crecimientoRequerido = 4, valorVentaBase = 3, pvFinalJuego = 2),
    CultivoMutado(id = "tomateCubico", nombre = "Tomate Cúbico", costePlantado = 4, crecimientoRequerido = 4, valorVentaBase = 6, pvFinalJuego = 5, efecto = "Al plantar, puedes descartar 1 token de Zanahoria para colocar 2 ➕ adicionales."),
    CultivoMutado(id = "maizArcoiris", nombre = "Maíz Arcoíris", costePlantado = 5, crecimientoRequerido = 5, valorVentaBase = 7, pvFinalJuego = 6, efecto = "Al vender, puedes descartar 1 token de Maíz para ganar 3 💰 adicionales."),
    CultivoMutado(id = "brocoliCristal", nombre = "Brócoli Cristal", costePlantado = 4, crecimientoRequerido = 4, valorVentaBase = 6, pvFinalJuego = 5, efecto = "Al cosechar, puedes descartar 1 token de Patata para cambiar el resultado de un dado."),
    CultivoMutado(id = "pimientoExplosivo", nombre = "Pimiento Explosivo", costePlantado = 6, crecimientoRequerido = 6, valorVentaBase = 8, pvFinalJuego = 7, efecto = "Al vender, puedes descartar 1 token de Zanahoria y 1 de Maíz para ganar 2 💰 adicionales.")
)

val allObjectives = listOf(
    Objective(id = "obj_money_15", description = "Acumula 15 Monedas en tu reserva.", rewardPV = 3, type = "money", targetValue = 15),
    Objective(id = "obj_money_25", description = "Acumula 25 Monedas en tu reserva.", rewardPV = 5, type = "money", targetValue = 25),
    Objective(id = "obj_total_harvest_5", description = "Cosecha un total de 5 cultivos.", rewardPV = 2, type = "total_harvest", targetValue = 5),
    Objective(id = "obj_specific_zanahoria_3", description = "Cosecha 3 Zanahorias.", rewardPV = 3, type = "specific_harvest", targetValue = 3, targetCropId = "zanahoria"),
    Objective(id = "obj_dice_all_same", description = "Saca los 4 dados con el mismo símbolo.", rewardPV = 8, type = "dice_roll_all_same", targetValue = 1)
)

val allGranjeros = listOf(
    Granjero(id = "ingeniero_glitch", nombre = "El Ingeniero Glitch", habilidadPasiva = "Una vez por turno, puedes volver a tirar uno de tus dados sin coste.", habilidadActivable = "Elige uno de tus dados. Puedes cambiar su resultado a la cara que elijas.", costeActivacion = "1 ⚡", iconName = "engineering"),
    Granjero(id = "botanica_mutante", nombre = "La Botánica Mutante", habilidadPasiva = "Cuando plantas un Cultivo Mutado, colocas sobre él 1 Marcador de Crecimiento adicional.", habilidadActivable = "Elige uno de tus cultivos. Hasta el final del turno, cuenta como si tuviera 2 Marcadores de Crecimiento adicionales para ser cosechado.", costeActivacion = "2 ⚡", iconName = "local_florist"),
    Granjero(id = "comerciante_sombrio", nombre = "El Comerciante Sombrío", habilidadPasiva = "Ganas 1 Moneda adicional por cada 2 cultivos que vendas en la misma transacción.", habilidadActivable = "Durante esta Fase de Mercado, ganas 1 Moneda adicional por cada cultivo que vendas.", costeActivacion = "1 ⚡", iconName = "storefront"),
    Granjero(id = "visionaria_pixel", nombre = "La Visionaria Píxel", habilidadPasiva = "Cada vez que resuelvas una Carta de Catástrofe, roba 1 carta del mazo de Mejoras Glitch.", habilidadActivable = "Mira las 3 cartas superiores del Mazo Principal. Añade 1 a tu mano y coloca las otras 2 en la parte superior del mazo en el orden que elijas.", costeActivacion = "1 ⚡", iconName = "visibility")
)

fun getCropMarketKey(cropName: String): String {
    return when (cropName) {
        "Zanahoria" -> "zanahoria"
        "Maíz Común" -> "maiz"
        "Patata Terrosa" -> "patata"
        "Tomate Cúbico" -> "tomateCubico"
        "Maíz Arcoíris" -> "maizArcoiris"
        "Brócoli Cristal" -> "brocoliCristal"
        "Pimiento Explosivo" -> "pimientoExplosivo"
        else -> ""
    }
}

