package com.seakernel.android.scoreapp.playerselect

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.InputFilter
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.ColorUtils
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import com.seakernel.android.scoreapp.R
import com.seakernel.android.scoreapp.databinding.DialogPlayerNameBinding
import com.seakernel.android.scoreapp.utility.PlayerColors

/**
 * The player dialog's color: a hex field showing the player's color, and a swatch for each graph color to pick instead
 * of typing one. A player without a color has an empty field, hinting at their seat's graph color, and keeps following
 * their seat (in every game) until a color is typed or picked. Clearing the field goes back to following the seat.
 *
 * @param seatColor the color the player gets from their seat in the game being set up
 * @param onChanged called when the color or whether it's valid changes
 */
class PlayerColorPicker(
    private val binding: DialogPlayerNameBinding,
    initialColor: Int?,
    private val seatColor: Int,
    private val onChanged: () -> Unit,
) {
    /** The color to save, null to follow the player's seat */
    var color: Int? = initialColor
        private set

    /** Whether the field holds a color that can be saved (an empty field can, it follows the seat) */
    var isValid = true
        private set

    private val context = binding.root.context
    private val swatches: List<Pair<ImageView, Int>>

    init {
        val names = context.resources.getStringArray(R.array.seatColorNames)
        swatches = PlayerColors.seatColors(context).mapIndexed { index, swatchColor ->
            createSwatch(swatchColor, names.getOrNull(index)) to swatchColor
        }

        binding.playerColorLayout.setStartIconTintList(null) // The icon is the color itself
        binding.playerColorLayout.placeholderText = PlayerColors.toHex(seatColor)
        binding.playerColorEdit.filters += InputFilter.AllCaps() // Typed like it's shown
        color?.let { binding.playerColorEdit.setText(PlayerColors.toHex(it)) }
        binding.playerColorEdit.doAfterTextChanged { text ->
            val typed = PlayerColors.fromHex(text.toString())
            isValid = text.isNullOrEmpty() || typed != null
            if (isValid) color = typed
            binding.playerColorLayout.error = if (isValid) null else context.getString(R.string.playerColorInvalid)
            render()
            onChanged()
        }
        render()
    }

    private fun createSwatch(swatchColor: Int, name: String?) = ImageView(context).apply {
        val size = resources.getDimensionPixelSize(R.dimen.color_swatch_size)
        val margin = resources.getDimensionPixelSize(R.dimen.color_swatch_margin)
        binding.playerColorSwatches.addView(this)
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            width = size
            height = size
            setMargins(margin, margin, margin, margin)
        }
        background = circle(swatchColor)
        scaleType = ImageView.ScaleType.CENTER
        // Black or white, whichever stands out more on the swatch
        setColorFilter(if (ColorUtils.calculateLuminance(swatchColor) > 0.5) Color.BLACK else Color.WHITE)
        contentDescription = name
        setOnClickListener { binding.playerColorEdit.setText(PlayerColors.toHex(swatchColor)) }
    }

    /** Shows the color in the field's icon, and checks its swatch (if it's one of them) */
    private fun render() {
        val shown = if (isValid) color ?: seatColor else null
        binding.playerColorLayout.startIconDrawable = shown?.let { circle(it) }
        swatches.forEach { (swatch, swatchColor) ->
            swatch.isSelected = swatchColor == shown
            swatch.setImageResource(if (swatch.isSelected) R.drawable.ic_check else 0)
        }
    }

    private fun circle(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        val size = context.resources.getDimensionPixelSize(R.dimen.color_preview_size)
        setSize(size, size)
    }
}
