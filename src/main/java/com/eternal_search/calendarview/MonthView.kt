package com.eternal_search.calendarview

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import java.util.*

class MonthView(context: Context, attrs: AttributeSet?, defStyleAttr: Int): TextGridView(context, attrs, defStyleAttr),
		TextGridView.OnCellClickedListener {
	private val weekDayFormatter = DateTimeFormat.forPattern("EE")
	private val firstDayOfWeek = (Calendar.getInstance().firstDayOfWeek + 5) % 7 + 1
	private val emptyBorders = arrayOf(
		Border(BorderStyle.STROKE, Float.MIN_VALUE, Color.BLACK)
	)
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
	var date: LocalDate = LocalDate()
		set(value) {
			field = value
			updateTexts()
			adapter?.notifyDataSetChanged()
		}
	private lateinit var dateStart: LocalDate
	private var monthStartIndex: Int = -1
	private var monthStopIndex: Int = -1
	private var todayIndex: Int = -1
	private val texts = mutableListOf<String>()
	var highlightAdapter: HighlightAdapter? = null
		set(value) {
			field?.view = null
			value?.view = this
			field = value
			value?.notifyDataSetChanged()
		}
	var focusableIfListeningForClicks = true
	var onDateClickedListener: OnDateClickedListener? = null
		set(value) {
			if (focusableIfListeningForClicks && value != null) {
				isFocusable = true
			}
			field = value
		}
	
	constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)
	
	constructor(context: Context): this(context, null)
	
	init {
		val values = context.obtainStyledAttributes(attrs, R.styleable.MonthView)
		val inactiveColor = values.getColor(R.styleable.MonthView_inactiveColor, Color.GRAY)
		emptyBorders[0].color = inactiveColor
		val regularColor = values.getColor(R.styleable.MonthView_regularColor, Color.GRAY)
		regularBorders[0].color = regularColor
		val activeColor = values.getColor(R.styleable.MonthView_activeColor, regularColor)
		activeBorders[0].color = activeColor
		val todayColor = values.getColor(R.styleable.MonthView_todayColor, Color.BLACK)
		todayBorders[0].color = todayColor
		todayBorders[1].color = applyAlpha(todayColor, 128)
		activeTodayBorders[0].color = todayColor
		activeTodayBorders[1].color = applyAlpha(todayColor, 128)
		values.recycle()
		updateTexts()
		adapter = Adapter()
		onCellClickedListener = this
	}
	
	fun setOnDateClickedListener(callback: (date: LocalDate) -> Unit) {
		onDateClickedListener = object : OnDateClickedListener {
			override fun onDateClicked(date: LocalDate) {
				callback(date)
			}
		}
	}
	
	override fun onCellClicked(index: Int) {
		if (index < 7) return
		val date = dateStart.plusDays(index - 7)
		onDateClickedListener?.onDateClicked(date)
	}
	
	private fun updateTexts() {
		texts.clear()
		texts.addAll(date.let { now ->
			(0 until 7).map {
				weekDayFormatter.print(now.withDayOfWeek((firstDayOfWeek + it - 1) % 7 + 1))
			}
		})
		val now = LocalDate()
		val monthStart = date.withDayOfMonth(1)
		val monthStop = monthStart.plusMonths(1)
		dateStart = monthStart
		while (dateStart.dayOfWeek != firstDayOfWeek) {
			dateStart = dateStart.minusDays(1)
		}
		var dateStop = monthStop
		while (dateStop.dayOfWeek != firstDayOfWeek) {
			dateStop = dateStop.plusDays(1)
		}
		var date = dateStart
		todayIndex = -1
		monthStopIndex = -1
		while (date != dateStop) {
			when (date) {
				monthStart -> monthStartIndex = texts.size
				monthStop -> monthStopIndex = texts.size
			}
			if (date == now) {
				todayIndex = texts.size
			}
			texts.add(date.dayOfMonth.toString())
			date = date.plusDays(1)
		}
		if (monthStopIndex == -1) {
			monthStopIndex = texts.size
		}
	}
	
	private inner class Adapter: TextGridView.Adapter() {
		override fun firstRowIsHeader(): Boolean = true
		
		override fun getColumnCount(): Int = 7
		
		override fun getCount(): Int = texts.size
		
		override fun getText(index: Int): String = texts[index]
		
		override fun getBorders(index: Int): Array<Border> = if (index in monthStartIndex until monthStopIndex) {
			val highlighted = highlightAdapter?.let {
				val i = index - monthStartIndex
				i < it.indexStates.size && it.indexStates[i]
			} == true
			when {
				index == todayIndex && highlighted -> activeTodayBorders
				index == todayIndex -> todayBorders
				highlighted -> activeBorders
				else -> regularBorders
			}
		} else {
			emptyBorders
		}
	}
	
	interface OnDateClickedListener {
		fun onDateClicked(date: LocalDate)
	}
	
	abstract class HighlightAdapter {
		internal var view: MonthView? = null
		internal var indexStates = BooleanArray(0)
		
		abstract fun isDateHighlighted(date: LocalDate): Boolean
		
		fun notifyDataSetChanged() {
			val view = view ?: return
			indexStates = BooleanArray(view.monthStopIndex - view.monthStartIndex) {
				isDateHighlighted(view.dateStart.plusDays(it))
			}
			view.adapter?.notifyDataSetChanged()
		}
	}
	
	open class SimpleHighlightAdapter: HighlightAdapter() {
		val states = mutableSetOf<LocalDate>()
		
		override fun isDateHighlighted(date: LocalDate): Boolean = states.contains(date)
	}
}
