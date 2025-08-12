package com.pingu.tfg_glitch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pingu.tfg_glitch.ui.theme.AccentPurple
import com.pingu.tfg_glitch.ui.theme.AccentYellow
import com.pingu.tfg_glitch.ui.theme.DarkCard
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import com.pingu.tfg_glitch.ui.theme.TextLight
import com.pingu.tfg_glitch.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(onBack: () -> Unit) {
    val gameManualContent = listOf(
        "1. Introducción al Juego",
        "¡Bienvenido a Granja Glitch: Desafío de Semillas Corruptas! En este juego, encarnas a un granjero que ha heredado una parcela de tierra afectada por extraños 'glitches'. Estos fallos no solo alteran tus cultivos, dándoles formas y propiedades inusuales, sino que también atraen a criaturas peculiares. Tu objetivo es dominar este caos, manipulando el volátil mercado de alimentos (¡incluyendo los mutados!) para convertir el 'glitch' en ganancia. ¿Podrás prosperar antes de que la granja sucumba al caos total?",
        "Granja Glitch es un juego dinámico de gestión de mano, tirada de dados y adaptación al mercado, donde la aplicación es una parte integral y necesaria de la experiencia.",
        "Número de Jugadores: 2-4",
        "Duración de la Partida: 25-45 minutos (hasta 60 minutos con jugadores nuevos o en partidas muy competitivas)",
        "Edad Recomendada: 10+",
        "Objetivo del Juego: Acumular la mayor cantidad de Puntos de Victoria (PV) al final de la partida, obtenidos principalmente a través de la venta de cultivos (normales y mutados) en el Mercado Glitch de la aplicación y el cumplimiento de Objetivos de Juego.",
        "",
        "2. Componentes del Juego",
        "Antes de comenzar, asegúrate de tener todos los componentes necesarios:",
        "Componentes Físicos:",
        "Cartas de Granjero/Habilidad (4): Una para cada jugador. Cada carta representa un personaje único con una habilidad pasiva y una habilidad activable.",
        "Cartas de Semilla Corrupta (aprox. 60-80): El mazo principal de donde los jugadores obtendrán sus cultivos. Contienen una mezcla de:",
        "Cultivos Normales: (Ej: Trigo, Maíz, Patata)",
        "Cultivos Mutados/Glitch: (Ej: Tomate Cuadrado, Maíz Arcoíris, Brócoli Cristal, Pimiento Explosivo) – Estos tienen valores base diferentes y pueden tener efectos especiales al ser cosechados o vendidos.",
        "Cartas de Evento Glitch (aprox. 15-20): Se mezclan en el mazo de Semilla Corrupta. Activan efectos negativos o desafíos cuando se revelan.",
        "Dados Personalizados (4 por jugador): Con los siguientes símbolos en sus caras:",
        "🌀 (Glitch): Activa una habilidad Glitch o roba una Carta de Evento Glitch.",
        "➕ (Crecimiento): Permite añadir un marcador de crecimiento a un cultivo.",
        "⚡ (Energía): Permite ganar un token de Energía Glitch.",
        "💰 (Moneda): Permite ganar una moneda.",
        "❓ (Misterio): La aplicación revela un efecto aleatorio.",
        "Tokens de Cultivo (Aprox. 60-70): Representan los cultivos cosechados. Tendrás tokens específicos para cada uno de los tipos de cultivo disponibles. Estos tokens se añaden a tu reserva personal al cosechar un cultivo y se devuelven a la reserva central al venderlo o usarlos como coste.",
        "Tokens de Energía Glitch (aprox. 20): Representan la energía necesaria para activar ciertas habilidades.",
        "Tokens de Moneda (aprox. 50): Representan el dinero del juego.",
        "Marcadores de Crecimiento (aprox. 30): Pequeñas fichas para colocar sobre las Cartas de Cultivo.",
        "Fichas de Jugador (4): Una para cada jugador, para marcar el turno o la posición.",
        "La Aplicación (App):",
        "La aplicación 'Granja Glitch' es el corazón del juego. Necesitarás un dispositivo (smartphone o tablet) con la aplicación instalada.",
        "Mercado Glitch Dinámico: Muestra los precios actuales de todos los cultivos (normales y mutados). Estos precios fluctúan constantemente, influenciados por la oferta y la demanda global (simulada por la app) y los eventos.",
        "Generador de Eventos de Corrupción: Introduce eventos globales o específicos de jugador que afectan el juego.",
        "Desafíos de Habilidad: Algunos efectos de cartas o dados activan mini-desafíos interactivos en la app.",
        "Seguimiento de Puntuación y Objetivos: La app lleva el recuento de los Puntos de Victoria de cada jugador y muestra los Objetivos de Juego activos.",
        "Tutorial y Reglas: Incluye un resumen de las reglas y un tutorial interactivo.",
        "Inventario Digital: Los jugadores registran sus cultivos cosechados y su dinero en la app para facilitar las ventas y el seguimiento.",
        "",
        "3. Preparación del Juego",
        "Sigue estos pasos para preparar tu partida de Granja Glitch:",
        "Configurar la Aplicación:",
        "Abre la aplicación 'Granja Glitch' en tu dispositivo.",
        "Selecciona 'Nueva Partida'.",
        "Introduce el número de jugadores.",
        "La app te guiará para seleccionar los Granjeros/Personajes (ver paso 2).",
        "Elegir Granjero/Personaje:",
        "Cada jugador elige una Carta de Granjero/Habilidad. Lee su habilidad pasiva y activable. Colócala frente a ti.",
        "En la app, selecciona el Granjero/Personaje que has elegido. La app inicializará tu inventario y habilidades.",
        "Preparar los Mazos de Cartas:",
        "Separa las Cartas de Semilla Corrupta de las Cartas de Evento Glitch.",
        "Baraja bien el mazo de Cartas de Evento Glitch.",
        "Baraja bien el mazo de Cartas de Semilla Corrupta.",
        "Ahora, para crear el mazo de robo principal: Toma 1 Carta de Evento Glitch por cada 2 jugadores (redondeando hacia arriba) y mézclalas con el mazo de Semilla Corrupta. Coloca el resto de las Cartas de Evento Glitch a un lado, boca abajo (estas no se usarán a menos que un efecto las pida).",
        "Coloca el mazo de robo principal (Semilla Corrupta + Eventos Glitch) en el centro de la mesa, boca abajo.",
        "Preparar los Recursos:",
        "Coloca los tokens de Cultivo, Energía Glitch, Monedas y Marcadores de Crecimiento en una reserva central accesible para todos los jugadores.",
        "Repartir Componentes Iniciales:",
        "Cada jugador recibe 3 Cartas de Semilla Corrupta de la parte superior del mazo de robo principal. Mantenlas en tu mano.",
        "Cada jugador recibe 5 Monedas y 1 Token de Energía Glitch de la reserva.",
        "Cada jugador toma sus 4 Dados Personalizados.",
        "Determinar el Primer Jugador:",
        "El jugador más joven es el primer jugador. Dale la Ficha de Jugador inicial.",
        "La app indicará el orden de los turnos.",
        "",
        "4. Flujo de Juego",
        "El juego se desarrolla a lo largo de una serie de rondas. Cada ronda consta de las siguientes fases:",
        "Fase de Evento Glitch (Solo App)",
        "Fase de Tirada de Dados (Todos los Jugadores)",
        "Fase de Acciones (Por Turno)",
        "Fase de Mercado Glitch (App y Jugadores)",
        "4.1. Fase de Evento Glitch (Solo App)",
        "Al comienzo de cada ronda (o cuando la app lo indique), la aplicación puede generar un Evento de Corrupción. Estos eventos pueden:",
        "Afectar los precios del mercado.",
        "Otorgar o quitar recursos a los jugadores.",
        "Introducir una regla temporal.",
        "Activar un mini-desafío para todos los jugadores.",
        "La app mostrará el evento y sus efectos. Resuelve el evento antes de pasar a la siguiente fase.",
        "4.2. Fase de Tirada de Dados (Todos los Jugadores)",
        "Todos los jugadores, al mismo tiempo, tiran sus 4 Dados Personalizados.",
        "Reroll (Opcional): Una vez por ronda, cada jugador puede elegir volver a tirar cualquier número de sus dados, pero solo una vez. Debes decidir qué dados quieres volver a tirar antes de lanzarlos de nuevo.",
        "4.3. Fase de Acciones (Por Turno)",
        "Comenzando por el primer jugador y continuando en el sentido de las agujas del reloj, cada jugador realiza su turno. Durante tu turno, puedes usar los símbolos de tus dados para realizar acciones, o elegir una de las nuevas acciones de pago disponibles. Puedes usar tus dados en cualquier orden.",
        "Acciones Disponibles (basadas en los símbolos de los dados):",
        "🌱 (Plantar):",
        "Acción: Juega 1 Carta de Cultivo de tu mano y colócala boca arriba en tu área de juego (tu 'parcela').",
        "Costo: Cada Carta de Cultivo tiene un costo de plantado (indicado en la carta). Paga este costo en Monedas a la reserva.",
        "Crecimiento Inicial: Coloca 1 Marcador de Crecimiento sobre la carta de Cultivo recién plantada.",
        "Límite: No hay límite de cuántos cultivos puedes tener plantados, pero cada uno debe tener espacio en tu área de juego.",
        "➕ (Crecimiento):",
        "Acción: Elige 1 Carta de Cultivo plantada en tu parcela y añade 1 Marcador de Crecimiento adicional sobre ella.",
        "Cosecha Automática: Si al añadir un Marcador de Crecimiento, el número total de marcadores sobre la carta iguala o supera su valor de 'Crecimiento Requerido' (indicado en la carta), el cultivo está listo para ser cosechado.",
        "Cosechar: Retira la Carta de Cultivo de tu parcela y guárdala en tu pila de descarte personal. Toma el token físico correspondiente a ese tipo de cultivo de la reserva central y añádelo a tu reserva personal de recursos. Luego, regístralo en tu inventario en la aplicación.",
        "Nota: No puedes cosechar un cultivo que no haya alcanzado su crecimiento requerido.",
        "⚡ (Energía):",
        "Acción: Gana 1 Token de Energía Glitch de la reserva y añádelo a tu reserva personal.",
        "Uso: Los Tokens de Energía Glitch se utilizan para activar las habilidades activables de tu Granjero/Personaje o ciertas habilidades de Cartas de Cultivo Mutado.",
        "💰 (Moneda):",
        "Acción: Gana 2 Monedas de la reserva y añádelas a tu reserva personal.",
        "🌀 (Glitch):",
        "Acción: Tienes dos opciones al usar un dado 🌀:",
        "Activar Habilidad Glitch: Si tu Granjero/Personaje tiene una 'Habilidad Glitch' activable (indicada en tu carta o en la app), puedes activarla ahora. Algunas habilidades Glitch pueden requerir el gasto de Tokens de Energía Glitch.",
        "Robar Carta de Evento Glitch: Roba la carta superior del mazo de robo principal. Si es una Carta de Semilla Corrupta, añádela a tu mano. Si es una Carta de Evento Glitch, resuélvela inmediatamente. Los efectos de las Cartas de Evento Glitch pueden ser variados (perder recursos, afectar precios, etc.). Una vez resuelta, la Carta de Evento Glitch se descarta.",
        "❓ (Misterio):",
        "Acción: Activa un efecto aleatorio en la aplicación. La app te presentará una opción o un resultado inesperado (ej: 'Encuentras una bolsa de semillas extrañas', 'Un pequeño temblor afecta tus cultivos', 'Un mercader misterioso aparece'). Sigue las instrucciones de la app.",
        "Acción de Compra de Crecimiento (sin dado):",
        "Acción: En cualquier momento durante tu Fase de Acciones, puedes pagar 3 Monedas de tu reserva para elegir un cultivo plantado en tu parcela y añadirle 1 Marcador de Crecimiento. Solo puedes realizar esta acción una vez por turno y no consume un dado.",
        "Habilidades de Granjero/Personaje:",
        "Habilidad Pasiva: Siempre está activa y no requiere dados ni costo.",
        "Habilidad Activable: Puede activarse gastando el símbolo 🌀 de un dado o pagando un costo específico (ej: 1 Energía Glitch). Solo puedes activar cada habilidad activable una vez por turno, a menos que se indique lo contrario.",
        "Cartas de Cultivo Mutado/Glitch:",
        "Algunas Cartas de Cultivo Mutado pueden tener habilidades especiales que se activan al ser plantadas, al ser cosechadas, o al ser vendidas. Lee atentamente el texto de la carta. Algunas de estas habilidades requieren descartar uno o más tokens de cultivo de tu reserva personal para activarse.",
        "4.4. Fase de Mercado Glitch (App y Jugadores)",
        "Una vez que todos los jugadores han completado su Fase de Acciones, comienza la Fase de Mercado Glitch. Esta fase se gestiona principalmente a través de la aplicación.",
        "Actualización del Mercado (App): La app actualizará automáticamente los precios de todos los cultivos (normales y mutados) basándose en la oferta y la demanda simulada, así como en los eventos.",
        "Venta de Cultivos:",
        "Los jugadores pueden vender cualquier cantidad de sus cultivos cosechados (registrados en la app y en su reserva física) al precio actual del mercado.",
        "Para vender, selecciona los cultivos en tu inventario en la app y confirma la venta. La app calculará las Monedas obtenidas y las añadirá a tu total. Debes devolver a la reserva central los tokens físicos que vendas.",
        "Importante: No hay límite de cuántos cultivos puedes vender en tu turno, siempre que los tengas cosechados y registrados en la app.",
        "Cumplimiento de Objetivos de Juego (App):",
        "La app mostrará una serie de Objetivos de Juego activos. Estos objetivos pueden requerir la venta de ciertas combinaciones de cultivos, la acumulación de una cantidad específica de Monedas, o la activación de ciertas habilidades.",
        "Si cumples los requisitos de un objetivo, la app te permitirá 'reclamarlo' para ganar Puntos de Victoria adicionales.",
        "Cada objetivo solo puede ser reclamado una vez por jugador (a menos que se especifique lo contrario).",
        "Una vez que todos los jugadores han tenido la oportunidad de vender y cumplir objetivos, la ronda termina. La Ficha de Jugador inicial pasa al siguiente jugador en el sentido de las agujas del reloj, y comienza una nueva ronda.",
        "",
        "5. Fin de la Partida",
        "La partida de Granja Glitch termina inmediatamente cuando se cumple una de las siguientes condiciones:",
        "Mazo de Semillas Agotado: El mazo de robo principal (Cartas de Semilla Corrupta + Evento Glitch) se agota y no quedan más cartas para robar.",
        "Objetivos Completados: Un número predeterminado de Objetivos de Juego (indicado en la configuración de la app al inicio de la partida, ej: 3 o 4 objetivos) han sido reclamados por cualquier jugador.",
        "Colapso de la Granja (Opcional/Variante de Dificultad): Si la app introduce una 'barra de colapso' y esta llega a su máximo debido a eventos de corrupción o fallos en desafíos. (Esta es una variante para partidas más desafiantes).",
        "Una vez que se dispara una de estas condiciones, se procede a la puntuación final.",
        "6. Puntuación Final y Victoria",
        "La aplicación calculará automáticamente los Puntos de Victoria (PV) de cada jugador. Los PV se obtienen de:",
        "Monedas: Cada 5 Monedas restantes en tu posesión valen 1 PV (redondeando hacia abajo).",
        "Objetivos de Juego Cumplidos: Los PV indicados en cada Objetivo de Juego que hayas reclamado.",
        "Cultivos Cosechados No Vendidos: Cada cultivo cosechado que aún tengas en tu inventario de la app y en tu reserva física tiene un valor base de PV (indicado en la carta o en la app).",
        "Habilidades/Cartas Específicas: Algunas Cartas de Granjero o Cartas de Cultivo Mutado pueden otorgar PV adicionales al final del juego.",
        "El jugador con la mayor cantidad de Puntos de Victoria es el ganador de Granja Glitch: Desafío de Semillas Corruptas y el granjero más astuto para dominar el caos.",
        "En caso de empate: El jugador con más Monedas restantes gana. Si el empate persiste, el jugador con más tokens de Energía Glitch restantes gana. Si aún hay empate, los jugadores comparten la victoria.",
        "",
        "7. Reglas Adicionales y Aclaraciones",
        "Límite de Mano: No hay límite de cartas en tu mano.",
        "Recursos Físicos vs. Digitales: El dinero (Monedas) y la Energía Glitch se gestionan con tokens físicos. Los cultivos plantados en tu parcela se representan con la carta y marcadores de crecimiento. Una vez cosechado, el cultivo se convierte en un token físico de ese tipo de cultivo, que guardas en tu reserva personal y que también se registra en tu inventario digital en la app.",
        "Interacciones con la App: Siempre sigue las instrucciones de la app. Si hay un conflicto entre el manual y una instrucción específica de la app durante un evento o desafío, la app tiene prioridad.",
        "Habilidades de un Solo Uso: Si una habilidad de carta o personaje indica 'de un solo uso', la carta o la habilidad se descarta/inhabilita después de su uso.",
        "Descartes: Las Cartas de Cultivo plantadas se descartan a tu pila personal cuando se cosechan. Las Cartas de Evento Glitch se descartan a una pila central después de ser resueltas.",
        "Comunicación: Los jugadores pueden discutir estrategias, pero no están obligados a revelar sus manos o planes exactos."
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Reglas del Juego") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al menú principal"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(gameManualContent) { paragraph ->
                Text(
                    text = paragraph,
                    fontSize = 16.sp,
                    color = TextLight,
                    textAlign = TextAlign.Justify
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RulesScreenPreview() {
    GranjaGlitchAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            RulesScreen(onBack = {})
        }
    }
}
