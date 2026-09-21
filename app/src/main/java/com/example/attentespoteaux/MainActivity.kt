package com.example.attentespoteaux

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) { AttentesScreen() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttentesScreen() {
    var colWidth by remember { mutableStateOf("30") }
    var colDepth by remember { mutableStateOf("30") }
    var barDia by remember { mutableStateOf("16") }
    var numBars by remember { mutableStateOf("4") }
    var fc28 by remember { mutableStateOf("25") }
    var fe by remember { mutableStateOf("500") }
    var isHA by remember { mutableStateOf(true) }
    var footHeight by remember { mutableStateOf("40") }
    var footWidth by remember { mutableStateOf("120") }
    var cover by remember { mutableStateOf("4") }
    var result by remember { mutableStateOf<CalculationResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attentes poteau / semelle") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard("🏛️ Poteau") {
                NumberField("Largeur poteau b (cm)", colWidth) { colWidth = it }
                NumberField("Profondeur poteau h (cm)", colDepth) { colDepth = it }
                NumberField("Ø barres (mm)", barDia) { barDia = it }
                NumberField("Nombre de barres", numBars) { numBars = it }
            }

            SectionCard("🧱 Matériaux") {
                NumberField("Résistance béton fc28 (MPa)", fc28) { fc28 = it }
                NumberField("Limite élastique acier fe (MPa)", fe) { fe = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Acier HA ?", modifier = Modifier.weight(1f))
                    Switch(checked = isHA, onCheckedChange = { isHA = it })
                }
            }

            SectionCard("🧩 Semelle") {
                NumberField("Hauteur semelle (cm)", footHeight) { footHeight = it }
                NumberField("Largeur semelle (cm)", footWidth) { footWidth = it }
                NumberField("Enrobage (cm)", cover) { cover = it }
            }

            // Aperçu graphique
            FerraillagePreview(
                colWidthCm = colWidth.toFloatOrNull() ?: 30f,
                footWidthCm = footWidth.toFloatOrNull() ?: 120f,
                footHeightCm = footHeight.toFloatOrNull() ?: 40f
            )

            Button(
                onClick = {
                    error = null
                    try {
                        val input = CalculationInput(
                            columnWidthCm = colWidth.toDouble(),
                            columnDepthCm = colDepth.toDouble(),
                            barDiameterMm = barDia.toDouble(),
                            numBars = numBars.toInt(),
                            fc28MPa = fc28.toDouble(),
                            feMPa = fe.toDouble(),
                            isHighAdherence = isHA,
                            footingHeightCm = footHeight.toDouble(),
                            footingWidthCm = footWidth.toDouble(),
                            coverCm = cover.toDouble(),
                            lapCoeff = if (isHA) 1.0 else 1.5
                        )
                        result = AnchorageCalculator.calculate(input)
                    } catch (e: Exception) {
                        error = "Veuillez vérifier les valeurs saisies."
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("CALCULER", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            result?.let { ResultsCard(it) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            content()
        }
    }
}

@Composable
fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ResultsCard(r: CalculationResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("📊 Résultats", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            HorizontalDivider()
            ResultLine("ft28", "%.2f MPa".format(r.ft28MPa))
            ResultLine("τsu", "%.2f MPa".format(r.tauSuMPa))
            ResultLine("fsu", "%.1f MPa".format(r.fsuMPa))
            ResultLine("ls minimum réglementaire", "%.2f cm".format(r.lsMinCm))
            HorizontalDivider()
            ResultLine("Ancrage droit", "%.2f cm".format(r.straightAnchorageCm))
            ResultLine("Crochet 90°", "%.2f cm".format(r.hook90TotalCm))
            ResultLine("Crochet 180°", "%.2f cm".format(r.hook180TotalCm))
            ResultLine("Recouvrement poteau", "%.2f cm".format(r.lapLengthCm))
            HorizontalDivider()
            ResultLine("Ancrage dans semelle", "%.2f cm".format(r.embedDepthRequiredCm), true)
            ResultLine("LONGUEUR TOTALE", "%.2f cm".format(r.totalWaitLengthCm), true)
            r.warning?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = if (r.isValid) Color(0xFF7A5C00) else Color.Red,
                    fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ResultLine(label: String, value: String, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) MaterialTheme.colorScheme.primary else Color.Unspecified)
    }
}

@Composable
fun FerraillagePreview(colWidthCm: Float, footWidthCm: Float, footHeightCm: Float) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🧱 Aperçu du ferraillage", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Canvas(
                modifier = Modifier.fillMaxWidth().height(320.dp)
                    .background(Color(0xFFFAFAFA))
            ) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val scale = (w * 0.8f) / footWidthCm
                val footWpx = footWidthCm * scale
                val footHpx = footHeightCm * scale
                val colWpx = colWidthCm * scale
                val colHpx = h * 0.35f
                val footTop = h * 0.70f
                val footBottom = footTop + footHpx
                val colTop = footTop - colHpx

                // Sol
                drawRect(Color(0xFFE8D9B8), Offset(0f, footBottom + 12f),
                    Size(w, h - footBottom - 12f))
                // Semelle
                drawRect(Color(0xFFF0F0F0), Offset(cx - footWpx/2, footTop),
                    Size(footWpx, footHpx))
                drawRect(Color(0xFF333333), Offset(cx - footWpx/2, footTop),
                    Size(footWpx, footHpx), style = Stroke(2f))
                // Nappe inférieure
                drawLine(Color(0xFFC0392B), Offset(cx - footWpx/2 + 8f, footBottom - 15f),
                    Offset(cx + footWpx/2 - 8f, footBottom - 15f), 4f)
                drawLine(Color(0xFFC0392B), Offset(cx - footWpx/2 + 8f, footBottom - 20f),
                    Offset(cx + footWpx/2 - 8f, footBottom - 20f), 4f)
                // Poteau
                drawRect(Color(0xFFEAF3FB), Offset(cx - colWpx/2, colTop),
                    Size(colWpx, colHpx))
                drawRect(Color(0xFF333333), Offset(cx - colWpx/2, colTop),
                    Size(colWpx, colHpx), style = Stroke(2f))
                // Cadres
                for (i in 0 until 6) {
                    val y = colTop + 20f + i * (colHpx - 30f) / 5f
                    drawLine(Color(0xFF2C3E50), Offset(cx - colWpx/2 + 6f, y),
                        Offset(cx + colWpx/2 - 6f, y), 1.5f)
                }
                // Barres + attentes
                val barOff = colWpx/2 - 12f
                val xL = cx - barOff
                val xR = cx + barOff
                drawLine(Color(0xFFE67E22), Offset(xL, colTop + 8f), Offset(xL, footTop), 5f)
                drawLine(Color(0xFFE67E22), Offset(xR, colTop + 8f), Offset(xR, footTop), 5f)
                val attEnd = footTop + footHpx * 0.55f
                drawLine(Color(0xFFE67E22), Offset(xL, footTop), Offset(xL, attEnd), 5f)
                drawLine(Color(0xFFE67E22), Offset(xR, footTop), Offset(xR, attEnd), 5f)
                // Crochets
                val pL = Path().apply {
                    moveTo(xL, attEnd); lineTo(xL, attEnd + 12f); lineTo(xL + 20f, attEnd + 12f)
                }
                drawPath(pL, Color(0xFFE67E22), style = Stroke(5f))
                val pR = Path().apply {
                    moveTo(xR, attEnd); lineTo(xR, attEnd + 12f); lineTo(xR - 20f, attEnd + 12f)
                }
                drawPath(pR, Color(0xFFE67E22), style = Stroke(5f))
                // Zone recouvrement
                drawRect(Color(0xFFFFF3CD), Offset(cx - colWpx/2 - 10f, footTop - 30f),
                    Size(colWpx + 20f, 30f))
                drawRect(Color(0xFFE0A800), Offset(cx - colWpx/2 - 10f, footTop - 30f),
                    Size(colWpx + 20f, 30f), style = Stroke(1.5f))
                // Flèche
                drawLine(Color(0xFF333333), Offset(cx, 20f), Offset(cx, colTop - 10f), 3f)
                val arrow = Path().apply {
                    moveTo(cx - 8f, colTop - 20f); lineTo(cx, colTop - 8f)
                    lineTo(cx + 8f, colTop - 20f); close()
                }
                drawPath(arrow, Color(0xFF333333))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendItem(Color(0xFFE67E22), "Attentes")
                LegendItem(Color(0xFFC0392B), "Nappe")
                LegendItem(Color(0xFF2C3E50), "Cadres")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(14.dp)) { drawRect(color) }
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp)
    }
}
