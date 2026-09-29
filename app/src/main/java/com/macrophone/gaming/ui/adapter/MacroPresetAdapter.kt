package com.macrophone.gaming.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.macrophone.gaming.R
import com.macrophone.gaming.data.model.MacroSequence
import com.macrophone.gaming.databinding.ItemMacroPresetBinding

/**
 * Adapter hiển thị danh sách các Macro Combo Preset trong MainActivity
 */
class MacroPresetAdapter(
    private var items: List<MacroSequence>,
    private var activeId: String?,
    private val onSelect: (MacroSequence) -> Unit,
    private val onQuickPlay: (MacroSequence) -> Unit,
    private val onDelete: (MacroSequence) -> Unit
) : RecyclerView.Adapter<MacroPresetAdapter.ViewHolder>() {

    fun updateData(newItems: List<MacroSequence>, currentActiveId: String?) {
        items = newItems
        activeId = currentActiveId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMacroPresetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemMacroPresetBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MacroSequence) {
            val context = binding.root.context
            val isActive = item.id == activeId

            binding.tvPresetName.text = item.name

            val loopText = if (item.config.isInfiniteLoop) "∞ Vô hạn" else "${item.config.loopCount} lần"
            binding.tvPresetInfo.text =
                "${item.totalActionCount} thao tác | Tốc độ: ${item.config.speedMultiplier}x | Lặp: $loopText"

            if (isActive) {
                binding.root.strokeColor = ContextCompat.getColor(context, R.color.cyan_neon)
                binding.root.strokeWidth = 3
                binding.ivPresetIcon.setColorFilter(ContextCompat.getColor(context, R.color.cyan_neon))
            } else {
                binding.root.strokeColor = ContextCompat.getColor(context, R.color.stroke_border)
                binding.root.strokeWidth = 1
                binding.ivPresetIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary))
            }

            binding.root.setOnClickListener {
                onSelect(item)
            }

            binding.btnQuickPlay.setOnClickListener {
                onQuickPlay(item)
            }

            binding.btnDelete.setOnClickListener {
                onDelete(item)
            }
        }
    }
}
