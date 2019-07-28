package com.seakernel.android.scoreapp.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

open class BaseViewHolder(parent: ViewGroup, resourceId: Int) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(resourceId, parent, false))
