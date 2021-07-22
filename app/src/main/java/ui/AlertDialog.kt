package ui

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.algorigo.pressuregoapp.databinding.DialogAlertBinding

class AlertDialog: DialogFragment() {

    private lateinit var binding: DialogAlertBinding

    private var yesCallback: (() -> Unit)? = null
    private var noCallback: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        binding = DialogAlertBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun onResume() {
        super.onResume()
        val window = dialog!!.window!!
        val size = Point()

        val display = window.windowManager.defaultDisplay
        display.getSize(size)

        val width = size.x * 0.9
        val height = window.attributes.height
        if (dialog?.window != null) {
            dialog?.window!!.setLayout(width.toInt(), height)
        }
    }

    private fun initView() {
        with(binding) {
            arguments?.getString(KEY_TITLE)?.let {
                tvTitle.text = it
            }
            arguments?.getString(KEY_CONTENT)?.let {
                tvContent.text = it
            }

            btnYes.setOnClickListener {
                yesCallback?.invoke()
                dismiss()
            }

            btnNo.setOnClickListener {
                noCallback?.invoke()
                dismiss()
            }
        }

    }

    companion object {

        const val KEY_TITLE = "KEY_TITLE"
        const val KEY_CONTENT = "KEY_CONTENT"

        fun newInstance(title: String, content: String, yesCallback: (() -> Unit)? = null, noCallback: (() -> Unit)? = null) = AlertDialog().apply {
            arguments = Bundle().apply {
                putString(KEY_TITLE, title)
                putString(KEY_CONTENT, content)
            }
            this.yesCallback = yesCallback
            this.noCallback = noCallback
        }
    }
}