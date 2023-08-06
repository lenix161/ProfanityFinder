package org.vosk.demo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.vosk.demo.databinding.TimingItemBinding
import ru.tinkoff.decoro.MaskImpl
import ru.tinkoff.decoro.parser.UnderscoreDigitSlotsParser
import ru.tinkoff.decoro.watchers.FormatWatcher
import ru.tinkoff.decoro.watchers.MaskFormatWatcher


class VideoPlayerRecyclerViewAdapter(private val listener: OnClickListener): RecyclerView.Adapter<VideoPlayerRecyclerViewAdapter.Holder>() {
    inner class Holder(item: View): RecyclerView.ViewHolder(item){
        private val binding = TimingItemBinding.bind(item)
        fun bind(time: Pair<String, String>, listener: OnClickListener){

            binding.startTiming.text = convertTime(time.first)
            binding.endTiming.text = convertTime(time.second)
            binding.playTimingButton.setOnClickListener { listener.onClickPlay(time) }
            binding.deleteTimingButton.setOnClickListener { listener.onClickDelete(time)  }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.timing_item, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int {
        return Data.timings.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(Data.timings[position], listener)
    }

    fun addTiming(time: Pair<String, String>){
        Data.timings.add(time)
        notifyDataSetChanged()
    }

    fun deleteTiming(time: Pair<String, String>){
        Data.timings.remove(time)
        notifyDataSetChanged()
    }

    private fun convertTime(time: String): String{
        var sec = time.split(".")[0].toInt()
        val ms = time.split(".")[1].toInt()

        val min = sec/60
        sec -= min * 60

        var result = ""
        if (min < 10){
            result += "0$min:"
        } else {
            result += "$min:"
        }

        if (sec < 10){
            result += "0$sec:"
        } else {
            result += "$sec:"
        }

        if (ms < 10){
            result += "0$ms"
        } else {
            result += "$ms"
        }

        return result
    }

    interface OnClickListener {
        fun onClickPlay(time: Pair<String, String>)
        fun onClickDelete(time: Pair<String, String>)
    }

}