package util

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Toast
import com.algorigo.pressuregoapp.R
import com.algorigo.pressuregoapp.databinding.LayoutToastBinding
import extension.dpToPx

object ToastUtil {
    fun makeToast(context: Context, text: String): Toast {
        val binding = LayoutToastBinding.bind(LayoutInflater.from(context).inflate(R.layout.layout_toast, null, false))

        binding.tvToastMessage.text = text

        return Toast(context).apply {
            setGravity(Gravity.BOTTOM or Gravity.CENTER, 0, 32f.dpToPx(context))
            duration = Toast.LENGTH_LONG
            view = binding.root
        }
    }
}