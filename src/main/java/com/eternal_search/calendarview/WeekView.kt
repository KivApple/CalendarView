package com.eternal_search.calendarview

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import java.util.*

class WeekView(context: Context, attrs: AttributeSet?, defStyleAttr: Int):
	TextGridView(context, attrs, defStyleAttr), TextGridView.OnCellClickedListener {
	
	private val weekDayFormatter = DateTimeFormat.forPattern("EE")
	private val firstDayOfWeek = (Calendar.getInstance().firstDayOfWeek + 5) % 7 + 1
	private val regularBorders = arrayOf(
		Border(BorderStyle.STROKE, -1.0f, Color.BLACK)
	)
	private val todayBorders = arrayOf(
		Border(BorderStyle.STROKE, -1.0f, Color.BLACK),
		Border(BorderStyle.STROKE, -3.0f, Color.GRAY)
	)
	private val activeBorders = arrayOf(
		Border(BorderStyle.FILL, -1.0f, Color.BLACK)
	)
	private val activeTodayBorders = arrayOf(
		Border(BorderStyle.FILL, -1.0f, Color.BLACK),
		Border(BorderStyle.STROKE, -3.0f, Color.GRAY)
	)
	var focusableIfListeningForClicks = true
	var onWeekDayClickedListener: OnWeekDayClickedListener? = null
		set(value) {
			if (focusableIfListeningForClicks && value != null) {
				isFocusable = true
			}
			field = value
		}
	private val highlightToday: Boolean
	var highlightAdapter: HighlightAdapter? = null
		set(value) {
			field?.view = null
			value?.view = this
			if (focusableIfListeningForClicks && (value as? SimpleHighlightAdapter)?.changeStateByClicks == true) {
				isFocusable = true
			}
			field = value
			adapter?.notifyDataSetChanged()
		}
	
	constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)
	
	constructor(context: Context): this(context, null)
	
	init {
		val values = context.obtainStyledAttributes(attrs, R.styleable.WeekView)
		highlightToday = values.getBoolean(R.styleable.WeekView_highlightToday, true)
		val regularColor = values.getColor(R.styleable.WeekView_regularColor, Color.GRAY)
		regularBorders[0].color = regularColor
		val activeColor = values.getColor(R.styleable.WeekView_activeColor, regularColor)
		activeBorders[0].color = activeColor
		val todayColor = values.getColor(R.styleable.WeekView_todayColor, Color.BLACK)
		todayBorders[0].color = todayColor
		todayBorders[1].color = applyAlpha(todayColor, 128)
		activeTodayBorders[0].color = todayColor
		activeTodayBorders[1].color = applyAlpha(todayColor, 128)
		values.recycle()
		adapter = Adapter()
		onCellClickedListener = this
	}
	
	fun setOnWeekDayClickedListener(callback: (weekDay: Int) -> Unit) {
		onWeekDayClickedListener = object : OnWeekDayClickedListener {
			override fun onWeekDayClicked(weekDay: Int) {
				callback(weekDay)
			}
		}
	}
	
	override fun onCellClicked(index: Int) {
		val weekDay = (index + firstDayOfWeek + 6) % 7 + 1
		(highlightAdapter as? SimpleHighlightAdapter)?.let { adapter ->
			if (adapter.changeStateByClicks) {
				adapter.state[weekDay - 1] = !adapter.state[weekDay - 1]
				adapter.notifyDataSetChanged()
			}
		}
		onWeekDayClickedListener?.onWeekDayClicked(weekDay)
	}
	
	private inner class Adapter: TextGridView.Adapter() {
		private val texts = LocalDate().let { now ->
			(0 until 7).map {
				weekDayFormatter.print(now.withDayOfWeek((firstDayOfWeek + it - 1) % 7 + 1))
			}
		}
		
		override fun getCount(): Int = 7
		
		override fun getColumnCount(): Int = getCount()
		
		override fun getText(index: Int): String = texts[index]
		
		override fun getBorders(index: Int): Array<Border> {
			val weekDayIndex = (firstDayOfWeek + index - 1) % 7 + 1
			val today = highlightToday && LocalDate().dayOfWeek == weekDayIndex
			val active = highlightAdapter?.isWeekDayHighlighted(weekDayIndex) == true
			return when {
				today && active -> activeTodayBorders
				today -> todayBorders
				active -> activeBorders
				else -> regularBorders
			}
		}
	}
	
	interface OnWeekDayClickedListener {
		fun onWeekDayClicked(weekDay: Int)
	}
	
	abstract class HighlightAdapter {
		internal var view: WeekView? = null
		
		open fun isWeekDayHighlighted(index: Int): Boolean = false
		
		fun notifyDataSetChanged() {
			view?.adapter?.notifyDataSetChanged()
		}
	}
	
	open class SimpleHighlightAdapter: HighlightAdapter() {
		val state = BooleanArray(7)
		var changeStateByClicks = false
		
		override fun isWeekDayHighlighted(index: Int): Boolean = state[index - 1]
	}
}
