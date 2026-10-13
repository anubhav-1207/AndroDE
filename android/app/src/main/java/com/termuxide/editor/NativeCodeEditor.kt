package com.termuxide.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.OverScroller
import androidx.core.view.GestureDetectorCompat
import com.termuxide.editor.treesitter.TokenType
import com.termuxide.editor.treesitter.TreeSitterEngine
import kotlin.math.max
import kotlin.math.min

/**
 * NativeCodeEditor:
 * High-performance Android Canvas-rendered code editor with custom gutter,
 * Tree-sitter incremental token coloring, line folding markers, and smooth scrolling.
 */
class NativeCodeEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val treeSitterEngine = TreeSitterEngine("rust")
    private var codeLines = mutableListOf<String>()

    // Paints
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E4E4E7")
        textSize = 34f
        typeface = Typeface.MONOSPACE
    }

    private val gutterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#52525B")
        textSize = 28f
        typeface = Typeface.MONOSPACE
    }

    private val gutterBgPaint = Paint().apply {
        color = Color.parseColor("#0E0E11")
    }

    private val currentLinePaint = Paint().apply {
        color = Color.parseColor("#18181D")
    }

    private val dividerPaint = Paint().apply {
        color = Color.parseColor("#27272A")
        strokeWidth = 2f
    }

    private val tokenPaints = mapOf(
        TokenType.KEYWORD to Paint(textPaint).apply { color = Color.parseColor("#F43F5E") },
        TokenType.FUNCTION to Paint(textPaint).apply { color = Color.parseColor("#60A5FA") },
        TokenType.TYPE to Paint(textPaint).apply { color = Color.parseColor("#34D399") },
        TokenType.STRING to Paint(textPaint).apply { color = Color.parseColor("#FBBF24") },
        TokenType.NUMBER to Paint(textPaint).apply { color = Color.parseColor("#FB923C") },
        TokenType.COMMENT to Paint(textPaint).apply { color = Color.parseColor("#71717A") },
        TokenType.OPERATOR to Paint(textPaint).apply { color = Color.parseColor("#E4E4E7") },
        TokenType.VARIABLE to Paint(textPaint).apply { color = Color.parseColor("#E4E4E7") },
        TokenType.PLAIN to Paint(textPaint)
    )

    // Layout metrics
    private var lineHeight = 52f
    private var gutterWidth = 120f
    private var currentLineIndex = 0

    // Scrolling
    private val scroller = OverScroller(context)
    private var scrollYOffset = 0f
    private var scrollXOffset = 0f
    private val gestureDetector: GestureDetectorCompat

    init {
        val sampleCode = """// TermuxIDE - Native Android Code Editor
// High-performance Tree-sitter incremental syntax highlighting
fn main() {
    let port: u16 = 8080;
    println!("Termux IDE running on port {}", port);
    for i in 0..10 {
        spawn_worker(i);
    }
}

fn spawn_worker(id: usize) {
    let status = format!("Worker #{} active", id);
    ipc_termux_exec(&status);
}"""
        setText(sampleCode, "rust")

        val listener = object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                val maxScrollY = max(0f, codeLines.size * lineHeight - height + 200f)
                scrollYOffset = min(max(0f, scrollYOffset + distanceY), maxScrollY)
                scrollXOffset = max(0f, scrollXOffset + distanceX)
                invalidate()
                return true
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val lineTapped = ((e.y + scrollYOffset) / lineHeight).toInt()
                if (lineTapped in codeLines.indices) {
                    currentLineIndex = lineTapped
                    invalidate()
                }
                return true
            }
        }
        gestureDetector = GestureDetectorCompat(context, listener)
    }

    fun setText(text: String, language: String) {
        this.codeLines = text.split("\n").toMutableList()
        this.treeSitterEngine.setLanguage(language)
        this.treeSitterEngine.invalidateRange(0, codeLines.size)
        this.currentLineIndex = 0
        this.scrollYOffset = 0f
        this.scrollXOffset = 0f
        invalidate()
    }

    fun getText(): String = codeLines.joinToString("\n")

    fun getLineCount(): Int = codeLines.size

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        // 1. Draw Gutter Background
        canvas.drawRect(0f, 0f, gutterWidth, viewHeight, gutterBgPaint)

        // 2. Draw Gutter Divider Line
        canvas.drawLine(gutterWidth, 0f, gutterWidth, viewHeight, dividerPaint)

        // Compute visible line range for 60fps rendering
        val firstVisibleLine = max(0, (scrollYOffset / lineHeight).toInt())
        val lastVisibleLine = min(
            codeLines.size - 1,
            ((scrollYOffset + viewHeight) / lineHeight).toInt() + 1
        )

        val fontMetrics = textPaint.fontMetrics
        val baselineOffset = (lineHeight - (fontMetrics.descent - fontMetrics.ascent)) / 2f - fontMetrics.ascent

        for (lineIdx in firstVisibleLine..lastVisibleLine) {
            val lineY = lineIdx * lineHeight - scrollYOffset

            // Draw current active line highlight
            if (lineIdx == currentLineIndex) {
                canvas.drawRect(
                    gutterWidth,
                    lineY,
                    viewWidth,
                    lineY + lineHeight,
                    currentLinePaint
                )
            }

            // Draw Line Number in Gutter
            val lineNumStr = (lineIdx + 1).toString()
            val numWidth = gutterPaint.measureText(lineNumStr)
            canvas.drawText(
                lineNumStr,
                gutterWidth - numWidth - 24f,
                lineY + baselineOffset,
                gutterPaint
            )

            // Draw Fold Marker if line ends with '{' or ':'
            val lineContent = codeLines[lineIdx]
            if (lineContent.trimEnd().endsWith("{") || lineContent.trimEnd().endsWith(":")) {
                canvas.drawText("▾", gutterWidth - 18f, lineY + baselineOffset, gutterPaint)
            }

            // Draw Syntax-Highlighted Tokens
            val tokens = treeSitterEngine.tokenizeLine(lineIdx, lineContent)
            var currentX = gutterWidth + 16f - scrollXOffset

            if (tokens.isEmpty()) {
                canvas.drawText(lineContent, currentX, lineY + baselineOffset, textPaint)
            } else {
                for (token in tokens) {
                    if (token.start >= lineContent.length) continue
                    val safeEnd = min(token.end, lineContent.length)
                    val tokenText = lineContent.substring(token.start, safeEnd)
                    val paint = tokenPaints[token.type] ?: textPaint

                    canvas.drawText(tokenText, currentX, lineY + baselineOffset, paint)
                    currentX += paint.measureText(tokenText)
                }
            }
        }
    }
}
