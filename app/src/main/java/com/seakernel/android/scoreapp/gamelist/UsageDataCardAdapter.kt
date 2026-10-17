package com.seakernel.android.scoreapp.gamelist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.databinding.HolderUsageDataCardBinding
import com.seakernel.android.scoreapp.ui.BaseViewHolder
import com.spotify.mobius.functions.Consumer

/**
 * Asks whether usage statistics and crash reports can be shared, as a card at the top of the game list. Both stay
 * off until the user answers, which they can put off as long as they like. Both answers are equally easy to pick,
 * and each one can be changed separately later in settings.
 */
class UsageDataCardAdapter(
    private val isShown: Boolean,
    private val eventConsumer: Consumer<ListEvent>
) : RecyclerView.Adapter<UsageDataCardViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsageDataCardViewHolder {
        return UsageDataCardViewHolder(parent)
    }

    override fun getItemCount(): Int = if (isShown) 1 else 0

    // Unique among the game list's view types, so the two adapters' recycled views are never mixed up
    override fun getItemViewType(position: Int): Int = R.layout.holder_usage_data_card

    override fun getItemId(position: Int): Long = 0

    override fun onBindViewHolder(holder: UsageDataCardViewHolder, position: Int) {
        holder.bind(eventConsumer)
    }
}

class UsageDataCardViewHolder(parent: ViewGroup) : BaseViewHolder<HolderUsageDataCardBinding>(
    HolderUsageDataCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
) {

    fun bind(eventConsumer: Consumer<ListEvent>) {
        binding.usageDataAllow.setOnClickListener { eventConsumer.accept(ListEvent.UsageDataAnswered(true)) }
        binding.usageDataDeny.setOnClickListener { eventConsumer.accept(ListEvent.UsageDataAnswered(false)) }
    }
}
