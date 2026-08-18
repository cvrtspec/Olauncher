package app.olauncher.ui.oasis

import android.app.AlertDialog
import android.content.Context
import android.os.CountDownTimer
import android.text.InputType
import android.widget.EditText
import android.widget.TextView
import app.olauncher.R
import app.olauncher.data.Prefs
import app.olauncher.databinding.OasisPomodoroBinding
import app.olauncher.helper.getColorFromAttr
import app.olauncher.helper.showToast

/**
 * Pomodoro block: work / short-break / long-break modes. Durations are free-set per run
 * (tap the time to enter minutes) and remembered per mode via Prefs (last-used defaults,
 * initially 25 / 5 / 10 minutes).
 */
class OasisPomodoroBlock(
    private val context: Context,
    private val prefs: Prefs,
    private val binding: OasisPomodoroBinding
) {
    private enum class Mode { WORK, SHORT, LONG }

    private var mode = Mode.WORK
    private var remaining = 0            // seconds left
    private var running = false
    private var timer: CountDownTimer? = null

    private val textColor get() = context.getColorFromAttr(android.R.attr.textColorPrimary)
    private val hintColor get() = context.getColorFromAttr(android.R.attr.textColorHint)

    fun bind() {
        binding.apply {
            pomoHeader.setTextColor(textColor)
            pomoWork.setTextColor(textColor)
            pomoShort.setTextColor(textColor)
            pomoLong.setTextColor(textColor)
            pomoTime.setTextColor(textColor)
            pomoStart.setTextColor(textColor)
            pomoReset.setTextColor(textColor)
            pomoHint.setTextColor(hintColor)

            pomoWork.setOnClickListener { selectMode(Mode.WORK) }
            pomoShort.setOnClickListener { selectMode(Mode.SHORT) }
            pomoLong.setOnClickListener { selectMode(Mode.LONG) }
            pomoTime.setOnClickListener { if (!running) editMinutes() }
            pomoStart.setOnClickListener { toggleStart() }
            pomoReset.setOnClickListener { resetToMode() }
        }
        selectMode(Mode.WORK)
    }

    /** Stop the timer when the panel goes away. */
    fun stop() {
        timer?.cancel()
        timer = null
        running = false
    }

    private fun durationOf(m: Mode): Int = when (m) {
        Mode.WORK -> prefs.oasisPomoWork
        Mode.SHORT -> prefs.oasisPomoShort
        Mode.LONG -> prefs.oasisPomoLong
    }

    private fun setDurationOf(m: Mode, seconds: Int) {
        when (m) {
            Mode.WORK -> prefs.oasisPomoWork = seconds
            Mode.SHORT -> prefs.oasisPomoShort = seconds
            Mode.LONG -> prefs.oasisPomoLong = seconds
        }
    }

    private fun selectMode(m: Mode) {
        timer?.cancel()
        running = false
        mode = m
        remaining = durationOf(m)
        highlightMode()
        updateStartLabel()
        updateDisplay()
    }

    private fun highlightMode() {
        binding.pomoWork.paintFlags = flagsFor(mode == Mode.WORK, binding.pomoWork)
        binding.pomoShort.paintFlags = flagsFor(mode == Mode.SHORT, binding.pomoShort)
        binding.pomoLong.paintFlags = flagsFor(mode == Mode.LONG, binding.pomoLong)
        binding.pomoWork.alpha = if (mode == Mode.WORK) 1f else 0.5f
        binding.pomoShort.alpha = if (mode == Mode.SHORT) 1f else 0.5f
        binding.pomoLong.alpha = if (mode == Mode.LONG) 1f else 0.5f
    }

    private fun flagsFor(active: Boolean, view: TextView): Int =
        if (active) view.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
        else view.paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()

    private fun toggleStart() {
        if (running) {
            timer?.cancel()
            running = false
            updateStartLabel()
        } else {
            if (remaining <= 0) remaining = durationOf(mode)
            timer = object : CountDownTimer(remaining * 1000L, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    remaining = (millisUntilFinished / 1000L).toInt()
                    updateDisplay()
                }

                override fun onFinish() {
                    running = false
                    remaining = durationOf(mode)
                    updateStartLabel()
                    updateDisplay()
                    context.showToast(context.getString(R.string.oasis_pomo_done))
                }
            }.start()
            running = true
            updateStartLabel()
        }
    }

    private fun resetToMode() {
        timer?.cancel()
        running = false
        remaining = durationOf(mode)
        updateStartLabel()
        updateDisplay()
    }

    private fun editMinutes() {
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText((durationOf(mode) / 60).toString())
        }
        AlertDialog.Builder(context)
            .setTitle(R.string.oasis_pomo_set_title)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val mins = input.text.toString().toIntOrNull()?.coerceIn(1, 180) ?: return@setPositiveButton
                setDurationOf(mode, mins * 60)
                resetToMode()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateStartLabel() {
        binding.pomoStart.text = context.getString(
            if (running) R.string.oasis_pomo_pause else R.string.oasis_pomo_start
        )
    }

    private fun updateDisplay() {
        val m = remaining / 60
        val s = remaining % 60
        binding.pomoTime.text = String.format("%02d:%02d", m, s)
    }
}
