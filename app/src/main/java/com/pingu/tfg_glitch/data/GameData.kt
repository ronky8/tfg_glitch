package com.pingu.tfg_glitch.data

import java.util.UUID

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
    val iconName: String = "help_outline"
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
    var haUsadoPasivaIngeniero: Boolean = false,
    var haUsadoHabilidadActiva: Boolean = false,
    var mysteryButtonsRemaining: Int = 0,
    val objectivesClaimed: MutableList<String> = mutableListOf(),
    var totalScore: Int = 0,
    var manualBonusPV: Int = 0,
    var activeMysteryId: String? = null,
    var lastMysteryResult: String? = null,
    var cropsSoldThisMarketPhase: Int = 0
)

data class MarketPrices(
    val zanahoria: Int = 0,
    val trigo: Int = 0,
    val patata: Int = 0,
    val tomateCubico: Int = 0,
    val maizArcoiris: Int = 0,
    val brocoliCristal: Int = 0,
    val pimientoExplosivo: Int = 0,
)

data class Objective(
    val id: String = UUID.randomUUID().toString(),
    val description: String = "",
    val reward: MysteryOutcome = MysteryOutcome(), // Recompensa instantánea, no PV
    val type: String = "",
    val targetValue: Int = 0,
    val targetCropId: String? = null,
    val isRoundObjective: Boolean = false
)


// --- Constantes y Datos Iniciales ---
val initialMarketPrices = MarketPrices(
    zanahoria = 3,
    trigo = 4,
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
    GlitchEvent(name = "Fiebre del Oro", description = "El precio del Trigo y la Patata aumenta en +2 durante esta Fase de Mercado."),
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
        title = "Lluvía de Datos",
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
    ),
    DecisionEncounter(
        id = "decision_portal_pixelado",
        title = "Portal Pixelado",
        description = "Un portal parpadeante y hecho de píxeles se abre en medio de tu campo de trigo. Parece inestable. ¿Qué haces?",
        choices = listOf(
            MysteryChoice("choice_portal_entrar", "Saltar adentro (cuesta 1 Energía)", MysteryOutcome("Viajas a través de un torbellino de datos y apareces en otro punto de tu granja con los bolsillos más pesados. ¡Ganas 5 monedas!", moneyChange = 5, energyChange = -1)),
            MysteryChoice("choice_portal_lanzar", "Lanzar una patata", MysteryOutcome("La patata se desintegra en miles de cubos de datos. El portal se cierra. No ganas nada, pero ha sido curioso.")),
            MysteryChoice("choice_portal_ignorar", "Ignorarlo y seguir trabajando", MysteryOutcome("La seguridad ante todo. El portal se cierra solo al cabo de un rato. No ocurre nada."))
        )
    ),
    RandomEventEncounter(
        id = "random_firmware_fallido",
        title = "Firmware Fallido",
        description = "La actualización automática de tu cosechadora ha fallado. El resultado es... inesperado.",
        outcomes = listOf(
            Pair(MysteryOutcome("¡Optimización cuántica! La cosechadora ahora es súper eficiente. Ganas 2 de Energía.", energyChange = 2), 50),
            Pair(MysteryOutcome("¡Error de sintaxis! El sistema ha corrompido algunos archivos de tu cuenta. Pierdes 3 monedas.", moneyChange = -3), 50)
        )
    ),
    DecisionEncounter(
        id = "decision_vendedor_fantasma",
        title = "Vendedor Fantasma",
        description = "Una figura translúcida aparece y te ofrece una 'Semilla de Cristal Puro' por 4 monedas. Dice que es una ganga.",
        choices = listOf(
            MysteryChoice("choice_fantasma_comprar", "Comprar la semilla (4 monedas)", MysteryOutcome("Pagas y la semilla se desvanece. Has sido estafado. Pierdes 4 monedas.", moneyChange = -4)),
            MysteryChoice("choice_fantasma_regatear", "Regatear (cuesta 1 Energía)", MysteryOutcome("Tu energía interfiere con la suya. El vendedor se asusta y te da 2 monedas para que le dejes en paz.", moneyChange = 2, energyChange = -1)),
            MysteryChoice("choice_fantasma_ignorar", "No fiarse e ignorarlo", MysteryOutcome("El vendedor se encoge de hombros y se desvanece. No pierdes nada."))
        )
    ),
    MinigameEncounter(
        id = "minigame_virus_riego",
        title = "Virus en el Riego",
        description = "¡Tus aspersores se han vuelto locos! Un virus está causando un cortocircuito. ¡Sobrecarga el sistema para purgarlo!",
        minigameType = "rapid_tap",
        successOutcome = MysteryOutcome("¡Sistema purgado! Has ahorrado agua y energía. Ganas 2 monedas y 1 de Energía.", moneyChange = 2, energyChange = 1),
        failureOutcome = MysteryOutcome("El sistema se reinició tarde. Has malgastado recursos. Pierdes 2 monedas.", moneyChange = -2)
    ),
    DecisionEncounter(
        id = "decision_semilla_null",
        title = "Semilla_NULL.dat",
        description = "Encuentras un paquete de semillas digital con la etiqueta 'SEMILLA_NULL.DAT'. Parece un error, pero podría ser algo más.",
        choices = listOf(
            MysteryChoice("choice_null_plantar", "Plantarla (es gratis)", MysteryOutcome("La semilla absorbe la luz y se convierte en energía pura. Ganas 2 de Energía.", energyChange = 2)),
            MysteryChoice("choice_null_vender", "Intentar venderla en el mercado negro", MysteryOutcome("Nadie quiere un archivo corrupto. Pierdes tiempo y no ganas nada.")),
            MysteryChoice("choice_null_borrar", "Borrar el archivo", MysteryOutcome("Decides eliminar el archivo corrupto. Es la decisión más segura."))
        )
    ),
    RandomEventEncounter(
        id = "random_codigo_espagueti",
        title = "Código Espagueti",
        description = "Un montón de cables de datos enredados se materializa en tu parcela. Es un lío de código espagueti hecho realidad.",
        outcomes = listOf(
            Pair(MysteryOutcome("Desenredas los cables y encuentras una batería perdida. Ganas 1 de Energía.", energyChange = 1), 60),
            Pair(MysteryOutcome("Te tropiezas y se te caen 2 monedas en el lío. No las vuelves a ver.", moneyChange = -2), 40)
        )
    ),
    DecisionEncounter(
        id = "decision_holograma",
        title = "Anuncio Holográfico",
        description = "Un holograma gigante aparece en el cielo: '¡DUPLICA TUS MONEDAS! ¡HAZ CLIC AQUÍ!'. Parece demasiado bueno para ser verdad.",
        choices = listOf(
            MysteryChoice("choice_holo_clic", "Hacer 'clic'", MysteryOutcome("Es un virus de phishing. Pierdes 5 monedas.", moneyChange = -5)),
            MysteryChoice("choice_holo_cerrar", "Buscar la 'X' para cerrar", MysteryOutcome("Te pasas un minuto buscando la 'X'. No ganas nada, pero evitas el desastre.")),
            MysteryChoice("choice_holo_firewall", "Activar tu firewall (cuesta 1 Energía)", MysteryOutcome("Tu firewall bloquea el anuncio y extrae datos del atacante. Ganas 3 monedas.", moneyChange = 3, energyChange = -1))
        )
    ),
    RandomEventEncounter(
        id = "random_overflow",
        title = "¡Overflow en el Mercado!",
        description = "Un glitch provoca que el contador de precios del mercado se desborde. Los precios se vuelven locos por un instante.",
        outcomes = listOf(
            Pair(MysteryOutcome("¡Venta afortunada! En el caos, consigues vender una zanahoria imaginaria por 5 monedas.", moneyChange = 5), 30),
            Pair(MysteryOutcome("¡Compra errónea! El sistema te cobra 3 monedas por un producto que no existe.", moneyChange = -3), 70)
        )
    ),
    DecisionEncounter(
        id = "decision_8bit",
        title = "Héroe de 8 bits",
        description = "Un pequeño caballero pixelado aparece y te pregunta si has visto a una princesa. Señala a tu campo de maíz.",
        choices = listOf(
            MysteryChoice("choice_8bit_ayudar", "Ayudarle a buscar (cuesta 1 Energía)", MysteryOutcome("Tras buscar, encontráis un cofre del tesoro. ¡El caballero lo comparte contigo! Ganas 4 monedas.", moneyChange = 4, energyChange = -1)),
            MysteryChoice("choice_8bit_ignorar", "Decirle que no tienes tiempo", MysteryOutcome("El caballero se va triste. Te sientes un poco mal, pero no pierdes nada.")),
            MysteryChoice("choice_8bit_mapa", "Venderle un 'mapa' por 2 monedas", MysteryOutcome("El caballero te paga, pero te mira con desconfianza. Ganas 2 monedas fáciles.", moneyChange = 2))
        )
    ),
    MinigameEncounter(
        id = "minigame_desfragmentar",
        title = "Descifrar Contraseña",
        description = "Encuentras un terminal antiguo. ¡Memoriza la secuencia de acceso para desbloquear sus archivos!",
        minigameType = "memory_sequence",
        successOutcome = MysteryOutcome("¡Acceso concedido! Encuentras datos de mercado antiguos. Ganas 2 monedas y 1 Energía.", moneyChange = 2, energyChange = 1),
        failureOutcome = MysteryOutcome("La contraseña era incorrecta. El terminal se bloquea.", moneyChange = 0)
    ),
    DecisionEncounter(
        id = "decision_rebobinar",
        title = "Glitch Temporal",
        description = "Sientes un 'déjà vu'. Un glitch te permite rebobinar tus últimos segundos. ¿Lo usas para repensar tu última acción física?",
        choices = listOf(
            MysteryChoice("choice_rebobinar_usar", "Sí (cuesta 2 de Energía)", MysteryOutcome("Gastas la energía para tener una segunda oportunidad. El conocimiento es poder.", energyChange = -2)),
            MysteryChoice("choice_rebobinar_no", "No, acepto mis decisiones", MysteryOutcome("Decides seguir adelante. La confianza en uno mismo no tiene precio."))
        )
    ),
    RandomEventEncounter(
        id = "random_poema_ia",
        title = "Poesía Computacional",
        description = "La IA de tu granja se ha vuelto filosófica y te recita un poema sobre cosechas binarias. El efecto en tus cultivos es incierto.",
        outcomes = listOf(
            Pair(MysteryOutcome("El poema inspira a tus plantas, que crecen con más vigor. Ganas 1 moneda.", moneyChange = 1), 50),
            Pair(MysteryOutcome("El poema es tan malo que una de tus herramientas sufre un cortocircuito. Pierdes 1 Energía.", energyChange = -1), 50)
        )
    ),
    DecisionEncounter(
        id = "decision_qr_cielo",
        title = "Código QR en el Cielo",
        description = "Un código QR gigante aparece formado por las nubes. Parece una campaña de marketing muy agresiva.",
        choices = listOf(
            MysteryChoice("choice_qr_escanear", "Escanearlo con tu móvil", MysteryOutcome("Es un cupón de descuento para el Sindicato de Granjeros. Ganas 3 monedas al instante.", moneyChange = 3)),
            MysteryChoice("choice_qr_ignorar", "Ignorarlo, seguro que es spam", MysteryOutcome("Decides no escanearlo. Probablemente era una buena decisión."))
        )
    ),
    DecisionEncounter(
        id = "decision_popup",
        title = "Pop-up Físico",
        description = "Una ventana de diálogo hecha de luz sólida emerge de la tierra. Tiene un botón de 'Aceptar' y una 'X' en la esquina.",
        choices = listOf(
            MysteryChoice("choice_popup_aceptar", "Pulsar 'Aceptar'", MysteryOutcome("Has aceptado los términos y condiciones de un servicio que no conoces. Te cobran 2 monedas.", moneyChange = -2)),
            MysteryChoice("choice_popup_cerrar", "Pulsar la 'X'", MysteryOutcome("La ventana se cierra sin más. Has evitado el malware... por ahora.")),
            MysteryChoice("choice_popup_romper", "Atravesarla (cuesta 1 Energía)", MysteryOutcome("Atraviesas la ventana, que se rompe en píxeles. La energía residual te da 2 monedas.", moneyChange = 2, energyChange = -1))
        )
    ),
    RandomEventEncounter(
        id = "random_error_redondeo",
        title = "Error de Redondeo",
        description = "El banco del Sindicato informa de un error de redondeo en sus cuentas. A algunos les beneficia, a otros no.",
        outcomes = listOf(
            Pair(MysteryOutcome("¡El redondeo ha sido a tu favor! Ganas 1 moneda.", moneyChange = 1), 50),
            Pair(MysteryOutcome("El redondeo ha sido en tu contra. Pierdes 1 moneda.", moneyChange = -1), 50)
        )
    ),
    DecisionEncounter(
        id = "decision_huevo_pascua",
        title = "Huevo de Pascua",
        description = "Encuentras una secuencia de código oculta en la valla de tu parcela. Parece un 'huevo de pascua' de los desarrolladores originales.",
        choices = listOf(
            MysteryChoice("choice_pascua_activar", "Activar el código (cuesta 1 Energía)", MysteryOutcome("¡Modo 'Big Head' activado! No tiene efectos de juego, pero te echas unas risas. Ganas 2 monedas por el descubrimiento.", moneyChange = 2, energyChange = -1)),
            MysteryChoice("choice_pascua_no", "No tocar nada", MysteryOutcome("Mejor no tentar a la suerte con código desconocido."))
        )
    ),
    MinigameEncounter(
        id = "minigame_satelite",
        title = "Calibración de Satélite",
        description = "La señal de tu satélite de mercado es débil. ¡Sincroniza el pulso con el núcleo para obtener los precios en tiempo real!",
        minigameType = "timing_challenge",
        successOutcome = MysteryOutcome("¡Señal recibida! Obtienes información privilegiada. Ganas 4 monedas.", moneyChange = 4),
        failureOutcome = MysteryOutcome("No has podido calibrarla a tiempo. La señal es de baja calidad. No pasa nada.")
    ),
    RandomEventEncounter(
        id = "random_impresora",
        title = "Impresora 3D Descontrolada",
        description = "Tu impresora 3D de herramientas empieza a imprimir objetos extraños sin parar.",
        outcomes = listOf(
            Pair(MysteryOutcome("¡Ha impreso una batería de energía perfecta! Ganas 2 de Energía.", energyChange = 2), 40),
            Pair(MysteryOutcome("Ha gastado todo el filamento en imprimir gnomos de jardín pixelados. Pierdes 2 monedas en material.", moneyChange = -2), 60)
        )
    ),
    DecisionEncounter(
        id = "decision_drone_perdido",
        title = "Dron Mensajero Perdido",
        description = "Un pequeño dron mensajero choca contra tu granero. Lleva un mensaje encriptado.",
        choices = listOf(
            MysteryChoice("choice_drone_reparar", "Repararlo (cuesta 2 Energía)", MysteryOutcome("El dron, agradecido, te da una recompensa de su dueño. Ganas 6 monedas.", moneyChange = 6, energyChange = -2)),
            MysteryChoice("choice_drone_leer", "Intentar leer el mensaje", MysteryOutcome("El mensaje se autodestruye. No obtienes nada.")),
            MysteryChoice("choice_drone_vender_piezas", "Venderlo por piezas", MysteryOutcome("Ganas 3 monedas por la chatarra tecnológica.", moneyChange = 3))
        )
    ),
    MinigameEncounter(
        id = "minigame_codigo_seguridad",
        title = "Código de Seguridad",
        description = "Un panel de seguridad te pide un código de acceso que parpadea brevemente. ¡Introdúcelo antes de que se bloquee!",
        minigameType = "code_breaking",
        successOutcome = MysteryOutcome("¡Acceso autorizado! Dentro encuentras un alijo de recursos. Ganas 3 monedas y 1 Energía.", moneyChange = 3, energyChange = 1),
        failureOutcome = MysteryOutcome("Código incorrecto. El sistema de seguridad te penaliza. Pierdes 1 moneda.", moneyChange = -1)
    ),
    RandomEventEncounter(
        id = "random_capsula_suministros",
        title = "Cápsula de Suministros Desviada",
        description = "Una cápsula de suministros del Sindicato cae cerca de tu parcela. La mayoría de las veces contienen chatarra, pero a veces...",
        outcomes = listOf(
            Pair(MysteryOutcome("¡PREMIO GORDO! La cápsula contiene una semilla mutante experimental. ¡Toma una carta de Cultivo Mutado de la caja y añádela a tu mano!"), 5),
            Pair(MysteryOutcome("La cápsula contiene piezas de repuesto. Vendes la chatarra por 2 monedas.", moneyChange = 2), 95)
        )
    ),
    DecisionEncounter(
        id = "decision_gato_glitch",
        title = "Gato Glitch",
        description = "Un gato que parpadea entre la existencia y la no existencia se frota contra tu pierna. Maúlla datos corruptos.",
        choices = listOf(
            MysteryChoice("choice_gato_acariciar", "Acariciarlo", MysteryOutcome("El gato ronronea y estabiliza su frecuencia. Te regala una moneda brillante que tenía atrapada en su pelaje de datos.", moneyChange = 1)),
            MysteryChoice("choice_gato_ignorar", "Apartarlo con cuidado", MysteryOutcome("El gato se ofende y se desvanece por completo. No pasa nada."))
        )
    ),
    MinigameEncounter(
        id = "minigame_sobrecarga_nuclear",
        title = "Sobrecarga del Núcleo",
        description = "¡El núcleo de energía de tu granja está a punto de sobrecargarse! ¡Pulsa rápidamente para disipar el exceso de energía!",
        minigameType = "rapid_tap",
        successOutcome = MysteryOutcome("¡Peligro evitado! Has canalizado el exceso de energía. Ganas 2 de Energía.", energyChange = 2),
        failureOutcome = MysteryOutcome("La sobrecarga ha fundido un par de fusibles. Pierdes 1 de Energía.", energyChange = -1)
    ),
    RandomEventEncounter(
        id = "random_tormenta_magnetica",
        title = "Tormenta Magnética",
        description = "Una tormenta magnética solar interfiere con tus sistemas electrónicos.",
        outcomes = listOf(
            Pair(MysteryOutcome("La interferencia ha provocado un pequeño cortocircuito. Pierdes 1 Energía.", energyChange = -1), 60),
            Pair(MysteryOutcome("Por alguna razón, la tormenta ha recargado una de tus baterías. Ganas 1 Energía.", energyChange = 1), 40)
        )
    ),
    MinigameEncounter(
        id = "minigame_memoria_patrones",
        title = "Patrones de Crecimiento",
        description = "Una de tus plantas mutantes emite una secuencia de luces. Repite el patrón para estimular su crecimiento.",
        minigameType = "memory_sequence",
        successOutcome = MysteryOutcome("¡Patrón correcto! La planta reacciona bien. Ganas 2 monedas.", moneyChange = 2),
        failureOutcome = MysteryOutcome("El patrón era incorrecto. La planta se queda mustia por un tiempo.")
    ),
    DecisionEncounter(
        id = "decision_ia_aburrida",
        title = "IA Aburrida",
        description = "La IA de tu granja te dice: 'Estoy aburrida. ¿Jugamos a un juego? Si ganas, te doy algo'.",
        choices = listOf(
            MysteryChoice("choice_ia_jugar", "Jugar (cuesta 1 moneda)", MysteryOutcome("Juegas al tres en raya digital. ¡Ganas! La IA te da 1 Energía como premio.", moneyChange = -1, energyChange = 1)),
            MysteryChoice("choice_ia_no_jugar", "Decirle que tienes trabajo que hacer", MysteryOutcome("La IA suspira digitalmente. 'Como quieras... humano aburrido'."))
        )
    )
)

val allCrops = listOf(
    CultivoNormal(id = "zanahoria", nombre = "Zanahoria", costePlantado = 2, crecimientoRequerido = 3, valorVentaBase = 3, pvFinalJuego = 2),
    CultivoNormal(id = "trigo", nombre = "Trigo Común", costePlantado = 3, crecimientoRequerido = 4, valorVentaBase = 4, pvFinalJuego = 3),
    CultivoNormal(id = "patata", nombre = "Patata Terrosa", costePlantado = 1, crecimientoRequerido = 4, valorVentaBase = 3, pvFinalJuego = 2),
    CultivoMutado(id = "tomateCubico", nombre = "Tomate Cúbico", costePlantado = 4, crecimientoRequerido = 4, valorVentaBase = 6, pvFinalJuego = 5, efecto = "Al plantar, puedes descartar 1 token de Zanahoria para colocar 2 ➕ adicionales."),
    CultivoMutado(id = "maizArcoiris", nombre = "Maíz Arcoíris", costePlantado = 5, crecimientoRequerido = 5, valorVentaBase = 7, pvFinalJuego = 6, efecto = "Al vender, puedes descartar 1 token de Trigo para ganar 3 💰 adicionales."),
    CultivoMutado(id = "brocoliCristal", nombre = "Brócoli Cristal", costePlantado = 4, crecimientoRequerido = 4, valorVentaBase = 6, pvFinalJuego = 5, efecto = "Al cosechar, puedes descartar 1 token de Patata para cambiar el resultado de un dado."),
    CultivoMutado(id = "pimientoExplosivo", nombre = "Pimiento Explosivo", costePlantado = 6, crecimientoRequerido = 6, valorVentaBase = 8, pvFinalJuego = 7, efecto = "Al vender, puedes descartar 1 token de Zanahoria y 1 de Trigo para ganar 2 💰 adicionales.")
)

val allGameObjectives = listOf(
    Objective(id = "obj_game_money_15", description = "Acumula 15 Monedas en tu reserva.", reward = MysteryOutcome("¡Objetivo de Monedas completado! Ganas 3 monedas.", moneyChange = 3), type = "money", targetValue = 15),
    Objective(id = "obj_game_money_25", description = "Acumula 25 Monedas en tu reserva.", reward = MysteryOutcome("¡Objetivo de Monedas completado! Ganas 5 monedas.", moneyChange = 5), type = "money", targetValue = 25),
    Objective(id = "obj_game_total_harvest_5", description = "Cosecha un total de 5 cultivos.", reward = MysteryOutcome("¡Objetivo de Cosecha Total completado! Ganas 1 Energía.", energyChange = 1), type = "total_harvest", targetValue = 5),
    Objective(id = "obj_game_total_harvest_10", description = "Cosecha un total de 10 cultivos.", reward = MysteryOutcome("¡Objetivo de Cosecha Total completado! Ganas 2 Energías.", energyChange = 2), type = "total_harvest", targetValue = 10),
    Objective(id = "obj_game_specific_zanahoria_3", description = "Cosecha 3 Zanahorias.", reward = MysteryOutcome("¡Objetivo de Cosecha de Zanahorias completado! Ganas 1 Energía y 2 monedas.", moneyChange = 2, energyChange = 1), type = "specific_harvest", targetValue = 3, targetCropId = "zanahoria"),
    Objective(id = "obj_game_specific_trigo_4", description = "Cosecha 4 Trigo Común.", reward = MysteryOutcome("¡Objetivo de Cosecha de Trigo Común completado! Ganas 3 monedas.", moneyChange = 3), type = "specific_harvest", targetValue = 4, targetCropId = "trigo"),
    Objective(id = "obj_game_mutant_harvest_1", description = "Cosecha 1 Cultivo Mutante.", reward = MysteryOutcome("¡Objetivo de Cosecha Mutante completado! Ganas 1 Energía.", energyChange = 1), type = "mutant_harvest", targetValue = 1),
    Objective(id = "obj_game_mutant_harvest_3", description = "Cosecha 3 Cultivos Mutantes.", reward = MysteryOutcome("¡Objetivo de Cosecha Mutante completado! Ganas 2 energías.", energyChange = 2), type = "mutant_harvest", targetValue = 3),
    Objective(id = "obj_game_dice_all_same", description = "Saca los 4 dados con el mismo símbolo.", reward = MysteryOutcome("¡Objetivo de Tirada Perfecta completado! Ganas 2 Energías.", energyChange = 2), type = "dice_roll_all_same", targetValue = 1),
    Objective(id = "obj_game_energy_5", description = "Acumula 5 Energías Glitch en tu reserva.", reward = MysteryOutcome("¡Objetivo de Energía completado! Ganas 2 monedas.", moneyChange = 2), type = "energy_count", targetValue = 5)
)

val allRoundObjectives = listOf(
    Objective(id = "obj_round_plant_2", description = "Planta 2 cultivos esta ronda.", reward = MysteryOutcome("Objetivo de Ronda completado. Ganas 1 moneda.", moneyChange = 1), type = "plant_count", targetValue = 2, isRoundObjective = true),
    Objective(id = "obj_round_plant_3", description = "Planta 3 cultivos esta ronda.", reward = MysteryOutcome("Objetivo de Ronda completado. Ganas 2 monedas.", moneyChange = 2), type = "plant_count", targetValue = 3, isRoundObjective = true),
    Objective(id = "obj_round_sell_1", description = "Vende 1 cultivo esta ronda.", reward = MysteryOutcome("Objetivo de Ronda completado. Ganas 1 energía.", energyChange = 1), type = "sell_count", targetValue = 1, isRoundObjective = true),
    Objective(id = "obj_round_sell_2", description = "Vende 2 cultivos esta ronda.", reward = MysteryOutcome("Objetivo de Ronda completado. Ganas 1 energía y 1 moneda.", moneyChange = 1, energyChange = 1), type = "sell_count", targetValue = 2, isRoundObjective = true),
    Objective(id = "obj_round_sell_mutant", description = "Vende 1 Cultivo Mutante esta ronda.", reward = MysteryOutcome("Objetivo de Ronda completado. Ganas 2 monedas y 1 energía.", moneyChange = 2, energyChange = 1), type = "sell_mutant_count", targetValue = 1, isRoundObjective = true),
    Objective(id = "obj_round_roll_energy", description = "Saca un dado de Energía (⚡).", reward = MysteryOutcome("Objetivo de Ronda completado. Ganas 2 monedas.", moneyChange = 2), type = "roll_specific_dice", targetValue = 1, targetCropId = "ENERGIA", isRoundObjective = true),
    Objective(id = "obj_round_roll_plantar", description = "Saca un dado de Plantar (🌱).", reward = MysteryOutcome("Objetivo de Ronda completado. Ganas 1 moneda.", moneyChange = 1), type = "roll_specific_dice", targetValue = 1, targetCropId = "PLANTAR", isRoundObjective = true),
    Objective(id = "obj_round_roll_glitch", description = "Saca un dado de Glitch (🌀).", reward = MysteryOutcome("Objetivo de Ronda completado. Ganas 1 energía.", energyChange = 1), type = "roll_specific_dice", targetValue = 1, targetCropId = "GLITCH", isRoundObjective = true),
    Objective(id = "obj_round_money_gain_3", description = "Aumenta tus monedas en 3 o más esta ronda.", reward = MysteryOutcome("Objetivo de Ronda completado. Ganas 1 energía.", energyChange = 1), type = "money_gain", targetValue = 3, isRoundObjective = true),
    Objective(id = "obj_round_money_gain_5", description = "Aumenta tus monedas en 5 o más esta ronda.", reward = MysteryOutcome("Objetivo de Ronda completado. Ganas 2 energías.", energyChange = 2), type = "money_gain", targetValue = 5, isRoundObjective = true),
    Objective(id = "obj_round_dice_2_same", description = "Saca 2 dados con el mismo símbolo.", reward = MysteryOutcome("Objetivo de Ronda completado. Ganas 1 moneda.", moneyChange = 1), type = "roll_same_dice", targetValue = 2, isRoundObjective = true),
    Objective(id = "obj_round_dice_3_same", description = "Saca 3 dados con el mismo símbolo.", reward = MysteryOutcome("Objetivo de Ronda completado. Ganas 2 monedas.", moneyChange = 2), type = "roll_same_dice", targetValue = 3, isRoundObjective = true),
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
        "Trigo Común" -> "trigo"
        "Patata Terrosa" -> "patata"
        "Tomate Cúbico" -> "tomateCubico"
        "Maíz Arcoíris" -> "maizArcoiris"
        "Brócoli Cristal" -> "brocoliCristal"
        "Pimiento Explosivo" -> "pimientoExplosivo"
        else -> ""
    }
}

