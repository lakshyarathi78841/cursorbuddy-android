package com.cursorbuddy.android.overlay

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*

class InputPanel(context: Context) : FrameLayout(context) {

    interface InputListener {
        fun onQuestionSubmitted(question: String)
        fun onDismissed()
        fun onMicTapped()
    }

    var inputListener: InputListener? = null
    private val editText: EditText
    private val quickPromptsContainer: LinearLayout
    private val greetingText: TextView
    private val micButton: ImageView

    private val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    // Glassmorphism palette — low-alpha surfaces pair with the window's
    // FLAG_BLUR_BEHIND (API 31+) for real frosted-glass.
    private val glassTop = if (isDark) 0x66202838.toInt() else 0x88FFFFFF.toInt()
    private val glassBottom = if (isDark) 0x441A2030.toInt() else 0x55F4F8FF.toInt()
    private val textColor = if (isDark) 0xFFF2F4F8.toInt() else 0xFF17202A.toInt()
    private val secondaryText = if (isDark) 0xCCB8C2CC.toInt() else 0xCC4A5560.toInt()
    private val inputBg = if (isDark) 0x55101520.toInt() else 0x66FFFFFF.toInt()
    private val inputStroke = if (isDark) 0x44FFFFFF else 0x33FFFFFF
    private val chipBg = if (isDark) 0x661A3A4A.toInt() else 0x88E3F2FD.toInt()
    private val strokeColor = if (isDark) 0x55FFFFFF else 0x66FFFFFF
    private val accentColor = 0xFF4FC3F7.toInt()

    init {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(14))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(glassTop, glassBottom)
            ).apply {
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), strokeColor)
            }
            elevation = dp(12).toFloat()
        }

        // Drag handle
        val handle = View(context).apply {
            background = GradientDrawable().apply {
                setColor(if (isDark) 0x88FFFFFF.toInt() else 0x88222B38.toInt())
                cornerRadius = dp(2).toFloat()
            }
            setOnClickListener { inputListener?.onDismissed() }
        }
        container.addView(handle, LinearLayout.LayoutParams(dp(32), dp(4)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(8)
        })

        // Greeting
        greetingText = TextView(context).apply {
            text = "What would you like help with?"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(secondaryText)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(dp(2), 0, 0, dp(8))
        }
        container.addView(greetingText)

        // Input row
        val inputRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Mic button
        micButton = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (isDark) 0x66101520.toInt() else 0x88FFFFFF.toInt())
                setStroke(dp(1), strokeColor)
            }
            val btnSize = dp(42)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                marginEnd = dp(8)
            }
            setPadding(dp(9), dp(9), dp(9), dp(9))
            setOnClickListener { inputListener?.onMicTapped() }
        }
        inputRow.addView(micButton)

        editText = EditText(context).apply {
            hint = "Ask me anything..."
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(textColor)
            setHintTextColor(secondaryText)
            background = GradientDrawable().apply {
                setColor(inputBg)
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), inputStroke)
            }
            setPadding(dp(16), dp(11), dp(16), dp(11))
            imeOptions = EditorInfo.IME_ACTION_SEND
            isSingleLine = true
            maxLines = 1
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                    submitQuestion()
                    true
                } else false
            }
        }
        inputRow.addView(editText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        // Send button
        val sendBtn = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_send)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF81D4FA.toInt(), 0xFF4FC3F7.toInt())
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(1), 0x55FFFFFF)
            }
            val btnSize = dp(42)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                marginStart = dp(8)
            }
            setPadding(dp(9), dp(9), dp(9), dp(9))
            setColorFilter(0xFFFFFFFF.toInt())
            setOnClickListener { submitQuestion() }
        }
        inputRow.addView(sendBtn)

        container.addView(inputRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // Quick prompts
        val scrollView = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, dp(8), 0, 0)
        }
        quickPromptsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        scrollView.addView(quickPromptsContainer)
        container.addView(scrollView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        setQuickPrompts(listOf("Show me around", "Find settings", "Help me navigate"))

        addView(container, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun setGreeting(text: String) { greetingText.text = text }

    fun setListeningState(listening: Boolean) {
        if (listening) {
            editText.hint = "Listening..."
            editText.isEnabled = false
            micButton.setColorFilter(0xFFFF5252.toInt())
        } else {
            editText.hint = "Ask me anything..."
            editText.isEnabled = true
            micButton.clearColorFilter()
        }
    }

    fun setPartialText(text: String) {
        editText.setText(text)
        editText.setSelection(text.length)
    }

    fun setQuickPrompts(prompts: List<String>) {
        quickPromptsContainer.removeAllViews()
        prompts.forEach { text ->
            val chip = TextView(context).apply {
                this.text = text
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(accentColor)
                background = GradientDrawable().apply {
                    setColor(chipBg)
                    cornerRadius = dp(16).toFloat()
                    setStroke(dp(1), strokeColor)
                }
                setPadding(dp(12), dp(6), dp(12), dp(6))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(6) }
                setOnClickListener {
                    editText.setText(text)
                    submitQuestion()
                }
            }
            quickPromptsContainer.addView(chip)
        }
    }

    private fun submitQuestion() {
        val question = editText.text.toString().trim()
        if (question.isNotEmpty()) {
            editText.text.clear()
            inputListener?.onQuestionSubmitted(question)
        }
    }

    fun requestInputFocus() { editText.requestFocus() }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()
    }
}
