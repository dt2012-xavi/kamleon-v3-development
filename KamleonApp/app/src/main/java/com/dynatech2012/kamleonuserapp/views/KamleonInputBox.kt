package com.dynatech2012.kamleonuserapp.views

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.dynatech2012.kamleonuserapp.R

class KamleonInputBox : ConstraintLayout {
    private var view : View = LayoutInflater.from(context).inflate(R.layout.layout_kamleon_input_box, this, true)
    private var textView: TextView? = null
    private var editText: EditText? = null
    private var eyeIconLayout: LinearLayout? = null
    private var ivEye: ImageView? = null

    private var securePwdOn: Boolean = false

    constructor(context: Context?) : super(context!!) {
        commonInit()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
        commonInit()
        attrs?.let {
            parseAttributes(it)
        }
    }

    private fun commonInit() {
        textView = view.findViewById(R.id.inputBoxTextView) as TextView
        editText = view.findViewById(R.id.inputBoxEditText) as EditText
        eyeIconLayout = view.findViewById(R.id.eyeIconContainerLayout)
        ivEye = view.findViewById(R.id.imageViewEye)

        eyeIconLayout?.visibility = GONE

        eyeIconLayout?.setOnClickListener {
            securePwdOn = !securePwdOn
            updateUIFromSecureMode()
        }
    }

    private fun parseAttributes(attrs: AttributeSet) {
        val attrEditText =
            context.obtainStyledAttributes(attrs, R.styleable.KamleonInputBoxStyle)

        try {
            val attrTextPlaceholder =
                attrEditText.getString(R.styleable.KamleonInputBoxStyle_placeholder)

            attrTextPlaceholder?.let { attrStr ->
                textView?.text = attrStr
            }

            val attrSecureInput =
                attrEditText.getBoolean(R.styleable.KamleonInputBoxStyle_secureInput, false)
            attrSecureInput?.let { attrBool ->
                if (attrBool) {
                    setupForPassword()
                }
            }
        } finally {
            attrEditText.recycle()
        }
    }

    private fun setupForPassword() {
        eyeIconLayout?.visibility = VISIBLE
        ivEye?.setImageResource(R.drawable.icn_eye_on)
        securePwdOn = true

        getEditTextView()?.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    }

    private fun updateUIFromSecureMode() {
        ivEye?.setImageResource(if (securePwdOn) R.drawable.icn_eye_on else R.drawable.icn_eye_off)
        if (securePwdOn) {
            getEditTextView()?.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else {
            getEditTextView()?.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
    }

    fun getEditTextView() : EditText? {
        return editText
    }
}