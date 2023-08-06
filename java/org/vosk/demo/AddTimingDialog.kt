package org.vosk.demo

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import org.vosk.demo.activities.PlayerActivity
import org.vosk.demo.databinding.DialogLayoutBinding
import ru.tinkoff.decoro.MaskImpl
import ru.tinkoff.decoro.parser.UnderscoreDigitSlotsParser
import ru.tinkoff.decoro.watchers.FormatWatcher
import ru.tinkoff.decoro.watchers.MaskFormatWatcher

class AddTimingDialog(private val adapter: VideoPlayerRecyclerViewAdapter): DialogFragment() {
    private lateinit var binding: DialogLayoutBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogLayoutBinding.inflate(inflater, container, false)
        dialog!!.window?.setBackgroundDrawableResource(R.drawable.background)
        binding.addTimingButton.setOnClickListener {addTiming()}

        val slots = UnderscoreDigitSlotsParser().parseSlots("__:__:__")
        val formatWatcher: FormatWatcher = MaskFormatWatcher(MaskImpl.createTerminated(slots))
        formatWatcher.installOn(binding.timingFromInput)
        formatWatcher.installOn(binding.timingToInput)

        return binding.root
    }
    private fun addTiming(){
        with(binding){
            if (timingFromInput.text.isNotBlank() &&
                timingToInput.text.isNotBlank() &&
                timingFromInput.text != timingToInput.text){
                val fromList = timingFromInput.text.split(":")
                var fromMs = fromList[0].toInt()*60*1000
                fromMs += fromList[1].toInt()*1000
                fromMs += fromList[2].toInt()*10
                val from = fromMs.toFloat()/1000

                val toList = timingToInput.text.split(":")
                var toMs = toList[0].toInt()*60*1000
                toMs += toList[1].toInt()*1000
                toMs += toList[2].toInt()*10
                val to = toMs.toFloat()/1000

                adapter.addTiming(Pair(from.toString(), to.toString()))
                dialog?.dismiss()

            } else {
                Toast.makeText(activity?.applicationContext, "Некорректное время", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        val height = (resources.displayMetrics.heightPixels * 0.40).toInt()
        dialog!!.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}