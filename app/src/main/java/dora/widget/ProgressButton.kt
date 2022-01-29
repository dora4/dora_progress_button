package dora.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.Gravity
import androidx.annotation.MainThread
import androidx.appcompat.widget.AppCompatButton
import java.text.DecimalFormat
import java.util.*

class ProgressButton @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {

    private var backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var hoverPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var cornerRadius = 0
    private var showBorder = false
    private var borderWidth = 10
    private var borderColor = textColors.defaultColor
    private var progressText: String = ""
    private var hoverTextColor = Color.WHITE
    private var progressBackgroundColor = Color.BLACK
    private var progressHoverColor = Color.GRAY
    private var borderBounds = RectF()
    private var hoverBounds = RectF()
    private var progress = 0f
    private var progressPercent = 0f
    private var targetProgress: Float = 0f
    private var maxProgress: Float = 0f
    private var minProgress: Float = 0f
    private var textBottomBorder = 0f
    private var state = STATE_NORMAL
    private var progressAnimator: ValueAnimator? = null
    private var onProgressChangeListener: OnProgressChangeListener? = null
    private var normalText = ""
    private var pausedText = ""
    private var finishedText = ""
    private var autoReset: Boolean = false
    private var timer = Timer()
    private var timerTask: TimerTask? = null
    private var lastNotifyProgressTime: Long = 0
    private val stateHandler = Handler(Looper.getMainLooper()) {
        when (it.what) {
            PROGRESS_UPDATE -> {
                lastNotifyProgressTime = System.currentTimeMillis()
                if (System.currentTimeMillis() - lastNotifyProgressTime < 100f) {
                    return@Handler false
                }
                onProgressChangeListener?.onPendingProgress(progressPercent)
            }
        }
        false
    }

    override fun onDraw(canvas: Canvas) {
        if (!isInEditMode) {
            drawBackground(canvas)
            if (state === STATE_NORMAL) {
                super.onDraw(canvas)
            } else if (state === STATE_PAUSE || state === STATE_PENDING || state === STATE_COMPLETE) {
                drawText(canvas)
            }
        }
    }

    /**
     * 模拟进度，每次递增1%。
     */
    fun mockProgress() {
        if (isNew()) {
            if (timerTask != null) {
                timer.cancel()
                timer = Timer()
            }
            timerTask = object : TimerTask() {
                override fun run() {
                    if (progress < 100f) {
                        progress += 1f
                    } else {
                        if (timer != null) {
                            timer.cancel()
                        }
                        return
                    }
                    post {
                        setProgressText("", progress)
                    }
                }
            }
            timer.schedule(timerTask, 0, 100)
        }
    }

    private fun drawBackground(canvas: Canvas) {
        val measuredWidth = measuredWidth
        val measuredHeight = measuredHeight
        val left = if (showBorder) borderWidth / 2 else 0
        val top = if (showBorder) borderWidth / 2 else 0
        val right = measuredWidth - if (showBorder) borderWidth / 2 else 0
        val bottom = measuredHeight - if (showBorder) borderWidth / 2 else 0
        borderBounds[left.toFloat(), top.toFloat(), right.toFloat()] = bottom.toFloat()
        hoverBounds.set(borderBounds.left, borderBounds.top,
                borderBounds.right, borderBounds.bottom)
        when (state) {
            STATE_NORMAL, STATE_COMPLETE -> {
                backgroundPaint.color = progressBackgroundColor
                canvas.drawRoundRect(
                        hoverBounds,
                        cornerRadius.toFloat(),
                        cornerRadius.toFloat(),
                        backgroundPaint
                )
            }
            STATE_PENDING -> {
                progressPercent = progress / maxProgress
                backgroundPaint.color = progressBackgroundColor
                canvas.drawRoundRect(
                        hoverBounds,
                        cornerRadius.toFloat(),
                        cornerRadius.toFloat(),
                        backgroundPaint
                )
                val rightGap = hoverBounds.right * progressPercent
                hoverPaint.strokeWidth = hoverBounds.height()
                hoverPaint.color = progressHoverColor
                hoverPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                canvas.drawLine(
                        hoverBounds.left, hoverBounds.height() / 2, rightGap,
                        hoverBounds.height() / 2, hoverPaint
                )
                hoverPaint.xfermode = null
                if (progressPercent < 0) {
                    return
                }
                if (progressPercent >= 1f) {
                    state = STATE_COMPLETE
                    invalidate()
                    onProgressChangeListener?.onFinish()
                    if (autoReset) {
                        reset()
                    }
                } else {
                    stateHandler.obtainMessage(PROGRESS_UPDATE).sendToTarget()
                }
            }
            STATE_PAUSE -> {
                onProgressChangeListener?.onPause()
            }
        }
        if (showBorder) {
            borderPaint.style = Paint.Style.STROKE
            borderPaint.color = borderColor
            borderPaint.strokeWidth = borderWidth.toFloat()
            canvas.drawRoundRect(
                    borderBounds,
                    cornerRadius.toFloat(),
                    cornerRadius.toFloat(),
                    borderPaint
            )
        }
    }

    private fun drawText(canvas: Canvas) {
        val y = canvas.height / 2 - (textPaint.descent() / 2 + textPaint.ascent() / 2)
        val textWidth = textPaint.measureText(progressText)
        textBottomBorder = y

        val hoverWidth = measuredWidth * progressPercent
        val start = (measuredWidth - textWidth) / 2
        val end = (measuredWidth + textWidth) / 2
        val hoverTextWidth = (textWidth - measuredWidth) / 2 + hoverWidth
        val textProgress = hoverTextWidth / textWidth
        if (hoverWidth <= start) {
            textPaint.shader = null
            textPaint.color = textColors.defaultColor
            textPaint.textSize = textSize
        } else if (start < hoverWidth && hoverWidth <= end) {
            val progressGradient = LinearGradient(
                    (measuredWidth - textWidth) / 2f, 0f,
                    (measuredWidth + textWidth) / 2f, 0f, intArrayOf(
                    hoverTextColor,
                    textColors.defaultColor
            ), floatArrayOf(textProgress, textProgress + 0.001f),
                    Shader.TileMode.CLAMP
            )
            textPaint.color = textColors.defaultColor
            textPaint.textSize = textSize
            textPaint.shader = progressGradient
        } else {
            textPaint.shader = null
            textPaint.color = hoverTextColor
            textPaint.textSize = textSize
        }

        when (state) {
            STATE_NORMAL -> {
                textPaint.shader = null
                textPaint.color = hoverTextColor
                canvas.drawText(progressText, (measuredWidth - textWidth) / 2, y, textPaint)
            }
            STATE_PENDING -> {
                canvas.drawText(progressText, (measuredWidth - textWidth) / 2, y, textPaint)
            }
            STATE_PAUSE -> {
                reset()
                text = pausedText
            }
            STATE_COMPLETE -> {
                textPaint.shader = null
                textPaint.color = textColors.defaultColor
                val finishedTextWidth = textPaint.measureText(finishedText)
                canvas.drawText(finishedText, (measuredWidth - finishedTextWidth) / 2, y, textPaint)
            }
        }
    }

    fun isNew(): Boolean {
        return state === STATE_NORMAL
    }

    fun isPause(): Boolean {
        return state === STATE_PAUSE
    }

    fun isPending(): Boolean {
        return state === STATE_PENDING
    }

    fun isFinish(): Boolean {
        return state === STATE_COMPLETE
    }

    fun pause() {
        if (state === STATE_PENDING) {
            state = STATE_PAUSE
        }
        invalidate()
    }

    fun getProgressPercent(): Float {
        return progressPercent
    }

    /**
     * 调用间隔时间建议不小于100毫秒。
     */
    @MainThread
    fun setProgressText(text: String, progress: Float) {
        if (progress < minProgress) {
            return
        }
        if (progress > maxProgress) {
            setProgressText(text, 100f)
        }
        if (state != STATE_PENDING) {
            state = STATE_PENDING
        }
        val format = DecimalFormat("##0.0")
        progressText = text + format.format(progress.toDouble()) + "%"
        targetProgress = progress
        if (progressAnimator!!.isRunning) {
            progressAnimator!!.resume()
            progressAnimator!!.start()
        } else {
            progressAnimator!!.start()
        }
        invalidate()
    }

    init {
        normalText = text.toString()
        if (!isInEditMode) {
            gravity = Gravity.CENTER
            val a = context.obtainStyledAttributes(attrs, R.styleable.ProgressButton, defStyleAttr, 0)
            cornerRadius = a.getDimensionPixelOffset(R.styleable.ProgressButton_dora_cornerRadius, cornerRadius)
            showBorder = a.getBoolean(R.styleable.ProgressButton_dora_showBorder, showBorder)
            borderWidth = a.getDimensionPixelOffset(R.styleable.ProgressButton_dora_borderWidth, borderWidth)
            borderColor = a.getColor(R.styleable.ProgressButton_dora_borderColor, borderColor)
            hoverTextColor = a.getColor(R.styleable.ProgressButton_dora_hoverTextColor, hoverTextColor)
            progressBackgroundColor = a.getColor(R.styleable.ProgressButton_dora_backgroundColor, progressBackgroundColor)
            progressHoverColor = a.getColor(R.styleable.ProgressButton_dora_hoverColor, progressHoverColor)
            pausedText = a.getString(R.styleable.ProgressButton_dora_pausedText) ?: ""
            finishedText = a.getString(R.styleable.ProgressButton_dora_finishedText) ?: ""
            autoReset = a.getBoolean(R.styleable.ProgressButton_dora_autoReset, autoReset)
            a.recycle()
            backgroundPaint.style = Paint.Style.FILL
            hoverPaint.style = Paint.Style.STROKE
            setLayerType(LAYER_TYPE_SOFTWARE, textPaint)
            progressAnimator = ValueAnimator.ofFloat(0f, 1f).setDuration(5)
            progressAnimator!!.addUpdateListener { animation ->
                val ratio = animation.animatedValue as Float
                progress += (targetProgress - progress) * ratio
                invalidate()
            }
            reset()
        }
    }

    interface OnProgressChangeListener {
        fun onPause()
        fun onPendingProgress(progress: Float)
        fun onFinish()
    }

    fun setOnProgressChangeListener(listener: OnProgressChangeListener) {
        this.onProgressChangeListener = listener
    }

    fun reset() {
        minProgress = 0f
        maxProgress = 100f
        progress = 0f
        state = STATE_NORMAL
        text = normalText
        invalidate()
    }

    companion object {
        const val STATE_NORMAL = 0
        const val STATE_PENDING = 1
        const val STATE_PAUSE = 2
        const val STATE_COMPLETE = 3
        const val PROGRESS_UPDATE = 0x10
    }
}