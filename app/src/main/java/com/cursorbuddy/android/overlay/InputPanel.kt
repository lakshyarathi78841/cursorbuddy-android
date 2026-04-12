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
    private val surfaceColor = if (isDark) 0xF01E1E1E.toInt() else 0xF5FFFFFF.toInt()
    private val textColor = if (isDark) 0xFFE0E0E0.toInt() else 0xFF333333.toInt()
    private val secondaryText = if (isDark) 0xFF999999.toInt() else 0xFF666666.toInt()
    private val inputBg = if (isDark) 0xFF2A2A2A.toInt() else 0xFFF0F0F0.toInt()
    private val chipBg = if (isDark) 0xFF1A3A4A.toInt() else 0xFFE3F2FD.toInt()
    private val strokeColor = if (isDark) 0x33FFFFFF else 0x22000000
    private val accentColor = 0xFF4FC3F7.toInt()

    init {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(12))
            background = GradientDrawable().apply {
                setColor(surfaceColor)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), strokeColor)
            }
            elevation = dp(10).toFloat()
        }

        // Drag handle
        val handle = View(context).apply {
            background = GradientDrawable().apply {
                setColor(if (isDark) 0xFF555555.toInt() else 0xFFCCCCCC.toInt())
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
                setColor(if (isDark) 0xFF333333.toInt() else 0xFFE8E8E8.toInt())
            }
            val btnSize = dp(40)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                marginEnd = dp(6)
            }
            setPadding(dp(8), dp(8), dp(8), dp(8))
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
                cornerRadius = dp(20).toFloat()
            }
            setPadding(dp(14), dp(10), dp(14), dp(10))
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
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accentColor)
            }
            val btnSize = dp(40)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                marginStart = dp(6)
            }
            setPadding(dp(8), dp(8), dp(8), dp(8))
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
                    cornerRadius = dp(14).toFloat()
                }
                setPadding(dp(10), dp(5), dp(10), dp(5))
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
