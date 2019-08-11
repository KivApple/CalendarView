package com.eternal_search.calendarview

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

open class TextGridView(context: Context, attrs: AttributeSet?, defStyleAttr: Int): View(context, attrs, defStyleAttr) {
	private val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val textBounds = Rect()
	private val cellSize: Float
	private val cellMargin: Float
	private val borderWidth: Float
	private val headerHeight: Float
	private var scale: Float = 1.0f
	private var offsetX: Float = 0.0f
	private var offsetY: Float = 0.0f
	private var currentTouchingCell: Int = -1
	private val selectableItemBackground: Drawable?
	var onCellClickedListener: OnCellClickedListener? = null
	var adapter: Adapter? = null
		set(value) {
			field?.view = null
			value?.view = this
			field = value
			invalidate()
		}
	
	constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)
	
	constructor(context: Context): this(context, null)
	
	init {
		var values = context.obtainStyledAttributes(attrs, R.styleable.TextGridView)
		textPaint.textSize = values.getDimension(R.styleable.TextGridView_textSize,
			resources.getDimension(R.dimen.TextGridView_defaultTextSize))
		cellSize = values.getDimension(R.styleable.TextGridView_cellSize,
			resources.getDimension(R.dimen.TextGridView_defaultCellSize))
		cellMargin = values.getDimension(R.styleable.TextGridView_cellMargin,
			resources.getDimension(R.dimen.TextGridView_defaultCellMargin))
		borderWidth = values.getDimension(R.styleable.TextGridView_borderWidth,
			resources.getDimension(R.dimen.TextGridView_defaultBorderWidth))
		headerHeight = values.getDimension(R.styleable.TextGridView_headerHeight,
			resources.getDimension(R.dimen.TextGridView_defaultHeaderHeight))
		values.recycle()
		values = context.obtainStyledAttributes(attrs, intArrayOf(R.attr.selectableItemBackgroundBorderless))
		selectableItemBackground = values.getDrawable(0)
		selectableItemBackground?.callback = this
		values.recycle()
	}
	
	fun setOnCellClickedListener(callback: (index: Int) -> Unit) {
		onCellClickedListener = object : OnCellClickedListener {
			override fun onCellClicked(index: Int) {
				callback(index)
			}
		}
	}
	
	private fun getBackgroundColor(): Int {
		var view: View? = this
		while (view != null) {
			val colorDrawable = view.background as? ColorDrawable
			if (colorDrawable != null) {
				val color = colorDrawable.color
				if (color != Color.TRANSPARENT) {
					return colorDrawable.color
				}
			}
			view = view.parent as? View
		}
		return Color.WHITE
	}
	
	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val adapter = adapter ?: return super.onMeasure(widthMeasureSpec, heightMeasureSpec)
		val columnCount = adapter.getColumnCount()
		val rowCount = (adapter.getCount() + columnCount - 1) / columnCount
		val fullCellSize = cellSize + cellMargin
		val originalWidth = columnCount * fullCellSize
		val originalHeight = if (adapter.firstRowIsHeader())
			headerHeight + (rowCount - 1) * fullCellSize
		else
			rowCount * fullCellSize
		val targetWidth = MeasureSpec.getSize(widthMeasureSpec)
		val targetHeight = MeasureSpec.getSize(heightMeasureSpec)
		val widthMode = MeasureSpec.getMode(widthMeasureSpec)
		val heightMode = MeasureSpec.getMode(heightMeasureSpec)
		val scaleX = when (widthMode) {
			MeasureSpec.UNSPECIFIED -> 1.0f
			MeasureSpec.EXACTLY -> targetWidth.toFloat() / originalWidth
			MeasureSpec.AT_MOST -> if (targetWidth > originalWidth) 1.0f else targetWidth.toFloat() / originalWidth
			else -> throw IllegalArgumentException()
		}
		val scaleY = when (heightMode) {
			MeasureSpec.UNSPECIFIED -> 1.0f
			MeasureSpec.EXACTLY -> targetHeight.toFloat() / originalHeight
			MeasureSpec.AT_MOST -> if (targetHeight > originalHeight) 1.0f else targetHeight.toFloat() / originalHeight
			else -> throw IllegalArgumentException()
		}
		scale = max(scaleX, scaleY)
		val width = if (widthMode == MeasureSpec.EXACTLY)
			targetWidth.toFloat()
		else
			originalWidth * scale
		val height = if (heightMode == MeasureSpec.EXACTLY)
			targetHeight.toFloat()
		else
			originalHeight * scale
		offsetX = (width - originalWidth * scale) / 2.0f
		offsetY = (height - originalHeight * scale) / 2.0f
		setMeasuredDimension(width.toInt(), height.toInt())
	}
	
	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		val adapter = adapter ?: return
		
		canvas.translate(offsetX, offsetY)
		canvas.scale(scale, scale)
		
		val backgroundColor = getBackgroundColor()
		val foregroundColor = if (getColorBrightness(backgroundColor) > MAX_COLOR_BRIGHTNESS / 2)
			Color.BLACK
		else
			Color.WHITE
		val count = adapter.getCount()
		val columnCount = adapter.getColumnCount()
		val fullCellSize = cellSize + cellMargin
		
		var x = 0.0f
		var y = 0.0f
		for (i in 0 until count) {
			val cx = x + fullCellSize / 2
			val cy = y + (if (!adapter.firstRowIsHeader() || y > 0.0f) fullCellSize else headerHeight) / 2.0f
			var r = (if (!adapter.firstRowIsHeader() || y > 0.0f) cellSize else headerHeight) / 2.0f
			
			val borders = adapter.getBorders(i)
			for (border in borders) {
				if (border.width == Float.MIN_VALUE) continue
				r += if (border.width >= 0)
					border.width
				else
					this.borderWidth * -border.width
			}
			for (j in borders.lastIndex downTo 0) {
				val border = borders[j]
				if (border.width == Float.MIN_VALUE) continue
				val borderWidth = if (border.width >= 0)
					border.width
				else
					this.borderWidth * -border.width
				r -= borderWidth
				if (border.style == BorderStyle.STROKE) {
					shapePaint.color = backgroundColor
					shapePaint.style = Paint.Style.FILL
					canvas.drawCircle(cx, cy, r + borderWidth / 2.0f, shapePaint)
				}
				shapePaint.color = mixColors(border.color, backgroundColor)
				shapePaint.strokeWidth = borderWidth
				shapePaint.style = when (border.style) {
					BorderStyle.FILL -> Paint.Style.FILL_AND_STROKE
					BorderStyle.STROKE -> Paint.Style.STROKE
				}
				canvas.drawCircle(cx, cy, r + borderWidth / 2.0f, shapePaint)
			}
			
			val text = adapter.getText(i)
			textPaint.color = when {
				borders.isEmpty() -> foregroundColor
				borders.first().style == BorderStyle.FILL -> backgroundColor
				else -> mixColors(borders.first().color, backgroundColor)
			}
			textPaint.getTextBounds(text, 0, text.length, textBounds)
			canvas.drawText(text, cx - textBounds.exactCenterX(), cy - textBounds.exactCenterY(), textPaint)
			
			x += fullCellSize
			if ((i + 1) % columnCount == 0) {
				x = 0.0f
				y += if (adapter.firstRowIsHeader() && y == 0.0f) {
					headerHeight
				} else {
					fullCellSize
				}
			}
		}
		
		selectableItemBackground?.draw(canvas)
	}
	
	private fun getCellIndexFromMotionEvent(event: MotionEvent): Int {
		val adapter = adapter ?: return -1
		val fullCellSize = cellSize + cellMargin
		val touchX = (event.x - offsetX) / scale
		var touchY = (event.y - offsetY) / scale
		
		if (adapter.firstRowIsHeader()) {
			touchY += fullCellSize - headerHeight
		}
		
		val cellX = (touchX / fullCellSize).toInt()
		val cellY = (touchY / fullCellSize).toInt()
		
		val cellCount = adapter.getCount()
		val columnCount = adapter.getColumnCount()
		val rowCount = (cellCount + columnCount - 1) / columnCount
		if (!(cellX in 0 until columnCount && cellY in 0 until rowCount)) return -1
		
		val index = cellY * columnCount + cellX
		if (index >= cellCount) return -1
		return index
	}
	
	private fun getCellBounds(index: Int): Rect {
		val adapter = adapter ?: return Rect()
		val fullCellSize = cellSize + cellMargin
		val columnCount = adapter.getColumnCount()
		val x1 = (index % columnCount) * fullCellSize
		var y1 = (index / columnCount) * fullCellSize
		if (adapter.firstRowIsHeader() && y1 > 0.0f) {
			y1 -= fullCellSize - headerHeight
		}
		val x2 = x1 + fullCellSize
		val y2 = if (!adapter.firstRowIsHeader() || index >= adapter.getColumnCount())
			y1 + fullCellSize
		else
			y1 + headerHeight
		return Rect(x1.toInt(), y1.toInt(), x2.toInt(), y2.toInt())
	}
	
	private fun beginRippleEffect(bounds: Rect, touchX: Float, touchY: Float) {
		if (!isFocusable) return
		selectableItemBackground?.bounds = bounds
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			selectableItemBackground?.setHotspot(touchX, touchY)
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			(selectableItemBackground as? RippleDrawable)?.radius = ((cellSize + cellMargin) / 2.0f).toInt()
		}
		if (selectableItemBackground?.isStateful == true) {
			selectableItemBackground.state = STATE_ENABLED_FOCUSED_PRESSED
		}
		invalidate()
	}
	
	private fun endRippleEffect() {
		if (selectableItemBackground?.isStateful == true) {
			selectableItemBackground.state = STATE_ENABLED
			invalidate()
		}
	}
	
	override fun onTouchEvent(event: MotionEvent): Boolean {
		if (event.actionMasked == MotionEvent.ACTION_DOWN) {
			val adapter = adapter
			if (adapter != null) {
				currentTouchingCell = getCellIndexFromMotionEvent(event)
				if (!adapter.firstRowIsHeader() && currentTouchingCell >= 0 ||
					currentTouchingCell >= adapter.getColumnCount()) {
					
					val bounds = getCellBounds(currentTouchingCell)
					beginRippleEffect(bounds, (event.x - offsetX) / scale, (event.y - offsetY) / scale)
				}
			}
		} else if (event.actionMasked == MotionEvent.ACTION_MOVE) {
			val newTouchingCell = getCellIndexFromMotionEvent(event)
			if (newTouchingCell != currentTouchingCell) {
				currentTouchingCell = -1
				endRippleEffect()
			}
		}
		if (event.actionMasked == MotionEvent.ACTION_UP) {
			performClick()
		}
		if (event.actionMasked == MotionEvent.ACTION_UP || (event.actionMasked == MotionEvent.ACTION_CANCEL)) {
			currentTouchingCell = -1
			endRippleEffect()
		}
		return true
	}
	
	override fun performClick(): Boolean {
		if (currentTouchingCell >= 0) {
			if (onCellClickedListener?.onCellClicked(currentTouchingCell) != null) {
				return true
			}
		}
		return super.performClick()
	}
	
	interface OnCellClickedListener {
		fun onCellClicked(index: Int)
	}
	
	data class Border(
		var style: BorderStyle,
		var width: Float,
		var color: Int
	)
	
	enum class BorderStyle {
		STROKE,
		FILL
	}
	
	abstract class Adapter {
		internal var view: TextGridView? = null
		
		abstract fun getCount(): Int
		
		abstract fun getColumnCount(): Int
		
		abstract fun getText(index: Int): String
		
		abstract fun getBorders(index: Int): Array<Border>
		
		open fun firstRowIsHeader(): Boolean = false
		
		fun notifyDataSetChanged() {
			view?.invalidate()
		}
	}
	
	companion object {
		private const val MAX_COLOR_BRIGHTNESS = 255 * 255 * 3
		
		private val STATE_ENABLED_FOCUSED_PRESSED = intArrayOf(
			android.R.attr.state_enabled,
			android.R.attr.state_focused,
			android.R.attr.state_pressed
		)
		private val STATE_ENABLED = intArrayOf(
			android.R.attr.state_enabled
		)
		
		@JvmStatic
		internal fun mixColors(foreground: Int, background: Int): Int {
			val r1 = Color.red(foreground)
			val g1 = Color.green(foreground)
			val b1 = Color.blue(foreground)
			val a1 = Color.alpha(foreground)
			val r2 = Color.red(background)
			val g2 = Color.green(background)
			val b2 = Color.blue(background)
			val a2 = 255 - a1
			val r3 = r1 * a1 / 255 + r2 * a2 / 255
			val g3 = g1 * a1 / 255 + g2 * a2 / 255
			val b3 = b1 * a1 / 255 + b2 * a2 / 255
			return Color.rgb(r3, g3, b3)
		}
		
		@JvmStatic
		private fun getColorBrightness(color: Int): Int {
			val r = Color.red(color)
			val g = Color.green(color)
			val b = Color.blue(color)
			return r * r + g * g + b * b
		}
		
		@JvmStatic
		internal fun applyAlpha(color: Int, alpha: Int) =
			Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
	}
}
