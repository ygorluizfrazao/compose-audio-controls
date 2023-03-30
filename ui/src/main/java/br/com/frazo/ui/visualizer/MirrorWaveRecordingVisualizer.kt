package br.com.frazo.ui.visualizer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path

class MirrorWaveRecordingVisualizer(
    private val samples: Int = 50,
    private val wavePaint: Paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 2f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        flags = Paint.ANTI_ALIAS_FLAG
        strokeJoin = Paint.Join.BEVEL
    },
    private val middleLinePaint: Paint? = null
) : RecordingVisualizer {

    override fun drawGraphics(
        canvas: Canvas,
        width: Float,
        height: Float,
        offsetX: Float,
        offsetY: Float,
        amplitudes: List<Float>
    ) {

        val midHeight = height/2f
        middleLinePaint?.let {
            canvas.drawLine(0f, midHeight, width, midHeight, it)
        }

        val drawSamplesList =
            if (amplitudes.size >= samples) {
                amplitudes.takeLast(samples).reversed()
            } else {
                (List(samples - amplitudes.size) { 0f } + amplitudes).reversed()
            }

        val maxAmp =
            amplitudes.maxOfOrNull { it }
        maxAmp?.let {

            var lastXR = width / 2f
            var lastXL = width / 2f
            val stepX = width / 2f / samples

            val pathR = Path().apply { moveTo(width/2,midHeight - ((drawSamplesList.first() / maxAmp) * midHeight)) }
            val pathL = Path().apply { moveTo(width/2,midHeight - ((drawSamplesList.first() / maxAmp) * midHeight)) }

            drawSamplesList
                .takeLast(samples-1)
                .forEach {
                    val nextY =
                        midHeight - ((it / maxAmp) * midHeight)
                    pathL.lineTo(lastXL + stepX,nextY)
                    pathR.lineTo(lastXR + stepX,nextY)
                    lastXR += stepX
                    lastXL -= stepX
                }
            val finalPath = Path().apply {
                addPath(pathL)
                addPath(pathR)
            }
            canvas.drawPath(finalPath,wavePaint)
        }
    }
}